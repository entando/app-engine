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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.entando.entando.aps.system.common.entity.search.NestedSearchSupport.ClearedFlagReason;
import org.entando.entando.aps.system.common.entity.search.NestedSearchSupport.ClearedSearchableFlag;
import org.entando.entando.aps.system.common.entity.search.NestedSearchSupport.KeyProblem;
import org.entando.entando.aps.system.common.entity.search.NestedSearchSupport.KeyProblemType;
import com.agiletec.aps.system.common.entity.model.ApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CheckBoxAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.DateAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.EnumeratorAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoListAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.NumberAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoTextAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.ThreeStateAttribute;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NestedSearchSupport} - the shared logic used to index/resolve
 * boolean attributes nested inside a Composite in the DB search tables (Solr-disabled path).
 */
class NestedSearchSupportTest {

    /** The bound a manager passes in; here the width of the tables the platform ships. */
    private static final int MAX_KEY_LENGTH = EntitySearchKeys.DEFAULT_MAX_KEY_LENGTH;

    @Test
    void isIndexableNested_shouldAcceptAllBooleanLikes() {
        assertTrue(NestedSearchSupport.isIndexableNested(booleanAttr("b", true, Boolean.TRUE)));
        assertTrue(NestedSearchSupport.isIndexableNested(checkBox("c", true)));
        assertTrue(NestedSearchSupport.isIndexableNested(threeState("t", true)));
        assertFalse(NestedSearchSupport.isIndexableNested(new MonoTextAttribute()));
        assertFalse(NestedSearchSupport.isIndexableNested(null));
    }

    @Test
    void shouldResolveCompositeNestedBoolean() {
        BooleanAttribute certified = booleanAttr("certified", true, Boolean.TRUE);
        ApsEntity entity = entity(composite("address", certified));
        assertSame(certified, NestedSearchSupport.resolveNestedByKey(entity, "address_certified"));
    }

    @Test
    void shouldResolveDeepCompositePath() {
        BooleanAttribute leaf = booleanAttr("c", true, Boolean.TRUE);
        ApsEntity entity = entity(composite("a", composite("b", leaf)));
        assertSame(leaf, NestedSearchSupport.resolveNestedByKey(entity, "a_b_c"));
    }

    @Test
    void shouldReturnNullForUnknownKey() {
        ApsEntity entity = entity(composite("address", booleanAttr("certified", true, Boolean.TRUE)));
        assertNull(NestedSearchSupport.resolveNestedByKey(entity, "address_missing"));
        assertNull(NestedSearchSupport.resolveNestedByKey(entity, "nope"));
        assertNull(NestedSearchSupport.resolveNestedByKey(null, "address_certified"));
        assertNull(NestedSearchSupport.resolveNestedByKey(entity, null));
    }

    @Test
    void shouldResolveNestedCheckBoxAndThreeState() {
        CheckBoxAttribute verified = checkBox("verified", true);
        ThreeStateAttribute maybe = threeState("maybe", true);
        ApsEntity entity = entity(composite("address", verified, maybe));
        assertSame(verified, NestedSearchSupport.resolveNestedByKey(entity, "address_verified"));
        assertSame(maybe, NestedSearchSupport.resolveNestedByKey(entity, "address_maybe"));
    }

    @Test
    void shouldNotResolveListReachedBoolean() {
        ApsEntity listOfBoolean = entity(monolist("tags", booleanAttr("flag", true, Boolean.TRUE)));
        assertNull(NestedSearchSupport.resolveNestedByKey(listOfBoolean, "tags_flag"));

        ApsEntity listOfComposite = entity(monolist("rows", composite("row", booleanAttr("active", true, Boolean.TRUE))));
        assertNull(NestedSearchSupport.resolveNestedByKey(listOfComposite, "rows_row_active"));
    }

    @Test
    void shouldNotResolveTopLevelAttribute() {
        // the resolver only matches nested booleans; top-level precedence is handled by the caller
        ApsEntity entity = entity(booleanAttr("flag", true, Boolean.TRUE));
        assertNull(NestedSearchSupport.resolveNestedByKey(entity, "flag"));
    }

    @Test
    void shouldSkipCompositeWithNullChildrenList() {
        CompositeAttribute composite = spy(composite("address", booleanAttr("certified", true, Boolean.TRUE)));
        when(composite.getAttributes()).thenReturn(null);
        ApsEntity entity = entity(composite);
        assertNull(NestedSearchSupport.resolveNestedByKey(entity, "address_certified"));
    }

    @Test
    void shouldNotResolveNestedNonBooleanAttribute() {
        // a non boolean-like simple attribute nested in a Composite is never eligible, whatever the key
        MonoTextAttribute note = new MonoTextAttribute();
        note.setName("note");
        ApsEntity entity = entity(composite("address", note));
        assertNull(NestedSearchSupport.resolveNestedByKey(entity, "address_note"));
    }

    // --- collectSearchable (search-form enumeration) -----------------------

    @Test
    void collectSearchable_shouldReturnEmptyForNullEntity() {
        assertTrue(NestedSearchSupport.collectSearchable(null).isEmpty());
    }

