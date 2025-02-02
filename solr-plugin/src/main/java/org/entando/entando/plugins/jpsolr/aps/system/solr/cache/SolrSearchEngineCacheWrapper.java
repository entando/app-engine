/*
 * Copyright 2023-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpsolr.aps.system.solr.cache;

import com.agiletec.aps.system.common.AbstractCacheWrapper;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.LastReloadInfo;
import java.util.Optional;
import org.entando.entando.plugins.jpsolr.aps.system.solr.SolrLastReloadInfo;
import org.springframework.stereotype.Component;

@Component("jpsolrSolrSearchEngineCacheWrapper")
public class SolrSearchEngineCacheWrapper extends AbstractCacheWrapper implements ISolrSearchEngineCacheWrapper {
    
    @Override
    public void release() {
        super.getCache().clear();
    }
    
    @Override
    protected String getCacheName() {
        return SOLR_SE_MANAGER_CACHE_NAME;
    }

    @Override
    public void setLastReloadInfo(LastReloadInfo lastReloadInfo) {
        this.getCache().put(LAST_RELOAD_CACHE_PARAM_NAME, lastReloadInfo);
    }

    @Override
    public LastReloadInfo getLastReloadInfo() {
        return this.get(LAST_RELOAD_CACHE_PARAM_NAME, SolrLastReloadInfo.class);
    }
    
    @Override
    public void markContentTypeStatusSchema(String typeCode) {
        String entryKey = CONTENT_TYPE_STATUS_CACHE_ENTRY_PREFIX + typeCode;
        this.getCache().put(entryKey, "true");
    }

    @Override
    public boolean isTypeMarked(String typeCode) {
        String entryKey = CONTENT_TYPE_STATUS_CACHE_ENTRY_PREFIX + typeCode;
        return Optional.ofNullable(this.get(entryKey, String.class)).isPresent();
    }
    
}
