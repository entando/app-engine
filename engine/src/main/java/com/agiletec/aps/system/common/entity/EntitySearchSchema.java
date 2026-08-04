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

import com.agiletec.aps.system.common.entity.NestedBooleanSearchSupport.KeyProblem;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What one entity type offers to a search: the attributes a search form can filter on, the label of
 * each, the key each is addressed by, and the defects - if any - of that key set.
 *
 * <p><b>Why this exists.</b> All of that is a pure function of the <i>type</i>, yet it used to be
 * recomputed by walking the attribute tree on every search-form render, every filter resolution and
 * every type save. Computing it once per type turns four traversals per page into none, and - more
 * importantly - gives the key set a single point of creation: because a schema is built whenever a type
 * is loaded, a type whose keys are unusable is <b>reported when it is loaded</b>, not silently indexed.
 * Type loading is deliberately not allowed to fail (an existing deployment must always boot), so the
 * schema records the problems and {@link ApsEntityManager} logs them; explicit configuration paths still
 * reject them outright.</p>
 *
 * <p><b>Lifetime.</b> A schema holds references to the <i>prototype's</i> attributes, so it is only
 * valid for as long as that prototype is. {@link ApsEntityManager} caches one per type code and drops
 * the cache in {@code refresh()}, which is what every type change goes through.</p>
 *
 * <p><b>Read-only for consumers.</b> Because a schema is cached, the attributes reachable through
 * {@link SearchableAttributeRef#source()} are <b>shared</b> by every caller that asks for this type - they
 * are not the freshly parsed prototype each caller used to get from
 * {@code IEntityManager.getEntityPrototype}, which re-reads and re-parses the type configuration on every
 * call. Read metadata from them freely; do not mutate them ({@code setRenderingLang},
 * {@code setSearchable}, values, ...), or the mutation becomes visible to every later request for the same
 * type. Code that needs a mutable attribute - an indexer setting a rendering language, for instance - must
 * work from its own prototype, which is what the search back-ends do.</p>
 *
 * <p>Obtain one from {@link IEntityManager#getSearchSchema(String)}.</p>
 *
 * @author Entando
 */
public class EntitySearchSchema implements Serializable {

    private final String typeCode;
    private final List<SearchableAttributeRef> searchableAttributes;
    private final Map<String, String> labels;
    private final List<KeyProblem> problems;

    /**
     * Compute the schema of a type. Nothing here traverses the tree itself: the traversal that owns the
     * rules is {@link NestedBooleanSearchSupport}, and this is the memo of its answers.
     *
     * @param entityType the type (or prototype) to describe; may be null, giving an empty schema.
     * @param strategy the key strategy of the entity manager that owns the type.
     * @return the schema; never null.
     */
    public static EntitySearchSchema build(IApsEntity entityType, IEntitySearchKeyStrategy strategy) {
        List<SearchableAttributeRef> refs = NestedBooleanSearchSupport.collectSearchable(entityType, strategy);
        Map<String, String> labels = new LinkedHashMap<>();
        for (SearchableAttributeRef ref : refs) {
            labels.put(ref.key(), ref.label());
        }
        return new EntitySearchSchema(null == entityType ? null : entityType.getTypeCode(), refs, labels,
                NestedBooleanSearchSupport.validateNestedBooleanKeys(entityType, strategy));
    }

    private EntitySearchSchema(String typeCode, List<SearchableAttributeRef> searchableAttributes,
            Map<String, String> labels, List<KeyProblem> problems) {
        this.typeCode = typeCode;
        this.searchableAttributes = List.copyOf(searchableAttributes);
        this.labels = Collections.unmodifiableMap(labels);
        this.problems = List.copyOf(problems);
    }

    /** The code of the type this schema describes; null for an empty schema. */
    public String getTypeCode() {
        return this.typeCode;
    }

    /**
     * The attributes a search form offers, in attribute-tree order: every searchable top-level attribute
     * plus every path-indexed nested one, each with the key the form field and the filter carry.
     */
    public List<SearchableAttributeRef> getSearchableAttributes() {
        return this.searchableAttributes;
    }

    /**
     * The display label of every offered attribute, keyed by its machine key - a nested attribute's
     * hierarchy, built from the real tree boundaries so a form never has to split a flattened key.
     */
    public Map<String, String> getLabels() {
        return this.labels;
    }

    /**
     * The attribute a key addresses, or null if this type offers no such key.
     *
     * <p>Deliberately <b>not</b> a replacement for
     * {@code NestedBooleanSearchSupport.resolveNestedBooleanByKey}: that one resolves a key regardless of
     * the {@code searchable} flag, so a filter remembered before the flag was cleared still finds its
     * attribute's type. A schema contains only what the type currently offers, so resolving through it
     * would silently change that behaviour. Use this when you want "what the form offers now"; use the
     * traversal when you want "what this key ever meant".</p>
     *
     * @param key the machine key.
     * @return the attribute currently offered under that key, or null.
     */
    public AttributeInterface getOfferedAttribute(String key) {
        if (null == key) {
            return null;
        }
        return this.searchableAttributes.stream()
                .filter(ref -> key.equals(ref.key()))
                .map(SearchableAttributeRef::source)
                .findFirst().orElse(null);
    }

    /**
     * The defects of this type's key set: a key produced by more than one attribute path, or one longer
     * than the {@code attrname} column that has to store it. Empty for a sound type - and for any type
     * that does not use nested search at all.
     */
    public List<KeyProblem> getProblems() {
        return this.problems;
    }

    /** Whether this type's keys can all be stored and resolved unambiguously. */
    public boolean isValid() {
        return this.problems.isEmpty();
    }

}
