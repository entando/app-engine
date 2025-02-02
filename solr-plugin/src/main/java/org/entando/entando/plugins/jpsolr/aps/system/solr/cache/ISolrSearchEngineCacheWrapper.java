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

import com.agiletec.aps.system.common.ICacheWrapper;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.LastReloadInfo;

public interface ISolrSearchEngineCacheWrapper extends ICacheWrapper {

    public static final String LAST_RELOAD_CACHE_PARAM_NAME = "SolrSearchEngine_lastReloadInfo";
    
    public static final String SOLR_SE_MANAGER_CACHE_NAME = "Entando_SolrSEManager";
	public static final String CONTENT_TYPE_STATUS_CACHE_ENTRY_PREFIX = "SolrSEManager_type_";
    
    public void setLastReloadInfo(LastReloadInfo lastReloadInfo);
    
    public LastReloadInfo getLastReloadInfo();
    
    public void markContentTypeStatusSchema(String typeCode);
    
    public boolean isTypeMarked(String typeCode);

}
