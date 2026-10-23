/*
 * Copyright 2020-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.support.caching.CacheFrontend;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.lang.Nullable;

/**
 * Extension of default RedisCache with Client-side caching support
 * @author E.Santoboni
 */
public class LettuceCache extends RedisCache {
    
    private static final Logger logger = LoggerFactory.getLogger(LettuceCache.class);
    
    private final AtomicReference<Supplier<CacheFrontend<String, Object>>> cacheFrontendSupplier = new AtomicReference<>();

    protected LettuceCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig,
            Supplier<CacheFrontend<String, Object>> cacheFrontendSupplier) {
        super(name, cacheWriter, cacheConfig);
        this.cacheFrontendSupplier.set(cacheFrontendSupplier);
    }

    /**
     * @deprecated use {@link #LettuceCache(String, RedisCacheWriter, RedisCacheConfiguration, Supplier)} instead
     */
    @Deprecated(since = "7.5.4")
	protected LettuceCache(String name, RedisCacheWriter cacheWriter, RedisCacheConfiguration cacheConfig, CacheFrontend<String, Object> cacheFrontend) {
        this(name, cacheWriter, cacheConfig, () -> cacheFrontend);
    }

    /**
     * @deprecated use {@link #LettuceCache(String, RedisCacheWriter, RedisCacheConfiguration, Supplier)} instead
     */
    @Deprecated(since = "7.5.4")
    protected void setFrontendCache(CacheFrontend<String, Object> cacheFrontend) {
        this.cacheFrontendSupplier.set(() -> cacheFrontend);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, @Nullable Class<T> type) {
        Object value = super.fromStoreValue(this.getFromLocalCache(key));
        if (value != null && type != null && !type.isInstance(value)) {
			throw new IllegalStateException(
					"Cached value is not of required type [" + type.getName() + "]: " + value);
		}
		return (T) value;
    }
    
    @Override
    public ValueWrapper get(Object key) {
        Object localCacheValue = this.getFromLocalCache(key);
        return toValueWrapper(localCacheValue);
    }

    private Object getFromLocalCache(Object key) {
        return this.cacheFrontendSupplier.get().get().get(super.getName() + "::" + key.toString());
    }
    
    @Override
    public synchronized <T> T get(Object key, Callable<T> valueLoader) {
        logger.warn("Calling remote cache for key {}", key);
        return super.get(key, valueLoader);
    }

}
