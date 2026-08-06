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
package org.entando.entando.aps.system.common.entity.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.entando.entando.aps.system.common.entity.search.NestedSearchSupport.KeyProblem;
import org.entando.entando.aps.system.common.entity.search.NestedSearchSupport.KeyProblemType;
import com.agiletec.aps.system.common.entity.model.ApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoTextAttribute;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The search key encoding, and the one part of the policy that is <b>not</b> engine-wide: how long a key
 * may be. Every manager owns its own search table, so a manager whose {@code attrname} column is narrower
 * than the platform's must be able to say so - otherwise a key the engine validated still truncates or
 * fails on insert.
 *
 * <p>Which attributes get a nested key is a separate question, and not this class's: it is the
 * attribute's own declaration, pinned by the last test here.</p>
 */
class EntitySearchKeysTest {

    private static final int MAX_KEY_LENGTH = EntitySearchKeys.DEFAULT_MAX_KEY_LENGTH;

    @Test
    void shouldKeepTopLevelKeysRawAndEscapeNestedOnes() {
        assertEquals(255, EntitySearchKeys.DEFAULT_MAX_KEY_LENGTH);
        // a top-level attribute keeps the exact name the search tables have always stored
        assertEquals("a_b_c", EntitySearchKeys.buildKey(List.of("a_b_c")));
        // a nested path escapes the delimiter inside each segment, so it cannot collide
        assertEquals("a__b_c", EntitySearchKeys.buildKey(List.of("a_b", "c")));
        assertEquals("a_b__c", EntitySearchKeys.buildKey(List.of("a", "b_c")));
        assertEquals("a_b_c", EntitySearchKeys.buildKey(List.of("a", "b", "c")));
        assertEquals("a_b > c", EntitySearchKeys.buildLabel(List.of("a_b", "c")));
        assertNull(EntitySearchKeys.buildKey(null));
        assertNull(EntitySearchKeys.buildLabel(null));
    }

    @Test
    void aNarrowerColumnShouldRejectAKeyTheDefaultWouldAccept() {
        // the invariant that used to be a Javadoc warning: a manager whose attrname column is narrower
        // than the platform's now enforces its own width, instead of silently truncating on insert
        ApsEntity entity = entity(composite("configuration", booleanAttr("highlighted", true)));
        String key = "configuration_highlighted";

        assertTrue(NestedSearchSupport.validateNestedSearchKeys(entity, MAX_KEY_LENGTH).isEmpty(),
                "the default 255-char column accepts this key");

        List<KeyProblem> problems = NestedSearchSupport.validateNestedSearchKeys(entity, 20);
        assertEquals(1, problems.size());
        KeyProblem problem = problems.get(0);
        assertEquals(KeyProblemType.TOO_LONG, problem.type());
        assertEquals(key, problem.key());
        // the message quotes the bound that actually applied, not an engine-wide constant
        assertEquals(20, problem.maxKeyLength());
        assertTrue(problem.getDescription().endsWith("the maximum is 20"), problem.getDescription());
    }

    @Test
    void aWiderColumnShouldAcceptAKeyTheDefaultRejects() {
        String longName = "c".repeat(300);
        ApsEntity entity = entity(composite(longName, booleanAttr("flag", true)));
        assertEquals(1, NestedSearchSupport.validateNestedSearchKeys(entity, MAX_KEY_LENGTH).size());
        assertTrue(NestedSearchSupport.validateNestedSearchKeys(entity, 1000).isEmpty());
    }

    @Test
    void escapingAloneIsNotEnough_soABoundarySeparatorIsRejected() {
        // The doubling escape does NOT make the encoding injective by itself: a run of n separators can
        // be read as k literals ending a segment + the separator + m literals starting the next, for any
        // 2k+1+2m = n. Composite "a_" + child "b" and composite "a" + child "_b" both flatten to
        // "a___b". The encoding therefore refuses a segment that touches the separator.
        assertEquals("a___b", EntitySearchKeys.buildKey(List.of("a_", "b")));
        assertEquals("a___b", EntitySearchKeys.buildKey(List.of("a", "_b")));

        assertFalse(EntitySearchKeys.isEncodableSegment("a_"), "a trailing separator is ambiguous");
        assertFalse(EntitySearchKeys.isEncodableSegment("_b"), "a leading separator is ambiguous");
        assertFalse(EntitySearchKeys.isEncodableSegment("_"));
        // an underscore INSIDE a name is fine, however many of them
        assertTrue(EntitySearchKeys.isEncodableSegment("press_kit"));
        assertTrue(EntitySearchKeys.isEncodableSegment("b__c"));
        assertTrue(EntitySearchKeys.isEncodableSegment("a___b"));
        assertTrue(EntitySearchKeys.isEncodableSegment(null));
    }

