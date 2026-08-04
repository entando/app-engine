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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.system.common.entity.NestedBooleanSearchSupport.KeyProblem;
import com.agiletec.aps.system.common.entity.NestedBooleanSearchSupport.KeyProblemType;
import com.agiletec.aps.system.common.entity.model.ApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoTextAttribute;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The search key policy as a replaceable collaborator of an entity manager, rather than a set of
 * constants baked into the engine.
 *
 * <p>The bound that matters most is the {@code attrname} column width: every manager owns its own search
 * table, so a manager whose column is narrower than the platform's must be able to say so - otherwise a
 * key the engine validated still truncates or fails on insert. These tests pin that, and pin that the
 * encoding is replaceable too.</p>
 *
 * <p>What a strategy deliberately cannot do is decide <b>which</b> attributes get a nested key: that is the
 * attribute's own declaration, and the last test here pins that a custom attribute type opts in on its own,
 * with no strategy involved.</p>
 */
class EntitySearchKeyStrategyTest {

    @Test
    void defaultStrategyShouldKeepTopLevelKeysRawAndEscapeNestedOnes() {
        IEntitySearchKeyStrategy strategy = DefaultEntitySearchKeyStrategy.INSTANCE;
        assertEquals(255, strategy.getMaxKeyLength());
        // a top-level attribute keeps the exact name the search tables have always stored
        assertEquals("a_b_c", strategy.buildKey(List.of("a_b_c")));
        // a nested path escapes the delimiter inside each segment, so it cannot collide
        assertEquals("a__b_c", strategy.buildKey(List.of("a_b", "c")));
        assertEquals("a_b__c", strategy.buildKey(List.of("a", "b_c")));
        assertEquals("a_b_c", strategy.buildKey(List.of("a", "b", "c")));
        assertEquals("a_b > c", strategy.buildLabel(List.of("a_b", "c")));
        assertNull(strategy.buildKey(null));
        assertNull(strategy.buildLabel(null));
    }

    @Test
    void aNarrowerColumnShouldRejectAKeyTheDefaultWouldAccept() {
        // the invariant that used to be a Javadoc warning: a manager whose attrname column is narrower
        // than the platform's now enforces its own width, instead of silently truncating on insert
        ApsEntity entity = entity(composite("configuration", booleanAttr("highlighted", true)));
        String key = "configuration_highlighted";

        assertTrue(NestedBooleanSearchSupport.validateNestedBooleanKeys(entity).isEmpty(),
                "the default 255-char column accepts this key");

        IEntitySearchKeyStrategy narrow = new DefaultEntitySearchKeyStrategy(20);
        List<KeyProblem> problems = NestedBooleanSearchSupport.validateNestedBooleanKeys(entity, narrow);
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
        assertEquals(1, NestedBooleanSearchSupport.validateNestedBooleanKeys(entity).size());
        assertTrue(NestedBooleanSearchSupport
                .validateNestedBooleanKeys(entity, new DefaultEntitySearchKeyStrategy(1000)).isEmpty());
    }

