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
package com.agiletec.aps.system.services.i18n.cache;

import java.util.Map;

import java.util.Optional;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.springframework.cache.Cache;

import com.agiletec.aps.system.common.AbstractGenericCacheWrapper;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.i18n.II18nDAO;
import com.agiletec.aps.util.ApsProperties;

public class I18nManagerCacheWrapper extends AbstractGenericCacheWrapper<ApsProperties> implements II18nManagerCacheWrapper {

    private final EntLogger logger = EntLogFactory.getSanitizedLogger(getClass());

    @Override
    protected String getCacheName() {
        return I18N_MANAGER_CACHE_NAME;
    }

    @Override
    public void initCache(II18nDAO i18nDAO) throws EntException {
        try {
            Cache cache = this.getCache();
            Map<String, ApsProperties> labels = i18nDAO.loadLabelGroups();
            this.insertAndCleanCache(cache, labels);
        } catch (Throwable t) {
            logger.error("Error loading labels", t);
            throw new EntException("Error loading labels", t);
        }
    }

    @Override
    public Map<String, ApsProperties> getLabelGroups() {
        return super.getObjectMap();
    }

    @Override
    public ApsProperties getLabelGroup(String key) {
        return Optional.ofNullable(super.get(this.getCacheKeyPrefix() + key, ApsProperties.class))
                .map(ApsProperties::clone)
                .orElse(null);
    }
    
    @Override
    public void addLabelGroup(String key, ApsProperties labels) {
        this.manage(key, labels, Action.ADD);
    }

    @Override
    public void updateLabelGroup(String key, ApsProperties labels) {
        this.manage(key, labels, Action.UPDATE);
    }

    @Override
    public void removeLabelGroup(String key) {
        this.manage(key, new ApsProperties(), Action.DELETE);
    }

    @Override
    protected String getCodesCacheKey() {
        return I18N_CODES_CACHE_NAME;
    }

    @Override
    protected String getCacheKeyPrefix() {
        return I18N_CACHE_NAME_PREFIX;
    }

}
