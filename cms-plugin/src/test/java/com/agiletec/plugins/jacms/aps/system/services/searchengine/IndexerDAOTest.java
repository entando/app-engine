/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package com.agiletec.plugins.jacms.aps.system.services.searchengine;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CheckBoxAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.DateAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoTextAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.NumberAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.ThreeStateAttribute;
import com.agiletec.aps.system.common.searchengine.IndexableAttributeInterface;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The document this engine builds for one content's attributes.
 *
 * <p>The point of these tests is the <b>boundary between the two search engines</b>. This is the
 * baseline (Lucene) indexer, used when Solr is disabled, and it indexes only attributes that carry
 * indexable content: boolean-like attributes are deliberately absent, because on this engine attribute
 * filtering is served by the DB search tables. The Solr indexer does the opposite - it creates a field
 * per searchable boolean. Both behaviours are correct, and neither must be "harmonised" into the other
 * by someone reading only one of them.</p>
 */
class IndexerDAOTest {

    private IndexerDAO newIndexerDAO() {
        IndexerDAO indexerDAO = new IndexerDAO();
        ILangManager langManager = mock(ILangManager.class);
        Lang lang = new Lang();
        lang.setCode("en");
        lang.setDescr("English");
        when(langManager.getLangs()).thenReturn(List.of(lang));
        indexerDAO.setLangManager(langManager);
        return indexerDAO;
    }

    private Content newContent(AttributeInterface... attributes) {
        Content content = new Content();
        content.setId("ART1");
        content.setTypeCode("ART");
        content.setMainGroup("free");
        content.setDefaultLang("en");
        for (AttributeInterface attribute : attributes) {
            content.addAttribute(attribute);
        }
        return content;
    }

    @Test
    void shouldNotIndexBooleanLikeAttributes() throws Exception {
        // the deliberate divergence: switching the eligibility test to the engine-wide
        // attribute.hasSearchField() would add these three fields to every document
        BooleanAttribute flag = new BooleanAttribute();
        flag.setName("flag");
        flag.setType("Boolean");
        flag.setSearchable(true);
        flag.setBooleanValue(Boolean.TRUE);

        CheckBoxAttribute check = new CheckBoxAttribute();
        check.setName("check");
        check.setType("CheckBox");
        check.setSearchable(true);
        check.setBooleanValue(Boolean.TRUE);

        ThreeStateAttribute tri = new ThreeStateAttribute();
        tri.setName("tri");
        tri.setType("ThreeState");
        tri.setSearchable(true);

        Document document = newIndexerDAO().createDocument(newContent(flag, check, tri));

        Assertions.assertNull(document.get("en_flag"), "booleans are DB-served on this engine");
        Assertions.assertNull(document.get("en_check"));
        Assertions.assertNull(document.get("en_tri"));
        Assertions.assertNull(document.get("en"), "and they must not reach the full-text field either");
    }

    @Test
    void shouldIndexTextAttributeAsItsOwnFieldAndAsFullText() throws Exception {
        MonoTextAttribute title = new MonoTextAttribute();
        title.setName("title");
        title.setType("Monotext");
        title.setIndexingType(IndexableAttributeInterface.INDEXING_TYPE_TEXT);
        title.setText("Press release");

        Document document = newIndexerDAO().createDocument(newContent(title));

        Assertions.assertEquals("press release", document.get("en_title"));
        Assertions.assertEquals("Press release", document.get("en"));
    }

    @Test
    void shouldIndexDateAndNumberEvenWhenNotFlaggedSearchable() throws Exception {
        // Date and Number implement IndexableAttributeInterface, so the "&& isSearchable()" clause the
        // eligibility test used to carry could never add anything - dropping it changed nothing. This
        // pins that, so the simplification cannot be misread as a behaviour change.
        DateAttribute date = new DateAttribute();
        date.setName("start");
        date.setType("Date");
        date.setIndexingType(IndexableAttributeInterface.INDEXING_TYPE_NONE);
        date.setDate(new Date(0L));
        Assertions.assertFalse(date.isSearchable());

        NumberAttribute number = new NumberAttribute();
        number.setName("price");
        number.setType("Number");
        number.setIndexingType(IndexableAttributeInterface.INDEXING_TYPE_NONE);
        number.setValue(new BigDecimal("42"));
        Assertions.assertFalse(number.isSearchable());

        Document document = newIndexerDAO().createDocument(newContent(date, number));

        Assertions.assertNotNull(document.get("en_start"), "a date field is indexed regardless of the flag");
        Assertions.assertEquals("42", document.get("en_price"));
    }

    @Test
    void shouldIndexANumberTooLargeForAnIntWithoutTruncating() throws Exception {
        // why this engine keeps its own value extraction instead of reusing getSearchFieldValue():
        // that method narrows a number to int for the per-attribute field, which would truncate here
        NumberAttribute big = new NumberAttribute();
        big.setName("big");
        big.setType("Number");
        big.setIndexingType(IndexableAttributeInterface.INDEXING_TYPE_NONE);
        big.setValue(new BigDecimal("3000000000"));

        Document document = newIndexerDAO().createDocument(newContent(big));

        Assertions.assertEquals("3000000000", document.get("en_big"));
    }

}
