package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.RedisCodec;
import java.util.List;
import java.util.Set;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.plugins.jpredis.utils.RedisClientList;
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
    private RedisClient adminClient;
    private RedisCommands<String, String> admin;
    private LettuceConnectionFactory connectionFactory;
    private CacheFrontendManager cacheFrontendManager;
    private long frontendConnection;
    private Cache cache;

    @BeforeEach
    void setUp(GenericContainer redis) {
        String host = redis.getHost();
        int port = redis.getMappedPort(6379);
        redisClient = Mockito.spy(RedisClient.create(RedisURI.create(host, port)));
        adminClient = RedisClient.create(RedisURI.create(host, port));
        admin = adminClient.connect().sync();
        connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        cacheFrontendManager = new CacheFrontendManager(redisClient);
        frontendConnection = RedisClientList.openedTrackingConnection(admin, this::createCache);
        cache.clear();
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
        cacheFrontendManager.close();
        connectionFactory.destroy();
        redisClient.shutdown();
        adminClient.shutdown();
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

    @Test
    void hitShouldNotBeStaleAfterFrontendReconnect() throws Exception {
        cache.put("key", "value1");
        Assertions.assertEquals("value1", cache.get("key").get());

        reconnectFrontend();
        cache.put("key", "value2");

        Assertions.assertEquals("value2", awaitValue("key", "value2", 3000));
    }

    @Test
    void missShouldNotBeStaleAfterFrontendReconnect() throws Exception {
        cache.put("key", "value1");

        reconnectFrontend();
        Assertions.assertEquals("value1", cache.get("key").get());
        cache.put("key", "value2");

        Assertions.assertEquals("value2", awaitValue("key", "value2", 3000));
    }

    @Test
    void trackingShouldBeRestoredAfterFrontendReconnect() throws Exception {
        Set<Long> trackingBeforeReconnect = RedisClientList.trackingIds(admin);

        reconnectFrontend();

        Assertions.assertTrue(awaitNewTrackingConnection(trackingBeforeReconnect, 3000));
    }

    private void reconnectFrontend() throws InterruptedException {
        RedisClientList.killAndAwaitReconnect(admin, frontendConnection, 3000);
    }

    private boolean awaitNewTrackingConnection(Set<Long> existing, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (existing.containsAll(RedisClientList.trackingIds(admin))) {
            if (System.currentTimeMillis() > deadline) {
                return false;
            }
            Thread.sleep(50);
        }
        return true;
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
