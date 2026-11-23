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
package org.entando.entando.aps.system.services.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.serializer.support.SerializationDelegate;

class CustomConcurrentMapCacheTest {

    @Test
    void returningTheSameInstanceWritesNothing() {
        ConcurrentMap<Object, Object> store = new ConcurrentHashMap<>();
        CustomConcurrentMapCache cache = new CustomConcurrentMapCache("test", store, true);
        List<String> stored = new ArrayList<>(List.of("a"));
        cache.put("key", stored);

        List<String> result = cache.updateAtomically("key", v -> v);

        Assertions.assertSame(stored, result);
        Assertions.assertSame(stored, store.get("key"));
    }

    @Test
    void returningTheSameInstanceWritesNothingWhenStoringByValue() {
        ConcurrentMap<Object, Object> store = new ConcurrentHashMap<>();
        CustomConcurrentMapCache cache = new CustomConcurrentMapCache("test", store, true,
                new SerializationDelegate(getClass().getClassLoader()));
        cache.put("key", new ArrayList<>(List.of("a")));
        Object storedBytes = store.get("key");

        List<String> result = cache.updateAtomically("key", v -> v);

        Assertions.assertEquals(List.of("a"), result);
        Assertions.assertSame(storedBytes, store.get("key"));
    }

    @Test
    void updaterReceivesTheStoredValueAndItsResultIsStored() {
        CustomConcurrentMapCache cache = new CustomConcurrentMapCache("test");
        cache.put("key", List.of("a"));

        List<String> result = cache.<List<String>>updateAtomically("key", v -> {
            List<String> copy = new ArrayList<>(v);
            copy.add("b");
            return copy;
        });

        Assertions.assertEquals(List.of("a", "b"), result);
        Assertions.assertEquals(List.of("a", "b"), cache.get("key").get());
    }

    @Test
    void absentKeyIsPassedAsNullAndStaysAbsentWhenUnchanged() {
        ConcurrentMap<Object, Object> store = new ConcurrentHashMap<>();
        CustomConcurrentMapCache cache = new CustomConcurrentMapCache("test", store, true);

        Object result = cache.updateAtomically("key", v -> {
            Assertions.assertNull(v);
            return v;
        });

        Assertions.assertNull(result);
        Assertions.assertFalse(store.containsKey("key"));
    }

    @Test
    void nullValuesRoundTrip() {
        CustomConcurrentMapCache cache = new CustomConcurrentMapCache("test", true);
        cache.put("key", null);

        String result = cache.<String>updateAtomically("key", v -> {
            Assertions.assertNull(v);
            return "x";
        });
        Assertions.assertEquals("x", result);
        Assertions.assertEquals("x", cache.get("key").get());

        Assertions.assertNull(cache.<String>updateAtomically("key", v -> null));
        Assertions.assertNotNull(cache.get("key"));
        Assertions.assertNull(cache.get("key").get());
    }
}