    @Test
    void anAttributeTypeShouldOptIntoNestedIndexingOnItsOwn() {
        // The extensibility claim, exercised where it belongs: a custom attribute type becomes
        // path-indexable by overriding isNestedSearchSupported(), with no strategy, no entity-manager
        // configuration and no change to the engine. A strategy cannot grant this - if it could, it would
        // contradict CompositeAttribute, which clears the searchable flag of a child that declares no
        // support and has no manager to ask.
        MonoTextAttribute plainStreet = monoText("street", true);
        ApsEntity plain = entity(composite("address", plainStreet));

        // a plain text child is not offered as a nested criterion, and keeps the legacy bare-name record
        assertTrue(NestedBooleanSearchSupport.collectSearchable(plain).isEmpty());
        assertEquals(List.of("street"), NestedBooleanSearchSupport.planSearchRecords(plain).stream()
                .map(SearchRecordSpec::attrName).toList());
        assertFalse(NestedBooleanSearchSupport.isIndexableNestedBoolean(plainStreet));

        // the same attribute type, opting in for itself, is offered and written under its path key
        NestedSearchableTextAttribute street = new NestedSearchableTextAttribute();
        street.setName("street");
        street.setSearchable(true);
        ApsEntity entity = entity(composite("address", street));

        assertTrue(NestedBooleanSearchSupport.isIndexableNestedBoolean(street));
        List<SearchableAttributeRef> offered = NestedBooleanSearchSupport.collectSearchable(entity);
        assertEquals(1, offered.size());
        assertEquals("address_street", offered.get(0).key());
        assertEquals("address > street", offered.get(0).label());
        assertSame(street, offered.get(0).source());
        assertEquals(List.of("address_street"), NestedBooleanSearchSupport.planSearchRecords(entity).stream()
                .map(SearchRecordSpec::attrName).toList());
        assertSame(street, NestedBooleanSearchSupport.resolveNestedBooleanByKey(entity, "address_street"));

        // and a custom encoding still applies to it, because the two concerns compose
        IEntitySearchKeyStrategy dotted = new DefaultEntitySearchKeyStrategy() {
            @Override
            public String buildKey(List<String> segments) {
                return String.join(".", segments);
            }
        };
        assertEquals("address.street",
                NestedBooleanSearchSupport.collectSearchable(entity, dotted).get(0).key());
    }

    /** A custom attribute type that declares itself nested-searchable - the supported way to opt in. */
    private static class NestedSearchableTextAttribute extends MonoTextAttribute {

        @Override
        public boolean isNestedSearchSupported() {
            return true;
        }
    }

    @Test
    void aStrategyShouldBeAbleToChangeTheEncoding() {
        IEntitySearchKeyStrategy dotted = new DefaultEntitySearchKeyStrategy() {
            @Override
            public String buildKey(List<String> segments) {
                return String.join(".", segments);
            }
        };
        BooleanAttribute flag = booleanAttr("flag", true);
        ApsEntity entity = entity(composite("compo", flag));
        assertEquals("compo.flag",
                NestedBooleanSearchSupport.collectSearchable(entity, dotted).get(0).key());
        // and the writer and the resolver follow the same strategy, so they still agree
        assertEquals(List.of("compo.flag"),
                NestedBooleanSearchSupport.planSearchRecords(entity, dotted).stream()
                        .map(SearchRecordSpec::attrName).toList());
        assertSame(flag, NestedBooleanSearchSupport.resolveNestedBooleanByKey(entity, "compo.flag", dotted));
        // the default encoding no longer resolves it - which is why an override has to be applied to the
        // manager and its DAO together
        assertNull(NestedBooleanSearchSupport.resolveNestedBooleanByKey(entity, "compo.flag"));
    }

    @Test
    void escapingAloneIsNotEnough_soABoundarySeparatorIsRejected() {
        // The doubling escape does NOT make the encoding injective by itself: a run of n separators can
        // be read as k literals ending a segment + the separator + m literals starting the next, for any
        // 2k+1+2m = n. Composite "a_" + child "b" and composite "a" + child "_b" both flatten to
        // "a___b". The strategy therefore refuses a segment that touches the separator.
        assertEquals("a___b", DefaultEntitySearchKeyStrategy.INSTANCE.buildKey(List.of("a_", "b")));
        assertEquals("a___b", DefaultEntitySearchKeyStrategy.INSTANCE.buildKey(List.of("a", "_b")));

        IEntitySearchKeyStrategy strategy = DefaultEntitySearchKeyStrategy.INSTANCE;
        assertFalse(strategy.isEncodableSegment("a_"), "a trailing separator is ambiguous");
        assertFalse(strategy.isEncodableSegment("_b"), "a leading separator is ambiguous");
        assertFalse(strategy.isEncodableSegment("_"));
        // an underscore INSIDE a name is fine, however many of them
        assertTrue(strategy.isEncodableSegment("press_kit"));
        assertTrue(strategy.isEncodableSegment("b__c"));
        assertTrue(strategy.isEncodableSegment("a___b"));
        assertTrue(strategy.isEncodableSegment(null));
    }