    @Test
    void collectSearchable_shouldKeepSearchableTopLevelAttributesUnchanged() {
        // any searchable top-level attribute is offered (legacy behaviour), non-searchable ones are not
        BooleanAttribute topFlag = booleanAttr("flag", true, Boolean.TRUE);
        MonoTextAttribute title = monoText("title", true);
        MonoTextAttribute hidden = monoText("hidden", false);
        ApsEntity entity = entity(topFlag, title, hidden);
        List<SearchableAttributeRef> result = NestedSearchSupport.collectSearchable(entity);
        assertEquals(2, result.size());
        // a top-level attribute is keyed by its own name and carries the very attribute, never a copy
        assertEquals("flag", result.get(0).key());
        assertSame(topFlag, result.get(0).source());
        assertEquals("title", result.get(1).key());
        assertSame(title, result.get(1).source());
    }

    @Test
    void collectSearchable_shouldExposeCompositeNestedBooleanUnderPathKey() {
        BooleanAttribute certified = booleanAttr("certified", true, Boolean.TRUE);
        certified.setType("Boolean");
        ApsEntity entity = entity(composite("address", certified));
        List<SearchableAttributeRef> result = NestedSearchSupport.collectSearchable(entity);
        assertEquals(1, result.size());
        SearchableAttributeRef ref = result.get(0);
        // the key is the path; the attribute itself is the real one, not a renamed copy
        assertEquals("address_certified", ref.key());
        assertEquals("address > certified", ref.label());
        assertSame(certified, ref.source());
        assertEquals("certified", certified.getName());
        // the JavaBean accessors the finder JSPs read through OGNL
        assertEquals("address_certified", ref.getName());
        assertEquals("Boolean", ref.getType());
        assertFalse(ref.isTextAttribute());
        // dispatch happens on the real attribute, so instanceof keeps working without a stand-in
        assertInstanceOf(BooleanAttribute.class, ref.source());
    }

    @Test
    void collectSearchable_shouldExposeAllBooleanLikesNested() {
        ApsEntity entity = entity(composite("address",
                booleanAttr("b", true, Boolean.TRUE), checkBox("c", true), threeState("t", true)));
        List<SearchableAttributeRef> result = NestedSearchSupport.collectSearchable(entity);
        assertEquals(3, result.size());
        assertEquals("address_b", result.get(0).key());
        assertEquals("address_c", result.get(1).key());
        assertEquals("address_t", result.get(2).key());
    }

    @Test
    void collectSearchable_shouldExcludeNonSearchableNestedBoolean() {
        ApsEntity entity = entity(composite("address", booleanAttr("certified", false, Boolean.TRUE)));
        assertTrue(NestedSearchSupport.collectSearchable(entity).isEmpty());
    }

    @Test
    void collectSearchable_shouldExcludeNonBooleanNestedAttribute() {
        ApsEntity entity = entity(composite("address", monoText("note", true)));
        assertTrue(NestedSearchSupport.collectSearchable(entity).isEmpty());
    }

    @Test
    void collectSearchable_shouldResolveDeepCompositePath() {
        ApsEntity entity = entity(composite("a", composite("b", booleanAttr("c", true, Boolean.TRUE))));
        List<SearchableAttributeRef> result = NestedSearchSupport.collectSearchable(entity);
        assertEquals(1, result.size());
        assertEquals("a_b_c", result.get(0).key());
    }

    @Test
    void collectSearchable_shouldNotDescendLists() {
        ApsEntity listOfBoolean = entity(monolist("tags", booleanAttr("flag", true, Boolean.TRUE)));
        assertTrue(NestedSearchSupport.collectSearchable(listOfBoolean).isEmpty());

        ApsEntity listOfComposite = entity(monolist("rows",
                composite("row", booleanAttr("active", true, Boolean.TRUE))));
        assertTrue(NestedSearchSupport.collectSearchable(listOfComposite).isEmpty());
    }

    @Test
    void collectSearchable_shouldKeepBothTopLevelAndNested() {
        BooleanAttribute topFlag = booleanAttr("flag", true, Boolean.TRUE);
        ApsEntity entity = entity(topFlag, composite("address", booleanAttr("certified", true, Boolean.TRUE)));
        List<SearchableAttributeRef> result = NestedSearchSupport.collectSearchable(entity);
        assertEquals(2, result.size());
        assertSame(topFlag, result.get(0).source());
        assertEquals("address_certified", result.get(1).key());
    }

    // --- buildSearchLabels (hierarchical display labels) -------------------

    @Test
    void buildSearchLabels_shouldKeepSegmentBoundariesWhenNameContainsUnderscore() {
        // the reported defect: composite "compo" + boolean "cmp_bool" must read "compo > cmp_bool",
        // NOT "compo > cmp > bool" - the label is built from the real tree, not by splitting the key
        BooleanAttribute cmpBool = booleanAttr("cmp_bool", true, Boolean.TRUE);
        ApsEntity entity = entity(composite("compo", cmpBool));
        Map<String, String> labels = NestedSearchSupport.buildSearchLabels(entity);
        assertEquals(1, labels.size());
        // the key escapes the '_' inside the child name (compo_cmp__bool); the label never does
        assertEquals("compo > cmp_bool", labels.get("compo_cmp__bool"));
    }

