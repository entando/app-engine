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

import com.agiletec.aps.system.common.notify.INotifyManager;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.SearchEngineDAOFactory;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.SearchEngineManager;
import lombok.Setter;
import org.apache.http.impl.client.HttpClientBuilder;
import org.entando.entando.aps.system.services.searchengine.SolrEnvironmentVariables;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.plugins.jpsolr.aps.system.solr.cache.ISolrSearchEngineCacheWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

public class SearchEngineManagerFactory implements BeanFactoryAware {

    @Setter
    private String indexDiskRootFolder;
    @Setter
    private ConfigInterface configManager;
    @Setter
    private ILangManager langManager;
    @Setter
    private ICategoryManager categoryManager;
    @Setter
    private IContentManager contentManager;
    @Setter
    private ITenantManager tenantManager;
    @Setter
    private ISolrSearchEngineCacheWrapper cacheWrapper;
    @Setter
    private HttpClientBuilder solrHttpClientBuilder;
    @Setter
    private INotifyManager notifyManager;

    private BeanFactory beanFactory;

    public ICmsSearchEngineManager createSearchEngineManager() throws Exception {
        if (SolrEnvironmentVariables.active()) {
            SolrProxyTenantAware solrProxy = new SolrProxyTenantAware(
                    langManager, categoryManager, tenantManager, solrHttpClientBuilder
            );
            solrProxy.afterPropertiesSet();
            SolrSearchEngineManager solrSearchEngineManager = new SolrSearchEngineManager();
            solrSearchEngineManager.setSolrProxy(solrProxy);
            solrSearchEngineManager.setContentManager(contentManager);
            solrSearchEngineManager.setLangManager(langManager);
            solrSearchEngineManager.setCacheWrapper(cacheWrapper);
            solrSearchEngineManager.setNotifyManager(notifyManager);
            solrSearchEngineManager.setBeanFactory(beanFactory);
            solrSearchEngineManager.setBeanName(JacmsSystemConstants.SEARCH_ENGINE_MANAGER);
            return solrSearchEngineManager;
        } else {
            SearchEngineDAOFactory factory = new SearchEngineDAOFactory();
            factory.setIndexDiskRootFolder(indexDiskRootFolder);
            factory.setConfigManager(configManager);
            factory.setLangManager(langManager);
            factory.setCategoryManager(categoryManager);
            SearchEngineManager baseSearchEngineManager = new SearchEngineManager();
            baseSearchEngineManager.setFactory(factory);
            baseSearchEngineManager.setContentManager(contentManager);
            baseSearchEngineManager.setNotifyManager(notifyManager);
            baseSearchEngineManager.setBeanFactory(beanFactory);
            baseSearchEngineManager.setBeanName(JacmsSystemConstants.SEARCH_ENGINE_MANAGER);
            return baseSearchEngineManager;
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
