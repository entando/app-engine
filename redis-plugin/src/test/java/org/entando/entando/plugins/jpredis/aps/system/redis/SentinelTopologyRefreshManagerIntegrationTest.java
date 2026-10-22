package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.support.caching.CacheFrontend;
import java.net.SocketAddress;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.plugins.jpredis.utils.RedisSentinelTestExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@ExtendWith(RedisSentinelTestExtension.class)
class SentinelTopologyRefreshManagerIntegrationTest {

    private static final String MASTER_ID = "mymaster";
    private static final long TIMEOUT_MILLIS = 60_000;

    @Test
    void existingCacheShouldReadThroughNewMasterAfterFailover() throws Exception {
        SentinelConfig config = new SentinelConfig();
        DefaultClientResources resources = DefaultClientResources.create();
        RedisURI redisURI = config.redisURI();
        RedisClient redisClient = config.getRedisClient(resources, redisURI);
        LettuceConnectionFactory connectionFactory = config.connectionFactory();
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        CacheFrontendManager cacheFrontendManager = new CacheFrontendManager(redisClient);
        ITenantManager tenantManager = Mockito.mock(ITenantManager.class);
        Mockito.when(tenantManager.getCodes()).thenReturn(List.of());
        LettuceCacheManager cacheManager = (LettuceCacheManager) new BaseRedisCacheConfig().cacheManager(
                redisClient, connectionFactory, cacheFrontendManager, tenantManager);
        cacheManager.afterPropertiesSet();
        SentinelTopologyRefreshManager refreshManager = new SentinelTopologyRefreshManager(redisClient, redisURI,
                cacheFrontendManager);
        try {
            Cache cache = cacheManager.getCache("Entando_Cache");
            cache.put("beforeFailover", "value1");
            Assertions.assertEquals("value1", cache.get("beforeFailover").get());
            CacheFrontend<String, Object> frontendBeforeFailover = cacheFrontendManager.getCacheFrontend();
            SocketAddress masterBeforeFailover = getMasterAddress(redisClient);

            try (StatefulRedisSentinelConnection<String, String> connection = redisClient.connectSentinel()) {
                connection.sync().failover(MASTER_ID);
            }

            awaitTrue(() -> !masterBeforeFailover.equals(getMasterAddress(redisClient)), "master switch");
            awaitTrue(() -> cacheFrontendManager.getCacheFrontend() != frontendBeforeFailover, "frontend rebuild");
            awaitTrue(() -> tryPutAndRead(cache, "afterFailover", "value2"), "write visible on the new master");

            Cache.ValueWrapper value = Assertions.assertDoesNotThrow(() -> cache.get("afterFailover"));
            Assertions.assertEquals("value2", value.get());
            Assertions.assertDoesNotThrow(() -> cache.get("missingKey"));
        } finally {
            refreshManager.close();
            connectionFactory.destroy();
            redisClient.shutdown();
            resources.shutdown();
        }
    }

    private static SocketAddress getMasterAddress(RedisClient redisClient) {
        try (StatefulRedisSentinelConnection<String, String> connection = redisClient.connectSentinel()) {
            return connection.sync().getMasterAddrByName(MASTER_ID);
        }
    }

    private static boolean tryPutAndRead(Cache cache, String key, String value) {
        try {
            cache.put(key, value);
            Cache.ValueWrapper stored = cache.get(key);
            return stored != null && value.equals(stored.get());
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static void awaitTrue(BooleanSupplier condition, String description) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                Assertions.fail("Timed out waiting for " + description);
            }
            Thread.sleep(200);
        }
    }
}
