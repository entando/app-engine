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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.entando.entando.aps.system.exception.CacheItemNotFoundException;
import org.springframework.cache.Cache;

/**
 * @author E.Santoboni
 * @param <O> The object to manage
 */
public abstract class AbstractGenericCacheWrapper<O> extends AbstractCacheWrapper {

    protected static enum Action {
        ADD, UPDATE, DELETE
    }
    
    @Override
    public void release() {
        Cache cache = this.getCache();
        this.releaseCachedObjects(cache);
    }

    protected void releaseCachedObjects(Cache cache) {
        List<String> codes = (List<String>) this.get(cache, this.getCodesCacheKey(), List.class);
        this.releaseObjects(cache, codes, this.getCacheKeyPrefix());
        if (null != codes) {
            cache.evict(this.getCodesCacheKey());
        }
    }

    protected void insertObjectsOnCache(Cache cache, Map<String, O> objects) {
        List<String> codes = new ArrayList<>();
        Iterator<String> iter = objects.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            cache.put(this.getCacheKeyPrefix() + key, objects.get(key));
            codes.add(key);
        }
        cache.put(this.getCodesCacheKey(), codes);
    }

    protected void insertAndCleanCache(Cache cache, Map<String, O> objects) {
        this.insertAndCleanCache(cache, objects, this.getCodesCacheKey(), this.getCacheKeyPrefix());
    }

    protected void insertAndCleanCache(Cache cache, Map<String, O> objects, String codesCacheKey, String cacheKeyPrefix) {
        List<String> oldCodes = this.get(cache, codesCacheKey, List.class);
        List<String> codes = new ArrayList<>();
        for (Map.Entry<String, O> entry: objects.entrySet()) {
            cache.put(cacheKeyPrefix + entry.getKey(), entry.getValue());
            codes.add(entry.getKey());
        }
        cache.put(codesCacheKey, codes);
        List<String> keysToRelease = oldCodes == null ? null :
                oldCodes.stream().filter(c -> !objects.containsKey(c)).collect(Collectors.toList());
        this.releaseObjects(cache, keysToRelease, cacheKeyPrefix);
    }

    private void releaseObjects(Cache cache, List<String> keysToRelease, String cacheKeyPrefix) {
        if (null != keysToRelease) {
            for (String code : keysToRelease) {
                cache.evict(cacheKeyPrefix + code);
            }
        }
    }

    protected <O> Map<String, O> getObjectMap() {
        Map<String, O> map = new HashMap<>();
        Cache cache = this.getCache();
        List<String> codes = (List<String>) this.get(cache, this.getCodesCacheKey(), List.class);
        if (null != codes) {
            for (String code : codes) {
                O value = (O) this.get(cache, this.getCacheKeyPrefix() + code, Object.class);
                // Objects in cache and their codes in cache are not properly synchronized so in case of
                // unlucky timing it might happen that the object for a given code is null.
                // Following check mitigates the consequences of that concurrency issue.
                if (value != null) {
                    map.put(code, value);
                }
            }
        }
        return map;
    }

    protected void add(String key, O object) {
        this.manage(key, object, Action.ADD);
    }

    protected void update(String key, O object) {
        this.manage(key, object, Action.UPDATE);
    }

    protected void remove(String key, O object) {
        this.manage(key, object, Action.DELETE);
    }

    protected <O> void manage(String key, O object, Action operation) {
        if (null == object) {
            return;
        }
        Cache cache = this.getCache();
        List<String> codes = this.getCopyOfListFromCache(cache, this.getCodesCacheKey());
        if (Action.ADD.equals(operation)) {
            if (!codes.contains(key)) {
                codes.add(key);
                cache.put(this.getCodesCacheKey(), codes);
            }
            cache.put(this.getCacheKeyPrefix() + key, object);
        } else if (Action.UPDATE.equals(operation)) {
            if (!codes.contains(key)) {
                throw new CacheItemNotFoundException(key, cache.getName());
            }
            cache.put(this.getCacheKeyPrefix() + key, object);
        } else if (Action.DELETE.equals(operation)) {
            codes.remove(key);
            cache.evict(this.getCacheKeyPrefix() + key);
            cache.put(this.getCodesCacheKey(), codes);
        }
    }

    protected abstract String getCodesCacheKey();

    protected abstract String getCacheKeyPrefix();

}
