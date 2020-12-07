/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.entando.entando.plugins.jpsolr.aps.system.solr;

import com.agiletec.aps.system.common.entity.event.EntityTypesChangingObserver;
import com.agiletec.plugins.jacms.aps.system.services.content.event.PublicContentChangedObserver;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;

/**
 *
 * @author eu
 */
public class SearchEngineManager extends com.agiletec.plugins.jacms.aps.system.services.searchengine.SearchEngineManager 
        implements ICmsSearchEngineManager, PublicContentChangedObserver, EntityTypesChangingObserver {
    
    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(SearchEngineManager.class);
    
    @Override
    public void deleteIndexedEntity(String entityId) throws EntException {
        try {
            this.getIndexerDao().delete(SolrFields.SOLR_CONTENT_ID_FIELD_NAME, entityId);
        } catch (EntException e) {
            logger.error("Error deleting content {} from index", entityId, e);
            throw e;
        }
    }
    
}
