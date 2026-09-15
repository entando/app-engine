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
package com.agiletec.apsadmin.system.entity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.common.entity.model.ApsEntity;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.ThreeStateAttribute;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the admin-console side of the nested-boolean search: {@link EntityActionHelper}
 * must turn a Composite-nested boolean form field into a path-keyed {@link EntitySearchFilter}, and
 * {@link EntityActionHelper#getAttributeFilterFieldName} must round-trip a nested path key back to its
 * form field (the case that previously threw an NPE).
 */
class EntityActionHelperNestedSearchTest {

    private final EntityActionHelper helper = new EntityActionHelper();

    @Test
    void getAttributeFilters_shouldBuildPathKeyedFilterForCompositeNestedBoolean() {
        ApsEntity prototype = entity(
                booleanAttr("topFlag", true),
                composite("Composite", booleanAttr("Boolean", true)));

        AbstractApsEntityFinderAction action = mock(AbstractApsEntityFinderAction.class);
        // top-level boolean submitted as "false", nested boolean submitted as "true"
        lenient().when(action.getSearchFormFieldValue("topFlag_booleanFieldName")).thenReturn("false");
        lenient().when(action.getSearchFormFieldValue("Composite_Boolean_booleanFieldName")).thenReturn("true");

        EntitySearchFilter[] filters = helper.getAttributeFilters(action, prototype);

        assertEquals(2, filters.length);
        // top-level filter keyed by its own name (legacy behaviour, unchanged)
        assertEquals("topFlag", filters[0].getKey());
        assertEquals("false", filters[0].getValue());
        // nested filter keyed by the path "<composite>_<boolean>" - the key the DB records use
        assertEquals("Composite_Boolean", filters[1].getKey());
        assertEquals("true", filters[1].getValue());
    }

    @Test
    void getAttributeFilters_shouldIgnoreNestedBooleanWithNoSubmittedValue() {
        ApsEntity prototype = entity(composite("Composite", booleanAttr("Boolean", true)));
        AbstractApsEntityFinderAction action = mock(AbstractApsEntityFinderAction.class);
        // nothing submitted -> "both" -> no filter
        EntitySearchFilter[] filters = helper.getAttributeFilters(action, prototype);
        assertEquals(0, filters.length);
    }

    @Test
    void getAttributeFilters_shouldSkipNonSearchableNestedBoolean() {
        ApsEntity prototype = entity(composite("Composite", booleanAttr("Boolean", false)));
        AbstractApsEntityFinderAction action = mock(AbstractApsEntityFinderAction.class);
        lenient().when(action.getSearchFormFieldValue("Composite_Boolean_booleanFieldName")).thenReturn("true");
        // the nested boolean is not searchable -> not offered -> no filter even if a value is present
        assertEquals(0, helper.getAttributeFilters(action, prototype).length);
    }

    @Test
    void getAttributeFilterFieldName_shouldResolveNestedPathKey() {
        ApsEntity prototype = entity(composite("Composite", booleanAttr("Boolean", true)));
        assertArrayEquals(new String[]{"Composite_Boolean_booleanFieldName"},
                helper.getAttributeFilterFieldName(prototype, "Composite_Boolean"));
    }

    @Test
    void getAttributeFilterFieldName_shouldReturnEmptyArrayForUnknownKey() {
        // a key that is neither a top-level attribute nor a resolvable nested boolean: no NPE and,
        // per Sonar S1168, an empty array (never null) - the caller treats it as "no field names"
        ApsEntity prototype = entity(composite("Composite", booleanAttr("Boolean", true)));
        String[] result = helper.getAttributeFilterFieldName(prototype, "Composite_Missing");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    // --- ThreeState (Any / Yes / No / Not set) -----------------------------

    @Test
    void getAttributeFilters_shouldBuildNullOptionFilterForThreeStateNotSet() {
        ApsEntity prototype = entity(threeState("flag", true));
        AbstractApsEntityFinderAction action = mock(AbstractApsEntityFinderAction.class);
        when(action.getSearchFormFieldValue("flag_booleanFieldName")).thenReturn("none");
        EntitySearchFilter[] filters = helper.getAttributeFilters(action, prototype);
        assertEquals(1, filters.length);
        assertEquals("flag", filters[0].getKey());
        // "Not set" is the unset state: matched by the null option, never by a value
        assertTrue(filters[0].isNullOption());
        assertNull(filters[0].getValue());
    }

    @Test
    void getAttributeFilters_shouldBuildValueFilterForThreeStateTrueFalse() {
        ApsEntity prototype = entity(threeState("flag", true));
        AbstractApsEntityFinderAction action = mock(AbstractApsEntityFinderAction.class);
        when(action.getSearchFormFieldValue("flag_booleanFieldName")).thenReturn("true");
        EntitySearchFilter[] filters = helper.getAttributeFilters(action, prototype);
        assertEquals(1, filters.length);
        assertEquals("true", filters[0].getValue());
        assertFalse(filters[0].isNullOption());
    }

    @Test
    void getAttributeFilters_shouldSkipThreeStateWhenAny() {
        ApsEntity prototype = entity(threeState("flag", true));
        AbstractApsEntityFinderAction action = mock(AbstractApsEntityFinderAction.class);
        // "Any" submits a blank value -> no filter
        assertEquals(0, helper.getAttributeFilters(action, prototype).length);
    }

    @Test
    void getAttributeFilters_plainBooleanNeverUsesNullOption() {
        // "none" is only meaningful for ThreeState; a plain Boolean keeps the plain value path
        ApsEntity prototype = entity(booleanAttr("flag", true));
        AbstractApsEntityFinderAction action = mock(AbstractApsEntityFinderAction.class);
        when(action.getSearchFormFieldValue("flag_booleanFieldName")).thenReturn("false");
        EntitySearchFilter[] filters = helper.getAttributeFilters(action, prototype);
        assertEquals(1, filters.length);
        assertEquals("false", filters[0].getValue());
        assertFalse(filters[0].isNullOption());
    }

    // --- helpers -----------------------------------------------------------

    private ThreeStateAttribute threeState(String name, boolean searchable) {
        ThreeStateAttribute a = new ThreeStateAttribute();
        a.setName(name);
        a.setType("ThreeState");
        a.setSearchable(searchable);
        return a;
    }

    private BooleanAttribute booleanAttr(String name, boolean searchable) {
        BooleanAttribute a = new BooleanAttribute();
        a.setName(name);
        a.setType("Boolean");
        a.setSearchable(searchable);
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

    private ApsEntity entity(AttributeInterface... attributes) {
        ApsEntity entity = new ApsEntity();
        entity.setTypeCode("TST");
        for (AttributeInterface attribute : attributes) {
            entity.addAttribute(attribute);
        }
        return entity;
    }

}