    @Test
    void buildSearchLabels_shouldRenderDeepHierarchy() {
        ApsEntity entity = entity(composite("a", composite("b", booleanAttr("c", true, Boolean.TRUE))));
        assertEquals("a > b > c", NestedSearchSupport.buildSearchLabels(entity).get("a_b_c"));
    }

    @Test
    void buildSearchLabels_shouldLabelTopLevelByItsOwnName() {
        ApsEntity entity = entity(booleanAttr("flag", true, Boolean.TRUE), monoText("title", true));
        Map<String, String> labels = NestedSearchSupport.buildSearchLabels(entity);
        assertEquals("flag", labels.get("flag"));
        assertEquals("title", labels.get("title"));
    }

    @Test
    void buildSearchLabels_keyAndLabelShareTheSameSegments() {
        // alignment invariant: the label's segments, escaped and re-joined, reproduce the key exactly for
        // every entry - so label and machine key can never drift. Escaping is what the raw substitution
        // this test used to perform cannot express: 'compo > cmp_bool' keys as 'compo_cmp__bool'.
        ApsEntity entity = entity(
                booleanAttr("flag", true, Boolean.TRUE),
                composite("compo", booleanAttr("cmp_bool", true, Boolean.TRUE)),
                composite("a", composite("b", booleanAttr("c", true, Boolean.TRUE))));
        Map<String, String> labels = NestedSearchSupport.buildSearchLabels(entity);
        assertEquals(3, labels.size());
        for (Map.Entry<String, String> e : labels.entrySet()) {
            assertEquals(e.getKey(), expectedKey(e.getValue()));
        }
    }

    /** The key a label's hierarchy must produce: each segment escaped, joined by the key separator. */
    private String expectedKey(String label) {
        String[] segments = label.split(EntitySearchKeys.LABEL_SEPARATOR);
        if (1 == segments.length) {
            return segments[0];
        }
        return String.join(EntitySearchKeys.KEY_SEPARATOR,
                java.util.Arrays.stream(segments)
                        .map(seg -> seg.replace(EntitySearchKeys.KEY_SEPARATOR,
                                EntitySearchKeys.ESCAPED_KEY_SEPARATOR))
                        .toList());
    }

    @Test
    void buildSearchLabels_shouldNotDescendLists() {
        ApsEntity entity = entity(monolist("rows", composite("row", booleanAttr("active", true, Boolean.TRUE))));
        assertTrue(NestedSearchSupport.buildSearchLabels(entity).isEmpty());
    }

    // --- validateNestedSearchKeys (persist-time rejection) ----------------

    @Test
    void validate_shouldAcceptNullEntityAndCleanType() {
        assertTrue(NestedSearchSupport.validateNestedSearchKeys(null, MAX_KEY_LENGTH).isEmpty());
        ApsEntity entity = entity(booleanAttr("flag", true, Boolean.TRUE),
                composite("compo", booleanAttr("certified", true, Boolean.TRUE)));
        assertTrue(NestedSearchSupport.validateNestedSearchKeys(entity, MAX_KEY_LENGTH).isEmpty());
    }

    @Test
    void validate_shouldAcceptDifferentlyStructuredPathsThatUsedToCollide() {
        // R20: composite 'a_b' + child 'c' and composite 'a' + child 'b_c' both used to flatten to
        // 'a_b_c'. Escaping the separator inside each segment separates them, so this type - which the
        // previous encoding had to reject - is now legal. (Escaping does not cover a separator against a
        // segment boundary; that case is refused outright - see EntitySearchKeysTest.)
        ApsEntity entity = entity(
                composite("a_b", booleanAttr("c", true, Boolean.TRUE)),
                composite("a", booleanAttr("b_c", true, Boolean.TRUE)));
        assertTrue(NestedSearchSupport.validateNestedSearchKeys(entity, MAX_KEY_LENGTH).isEmpty());
        List<SearchableAttributeRef> offered = NestedSearchSupport.collectSearchable(entity);
        assertEquals(List.of("a__b_c", "a_b__c"), offered.stream().map(SearchableAttributeRef::key).toList());
        // and each still resolves back to its own attribute
        assertEquals("c", NestedSearchSupport.resolveNestedByKey(entity, "a__b_c").getName());
        assertEquals("b_c", NestedSearchSupport.resolveNestedByKey(entity, "a_b__c").getName());
    }

