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

import org.entando.entando.aps.system.common.entity.search.NestedSearchSupport.KeyProblem;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What one entity type offers to a search: the filterable attributes, their labels and keys, and any
 * defects in that key set. A pure function of the type, computed once and cached by
 * {@link ApsEntityManager} per type code, dropped in its {@code refresh()}. Obtain one from
 * {@link IEntityManager#getSearchSchema(String)}.
 *
 * <p><b>Do not mutate the attributes</b> reachable through {@link SearchableAttributeRef#source()}: the
 * schema is cached, so they are shared by every caller for this type. Code needing a mutable attribute -
 * an indexer setting a rendering language - must work from its own prototype.</p>
 */
public class EntitySearchSchema implements Serializable {

    private final String typeCode;
    private final List<SearchableAttributeRef> searchableAttributes;
    private final List<KeyProblem> problems;

    /**
     * @param entityType the type to describe; null gives an empty schema.
     * @param maxKeyLength the {@code attrname} width of the owning entity manager's search table.
     */
    public static EntitySearchSchema build(IApsEntity entityType, int maxKeyLength) {
        return new EntitySearchSchema(null == entityType ? null : entityType.getTypeCode(),
                NestedSearchSupport.collectSearchable(entityType),
                NestedSearchSupport.validateNestedSearchKeys(entityType, maxKeyLength));
    }

    private EntitySearchSchema(String typeCode, List<SearchableAttributeRef> searchableAttributes,
            List<KeyProblem> problems) {
        this.typeCode = typeCode;
        this.searchableAttributes = List.copyOf(searchableAttributes);
        this.problems = List.copyOf(problems);
    }

    /** The code of the type this schema describes; null for an empty schema. */
    public String getTypeCode() {
        return this.typeCode;
    }

    /**
     * The attributes a search form offers, in tree order: searchable top-level ones plus path-indexed
     * nested ones.
     */
    public List<SearchableAttributeRef> getSearchableAttributes() {
        return this.searchableAttributes;
    }

    /**
     * Display labels keyed by machine key, derived from {@link #getSearchableAttributes()} on each call.
     *
     * @deprecated the label belongs to the attribute that carries it: read
     * {@link SearchableAttributeRef#getLabel()} while iterating the refs a form already iterates. Kept
     * because a downstream JSP may still bind {@code searchableAttributeLabels}, and this build compiles
     * no JSP, so removing it would break such a page silently.
     */
    @Deprecated
    public Map<String, String> getLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        for (SearchableAttributeRef ref : this.searchableAttributes) {
            labels.put(ref.key(), ref.label());
        }
        return Collections.unmodifiableMap(labels);
    }

    /**
     * The attribute currently offered under this key, or null. Not a replacement for
     * {@code NestedSearchSupport.resolveNestedByKey}, which resolves regardless of the {@code searchable}
     * flag so a remembered filter still finds its attribute's type.
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

    /** Key-set defects: duplicated, over-long or un-encodable. Empty for a sound type. */
    public List<KeyProblem> getProblems() {
        return this.problems;
    }

    /** Whether this type's keys can all be stored and resolved unambiguously. */
    public boolean isValid() {
        return this.problems.isEmpty();
    }

}
