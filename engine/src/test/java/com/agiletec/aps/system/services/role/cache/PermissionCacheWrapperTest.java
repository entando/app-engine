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
package com.agiletec.aps.system.services.role.cache;

import com.agiletec.aps.system.common.AbstractGenericCacheWrapperTest;
import com.agiletec.aps.system.common.AbstractGenericCacheWrapperTest.GatedStore;
import com.agiletec.aps.system.services.role.IPermissionDAO;
import com.agiletec.aps.system.services.role.IRoleManager;
import com.agiletec.aps.system.services.role.Permission;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.entando.entando.aps.system.services.cache.CustomConcurrentMapCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.CacheManager;

class PermissionCacheWrapperTest {

    private GatedStore store;
    private PermissionCacheWrapper wrapper;

    @BeforeEach
    void setUp() throws Exception {
        store = new GatedStore(IPermissionCacheWrapper.PERMISSION_CODES_CACHE_NAME);
        CustomConcurrentMapCache cache = new CustomConcurrentMapCache(IRoleManager.ROLE_MANAGER_CACHE_NAME, store, true);
        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        Mockito.when(cacheManager.getCache(IRoleManager.ROLE_MANAGER_CACHE_NAME)).thenReturn(cache);
        wrapper = new PermissionCacheWrapper();
        wrapper.setSpringCacheManager(cacheManager);
        IPermissionDAO dao = Mockito.mock(IPermissionDAO.class);
        Mockito.when(dao.loadPermissions()).thenReturn(new HashMap<>());
        wrapper.initCache(dao);
    }

    @Test
    void concurrentAddPermissionKeepsBothCodes() throws Exception {
        AbstractGenericCacheWrapperTest.race(store,
                () -> wrapper.addPermission(permission("perm_a")),
                () -> wrapper.addPermission(permission("perm_b")));

        Assertions.assertEquals(Set.of("perm_a", "perm_b"), new HashSet<>(wrapper.getPermissionsCodes()),
                "a permission added concurrently was lost from the permission codes list");
    }

    private static Permission permission(String name) {
        Permission permission = new Permission();
        permission.setName(name);
        permission.setDescription(name);
        return permission;
    }
}
