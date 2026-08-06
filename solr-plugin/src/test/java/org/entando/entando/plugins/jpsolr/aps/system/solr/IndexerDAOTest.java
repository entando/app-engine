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

import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CheckBoxAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoListAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoTextAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.ThreeStateAttribute;
import com.agiletec.aps.system.common.searchengine.IndexableAttributeInterface;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.ImageAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IndexerDAOTest {

    private IndexerDAO newIndexerDAO() {
        IndexerDAO indexerDAO = new IndexerDAO(mock(SolrClient.class), "core");
        ILangManager langManager = mock(ILangManager.class);
        when(langManager.getLangs()).thenReturn(List.of(lang("en")));
        indexerDAO.setLangManager(langManager);
        return indexerDAO;
    }

    private Lang lang(String code) {
        Lang lang = new Lang();
        lang.setCode(code);
        lang.setDescr(code);
        return lang;
    }

    private Content newContent() {
        Content content = new Content();
        content.setId("1");
        content.setTypeCode("TST");
        content.setMainGroup("free");
        return content;
    }

    private BooleanAttribute booleanAttribute(String name, boolean searchable, Boolean value) {
        BooleanAttribute attribute = new BooleanAttribute();
        attribute.setName(name);
        attribute.setType("Boolean");
        attribute.setSearchable(searchable);
        attribute.setBooleanValue(value);
        return attribute;
    }

    private ThreeStateAttribute threeStateAttribute(String name, boolean searchable, Boolean value) {
        ThreeStateAttribute attribute = new ThreeStateAttribute();
        attribute.setName(name);
        attribute.setType("ThreeState");
        attribute.setSearchable(searchable);
        attribute.setBooleanValue(value);
        return attribute;
    }

    private CheckBoxAttribute checkBoxAttribute(String name, boolean searchable, Boolean value) {
        CheckBoxAttribute attribute = new CheckBoxAttribute();
        attribute.setName(name);
        attribute.setType("CheckBox");
        attribute.setSearchable(searchable);
        attribute.setBooleanValue(value);
        return attribute;
    }

    private MonoTextAttribute fullTextMonoText(String name, String text) {
        MonoTextAttribute attribute = new MonoTextAttribute();
        attribute.setName(name);
        attribute.setType("Monotext");
        attribute.setIndexingType(IndexableAttributeInterface.INDEXING_TYPE_TEXT);
        attribute.setText(text);
        return attribute;
    }

    /** Exposes the protected {@code addAttribute} so tests can build composite fixtures. */
    private static class TestComposite extends CompositeAttribute {

        void addChild(AttributeInterface attribute) {
            this.addAttribute(attribute);
        }
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
    void shouldIndexSearchableTopLevelBooleanAttribute() {
        Content content = newContent();
        content.addAttribute(booleanAttribute("myFlag", true, Boolean.TRUE));

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Assertions.assertEquals(Boolean.TRUE, document.getFieldValue("en_myFlag"));
        Assertions.assertNull(document.getField("en"), "boolean values must not pollute the full-text field");
    }

    @Test
    void shouldIndexFalseTopLevelBooleanAttribute() {
        Content content = newContent();
        content.addAttribute(booleanAttribute("myFlag", true, Boolean.FALSE));

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Assertions.assertEquals(Boolean.FALSE, document.getFieldValue("en_myFlag"));
    }

    @Test
    void shouldNotIndexAnEmptyResourceAttribute() {
        // Regression guard. An Image/Attach/Link with nothing set has a NULL getValue() but a non-null
        // (empty-string) indexable value, so a "has something to index" test written against the value
        // instead of against the field TYPE stops skipping it - and starts adding empty entries to the
        // full-text field that this indexer has never added. Pins the skip.
        ImageAttribute image = new ImageAttribute();
        image.setName("photo");
        image.setType("Image");
        image.setIndexingType(IndexableAttributeInterface.INDEXING_TYPE_TEXT);
        Assertions.assertNull(image.getValue(), "an unset resource attribute has no value");
        Assertions.assertEquals("", image.getSearchFieldValue(),
                "but its indexable value is empty, not null - which is what makes this a trap");

        Content content = newContent();
        content.addAttribute(image);

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Assertions.assertNull(document.getField("en"),
                "an unset resource attribute must add nothing to the full-text field");
        Assertions.assertNull(document.getField("en_attachment"));
        Assertions.assertNull(document.getField("en_photo"));
    }

    @Test
    void shouldNotIndexNonSearchableTopLevelBoolean() {
        Content content = newContent();
        content.addAttribute(booleanAttribute("myFlag", false, Boolean.TRUE));

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Assertions.assertNull(document.getField("en_myFlag"));
    }

    @Test
    void shouldIndexUnsetThreeStateAttributeAsNoneLiteral() {
        // Unlike a plain Boolean/CheckBox (which coerces null to false), an unset ThreeState must
        // reach the index as the literal "none" string so it stays distinguishable and queryable.
        Content content = newContent();
        content.addAttribute(threeStateAttribute("flag3", true, null));

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Assertions.assertEquals("none", document.getFieldValue("en_flag3"));
    }

    @Test
    void shouldIndexTrueTopLevelThreeStateAttributeAsStringLiteral() {
        Content content = newContent();
        content.addAttribute(threeStateAttribute("flag3", true, Boolean.TRUE));

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Assertions.assertEquals("true", document.getFieldValue("en_flag3"));
    }

    @Test
    void shouldIndexSearchableTopLevelCheckBoxAttribute() {
        Content content = newContent();
        content.addAttribute(checkBoxAttribute("myCheck", true, Boolean.TRUE));

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Assertions.assertEquals(Boolean.TRUE, document.getFieldValue("en_myCheck"));
    }

    @Test
    void shouldIndexSearchableTopLevelThreeStateAttributeWithFalseValue() {
        Content content = newContent();
        content.addAttribute(threeStateAttribute("flag3", true, Boolean.FALSE));

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Assertions.assertEquals("false", document.getFieldValue("en_flag3"));
    }

    @Test
    void shouldNotIndexTopLevelAttributeOfUnsupportedType() {
        // Closes the final "instanceof BooleanAttribute" false outcome of indexAttribute's
        // top-level dispatch condition: an attribute that is neither IndexableAttributeInterface,
        // Date, Number nor Boolean must be skipped entirely rather than throw.
        Content content = newContent();
        AttributeInterface unsupported = mock(AttributeInterface.class);
        when(unsupported.isSimple()).thenReturn(true);
        when(unsupported.getName()).thenReturn("unsupported");
        when(unsupported.getValue()).thenReturn("nonNullValue");
        content.addAttribute(unsupported);

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Assertions.assertNull(document.getField("en_unsupported"));
    }

    // ---- composite children: qualified path, gated by the inherited searchable flag ----

    @Test
    void shouldIndexSearchableBooleanChildOfCompositeUnderQualifiedName() {
        Content content = newContent();
        content.addAttribute(composite("myComposite", booleanAttribute("featured", true, Boolean.TRUE)));

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Assertions.assertEquals(Boolean.TRUE, document.getFieldValue("en_myComposite_featured"));
    }

    @Test
    void shouldNotIndexNonSearchableCompositeChild() {
        Content content = newContent();
        content.addAttribute(composite("myComposite", booleanAttribute("featured", false, Boolean.TRUE)));

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Assertions.assertNull(document.getField("en_myComposite_featured"));
    }

    @Test
    void shouldIndexCheckBoxChildOfCompositeUnderQualifiedName() {
        Content content = newContent();
        content.addAttribute(composite("myComposite", checkBoxAttribute("featuredCheck", true, Boolean.TRUE)));

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Assertions.assertEquals(Boolean.TRUE, document.getFieldValue("en_myComposite_featuredCheck"));
    }

    @Test
    void shouldIndexValuedThreeStateChildOfCompositeUnderQualifiedName() {
        Content content = newContent();
        content.addAttribute(composite("myComposite", threeStateAttribute("featured3", true, Boolean.FALSE)));

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Assertions.assertEquals("false", document.getFieldValue("en_myComposite_featured3"));
    }

    @Test
    void shouldIndexUnsetThreeStateChildOfCompositeAsNoneLiteral() {
        Content content = newContent();
        content.addAttribute(composite("myComposite", threeStateAttribute("featured3", true, null)));

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Assertions.assertEquals("none", document.getFieldValue("en_myComposite_featured3"));
    }

    @Test
    void shouldIndexBooleanInNestedCompositeUnderFullPath() {
        Content content = newContent();
        TestComposite inner = composite("inner", booleanAttribute("boolAttrName", true, Boolean.TRUE));
        content.addAttribute(composite("outer", inner));

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Assertions.assertEquals(Boolean.TRUE, document.getFieldValue("en_outer_inner_boolAttrName"));
    }

    // ---- List / Monolist: excluded from boolean indexing ----

    @Test
    void shouldNotIndexBooleanElementsOfMonolist() {
        Content content = newContent();
        MonoListAttribute list = new MonoListAttribute();
        list.setName("myList");
        list.setType("Monolist");
        list.getAttributes().add(booleanAttribute("myList", true, Boolean.TRUE));
        list.getAttributes().add(booleanAttribute("myList", true, Boolean.FALSE));
        content.addAttribute(list);

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Assertions.assertNull(document.getField("en_myList"));
    }

    @Test
    void shouldNotIndexBooleanInCompositeNestedInMonolist() {
        Content content = newContent();
        MonoListAttribute list = new MonoListAttribute();
        list.setName("comboList");
        list.setType("Monolist");
        list.getAttributes().add(composite("comboList", booleanAttribute("subFlag", true, Boolean.TRUE)));
        content.addAttribute(list);

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Assertions.assertNull(document.getField("en_comboList_subFlag"));
        Assertions.assertNull(document.getField("en_subFlag"));
    }

    @Test
    void shouldStillFullTextIndexTextChildrenInsideMonolist() {
        // Regression guard: the list exclusion gates only the per-attribute boolean branch; text
        // children inside a list must still feed the full-text "<lang>" field.
        Content content = newContent();
        MonoListAttribute list = new MonoListAttribute();
        list.setName("notes");
        list.setType("Monolist");
        list.getAttributes().add(fullTextMonoText("notes", "hello"));
        content.addAttribute(list);

        SolrInputDocument document = newIndexerDAO().createDocument(content);

        Collection<Object> fullText = document.getFieldValues("en");
        Assertions.assertNotNull(fullText, "text children in a list must still feed the full-text field");
        Assertions.assertTrue(new ArrayList<>(fullText).contains("hello"), String.valueOf(fullText));
    }
}
