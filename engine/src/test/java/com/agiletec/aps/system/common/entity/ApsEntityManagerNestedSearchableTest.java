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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.BaseTestCase;
import org.entando.entando.aps.system.common.entity.search.NestedSearchSupport;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoListAttribute;
import java.util.List;
import org.entando.entando.aps.system.services.userprofile.IUserProfileManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.entando.entando.aps.system.common.entity.search.SearchRecordSpec;

/**
 * The persistence boundary is the single enforcement point of the nested-searchability rule: whatever
 * built the type - the REST services, the legacy API, the admin console, or a path added later - it
 * arrives at {@code addEntityPrototype}/{@code updateEntityPrototype}, and the flag is corrected there.
 *
 * <p>These tests drive the <b>real</b> manager rather than the traversal in isolation, so they also pin
 * the part that matters most in practice: the normalized type is what gets written, so the behaviour no
 * longer differs before and after a restart.</p>
 */
class ApsEntityManagerNestedSearchableTest extends BaseTestCase {

    private static final String TYPE_CODE = "TSN";

    private IUserProfileManager entityManager;

    @BeforeEach
    void resolveManager() {
        this.entityManager = getApplicationContext().getBean(IUserProfileManager.class);
    }

    @AfterEach
    void removeTestType() throws Exception {
        if (null != this.entityManager && null != this.entityManager.getEntityPrototype(TYPE_CODE)) {
            ((IEntityTypesConfigurer) this.entityManager).removeEntityPrototype(TYPE_CODE);
        }
    }

    @Test
    void addEntityPrototype_shouldClearTheFlagOnACompositeChildOfANonNestableType() throws Exception {
        IApsEntity type = this.prototype();
        type.addAttribute(composite("compo", searchable("Monotext", "note")));

        ((IEntityTypesConfigurer) this.entityManager).addEntityPrototype(type);

        // read back from the stored configuration, not from the object we handed over
        IApsEntity stored = this.entityManager.getEntityPrototype(TYPE_CODE);
        assertNotNull(stored);
        assertFalse(child(stored, "compo", "note").isSearchable());
        // and the unqualified-name record that would collide with a top-level 'note' is gone
        assertTrue(NestedSearchSupport.planSearchRecords(stored).isEmpty());
    }

    /**
     * Documents the boundary's <b>reach</b>, which is not the same as the traversal's rule. On a type
     * prototype a list carries its structure in {@code getNestedAttributeType()}, not in
     * {@code getAttributes()}, and the shared traversal descends only the latter - so a boolean under
     * monolist -&gt; composite keeps its flag in the stored configuration.
     *
     * <p>Asserted rather than fixed, because the flag is <b>inert</b>: no search record is planned for
     * it, no search form offers it, and the composite editor hides the checkbox inside a list. Giving the
     * normalizer a second descent to reach it would buy a cosmetic correction at the cost of two
     * traversals with different reach - the thing this class exists to avoid.</p>
     */
    @Test
    void addEntityPrototype_shouldNotReachInsideAListOnAPrototypeButTheFlagStaysInert() throws Exception {
        IApsEntity type = this.prototype();
        type.addAttribute(monolist("rows", composite("row", searchable("Boolean", "flag"))));

        ((IEntityTypesConfigurer) this.entityManager).addEntityPrototype(type);

        IApsEntity stored = this.entityManager.getEntityPrototype(TYPE_CODE);
        assertNotNull(stored);
        MonoListAttribute storedList = (MonoListAttribute) stored.getAttribute("rows");
        CompositeAttribute storedComposite = (CompositeAttribute) storedList.getNestedAttributeType();
        assertTrue(storedComposite.getAttribute("flag").isSearchable());
        // ...and nothing reads it: no record is planned, and the search schema offers no key for it
        assertTrue(NestedSearchSupport.planSearchRecords(stored).isEmpty());
        assertTrue(NestedSearchSupport.collectSearchable(stored).isEmpty());
    }

