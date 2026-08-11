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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AbstractComplexAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;

/**
 * Single source of truth for indexing and searching attributes nested inside a Composite, on both search
 * back-ends. It sits beside the entity model because what it owns is a property of the attribute tree;
 * the relational search tables and the full-text engine are both consumers.
 *
 * <p>Eligibility is the attribute's own answer ({@link AttributeInterface#isNestedSearchSupported()},
 * default false), gated by the type's {@code searchable} flag. This class holds no list of types - a
 * custom attribute opts in by overriding that method. Today's shipped set happens to be the boolean-like
 * family ({@link BooleanAttribute} and subclasses).</p>
 *
 * <p>An eligible nested attribute is addressed by a path key, so it cannot collide with a same-named
 * attribute elsewhere. Only <b>Composite</b> ancestry qualifies: below a {@code List}/{@code Monolist} no
 * path identifies a single value, since a list occurs many times per entity. For the encoding see
 * {@link EntitySearchKeys}; a top-level attribute keeps its raw name, so the one residual
 * collision - a top-level name equal to a nested key - is rejected by
 * {@link #validateNestedSearchKeys(IApsEntity, int)} at persist time.</p>
 *
 * <p>{@link #walk} is the only descent; every entry point below goes through it, so a key a writer
 * produces and a key a reader resolves cannot drift apart:</p>
 * <ul>
 *   <li>{@link #collectSearchable(IApsEntity)} - what a search form offers;</li>
 *   <li>{@link #resolveNestedByKey(IApsEntity, String)} - key back to attribute;</li>
 *   <li>{@link #planSearchRecords(IApsEntity)} - the DB search-table write plan;</li>
 *   <li>{@link #forEachIndexableNested(AttributeInterface, BiConsumer)} - the Solr paths;</li>
 *   <li>{@link #validateNestedSearchKeys(IApsEntity, int)} - persist-time rejection of unusable keys;</li>
 *   <li>{@link #normalizeSearchableFlags(IApsEntity)} - persist-time correction of an unhonourable flag.</li>
 * </ul>
 */
public final class NestedSearchSupport {

    private NestedSearchSupport() {
        // utility class
    }

    /** The kind of defect {@link #validateNestedSearchKeys} reports. */
    public enum KeyProblemType {
        /** The same key is produced by more than one attribute path. */
        DUPLICATED,
        /** The key is longer than the storing manager's {@code attrname} column. */
        TOO_LONG,
        /**
         * A segment {@link EntitySearchKeys#buildKey} cannot encode unambiguously. Reported up front
         * rather than waiting for the collision it would eventually cause.
         */
        AMBIGUOUS_SEGMENT
    }

    /**
     * A defect on a search key, with the path(s) that produced it - one for
     * {@link KeyProblemType#TOO_LONG}, two or more for {@link KeyProblemType#DUPLICATED}.
     *
     * @param maxKeyLength the bound in force when it was found, so a message can quote the real limit.
     */
    public record KeyProblem(KeyProblemType type, String key, List<String> paths, int maxKeyLength)
            implements Serializable {

        public KeyProblem {
            paths = List.copyOf(paths);
        }

        /** The paths that produced the key, rendered as a single comma-separated string. */
        public String getJoinedPaths() {
            return String.join(", ", this.paths);
        }

        /** A self-contained English description, used for log and exception messages. */
        public String getDescription() {
            if (KeyProblemType.DUPLICATED == this.type) {
                return "search key '" + this.key + "' is produced by more than one attribute path ("
                        + this.getJoinedPaths() + ")";
            }
            if (KeyProblemType.AMBIGUOUS_SEGMENT == this.type) {
                return "attribute path '" + this.getJoinedPaths() + "' cannot be encoded unambiguously: "
                        + "a name must not begin or end with '" + EntitySearchKeys.KEY_SEPARATOR
                        + "', because there it "
                        + "cannot be told apart from the separator between two names";
            }
            return "search key '" + this.key + "' of attribute path '" + this.getJoinedPaths()
                    + "' is " + this.key.length() + " characters long, the maximum is "
                    + this.maxKeyLength;
        }
    }

