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
import java.util.List;

/**
 * How an entity manager addresses a nested attribute in its search table: how a path becomes the
 * {@code attrname} that is stored, and how long that key is allowed to be.
 *
 * <p>Note what is <b>not</b> here: whether an attribute may be path-indexed at all. That is the
 * attribute's own property, answered by {@code AttributeInterface.isNestedSearchSupported()}, because the
 * places that must agree on it include model code with no entity manager to ask - the Composite that
 * clears the {@code searchable} flag of a child that cannot carry one. A strategy that could widen or
 * narrow eligibility would contradict that gate rather than extend it. A strategy owns keys; an attribute
 * owns whether it deserves one.</p>
 *
 * <p>This exists because the bound it carries is <b>per entity manager</b>, not per platform: every
 * manager owns its own search table, so the width of the {@code attrname} column is a property of that
 * table. Expressing it as a constant made the invariant undocumentable in code - the engine validated
 * one number while each table was free to be narrower - and a key that passed validation could still
 * truncate (MySQL, non strict mode) or fail the transaction (PostgreSQL) on insert. A manager whose
 * column is narrower now declares it, and the same validation that protects the shipped tables protects
 * that one.</p>
 *
 * <p>Obtain it from {@link IEntityManager#getSearchKeyStrategy()}; the shipped implementation is
 * {@link DefaultEntitySearchKeyStrategy}.</p>
 *
 * <p><b>Scope of an override.</b> The length bound is genuinely per-manager - it is the width of that
 * manager's column. The <i>encoding</i> ({@link #buildKey}) is a different matter: it is used symmetrically by the writer
 * ({@link AbstractEntityDAO}) and by the readers, and one of those readers -
 * {@code EntitySearchFilter.getInstance}, which resolves a remembered search - is a static on a model
 * class that has no entity manager to ask and therefore always uses the default. A project that
 * overrides the encoding must therefore inject the same strategy into both the manager and its DAO
 * <b>and</b> accept that the model-level resolver assumes the default; overriding only the length or
 * only the predicate has no such caveat.</p>
 *
 * @author Entando
 */
public interface IEntitySearchKeyStrategy extends Serializable {

    /**
     * The maximum length of a search key this manager can store, i.e. the width of the {@code attrname}
     * column of its search table. A type whose keys exceed it is rejected when it is persisted.
     *
     * @return the maximum key length, in characters.
     */
    int getMaxKeyLength();

    /**
     * The machine key of an attribute path: what goes in the {@code attrname} column, and what a Solr
     * field is named after. Must be injective - two different segment lists must never produce the same
     * key - or two attributes silently share one index entry.
     *
     * @param segments the attribute names along the path, from the top-level attribute down.
     * @return the key.
     */
    String buildKey(List<String> segments);

    /**
     * Whether a path segment can be encoded unambiguously by {@link #buildKey}, i.e. whether a path
     * containing it is safe to use as a key.
     *
     * <p>This exists because {@link #buildKey} must be <b>injective</b>, and an encoding can usually
     * guarantee that only over a restricted alphabet. The default answer is permissive - a strategy
     * whose encoding has no such restriction (one that delimits with a character illegal in a name, say)
     * simply does not override it. {@link DefaultEntitySearchKeyStrategy} does override it, because its
     * escaping cannot separate an underscore that sits against a segment boundary.</p>
     *
     * <p>Checked at content-type save time for every segment of a nested searchable path, so an
     * un-encodable name is refused before it can produce a key. A <b>top-level</b> attribute's name is
     * never checked: its key is the raw name, so no encoding applies to it.</p>
     *
     * @param segment one attribute name along a nested path.
     * @return true if a key built from a path containing this segment is unambiguous.
     */
    default boolean isEncodableSegment(String segment) {
        return true;
    }

    /**
     * The human-readable rendering of an attribute path, shown in a search form. Unlike
     * {@link #buildKey} it need not be reversible or collision-free; it must simply keep the segment
     * boundaries visible.
     *
     * @param segments the attribute names along the path, from the top-level attribute down.
     * @return the label.
     */
    String buildLabel(List<String> segments);

}
