/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.entando.entando.plugins.jpsolr.aps.system.solr;

import com.agiletec.aps.BaseTestCase;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.entando.entando.plugins.jpsolr.SolrTestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author E.Santoboni
 */
public class TestSolrSchemaClient {

    @BeforeAll
    public static void startUp() throws Exception {
        SolrTestUtils.startContainer();
        BaseTestCase.setUp();
    }
    
    @AfterAll
    public static void tearDown() throws Exception {
        BaseTestCase.tearDown();
        SolrTestUtils.stopContainer();
    }
    
    @Test
    public void testGetFields() throws Throwable {
        String address = System.getenv("SOLR_ADDRESS");
        String core = System.getenv("SOLR_CORE");
        List<Map<String, Object>> fields = SolrSchemaClient.getFields(address, core);
        Assertions.assertNotNull(fields);
    }
    
    @Test
    public void testAddDeleteField() throws Throwable {
        String address = System.getenv("SOLR_ADDRESS");
        String core = System.getenv("SOLR_CORE");
        String fieldName = "test_solr";
        List<Map<String, Object>> fields = SolrSchemaClient.getFields(address, core);
        Assertions.assertNotNull(fields);
        try {
            Map<String, Object> addedFiled = fields.stream().filter(f -> f.containsKey(fieldName)).findFirst().orElse(null);
            Assertions.assertNull(addedFiled);

            Map<String, String> propertis = new HashMap<>();
            propertis.put("name", fieldName);
            propertis.put("type", "text_general");
            boolean result = SolrSchemaClient.addField(address, core, propertis);
            Assertions.assertTrue(result);

            fields = SolrSchemaClient.getFields(address, core);
            Assertions.assertNotNull(fields);
            addedFiled = fields.stream().filter(f -> f.get("name").equals(fieldName)).findFirst().orElse(null);
            Assertions.assertNotNull(addedFiled);
            Assertions.assertEquals("text_general", addedFiled.get("type"));

            propertis.put("type", "plong");
            result = SolrSchemaClient.replaceField(address, core, propertis);
            Assertions.assertTrue(result);

            fields = SolrSchemaClient.getFields(address, core);
            Assertions.assertNotNull(fields);
            addedFiled = fields.stream().filter(f -> f.get("name").equals(fieldName)).findFirst().orElse(null);
            Assertions.assertNotNull(addedFiled);
            Assertions.assertEquals("plong", addedFiled.get("type"));
        } catch (Exception e) {
        } finally {
            boolean result = SolrSchemaClient.deleteField(address, core, fieldName);
            Assertions.assertTrue(result);

            fields = SolrSchemaClient.getFields(address, core);
            Assertions.assertNotNull(fields);
            Map<String, Object> addedFiled = fields.stream().filter(f -> f.get("name").equals(fieldName)).findFirst().orElse(null);
            Assertions.assertNull(addedFiled);
        }
    }
    
}
