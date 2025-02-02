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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.DateAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.NumberAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.TextAttribute;
import com.agiletec.aps.system.services.lang.Lang;
import org.junit.Before;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class SolrFieldsCheckerTest {
    
    private static final List<String> defaultFields = List.of(SolrFields.SOLR_CONTENT_ID_FIELD_NAME, 
            SolrFields.SOLR_CONTENT_TYPE_CODE_FIELD_NAME, 
            SolrFields.SOLR_CONTENT_GROUP_FIELD_NAME, 
            SolrFields.SOLR_CONTENT_DESCRIPTION_FIELD_NAME, 
            SolrFields.SOLR_CONTENT_MAIN_GROUP_FIELD_NAME, 
            SolrFields.SOLR_CONTENT_CATEGORY_FIELD_NAME, 
            SolrFields.SOLR_CONTENT_CREATION_FIELD_NAME, 
            SolrFields.SOLR_CONTENT_LAST_MODIFY_FIELD_NAME);
    
    private SolrFieldsChecker solrFieldsChecker;
    private List<Map<String, ?>> fields;
    private List<AttributeInterface> attributes;
    private List<Lang> languages;
    
    @BeforeEach
    void setUp() {
        fields = new ArrayList<>();
        attributes = new ArrayList<>();
        languages = new ArrayList<>();
        Lang mockLang = mock(Lang.class);
        when(mockLang.getCode()).thenReturn("en");
        languages.add(mockLang);
        solrFieldsChecker = new SolrFieldsChecker(fields, attributes, languages);
    }

    @Test
    void testCheckFields() {
        TextAttribute mockAttribute = mock(TextAttribute.class);
        when(mockAttribute.getName()).thenReturn("mockAttribute");
        when(mockAttribute.getRoles()).thenReturn(new String[]{"role1", "role2"});
        attributes.add(mockAttribute);
        
        SolrFieldsChecker.CheckFieldsResult result = solrFieldsChecker.checkFields();
        
        assertTrue(result.needsUpdate());
        List<String> expectedAddedFieldCodes = new ArrayList<>(defaultFields);
        expectedAddedFieldCodes.addAll(List.of("en_mockAttribute", "en", "en_role2", "en_role1"));
        assertEquals(expectedAddedFieldCodes.size(), result.getFieldsToAdd().size());
        assertEquals(0, result.getFieldsToReplace().size());
        List<String> addedFieldCodesRes = result.getFieldsToAdd()
                .stream().map(m -> m.get(SolrFields.SOLR_FIELD_NAME).toString()).collect(Collectors.toList());
        assertTrue(addedFieldCodesRes.containsAll(expectedAddedFieldCodes));
        
        List<Map<String, ?>> addedFieldRes = result.getFieldsToAdd().stream()
                .filter(f -> f.get(SolrFields.SOLR_FIELD_NAME).toString().contains("mockAttribute")).collect(Collectors.toList());
        assertEquals(1, addedFieldRes.size());
        assertEquals("en_mockAttribute", addedFieldRes.get(0).get(SolrFields.SOLR_FIELD_NAME));
        assertEquals(SolrFields.TYPE_TEXT_GEN_SORT, addedFieldRes.get(0).get(SolrFields.SOLR_FIELD_TYPE));
        assertFalse((boolean) addedFieldRes.get(0).get(SolrFields.SOLR_FIELD_MULTIVALUED));
    }

    @Test
    void testReplaceFieldWithSameType() {
        Map<String, Object> existingField = new HashMap<>();
        existingField.put(SolrFields.SOLR_FIELD_NAME, "en_existingField");
        existingField.put(SolrFields.SOLR_FIELD_TYPE, SolrFields.TYPE_TEXT_GEN_SORT);
        existingField.put(SolrFields.SOLR_FIELD_MULTIVALUED, false);
        fields.add(existingField);
        
        TextAttribute mockAttribute = mock(TextAttribute.class);
        when(mockAttribute.getName()).thenReturn("existingField");
        attributes.add(mockAttribute);
        
        SolrFieldsChecker.CheckFieldsResult result = solrFieldsChecker.checkFields();
        
        assertTrue(result.needsUpdate());
        assertEquals(9, result.getFieldsToAdd().size());
        assertEquals(0, result.getFieldsToReplace().size());
        
        List<String> expectedAddedFieldCodes = new ArrayList<>(defaultFields);
        expectedAddedFieldCodes.addAll(List.of("en"));
        List<String> addedFieldCodesRes = result.getFieldsToAdd()
                .stream().map(m -> m.get(SolrFields.SOLR_FIELD_NAME).toString()).collect(Collectors.toList());
        assertTrue(addedFieldCodesRes.containsAll(expectedAddedFieldCodes));
    }
    
    @Test
    void testReplaceFieldWithDifferentType() {
        Map<String, Object> existingField = new HashMap<>();
        existingField.put(SolrFields.SOLR_FIELD_NAME, "en_existingField");
        existingField.put(SolrFields.SOLR_FIELD_TYPE, SolrFields.TYPE_TEXT_GEN_SORT);
        existingField.put(SolrFields.SOLR_FIELD_MULTIVALUED, false);
        fields.add(existingField);
        
        BooleanAttribute mockAttribute = mock(BooleanAttribute.class);
        when(mockAttribute.getName()).thenReturn("existingField");
        when(mockAttribute.isSearchable()).thenReturn(true);
        attributes.add(mockAttribute);
        
        SolrFieldsChecker.CheckFieldsResult result = solrFieldsChecker.checkFields();
        
        assertTrue(result.needsUpdate());
        assertEquals(9, result.getFieldsToAdd().size());
        assertEquals(1, result.getFieldsToReplace().size());
        
        List<String> expectedAddedFieldCodes = new ArrayList<>(defaultFields);
        expectedAddedFieldCodes.addAll(List.of("en"));
        List<String> addedFieldCodesRes = result.getFieldsToAdd()
                .stream().map(m -> m.get(SolrFields.SOLR_FIELD_NAME).toString()).collect(Collectors.toList());
        assertTrue(addedFieldCodesRes.containsAll(expectedAddedFieldCodes));
        
        Map<String, ?> replacedField = result.getFieldsToReplace().get(0);
        assertEquals("en_existingField", replacedField.get(SolrFields.SOLR_FIELD_NAME));
        assertEquals(SolrFields.TYPE_BOOLEAN, replacedField.get(SolrFields.SOLR_FIELD_TYPE));
        assertFalse((boolean) replacedField.get(SolrFields.SOLR_FIELD_MULTIVALUED));
    }
    
    @Test
    void testReplaceFieldWithDifferentMultifieldValue() {
        Map<String, Object> existingField = new HashMap<>();
        existingField.put(SolrFields.SOLR_FIELD_NAME, "en_existingDateField");
        existingField.put(SolrFields.SOLR_FIELD_TYPE, SolrFields.TYPE_PDATES);
        existingField.put(SolrFields.SOLR_FIELD_MULTIVALUED, true);
        fields.add(existingField);
        
        DateAttribute mockAttribute = mock(DateAttribute.class);
        when(mockAttribute.getName()).thenReturn("existingDateField");
        attributes.add(mockAttribute);
        
        SolrFieldsChecker.CheckFieldsResult result = solrFieldsChecker.checkFields();
        
        assertTrue(result.needsUpdate());
        assertEquals(9, result.getFieldsToAdd().size());
        assertEquals(1, result.getFieldsToReplace().size());
        
        List<String> expectedAddedFieldCodes = new ArrayList<>(defaultFields);
        expectedAddedFieldCodes.addAll(List.of("en"));
        List<String> addedFieldCodesRes = result.getFieldsToAdd()
                .stream().map(m -> m.get(SolrFields.SOLR_FIELD_NAME).toString()).collect(Collectors.toList());
        assertTrue(addedFieldCodesRes.containsAll(expectedAddedFieldCodes));
        
        Map<String, ?> replacedField = result.getFieldsToReplace().get(0);
        assertEquals("en_existingDateField", replacedField.get(SolrFields.SOLR_FIELD_NAME));
        assertEquals(SolrFields.TYPE_PDATES, replacedField.get(SolrFields.SOLR_FIELD_TYPE));
        assertFalse((boolean) replacedField.get(SolrFields.SOLR_FIELD_MULTIVALUED));
    }
    
}