    @Test
    void validate_shouldStillRejectATopLevelNameEqualToANestedKey() {
        // the one collision escaping cannot rule out: top-level keys stay raw (that is what the search
        // tables have always stored), so a top-level attribute may still be named exactly like a key
        ApsEntity entity = entity(booleanAttr("a__b_c", true, Boolean.TRUE),
                composite("a_b", booleanAttr("c", true, Boolean.TRUE)));
        List<KeyProblem> problems = NestedSearchSupport.validateNestedSearchKeys(entity, MAX_KEY_LENGTH);
        assertEquals(1, problems.size());
        assertEquals(KeyProblemType.DUPLICATED, problems.get(0).type());
        assertEquals("a__b_c", problems.get(0).key());
        assertEquals(List.of("a__b_c", "a_b > c"), problems.get(0).paths());
    }

    @Test
    void validate_shouldRejectDuplicateAgainstTopLevelAttribute() {
        // a top-level attribute named 'compo_flag' occupies the key of composite 'compo' child 'flag'
        ApsEntity entity = entity(booleanAttr("compo_flag", true, Boolean.TRUE),
                composite("compo", booleanAttr("flag", true, Boolean.TRUE)));
        List<KeyProblem> problems = NestedSearchSupport.validateNestedSearchKeys(entity, MAX_KEY_LENGTH);
        assertEquals(1, problems.size());
        assertEquals(KeyProblemType.DUPLICATED, problems.get(0).type());
        assertEquals("compo_flag", problems.get(0).key());
        assertEquals(List.of("compo_flag", "compo > flag"), problems.get(0).paths());
    }

    @Test
    void validate_shouldIgnoreUnderscoreWithoutAnActualCollision() {
        // the shipped mitigation warned on every '_' in a name - ordinary snake_case, pure noise.
        // Only a key actually produced twice is a defect
        ApsEntity entity = entity(
                composite("compo", booleanAttr("cmp_bool", true, Boolean.TRUE)),
                composite("other_compo", booleanAttr("bool", true, Boolean.TRUE)),
                booleanAttr("top_flag", true, Boolean.TRUE));
        assertTrue(NestedSearchSupport.validateNestedSearchKeys(entity, MAX_KEY_LENGTH).isEmpty());
    }

    @Test
    void validate_shouldIgnoreDuplicateNotInvolvingANestedBoolean() {
        // a type that does not use the nested boolean feature is never rejected by this validation:
        // the non-searchable nested boolean produces no key at all, so nothing can collide
        ApsEntity entity = entity(booleanAttr("compo_flag", true, Boolean.TRUE),
                composite("compo", booleanAttr("flag", false, Boolean.TRUE)));
        assertTrue(NestedSearchSupport.validateNestedSearchKeys(entity, MAX_KEY_LENGTH).isEmpty());
    }

    @Test
    void validate_shouldRejectKeyLongerThanTheColumn() {
        String longComposite = "c".repeat(200);
        String longChild = "b".repeat(MAX_KEY_LENGTH - 200);
        ApsEntity entity = entity(composite(longComposite, booleanAttr(longChild, true, Boolean.TRUE)));
        List<KeyProblem> problems = NestedSearchSupport.validateNestedSearchKeys(entity, MAX_KEY_LENGTH);
        assertEquals(1, problems.size());
        assertEquals(KeyProblemType.TOO_LONG, problems.get(0).type());
        // 200 + '_' + 55 = 256, one over the limit
        assertEquals(MAX_KEY_LENGTH + 1, problems.get(0).key().length());
        assertEquals(List.of(longComposite + " > " + longChild), problems.get(0).paths());
    }

    @Test
    void validate_shouldAcceptKeyExactlyAtTheLimit() {
        String longComposite = "c".repeat(200);
        String longChild = "b".repeat(MAX_KEY_LENGTH - 201);
        ApsEntity entity = entity(composite(longComposite, booleanAttr(longChild, true, Boolean.TRUE)));
        assertTrue(NestedSearchSupport.validateNestedSearchKeys(entity, MAX_KEY_LENGTH).isEmpty());
    }

    @Test
    void validate_shouldNotBoundTopLevelAttributeNames() {
        // length is a nested-key concern: a long top-level name is legacy behaviour, left untouched
        ApsEntity entity = entity(monoText("t".repeat(300), true));
        assertTrue(NestedSearchSupport.validateNestedSearchKeys(entity, MAX_KEY_LENGTH).isEmpty());
    }

    @Test
    void validate_shouldReportEveryProblemAtOnce() {
        // the only remaining duplicate shape (a top-level name equal to a nested key) plus a too-long key
        String longComposite = "c".repeat(260);
        ApsEntity entity = entity(
                booleanAttr("compo_flag", true, Boolean.TRUE),
                composite("compo", booleanAttr("flag", true, Boolean.TRUE)),
                composite(longComposite, booleanAttr("flag", true, Boolean.TRUE)));
        List<KeyProblem> problems = NestedSearchSupport.validateNestedSearchKeys(entity, MAX_KEY_LENGTH);
        assertEquals(2, problems.size());
        assertEquals(KeyProblemType.DUPLICATED, problems.get(0).type());
        assertEquals(KeyProblemType.TOO_LONG, problems.get(1).type());
    }

