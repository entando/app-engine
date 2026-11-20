package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import jakarta.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.entando.entando.aps.system.services.cache.RedisEnvironmentVariables;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisActive;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisSentinel;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisSentinelEventsActive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RedisActive(true)
@RedisSentinel(true)
@RedisSentinelEventsActive(true)
public class SentinelTopologyRefreshManager {

    private static final String SWITCH_MASTER_EVENT = "+switch-master";
    private static final long INITIAL_RETRY_DELAY_MILLIS = 1000;

    private final RedisClient redisClient;
    private final CacheFrontendManager cacheFrontendManager;
    private final String masterId;
    private final long maxRetryDelayMillis;
    private final ScheduledExecutorService executor;

    private final AtomicReference<String> desiredMaster = new AtomicReference<>();
    private final AtomicBoolean refreshScheduled = new AtomicBoolean();
    private volatile String appliedMaster;
    private volatile boolean closing;
    private long retryDelayMillis = INITIAL_RETRY_DELAY_MILLIS;

    private final List<StatefulRedisPubSubConnection<String, String>> pubSubConnections = new ArrayList<>();

    @Autowired
    public SentinelTopologyRefreshManager(RedisClient redisClient, RedisURI redisURI,
            CacheFrontendManager cacheFrontendManager) {
        this.redisClient = redisClient;
        this.cacheFrontendManager = cacheFrontendManager;
        this.masterId = redisURI.getSentinelMasterId();
        this.maxRetryDelayMillis = TimeUnit.SECONDS.toMillis(RedisEnvironmentVariables.frontEndCacheCheckDelay());
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "sentinel-topology-refresh");
            thread.setDaemon(true);
            return thread;
        });

        this.getInitialMasterAddress().ifPresent(address -> {
            this.desiredMaster.set(address);
            this.appliedMaster = address;
            log.info("CURRENT master node '{}'", address);
        });

        for (RedisURI sentinelUri : redisURI.getSentinels()) {
            try {
                subscribeSentinel(sentinelUri);
            } catch (RedisException e) {
                log.warn("Unable to subscribe to {} event on {}", SWITCH_MASTER_EVENT, sentinelUri, e);
            }
        }
        if (pubSubConnections.isEmpty()) {
            log.error("No Sentinel subscribed to {} event: front-end-cache will not be refreshed on failover. "
                    + "Consider switching to sentinel scheduler setting REDIS_USE_SENTINEL_EVENTS to false",
                    SWITCH_MASTER_EVENT);
        }

        log.debug("{} initialized", SentinelTopologyRefreshManager.class.getName());
    }

    /**
     * @deprecated use {@link #SentinelTopologyRefreshManager(RedisClient, RedisURI, CacheFrontendManager)} instead
     */
    @Deprecated(since = "7.5.4")
    public SentinelTopologyRefreshManager(RedisClient redisClient, RedisURI redisURI,
            LettuceCacheManager cacheManager, CacheFrontendManager cacheFrontendManager) {
        this(redisClient, redisURI, cacheFrontendManager);
    }

    private void subscribeSentinel(RedisURI sentinelUri) {
        log.debug("Subscribing to {} event on {}", SWITCH_MASTER_EVENT, sentinelUri);
        StatefulRedisPubSubConnection<String, String> pubSubConnection = redisClient.connectPubSub(sentinelUri);
        pubSubConnection.addListener(getSwitchMasterListener());
        pubSubConnection.sync().psubscribe(SWITCH_MASTER_EVENT);
        pubSubConnections.add(pubSubConnection);
    }

    @PreDestroy
    public void close() {
        this.closing = true;
        this.executor.shutdownNow();
        for (StatefulRedisPubSubConnection<String, String> pubSubConnection : pubSubConnections) {
            pubSubConnection.close();
        }
    }

    private RedisPubSubAdapter<String, String> getSwitchMasterListener() {
        return new RedisPubSubAdapter<>() {
            @Override
            public void message(String pattern, String channel, String message) {
                log.debug("pattern: {}, channel: {}, message: {}", pattern, channel, message);
                String[] parts = message.split(" ");
                if (parts.length != 5) {
                    log.error("Unexpected message format for Sentinel switch master event: '{}'. "
                            + "Front-end-cache will not be refreshed. Consider switching to "
                            + "sentinel scheduler setting REDIS_USE_SENTINEL_EVENTS to false", message);
                    return;
                }
                if (!parts[0].equals(masterId)) {
                    log.debug("Ignoring {} event of master '{}'", SWITCH_MASTER_EVENT, parts[0]);
                    return;
                }
                SentinelTopologyRefreshManager.this.updateMaster(parts[3] + ":" + parts[4]);
            }
        };
    }

    private void updateMaster(String newMaster) {
        this.desiredMaster.set(newMaster);
        this.scheduleRefresh(0);
    }

    private void scheduleRefresh(long delayMillis) {
        if (this.closing || !this.refreshScheduled.compareAndSet(false, true)) {
            return;
        }
        try {
            this.executor.schedule(this::refresh, delayMillis, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            this.refreshScheduled.set(false);
        }
    }

    private void refresh() {
        this.refreshScheduled.set(false);
        String target = this.desiredMaster.get();
        if (this.closing || target == null || target.equals(this.appliedMaster)) {
            return;
        }
        try {
            Optional<String> sentinelMaster = this.getMasterAddress();
            if (!sentinelMaster.filter(target::equals).isPresent()) {
                log.warn("Sentinel reports master node '{}' instead of '{}': front-end-cache refresh postponed",
                        sentinelMaster.orElse(null), target);
                this.scheduleRetry();
                return;
            }
            log.warn("Refresh of front-end-cache -> from master node '{}' to '{}'", this.appliedMaster, target);
            this.cacheFrontendManager.rebuildCacheFrontend();
            this.appliedMaster = target;
            this.retryDelayMillis = INITIAL_RETRY_DELAY_MILLIS;
            log.warn("Refresh of front-end-cache completed on master node '{}'", target);
        } catch (Exception e) {
            log.error("Refresh of front-end-cache to master node '{}' failed, retrying in {} ms",
                    target, this.retryDelayMillis, e);
            this.scheduleRetry();
        }
    }

    private void scheduleRetry() {
        long delay = this.retryDelayMillis;
        this.retryDelayMillis = Math.min(delay * 2, this.maxRetryDelayMillis);
        this.scheduleRefresh(delay);
    }

    private Optional<String> getInitialMasterAddress() {
        try {
            return this.getMasterAddress();
        } catch (RuntimeException e) {
            log.warn("Unable to read the current master node from Sentinel", e);
            return Optional.empty();
        }
    }

    private Optional<String> getMasterAddress() {
        log.debug("Checking Sentinel master address");
        try (StatefulRedisSentinelConnection<String, String> connection = redisClient.connectSentinel()) {
            SocketAddress address = connection.sync().getMasterAddrByName(this.masterId);
            if (!(address instanceof InetSocketAddress)) {
                log.warn("Sentinel master node '{}' not found!", this.masterId);
                return Optional.empty();
            }
            InetSocketAddress inetAddress = (InetSocketAddress) address;
            return Optional.of(inetAddress.getHostString() + ":" + inetAddress.getPort());
        }
    }
}
