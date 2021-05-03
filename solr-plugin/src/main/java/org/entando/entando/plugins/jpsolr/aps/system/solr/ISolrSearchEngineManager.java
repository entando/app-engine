/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
