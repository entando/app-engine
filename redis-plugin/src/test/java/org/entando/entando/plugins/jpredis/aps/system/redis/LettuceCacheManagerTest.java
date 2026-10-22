package org.entando.entando.plugins.jpredis.aps.system.redis;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import io.lettuce.core.support.caching.CacheFrontend;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

@ExtendWith(MockitoExtension.class)
class LettuceCacheManagerTest {

    @Mock
    private RedisCacheWriter redisCacheWriter;
    @Mock
    private RedisCacheConfiguration redisCacheConfiguration;
    @Mock
    private CacheFrontend<String, Object> cacheFrontend;

    private LettuceCacheManager lettuceCacheManager;

    @BeforeEach
    @SuppressWarnings("deprecation")
    void setUp() {
        lettuceCacheManager = new LettuceCacheManager(redisCacheWriter, redisCacheConfiguration,
                new HashMap<>(), false, cacheFrontend);
    }

    @Test
    @SuppressWarnings("deprecation")
    void shouldUpdateCacheFrontend() {
        Mockito.when(cacheFrontend.get("test::testKey1")).thenReturn("testValue1");
        RedisCache cache1 = lettuceCacheManager.createRedisCache("test", null);
        Assertions.assertEquals("testValue1", cache1.get("testKey1").get());

        CacheFrontend<String, Object> cacheFrontend = Mockito.mock(CacheFrontend.class);
        Mockito.when(cacheFrontend.get("test::testKey1")).thenReturn(null);
        Mockito.when(cacheFrontend.get("test::testKey2")).thenReturn("testValue2");
        lettuceCacheManager.updateCacheFrontend(cacheFrontend);
        RedisCache cache2 = lettuceCacheManager.createRedisCache("test", null);

        Assertions.assertNull(cache2.get("testKey1"));
        Assertions.assertEquals("testValue2", cache2.get("testKey2").get());
    }

    @Test
    void existingCacheShouldReadNewFrontend() {
        AtomicReference<CacheFrontend<String, Object>> current = new AtomicReference<>(cacheFrontend);
        LettuceCacheManager supplierCacheManager = new LettuceCacheManager(redisCacheWriter,
                RedisCacheConfiguration.defaultCacheConfig(), Map.of("test", RedisCacheConfiguration.defaultCacheConfig()),
                false, current::get);
        supplierCacheManager.afterPropertiesSet();
        Cache cache = supplierCacheManager.getCache("test");
        Mockito.when(cacheFrontend.get("test::testKey")).thenReturn("testValue1");
        Assertions.assertEquals("testValue1", cache.get("testKey").get());

        CacheFrontend<String, Object> nextFrontend = Mockito.mock(CacheFrontend.class);
        Mockito.when(nextFrontend.get("test::testKey")).thenReturn("testValue2");
        current.set(nextFrontend);

        Assertions.assertEquals("testValue2", cache.get("testKey").get());
    }

    @Test
    void existingTenantCacheShouldReadNewFrontend() {
        AtomicReference<CacheFrontend<String, Object>> current = new AtomicReference<>(cacheFrontend);
        LettuceCacheManager supplierCacheManager = new LettuceCacheManager(redisCacheWriter,
                RedisCacheConfiguration.defaultCacheConfig(),
                Map.of("tenant1_test", RedisCacheConfiguration.defaultCacheConfig()), false, current::get);
        supplierCacheManager.afterPropertiesSet();
        CacheFrontend<String, Object> nextFrontend = Mockito.mock(CacheFrontend.class);
        Mockito.when(nextFrontend.get("tenant1_test::testKey")).thenReturn("testValue2");
        ApsTenantApplicationUtils.setTenant("tenant1");
        try {
            Cache cache = supplierCacheManager.getCache("test");
            current.set(nextFrontend);
            Assertions.assertEquals("testValue2", cache.get("testKey").get());
        } finally {
            ApsTenantApplicationUtils.removeTenant();
        }
        Mockito.verifyNoInteractions(cacheFrontend);
    }
}
