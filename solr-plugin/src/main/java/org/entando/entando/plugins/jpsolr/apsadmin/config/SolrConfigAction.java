/*
 * Copyright 2021-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpsolr.apsadmin.config;

import com.agiletec.apsadmin.system.BaseAction;
import com.opensymphony.xwork2.Action;
import java.util.List;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.plugins.jpsolr.aps.system.solr.ISolrSearchEngineManager;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.ContentTypeSettings;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author E.Santoboni
 */
public class SolrConfigAction extends BaseAction {
    
    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(SolrConfigAction.class);
    
    private String typeCode;
    
    private int refreshResult = -1;
    
    private ISolrSearchEngineManager solrSearchEngineManager;
    
    public List<ContentTypeSettings> getContentTypesSettings() {
        try {
            return this.getSolrSearchEngineManager().getContentTypesSettings();
        } catch (Exception e) {
            logger.error("Error extracting config", e);
            throw new EntRuntimeException("Error extracting config", e);
        }
    }
    
    public String refreshContentType() {
        try {
            this.getSolrSearchEngineManager().refreshContentType(this.getTypeCode());
            this.setRefreshResult(1);
        } catch (Exception e) {
            this.setRefreshResult(0);
            logger.error("Error refreshing content type " + this.getTypeCode(), e);
            return FAILURE;
        }
        return Action.SUCCESS;
    }

    public String getTypeCode() {
        return typeCode;
    }
    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public int getRefreshResult() {
        return refreshResult;
    }
    public void setRefreshResult(int refreshResult) {
        this.refreshResult = refreshResult;
    }
    
    protected ISolrSearchEngineManager getSolrSearchEngineManager() {
        return solrSearchEngineManager;
    }
    @Autowired
    public void setSolrSearchEngineManager(ISolrSearchEngineManager solrSearchEngineManager) {
        this.solrSearchEngineManager = solrSearchEngineManager;
    }
    
}
