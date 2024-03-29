/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * @author E.Santoboni
 */
public abstract class AbstractCacheWrapper implements ICacheWrapper {

    protected static enum Action {
        ADD,
        UPDATE,
        DELETE
    }

    private CacheManager springCacheManager;

    protected CacheManager getSpringCacheManager() {
        return springCacheManager;
    }
    @Autowired
    public void setSpringCacheManager(CacheManager springCacheManager) {
        this.springCacheManager = springCacheManager;
    }
    
    @Override
    public void release() {
        // nothing to do
    }

    protected abstract String getCacheName();

    protected <T> T get(String name, Class<T> requiredType) {
        return this.get(this.getCache(), name, requiredType);
    }

    protected <T> T get(Cache cache, String name, Class<T> requiredType) {
        Object value = cache.get(name);
        if (value instanceof Cache.ValueWrapper) {
            value = ((Cache.ValueWrapper) value).get();
        }
        return (T) value;
    }

    protected <T> List<T> getCopyOfListFromCache(Cache cache, String name) {
        List<T> value = this.get(cache, name, List.class);
        if (value == null) {
            return null;
        }
        return new ArrayList<>(value);
    }

    protected <K, V> Map<K, V> getCopyOfMapFromCache(Cache cache, String name) {
        Map<K, V> value = this.get(cache, name, Map.class);
        if (value == null) {
            return null;
        }
        return new HashMap<>(value);
    }

    protected <K, V> Map<K, V> getCopyOfMapFromCache(String name) {
        return this.getCopyOfMapFromCache(this.getCache(), name);
    }

    protected Cache getCache() {
        return this.getSpringCacheManager().getCache(this.getCacheName());
    }


}
