package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.support.caching.CacheFrontend;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

@ExtendWith(MockitoExtension.class)
class LettuceCacheTest {

    @Mock
    private RedisCacheWriter cacheWriter;
    @Mock
    private RedisCacheConfiguration cacheConfig;
    @Mock
    private CacheFrontend<String, Object> cacheFrontend;

    private LettuceCache lettuceCache;

    @BeforeEach
    @SuppressWarnings("deprecation")
    void setUp() {
        lettuceCache = new LettuceCache("CacheName", cacheWriter, cacheConfig, cacheFrontend);
    }
    
    @Test
    void cacheShouldHandleNullValues() {
        Object value = lettuceCache.get("null");
        Assertions.assertNull(value);
    }

    @Test
    void cacheShouldHandleValuesThatAreNeitherListNorMaps() {
        Mockito.when(cacheFrontend.get("CacheName::string")).thenReturn("string");
        Object value = lettuceCache.get("string").get();
        Assertions.assertEquals("string", value);
    }

    @Test
    void cacheShouldReadThroughCurrentSupplierFrontend() {
        CacheFrontend<String, Object> nextFrontend = Mockito.mock(CacheFrontend.class);
        AtomicReference<CacheFrontend<String, Object>> current = new AtomicReference<>(cacheFrontend);
        LettuceCache cache = new LettuceCache("CacheName", cacheWriter, cacheConfig, current::get);
        Mockito.when(cacheFrontend.get("CacheName::key")).thenReturn("value1");
        Mockito.when(nextFrontend.get("CacheName::key")).thenReturn("value2");

        Assertions.assertEquals("value1", cache.get("key").get());
        current.set(nextFrontend);
        Assertions.assertEquals("value2", cache.get("key").get());
    }
}
