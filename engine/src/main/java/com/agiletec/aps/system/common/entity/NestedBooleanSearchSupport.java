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
 * Single source of truth for indexing/searching boolean attributes nested inside a Composite
 * attribute, on both search back-ends.
 *
 * <p>A boolean nested in a Composite is addressed by a path key built from the names along its path, so
 * it cannot collide with a same-named attribute elsewhere. All boolean-like attributes are eligible -
 * {@link BooleanAttribute} and its subclasses {@code CheckBoxAttribute} and {@code ThreeStateAttribute},
 * which declare themselves nested-searchable through
 * {@link AttributeInterface#isNestedSearchSupported()} - governed by the {@code searchable} flag
 * inherited from the content type. Only <b>Composite</b> ancestry is supported: a boolean reached
 * through a {@code MonoListAttribute}/{@code ListAttribute} is never path-indexed, because a list
 * occurs many times per entity so the path would not identify a single value.</p>
 *
 * <p><b>The key encoding.</b> Segments are joined by {@link #KEY_SEPARATOR} ({@code '_'}), and because
 * that character is legal inside an attribute name it is <b>escaped by doubling</b> inside each segment.
 * So composite {@code a_b} + child {@code c} yields {@code a__b_c} while composite {@code a} + child
 * {@code b_c} yields {@code a_b__c}. Doubling alone is not sufficient - a run of separators against a
 * segment boundary stays ambiguous ({@code a_} + {@code b} and {@code a} + {@code _b} both give
 * {@code a___b}) - so the strategy additionally refuses a segment that begins or ends with the
 * separator ({@link IEntitySearchKeyStrategy#isEncodableSegment}). Together the two make the encoding
 * injective for every path the platform accepts. A <b>top-level</b> attribute keeps its raw name as its key,
 * unescaped and unchanged from before this feature existed, because that is what the search tables have
 * always stored; the one collision the encoding therefore cannot rule out is a top-level attribute whose
 * name is literally equal to a nested key, and {@link #validateNestedBooleanKeys(IApsEntity)} rejects
 * that at persist time. The escaping stays inside {@code [a-zA-Z0-9_]}, so it needs no widening of the
 * Solr field-name charset nor of the Lucene-injection allowlist in the searcher.</p>
 *
 * <p><b>One traversal owns the whole rule.</b> {@link #walk} is the only place that decides which
 * attributes are reachable, whether a path can address them and what that path is; every consumer goes
 * through one of the entry points below rather than re-implementing the descent:</p>
 * <ul>
 *   <li>{@link #collectSearchable(IApsEntity)} - what a search form offers (admin finders);</li>
 *   <li>{@link #resolveNestedBooleanByKey(IApsEntity, String)} - key back to attribute
 *   ({@code EntitySearchFilter.getInstance}, the remembered-search round-trip);</li>
 *   <li>{@link #planSearchRecords(IApsEntity)} - the whole write plan for the DB search table
 *   ({@code AbstractEntityDAO.addEntitySearchRecord}): every attribute to index, each with the
 *   {@code attrname} to index it under;</li>
 *   <li>{@link #forEachIndexableNestedBoolean(AttributeInterface, BiConsumer)} - the Solr schema
 *   checker, the document indexer and the content-type settings report;</li>
 *   <li>{@link #validateNestedBooleanKeys(IApsEntity)} - persist-time rejection of unusable keys;</li>
 *   <li>{@link #normalizeSearchableFlags(IApsEntity)} - persist-time correction of a {@code searchable}
 *   flag the tree cannot honour, so no builder has to enforce the rule for itself.</li>
 * </ul>
 * <p>Because the key a writer produces and the key a reader resolves come from the same traversal, they
 * cannot drift apart - which is exactly what happened when the list exclusion was expressed
 * independently in each write path.</p>
 *
 * <p>Because the key has to fit the {@code attrname} column, and because of the residual top-level
 * collision described above, {@link #validateNestedBooleanKeys(IApsEntity)} owns both checks and is
 * enforced when a content type is persisted, so neither can reach the index.</p>
 *
 * @author Entando
 */
public final class NestedBooleanSearchSupport {

    /** Separator used to render a nested attribute's hierarchy for humans (never occurs in a name). */
    public static final String LABEL_SEPARATOR = DefaultEntitySearchKeyStrategy.LABEL_SEPARATOR;

    /** Separator joining the path segments into the machine key (DB {@code attrname} / Solr field). */
    public static final String KEY_SEPARATOR = DefaultEntitySearchKeyStrategy.KEY_SEPARATOR;

    /**
     * How {@link #KEY_SEPARATOR} is represented <b>inside</b> a path segment: doubled - see
     * {@link DefaultEntitySearchKeyStrategy#ESCAPED_KEY_SEPARATOR}.
     */
    public static final String ESCAPED_KEY_SEPARATOR = DefaultEntitySearchKeyStrategy.ESCAPED_KEY_SEPARATOR;

    /**
     * Maximum length of a search key <b>under the default strategy</b>, matching the {@code attrname}
     * column of the search tables the platform ships. Keys longer than this are rejected at persist
     * time, so the writer can never hit a truncation (MySQL, non strict mode) or a failed transaction
     * (PostgreSQL).
     *
     * <p>An entity manager with a narrower or wider column does not have to live with this number: it
     * declares its own through {@link IEntityManager#getSearchKeyStrategy()}, and the overloads of the
     * entry points below that take an {@link IEntitySearchKeyStrategy} honour it. This constant is the
     * default's value, not an engine-wide law.</p>
     *
     * @see DefaultEntitySearchKeyStrategy#DEFAULT_MAX_KEY_LENGTH
     */
    public static final int MAX_SEARCH_KEY_LENGTH = DefaultEntitySearchKeyStrategy.DEFAULT_MAX_KEY_LENGTH;

    private NestedBooleanSearchSupport() {
        // utility class
    }

    /**
     * A null strategy means the default one - the same coalescing the manager and DAO setters do. Stated
     * once here so no entry point can NPE deep inside a traversal on a partially configured caller.
     */
    private static IEntitySearchKeyStrategy orDefault(IEntitySearchKeyStrategy strategy) {
        return (null != strategy) ? strategy : DefaultEntitySearchKeyStrategy.INSTANCE;
    }

    /**
     * The kind of defect {@link #validateNestedBooleanKeys} can report on a nested boolean search key.
     */
    public enum KeyProblemType {
        /** The same key is produced by more than one attribute path. */
        DUPLICATED,
        /** The key is longer than the storing manager's {@code attrname} column. */
        TOO_LONG,
        /**
         * A segment of the path cannot be encoded unambiguously by the strategy - for the default
         * encoding, a name that begins or ends with {@code '_'}. Reported instead of waiting for the
         * collision it would cause, which may only appear once a second attribute is added.
         */
        AMBIGUOUS_SEGMENT
    }

    /**
     * A defect found on a nested boolean search key: the key itself plus the human-readable
     * attribute path(s) that produced it (a single one for {@link KeyProblemType#TOO_LONG}, two or
     * more for {@link KeyProblemType#DUPLICATED}).
     *
     * @param type the kind of defect.
     * @param key the offending machine key.
     * @param paths the attribute path(s) that produced it, joined by {@link #LABEL_SEPARATOR}.
     * @param maxKeyLength the bound that was in force when the defect was found - the {@code attrname}
     * width the storing entity manager declared, not an engine-wide constant. Carried on the problem so
     * the message a user sees quotes the limit that actually applied.
     */
    public record KeyProblem(KeyProblemType type, String key, List<String> paths, int maxKeyLength)
            implements Serializable {

        public KeyProblem {
            paths = List.copyOf(paths);
        }

        /** A problem found under the default strategy's bound. */
        public KeyProblem(KeyProblemType type, String key, List<String> paths) {
            this(type, key, paths, MAX_SEARCH_KEY_LENGTH);
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
                        + "a name must not begin or end with '" + KEY_SEPARATOR + "', because there it "
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

        /** The path rendered for humans, independently of any key strategy. */
        public String getJoinedPath() {
            return String.join(LABEL_SEPARATOR, this.segments);
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
     * Whether the given attribute is eligible for nested (path-based) indexing, i.e. whether it declares
     * itself nested-searchable. Today that is exactly the boolean-like family - {@link BooleanAttribute}
     * and its subclasses CheckBox and ThreeState - because that is where
     * {@link AttributeInterface#isNestedSearchSupported()} is overridden; the platform no longer holds
     * the closed list, so a custom attribute type can opt in without a change here.
     *
     * <p>The question is put to the <b>attribute</b> and to nothing else, deliberately - and this method is
     * the only place the engine asks it, so every consumer gets the same answer. It is not routed through an
     * {@link IEntitySearchKeyStrategy}: a strategy that could widen or narrow eligibility would contradict
     * the gate in {@code CompositeAttribute}, which clears the {@code searchable} flag of a child that
     * cannot carry one and has no entity manager to consult; and the admin editor that decides whether to
     * offer the checkbox would have to guess which manager's strategy applies. A strategy owns how a path
     * becomes a key - separator, escaping, {@code attrname} width - not who is allowed to have one.</p>
     *
     * @param attribute the attribute to test; null yields false.
     * @return true if the attribute may be path-indexed when nested in a Composite.
     */
    public static boolean isIndexableNestedBoolean(AttributeInterface attribute) {
        return null != attribute && attribute.isNestedSearchSupported();
    }

    /**
     * Resolve a Composite-nested boolean attribute from its path key, under the default strategy.
     * @param entity the entity (or type prototype) to inspect.
     * @param key the path key.
     * @return the matching nested attribute, or null if none matches.
     */
    public static AttributeInterface resolveNestedBooleanByKey(IApsEntity entity, String key) {
        return resolveNestedBooleanByKey(entity, key, DefaultEntitySearchKeyStrategy.INSTANCE);
    }

    /**
     * Resolve a Composite-nested boolean attribute from its path key. Shares {@link #walk} with the
     * writers, so a key any of them produces resolves back here by construction - provided the same
     * strategy produced it. Deliberately <b>not</b> gated on {@code searchable}: a filter on an attribute
     * whose flag was cleared after the fact still needs its type resolved, and it simply matches no
     * record.
     * @param entity the entity (or type prototype) to inspect.
     * @param key the path key.
     * @param searchKeyStrategy the key strategy of the entity manager that owns this entity.
     * @return the matching nested boolean-like attribute ({@code Boolean}, {@code CheckBox} or
     * {@code ThreeState}), or null if none matches.
     */
    public static AttributeInterface resolveNestedBooleanByKey(IApsEntity entity, String key,
            IEntitySearchKeyStrategy searchKeyStrategy) {
        if (null == entity || null == key) {
            return null;
        }
        IEntitySearchKeyStrategy strategy = orDefault(searchKeyStrategy);
        AttributeInterface[] found = new AttributeInterface[1];
        walk(entity, visit -> {
            if (!visit.topLevel() && visit.pathIndexable() && null == found[0]
                    && isIndexableNestedBoolean(visit.attribute())
                    && key.equals(strategy.buildKey(visit.segments()))) {
                found[0] = visit.attribute();
            }
        });
        return found[0];
    }

    /**
     * The attributes a search form should offer as filter criteria, under the default strategy.
     * @param entity the entity (or type prototype) to inspect.
     * @return the ordered list of searchable attribute references; never null.
     */
    public static List<SearchableAttributeRef> collectSearchable(IApsEntity entity) {
        return collectSearchable(entity, DefaultEntitySearchKeyStrategy.INSTANCE);
    }

    /**
     * The attributes a search form should offer as filter criteria: every searchable top-level
     * attribute (unchanged legacy behaviour, any type) plus every nested-searchable attribute inside a
     * <b>Composite</b> whose inherited {@code searchable} flag is set. Nested attributes are returned
     * under their path key, which is exactly the key the DB search records were written under; the real
     * attribute travels along in {@link SearchableAttributeRef#source()} so callers can dispatch on its
     * actual type.
     * @param entity the entity (or type prototype) to inspect.
     * @param searchKeyStrategy the key strategy of the entity manager that owns this entity.
     * @return the ordered list of searchable attribute references; never null.
     */
    public static List<SearchableAttributeRef> collectSearchable(IApsEntity entity,
            IEntitySearchKeyStrategy searchKeyStrategy) {
        List<SearchableAttributeRef> result = new ArrayList<>();
        if (null == entity) {
            return result;
        }
        IEntitySearchKeyStrategy strategy = orDefault(searchKeyStrategy);
        walk(entity, visit -> {
            if (isOffered(visit)) {
                result.add(new SearchableAttributeRef(strategy.buildKey(visit.segments()),
                        strategy.buildLabel(visit.segments()), visit.attribute()));
            }
        });
        return result;
    }

    /**
     * The human-readable label of every attribute {@link #collectSearchable} offers, keyed by the same
     * machine key, under the default strategy.
     * @param entity the entity (or type prototype) to inspect.
     * @return a map from machine key to display label; never null.
     */
    public static Map<String, String> buildSearchLabels(IApsEntity entity) {
        return buildSearchLabels(entity, DefaultEntitySearchKeyStrategy.INSTANCE);
    }

    /**
     * The human-readable label of every attribute {@link #collectSearchable} offers, keyed by the same
     * machine key. Callers (the search-form JSPs) render this verbatim instead of splitting the
     * flattened key, which would mis-segment a name containing '_'. Insertion order matches
     * {@link #collectSearchable}.
     * @param entity the entity (or type prototype) to inspect.
     * @param strategy the key strategy of the entity manager that owns this entity.
     * @return a map from machine key to display label; never null.
     */
    public static Map<String, String> buildSearchLabels(IApsEntity entity,
            IEntitySearchKeyStrategy strategy) {
        Map<String, String> labels = new LinkedHashMap<>();
        // strategy coalescing happens in collectSearchable, which this delegates to
        for (SearchableAttributeRef ref : collectSearchable(entity, strategy)) {
            labels.put(ref.key(), ref.label());
        }
        return labels;
    }

    /**
     * The complete plan of search-table records to write for this entity, under the default strategy.
     * @param entity the entity to index.
     * @return the records to write, in attribute-tree order; never null.
     */
    public static List<SearchRecordSpec> planSearchRecords(IApsEntity entity) {
        return planSearchRecords(entity, DefaultEntitySearchKeyStrategy.INSTANCE);
    }

    /**
     * The complete plan of search-table records to write for this entity: every attribute that has to be
     * indexed, each paired with the {@code attrname} to index it under. This is the whole write rule in
     * one place, so {@code AbstractEntityDAO} neither traverses the attribute tree nor knows the naming
     * convention - it just writes what it is handed.
     *
     * <p>The rule, unchanged in behaviour from the hand-rolled descent it replaces:</p>
     * <ul>
     *   <li>an elementary <b>top-level</b> attribute, when searchable, is indexed under its own name -
     *   legacy behaviour, any type;</li>
     *   <li>an elementary attribute reached through any complex attribute is likewise indexed under its
     *   own name - legacy "flattened" behaviour, lists included;</li>
     *   <li><b>except</b> a nested-searchable child of a Composite, which is indexed under its path key
     *   when that path is addressable (Composite ancestry only, attribute active and searchable), and
     *   <b>skipped entirely</b> when it is not: it cannot be path-qualified (a list above it occurs many
     *   times per entity) and writing it under its unqualified name would collide with a same-named
     *   top-level attribute, producing false positives on that attribute's filters. Nothing could read
     *   such a record either - Solr excludes lists and the content-type editor reports the flag as not
     *   available - so it would be unreachable data.</li>
     * </ul>
     * <p>Complex attributes never produce a record of their own; they are descended.</p>
     *
     * @param entity the entity to index.
     * @param searchKeyStrategy the key strategy of the entity manager that owns this entity.
     * @return the records to write, in attribute-tree order; never null.
     */
    public static List<SearchRecordSpec> planSearchRecords(IApsEntity entity,
            IEntitySearchKeyStrategy searchKeyStrategy) {
        List<SearchRecordSpec> specs = new ArrayList<>();
        if (null == entity) {
            return specs;
        }
        IEntitySearchKeyStrategy strategy = orDefault(searchKeyStrategy);
        walk(entity, visit -> {
            AttributeInterface attribute = visit.attribute();
            if (!attribute.isSimple() || !attribute.isSearchable()) {
                return;
            }
            if (!visit.topLevel() && isOffered(visit)) {
                specs.add(new SearchRecordSpec(strategy.buildKey(visit.segments()), attribute));
            } else if (visit.topLevel() || !visit.parentIsComposite()
                    || !isIndexableNestedBoolean(attribute)) {
                specs.add(new SearchRecordSpec(attribute.getName(), attribute));
            }
            // else: a nested-searchable child of a Composite with no addressable path - unindexable.
        });
        return specs;
    }

    /**
     * Visit every path-indexable nested attribute under the given top-level attribute, under the default
     * strategy.
     * @param topLevelAttribute the top-level attribute to descend; may be null.
     * @param visitor receives each eligible attribute and its path key.
     */
    public static void forEachIndexableNestedBoolean(AttributeInterface topLevelAttribute,
            BiConsumer<AttributeInterface, String> visitor) {
        forEachIndexableNestedBoolean(topLevelAttribute, visitor, DefaultEntitySearchKeyStrategy.INSTANCE);
    }

    /**
     * Visit every nested-searchable attribute under the given <b>top-level</b> attribute that is
     * eligible for path-based indexing, passing the attribute and its full path key (the top-level
     * attribute's own name included). Nothing is visited for a simple attribute, or for a
     * {@code List}/{@code Monolist}, or for a Composite with no eligible descendant.
     * @param topLevelAttribute the top-level attribute to descend; may be null.
     * @param visitor receives each eligible attribute and its path key.
     * @param searchKeyStrategy the key strategy of the entity manager that owns this entity.
     */
    public static void forEachIndexableNestedBoolean(AttributeInterface topLevelAttribute,
            BiConsumer<AttributeInterface, String> visitor, IEntitySearchKeyStrategy searchKeyStrategy) {
        if (!(topLevelAttribute instanceof CompositeAttribute)) {
            return;
        }
        IEntitySearchKeyStrategy strategy = orDefault(searchKeyStrategy);
        walk(((AbstractComplexAttribute) topLevelAttribute).getAttributes(),
                Collections.singletonList(topLevelAttribute.getName()), true, true,
                visit -> {
                    if (isOffered(visit)) {
                        visitor.accept(visit.attribute(), strategy.buildKey(visit.segments()));
                    }
                });
    }

    /**
     * Validate the search keys an entity type would write, under the default strategy.
     * @param entity the entity type to validate.
     * @return the problems found, in a stable order; empty when the type is sound. Never null.
     */
    public static List<KeyProblem> validateNestedBooleanKeys(IApsEntity entity) {
        return validateNestedBooleanKeys(entity, DefaultEntitySearchKeyStrategy.INSTANCE);
    }

    /**
     * Validate the search keys an entity type would write, so a defect is reported when the type is
     * saved rather than when a content is indexed. Two checks, both over the key set
     * {@link #collectSearchable} offers:
     * <ul>
     *   <li><b>duplicates</b> - the same key produced by more than one attribute path. With escaping and
     *   the boundary rule together, two <i>nested</i> paths cannot collide, so what remains is a
     *   <b>top-level</b> attribute whose raw name equals a nested key (top-level keys are stored unescaped, for
     *   backward compatibility): a top-level attribute named {@code a__b_c} occupies the key of
     *   composite {@code a_b} + child {@code c}. A duplicate means silent false positives on the DB
     *   search path and a rejected document (or an endless schema refresh loop) on the Solr one;</li>
     *   <li><b>ambiguous segment</b> - a name that cannot survive the encoding; for the default strategy,
     *   one that begins or ends with {@code '_'}. Reported on the path itself rather than waiting for the
     *   collision it would eventually cause with some other attribute;</li>
     *   <li><b>length</b> - a key longer than the {@code attrname} column that has to store it, whose
     *   width the <b>strategy</b> declares: this is why the bound is a per-manager property and not an
     *   engine-wide constant.</li>
     * </ul>
     * <p>Only keys that involve the nested feature are reported: a duplicate is reported only when at
     * least one of the colliding paths is nested, and the length is checked on nested keys only. An
     * entity type that does not use the feature can therefore never be rejected by this validation.</p>
     * @param entity the entity type to validate.
     * @param searchKeyStrategy the key strategy of the entity manager that will store this type.
     * @return the problems found, in a stable order; empty when the type is sound. Never null.
     */
    public static List<KeyProblem> validateNestedBooleanKeys(IApsEntity entity,
            IEntitySearchKeyStrategy searchKeyStrategy) {
        List<KeyProblem> problems = new ArrayList<>();
        if (null == entity) {
            return problems;
        }
        IEntitySearchKeyStrategy strategy = orDefault(searchKeyStrategy);
        Map<String, List<String>> pathsByKey = new LinkedHashMap<>();
        Set<String> nestedKeys = new LinkedHashSet<>();
        walk(entity, visit -> {
            if (!isOffered(visit)) {
                return;
            }
            String key = strategy.buildKey(visit.segments());
            String label = strategy.buildLabel(visit.segments());
            pathsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(label);
            if (!visit.topLevel()) {
                nestedKeys.add(key);
                // A top-level name is never checked: its key is the raw name, so no encoding applies to
                // it and any name is representable. Only a segment of a nested path has to survive the
                // encoding.
                if (!visit.segments().stream().allMatch(strategy::isEncodableSegment)) {
                    problems.add(new KeyProblem(KeyProblemType.AMBIGUOUS_SEGMENT, key, List.of(label),
                            strategy.getMaxKeyLength()));
                }
            }
        });
        for (Map.Entry<String, List<String>> entry : pathsByKey.entrySet()) {
            if (entry.getValue().size() > 1 && nestedKeys.contains(entry.getKey())) {
                problems.add(new KeyProblem(KeyProblemType.DUPLICATED, entry.getKey(), entry.getValue()));
            }
        }
        for (String key : nestedKeys) {
            if (key.length() > strategy.getMaxKeyLength()) {
                problems.add(new KeyProblem(KeyProblemType.TOO_LONG, key, pathsByKey.get(key),
                        strategy.getMaxKeyLength()));
            }
        }
        return problems;
    }

    /**
     * Clear the {@code searchable} flag wherever the attribute tree cannot honour it, and report what
     * was cleared. This is the engine's single enforcement point for the rule that a Composite child is
     * searchable only when it says so: it runs when a type is explicitly configured
     * ({@code ApsEntityManager.addEntityPrototype}/{@code updateEntityPrototype}), so every write path -
     * REST, the legacy API, the admin console, and any path added later - is gated once, at the boundary,
     * rather than in each builder.
     *
     * <p>Two cases are cleared, and both are children of a <b>Composite</b>:</p>
     * <ul>
     *   <li>a child whose type does not declare {@link AttributeInterface#isNestedSearchSupported()}: it
     *   has no path key, so it would be written to the search tables under its unqualified name and
     *   collide with a same-named attribute elsewhere in the type;</li>
     *   <li>a child whose path is not addressable because a {@code List}/{@code Monolist} sits above the
     *   Composite: the list occurs many times per entity, so no path identifies a single value. This is
     *   the rule the composite editor spells out as {@code clearSearchableWithinList}, enforced here for
     *   every path instead of just that one.</li>
     * </ul>
     *
     * <p><b>Children of a {@code List}/{@code Monolist} are deliberately left alone.</b> They are still
     * indexed under their own names - the long-standing flattened behaviour {@link #planSearchRecords}
     * preserves - and clearing them would break every existing list filter. That is what the
     * {@code parentIsComposite} condition below guards, and it should be treated as a breaking change if
     * it is ever edited.</p>
     *
     * <p>Top-level attributes are never touched: a searchable top-level attribute of any type is indexed
     * under its own name, which is what the search tables have always stored.</p>
     *
     * <p><b>Reach.</b> {@link #walk} descends {@code AbstractComplexAttribute.getAttributes()}, which on a
     * <i>type prototype</i> is empty for a {@code List}/{@code Monolist} - a list's structure lives in
     * {@code getNestedAttributeType()} there, and is only materialized as elements on a real entity. So
     * when this runs at the persistence boundary, where the subject is a prototype, nothing below a list
     * is visited and {@link ClearedFlagReason#PATH_NOT_ADDRESSABLE} does not arise; it does when the
     * subject is an entity. That is deliberate rather than an oversight: adding a second descent into the
     * nested type would give this class two traversals with different reach, and the flag it would clear
     * is inert anyway - {@link #isOffered} refuses it, {@link #planSearchRecords} skips it, and the
     * composite editor never offers the checkbox inside a list. The rule is still enforced where a user
     * can see it, by {@code CompositeAttributeConfigAction.clearSearchableWithinList}.</p>
     *
     * <p>Deliberately takes no {@link IEntitySearchKeyStrategy}: <b>whether</b> an attribute deserves a
     * key is the attribute's own answer, and a strategy owns only <b>how</b> a path becomes one. Letting a
     * strategy influence this would reintroduce the second authority that was removed when
     * {@code isNestable} was deleted. Callers that want to render the returned paths join the segments
     * with their own strategy.</p>
     *
     * @param entity the entity type to normalize, modified in place; may be null.
     * @return every attribute whose flag was cleared, in traversal order; empty when the type was already
     * sound. Never null.
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
            if (!isIndexableNestedBoolean(attribute)) {
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
     * Whether a search form offers this attribute, and equivalently whether it is indexed under its path
     * key: any <b>active, searchable</b> attribute, of any type at top level (legacy behaviour) and
     * nested-searchable only below a Composite - which also requires the path to be addressable, i.e.
     * no {@code List}/{@code Monolist} anywhere above it.
     *
     * <p>{@code isActive()} used to be checked at top level but not below. Aligning the two is a
     * <b>no-op</b> rather than a behaviour change, because a nested attribute cannot be inactive:
     * {@code _active} defaults to true and only {@code AbstractAttribute.disable(code)} clears it,
     * {@code disable}/{@code activate} are never overridden to recurse into a Composite, and the only
     * caller - {@code ApsEntity.disableAttributes} - iterates the <b>top-level</b> attribute list. So the
     * added condition is always satisfied for a nested attribute today, and is the behaviour we would want
     * if disabling ever learned to reach children.</p>
     */
    private static boolean isOffered(Visit visit) {
        AttributeInterface attribute = visit.attribute();
        return attribute.isActive() && attribute.isSearchable()
                && (visit.topLevel()
                || (visit.pathIndexable() && isIndexableNestedBoolean(attribute)));
    }

    /** Walk an entity's whole attribute tree from the top level. */
    private static void walk(IApsEntity entity, StructureVisitor visitor) {
        walk(entity.getAttributeList(), null, false, true, visitor);
    }

    /**
     * Reports the <b>structure</b> of the attribute tree: every top-level attribute, and every
     * elementary attribute reachable through any complex attribute, each with the path segments that
     * lead to it and with the two facts a naming rule needs - whether its direct parent is a Composite,
     * and whether its path is <b>addressable</b> ({@code pathIndexable}: every ancestor is a Composite).
     * Policy - which of those a caller wants, and whether the {@code searchable} flag matters - is
     * applied by the entry points above, never in here.
     *
     * <p>{@code List}/{@code Monolist} attributes are visited at top level (they are ordinary attributes
     * there) and are descended, because their elementary children are still indexed under their own
     * names (legacy behaviour) - but everything below a list has {@code pathIndexable} false, since a
     * list occurs many times per entity and so no path through it identifies a single value. Nested
     * complex attributes are descended but not themselves reported. This single rule is what every write
     * and read path now shares.</p>
     *
     * @param attributes the attributes to walk.
     * @param parentSegments the path segments of the enclosing complex attribute, null at top level.
     * @param parentIsComposite whether the enclosing complex attribute is a Composite.
     * @param pathIndexable whether the enclosing path is addressable (no list crossed so far).
     * @param visitor receives each reported attribute with its context.
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
