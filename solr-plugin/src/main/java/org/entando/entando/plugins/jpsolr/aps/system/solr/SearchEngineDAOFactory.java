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
import com.agiletec.aps.util.ApsTenantApplicationUtils;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.IIndexerDAO;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ISearcherDAO;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.ent.exception.EntException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Classe factory degli elementi ad uso del SearchEngine.
 *
 * @author E.Santoboni
 */
@Component
public class SearchEngineDAOFactory implements ISolrSearchEngineDAOFactory {

    private static final String SOLR_ADDRESS_TENANT_PARAM = "solrAddress";
    private static final String SOLR_CORE_TENANT_PARAM = "solrCore";

    @Value("${SOLR_ADDRESS:http://localhost:8983/solr}")
    private String solrAddress;

    @Value("${SOLR_CORE:entando}")
    private String solrCore;

    private final ConfigInterface configManager;
    private final ILangManager langManager;
    private final ICategoryManager categoryManager;
    private final ITenantManager tenantManager;

    @Autowired
    public SearchEngineDAOFactory(ConfigInterface configManager, ILangManager langManager,
            ICategoryManager categoryManager, ITenantManager tenantManager) {
        this.configManager = configManager;
        this.langManager = langManager;
        this.categoryManager = categoryManager;
        this.tenantManager = tenantManager;
    }

    private static class SolrDAOResources {

        private final SolrClient solrClient;
        private final IndexerDAO indexerDAO;
        private final SearcherDAO searcherDAO;

        public SolrDAOResources(String solrAddress, String solrCore, ILangManager langManager,
                ICategoryManager categoryManager) {
            this.solrClient = new HttpSolrClient.Builder(solrAddress)
                    .withConnectionTimeout(10000)
                    .withSocketTimeout(60000)
                    .build();

            this.indexerDAO = new IndexerDAO(solrClient, solrCore);
            this.indexerDAO.setLangManager(langManager);
            this.indexerDAO.setTreeNodeManager(categoryManager);

            this.searcherDAO = new SearcherDAO(solrClient, solrCore);
            this.searcherDAO.setLangManager(langManager);
            this.searcherDAO.setTreeNodeManager(categoryManager);
        }

        public void close() throws IOException {
            solrClient.close();
        }
    }

    private SolrDAOResources primaryDAOResources;
    private final Map<String, SolrDAOResources> tenantDAOResources = new ConcurrentHashMap<>();

    @Override
    @PostConstruct
    public void init() throws Exception {
        this.primaryDAOResources = newSolrDAOResources();
    }

    private SolrDAOResources newSolrDAOResources() {
        return new SolrDAOResources(this.getSolrAddress(), this.getSolrCore(), this.langManager, this.categoryManager);
    }

    @PreDestroy
    public void close() throws IOException {
        this.primaryDAOResources.close();
        for (SolrDAOResources tenantResources : tenantDAOResources.values()) {
            tenantResources.close();
        }
    }

    @Override
    public List<Map<String, Serializable>> getFields() {
        return SolrSchemaClient.getFields(this.getSolrAddress(), this.getSolrCore());
    }

    @Override
    public boolean addField(Map<String, Serializable> properties) {
        return SolrSchemaClient.addField(this.getSolrAddress(), this.getSolrCore(), properties);
    }

    @Override
    public boolean replaceField(Map<String, Serializable> properties) {
        return SolrSchemaClient.replaceField(this.getSolrAddress(), this.getSolrCore(), properties);
    }

    @Override
    public boolean deleteField(String fieldKey) {
        return SolrSchemaClient.deleteField(this.getSolrAddress(), this.getSolrCore(), fieldKey);
    }

    @Override
    public boolean deleteAllDocuments() {
        return SolrSchemaClient.deleteAllDocuments(this.getSolrAddress(), this.getSolrCore());
    }

    @Override
    public boolean checkCurrentSubfolder() throws EntException {
        // nothing to do
        return true;
    }

    @Override
    public IIndexerDAO getIndexer() throws EntException {
        return ApsTenantApplicationUtils.getTenant()
                .map(tenantCode -> tenantDAOResources
                        .computeIfAbsent(tenantCode, t -> newSolrDAOResources()).indexerDAO)
                .orElse(primaryDAOResources.indexerDAO);
    }

    @Override
    public ISearcherDAO getSearcher() throws EntException {
        return ApsTenantApplicationUtils.getTenant()
                .map(tenantCode -> tenantDAOResources
                        .computeIfAbsent(tenantCode, t -> newSolrDAOResources()).searcherDAO)
                .orElse(primaryDAOResources.searcherDAO);
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

    public String getSolrAddress() {
        return this.getTenantParameter(SOLR_ADDRESS_TENANT_PARAM, this.solrAddress);
    }

    public String getSolrCore() {
        return this.getTenantParameter(SOLR_CORE_TENANT_PARAM, this.solrCore);
    }

    private String getTenantParameter(String paramName, String defaultValue) {
        return ApsTenantApplicationUtils.getTenant()
                .map(tenantCode -> tenantManager.getConfig(tenantCode)
                        .getProperty(paramName)
                        .orElse(defaultValue))
                .orElse(defaultValue);
    }
}
