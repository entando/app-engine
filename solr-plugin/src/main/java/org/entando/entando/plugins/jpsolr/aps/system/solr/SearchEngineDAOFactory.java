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

import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.IIndexerDAO;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ISearchEngineDAOFactory;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ISearcherDAO;
import java.io.File;
import java.io.IOException;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.springframework.beans.factory.annotation.Value;

/**
 * Classe factory degli elementi ad uso del SearchEngine.
 *
 * @author E.Santoboni
 */
public class SearchEngineDAOFactory implements ISearchEngineDAOFactory {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(SearchEngineDAOFactory.class);
    
    @Value("${SOLR_ADDRESS:http://localhost:8983}")
    private String solrAddress;
    
    private String subDirectory;
    
    private ConfigInterface configManager;
    private ILangManager langManager;
    private ICategoryManager categoryManager;

    @Override
    public void init() throws Exception {
        this.subDirectory = this.getConfigManager().getConfigItem(JacmsSystemConstants.CONFIG_ITEM_CONTENT_INDEX_SUB_DIR);
        if (this.subDirectory == null) {
            throw new EntException("Item configurazione assente: " + JacmsSystemConstants.CONFIG_ITEM_CONTENT_INDEX_SUB_DIR);
        }
        SolrClient client = null;
        try {
            client = this.getSolrClient();
            CoreAdminResponse check = CoreAdminRequest.getStatus(this.subDirectory, client);
            if (null == check.getCoreStatus(this.subDirectory)) {
                CoreAdminRequest.createCore(this.subDirectory, this.subDirectory, client);
            }
        } catch (Exception e) {
            logger.error("Error creating core {}", this.subDirectory, e);
            throw new EntException("Error creating core", e);
        } finally {
            if (null != client) {
                client.close();
            }
        }
    }

    @Override
    public boolean checkCurrentSubfolder() throws EntException {
        String currentSubDir = this.getConfigManager().getConfigItem(JacmsSystemConstants.CONFIG_ITEM_CONTENT_INDEX_SUB_DIR);
        boolean check = currentSubDir.equals(this.subDirectory);
        if (!check) {
            logger.info("Actual subfolder {} - Current subfolder {}", this.subDirectory, currentSubDir);
        }
        return check;
    }

    @Override
    public IIndexerDAO getIndexer() throws EntException {
        return this.getIndexer(this.subDirectory);
    }

    @Override
    public ISearcherDAO getSearcher() throws EntException {
        return this.getSearcher(this.subDirectory);
    }

    @Override
    public IIndexerDAO getIndexer(String subDir) throws EntException {
        IndexerDAO indexerDao = new IndexerDAO();
        indexerDao.setLangManager(this.getLangManager());
        indexerDao.setTreeNodeManager(this.getCategoryManager());
        indexerDao.setSolrAddress(this.solrAddress);
        indexerDao.setSolrCore(subDir);
        return indexerDao;
    }

    @Override
    public ISearcherDAO getSearcher(String subDir) throws EntException {
        SearcherDAO searcherDao = new SearcherDAO();
        searcherDao.setTreeNodeManager(this.getCategoryManager());
        searcherDao.setLangManager(this.getLangManager());
        searcherDao.setSolrAddress(this.solrAddress);
        searcherDao.setSolrCore(subDir);
        return searcherDao;
    }

    @Override
    public void updateSubDir(String newSubDirectory) throws EntException {
        this.getConfigManager().updateConfigItem(JacmsSystemConstants.CONFIG_ITEM_CONTENT_INDEX_SUB_DIR, newSubDirectory);
        String oldDir = subDirectory;
        this.subDirectory = newSubDirectory;
        this.deleteSubDirectory(oldDir);
    }
    
    private SolrClient getSolrClient() {
        return new HttpSolrClient.Builder(this.solrAddress)
                .withConnectionTimeout(10000)
                .withSocketTimeout(60000)
                .build();
    }
    
    @Override
    public void deleteSubDirectory(String subDirectory) {
        SolrClient client = null;
        try {
            client = this.getSolrClient();
            client.deleteByQuery(subDirectory, "*:*");
            client.commit(subDirectory);
        } catch (Throwable t) {
            //_logger.error("Error saving entity {}", entity.getId(), t);
            //throw new EntException("Error saving entity", t);
        } finally {
            if (null != client) {
                try {
                    client.close();
                } catch (IOException ex) {
                    //throw new EntException("Error closing client", ex);
                }
            }
        }
    }
    
    protected ConfigInterface getConfigManager() {
        return configManager;
    }
    public void setConfigManager(ConfigInterface configService) {
        this.configManager = configService;
    }

    protected ILangManager getLangManager() {
        return langManager;
    }
    public void setLangManager(ILangManager langManager) {
        this.langManager = langManager;
    }

    protected ICategoryManager getCategoryManager() {
        return categoryManager;
    }
    public void setCategoryManager(ICategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }

}