    @Test
    void anUnencodableSegmentShouldBeRejectedWhenTheTypeIsSaved() {
        ApsEntity entity = entity(composite("a_", booleanAttr("b", true)));
        List<KeyProblem> problems = NestedSearchSupport.validateNestedSearchKeys(entity, MAX_KEY_LENGTH);
        assertEquals(1, problems.size());
        assertEquals(KeyProblemType.AMBIGUOUS_SEGMENT, problems.get(0).type());
        assertEquals(List.of("a_ > b"), problems.get(0).paths());
        assertTrue(problems.get(0).getDescription().contains("must not begin or end"),
                problems.get(0).getDescription());

        // the child side too
        assertEquals(KeyProblemType.AMBIGUOUS_SEGMENT, NestedSearchSupport
                .validateNestedSearchKeys(entity(composite("a", booleanAttr("_b", true))),
                        MAX_KEY_LENGTH)
                .get(0).type());
    }

    @Test
    void aTopLevelNameMayStillTouchTheSeparator() {
        // only a nested path is encoded; a top-level key is the raw name, so nothing is ambiguous there
        ApsEntity entity = entity(booleanAttr("_flag_", true));
        assertTrue(NestedSearchSupport.validateNestedSearchKeys(entity, MAX_KEY_LENGTH).isEmpty());
        assertEquals("_flag_",
                NestedSearchSupport.collectSearchable(entity).get(0).key());
    }

    @Test
    void underscoresInsideANameRemainPerfectlyLegal() {
        // the case that must NOT be rejected: doubled underscores inside a segment encode as an even run
        ApsEntity entity = entity(composite("a", booleanAttr("b__c", true)));
        assertTrue(NestedSearchSupport.validateNestedSearchKeys(entity, MAX_KEY_LENGTH).isEmpty());
        assertEquals("a_b____c", NestedSearchSupport.collectSearchable(entity).get(0).key());
        assertEquals("a > b__c", NestedSearchSupport.buildSearchLabels(entity).get("a_b____c"));
    }

    @Test
    void anAttributeTypeShouldOptIntoNestedIndexingOnItsOwn() {
        // The extensibility claim, exercised where it belongs: a custom attribute type becomes
        // path-indexable by overriding isNestedSearchSupported(), with no entity-manager configuration
        // and no change to the engine. The key encoding cannot grant this - if it could, it would
        // contradict CompositeAttribute, which clears the searchable flag of a child that declares no
        // support.
        MonoTextAttribute plainStreet = monoText("street", true);
        ApsEntity plain = entity(composite("address", plainStreet));

        // a plain text child is not offered as a nested criterion, and keeps the legacy bare-name record
        assertTrue(NestedSearchSupport.collectSearchable(plain).isEmpty());
        assertEquals(List.of("street"), NestedSearchSupport.planSearchRecords(plain).stream()
                .map(SearchRecordSpec::attrName).toList());
        assertFalse(NestedSearchSupport.isIndexableNested(plainStreet));

        // the same attribute type, opting in for itself, is offered and written under its path key
        NestedSearchableTextAttribute street = new NestedSearchableTextAttribute();
        street.setName("street");
        street.setSearchable(true);
        ApsEntity entity = entity(composite("address", street));

        assertTrue(NestedSearchSupport.isIndexableNested(street));
        List<SearchableAttributeRef> offered = NestedSearchSupport.collectSearchable(entity);
        assertEquals(1, offered.size());
        assertEquals("address_street", offered.get(0).key());
        assertEquals("address > street", offered.get(0).label());
        assertSame(street, offered.get(0).source());
        assertEquals(List.of("address_street"), NestedSearchSupport.planSearchRecords(entity).stream()
                .map(SearchRecordSpec::attrName).toList());
        assertSame(street, NestedSearchSupport.resolveNestedByKey(entity, "address_street"));
    }

    /** A custom attribute type that declares itself nested-searchable - the supported way to opt in. */
    private static class NestedSearchableTextAttribute extends MonoTextAttribute {

        @Override
        public boolean isNestedSearchSupported() {
            return true;
        }
    }

    // --- helpers -----------------------------------------------------------

    private ApsEntity entity(AttributeInterface... attributes) {
        ApsEntity entity = new ApsEntity();
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
