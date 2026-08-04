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
package com.agiletec.aps.system.common.searchengine;

import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;

/**
 * The kind of per-attribute search field an attribute contributes to a search engine - what the index
 * has to be able to store for it, expressed independently of any back-end.
 *
 * <p>This is the platform's answer to a question every search back-end has to ask and that used to be
 * answered by an {@code instanceof} ladder repeated in each of them ("is it a Date? a Number? a
 * Boolean? then a text one"): each ladder had to enumerate the closed set of attribute types, and each
 * had to remember that {@code ThreeStateAttribute} <b>extends</b> {@code BooleanAttribute} and must
 * therefore be tested first. Attributes now answer for themselves through
 * {@link AttributeInterface#getSearchFieldType()}, so the dispatch is polymorphic: a new attribute type
 * - including one from a custom project - declares its own field type and every back-end honours it
 * without being edited.</p>
 *
 * <p>A back-end maps these to its own field types (the Solr plugin maps them to {@code pdates},
 * {@code plongs}, {@code boolean}, {@code string} and {@code text_gen_sort}). The mapping lives in the
 * back-end; the classification lives here.</p>
 *
 * @author Entando
 */
public enum SearchFieldType {

    /**
     * Free text: the attribute's indexable content is what gets stored. Also what an attribute that
     * merely implements {@link IndexableAttributeInterface} reports by default.
     */
    TEXT,

    /** A date/time instant, stored so it can be range-queried. */
    DATE,

    /** A number, stored so it can be range-queried. */
    NUMBER,

    /** A two-valued boolean: {@code true} or {@code false}. */
    BOOLEAN,

    /**
     * A three-valued boolean - {@code true}, {@code false} or <i>unset</i>. Deliberately distinct from
     * {@link #BOOLEAN}: the unset state does not fit a two-valued boolean field, so a back-end has to
     * store this as a string (the engine renders the unset state as
     * {@link com.agiletec.aps.system.common.entity.model.attribute.ThreeStateAttribute#NOT_SET_SEARCH_VALUE}).
     * Keeping it a separate <i>value</i> rather than a subtype is what removes the
     * "test the subclass first" hazard from every consumer.
     */
    TRISTATE;

    /**
     * Whether an attribute of this type contributes a search field <b>only</b> when it is flagged
     * searchable. Types whose attributes already produce indexable content ({@link #TEXT},
     * {@link #DATE}, {@link #NUMBER} - all implemented by
     * {@link IndexableAttributeInterface} attributes) get their field either way, which is the
     * behaviour the platform has always had; the boolean family does not.
     *
     * @return true when the {@code searchable} flag is a precondition for the field to exist.
     */
    public boolean isSearchableFlagRequired() {
        return BOOLEAN == this || TRISTATE == this;
    }

}
