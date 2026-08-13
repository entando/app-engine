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
package com.agiletec.aps.system.common.entity.model.attribute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.entando.entando.aps.system.common.entity.search.SearchFieldType;
import java.math.BigDecimal;
import java.util.Date;
import org.junit.jupiter.api.Test;

/**
 * The per-attribute search field SPI: every attribute declares its own field type and value, so no
 * search back-end has to enumerate attribute types or remember their subclass relationships.
 *
 * <p>These tests pin two things the three Solr consumers used to restate independently: the type
 * mapping, and the <b>asymmetric eligibility</b> - an attribute that produces indexable content gets a
 * field whether or not it is flagged searchable, while the boolean family gets one only when it is.</p>
 */
class AttributeSearchFieldTest {

    @Test
    void textAttributeShouldReportTextAndItsIndexableContent() {
        MonoTextAttribute attribute = new MonoTextAttribute();
        attribute.setName("title");
        attribute.setText("hello", "en");
        attribute.setRenderingLang("en");
        assertEquals(SearchFieldType.TEXT, attribute.getSearchFieldType());
        assertEquals("hello", attribute.getSearchFieldValue());
    }

    @Test
    void dateAttributeShouldReportDateAndTheInstantItself() {
        DateAttribute attribute = new DateAttribute();
        Date date = new Date(0L);
        attribute.setDate(date);
        assertEquals(SearchFieldType.DATE, attribute.getSearchFieldType());
        assertEquals(date, attribute.getSearchFieldValue());
    }

    @Test
    void numberAttributeShouldReportNumberAndNarrowItsOwnValue() {
        NumberAttribute attribute = new NumberAttribute();
        attribute.setValue(new BigDecimal("42.7"));
        assertEquals(SearchFieldType.NUMBER, attribute.getSearchFieldType());
        // narrowed by the attribute, so no indexing site has to know it holds a BigDecimal
        assertEquals(42, attribute.getSearchFieldValue());
        assertNull(new NumberAttribute().getSearchFieldValue());
    }

    @Test
    void booleanAndCheckBoxShouldReportBooleanAndCoerceTheUnsetStateToFalse() {
        BooleanAttribute booleanAttribute = new BooleanAttribute();
        assertEquals(SearchFieldType.BOOLEAN, booleanAttribute.getSearchFieldType());
        assertEquals(Boolean.FALSE, booleanAttribute.getSearchFieldValue());
        booleanAttribute.setBooleanValue(Boolean.TRUE);
        assertEquals(Boolean.TRUE, booleanAttribute.getSearchFieldValue());

        CheckBoxAttribute checkBox = new CheckBoxAttribute();
        assertEquals(SearchFieldType.BOOLEAN, checkBox.getSearchFieldType());
        assertEquals(Boolean.FALSE, checkBox.getSearchFieldValue());
    }

    @Test
    void threeStateShouldReportTristateWithoutAnyoneTestingTheSubclassFirst() {
        // the LSP break made safe: ThreeStateAttribute IS a BooleanAttribute, but it answers TRISTATE,
        // so a consumer switching on the declared type can no longer mistake it for a plain boolean -
        // which is what every "test the subclass first" comment used to guard against
        ThreeStateAttribute unset = new ThreeStateAttribute();
        assertEquals(SearchFieldType.TRISTATE, unset.getSearchFieldType());
        assertEquals(ThreeStateAttribute.NOT_SET_SEARCH_VALUE, unset.getSearchFieldValue());

        ThreeStateAttribute valued = new ThreeStateAttribute();
        valued.setBooleanValue(Boolean.TRUE);
        assertEquals("true", valued.getSearchFieldValue());
        valued.setBooleanValue(Boolean.FALSE);
        assertEquals("false", valued.getSearchFieldValue());

        // and it is never confused with its superclass by an unordered dispatch
        assertEquals(SearchFieldType.BOOLEAN, new BooleanAttribute().getSearchFieldType());
    }

    @Test
    void anUnsetThreeStateStillHasSomethingToIndex() {
        // the guard IndexerDAO applies: getValue() is null, yet the unset state is a value to store
        ThreeStateAttribute unset = new ThreeStateAttribute();
        assertNull(unset.getValue());
        assertNull(new ThreeStateAttribute().getBooleanValue());
        assertEquals(ThreeStateAttribute.NOT_SET_SEARCH_VALUE, unset.getSearchFieldValue());
    }

    @Test
    void indexableContentShouldGetItsFieldEvenWhenNotFlaggedSearchable() {
        // long-standing platform behaviour, now stated once instead of in three ladders: Text, Date and
        // Number attributes all produce indexable content, so their field exists either way
        MonoTextAttribute text = new MonoTextAttribute();
        assertFalse(text.isSearchable());
        assertTrue(text.hasSearchField());

        DateAttribute date = new DateAttribute();
        assertFalse(date.isSearchable());
        assertTrue(date.hasSearchField());

        NumberAttribute number = new NumberAttribute();
        assertFalse(number.isSearchable());
        assertTrue(number.hasSearchField());
    }

    @Test
    void theBooleanFamilyShouldGetItsFieldOnlyWhenFlaggedSearchable() {
        for (BooleanAttribute attribute : new BooleanAttribute[]{new BooleanAttribute(),
            new CheckBoxAttribute(), new ThreeStateAttribute()}) {
            assertFalse(attribute.hasSearchField(),
                    attribute.getClass().getSimpleName() + " must not be indexed unless searchable");
            attribute.setSearchable(true);
            assertTrue(attribute.hasSearchField(),
                    attribute.getClass().getSimpleName() + " must be indexed when searchable");
        }
        assertTrue(SearchFieldType.BOOLEAN.isSearchableFlagRequired());
        assertTrue(SearchFieldType.TRISTATE.isSearchableFlagRequired());
        assertFalse(SearchFieldType.TEXT.isSearchableFlagRequired());
        assertFalse(SearchFieldType.DATE.isSearchableFlagRequired());
        assertFalse(SearchFieldType.NUMBER.isSearchableFlagRequired());
    }

    @Test
    void aComplexAttributeShouldDeclareNoSearchFieldOfItsOwn() {
        // a Composite is descended, never indexed as one value; a list likewise
        CompositeAttribute composite = new CompositeAttribute();
        assertNull(composite.getSearchFieldType());
        assertNull(composite.getSearchFieldValue());
        assertFalse(composite.hasSearchField());

        MonoListAttribute list = new MonoListAttribute();
        assertNull(list.getSearchFieldType());
        assertFalse(list.hasSearchField());
    }

    @Test
    void onlyTheBooleanFamilyShouldSupportNestedSearch() {
        assertTrue(new BooleanAttribute().isNestedSearchSupported());
        assertTrue(new CheckBoxAttribute().isNestedSearchSupported());
        assertTrue(new ThreeStateAttribute().isNestedSearchSupported());
        assertFalse(new MonoTextAttribute().isNestedSearchSupported());
        assertFalse(new DateAttribute().isNestedSearchSupported());
        assertFalse(new NumberAttribute().isNestedSearchSupported());
        assertFalse(new CompositeAttribute().isNestedSearchSupported());
    }

}
