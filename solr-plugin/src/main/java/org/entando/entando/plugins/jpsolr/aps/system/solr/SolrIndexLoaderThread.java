/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.entando.entando.plugins.jpsolr.aps.system.solr;

import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.IIndexerDAO;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.LastReloadInfo;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author eu
 */
public class SolrIndexLoaderThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(SolrIndexLoaderThread.class);

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public SolrIndexLoaderThread(String typeCode, SearchEngineManager searchEngineManager,
            IContentManager contentManager, IIndexerDAO indexerDao) {
        this.contentManager = contentManager;
        this.searchEngineManager = searchEngineManager;
        this.indexerDao = indexerDao;
        this.setTypeCode(typeCode);
    }

    @Override
    public void run() {
        SolrLastReloadInfo reloadInfo =
                (StringUtils.isBlank(this.getTypeCode()) || (null == this.searchEngineManager.getLastReloadInfo())) ?
                        new SolrLastReloadInfo() : (SolrLastReloadInfo) this.searchEngineManager.getLastReloadInfo();
        try {
            this.loadNewIndex();
            reloadInfo.setResult(LastReloadInfo.ID_SUCCESS_RESULT);
        } catch (Throwable t) {
            reloadInfo.setResult(LastReloadInfo.ID_FAILURE_RESULT);
            logger.error("error in run", t);
        } finally {
            if (!StringUtils.isBlank(this.getTypeCode())) {
                reloadInfo.getDatesByType().put(typeCode, new Date());
            } else {
                reloadInfo.setDate(new Date());
            }
            this.searchEngineManager.notifyEndingIndexLoading(reloadInfo, this.indexerDao);
            this.searchEngineManager.sellOfQueueEvents();
        }
    }

    private void loadNewIndex() throws Throwable {
        try {
            EntitySearchFilter[] filters = null;
            if (!StringUtils.isBlank(this.getTypeCode())) {
                EntitySearchFilter filter = new EntitySearchFilter(IContentManager.ENTITY_TYPE_CODE_FILTER_KEY, false,
                        this.getTypeCode(), false);
                filters = new EntitySearchFilter[]{filter};
            }
            List<String> contentsId = this.contentManager.searchId(filters);
            ((IndexerDAO) this.indexerDao).addBulk(
                    this.contentManager.searchId(filters).stream()
                            .map(contentId -> {
                                try {
                                    return (IApsEntity) this.contentManager.loadContent(contentId, true);
                                } catch (EntException ex) {
                                    throw new EntRuntimeException("Unable to load content " + contentId, ex);
                                }
                            })
                            .filter(content -> content != null));
            logger.info("Indicizzazione effettuata");
        } catch (RuntimeException ex) {
            logger.error("error in reloadIndex", ex);
            throw ex;
        }
    }

    private String typeCode;
    private SearchEngineManager searchEngineManager;
    private IContentManager contentManager;
    private IIndexerDAO indexerDao;

}
