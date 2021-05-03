/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.entando.entando.plugins.jpsolr.aps.system.solr;

import java.util.List;
import java.util.Map;

/**
 * @author E.Santoboni
 */
public interface ISolrSearchEngineDAOFactory {
    
    public List<Map<String, Object>> getFields();

    public boolean addField(Map<String, Object> properties);

    public boolean replaceField(Map<String, Object> properties);

    public boolean deleteField(String fieldKey);
    
}
