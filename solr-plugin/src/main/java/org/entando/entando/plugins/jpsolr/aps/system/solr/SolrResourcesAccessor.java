/*
 * Copyright 2023-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.aps.system.services.lang.ILangManager;
import java.io.IOException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;

@Slf4j
public class SolrResourcesAccessor implements ISolrResourcesAccessor {

    @Getter
    private final String solrCore;

    private final SolrClient solrClient;

    @Getter
    private final ISolrIndexerDAO indexerDAO;
    @Getter
    private final ISolrSearcherDAO searcherDAO;
    @Getter
    private final ISolrSchemaDAO solrSchemaDAO;
    @Getter
    private final ISolrIndexStatus indexStatus;

    public SolrResourcesAccessor(String solrAddress, String solrCore, ILangManager langManager,
            ICategoryManager categoryManager, HttpClientBuilder httpClientBuilder) {
        log.debug("Creating Solr resources for {}", solrCore);
        this.solrCore = solrCore;
        this.solrClient = new Http2SolrClient.Builder(solrAddress).build();

        this.indexerDAO = new IndexerDAO(solrClient, solrCore);
        this.indexerDAO.setLangManager(langManager);
        this.indexerDAO.setTreeNodeManager(categoryManager);

        this.searcherDAO = new SearcherDAO(solrClient, solrCore);
        this.searcherDAO.setLangManager(langManager);
        this.searcherDAO.setTreeNodeManager(categoryManager);

        this.solrSchemaDAO = new SolrSchemaDAO(solrClient, solrCore);

        this.indexStatus = new SolrIndexStatus();
    }

    public void close() {
        try {
            solrClient.close();
        } catch (IOException ex) {
            log.error("Error closing SolrClient", ex);
        }
    }
}