    @Test
    void validate_descriptionShouldNameTheKeyAndThePaths() {
        ApsEntity entity = entity(
                booleanAttr("compo_flag", true, Boolean.TRUE),
                composite("compo", booleanAttr("flag", true, Boolean.TRUE)));
        String description = NestedSearchSupport.validateNestedSearchKeys(entity, MAX_KEY_LENGTH).get(0).getDescription();
        assertTrue(description.contains("compo_flag"));
        assertTrue(description.contains("compo > flag"));
    }

    // --- the isActive() gate (R4) -------------------------------------------

    @Test
    void collectSearchable_shouldExcludeADisabledTopLevelAttribute() {
        // pre-existing behaviour, unchanged: disabling is per top-level attribute, driven by matching
        // disabling codes (the user-profile-on-edit case is the only caller in the codebase)
        BooleanAttribute flag = booleanAttr("flag", true, Boolean.TRUE);
        flag.setDisablingCodes(new String[]{"onEdit"});
        BooleanAttribute kept = booleanAttr("kept", true, Boolean.TRUE);
        ApsEntity entity = entity(flag, kept);

        entity.disableAttributes("onEdit");

        List<SearchableAttributeRef> result = NestedSearchSupport.collectSearchable(entity);
        assertEquals(1, result.size());
        assertEquals("kept", result.get(0).key());
    }

    @Test
    void disablingAnEntityDoesNotReachCompositeChildren() {
        // This is why aligning the isActive() check on the nested branch is a no-op: disableAttributes
        // walks the top-level list only and disable() is not overridden to recurse, so a nested boolean
        // stays active even when its own disabling code matches.
        BooleanAttribute nested = booleanAttr("certified", true, Boolean.TRUE);
        nested.setDisablingCodes(new String[]{"onEdit"});
        CompositeAttribute composite = composite("address", nested);
        composite.setDisablingCodes(new String[]{"onEdit"});
        ApsEntity entity = entity(composite);

        entity.disableAttributes("onEdit");

        assertFalse(composite.isActive(), "the top-level Composite itself is disabled");
        assertTrue(nested.isActive(), "its child is not - disabling never recurses");
        // and the nested boolean is still offered, exactly as before the gate was aligned
        List<SearchableAttributeRef> result = NestedSearchSupport.collectSearchable(entity);
        assertEquals(1, result.size());
        assertEquals("address_certified", result.get(0).key());
    }

    @Test
    void collectSearchable_shouldExcludeANestedAttributeThatIsSomehowInactive() {
        // Unreachable through disableAttributes today (see above), so this pins the *intent* of the
        // aligned gate: if a nested attribute ever reports inactive, it must not be offered.
        BooleanAttribute nested = spy(booleanAttr("certified", true, Boolean.TRUE));
        when(nested.isActive()).thenReturn(false);
        ApsEntity entity = entity(composite("address", nested));
        assertTrue(NestedSearchSupport.collectSearchable(entity).isEmpty());
        // and it is not written under a path key either - nor under its bare name, which would collide
        assertTrue(NestedSearchSupport.planSearchRecords(entity).isEmpty());
    }

    // --- forEachIndexableNested (the API the Solr write paths share) ---

    @Test
    void forEachIndexableNested_shouldVisitCompositeDescendantsWithTheirFullPath() {
        CheckBoxAttribute verified = checkBox("verified", true);
        ThreeStateAttribute deep = threeState("maybe", true);
        CompositeAttribute composite = composite("address", verified, composite("inner", deep));
        Map<String, AttributeInterface> visited = new LinkedHashMap<>();
        NestedSearchSupport.forEachIndexableNested(composite, (child, path) -> visited.put(path, child));
        assertEquals(List.of("address_verified", "address_inner_maybe"), List.copyOf(visited.keySet()));
        assertSame(verified, visited.get("address_verified"));
        assertSame(deep, visited.get("address_inner_maybe"));
    }

    @Test
    void forEachIndexableNested_shouldSkipNonSearchableAndNonBoolean() {
        CompositeAttribute composite = composite("address",
                booleanAttr("off", false, Boolean.TRUE), monoText("note", true), checkBox("on", true));
        Map<String, AttributeInterface> visited = new LinkedHashMap<>();
        NestedSearchSupport.forEachIndexableNested(composite, (child, path) -> visited.put(path, child));
        assertEquals(List.of("address_on"), List.copyOf(visited.keySet()));
    }

    @Test
    void forEachIndexableNested_shouldNeverDescendAList() {
        // the list exclusion that the three Solr write paths used to express three different ways
        Map<String, AttributeInterface> visited = new LinkedHashMap<>();
        NestedSearchSupport.forEachIndexableNested(
                monolist("rows", composite("row", booleanAttr("active", true, Boolean.TRUE))), (child, path) -> visited.put(path, child));
        assertTrue(visited.isEmpty());

        NestedSearchSupport.forEachIndexableNested(
                composite("outer", monolist("rows", booleanAttr("active", true, Boolean.TRUE))), (child, path) -> visited.put(path, child));
        assertTrue(visited.isEmpty());
    }

