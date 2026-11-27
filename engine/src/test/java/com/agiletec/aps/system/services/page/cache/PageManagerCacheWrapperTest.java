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
package com.agiletec.aps.system.services.page.cache;

import com.agiletec.aps.system.common.AbstractGenericCacheWrapperTest;
import com.agiletec.aps.system.common.AbstractGenericCacheWrapperTest.GatedStore;
import com.agiletec.aps.system.services.page.Page;
import com.agiletec.aps.system.services.page.PagesStatus;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.entando.entando.aps.system.services.cache.CustomConcurrentMapCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class PageManagerCacheWrapperTest {

    private static final String ROOT = "homepage";

    private GatedStore store;
    private Cache cache;
    private PageManagerCacheWrapper wrapper;

    @Test
    void concurrentAddDraftPagesKeepBothCodes() throws Exception {
        this.setUp(IPageManagerCacheWrapper.DRAFT_PAGE_CODES_CACHE_NAME);

        AbstractGenericCacheWrapperTest.race(store,
                () -> wrapper.addDraftPage(page("a")),
                () -> wrapper.addDraftPage(page("b")));

        Assertions.assertEquals(Set.of(ROOT, "x", "a", "b"),
                new HashSet<>(codes(IPageManagerCacheWrapper.DRAFT_PAGE_CODES_CACHE_NAME)),
                "a page added concurrently was lost from the draft codes list");
    }

    @Test
    void concurrentAddAndDeleteDraftPageDoNotResurrectTheDeletedCode() throws Exception {
        this.setUp(IPageManagerCacheWrapper.DRAFT_PAGE_CODES_CACHE_NAME);

        AbstractGenericCacheWrapperTest.race(store,
                () -> wrapper.addDraftPage(page("a")),
                () -> wrapper.deleteDraftPage("x"));

        Assertions.assertEquals(Set.of(ROOT, "a"),
                new HashSet<>(codes(IPageManagerCacheWrapper.DRAFT_PAGE_CODES_CACHE_NAME)),
                "a concurrent add restored a page deleted meanwhile");
    }

    @Test
    void concurrentSetPageOnlineAndOfflineKeepTheOnlineCodesConsistent() throws Exception {
        this.setUp(IPageManagerCacheWrapper.ONLINE_PAGE_CODES_CACHE_NAME);
        wrapper.addDraftPage(page("a"));

        AbstractGenericCacheWrapperTest.race(store,
                () -> wrapper.setPageOnline("a"),
                () -> wrapper.setPageOffline("x"));

        Assertions.assertEquals(Set.of(ROOT, "a"),
                new HashSet<>(codes(IPageManagerCacheWrapper.ONLINE_PAGE_CODES_CACHE_NAME)),
                "a concurrent publication restored a page unpublished meanwhile");
    }

    @Test
    void codesListObtainedByAReaderIsNotMutated() {
        this.setUp(IPageManagerCacheWrapper.DRAFT_PAGE_CODES_CACHE_NAME);
        List<String> read = codes(IPageManagerCacheWrapper.DRAFT_PAGE_CODES_CACHE_NAME);

        wrapper.addDraftPage(page("a"));
        wrapper.deleteDraftPage("x");

        Assertions.assertEquals(List.of(ROOT, "x"), read);
        Assertions.assertEquals(List.of(ROOT, "a"), codes(IPageManagerCacheWrapper.DRAFT_PAGE_CODES_CACHE_NAME));
    }

    @Test
    void absentCodesListsAreLeftAbsent() {
        this.setUp(IPageManagerCacheWrapper.DRAFT_PAGE_CODES_CACHE_NAME);
        cache.evict(IPageManagerCacheWrapper.DRAFT_PAGE_CODES_CACHE_NAME);
        cache.evict(IPageManagerCacheWrapper.ONLINE_PAGE_CODES_CACHE_NAME);

        wrapper.addDraftPage(page("a"));
        wrapper.setPageOffline("x");
        wrapper.deleteDraftPage("a");

        Assertions.assertNull(cache.get(IPageManagerCacheWrapper.DRAFT_PAGE_CODES_CACHE_NAME));
        Assertions.assertNull(cache.get(IPageManagerCacheWrapper.ONLINE_PAGE_CODES_CACHE_NAME));
    }

    /**
     * Seeds a published root with one published child {@code x}; the gate pauses on {@code gatedKey}.
     */
    private void setUp(String gatedKey) {
        store = new GatedStore(gatedKey);
        cache = new CustomConcurrentMapCache(IPageManagerCacheWrapper.PAGE_MANAGER_CACHE_NAME, store, true);
        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Mockito.when(cacheManager.getCache(IPageManagerCacheWrapper.PAGE_MANAGER_CACHE_NAME)).thenReturn(cache);
        wrapper = new PageManagerCacheWrapper();
        wrapper.setSpringCacheManager(cacheManager);

        Page root = page(ROOT);
        root.setParentCode(ROOT);
        root.setChildrenCodes(new String[]{"x"});
        root.setOnline(true);
        Page x = page("x");
        x.setOnline(true);
        for (String prefix : List.of(IPageManagerCacheWrapper.DRAFT_PAGE_CACHE_NAME_PREFIX,
                IPageManagerCacheWrapper.ONLINE_PAGE_CACHE_NAME_PREFIX)) {
            cache.put(prefix + ROOT, root.clone());
            cache.put(prefix + "x", x.clone());
        }
        cache.put(IPageManagerCacheWrapper.DRAFT_ROOT_CACHE_NAME, root.clone());
        cache.put(IPageManagerCacheWrapper.ONLINE_ROOT_CACHE_NAME, root.clone());
        cache.put(IPageManagerCacheWrapper.DRAFT_PAGE_CODES_CACHE_NAME, new ArrayList<>(List.of(ROOT, "x")));
        cache.put(IPageManagerCacheWrapper.ONLINE_PAGE_CODES_CACHE_NAME, new ArrayList<>(List.of(ROOT, "x")));
        PagesStatus status = new PagesStatus();
        status.setOnline(2);
        cache.put(IPageManagerCacheWrapper.PAGE_STATUS_CACHE_NAME, status);
    }

    private List<String> codes(String key) {
        return (List<String>) cache.get(key).get();
    }

    private static Page page(String code) {
        Page page = new Page();
        page.setCode(code);
        page.setParentCode(ROOT);
        page.setChildrenCodes(new String[0]);
        return page;
    }
}
