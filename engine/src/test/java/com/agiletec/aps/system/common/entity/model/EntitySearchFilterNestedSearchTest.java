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
package com.agiletec.aps.system.common.entity.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoListAttribute;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link EntitySearchFilter#getInstance(IApsEntity, Properties)} accepts a
 * Composite-nested boolean referenced by its path key {@code <composite>_<boolean>} - the read-side
 * half of the Solr-disabled nested-boolean feature.
 */
class EntitySearchFilterNestedSearchTest {

    @Test
    void shouldResolveNestedBooleanPathKey() {
        ApsEntity prototype = entity(composite("address", booleanAttr("certified", true, Boolean.TRUE)));
        EntitySearchFilter filter = EntitySearchFilter.getInstance(prototype,
                attributeFilterProps("address_certified", "true"));
        assertNotNull(filter);
        assertTrue(filter.isAttributeFilter());
        assertEquals("address_certified", filter.getKey());
        assertEquals("true", filter.getValue());
    }

    @Test
    void topLevelAttributeTakesPrecedence() {
        ApsEntity prototype = entity(booleanAttr("flag", true, Boolean.TRUE));
        EntitySearchFilter filter = EntitySearchFilter.getInstance(prototype,
                attributeFilterProps("flag", "true"));
        assertNotNull(filter);
        assertEquals("flag", filter.getKey());
    }

    @Test
    void shouldRejectUnknownKey() {
        ApsEntity prototype = entity(composite("address", booleanAttr("certified", true, Boolean.TRUE)));
        Properties props = attributeFilterProps("address_missing", "true");
        assertThrows(RuntimeException.class, () -> EntitySearchFilter.getInstance(prototype, props));
    }

    @Test
    void shouldNotResolveListReachedBoolean() {
        ApsEntity prototype = entity(monolist("tags", booleanAttr("flag", true, Boolean.TRUE)));
        Properties props = attributeFilterProps("tags_flag", "true");
        assertThrows(RuntimeException.class, () -> EntitySearchFilter.getInstance(prototype, props));
    }

    // --- helpers -----------------------------------------------------------

    private Properties attributeFilterProps(String key, String value) {
        Properties props = new Properties();
        props.setProperty(EntitySearchFilter.KEY_PARAM, key);
        props.setProperty(EntitySearchFilter.FILTER_TYPE_PARAM, Boolean.TRUE.toString());
        props.setProperty(EntitySearchFilter.VALUE_PARAM, value);
        return props;
    }

    private BooleanAttribute booleanAttr(String name, boolean searchable, Boolean value) {
        BooleanAttribute a = new BooleanAttribute();
        a.setName(name);
        a.setSearchable(searchable);
        a.setBooleanValue(value);
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

}
