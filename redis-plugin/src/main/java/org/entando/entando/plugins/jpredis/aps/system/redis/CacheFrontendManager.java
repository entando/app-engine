package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.caching.CacheAccessor;
import io.lettuce.core.support.caching.CacheFrontend;
import io.lettuce.core.support.caching.ClientSideCaching;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisActive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RedisActive(true)
public class CacheFrontendManager {

    private final RedisClient redisClient;
    private final Object lock = new Object();
    private volatile CacheFrontend<String, Object> cacheFrontend;

    @Autowired
    public CacheFrontendManager(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    public CacheFrontend<String, Object> getCacheFrontend() {
        CacheFrontend<String, Object> current = this.cacheFrontend;
        if (current != null) {
            return current;
        }
        synchronized (this.lock) {
            if (this.cacheFrontend == null) {
                this.cacheFrontend = this.buildCacheFrontend();
            }
            return this.cacheFrontend;
        }
    }

    /**
     * Publishes a new frontend and closes the previous one only afterwards; if the build fails, the previous
     * frontend stays published and open.
     */
    public CacheFrontend<String, Object> rebuildCacheFrontend() {
        synchronized (this.lock) {
            CacheFrontend<String, Object> next = this.buildCacheFrontend();
            CacheFrontend<String, Object> previous = this.cacheFrontend;
            this.cacheFrontend = next;
            if (previous != null) {
                try {
                    previous.close();
                } catch (RuntimeException e) {
                    log.warn("Error closing the previous front-end-cache", e);
                }
            }
            return next;
        }
    }

    private CacheFrontend<String, Object> buildCacheFrontend() {
        StatefulRedisConnection<String, Object> connection = redisClient.connect(new SerializedObjectCodec());
        TrackingArgs trackingArgs = TrackingArgs.Builder.enabled().bcast();
        Map<String, Object> clientCache = new ConcurrentHashMap<>();
        return ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connection, trackingArgs);
    }
}
