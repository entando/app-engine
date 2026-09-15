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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CheckBoxAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoListAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.ThreeStateAttribute;
import com.agiletec.aps.system.services.lang.Lang;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SolrFieldsCheckerTest {

    private Lang lang(String code) {
        Lang lang = new Lang();
        lang.setCode(code);
        lang.setDescr(code);
        return lang;
    }

    private SolrFieldsChecker checker(List<AttributeInterface> attributes, Lang... langs) {
        return new SolrFieldsChecker(Collections.emptyList(), attributes, List.of(langs));
    }

    @Test
    void shouldCreateMainAndAttachmentFieldPerLanguage() {
        // No pre-existing fields, no content-type attributes: only the base + per-language
        // fields are generated. Each language must yield BOTH the main "<lang>" field and the
        // "<lang>_attachment" field, so a full-text search with includeAttachments=true never
        // queries a field the schema doesn't know about.
        SolrFieldsChecker checker = this.checker(
                Collections.<AttributeInterface>emptyList(), lang("en"), lang("it"));

        List<String> createdFieldNames = checker.checkFields().getFieldsToAdd().stream()
                .map(field -> (String) field.get(SolrFields.SOLR_FIELD_NAME))
                .collect(Collectors.toList());

        assertTrue(createdFieldNames.contains("en"), createdFieldNames.toString());
        assertTrue(createdFieldNames.contains("it"), createdFieldNames.toString());
        assertTrue(createdFieldNames.contains("en" + SolrFields.ATTACHMENT_FIELD_SUFFIX),
                createdFieldNames.toString());
        assertTrue(createdFieldNames.contains("it" + SolrFields.ATTACHMENT_FIELD_SUFFIX),
                createdFieldNames.toString());
    }

    @Test
    void attachmentFieldMatchesMainLanguageFieldTypeAndMultiplicity() {
        SolrFieldsChecker checker = this.checker(
                Collections.<AttributeInterface>emptyList(), lang("en"));

        List<Map<String, ?>> added = checker.checkFields().getFieldsToAdd();
        Map<String, ?> main = added.stream()
                .filter(f -> "en".equals(f.get(SolrFields.SOLR_FIELD_NAME))).findFirst().orElseThrow();
        Map<String, ?> attachment = added.stream()
                .filter(f -> ("en" + SolrFields.ATTACHMENT_FIELD_SUFFIX).equals(f.get(SolrFields.SOLR_FIELD_NAME)))
                .findFirst().orElseThrow();

        Assertions.assertEquals(main.get(SolrFields.SOLR_FIELD_TYPE),
                attachment.get(SolrFields.SOLR_FIELD_TYPE));
        Assertions.assertEquals(main.get(SolrFields.SOLR_FIELD_MULTIVALUED),
                attachment.get(SolrFields.SOLR_FIELD_MULTIVALUED));
    }

    /** Exposes the protected {@code addAttribute} so tests can build composite fixtures. */
    private static class TestComposite extends CompositeAttribute {

        void addChild(AttributeInterface attribute) {
            this.addAttribute(attribute);
        }
    }

    private BooleanAttribute booleanAttribute(String name, boolean searchable) {
        BooleanAttribute attribute = new BooleanAttribute();
        attribute.setName(name);
        attribute.setType("Boolean");
        attribute.setSearchable(searchable);
        return attribute;
    }

    private CheckBoxAttribute checkBoxAttribute(String name, boolean searchable) {
        CheckBoxAttribute attribute = new CheckBoxAttribute();
        attribute.setName(name);
        attribute.setType("CheckBox");
        attribute.setSearchable(searchable);
        return attribute;
    }

    private ThreeStateAttribute threeStateAttribute(String name, boolean searchable) {
        ThreeStateAttribute attribute = new ThreeStateAttribute();
        attribute.setName(name);
        attribute.setType("ThreeState");
        attribute.setSearchable(searchable);
        return attribute;
    }


    private TestComposite composite(String name, AttributeInterface... children) {
        TestComposite composite = new TestComposite();
        composite.setName(name);
        composite.setType("Composite");
        for (AttributeInterface child : children) {
            composite.addChild(child);
        }
        return composite;
    }

    // ---- top-level booleans: unchanged, still gated by the "searchable" flag ----

    @Test
    void shouldCreateSingleValuedBooleanFieldForSearchableTopLevelAttribute() {
        SolrFieldsChecker checker = this.checker(List.of(booleanAttribute("myFlag", true)), lang("en"));

        Map<String, ?> field = fieldsToAdd(checker, "en_myFlag");
        Assertions.assertEquals(SolrFields.TYPE_BOOLEAN, field.get(SolrFields.SOLR_FIELD_TYPE));
        Assertions.assertEquals(false, field.get(SolrFields.SOLR_FIELD_MULTIVALUED));
    }

    @Test
    void shouldNotCreateFieldForNonSearchableTopLevelBoolean() {
        SolrFieldsChecker checker = this.checker(List.of(booleanAttribute("myFlag", false)), lang("en"));

        assertFalse(fieldNames(checker).contains("en_myFlag"), fieldNames(checker).toString());
    }

    @Test
    void shouldCreateBooleanFieldForSearchableTopLevelCheckBoxAttribute() {
        SolrFieldsChecker checker = this.checker(List.of(checkBoxAttribute("myCheck", true)), lang("en"));

        Map<String, ?> field = fieldsToAdd(checker, "en_myCheck");
        Assertions.assertEquals(SolrFields.TYPE_BOOLEAN, field.get(SolrFields.SOLR_FIELD_TYPE));
        Assertions.assertEquals(false, field.get(SolrFields.SOLR_FIELD_MULTIVALUED));
    }

    @Test
    void shouldCreateStringFieldForSearchableTopLevelThreeStateAttribute() {
        // ThreeState is indexed as a Solr "string" (not "boolean"): its third, uninitialized state
        // cannot be represented in a two-valued BoolField. Pins the ThreeState-before-Boolean
        // dispatch order.
        SolrFieldsChecker checker = this.checker(List.of(threeStateAttribute("myFlag3", true)), lang("en"));

        Map<String, ?> field = fieldsToAdd(checker, "en_myFlag3");
        Assertions.assertEquals(SolrFields.TYPE_STRING, field.get(SolrFields.SOLR_FIELD_TYPE));
        Assertions.assertEquals(false, field.get(SolrFields.SOLR_FIELD_MULTIVALUED));
    }

    @Test
    void shouldNotCreateFieldForTopLevelAttributeOfUnsupportedType() {
        // Closes the final "instanceof BooleanAttribute" false outcome of
        // isIndexableAttributeField: an attribute that is neither IndexableAttributeInterface,
        // Date, Number nor Boolean must not produce a field.
        AttributeInterface unsupported = mock(AttributeInterface.class);
        when(unsupported.isSimple()).thenReturn(true);
        when(unsupported.getName()).thenReturn("unsupported");

        SolrFieldsChecker checker = this.checker(List.of(unsupported), lang("en"));

        List<String> names = fieldNames(checker);
        assertFalse(names.stream().anyMatch(n -> n.contains("unsupported")), names.toString());
    }

    @Test
    void shouldSkipNonComplexAttributeChildrenEvenWhenNotSimple() {
        // Defensive branch of the shared traversal (NestedSearchSupport
        // .forEachIndexableNested): an attribute that reports isSimple()==false but is not a
        // CompositeAttribute (unlike every real Composite/List attribute) must be skipped rather
        // than throw a ClassCastException.
        AttributeInterface fakeComplexAttribute = mock(AttributeInterface.class);
        when(fakeComplexAttribute.isSimple()).thenReturn(false);
        when(fakeComplexAttribute.getName()).thenReturn("fake");

        SolrFieldsChecker checker = this.checker(List.of(fakeComplexAttribute), lang("en"));

        List<String> names = fieldNames(checker);
        assertFalse(names.stream().anyMatch(n -> n.contains("fake")), names.toString());
    }

    // ---- composite children: qualified path, single-valued, gated by the inherited searchable flag ----
    // Single-valued because a Composite occurs at most once per document per lang: Monolist and
    // Monolist-of-Composite ancestry are excluded below, so the field can never repeat.

    @Test
    void shouldCreateSingleValuedQualifiedFieldForSearchableCompositeChild() {
        TestComposite composite = composite("myComposite", booleanAttribute("featured", true));
        SolrFieldsChecker checker = this.checker(List.of(composite), lang("en"));

        Map<String, ?> field = fieldsToAdd(checker, "en_myComposite_featured");
        Assertions.assertEquals(SolrFields.TYPE_BOOLEAN, field.get(SolrFields.SOLR_FIELD_TYPE));
        Assertions.assertEquals(false, field.get(SolrFields.SOLR_FIELD_MULTIVALUED));
    }

    @Test
    void shouldNotCreateFieldForNonSearchableCompositeChild() {
        TestComposite composite = composite("myComposite", booleanAttribute("featured", false));
        SolrFieldsChecker checker = this.checker(List.of(composite), lang("en"));

        assertFalse(fieldNames(checker).contains("en_myComposite_featured"), fieldNames(checker).toString());
    }

    @Test
    void shouldCreateQualifiedFieldForCompositeChildCheckBoxAttribute() {
        TestComposite composite = composite("myComposite", checkBoxAttribute("featuredCheck", true));
        SolrFieldsChecker checker = this.checker(List.of(composite), lang("en"));

        Map<String, ?> field = fieldsToAdd(checker, "en_myComposite_featuredCheck");
        Assertions.assertEquals(SolrFields.TYPE_BOOLEAN, field.get(SolrFields.SOLR_FIELD_TYPE));
        Assertions.assertEquals(false, field.get(SolrFields.SOLR_FIELD_MULTIVALUED));
    }

    @Test
    void shouldCreateStringFieldForCompositeChildThreeStateAttribute() {
        TestComposite composite = composite("myComposite", threeStateAttribute("featured3", true));
        SolrFieldsChecker checker = this.checker(List.of(composite), lang("en"));

        Map<String, ?> field = fieldsToAdd(checker, "en_myComposite_featured3");
        Assertions.assertEquals(SolrFields.TYPE_STRING, field.get(SolrFields.SOLR_FIELD_TYPE));
        Assertions.assertEquals(false, field.get(SolrFields.SOLR_FIELD_MULTIVALUED));
    }

    @Test
    void shouldCreateFullPathFieldForBooleanInNestedComposite() {
        TestComposite inner = composite("inner", booleanAttribute("boolAttrName", true));
        TestComposite outer = composite("outer", inner);
        SolrFieldsChecker checker = this.checker(List.of(outer), lang("en"));

        Map<String, ?> field = fieldsToAdd(checker, "en_outer_inner_boolAttrName");
        Assertions.assertEquals(SolrFields.TYPE_BOOLEAN, field.get(SolrFields.SOLR_FIELD_TYPE));
        Assertions.assertEquals(false, field.get(SolrFields.SOLR_FIELD_MULTIVALUED));
    }

    // ---- List / Monolist: excluded, no boolean field regardless of the flag ----

    @Test
    void shouldNotCreateFieldForBooleanElementOfMonolist() {
        BooleanAttribute nestedType = booleanAttribute("myList", true);
        MonoListAttribute list = new MonoListAttribute();
        list.setName("myList");
        list.setType("Monolist");
        list.setNestedAttributeType(nestedType);

        SolrFieldsChecker checker = this.checker(List.of(list), lang("en"));

        assertFalse(fieldNames(checker).contains("en_myList"), fieldNames(checker).toString());
    }

    @Test
    void shouldNotCreateFieldForBooleanInsideCompositeNestedInMonolist() {
        TestComposite compositePrototype = composite("comboList", booleanAttribute("subFlag", true));
        MonoListAttribute listOfComposite = new MonoListAttribute();
        listOfComposite.setName("comboList");
        listOfComposite.setType("Monolist");
        listOfComposite.setNestedAttributeType(compositePrototype);

        SolrFieldsChecker checker = this.checker(List.of(listOfComposite), lang("en"));

        List<String> names = fieldNames(checker);
        assertFalse(names.contains("en_comboList_subFlag"), names.toString());
        assertFalse(names.contains("en_subFlag"), names.toString());
    }

    private List<String> fieldNames(SolrFieldsChecker checker) {
        return checker.checkFields().getFieldsToAdd().stream()
                .map(field -> (String) field.get(SolrFields.SOLR_FIELD_NAME))
                .toList();
    }

    private Map<String, ?> fieldsToAdd(SolrFieldsChecker checker, String fieldName) {
        return checker.checkFields().getFieldsToAdd().stream()
                .filter(f -> fieldName.equals(f.get(SolrFields.SOLR_FIELD_NAME)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Field not created: " + fieldName));
    }
}
