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

import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;

/**
 * The kind of per-attribute search field an attribute contributes, independent of any back-end.
 * Attributes answer for themselves through {@link AttributeInterface#getSearchFieldType()}; each
 * back-end maps these to its own field types.
 */
public enum SearchFieldType {

    /** Free text. The default for any {@link IndexableAttributeInterface} attribute. */
    TEXT,

    /** A date/time instant, range-queryable. */
    DATE,

    /** A number, range-queryable. */
    NUMBER,

    /** {@code true} or {@code false}. */
    BOOLEAN,

    /**
     * {@code true}, {@code false} or unset. Distinct from {@link #BOOLEAN} because the unset state does
     * not fit a two-valued field, so back-ends store it as a string.
     */
    TRISTATE;

    /**
     * Whether this is the boolean family, whose members share one search form control and one storage
     * shape. The single place that membership is stated.
     */
    public boolean isBooleanFamily() {
        return BOOLEAN == this || TRISTATE == this;
    }

    /**
     * Whether the {@code searchable} flag is a precondition for the field to exist. True for the boolean
     * family only: text, date and number attributes already produce indexable content either way.
     */
    public boolean isSearchableFlagRequired() {
        return this.isBooleanFamily();
    }

}
