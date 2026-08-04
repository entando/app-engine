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

import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import java.io.Serializable;

/**
 * A searchable attribute as a search form addresses it: its machine {@code key} (the DB
 * {@code attrname} / Solr field / form field key), its display {@code label} (the hierarchy joined
 * by {@link NestedBooleanSearchSupport#LABEL_SEPARATOR}, built from the real tree boundaries so it
 * stays correct when a name itself contains '_' - which the key escapes rather than renders literally)
 * and the real attribute behind them.
 *
 * <p>A nested boolean's key differs from {@code source.getName()} - that is the whole point of the
 * path encoding - so consumers must read the key from here and must not rename the attribute.</p>
 *
 * <p>The JavaBean-style accessors below exist for OGNL: the finder JSPs address
 * {@code #attribute.name}, {@code #attribute.type} and {@code #attribute.textAttribute}. Anything
 * else a form needs from the attribute is reached through {@code #attribute.source.<property>} -
 * deliberately explicit, because a ref is not an attribute and should not pretend to be one.</p>
 *
 * <p>This is a top-level type rather than a member of {@link NestedBooleanSearchSupport} on purpose:
 * it appears in the return type of {@code AbstractApsEntityFinderAction.getSearchableAttributeRefs()},
 * so nesting it would bake the support class's name into a public API signature and make renaming
 * that class a breaking change for downstream code.</p>
 *
 * @param key the machine key the form field and the search filter carry.
 * @param label the human-readable hierarchy.
 * @param source the real attribute; never a copy, so its type, handler and validation rules are
 * the genuine ones.
 * @author Entando
 */
public record SearchableAttributeRef(String key, String label, AttributeInterface source)
        implements Serializable {

    /** The machine key - what the search form field is named after. */
    public String getName() {
        return this.key;
    }

    /** The real attribute's type code, which drives the search widget dispatch. */
    public String getType() {
        return this.source.getType();
    }

    /** Whether the real attribute is a text attribute (drives the search widget dispatch). */
    public boolean isTextAttribute() {
        return this.source.isTextAttribute();
    }

    /** The real attribute, for the type-specific properties a search widget may need. */
    public AttributeInterface getSource() {
        return this.source;
    }
}
