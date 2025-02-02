package org.entando.entando.plugins.jpsolr.aps.system.solr;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SolrSchemaDaoTest {

    @Mock
    private SolrClient solrClient;

    private SolrSchemaDAO solrSchemaDAO;

    @BeforeEach
    void setUp() {
        solrSchemaDAO = new SolrSchemaDAO(solrClient, "entando");
    }

    @Test
    void shouldGetFields() throws Exception {
        NamedList<Object> mockedResult = new NamedList<>();
        mockedResult.add("fields", getMockedFields());
        Mockito.when(solrClient.request(any(SchemaRequest.Fields.class), anyString())).thenReturn(mockedResult);
        List<Map<String, ?>> fields = solrSchemaDAO.getFields();
        Assertions.assertEquals(1, fields.size());
        Assertions.assertEquals("_version_", fields.get(0).get("name"));
        Assertions.assertFalse((boolean) fields.get(0).get("indexed"));
    }

    @Test
    void shouldReturnEmptyListIfGetFieldsFail() throws Exception {
        Mockito.doThrow(SolrServerException.class).when(solrClient)
                .request(any(SchemaRequest.Fields.class), anyString());
        List<Map<String, ?>> fields = solrSchemaDAO.getFields();
        Assertions.assertTrue(fields.isEmpty());
    }

    @Test
    void shouldUpdateFields() throws Exception {
        NamedList<Object> mockedResult = new NamedList<>();
        mockedResult.add("fields", getMockedFields());
        Mockito.lenient().when(solrClient.request(any(SchemaRequest.Fields.class), anyString())).thenReturn(mockedResult);
        List<Map<String, ?>> fieldsToAdd = List.of(Map.of("name", "field1"));
        List<Map<String, ?>> fieldsToReplace = List.of(Map.of("name", "field2"));
        Assertions.assertTrue(solrSchemaDAO.updateFields(fieldsToAdd, fieldsToReplace));
        Mockito.verify(solrClient).request(Mockito.any(SchemaRequest.MultiUpdate.class), anyString());
    }

    @Test
    void shouldReturnFalseIfUpdateFieldsFail() throws Exception {
        Mockito.doThrow(SolrServerException.class).when(solrClient)
                .request(any(SchemaRequest.MultiUpdate.class), anyString());
        List<Map<String, ?>> fieldsToAdd = List.of(Map.of("name", "field1"));
        List<Map<String, ?>> fieldsToReplace = List.of(Map.of("name", "field2"));
        Assertions.assertFalse(solrSchemaDAO.updateFields(fieldsToAdd, fieldsToReplace));
    }

    private List<SimpleOrderedMap<Object>> getMockedFields() {
        List<SimpleOrderedMap<Object>> fields = new ArrayList<>();
        SimpleOrderedMap<Object> field = new SimpleOrderedMap<>();
        field.add("name", "_version_");
        field.add("type", "plong");
        field.add("indexed", false);
        field.add("stored", false);
        fields.add(field);
        return fields;
    }
    
}
