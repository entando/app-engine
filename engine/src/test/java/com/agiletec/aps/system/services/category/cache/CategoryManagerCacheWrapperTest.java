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
package com.agiletec.aps.system.services.category.cache;

import com.agiletec.aps.system.common.AbstractGenericCacheWrapperTest;
import com.agiletec.aps.system.common.AbstractGenericCacheWrapperTest.GatedStore;
import com.agiletec.aps.system.services.category.Category;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.entando.entando.aps.system.services.cache.CustomConcurrentMapCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class CategoryManagerCacheWrapperTest {

    private static final String ROOT = "home";
    private static final String ROOT_KEY = ICategoryManagerCacheWrapper.CATEGORY_CACHE_NAME_PREFIX + ROOT;

    private GatedStore store;
    private Cache cache;
    private CategoryManagerCacheWrapper wrapper;

    @Test
    void concurrentAddsUnderTheSameParentKeepBothChildren() throws Exception {
        this.setUp(ROOT_KEY);

        AbstractGenericCacheWrapperTest.race(store,
                () -> wrapper.addCategory(category("a")),
                () -> wrapper.addCategory(category("b")));

        Assertions.assertEquals(Set.of("x", "a", "b"), children(wrapper.getCategory(ROOT)),
                "a category added concurrently was lost from the parent's children");
        Assertions.assertEquals(Set.of("x", "a", "b"), children(wrapper.getRoot()),
                "a category added concurrently was lost from the root's children");
    }

    @Test
    void concurrentAddAndDeleteUnderTheSameParentDoNotResurrectTheDeletedChild() throws Exception {
        this.setUp(ROOT_KEY);

        AbstractGenericCacheWrapperTest.race(store,
                () -> wrapper.addCategory(category("a")),
                () -> wrapper.deleteCategory("x"));

        Assertions.assertEquals(Set.of("a"), children(wrapper.getCategory(ROOT)),
                "a concurrent add restored a child deleted meanwhile");
        Assertions.assertEquals(Set.of("a"), children(wrapper.getRoot()));
    }

    @Test
    void concurrentAddsKeepBothCodesInTheCodesList() throws Exception {
        this.setUp(ICategoryManagerCacheWrapper.CATEGORY_CODES_CACHE_NAME);
        cache.put(ICategoryManagerCacheWrapper.CATEGORY_CODES_CACHE_NAME, new ArrayList<>(List.of(ROOT, "x")));

        AbstractGenericCacheWrapperTest.race(store,
                () -> wrapper.addCategory(category("a")),
                () -> wrapper.addCategory(category("b")));

        Assertions.assertEquals(Set.of(ROOT, "x", "a", "b"), new HashSet<>(codes()),
                "a category added concurrently was lost from the codes list");
    }

    @Test
    void storedObjectsObtainedByAReaderAreNotMutated() {
        this.setUp(ROOT_KEY);
        List<String> seededCodes = new ArrayList<>(List.of(ROOT, "x"));
        cache.put(ICategoryManagerCacheWrapper.CATEGORY_CODES_CACHE_NAME, seededCodes);
        Category storedRoot = (Category) cache.get(ROOT_KEY).get();

        wrapper.addCategory(category("a"));
        wrapper.deleteCategory("x");

        Assertions.assertEquals(List.of(ROOT, "x"), seededCodes);
        Assertions.assertArrayEquals(new String[]{"x"}, storedRoot.getChildrenCodes());
        Assertions.assertEquals(List.of(ROOT, "a"), codes());
        Assertions.assertArrayEquals(new String[]{"a"}, wrapper.getCategory(ROOT).getChildrenCodes());
    }

    @Test
    void addOfAPresentChildDoesNotDuplicateIt() {
        this.setUp(ROOT_KEY);

        wrapper.addCategory(category("x"));

        Assertions.assertArrayEquals(new String[]{"x"}, wrapper.getCategory(ROOT).getChildrenCodes());
    }

    /**
     * Seeds a root with one child {@code x}, and no codes list as {@code initCache} does; the gate pauses on
     * {@code gatedKey}.
     */
    private void setUp(String gatedKey) {
        store = new GatedStore(gatedKey);
        cache = new CustomConcurrentMapCache(ICategoryManagerCacheWrapper.CATEGORY_MANAGER_CACHE_NAME, store, true);
        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Mockito.when(cacheManager.getCache(ICategoryManagerCacheWrapper.CATEGORY_MANAGER_CACHE_NAME)).thenReturn(cache);
        wrapper = new CategoryManagerCacheWrapper();
        wrapper.setSpringCacheManager(cacheManager);

        Category root = category(ROOT);
        root.setChildrenCodes(new String[]{"x"});
        cache.put(ROOT_KEY, root);
        cache.put(ICategoryManagerCacheWrapper.CATEGORY_ROOT_CACHE_NAME, root.clone());
        cache.put(ICategoryManagerCacheWrapper.CATEGORY_CACHE_NAME_PREFIX + "x", category("x"));
    }

    private List<String> codes() {
        return (List<String>) cache.get(ICategoryManagerCacheWrapper.CATEGORY_CODES_CACHE_NAME).get();
    }

    private static Set<String> children(Category category) {
        return new HashSet<>(Arrays.asList(category.getChildrenCodes()));
    }

    private static Category category(String code) {
        Category category = new Category();
        category.setCode(code);
        category.setParentCode(ROOT);
        category.setChildrenCodes(new String[0]);
        return category;
    }
}
