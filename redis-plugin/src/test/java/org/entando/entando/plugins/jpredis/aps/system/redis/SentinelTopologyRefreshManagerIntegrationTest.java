package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.support.caching.CacheFrontend;
import java.net.SocketAddress;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.plugins.jpredis.utils.RedisClientList;
import org.entando.entando.plugins.jpredis.utils.RedisSentinelTestExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@ExtendWith(RedisSentinelTestExtension.class)
class SentinelTopologyRefreshManagerIntegrationTest {

    private static final String MASTER_ID = "mymaster";
    private static final long TIMEOUT_MILLIS = 60_000;

    private DefaultClientResources resources;
    private RedisClient redisClient;
    private LettuceConnectionFactory connectionFactory;
    private StatefulRedisConnection<String, String> master;
    private CacheFrontendManager cacheFrontendManager;
    private long frontendConnection;
    private SentinelTopologyRefreshManager refreshManager;
    private Cache cache;

    @BeforeEach
    void setUp() {
        SentinelConfig config = new SentinelConfig();
        resources = DefaultClientResources.create();
        RedisURI redisURI = config.redisURI();
        redisClient = config.getRedisClient(resources, redisURI);
        connectionFactory = config.connectionFactory();
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        master = redisClient.connect();
        cacheFrontendManager = new CacheFrontendManager(redisClient);
        frontendConnection = RedisClientList.openedTrackingConnection(master.sync(), this::createCache);
        refreshManager = new SentinelTopologyRefreshManager(redisClient, redisURI, cacheFrontendManager);
    }

    private void createCache() {
        ITenantManager tenantManager = Mockito.mock(ITenantManager.class);
        Mockito.when(tenantManager.getCodes()).thenReturn(List.of());
        LettuceCacheManager cacheManager = (LettuceCacheManager) new BaseRedisCacheConfig().cacheManager(
                redisClient, connectionFactory, cacheFrontendManager, tenantManager);
        cacheManager.afterPropertiesSet();
        cache = cacheManager.getCache("Entando_Cache");
    }

    @AfterEach
    void tearDown() {
        refreshManager.close();
        cacheFrontendManager.close();
        master.close();
        connectionFactory.destroy();
        redisClient.shutdown();
        resources.shutdown();
    }

    @Test
    void existingCacheShouldReadThroughNewMasterAfterFailover() throws Exception {
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
    }

    @Test
    void existingCacheShouldNotServeStaleValueAfterFrontendReconnect() throws Exception {
        cache.put("beforeReconnect", "value1");
        Assertions.assertEquals("value1", cache.get("beforeReconnect").get());

        RedisClientList.killAndAwaitReconnect(master.sync(), frontendConnection, TIMEOUT_MILLIS);
        cache.put("beforeReconnect", "value2");

        awaitTrue(() -> "value2".equals(cache.get("beforeReconnect").get()), "updated value read back", 3000);
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
        awaitTrue(condition, description, TIMEOUT_MILLIS);
    }

    private static void awaitTrue(BooleanSupplier condition, String description, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                Assertions.fail("Timed out waiting for " + description);
            }
            Thread.sleep(200);
        }
    }
}