    @Test
    void anUnencodableSegmentShouldBeRejectedWhenTheTypeIsSaved() {
        ApsEntity entity = entity(composite("a_", booleanAttr("b", true)));
        List<KeyProblem> problems = NestedBooleanSearchSupport.validateNestedBooleanKeys(entity);
        assertEquals(1, problems.size());
        assertEquals(KeyProblemType.AMBIGUOUS_SEGMENT, problems.get(0).type());
        assertEquals(List.of("a_ > b"), problems.get(0).paths());
        assertTrue(problems.get(0).getDescription().contains("must not begin or end"),
                problems.get(0).getDescription());

        // the child side too
        assertEquals(KeyProblemType.AMBIGUOUS_SEGMENT, NestedBooleanSearchSupport
                .validateNestedBooleanKeys(entity(composite("a", booleanAttr("_b", true))))
                .get(0).type());
    }

    @Test
    void aTopLevelNameMayStillTouchTheSeparator() {
        // only a nested path is encoded; a top-level key is the raw name, so nothing is ambiguous there
        ApsEntity entity = entity(booleanAttr("_flag_", true));
        assertTrue(NestedBooleanSearchSupport.validateNestedBooleanKeys(entity).isEmpty());
        assertEquals("_flag_",
                NestedBooleanSearchSupport.collectSearchable(entity).get(0).key());
    }

    @Test
    void underscoresInsideANameRemainPerfectlyLegal() {
        // the case that must NOT be rejected: doubled underscores inside a segment encode as an even run
        ApsEntity entity = entity(composite("a", booleanAttr("b__c", true)));
        assertTrue(NestedBooleanSearchSupport.validateNestedBooleanKeys(entity).isEmpty());
        assertEquals("a_b____c", NestedBooleanSearchSupport.collectSearchable(entity).get(0).key());
        assertEquals("a > b__c", NestedBooleanSearchSupport.buildSearchLabels(entity).get("a_b____c"));
    }

    @Test
    void aCustomStrategyMayDeclareEverySegmentEncodable() {
        // the default rule belongs to the default ENCODING, not to the platform: a strategy that
        // delimits differently simply does not inherit the restriction
        IEntitySearchKeyStrategy dotted = new DefaultEntitySearchKeyStrategy() {
            @Override
            public String buildKey(List<String> segments) {
                return String.join(".", segments);
            }

            @Override
            public boolean isEncodableSegment(String segment) {
                return true;
            }
        };
        ApsEntity entity = entity(composite("a_", booleanAttr("b", true)));
        assertTrue(NestedBooleanSearchSupport.validateNestedBooleanKeys(entity, dotted).isEmpty());
        assertEquals("a_.b", NestedBooleanSearchSupport.collectSearchable(entity, dotted).get(0).key());
    }

    @Test
    void theSupportConstantsShouldStillReportTheDefaultStrategysValues() {
        // kept as the default's values rather than deleted: they are public API
        assertEquals(DefaultEntitySearchKeyStrategy.DEFAULT_MAX_KEY_LENGTH,
                NestedBooleanSearchSupport.MAX_SEARCH_KEY_LENGTH);
        assertEquals(DefaultEntitySearchKeyStrategy.KEY_SEPARATOR,
                NestedBooleanSearchSupport.KEY_SEPARATOR);
        assertEquals(DefaultEntitySearchKeyStrategy.LABEL_SEPARATOR,
                NestedBooleanSearchSupport.LABEL_SEPARATOR);
        assertEquals(DefaultEntitySearchKeyStrategy.ESCAPED_KEY_SEPARATOR,
                NestedBooleanSearchSupport.ESCAPED_KEY_SEPARATOR);
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