    /**
     * Why {@link #normalizeSearchableFlags} had to clear an attribute's {@code searchable} flag.
     */
    public enum ClearedFlagReason {
        /**
         * The attribute's type does not declare {@link AttributeInterface#isNestedSearchSupported()}, so
         * it gets no path key and could only be written under its unqualified name - where it would
         * collide with a same-named attribute elsewhere in the type.
         */
        TYPE_NOT_SUPPORTED,
        /**
         * The type would support it, but a {@code List}/{@code Monolist} sits above the Composite: the
         * list occurs many times per entity, so no path identifies a single value.
         */
        PATH_NOT_ADDRESSABLE
    }

    /**
     * One attribute whose {@code searchable} flag {@link #normalizeSearchableFlags} cleared, with enough
     * context to tell a user which attribute it was and why.
     *
     * @param reason why the flag could not be honoured.
     * @param segments the names along the attribute's path, from the top-level attribute down to it.
     * @param attribute the attribute itself, already normalized.
     */
    public record ClearedSearchableFlag(ClearedFlagReason reason, List<String> segments,
            AttributeInterface attribute) {

        public ClearedSearchableFlag {
            segments = List.copyOf(segments);
        }

        /** The attribute's own name, i.e. the last path segment. */
        public String getAttributeName() {
            return this.attribute.getName();
        }

        /** The attribute's type code, quoted in the message a user sees. */
        public String getAttributeType() {
            return this.attribute.getType();
        }

        /** The path rendered for humans, independently of the machine key. */
        public String getJoinedPath() {
            return String.join(EntitySearchKeys.LABEL_SEPARATOR, this.segments);
        }

        /** A self-contained English description, used for log and exception messages. */
        public String getDescription() {
            if (ClearedFlagReason.TYPE_NOT_SUPPORTED == this.reason) {
                return "attribute '" + this.getJoinedPath() + "' of type '" + this.getAttributeType()
                        + "' cannot be searchable inside a Composite: the type does not support nested "
                        + "search";
            }
            return "attribute '" + this.getJoinedPath() + "' cannot be searchable: its Composite is "
                    + "nested in a list, which occurs many times per entity";
        }
    }

    /**
     * Whether the attribute declares itself nested-searchable. The only place the engine asks, so every
     * consumer gets the same answer, and it is asked of the <b>attribute</b> - not of
     * {@link EntitySearchKeys}, which owns how a path becomes a key, not who may have one.
     *
     * @param attribute the attribute to test; null yields false.
     */
    public static boolean isIndexableNested(AttributeInterface attribute) {
        return null != attribute && attribute.isNestedSearchSupported();
    }

    /**
     * Resolve a Composite-nested boolean attribute from its path key. Shares {@link #walk} with the
     * writers, so a key any of them produces resolves back here by construction. Deliberately <b>not</b>
     * gated on {@code searchable}: a filter on an attribute whose flag was cleared after the fact still
     * needs its type resolved, and it simply matches no record.
     * @param entity the entity (or type prototype) to inspect.
     * @param key the path key.
     * @return the matching nested boolean-like attribute ({@code Boolean}, {@code CheckBox} or
     * {@code ThreeState}), or null if none matches.
     */
    public static AttributeInterface resolveNestedByKey(IApsEntity entity, String key) {
        if (null == entity || null == key) {
            return null;
        }
        AttributeInterface[] found = new AttributeInterface[1];
        walk(entity, visit -> {
            if (!visit.topLevel() && visit.pathIndexable() && null == found[0]
                    && isIndexableNested(visit.attribute())
                    && key.equals(EntitySearchKeys.buildKey(visit.segments()))) {
                found[0] = visit.attribute();
            }
        });
        return found[0];
    }

    /**
     * The attributes a search form should offer as filter criteria: every searchable top-level
     * attribute (unchanged legacy behaviour, any type) plus every nested-searchable attribute inside a
     * <b>Composite</b> whose inherited {@code searchable} flag is set. Nested attributes are returned
     * under their path key, which is exactly the key the DB search records were written under; the real
     * attribute travels along in {@link SearchableAttributeRef#source()} so callers can dispatch on its
     * actual type.
     * @param entity the entity (or type prototype) to inspect.
     * @return the ordered list of searchable attribute references; never null.
     */
    public static List<SearchableAttributeRef> collectSearchable(IApsEntity entity) {
        List<SearchableAttributeRef> result = new ArrayList<>();
        if (null == entity) {
            return result;
        }
        walk(entity, visit -> {
            if (isOffered(visit)) {
                result.add(new SearchableAttributeRef(EntitySearchKeys.buildKey(visit.segments()),
                        EntitySearchKeys.buildLabel(visit.segments()), visit.attribute()));
            }
        });
        return result;
    }