    @Test
    void forEachIndexableNested_shouldVisitNothingForSimpleOrNullAttribute() {
        Map<String, AttributeInterface> visited = new LinkedHashMap<>();
        NestedSearchSupport.forEachIndexableNested(booleanAttr("flag", true, Boolean.TRUE), (child, path) -> visited.put(path, child));
        NestedSearchSupport.forEachIndexableNested(null, (child, path) -> visited.put(path, child));
        assertTrue(visited.isEmpty());
    }

    // --- planSearchRecords (the whole DB write plan) -----------------------

    @Test
    void planSearchRecords_shouldKeyNestedBooleansByPathAndEverythingElseByName() {
        BooleanAttribute nested = booleanAttr("certified", true, Boolean.TRUE);
        BooleanAttribute topLevel = booleanAttr("flag", true, Boolean.TRUE);
        MonoTextAttribute topText = monoText("title", true);
        BooleanAttribute nonSearchable = booleanAttr("off", false, Boolean.TRUE);
        ApsEntity entity = entity(topLevel, topText, composite("address", nested, nonSearchable));
        List<SearchRecordSpec> plan = NestedSearchSupport.planSearchRecords(entity);
        assertEquals(List.of("flag", "title", "address_certified"), attrNames(plan));
        assertSame(nested, plan.get(2).attribute());
        assertTrue(NestedSearchSupport.planSearchRecords(null).isEmpty());
    }

    @Test
    void planSearchRecords_shouldKeepIndexingListReachedAttributesUnderTheirOwnName() {
        // legacy "flattened" behaviour, untouched: a simple attribute inside a list is still indexed
        // under its bare name, because that is what the DB search tables have always held
        ApsEntity entity = entity(monolist("rows", composite("row", monoText("note", true))));
        assertEquals(List.of("note"), attrNames(NestedSearchSupport.planSearchRecords(entity)));
    }

    @Test
    void planSearchRecords_shouldSkipABooleanReachedThroughAListInsideAComposite() {
        // it cannot be path-qualified (the list occurs many times per entity) and its bare name would
        // collide with a same-named top-level attribute - so no record at all
        ApsEntity entity = entity(monolist("rows", composite("row", booleanAttr("active", true, Boolean.TRUE))));
        assertTrue(NestedSearchSupport.planSearchRecords(entity).isEmpty());
    }

    @Test
    void planSearchRecords_shouldIndexABooleanDirectlyInAListUnderItsOwnName() {
        // no Composite parent, so this predates nested boolean search and keeps its historical records
        ApsEntity entity = entity(monolist("flags", booleanAttr("active", true, Boolean.TRUE)));
        assertEquals(List.of("active"), attrNames(NestedSearchSupport.planSearchRecords(entity)));
    }

    @Test
    void planSearchRecords_shouldIndexANestedNonBooleanUnderItsOwnName() {
        // only nested-searchable types get a path key; anything else a type XML manages to flag keeps
        // the legacy bare-name behaviour rather than silently changing meaning
        ApsEntity entity = entity(composite("address", monoText("street", true)));
        assertEquals(List.of("street"), attrNames(NestedSearchSupport.planSearchRecords(entity)));
    }

    @Test
    void planSearchRecords_shouldNeverProduceARecordForAComplexAttributeItself() {
        CompositeAttribute composite = composite("address", booleanAttr("certified", true, Boolean.TRUE));
        composite.setSearchable(true);
        ApsEntity entity = entity(composite);
        List<SearchRecordSpec> plan = NestedSearchSupport.planSearchRecords(entity);
        assertEquals(List.of("address_certified"), attrNames(plan));
    }

    @Test
    void planSearchRecords_shouldEscapeTheSeparatorInsideEachSegment() {
        // R20 on the write side: the attrname the DAO stores is the escaped key, so two composites whose
        // names differ only in where the '_' falls write to two different rows
        ApsEntity entity = entity(
                composite("a_b", booleanAttr("c", true, Boolean.TRUE)),
                composite("a", booleanAttr("b_c", true, Boolean.TRUE)));
        assertEquals(List.of("a__b_c", "a_b__c"),
                attrNames(NestedSearchSupport.planSearchRecords(entity)));
    }

    @Test
    void planSearchRecords_shouldDistinguishSameNamedChildrenOfDifferentComposites() {
        BooleanAttribute first = booleanAttr("flag", true, Boolean.TRUE);
        BooleanAttribute second = booleanAttr("flag", true, Boolean.TRUE);
        ApsEntity entity = entity(composite("a", first), composite("b", second));
        List<SearchRecordSpec> plan = NestedSearchSupport.planSearchRecords(entity);
        assertEquals(List.of("a_flag", "b_flag"), attrNames(plan));
        assertSame(first, plan.get(0).attribute());
        assertSame(second, plan.get(1).attribute());
    }

    private List<String> attrNames(List<SearchRecordSpec> plan) {
        return plan.stream().map(SearchRecordSpec::attrName).toList();
    }

    // --- collectSearchable / resolveNestedByKey agreement -----------

