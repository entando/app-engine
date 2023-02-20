/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

import com.agiletec.aps.system.common.entity.IEntityManager;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.IIndexerDAO;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.LastReloadInfo;
import java.util.Date;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author E.Santoboni
 */
public class SolrIndexLoaderThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(SolrIndexLoaderThread.class);

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public SolrIndexLoaderThread(String typeCode, SolrSearchEngineManager searchEngineManager,
            IContentManager contentManager, IIndexerDAO indexerDao) {
        this.contentManager = contentManager;
        this.searchEngineManager = searchEngineManager;
        this.indexerDao = indexerDao;
        this.setTypeCode(typeCode);
    }

    @Override
    public void run() {
        SolrLastReloadInfo reloadInfo = (SolrLastReloadInfo) this.searchEngineManager.getLastReloadInfo();
        if (null == reloadInfo) {
            reloadInfo = new SolrLastReloadInfo();
        }
        try {
            this.loadNewIndex();
            reloadInfo.setResult(LastReloadInfo.ID_SUCCESS_RESULT);
        } catch (EntException t) {
            reloadInfo.setResult(LastReloadInfo.ID_FAILURE_RESULT);
            logger.error("error in run", t);
        } finally {
            if (!StringUtils.isBlank(this.getTypeCode())) {
                reloadInfo.getDatesByType().put(this.getTypeCode(), new Date());
            } else {
                reloadInfo.setDate(new Date());
            }
            this.searchEngineManager.notifyEndingIndexLoading(reloadInfo, this.indexerDao);
            this.searchEngineManager.sellOfQueueEvents();
        }
    }

    private void loadNewIndex() throws EntException {
        EntitySearchFilter[] filters = null;
        if (!StringUtils.isBlank(this.getTypeCode())) {
            EntitySearchFilter<?> filter = new EntitySearchFilter<>(IEntityManager.ENTITY_TYPE_CODE_FILTER_KEY, false,
                    this.getTypeCode(), false);
            filters = new EntitySearchFilter[]{filter};
        }
        ((IndexerDAO) this.indexerDao).addBulk(
                this.contentManager.searchId(filters).stream()
                        .map(contentId -> {
                            try {
                                return (IApsEntity) this.contentManager.loadContent(contentId, true);
                            } catch (EntException ex) {
                                throw new EntRuntimeException("Unable to load content " + contentId, ex);
                            }
                        })
                        .filter(Objects::nonNull));
        logger.info("Indicizzazione effettuata");
    }

    private String typeCode;
    private SolrSearchEngineManager searchEngineManager;
    private IContentManager contentManager;
    private IIndexerDAO indexerDao;

}