    /**
     * The human-readable label of every attribute {@link #collectSearchable} offers, keyed by the same
     * machine key. Callers (the search-form JSPs) render this verbatim instead of splitting the
     * flattened key, which would mis-segment a name containing '_'. Insertion order matches
     * {@link #collectSearchable}.
     * @param entity the entity (or type prototype) to inspect.
     * @return a map from machine key to display label; never null.
     * @deprecated the label travels with the attribute that owns it - read
     * {@link SearchableAttributeRef#getLabel()} off the refs {@link #collectSearchable} already returns,
     * or take {@link EntitySearchSchema} when a cached answer is wanted. This hands back a copy of what
     * its own refs were already carrying, and walks the type afresh on every call.
     */
    @Deprecated
    public static Map<String, String> buildSearchLabels(IApsEntity entity) {
        Map<String, String> labels = new LinkedHashMap<>();
        for (SearchableAttributeRef ref : collectSearchable(entity)) {
            labels.put(ref.key(), ref.label());
        }
        return labels;
    }

    /**
     * Every attribute to index, paired with its {@code attrname}, so {@code AbstractEntityDAO} neither
     * traverses the tree nor knows the naming rule. Behaviour is unchanged from the descent it replaced:
     * <ul>
     *   <li>an elementary attribute is indexed under its own name, top-level or reached through any
     *   complex attribute (legacy flattened behaviour, lists included);</li>
     *   <li><b>except</b> a nested-searchable child of a Composite, indexed under its path key when that
     *   path is addressable, and skipped entirely when it is not - it cannot be path-qualified, and its
     *   unqualified name would collide with a same-named top-level attribute.</li>
     * </ul>
     * <p>Complex attributes produce no record of their own; they are descended.</p>
     *
     * @param entity the entity to index; may be null.
     * @return the records to write, in tree order; never null.
     */
    public static List<SearchRecordSpec> planSearchRecords(IApsEntity entity) {
        List<SearchRecordSpec> specs = new ArrayList<>();
        if (null == entity) {
            return specs;
        }
        walk(entity, visit -> {
            AttributeInterface attribute = visit.attribute();
            if (!attribute.isSimple() || !attribute.isSearchable()) {
                return;
            }
            if (!visit.topLevel() && isOffered(visit)) {
                specs.add(new SearchRecordSpec(EntitySearchKeys.buildKey(visit.segments()), attribute));
            } else if (visit.topLevel() || !visit.parentIsComposite()
                    || !isIndexableNested(attribute)) {
                specs.add(new SearchRecordSpec(attribute.getName(), attribute));
            }
            // else: a nested-searchable child of a Composite with no addressable path - unindexable.
        });
        return specs;
    }

    /**
     * Visit every nested-searchable attribute under the given <b>top-level</b> attribute that is
     * eligible for path-based indexing, passing the attribute and its full path key (the top-level
     * attribute's own name included). Nothing is visited for a simple attribute, or for a
     * {@code List}/{@code Monolist}, or for a Composite with no eligible descendant.
     * @param topLevelAttribute the top-level attribute to descend; may be null.
     * @param visitor receives each eligible attribute and its path key.
     */
    public static void forEachIndexableNested(AttributeInterface topLevelAttribute,
            BiConsumer<AttributeInterface, String> visitor) {
        if (!(topLevelAttribute instanceof CompositeAttribute)) {
            return;
        }
        walk(((AbstractComplexAttribute) topLevelAttribute).getAttributes(),
                Collections.singletonList(topLevelAttribute.getName()), true, true,
                visit -> {
                    if (isOffered(visit)) {
                        visitor.accept(visit.attribute(),
                                EntitySearchKeys.buildKey(visit.segments()));
                    }
                });
    }