    @Test
    void everyOfferedNestedKeyShouldResolveBackToItsAttribute() {
        // collectSearchable, resolveNestedByKey and planSearchRecords all share one traversal, so
        // their agreement is structural rather than a coincidence - this pins it, and would catch a
        // regression that re-introduced a second recursion
        BooleanAttribute simple = booleanAttr("certified", true, Boolean.TRUE);
        CheckBoxAttribute withUnderscore = checkBox("cmp_bool", true);
        ThreeStateAttribute deep = threeState("maybe", true);
        ApsEntity entity = entity(
                booleanAttr("flag", true, Boolean.TRUE),
                monoText("title", true),
                composite("address", simple, withUnderscore),
                composite("outer_one", composite("inner", deep)),
                monolist("rows", composite("row", booleanAttr("ignored", true, Boolean.TRUE))));
        Map<String, String> labels = NestedSearchSupport.buildSearchLabels(entity);
        List<SearchableAttributeRef> offered = NestedSearchSupport.collectSearchable(entity);
        List<String> planned = attrNames(NestedSearchSupport.planSearchRecords(entity));
        assertEquals(labels.size(), offered.size());
        for (SearchableAttributeRef ref : offered) {
            String key = ref.key();
            assertEquals(ref.label(), labels.get(key), "no matching label for offered key " + key);
            boolean topLevel = null != entity.getAttribute(key);
            AttributeInterface resolved = NestedSearchSupport.resolveNestedByKey(entity, key);
            if (topLevel) {
                assertNull(resolved, "top-level key " + key + " must not resolve as nested");
            } else {
                assertSame(ref.source(), resolved, "offered key " + key + " does not resolve back");
            }
            // the DB writer indexes every offered key under exactly that key
            assertTrue(planned.contains(key), "writer and form disagree on " + key);
            assertEquals(key, expectedKey(ref.label()),
                    "label and key of " + key + " were built from different segments");
        }
        // the writer plans exactly the offered keys - nothing more: the list-reached boolean is skipped
        assertEquals(offered.stream().map(SearchableAttributeRef::key).toList(), planned);
        assertSame(simple, NestedSearchSupport.resolveNestedByKey(entity, "address_certified"));
        assertSame(withUnderscore,
                NestedSearchSupport.resolveNestedByKey(entity, "address_cmp__bool"));
        assertSame(deep,
                NestedSearchSupport.resolveNestedByKey(entity, "outer__one_inner_maybe"));
        // the pre-escaping keys must no longer resolve to anything
        assertNull(NestedSearchSupport.resolveNestedByKey(entity, "address_cmp_bool"));
        assertNull(NestedSearchSupport.resolveNestedByKey(entity, "outer_one_inner_maybe"));
    }

    // --- normalizeSearchableFlags (persist-time correction) ----------------

    @Test
    void normalize_shouldClearTheFlagOnCompositeChildrenOfATypeThatDoesNotSupportNestedSearch() {
        MonoTextAttribute text = monoText("note", true);
        DateAttribute date = dateAttr("since", true);
        NumberAttribute number = numberAttr("qty", true);
        EnumeratorAttribute enumerator = enumeratorAttr("choice", true);
        ApsEntity entity = entity(composite("address", text, date, number, enumerator));

        List<ClearedSearchableFlag> cleared = NestedSearchSupport.normalizeSearchableFlags(entity);

        assertEquals(4, cleared.size());
        assertTrue(cleared.stream()
                .allMatch(flag -> ClearedFlagReason.TYPE_NOT_SUPPORTED == flag.reason()));
        assertEquals(List.of("address" + EntitySearchKeys.LABEL_SEPARATOR + "note",
                        "address" + EntitySearchKeys.LABEL_SEPARATOR + "since",
                        "address" + EntitySearchKeys.LABEL_SEPARATOR + "qty",
                        "address" + EntitySearchKeys.LABEL_SEPARATOR + "choice"),
                cleared.stream().map(ClearedSearchableFlag::getJoinedPath).toList());
        assertFalse(text.isSearchable());
        assertFalse(date.isSearchable());
        assertFalse(number.isSearchable());
        assertFalse(enumerator.isSearchable());
        // and the collision the whole path-key design exists to prevent is gone
        assertTrue(NestedSearchSupport.planSearchRecords(entity).isEmpty());
    }

    @Test
    void normalize_shouldKeepTheFlagOnEveryTypeThatDeclaresItselfNestedSearchable() {
        BooleanAttribute bool = booleanAttr("certified", true, Boolean.TRUE);
        CheckBoxAttribute checkBox = checkBox("agreed", true);
        ThreeStateAttribute threeState = threeState("maybe", true);
        // a custom type opts in on its own - no platform change, the contract asserted end to end
        NestedSearchableTextAttribute custom = new NestedSearchableTextAttribute();
        custom.setName("custom");
        custom.setSearchable(true);
        ApsEntity entity = entity(composite("address", bool, checkBox, threeState, custom));

        assertTrue(NestedSearchSupport.normalizeSearchableFlags(entity).isEmpty());

        assertTrue(bool.isSearchable());
        assertTrue(checkBox.isSearchable());
        assertTrue(threeState.isSearchable());
        assertTrue(custom.isSearchable());
        assertEquals(List.of("address_certified", "address_agreed", "address_maybe", "address_custom"),
                attrNames(NestedSearchSupport.planSearchRecords(entity)));
    }

