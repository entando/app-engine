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

import static org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields.SOLR_FIELD_NAME;
import static org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields.SOLR_FIELD_TYPE;
import static org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields.TYPE_PLONG;
import static org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields.TYPE_TEXT_GENERAL;

import com.agiletec.aps.BaseTestCase;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import org.entando.entando.plugins.jpsolr.CustomConfigTestUtils;
import org.entando.entando.plugins.jpsolr.SolrTestExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockServletContext;

/**
 * @author E.Santoboni
 */
@ExtendWith(SolrTestExtension.class)
class TestSolrSchemaClient {

    private static ApplicationContext applicationContext;

    private static String solrAddress;
    private static String solrCore;

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static void setApplicationContext(ApplicationContext applicationContext) {
        TestSolrSchemaClient.applicationContext = applicationContext;
    }

    @BeforeAll
    public static void startUp() throws Exception {
        ServletContext srvCtx = new MockServletContext("", new FileSystemResourceLoader());
        applicationContext = new CustomConfigTestUtils().createApplicationContext(srvCtx);
        setApplicationContext(applicationContext);

        solrAddress = applicationContext.getEnvironment().getProperty("SOLR_ADDRESS");
        solrCore = applicationContext.getEnvironment().getProperty("SOLR_CORE");
    }

    @AfterAll
    public static void tearDown() throws Exception {
        BaseTestCase.tearDown();
    }

    @Test
    void testGetFields() {
        List<Map<String, Serializable>> fields = SolrSchemaClient.getFields(solrAddress, solrCore);
        Assertions.assertNotNull(fields);
    }

    @Test
    void testAddDeleteField() {
        String fieldName = "test_solr";
        List<Map<String, Serializable>> fields = SolrSchemaClient.getFields(solrAddress, solrCore);
        Assertions.assertNotNull(fields);
        try {
            Map<String, Serializable> addedFiled = fields.stream().filter(f -> f.get(SOLR_FIELD_NAME).equals(fieldName)).findFirst()
                    .orElse(null);
            Assertions.assertNull(addedFiled);

            Map<String, Serializable> properties = new HashMap<>();
            properties.put(SOLR_FIELD_NAME, fieldName);
            properties.put(SOLR_FIELD_TYPE, TYPE_TEXT_GENERAL);
            boolean result = SolrSchemaClient.addField(solrAddress, solrCore, properties);
            Assertions.assertTrue(result);

            fields = SolrSchemaClient.getFields(solrAddress, solrCore);
            Assertions.assertNotNull(fields);
            addedFiled = fields.stream().filter(f -> f.get(SOLR_FIELD_NAME).equals(fieldName)).findFirst().orElse(null);
            Assertions.assertNotNull(addedFiled);
            Assertions.assertEquals("text_general", addedFiled.get(SOLR_FIELD_TYPE));

            properties.put(SOLR_FIELD_TYPE, TYPE_PLONG);
            result = SolrSchemaClient.replaceField(solrAddress, solrCore, properties);
            Assertions.assertTrue(result);

            fields = SolrSchemaClient.getFields(solrAddress, solrCore);
            Assertions.assertNotNull(fields);
            addedFiled = fields.stream().filter(f -> f.get(SOLR_FIELD_NAME).equals(fieldName)).findFirst().orElse(null);
            Assertions.assertNotNull(addedFiled);
            Assertions.assertEquals("plong", addedFiled.get(SOLR_FIELD_TYPE));
        } finally {
            boolean result = SolrSchemaClient.deleteField(solrAddress, solrCore, fieldName);
            Assertions.assertTrue(result);

            fields = SolrSchemaClient.getFields(solrAddress, solrCore);
            Assertions.assertNotNull(fields);
            Map<String, Serializable> addedFiled = fields.stream().filter(f -> f.get(SOLR_FIELD_NAME).equals(fieldName)).findFirst()
                    .orElse(null);
            Assertions.assertNull(addedFiled);
        }
    }

}
