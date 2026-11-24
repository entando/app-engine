package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.caching.CacheAccessor;
import io.lettuce.core.support.caching.CacheFrontend;
import io.lettuce.core.support.caching.ClientSideCaching;
import jakarta.annotation.PreDestroy;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.entando.entando.aps.system.services.cache.RedisEnvironmentVariables;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisActive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RedisActive(true)
public class CacheFrontendManager {

    private static final long INITIAL_RETRY_DELAY_MILLIS = 1000;

    private final RedisClient redisClient;
    private final Object lock = new Object();
    private final AtomicReference<Generation> current = new AtomicReference<>();
    private final long maxRetryDelayMillis;
    private final ScheduledExecutorService executor;

    private final AtomicReference<Generation> refreshTarget = new AtomicReference<>();
    private final AtomicBoolean refreshScheduled = new AtomicBoolean();
    private volatile boolean closing;
    private long retryDelayMillis = INITIAL_RETRY_DELAY_MILLIS;

    @Autowired
    public CacheFrontendManager(RedisClient redisClient) {
        this(redisClient, Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "cache-frontend-refresh");
            thread.setDaemon(true);
            return thread;
        }));
    }

    CacheFrontendManager(RedisClient redisClient, ScheduledExecutorService executor) {
        this.redisClient = redisClient;
        this.maxRetryDelayMillis = TimeUnit.SECONDS.toMillis(RedisEnvironmentVariables.frontEndCacheCheckDelay());
        this.executor = executor;
    }

    /**
     * The returned frontend is closed when replaced: call this method on each access.
     */
    public CacheFrontend<String, Object> getCacheFrontend() {
        Generation generation = this.current.get();
        if (generation != null) {
            return generation.frontend();
        }
        synchronized (this.lock) {
            if (this.current.get() == null) {
                this.current.set(this.buildGeneration());
            }
            return this.current.get().frontend();
        }
    }

    /**
     * Publishes a new frontend and closes the previous one only afterwards; if the build fails, the previous
     * frontend stays published and open.
     */
    public CacheFrontend<String, Object> rebuildCacheFrontend() {
        synchronized (this.lock) {
            return this.replaceGeneration().frontend();
        }
    }

    @PreDestroy
    public void close() {
        this.closing = true;
        this.executor.shutdownNow();
    }

    private Generation replaceGeneration() {
        Generation next = this.buildGeneration();
        Generation previous = this.current.getAndSet(next);
        if (previous != null) {
            try {
                previous.frontend().close();
            } catch (RuntimeException e) {
                log.warn("Error closing the previous front-end-cache", e);
            }
        }
        return next;
    }

    private Generation buildGeneration() {
        StatefulRedisConnection<String, Object> connection = redisClient.connect(new SerializedObjectCodec());
        TrackingArgs trackingArgs = TrackingArgs.Builder.enabled().bcast();
        GatedCacheAccessor accessor = new GatedCacheAccessor();
        CacheFrontend<String, Object> frontend;
        try {
            frontend = ClientSideCaching.enable(accessor, connection, trackingArgs);
        } catch (RuntimeException e) {
            connection.close();
            throw e;
        }
        Generation generation = new Generation(frontend, accessor);
        connection.addListener(new ReconnectListener(generation));
        return generation;
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
        Generation target = this.refreshTarget.get();
        if (this.closing || target == null) {
            return;
        }
        try {
            synchronized (this.lock) {
                if (this.closing || this.current.get() != target) {
                    this.retryDelayMillis = INITIAL_RETRY_DELAY_MILLIS;
                    return;
                }
                this.rebuildCacheFrontend();
            }
            this.retryDelayMillis = INITIAL_RETRY_DELAY_MILLIS;
            log.warn("Refresh of front-end-cache after reconnection completed");
        } catch (Exception e) {
            if (this.closing) {
                return;
            }
            long delay = this.retryDelayMillis;
            this.retryDelayMillis = Math.min(delay * 2, this.maxRetryDelayMillis);
            log.error("Refresh of front-end-cache after reconnection failed, retrying in {} ms", delay, e);
            this.scheduleRefresh(delay);
        }
    }

    private record Generation(CacheFrontend<String, Object> frontend, GatedCacheAccessor accessor) {
    }

    private final class ReconnectListener implements RedisConnectionStateListener {

        private final Generation generation;
        private volatile boolean disconnected;

        private ReconnectListener(Generation generation) {
            this.generation = generation;
        }

        @Override
        public void onRedisDisconnected(RedisChannelHandler<?, ?> connection) {
            this.disconnected = true;
        }

        @Override
        public void onRedisConnected(RedisChannelHandler<?, ?> connection, SocketAddress socketAddress) {
            if (!this.disconnected) {
                return;
            }
            this.disconnected = false;
            this.generation.accessor().disable();
            log.warn("Front-end-cache connection reconnected to '{}': local cache disabled, refresh scheduled",
                    socketAddress);
            this.requestRefresh();
        }

        private void requestRefresh() {
            CacheFrontendManager.this.refreshTarget.set(this.generation);
            CacheFrontendManager.this.scheduleRefresh(0);
        }
    }

    private static final class GatedCacheAccessor implements CacheAccessor<String, Object> {

        private final Map<String, Object> map = new ConcurrentHashMap<>();
        private volatile boolean active = true;

        @Override
        public Object get(String key) {
            return this.active ? this.map.get(key) : null;
        }

        @Override
        public void put(String key, Object value) {
            if (!this.active) {
                return;
            }
            this.map.put(key, value);
            if (!this.active) {
                this.map.remove(key);
            }
        }

        @Override
        public void evict(String key) {
            this.map.remove(key);
        }

        private void disable() {
            this.active = false;
            this.map.clear();
        }
    }
}