    /**
     * Validate the keys a type would write, so a defect surfaces at save time rather than at index time.
     * Three checks over the key set {@link #collectSearchable} offers: <b>duplicates</b> (with escaping
     * two nested paths cannot collide, so what remains is a top-level name equal to a nested key, since
     * top-level keys are stored unescaped), <b>ambiguous segment</b>, and <b>length</b> against the
     * bound passed in.
     *
     * <p>Only keys involving nested search are reported, so a type that does not use the feature can
     * never be rejected here.</p>
     *
     * @param entity the type to validate; may be null.
     * @param maxKeyLength the width of the {@code attrname} column of the manager that will store the
     * type - see {@code IEntityManager.getMaxSearchKeyLength()}.
     * @return the problems, in a stable order; never null.
     */
    public static List<KeyProblem> validateNestedSearchKeys(IApsEntity entity, int maxKeyLength) {
        List<KeyProblem> problems = new ArrayList<>();
        if (null == entity) {
            return problems;
        }
        Map<String, List<String>> pathsByKey = new LinkedHashMap<>();
        Set<String> nestedKeys = new LinkedHashSet<>();
        walk(entity, visit -> {
            if (!isOffered(visit)) {
                return;
            }
            String key = EntitySearchKeys.buildKey(visit.segments());
            String label = EntitySearchKeys.buildLabel(visit.segments());
            pathsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(label);
            if (!visit.topLevel()) {
                nestedKeys.add(key);
                // A top-level name is never checked: its key is the raw name, so no encoding applies to
                // it and any name is representable. Only a segment of a nested path has to survive the
                // encoding.
                if (!visit.segments().stream().allMatch(EntitySearchKeys::isEncodableSegment)) {
                    problems.add(new KeyProblem(KeyProblemType.AMBIGUOUS_SEGMENT, key, List.of(label),
                            maxKeyLength));
                }
            }
        });
        for (Map.Entry<String, List<String>> entry : pathsByKey.entrySet()) {
            if (entry.getValue().size() > 1 && nestedKeys.contains(entry.getKey())) {
                problems.add(new KeyProblem(KeyProblemType.DUPLICATED, entry.getKey(), entry.getValue(),
                        maxKeyLength));
            }
        }
        for (String key : nestedKeys) {
            if (key.length() > maxKeyLength) {
                problems.add(new KeyProblem(KeyProblemType.TOO_LONG, key, pathsByKey.get(key),
                        maxKeyLength));
            }
        }
        return problems;
    }

    /**
     * Clear the {@code searchable} flag wherever the tree cannot honour it, and report what was cleared.
     * The single enforcement point: it runs from
     * {@code ApsEntityManager.add/updateEntityPrototype}, so every write path inherits the rule.
     *
     * <p>Two cases, both children of a Composite: a type that does not declare
     * {@link AttributeInterface#isNestedSearchSupported()}, and a path made unaddressable by a
     * {@code List}/{@code Monolist} above the Composite.</p>
     *
     * <p><b>Children of a List/Monolist are left alone</b> - they are still indexed under their own names
     * and clearing them would break every existing list filter. That is the {@code parentIsComposite}
     * guard below; treat editing it as a breaking change. Top-level attributes are never touched.</p>
     *
     * <p>{@link ClearedFlagReason#PATH_NOT_ADDRESSABLE} cannot arise on a type <i>prototype</i>, whose
     * lists hold their structure in {@code getNestedAttributeType()} rather than in the elements
     * {@link #walk} descends. Left so: the flag is inert below a list, and a second descent would give
     * this class two traversals of different reach.</p>
     *
     * @param entity the type to normalize, modified in place; may be null.
     * @return every attribute whose flag was cleared, in traversal order; never null.
     */
    public static List<ClearedSearchableFlag> normalizeSearchableFlags(IApsEntity entity) {
        List<ClearedSearchableFlag> cleared = new ArrayList<>();
        if (null == entity) {
            return cleared;
        }
        walk(entity, visit -> {
            AttributeInterface attribute = visit.attribute();
            if (!attribute.isSearchable() || !visit.parentIsComposite()) {
                return;
            }
            // The type question first: an unsupported type is unsupported wherever it sits, and saying so
            // is more useful than blaming an enclosing list the user may not think of as relevant.
            if (!isIndexableNested(attribute)) {
                attribute.setSearchable(false);
                cleared.add(new ClearedSearchableFlag(ClearedFlagReason.TYPE_NOT_SUPPORTED,
                        visit.segments(), attribute));
            } else if (!visit.pathIndexable()) {
                attribute.setSearchable(false);
                cleared.add(new ClearedSearchableFlag(ClearedFlagReason.PATH_NOT_ADDRESSABLE,
                        visit.segments(), attribute));
            }
        });
        return cleared;
    }

