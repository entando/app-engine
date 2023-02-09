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

import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.IIndexerDAO;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ISearchEngineDAOFactory;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ISearcherDAO;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.annotation.PreDestroy;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.entando.entando.ent.exception.EntException;
import org.springframework.beans.factory.annotation.Value;

/**
 * Classe factory degli elementi ad uso del SearchEngine.
 *
 * @author E.Santoboni
 */
public class SearchEngineDAOFactory implements ISearchEngineDAOFactory, ISolrSearchEngineDAOFactory {

    @Value("${SOLR_ADDRESS:http://localhost:8983/solr}")
    private String solrAddress;

    @Value("${SOLR_CORE:entando}")
    private String solrCore;

    private ConfigInterface configManager;
    private ILangManager langManager;
    private ICategoryManager categoryManager;

    private SolrClient solrClient;
    private IndexerDAO indexerDAO;
    private SearcherDAO searcherDAO;

    @Override
    public void init() throws Exception {
        this.solrClient = new HttpSolrClient.Builder(this.solrAddress)
                .withConnectionTimeout(10000)
                .withSocketTimeout(60000)
                .build();

        this.indexerDAO = new IndexerDAO(solrClient, this.solrCore);
        this.indexerDAO.setLangManager(this.getLangManager());
        this.indexerDAO.setTreeNodeManager(this.getCategoryManager());

        this.searcherDAO = new SearcherDAO(solrClient, this.solrCore);
        this.searcherDAO.setTreeNodeManager(this.getCategoryManager());
        this.searcherDAO.setLangManager(this.getLangManager());
    }

    @PreDestroy
    public void close() throws IOException {
        this.solrClient.close();
    }

    @Override
    public List<Map<String, Serializable>> getFields() {
        return SolrSchemaClient.getFields(this.solrAddress, this.solrCore);
    }

    @Override
    public boolean addField(Map<String, Serializable> properties) {
        return SolrSchemaClient.addField(this.solrAddress, this.solrCore, properties);
    }

    @Override
    public boolean replaceField(Map<String, Serializable> properties) {
        return SolrSchemaClient.replaceField(this.solrAddress, this.solrCore, properties);
    }

    @Override
    public boolean deleteField(String fieldKey) {
        return SolrSchemaClient.deleteField(this.solrAddress, this.solrCore, fieldKey);
    }

    @Override
    public boolean deleteAllDocuments() {
        return SolrSchemaClient.deleteAllDocuments(this.solrAddress, this.solrCore);
    }

    @Override
    public boolean checkCurrentSubfolder() throws EntException {
        // nothing to do
        return true;
    }

    @Override
    public IIndexerDAO getIndexer() throws EntException {
        return this.indexerDAO;
    }

    @Override
    public ISearcherDAO getSearcher() throws EntException {
        return this.searcherDAO;
    }

    @Override
    public IIndexerDAO getIndexer(String subDir) throws EntException {
        return this.getIndexer();
    }

    @Override
    public ISearcherDAO getSearcher(String subDir) throws EntException {
        return this.getSearcher();
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
