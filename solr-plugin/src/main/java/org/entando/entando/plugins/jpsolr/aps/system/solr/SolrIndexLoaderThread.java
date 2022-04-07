/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.entando.entando.plugins.jpsolr.aps.system.solr;

import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.IIndexerDAO;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.LastReloadInfo;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;

/**
 * @author eu
 */
public class SolrIndexLoaderThread extends Thread {

	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(SolrIndexLoaderThread.class);

    public String getTypeCode() {
        return typeCode;
    }
    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }
	
	public SolrIndexLoaderThread(String typeCode, SearchEngineManager searchEngineManager, 
			IContentManager contentManager, IIndexerDAO indexerDao) {
		this._contentManager = contentManager;
		this._searchEngineManager = searchEngineManager;
		this._indexerDao = indexerDao;
        this.setTypeCode(typeCode);
	}
	
	@Override
	public void run() {
        SolrLastReloadInfo reloadInfo = (StringUtils.isBlank(this.getTypeCode()) || (null == this._searchEngineManager.getLastReloadInfo())) ?
                new SolrLastReloadInfo() : (SolrLastReloadInfo) this._searchEngineManager.getLastReloadInfo();
		try {
			this.loadNewIndex();
			reloadInfo.setResult(LastReloadInfo.ID_SUCCESS_RESULT);
		} catch (Throwable t) {
			reloadInfo.setResult(LastReloadInfo.ID_FAILURE_RESULT);
			_logger.error("error in run", t);
		} finally {
            if (!StringUtils.isBlank(this.getTypeCode())) {
                reloadInfo.getDatesByType().put(typeCode, new Date());
            } else {
                reloadInfo.setDate(new Date());
            }
			this._searchEngineManager.notifyEndingIndexLoading(reloadInfo, this._indexerDao);
			this._searchEngineManager.sellOfQueueEvents();
		}
	}
	
	private void loadNewIndex() throws Throwable {
		try {
            EntitySearchFilter[] filters = null;
            if (!StringUtils.isBlank(this.getTypeCode())) {
                EntitySearchFilter filter = new EntitySearchFilter(IContentManager.ENTITY_TYPE_CODE_FILTER_KEY, false, this.getTypeCode(), false);
                filters = new EntitySearchFilter[]{filter};
            }
			List<String> contentsId = this._contentManager.searchId(filters);
			for (int i=0; i<contentsId.size(); i++) {
				String id = contentsId.get(i);
				this.reloadContentIndex(id);
			}
			_logger.info("Indicizzazione effettuata");
		} catch (Throwable t) {
			_logger.error("error in reloadIndex", t);
			throw t;
		}
	}
	
	private void reloadContentIndex(String id) {
		try {
			Content content = this._contentManager.loadContent(id, true);
			if (content != null) {
				this._indexerDao.add(content);
				_logger.debug("Indexed content {}", content.getId());
			}
		} catch (Throwable t) {
			_logger.error("Error reloading index: content id {}", id, t);
		}
	}
	private String typeCode;
	private SearchEngineManager _searchEngineManager;
	private IContentManager _contentManager;
	private IIndexerDAO _indexerDao;
    
}
