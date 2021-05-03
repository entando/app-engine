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
package org.entando.entando.plugins.jpsolr.aps.system.solr;

import com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager;

/**
 * @author E.Santoboni
 */
public interface ISolrSearchEngineManager extends ICmsSearchEngineManager {
    /*
    public List<Map<String, Object>> getFields();
    
    public boolean addField(Map<String, Object> properties);
    
    public boolean updateField(Map<String, Object> properties);
    
    public boolean deleteField(String fieldName);
    */
    public boolean refreshCmsFields();
    
}