    /**
     * Whether a search form offers this attribute, and equivalently whether it is path-indexed: active and
     * searchable, any type at top level (legacy), nested-searchable and addressable below a Composite.
     *
     * <p>The {@code isActive()} check below the top level is a no-op today - nothing clears {@code _active}
     * on a nested attribute, since {@code ApsEntity.disableAttributes} iterates the top-level list only -
     * but it is the behaviour we would want if disabling ever reached children.</p>
     */
    private static boolean isOffered(Visit visit) {
        AttributeInterface attribute = visit.attribute();
        return attribute.isActive() && attribute.isSearchable()
                && (visit.topLevel()
                || (visit.pathIndexable() && isIndexableNested(attribute)));
    }

    /** Walk an entity's whole attribute tree from the top level. */
    private static void walk(IApsEntity entity, StructureVisitor visitor) {
        walk(entity.getAttributeList(), null, false, true, visitor);
    }

    /**
     * Reports the <b>structure</b> only: every top-level attribute and every elementary attribute below a
     * complex one, with its path segments and the two facts a naming rule needs. Policy belongs to the
     * entry points above, never here.
     *
     * <p>Lists are descended, because their elementary children are still indexed under their own names,
     * but everything below one has {@code pathIndexable} false. Nested complex attributes are descended
     * without being reported.</p>
     *
     * @param parentSegments path segments of the enclosing complex attribute; null at top level.
     * @param parentIsComposite whether the enclosing complex attribute is a Composite.
     * @param pathIndexable whether no list has been crossed yet.
     */
    private static void walk(List<AttributeInterface> attributes, List<String> parentSegments,
            boolean parentIsComposite, boolean pathIndexable, StructureVisitor visitor) {
        if (null == attributes) {
            return;
        }
        boolean topLevel = (null == parentSegments);
        for (AttributeInterface attribute : attributes) {
            // singletonList, not List.of: an attribute with no name must not blow up a validation pass
            List<String> segments = topLevel
                    ? Collections.singletonList(attribute.getName())
                    : append(parentSegments, attribute.getName());
            if (topLevel || attribute.isSimple()) {
                visitor.visit(new Visit(attribute, segments, topLevel, parentIsComposite, pathIndexable));
            }
            if (attribute instanceof AbstractComplexAttribute complexAttribute) {
                boolean composite = attribute instanceof CompositeAttribute;
                walk(complexAttribute.getAttributes(), segments, composite,
                        pathIndexable && composite, visitor);
            }
        }
    }

    /**
     * One attribute as {@link #walk} reports it. {@code segments} are the names along the attribute's
     * path, from the top-level attribute down to the attribute itself; the strategy turns them into the
     * machine key and into the human hierarchy. Both come from the same segments, so they can never
     * drift apart, and no consumer ever needs to split a joined string back apart.
     *
     * @param attribute the attribute reported.
     * @param segments the names along its path.
     * @param topLevel whether it is a direct attribute of the entity.
     * @param parentIsComposite whether its direct parent is a Composite.
     * @param pathIndexable whether its path can address it, i.e. no List/Monolist above it.
     */
    private record Visit(AttributeInterface attribute, List<String> segments, boolean topLevel,
            boolean parentIsComposite, boolean pathIndexable) {

    }

    @FunctionalInterface
    private interface StructureVisitor {
        void visit(Visit visit);
    }

    private static List<String> append(List<String> segments, String name) {
        List<String> extended = new ArrayList<>(segments.size() + 1);
        extended.addAll(segments);
        extended.add(name);
        return Collections.unmodifiableList(extended);
    }

}