    @Test
    void normalize_shouldClearANestableChildWhoseCompositeIsInsideAList() {
        // the clearSearchableWithinList rule, enforced for every configuration path rather than only in
        // the composite editor: the list occurs many times per entity, so no path addresses one value
        BooleanAttribute nested = booleanAttr("active", true, Boolean.TRUE);
        ApsEntity entity = entity(monolist("rows", composite("row", nested)));

        List<ClearedSearchableFlag> cleared = NestedSearchSupport.normalizeSearchableFlags(entity);

        assertEquals(1, cleared.size());
        assertEquals(ClearedFlagReason.PATH_NOT_ADDRESSABLE, cleared.get(0).reason());
        assertEquals("rows" + EntitySearchKeys.LABEL_SEPARATOR + "row"
                + EntitySearchKeys.LABEL_SEPARATOR + "active", cleared.get(0).getJoinedPath());
        assertSame(nested, cleared.get(0).attribute());
        assertFalse(nested.isSearchable());
    }

    @Test
    void normalize_shouldLeaveChildrenOfAListAlone() {
        // REGRESSION GUARD. A simple attribute directly inside a List/Monolist is still indexed under its
        // own name - long-standing flattened behaviour that existing list filters depend on. Clearing it
        // here would silently break every one of them; treat any edit to this as a breaking change.
        MonoTextAttribute listedText = monoText("note", true);
        BooleanAttribute listedBoolean = booleanAttr("flag", true, Boolean.TRUE);
        ApsEntity entity = entity(monolist("notes", listedText), monolist("flags", listedBoolean));

        assertTrue(NestedSearchSupport.normalizeSearchableFlags(entity).isEmpty());

        assertTrue(listedText.isSearchable());
        assertTrue(listedBoolean.isSearchable());
        assertEquals(List.of("note", "flag"),
                attrNames(NestedSearchSupport.planSearchRecords(entity)));
    }

    @Test
    void normalize_shouldLeaveTopLevelAttributesAloneWhateverTheirType() {
        MonoTextAttribute text = monoText("title", true);
        DateAttribute date = dateAttr("published", true);
        NumberAttribute number = numberAttr("rank", true);
        ApsEntity entity = entity(text, date, number);

        assertTrue(NestedSearchSupport.normalizeSearchableFlags(entity).isEmpty());

        assertTrue(text.isSearchable());
        assertTrue(date.isSearchable());
        assertTrue(number.isSearchable());
        assertEquals(List.of("title", "published", "rank"),
                attrNames(NestedSearchSupport.planSearchRecords(entity)));
    }

    @Test
    void normalize_shouldReportTheTypeOfTheOffendingAttributeAndBeIdempotent() {
        MonoTextAttribute text = monoText("note", true);
        text.setType("Monotext");
        ApsEntity entity = entity(composite("address", text));

        List<ClearedSearchableFlag> first = NestedSearchSupport.normalizeSearchableFlags(entity);
        assertEquals(1, first.size());
        assertEquals("note", first.get(0).getAttributeName());
        assertEquals("Monotext", first.get(0).getAttributeType());
        assertSame(text, first.get(0).attribute());

        // a second pass has nothing left to do - the report is of what changed, not of what is wrong
        assertTrue(NestedSearchSupport.normalizeSearchableFlags(entity).isEmpty());
    }

    @Test
    void normalize_shouldIgnoreANullEntityAndANonSearchableChild() {
        assertTrue(NestedSearchSupport.normalizeSearchableFlags(null).isEmpty());
        ApsEntity entity = entity(composite("address", monoText("note", false)));
        assertTrue(NestedSearchSupport.normalizeSearchableFlags(entity).isEmpty());
    }

    // --- helpers -----------------------------------------------------------

    /** A custom attribute type that opts into nested indexing without any platform change. */
    private static class NestedSearchableTextAttribute extends MonoTextAttribute {

        @Override
        public boolean isNestedSearchSupported() {
            return true;
        }
    }

    private DateAttribute dateAttr(String name, boolean searchable) {
        DateAttribute a = new DateAttribute();
        a.setName(name);
        a.setSearchable(searchable);
        return a;
    }

    private NumberAttribute numberAttr(String name, boolean searchable) {
        NumberAttribute a = new NumberAttribute();
        a.setName(name);
        a.setSearchable(searchable);
        return a;
    }

    private EnumeratorAttribute enumeratorAttr(String name, boolean searchable) {
        EnumeratorAttribute a = new EnumeratorAttribute();
        a.setName(name);
        a.setSearchable(searchable);
        return a;
    }

    private MonoTextAttribute monoText(String name, boolean searchable) {
        MonoTextAttribute a = new MonoTextAttribute();
        a.setName(name);
        a.setSearchable(searchable);
        return a;
    }


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
