/*
 * Copyright 2022-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;
import org.entando.entando.aps.system.services.searchengine.FacetedContentsResult;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter.TextSearchOption;
import org.entando.entando.plugins.jpsolr.CustomConfigTestUtils;
import org.entando.entando.plugins.jpsolr.SolrTestExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockServletContext;

/**
 * Rewriting of some default test for content manager
 * @author E.Santoboni
 */
@ExtendWith(SolrTestExtension.class)
class ReloadByTypesIntegrationTest {

    private ISolrSearchEngineManager searchEngineManager = null;

    private static ApplicationContext applicationContext;

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static void setApplicationContext(ApplicationContext applicationContext) {
        ReloadByTypesIntegrationTest.applicationContext = applicationContext;
    }
    
    @BeforeAll
    public static void startUp() throws Exception {
        ServletContext srvCtx = new MockServletContext("", new FileSystemResourceLoader());
        ApplicationContext applicationContext = new CustomConfigTestUtils().createApplicationContext(srvCtx);
        setApplicationContext(applicationContext);
    }
    
    @AfterAll
    public static void tearDown() throws Exception {
        BaseTestCase.tearDown();
    }
    
    @BeforeEach
    protected void init() throws Exception {
        try {
            this.searchEngineManager = getApplicationContext().getBean(ISolrSearchEngineManager.class);
            this.searchEngineManager.refreshCmsFields();
        } catch (Exception e) {
            throw e;
        }
    }
    
    @Test
    void testLoadPublicContents() throws Exception {
        List<String> allowedGroup = new ArrayList<>();
        allowedGroup.add(Group.ADMINS_GROUP_NAME);
        List<String> allowedValues = new ArrayList<>();
        allowedValues.add("EVN");
        allowedValues.add("ART");
        SearchEngineFilter typeFilter = new SearchEngineFilter(IContentManager.ENTITY_TYPE_CODE_FILTER_KEY, allowedValues, TextSearchOption.EXACT);
        SearchEngineFilter[] filters = {typeFilter};
        SearchEngineFilter[] categoriesFilters = {};
        FacetedContentsResult result = this.searchEngineManager.searchFacetedEntities(filters, categoriesFilters, allowedGroup);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getContentsId().isEmpty());
        Assertions.assertNull(this.searchEngineManager.getLastReloadInfo());
        
        
        Thread thread = this.searchEngineManager.startReloadContentsReferencesByType("ART");
        thread.join();
        result = this.searchEngineManager.searchFacetedEntities(filters, categoriesFilters, allowedGroup);
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getContentsId().isEmpty());
        for (String contentId : result.getContentsId()) {
            assertTrue(contentId.startsWith("ART"));
        }
        Assertions.assertNotNull(this.searchEngineManager.getLastReloadInfo());
        Assertions.assertNotNull(((SolrLastReloadInfo) this.searchEngineManager.getLastReloadInfo()).getDateByType("ART"));
        Assertions.assertNull(((SolrLastReloadInfo) this.searchEngineManager.getLastReloadInfo()).getDateByType("EVN"));
        
        thread = this.searchEngineManager.startReloadContentsReferencesByType("EVN");
        thread.join();
        result = this.searchEngineManager.searchFacetedEntities(filters, categoriesFilters, allowedGroup);
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getContentsId().isEmpty());
        for (int i = 0; i < result.getContentsId().size(); i++) {
            assertTrue(result.getContentsId().get(i).startsWith("ART") || result.getContentsId().get(i).startsWith("EVN"));
        }
        Assertions.assertNotNull(this.searchEngineManager.getLastReloadInfo());
        Assertions.assertNotNull(((SolrLastReloadInfo) this.searchEngineManager.getLastReloadInfo()).getDateByType("ART"));
        Assertions.assertNotNull(((SolrLastReloadInfo) this.searchEngineManager.getLastReloadInfo()).getDateByType("EVN"));
    }
    
}
