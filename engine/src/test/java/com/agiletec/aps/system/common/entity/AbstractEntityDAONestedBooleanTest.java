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
package com.agiletec.aps.system.common.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.common.entity.model.ApsEntity;
import com.agiletec.aps.system.common.entity.model.ApsEntityRecord;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CheckBoxAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.DateAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoListAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.TextAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.ThreeStateAttribute;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Verifies the {@code attrname} values that {@link AbstractEntityDAO#addEntitySearchRecord} writes to
 * the DB search tables. A boolean-like attribute nested in a Composite is stored under the path key
 * {@code <composite>_<boolean>}; the same attribute reached through a List/Monolist is not stored at
 * all (it cannot be path-qualified and no engine can read it); everything else keeps its historical
 * unqualified name.
 */
class AbstractEntityDAONestedBooleanTest {

    private TestEntityDAO dao;
    private PreparedStatement stat;

    @BeforeEach
    void setUp() {
        this.dao = new TestEntityDAO();
        ILangManager langManager = mock(ILangManager.class);
        Lang en = new Lang();
        en.setCode("en");
        en.setDescr("English");
        when(langManager.getLangs()).thenReturn(Collections.singletonList(en));
        this.dao.setLangManager(langManager);
        this.stat = mock(PreparedStatement.class);
    }

    @Test
    void topLevelSearchableBooleanKeepsPlainName() throws Throwable {
        assertEquals(List.of("flag"),
                writtenAttrNames(entity(booleanAttr("flag", true, Boolean.TRUE))));
    }

    @Test
    void compositeSearchableBooleanUsesPathName() throws Throwable {
        assertEquals(List.of("address_certified"),
                writtenAttrNames(entity(composite("address", booleanAttr("certified", true, Boolean.TRUE)))));
    }

    @Test
    void compositeNonSearchableBooleanIsNotIndexed() throws Throwable {
        assertEquals(List.of(),
                writtenAttrNames(entity(composite("address", booleanAttr("certified", false, Boolean.TRUE)))));
    }

    @Test
    void deepCompositeBooleanUsesFullPathName() throws Throwable {
        assertEquals(List.of("a_b_c"),
                writtenAttrNames(entity(composite("a", composite("b", booleanAttr("c", true, Boolean.TRUE))))));
    }

    @Test
    void nestedCheckBoxIsPathIndexed() throws Throwable {
        // CheckBox is a boolean-like -> path-qualified like Boolean (value null -> "false")
        assertEquals(List.of("address_verified"),
                writtenAttrNames(entity(composite("address", checkBox("verified", true)))));
    }

    @Test
    void nestedThreeStateIsPathIndexed() throws Throwable {
        ThreeStateAttribute maybe = threeState("maybe", true);
        maybe.setBooleanValue(Boolean.TRUE); // non-null so ThreeState produces a search row
        assertEquals(List.of("address_maybe"),
                writtenAttrNames(entity(composite("address", maybe))));
    }

    @Test
    void listReachedBooleanKeepsPlainName() throws Throwable {
        // A Monolist whose nested type is a Boolean predates nested boolean search: its elements were
        // always indexed under the unqualified name, and that record is still queryable over REST.
        assertEquals(List.of("flag"),
                writtenAttrNames(entity(monolist("tags", booleanAttr("flag", true, Boolean.TRUE)))));
    }

    @Test
    void listReachedBooleanNestedInCompositeKeepsPlainName() throws Throwable {
        // Composite -> Monolist -> Boolean: the boolean's parent is the list, not the Composite, so it
        // was never affected by the composite-child clobber and keeps its historical plain name.
        assertEquals(List.of("flag"),
                writtenAttrNames(entity(composite("wrapper", monolist("tags", booleanAttr("flag", true, Boolean.TRUE))))));
    }

    @Test
    void compositeBooleanReachedThroughAListIsNotIndexed() throws Throwable {
        // Monolist -> Composite -> Boolean: only expressible since Composite children keep their
        // searchable flag. It cannot be path-qualified (the list repeats) and an unqualified record
        // would collide with a top-level attribute, so nothing is written.
        assertEquals(List.of(),
                writtenAttrNames(entity(monolist("rows", composite("row", booleanAttr("active", true, Boolean.TRUE))))));
    }

    @Test
    void compositeBooleanReachedThroughAListDoesNotPolluteASameNamedTopLevelAttribute() throws Throwable {
        ApsEntity entity = entity(
                booleanAttr("active", true, Boolean.FALSE),
                monolist("rows", composite("row", booleanAttr("active", true, Boolean.TRUE))));
        // exactly one record, carrying the top-level value - no false positive for "active = true"
        assertEquals(List.of("active"), writtenAttrNames(entity));
        verify(this.stat, atLeast(1)).setString(3, "false");
        verify(this.stat, never()).setString(3, "true");
    }

    @Test
    void compositeCheckBoxAndThreeStateReachedThroughAListAreNotIndexed() throws Throwable {
        ThreeStateAttribute maybe = threeState("maybe", true);
        maybe.setBooleanValue(Boolean.TRUE);
        assertEquals(List.of(), writtenAttrNames(entity(
                monolist("rows", composite("row", checkBox("verified", true), maybe)))));
    }

    @Test
    void nonBooleanCompositeChildReachedThroughAListKeepsPlainName() throws Throwable {
        // the skip is scoped to boolean-likes: other searchable types under a list are untouched
        assertEquals(List.of("note"),
                writtenAttrNames(entity(monolist("rows", composite("row", textAttr("note", true, "hello"))))));
    }

    @Test
    void mixedEntityWritesEachAttributeUnderItsExpectedName() throws Throwable {
        ApsEntity entity = entity(
                booleanAttr("published", true, Boolean.TRUE),
                composite("address", booleanAttr("certified", true, Boolean.TRUE)));
        assertEquals(List.of("published", "address_certified"), writtenAttrNames(entity));
    }

    @Test
    void nestedNonBooleanSimpleAttributeKeepsPlainName() throws Throwable {
        // a non boolean-like attribute nested in a Composite is never path-qualified, even if searchable
        assertEquals(List.of("note"),
                writtenAttrNames(entity(composite("address", textAttr("note", true, "hello")))));
    }

    @Test
    void searchableAttributeWithNoSearchInfosIsNotIndexed() throws Throwable {
        // DateAttribute.getSearchInfos() returns null when no date is set
        assertEquals(List.of(), writtenAttrNames(entity(dateAttr("published", true, null))));
    }

    @Test
    void searchableDateAttributeWritesTimestamp() throws Throwable {
        Date date = new Date();
        assertEquals(List.of("published"), writtenAttrNames(entity(dateAttr("published", true, date))));
        verify(this.stat, atLeast(1)).setTimestamp(eq(4), any(Timestamp.class));
    }

    @Test
    void nullChildrenListIsSkippedWithoutError() throws Throwable {
        CompositeAttribute compositeWithNullChildren = spy(composite("address", booleanAttr("certified", true, Boolean.TRUE)));
        when(compositeWithNullChildren.getAttributes()).thenReturn(null);
        assertEquals(List.of(), writtenAttrNames(entity(compositeWithNullChildren)));
        verify(this.stat, never()).setString(eq(2), any());
    }

    @Test
    void indexingANullEntityFailsWithAStatedPrecondition() {
        // The search-key helper tolerates a null entity (its admin-form callers may have no prototype),
        // so this writer says explicitly that it does not - rather than dereferencing and hoping.
        NullPointerException thrown = assertThrows(NullPointerException.class,
                () -> this.dao.addSearchRecords("ENTITY1", null, this.stat));
        assertEquals("entity to index", thrown.getMessage());
    }

    private List<String> writtenAttrNames(IApsEntity entity) throws Throwable {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        this.dao.addSearchRecords("ENTITY1", entity, this.stat);
        verify(this.stat, atLeast(0)).setString(eq(2), captor.capture());
        return captor.getAllValues();
    }

    // --- fixtures ----------------------------------------------------------

    private BooleanAttribute booleanAttr(String name, boolean searchable, Boolean value) {
        BooleanAttribute a = new BooleanAttribute();
        a.setName(name);
        a.setSearchable(searchable);
        a.setBooleanValue(value);
        return a;
    }

    private CheckBoxAttribute checkBox(String name, boolean searchable) {
        CheckBoxAttribute a = new CheckBoxAttribute();
        a.setName(name);
        a.setSearchable(searchable);
        return a;
    }

    private ThreeStateAttribute threeState(String name, boolean searchable) {
        ThreeStateAttribute a = new ThreeStateAttribute();
        a.setName(name);
        a.setSearchable(searchable);
        return a;
    }

    private TextAttribute textAttr(String name, boolean searchable, String text) {
        TextAttribute a = new TextAttribute();
        a.setName(name);
        a.setSearchable(searchable);
        a.setText(text, "en");
        return a;
    }

    private DateAttribute dateAttr(String name, boolean searchable, Date date) {
        DateAttribute a = new DateAttribute();
        a.setName(name);
        a.setSearchable(searchable);
        a.setDate(date);
        return a;
    }

    private CompositeAttribute composite(String name, AttributeInterface... children) {
        CompositeAttribute c = new CompositeAttribute();
        c.setName(name);
        for (AttributeInterface child : children) {
            c.getAttributes().add(child);
        }
        return c;
    }

    private MonoListAttribute monolist(String name, AttributeInterface... elements) {
        MonoListAttribute list = new MonoListAttribute();
        list.setName(name);
        for (AttributeInterface element : elements) {
            list.getAttributes().add(element);
        }
        return list;
    }

    private ApsEntity entity(AttributeInterface... attributes) {
        ApsEntity entity = new ApsEntity();
        entity.setTypeCode("TST");
        for (AttributeInterface attribute : attributes) {
            entity.addAttribute(attribute);
        }
        return entity;
    }

    /**
     * Minimal concrete {@link AbstractEntityDAO} exposing the protected search-record writer.
     */
    private static class TestEntityDAO extends AbstractEntityDAO {

        public void addSearchRecords(String id, IApsEntity entity, PreparedStatement stat) throws Throwable {
            this.addEntitySearchRecord(id, entity, stat);
        }

        @Override
        protected String getAddEntityRecordQuery() {
            return null;
        }

        @Override
        protected void buildAddEntityStatement(IApsEntity entity, PreparedStatement stat) throws Throwable {
            // no-op
        }

        @Override
        protected String getDeleteEntityRecordQuery() {
            return null;
        }

        @Override
        protected String getUpdateEntityRecordQuery() {
            return null;
        }

        @Override
        protected void buildUpdateEntityStatement(IApsEntity entity, PreparedStatement stat) throws Throwable {
            // no-op
        }

        @Override
        protected String getLoadEntityRecordQuery() {
            return null;
        }

        @Override
        protected ApsEntityRecord createEntityRecord(ResultSet res) throws Throwable {
            return null;
        }

        @Override
        protected String getAddingSearchRecordQuery() {
            return null;
        }

        @Override
        protected String getAddingAttributeRoleRecordQuery() {
            return null;
        }

        @Override
        protected String getRemovingSearchRecordQuery() {
            return null;
        }

        @Override
        protected String getRemovingAttributeRoleRecordQuery() {
            return null;
        }

        @Override
        protected String getExtractingAllEntityIdQuery() {
            return null;
        }
    }

}
