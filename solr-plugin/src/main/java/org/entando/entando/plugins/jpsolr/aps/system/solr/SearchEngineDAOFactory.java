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
import com.agiletec.plugins.jacms.aps.system.services.searchengine.IIndexerDAO;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ISearchEngineDAOFactory;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ISearcherDAO;
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
    
    @Value("${SOLR_ADDRESS:http://localhost:8983/solr}")
    private String solrAddress;
    
    @Value("${SOLR_CORE:entando}")
    private String solrCore;
    
    private ConfigInterface configManager;
    private ILangManager langManager;
    private ICategoryManager categoryManager;

    @Override
    public void init() throws Exception {
        // nothing to do
    }

    @Override
    public boolean checkCurrentSubfolder() throws EntException {
        // nothing to do
        return true;
    }

    @Override
    public IIndexerDAO getIndexer() throws EntException {
        return this.getIndexer("");
    }

    @Override
    public ISearcherDAO getSearcher() throws EntException {
        return this.getSearcher("");
    }

    @Override
    public IIndexerDAO getIndexer(String subDir) throws EntException {
        IndexerDAO indexerDao = new IndexerDAO();
        indexerDao.setLangManager(this.getLangManager());
        indexerDao.setTreeNodeManager(this.getCategoryManager());
        indexerDao.setSolrAddress(this.solrAddress);
        indexerDao.setSolrCore(this.solrCore);
        return indexerDao;
    }

    @Override
    public ISearcherDAO getSearcher(String subDir) throws EntException {
        SearcherDAO searcherDao = new SearcherDAO();
        searcherDao.setTreeNodeManager(this.getCategoryManager());
        searcherDao.setLangManager(this.getLangManager());
        searcherDao.setSolrAddress(this.solrAddress);
        searcherDao.setSolrCore(this.solrCore);
        return searcherDao;
    }

    @Override
    public void updateSubDir(String newSubDirectory) throws EntException {
        // nothing to do
    }
    
    @Override
    public void deleteSubDirectory(String subDirectory) {
        // nothing to do
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
