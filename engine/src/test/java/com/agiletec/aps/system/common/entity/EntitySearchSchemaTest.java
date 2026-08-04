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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.system.common.entity.NestedBooleanSearchSupport.KeyProblem;
import com.agiletec.aps.system.common.entity.NestedBooleanSearchSupport.KeyProblemType;
import com.agiletec.aps.system.common.entity.model.ApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoTextAttribute;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The type-level search schema: the memo of what a type offers to a search, and the single place a key
 * set comes into existence.
 */
class EntitySearchSchemaTest {

    @Test
    void shouldDescribeWhatTheTypeOffers() {
        BooleanAttribute nested = booleanAttr("certified", true);
        ApsEntity type = entity("NWS", monoText("title", true), monoText("body", false),
                composite("address", nested));

        EntitySearchSchema schema = EntitySearchSchema.build(type, DefaultEntitySearchKeyStrategy.INSTANCE);

        assertEquals("NWS", schema.getTypeCode());
        assertEquals(List.of("title", "address_certified"),
                schema.getSearchableAttributes().stream().map(SearchableAttributeRef::key).toList());
        assertEquals("address > certified", schema.getLabels().get("address_certified"));
        assertEquals("title", schema.getLabels().get("title"));
        assertSame(nested, schema.getOfferedAttribute("address_certified"));
        assertNull(schema.getOfferedAttribute("body"), "a non-searchable attribute is not offered");
        assertNull(schema.getOfferedAttribute(null));
        assertTrue(schema.isValid());
        assertTrue(schema.getProblems().isEmpty());
    }

    @Test
    void shouldAgreeWithTheTraversalItMemoises() {
        // the schema must be exactly the memo of the shared traversal - not a second implementation
        ApsEntity type = entity("NWS", booleanAttr("compo_flag", true),
                composite("compo", booleanAttr("flag", true)), composite("a_b", booleanAttr("c", true)));
        EntitySearchSchema schema = EntitySearchSchema.build(type, DefaultEntitySearchKeyStrategy.INSTANCE);
        assertEquals(NestedBooleanSearchSupport.collectSearchable(type), schema.getSearchableAttributes());
        assertEquals(NestedBooleanSearchSupport.buildSearchLabels(type), schema.getLabels());
        assertEquals(NestedBooleanSearchSupport.validateNestedBooleanKeys(type), schema.getProblems());
    }

    @Test
    void shouldRecordTheDefectsOfAnUnusableKeySetInsteadOfThrowing() {
        // a type that arrives through loading - which must never fail - still has to be diagnosable:
        // the schema carries the defect so the manager can report it
        ApsEntity type = entity("BAD", booleanAttr("compo_flag", true),
                composite("compo", booleanAttr("flag", true)));
        EntitySearchSchema schema = EntitySearchSchema.build(type, DefaultEntitySearchKeyStrategy.INSTANCE);
        assertFalse(schema.isValid());
        assertEquals(1, schema.getProblems().size());
        assertEquals(KeyProblemType.DUPLICATED, schema.getProblems().get(0).type());
        assertEquals("compo_flag", schema.getProblems().get(0).key());
    }

    @Test
    void shouldHonourTheStrategyItIsBuiltWith() {
        ApsEntity type = entity("NWS", composite("configuration", booleanAttr("highlighted", true)));
        assertTrue(EntitySearchSchema.build(type, DefaultEntitySearchKeyStrategy.INSTANCE).isValid());
        EntitySearchSchema narrow = EntitySearchSchema.build(type, new DefaultEntitySearchKeyStrategy(20));
        assertFalse(narrow.isValid());
        assertEquals(20, narrow.getProblems().get(0).maxKeyLength());
    }

    @Test
    void shouldBeEmptyRatherThanNullForAnUnknownType() {
        EntitySearchSchema schema = EntitySearchSchema.build(null, DefaultEntitySearchKeyStrategy.INSTANCE);
        assertNull(schema.getTypeCode());
        assertTrue(schema.getSearchableAttributes().isEmpty());
        assertTrue(schema.getLabels().isEmpty());
        assertTrue(schema.isValid());
    }

    @Test
    void shouldBeImmutableSoCallersCannotCorruptACachedSchema() {
        // it is shared by every caller for the lifetime of the type, so it must not be editable in place
        EntitySearchSchema schema = EntitySearchSchema.build(
                entity("NWS", monoText("title", true)), DefaultEntitySearchKeyStrategy.INSTANCE);
        List<SearchableAttributeRef> attributes = schema.getSearchableAttributes();
        Map<String, String> labels = schema.getLabels();
        List<KeyProblem> problems = schema.getProblems();
        assertThrows(UnsupportedOperationException.class, attributes::clear);
        assertThrows(UnsupportedOperationException.class, labels::clear);
        assertThrows(UnsupportedOperationException.class, problems::clear);
    }

    // --- helpers -----------------------------------------------------------

    private ApsEntity entity(String typeCode, AttributeInterface... attributes) {
        ApsEntity entity = new ApsEntity();
        entity.setTypeCode(typeCode);
        for (AttributeInterface attribute : attributes) {
            entity.addAttribute(attribute);
        }
        return entity;
    }

    private CompositeAttribute composite(String name, AttributeInterface... children) {
        CompositeAttribute composite = new CompositeAttribute();
        composite.setName(name);
        for (AttributeInterface child : children) {
            composite.getAttributes().add(child);
        }
        return composite;
    }

    private BooleanAttribute booleanAttr(String name, boolean searchable) {
        BooleanAttribute attribute = new BooleanAttribute();
        attribute.setName(name);
        attribute.setSearchable(searchable);
        attribute.setBooleanValue(Boolean.TRUE);
        return attribute;
    }

    private MonoTextAttribute monoText(String name, boolean searchable) {
        MonoTextAttribute attribute = new MonoTextAttribute();
        attribute.setName(name);
        attribute.setSearchable(searchable);
        return attribute;
    }

}
