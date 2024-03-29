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
package com.agiletec.aps.system.services.baseconfig.cache;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.AbstractCacheWrapper;
import com.agiletec.aps.system.services.baseconfig.IConfigItemDAO;
import com.agiletec.aps.system.services.baseconfig.SystemParamsUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.springframework.cache.Cache;

/**
 * @author E.Santoboni
 */
public class ConfigManagerCacheWrapper extends AbstractCacheWrapper implements IConfigManagerCacheWrapper {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(ConfigManagerCacheWrapper.class);

    @Override
    public void initCache(IConfigItemDAO configItemDAO, String version) throws EntException {
        try {
            Cache cache = this.getCache();
            Map<String, String> configItems = configItemDAO.loadVersionItems(version);
            String xmlParams = configItems.get(SystemConstants.CONFIG_ITEM_PARAMS);
            Map<String, String> params = SystemParamsUtils.getParams(xmlParams);
            this.insertAndCleanObjectsOnCache(cache, configItems, params);
        } catch (Exception t) {
            logger.error("Error loading configuration params", t);
            throw new EntException("Error loading configuration params", t);
        }
    }
    
    @Override
    public void release() {
        Cache cache = this.getCache();
        this.releaseCachedObjects(cache, CONFIG_ITEMS_CODES_CACHE_NAME, CONFIG_ITEM_CACHE_NAME_PREFIX);
        this.releaseCachedObjects(cache, CONFIG_PARAMS_CODES_CACHE_NAME, CONFIG_PARAM_CACHE_NAME_PREFIX);
    }
    
    protected void releaseCachedObjects(Cache cache, String codesCacheName, String codeCachePrefix) {
        List<String> codes = (List<String>) this.get(cache, codesCacheName, List.class);
        this.releaseObjects(cache, codes, codeCachePrefix);
        if (null != codes) {
            cache.evict(codesCacheName);
        }
    }

    protected void insertAndCleanObjectsOnCache(Cache cache, Map<String, String> configItems, Map<String, String> params) {
        this.insertAndCleanCache(cache, configItems, CONFIG_ITEMS_CODES_CACHE_NAME, CONFIG_ITEM_CACHE_NAME_PREFIX);
        this.insertAndCleanCache(cache, params, CONFIG_PARAMS_CODES_CACHE_NAME, CONFIG_PARAM_CACHE_NAME_PREFIX);
    }
    
    protected void insertAndCleanCache(Cache cache,
            Map<String, String> objects, String codesCacheName, String codeCachePrefix) {
        List<String> oldCodes = this.get(cache, codesCacheName, List.class);
        List<String> codes = new ArrayList<>();
        for (Map.Entry<String, String> entry : objects.entrySet()) {
            cache.put(codeCachePrefix + entry.getKey(), entry.getValue());
            codes.add(entry.getKey());
        }
        cache.put(codesCacheName, codes);
        List<String> keysToRelease = oldCodes == null ? null :
                oldCodes.stream().filter(c -> !objects.containsKey(c)).collect(Collectors.toList());
        this.releaseObjects(cache, keysToRelease, codeCachePrefix);
    }

    private void releaseObjects(Cache cache, List<String> keysToRelease, String prefix) {
        if (null != keysToRelease) {
            for (String code : keysToRelease) {
                cache.evict(prefix + code);
            }
        }
    }
    
    @Override
    public String getConfigItem(String name) {
        return this.get(CONFIG_ITEM_CACHE_NAME_PREFIX + name, String.class);
    }

    @Override
    public String getParam(String name) {
        return this.get(CONFIG_PARAM_CACHE_NAME_PREFIX + name, String.class);
    }

    @Override
    protected String getCacheName() {
        return CONFIG_MANAGER_CACHE_NAME;
    }

}
