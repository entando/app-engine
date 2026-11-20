package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.RedisCodec;
import java.util.List;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.plugins.jpredis.utils.RedisTestExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.GenericContainer;

@ExtendWith(RedisTestExtension.class)
class LettuceCacheIntegrationTest {

    private RedisClient redisClient;
    private LettuceConnectionFactory connectionFactory;
    private CacheFrontendManager cacheFrontendManager;
    private Cache cache;

    @BeforeEach
    void setUp(GenericContainer redis) {
        String host = redis.getHost();
        int port = redis.getMappedPort(6379);
        redisClient = Mockito.spy(RedisClient.create(RedisURI.create(host, port)));
        connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        cacheFrontendManager = new CacheFrontendManager(redisClient);
        ITenantManager tenantManager = Mockito.mock(ITenantManager.class);
        Mockito.when(tenantManager.getCodes()).thenReturn(List.of());
        LettuceCacheManager cacheManager = (LettuceCacheManager) new BaseRedisCacheConfig().cacheManager(
                redisClient, connectionFactory, cacheFrontendManager, tenantManager);
        cacheManager.afterPropertiesSet();
        cache = cacheManager.getCache("Entando_Cache");
        cache.clear();
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
        redisClient.shutdown();
    }

    @Test
    void missShouldReachRedisAfterFailedFrontendRebuild() {
        cache.put("key", "value");
        Assertions.assertEquals("value", cache.get("key").get());

        injectConnectFailure();
        Assertions.assertThrows(RedisConnectionException.class, () -> cacheFrontendManager.rebuildCacheFrontend());

        Assertions.assertDoesNotThrow(() -> cache.get("missingKey"));
    }

    @Test
    void hitShouldNotBeStaleAfterFailedFrontendRebuild() throws Exception {
        cache.put("key", "value1");
        Assertions.assertEquals("value1", cache.get("key").get());

        injectConnectFailure();
        Assertions.assertThrows(RedisConnectionException.class, () -> cacheFrontendManager.rebuildCacheFrontend());
        cache.put("key", "value2");

        Assertions.assertEquals("value2", awaitValue("key", "value2", 3000));
    }

    private void injectConnectFailure() {
        Mockito.doThrow(new RedisConnectionException("injected"))
                .when(redisClient).connect(Mockito.any(RedisCodec.class));
    }

    private Object awaitValue(String key, Object expected, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        Object value = cache.get(key).get();
        while (!expected.equals(value) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
            value = cache.get(key).get();
        }
        return value;
    }
}
