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
import java.io.Serializable;

/**
 * A searchable attribute as a search form addresses it. A nested attribute's {@code key} differs from
 * {@code source.getName()}, so consumers must use the key.
 *
 * <p>The getters exist for OGNL: a finder JSP picks the form control from
 * {@code #attribute.textAttribute}/{@code .date}/{@code .number}/{@code .booleanLike}/{@code .tristate}
 * rather than from a list of type codes, so an attribute type the platform does not ship renders a
 * control as soon as it declares its {@link SearchFieldType}. Anything else goes through
 * {@code #attribute.source.<property>}.</p>
 *
 * @param key the machine key (DB {@code attrname} / Solr field / form field).
 * @param label the human-readable hierarchy.
 * @param source the real attribute; not a copy.
 */
public record SearchableAttributeRef(String key, String label, AttributeInterface source)
        implements Serializable {

    /** The machine key, not the attribute's own name. */
    public String getName() {
        return this.key;
    }

    /**
     * The human-readable hierarchy, falling back to the key so a form never renders an empty label.
     * Exists as a getter because OGNL cannot see a record's {@code label()} accessor - which is why the
     * search form used to look the label up in a parallel map keyed by {@link #getName()}.
     */
    public String getLabel() {
        return (null != this.label) ? this.label : this.key;
    }

    public String getType() {
        return this.source.getType();
    }

    public boolean isTextAttribute() {
        return this.source.isTextAttribute();
    }

    /**
     * The kind of search field this attribute contributes, or <b>null</b> when it contributes none - a
     * searchable top-level {@code Composite} or {@code Monolist} is offered as a criterion by
     * {@code NestedSearchSupport.collectSearchable} and answers null here. Every predicate below is
     * null-safe for that reason.
     */
    public SearchFieldType getSearchFieldType() {
        return this.source.getSearchFieldType();
    }

    /** Whether a from/to date pair is the right control. */
    public boolean isDate() {
        return SearchFieldType.DATE == this.getSearchFieldType();
    }

    /** Whether a from/to number pair is the right control. */
    public boolean isNumber() {
        return SearchFieldType.NUMBER == this.getSearchFieldType();
    }

    /** Whether the control is the boolean family's single radio group - {@code ThreeState} included. */
    public boolean isBooleanLike() {
        SearchFieldType type = this.getSearchFieldType();
        return null != type && type.isBooleanFamily();
    }

    /** Whether that radio group needs the fourth "not set" option. */
    public boolean isTristate() {
        return SearchFieldType.TRISTATE == this.getSearchFieldType();
    }

    public AttributeInterface getSource() {
        return this.source;
    }
}
