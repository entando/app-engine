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

import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.util.ApsTenantApplicationUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.HttpClientBuilder;
import org.entando.entando.aps.system.services.searchengine.SolrEnvironmentVariables;
import org.entando.entando.aps.system.services.tenants.ITenantManager;

/**
 * Classe factory degli elementi ad uso del SearchEngine.
 *
 * @author E.Santoboni
 */
@Slf4j
public class SolrProxyTenantAware implements ISolrProxyTenantAware {

    private static final String SOLR_ADDRESS_TENANT_PARAM = "solrAddress";
    private static final String SOLR_CORE_TENANT_PARAM = "solrCore";

    private final ILangManager langManager;
    private final ICategoryManager categoryManager;
    private final ITenantManager tenantManager;
    private final HttpClientBuilder httpClientBuilder;

    private final Map<String, SolrResourcesAccessor> tenantResources = new ConcurrentHashMap<>();

    public SolrProxyTenantAware(ILangManager langManager, ICategoryManager categoryManager,
            ITenantManager tenantManager, HttpClientBuilder httpClientBuilder) {
        this.langManager = langManager;
        this.categoryManager = categoryManager;
        this.tenantManager = tenantManager;
        this.httpClientBuilder = httpClientBuilder;
    }

    public void afterPropertiesSet() throws Exception {
        log.debug("Initializing Solr tenant resources for primary tenant");
        tenantResources.put(ITenantManager.PRIMARY_CODE, newSolrDAOResources());
    }

    @Override
    public void init() throws Exception {
        ApsTenantApplicationUtils.getTenant().ifPresent(tenantCode -> {
            log.debug("Initializing Solr tenant resources for {}", tenantCode);
            tenantResources.put(tenantCode, newSolrDAOResources());
        });
    }

    private SolrResourcesAccessor newSolrDAOResources() {
        return new SolrResourcesAccessor(this.getSolrAddress(), this.getSolrCore(), this.langManager,
                this.categoryManager, this.httpClientBuilder);
    }

    @Override
    public void close() throws IOException {
        String tenantCode = ApsTenantApplicationUtils.getTenant().orElse(ITenantManager.PRIMARY_CODE);
        if (tenantResources.containsKey(tenantCode)) {
            tenantResources.get(tenantCode).close();
            tenantResources.remove(tenantCode);
        }
    }

    @Override
    public void destroy() {
        tenantResources.values().stream().forEach(r -> r.close());
        tenantResources.clear();
    }

    @Override
    public ISolrIndexerDAO getIndexerDAO() {
        return getSolrTenantResources().getIndexerDAO();
    }

    @Override
    public ISolrSearcherDAO getSearcherDAO() {
        return getSolrTenantResources().getSearcherDAO();
    }

    @Override
    public ISolrSchemaDAO getSolrSchemaDAO() {
        return getSolrTenantResources().getSolrSchemaDAO();
    }

    @Override
    public ISolrIndexStatus getIndexStatus() {
        return getSolrTenantResources().getIndexStatus();
    }

    @Override
    public String getSolrCore() {
        return this.getTenantParameter(SOLR_CORE_TENANT_PARAM, SolrEnvironmentVariables.solrCore());
    }

    @Override
    public ISolrResourcesAccessor getSolrTenantResources() {
        String tenantCode = ApsTenantApplicationUtils.getTenant().orElse(ITenantManager.PRIMARY_CODE);
        return tenantResources.computeIfAbsent(tenantCode, code -> newSolrDAOResources());
    }

    @Override
    public List<ISolrResourcesAccessor> getAllSolrTenantsResources() {
        return new ArrayList<>(tenantResources.values());
    }

    public String getSolrAddress() {
        return this.getTenantParameter(SOLR_ADDRESS_TENANT_PARAM, SolrEnvironmentVariables.solrAddress());
    }

    private String getTenantParameter(String paramName, String defaultValue) {
        return ApsTenantApplicationUtils.getTenant()
                .flatMap(tenantManager::getConfigOfReadyTenant)
                .flatMap(tenantConfig -> tenantConfig.getProperty(paramName))
                .orElse(defaultValue);
    }
}