    @Test
    void addEntityPrototype_shouldKeepTheFlagOnABooleanCompositeChildAndIndexItByPath() throws Exception {
        IApsEntity type = this.prototype();
        type.addAttribute(composite("compo", searchable("Boolean", "flag")));

        ((IEntityTypesConfigurer) this.entityManager).addEntityPrototype(type);

        IApsEntity stored = this.entityManager.getEntityPrototype(TYPE_CODE);
        assertNotNull(stored);
        assertTrue(child(stored, "compo", "flag").isSearchable());
        assertEquals(List.of("compo_flag"), NestedSearchSupport.planSearchRecords(stored).stream()
                .map(SearchRecordSpec::attrName).toList());
    }

    @Test
    void updateEntityPrototype_shouldNormalizeToo() throws Exception {
        IApsEntity type = this.prototype();
        type.addAttribute(composite("compo", searchable("Boolean", "flag")));
        ((IEntityTypesConfigurer) this.entityManager).addEntityPrototype(type);

        // the update carries a text child that asks to be searchable - the same gap, on the other method
        IApsEntity updated = this.newType();
        updated.addAttribute(composite("compo", searchable("Boolean", "flag"),
                searchable("Monotext", "note")));
        ((IEntityTypesConfigurer) this.entityManager).updateEntityPrototype(updated);

        IApsEntity stored = this.entityManager.getEntityPrototype(TYPE_CODE);
        assertNotNull(stored);
        assertTrue(child(stored, "compo", "flag").isSearchable());
        assertFalse(child(stored, "compo", "note").isSearchable());
    }

    @Test
    void addEntityPrototype_shouldLeaveASearchableChildOfAListAlone() throws Exception {
        // REGRESSION GUARD - the flattened legacy indexing every existing list filter depends on
        IApsEntity type = this.prototype();
        type.addAttribute(monolist("notes", searchable("Monotext", "note")));

        ((IEntityTypesConfigurer) this.entityManager).addEntityPrototype(type);

        IApsEntity stored = this.entityManager.getEntityPrototype(TYPE_CODE);
        assertNotNull(stored);
        MonoListAttribute storedList = (MonoListAttribute) stored.getAttribute("notes");
        assertTrue(storedList.getNestedAttributeType().isSearchable());
    }

    private IApsEntity prototype() throws Exception {
        assertNull(this.entityManager.getEntityPrototype(TYPE_CODE));
        return this.newType();
    }

    private IApsEntity newType() throws Exception {
        IApsEntity type = (IApsEntity) this.entityManager.getEntityClass()
                .getDeclaredConstructor().newInstance();
        type.setTypeCode(TYPE_CODE);
        type.setTypeDescription("Nested searchability boundary");
        return type;
    }

    private MonoListAttribute monolist(String name, AttributeInterface nestedType) {
        MonoListAttribute list = (MonoListAttribute) ((AttributeInterface) this.entityManager
                .getEntityAttributePrototypes().get("Monolist")).getAttributePrototype();
        list.setName(name);
        list.setNestedAttributeType(nestedType);
        return list;
    }

    /**
     * A searchable attribute of the given registered type, cloned from the manager's prototype exactly as
     * every real write path does - a hand-instantiated attribute would carry no type code and could not
     * be serialized.
     */
    private AttributeInterface searchable(String typeCode, String name) {
        AttributeInterface prototype = this.entityManager.getEntityAttributePrototypes().get(typeCode);
        assertNotNull(prototype, "attribute type '" + typeCode + "' is not registered");
        AttributeInterface attribute = (AttributeInterface) prototype.getAttributePrototype();
        attribute.setName(name);
        attribute.setSearchable(true);
        return attribute;
    }

    private CompositeAttribute composite(String name, AttributeInterface... children) {
        CompositeAttribute composite = (CompositeAttribute) ((AttributeInterface) this.entityManager
                .getEntityAttributePrototypes().get("Composite")).getAttributePrototype();
        composite.setName(name);
        for (AttributeInterface child : children) {
            // deliberately the hand-rolled insertion the write paths use: the point of the boundary is
            // that a type assembled this way is still normalized
            composite.getAttributes().add(child);
            composite.getAttributeMap().put(child.getName(), child);
        }
        return composite;
    }

    private AttributeInterface child(IApsEntity type, String compositeName, String childName) {
        CompositeAttribute composite = (CompositeAttribute) type.getAttribute(compositeName);
        assertNotNull(composite, "composite '" + compositeName + "' missing from the stored type");
        return composite.getAttribute(childName);
    }

}
