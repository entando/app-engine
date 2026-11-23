package com.agiletec.aps.system.common;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;
import org.entando.entando.aps.system.services.cache.IAtomicUpdateCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleValueWrapper;

@ExtendWith(MockitoExtension.class)
class UnitAbstractCacheWrapperTest {

    private static final String CACHE_NAME = "cache_name";

    @Mock
    private Cache cache;

    private AbstractCacheWrapper cacheWrapper;

    @BeforeEach
    void setUp() {
        cacheWrapper = new AbstractCacheWrapper() {
            @Override
            protected String getCacheName() {
                return CACHE_NAME;
            }
        };
    }

    @Test
    void getCopyFromImmutableCacheMapShouldHandleNullValue() {
        Assertions.assertNull(cacheWrapper.getCopyOfMapFromCache(cache, "not_set"));
    }

    @Test
    void getCopyFromImmutableCacheMapShouldCreateAMutableCopy() {
        Mockito.when(cache.get("key")).thenReturn(new SimpleValueWrapper(Map.of("k1", "v1")));
        Map<String, String> copy = cacheWrapper.getCopyOfMapFromCache(cache, "key");
        copy.put("k2", "v2");
        Assertions.assertEquals(2, copy.size());
    }

    @Test
    void updateCachedValueShouldDelegateToAnAtomicUpdateCache() {
        Cache atomicCache = Mockito.mock(Cache.class, Mockito.withSettings().extraInterfaces(IAtomicUpdateCache.class));
        UnaryOperator<String> updater = v -> "new";
        Mockito.when(((IAtomicUpdateCache) atomicCache).updateAtomically("key", updater)).thenReturn("new");

        Assertions.assertEquals("new", cacheWrapper.updateCachedValue(atomicCache, "key", updater));
        Mockito.verify((IAtomicUpdateCache) atomicCache).updateAtomically("key", updater);
        Mockito.verify(atomicCache, Mockito.never()).put(Mockito.any(), Mockito.any());
    }

    @Test
    void updateCachedValueFallbackShouldWriteOnlyWhenChanged() {
        Mockito.when(cache.getName()).thenReturn(CACHE_NAME);
        Mockito.when(cache.get("key")).thenReturn(new SimpleValueWrapper("old"));

        Assertions.assertEquals("old", cacheWrapper.<String>updateCachedValue(cache, "key", v -> v));
        Mockito.verify(cache, Mockito.never()).put(Mockito.any(), Mockito.any());

        Assertions.assertEquals("new", cacheWrapper.<String>updateCachedValue(cache, "key", v -> "new"));
        Mockito.verify(cache).put("key", "new");
    }

    @Test
    void updateCachedValueFallbackShouldSerializeUpdatesOfTheSameKey() throws Exception {
        Cache plainCache = new ConcurrentMapCache(CACHE_NAME);
        plainCache.put("key", "");
        CountDownLatch firstInside = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        AtomicBoolean secondEnteredWhileFirstInside = new AtomicBoolean();
        AtomicBoolean firstInsideFlag = new AtomicBoolean();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> cacheWrapper.<String>updateCachedValue(plainCache, "key", v -> {
                firstInsideFlag.set(true);
                firstInside.countDown();
                try {
                    releaseFirst.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                firstInsideFlag.set(false);
                return v + "a";
            }));
            Assertions.assertTrue(firstInside.await(5, TimeUnit.SECONDS));
            Future<?> second = executor.submit(() -> cacheWrapper.<String>updateCachedValue(plainCache, "key", v -> {
                secondEnteredWhileFirstInside.set(firstInsideFlag.get());
                return v + "b";
            }));
            Thread.sleep(200);
            releaseFirst.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        Assertions.assertFalse(secondEnteredWhileFirstInside.get());
        Assertions.assertEquals("ab", plainCache.get("key").get());
    }
}
