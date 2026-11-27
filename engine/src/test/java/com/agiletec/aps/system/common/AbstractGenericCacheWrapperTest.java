/*
 * Copyright 2026-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package com.agiletec.aps.system.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import org.entando.entando.aps.system.exception.CacheItemNotFoundException;
import org.entando.entando.aps.system.services.cache.CustomConcurrentMapCache;
import org.entando.entando.aps.system.services.cache.IAtomicUpdateCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

public class AbstractGenericCacheWrapperTest {

    private static final String CACHE_NAME = "TestCache";
    private static final String CODES_KEY = "codes";
    private static final String PREFIX = "obj_";

    private GatedStore store;
    private Cache cache;
    private TestCacheWrapper wrapper;

    @BeforeEach
    void setUp() {
        store = new GatedStore(CODES_KEY);
        cache = new CustomConcurrentMapCache(CACHE_NAME, store, true);
        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Mockito.when(cacheManager.getCache(CACHE_NAME)).thenReturn(cache);
        wrapper = new TestCacheWrapper();
        wrapper.setSpringCacheManager(cacheManager);
    }

    @Test
    void concurrentAddsOfDifferentCodesKeepBothCodes() throws Exception {
        cache.put(CODES_KEY, new ArrayList<>());

        boolean secondCompletedInsideWindow = race(store,
                () -> wrapper.add("a", "object-a"),
                () -> wrapper.add("b", "object-b"));

        Assertions.assertEquals(Set.of("a", "b"), new HashSet<>(codes()),
                "a code added concurrently was lost from the codes list");
        Assertions.assertEquals("object-a", cache.get(PREFIX + "a").get());
        Assertions.assertEquals("object-b", cache.get(PREFIX + "b").get());
        Assertions.assertFalse(secondCompletedInsideWindow,
                "writer 2 completed inside writer 1's read-write window of the codes list");
    }

    @Test
    void concurrentAddAndDeleteDoNotResurrectTheDeletedCode() throws Exception {
        List<String> seed = new ArrayList<>();
        seed.add("x");
        cache.put(CODES_KEY, seed);
        cache.put(PREFIX + "x", "object-x");

        race(store,
                () -> wrapper.add("a", "object-a"),
                () -> wrapper.remove("x", "object-x"));

        Assertions.assertEquals(List.of("a"), codes(),
                "a concurrent add restored a code deleted meanwhile");
        Assertions.assertNull(cache.get(PREFIX + "x"));
    }

    @Test
    void manyThreadsAddingDistinctCodesKeepAllCodes() throws Exception {
        cache.put(CODES_KEY, new ArrayList<>());
        int threads = 16;
        int perThread = 50;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                int thread = t;
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        String code = "c" + thread + "_" + i;
                        wrapper.add(code, code);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
        Assertions.assertEquals(threads * perThread, new HashSet<>(codes()).size(),
                "codes added concurrently were lost from the codes list");
    }

    @Test
    void addWritesTheObjectBeforeTheCode() {
        cache.put(CODES_KEY, new ArrayList<>());
        Cache spy = useSpyCache();

        wrapper.add("a", "object-a");

        InOrder inOrder = Mockito.inOrder(spy);
        inOrder.verify(spy).put(PREFIX + "a", "object-a");
        inOrder.verify((IAtomicUpdateCache) spy).updateAtomically(Mockito.eq(CODES_KEY), Mockito.any());
        Assertions.assertEquals(List.of("a"), codes());
    }

    @Test
    void addOfAPresentCodeDoesNotDuplicateIt() {
        cache.put(CODES_KEY, new ArrayList<>(List.of("a")));

        wrapper.add("a", "object-a2");
        wrapper.add("a", "object-a3");

        Assertions.assertEquals(List.of("a"), codes());
        Assertions.assertEquals("object-a3", cache.get(PREFIX + "a").get());
    }

    @Test
    void deleteRemovesTheCodeBeforeEvictingTheObject() {
        cache.put(CODES_KEY, new ArrayList<>(List.of("a", "b")));
        cache.put(PREFIX + "a", "object-a");
        Cache spy = useSpyCache();

        wrapper.remove("a", "object-a");

        InOrder inOrder = Mockito.inOrder(spy);
        inOrder.verify((IAtomicUpdateCache) spy).updateAtomically(Mockito.eq(CODES_KEY), Mockito.any());
        inOrder.verify(spy).evict(PREFIX + "a");
        Assertions.assertEquals(List.of("b"), codes());
        Assertions.assertNull(cache.get(PREFIX + "a"));
    }

    @Test
    void updateOfAnAbsentCodeThrows() {
        cache.put(CODES_KEY, new ArrayList<>(List.of("a")));

        Assertions.assertThrows(CacheItemNotFoundException.class, () -> wrapper.update("b", "object-b"));
        Assertions.assertNull(cache.get(PREFIX + "b"));
    }

    @Test
    void updateOfAPresentCodeWritesTheObject() {
        List<String> seed = new ArrayList<>(List.of("a"));
        cache.put(CODES_KEY, seed);

        wrapper.update("a", "object-a2");

        Assertions.assertEquals("object-a2", cache.get(PREFIX + "a").get());
        Assertions.assertSame(seed, codes());
    }

    @Test
    void absentCodesListIsLeftAbsent() {
        wrapper.add("a", "object-a");
        Assertions.assertEquals("object-a", cache.get(PREFIX + "a").get());
        Assertions.assertNull(cache.get(CODES_KEY));

        wrapper.remove("a", "object-a");
        Assertions.assertNull(cache.get(PREFIX + "a"));
        Assertions.assertNull(cache.get(CODES_KEY));

        Assertions.assertThrows(CacheItemNotFoundException.class, () -> wrapper.update("a", "object-a"));
    }

    @Test
    void listObtainedByAReaderIsNotMutated() {
        cache.put(CODES_KEY, new ArrayList<>(List.of("x")));
        List<String> read = codes();

        wrapper.add("a", "object-a");
        wrapper.remove("x", "object-x");

        Assertions.assertEquals(List.of("x"), read);
        Assertions.assertEquals(List.of("a"), codes());
    }

    private Cache useSpyCache() {
        Cache spy = Mockito.spy(cache);
        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Mockito.when(cacheManager.getCache(CACHE_NAME)).thenReturn(spy);
        wrapper.setSpringCacheManager(cacheManager);
        return spy;
    }

    private List<String> codes() {
        return (List<String>) cache.get(CODES_KEY).get();
    }

    /**
     * Starts writer 1, pauses it right after it reads the gated key, runs writer 2 meanwhile and waits for both.
     *
     * @return true if writer 2 completed while writer 1 was paused between its read and its write
     */
    public static boolean race(GatedStore store, Runnable first, Runnable second) throws InterruptedException {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread writer1 = new Thread(guarded(first, failure, null), "race-writer-1");
        store.arm(writer1);
        writer1.start();
        Assertions.assertTrue(store.firstRead.await(5, TimeUnit.SECONDS),
                "writer 1 never read the gated key");
        Thread writer2 = new Thread(guarded(second, failure, store.secondDone), "race-writer-2");
        writer2.start();
        writer1.join(10_000);
        writer2.join(10_000);
        Assertions.assertFalse(writer1.isAlive() || writer2.isAlive(), "a writer did not finish");
        if (failure.get() != null) {
            Assertions.fail("a writer failed", failure.get());
        }
        return store.secondDoneInsideWindow;
    }

    private static Runnable guarded(Runnable work, AtomicReference<Throwable> failure, CountDownLatch done) {
        return () -> {
            try {
                work.run();
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            } finally {
                if (done != null) {
                    done.countDown();
                }
            }
        };
    }

    /**
     * Store that pauses the armed thread once, right after it reads the gated key through {@code get} or inside
     * {@code compute}, until writer 2 is done or {@link #GATE_TIMEOUT_MS} expires.
     */
    public static class GatedStore extends ConcurrentHashMap<Object, Object> {

        public static final long GATE_TIMEOUT_MS = 1000;

        private final transient Object gatedKey;
        private final transient AtomicBoolean fired = new AtomicBoolean();
        private final transient CountDownLatch firstRead = new CountDownLatch(1);
        private final transient CountDownLatch secondDone = new CountDownLatch(1);
        private transient volatile Thread armedThread;
        private transient volatile boolean secondDoneInsideWindow;

        public GatedStore(Object gatedKey) {
            this.gatedKey = gatedKey;
        }

        void arm(Thread thread) {
            this.armedThread = thread;
        }

        @Override
        public Object get(Object key) {
            Object value = super.get(key);
            this.pauseIfGated(key);
            return value;
        }

        @Override
        public Object compute(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
            return super.compute(key, (k, v) -> {
                this.pauseIfGated(k);
                return remappingFunction.apply(k, v);
            });
        }

        private void pauseIfGated(Object key) {
            if (Thread.currentThread() != this.armedThread || !this.gatedKey.equals(key)
                    || !this.fired.compareAndSet(false, true)) {
                return;
            }
            this.firstRead.countDown();
            try {
                this.secondDoneInsideWindow = this.secondDone.await(GATE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class TestCacheWrapper extends AbstractGenericCacheWrapper<String> {

        @Override
        protected String getCacheName() {
            return CACHE_NAME;
        }

        @Override
        protected String getCodesCacheKey() {
            return CODES_KEY;
        }

        @Override
        protected String getCacheKeyPrefix() {
            return PREFIX;
        }
    }
}
