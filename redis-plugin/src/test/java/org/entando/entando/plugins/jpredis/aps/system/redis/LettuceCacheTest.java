package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.RedisCommandInterruptedException;
import io.lettuce.core.RedisException;
import io.lettuce.core.support.caching.CacheFrontend;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
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
    @SuppressWarnings("deprecation")
    void cacheShouldReadThroughFrontendSetByLegacySetter() {
        CacheFrontend<String, Object> nextFrontend = Mockito.mock(CacheFrontend.class);
        Mockito.when(nextFrontend.get("CacheName::key")).thenReturn("value");

        lettuceCache.setFrontendCache(nextFrontend);

        Assertions.assertEquals("value", lettuceCache.get("key").get());
        Mockito.verifyNoInteractions(cacheFrontend);
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

    @Test
    void readShouldBeRepeatedOnFrontendThatReplacedClosedOne() {
        CacheFrontend<String, Object> nextFrontend = Mockito.mock(CacheFrontend.class);
        LettuceCache cache = new LettuceCache("CacheName", cacheWriter, cacheConfig,
                frontends(cacheFrontend, nextFrontend));
        Mockito.when(cacheFrontend.get("CacheName::key")).thenThrow(new RedisException("Connection closed"));
        Mockito.when(nextFrontend.get("CacheName::key")).thenReturn("value2");

        Assertions.assertEquals("value2", cache.get("key").get());
        Mockito.verify(cacheFrontend).get("CacheName::key");
        Mockito.verify(nextFrontend).get("CacheName::key");
    }

    @Test
    void readShouldBeRepeatedAtMostOnce() {
        CacheFrontend<String, Object> nextFrontend = Mockito.mock(CacheFrontend.class);
        CacheFrontend<String, Object> lastFrontend = Mockito.mock(CacheFrontend.class);
        LettuceCache cache = new LettuceCache("CacheName", cacheWriter, cacheConfig,
                frontends(cacheFrontend, nextFrontend, lastFrontend));
        RedisException firstFailure = new RedisException("Connection closed");
        RedisException repeatedFailure = new RedisException("Connection closed");
        Mockito.when(cacheFrontend.get("CacheName::key")).thenThrow(firstFailure);
        Mockito.when(nextFrontend.get("CacheName::key")).thenThrow(repeatedFailure);

        RedisException thrown = Assertions.assertThrows(RedisException.class, () -> cache.get("key"));

        Assertions.assertSame(repeatedFailure, thrown);
        Assertions.assertArrayEquals(new Throwable[]{firstFailure}, thrown.getSuppressed());
        Mockito.verifyNoInteractions(lastFrontend);
    }

    @Test
    void interruptedReadShouldNotBeRepeated() {
        CacheFrontend<String, Object> nextFrontend = Mockito.mock(CacheFrontend.class);
        LettuceCache cache = new LettuceCache("CacheName", cacheWriter, cacheConfig,
                frontends(cacheFrontend, nextFrontend));
        RedisCommandInterruptedException interrupted =
                new RedisCommandInterruptedException(new InterruptedException());
        Mockito.when(cacheFrontend.get("CacheName::key")).thenThrow(interrupted);

        Assertions.assertSame(interrupted,
                Assertions.assertThrows(RedisCommandInterruptedException.class, () -> cache.get("key")));
        Mockito.verifyNoInteractions(nextFrontend);
    }

    @SafeVarargs
    private static Supplier<CacheFrontend<String, Object>> frontends(CacheFrontend<String, Object>... sequence) {
        AtomicInteger calls = new AtomicInteger();
        return () -> sequence[Math.min(calls.getAndIncrement(), sequence.length - 1)];
    }
}
