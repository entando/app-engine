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
package com.agiletec.aps.system.common.entity.model.attribute;

import org.entando.entando.aps.system.common.entity.search.SearchFieldType;

/**
 * This attribute represent an information of type Three State. This attribute
 * does not support multiple languages.
 *
 * @author E.Santoboni
 */
public class ThreeStateAttribute extends BooleanAttribute {

    /**
     * How the <i>unset</i> state is rendered in a search field. A three-state attribute has no
     * two-valued representation for "not set", so it is indexed as this literal instead - and searched
     * for by it. Defined here, on the attribute that owns the third state, so the search back-ends and
     * the admin search form cannot disagree on the spelling.
     */
    public static final String NOT_SET_SEARCH_VALUE = "none";

    @Override
    public Boolean getValue() {
        return super.getBooleanValue();
    }

    /**
     * A three-valued boolean cannot be stored in a two-valued boolean field, so it declares its own
     * field type. This override is what makes the distinction polymorphic: consumers switch on the
     * value, instead of having to test this subclass before {@link BooleanAttribute}.
     */
    @Override
    public SearchFieldType getSearchFieldType() {
        return SearchFieldType.TRISTATE;
    }

    /**
     * The literal {@code "true"}/{@code "false"}, or {@link #NOT_SET_SEARCH_VALUE} when unset. Never
     * null - the unset state is a value to index, not the absence of one.
     */
    @Override
    public Object getSearchFieldValue() {
        Boolean value = this.getValue();
        return (null == value) ? NOT_SET_SEARCH_VALUE : value.toString();
    }

    @Override
    protected boolean saveBooleanJDOMElement() {
        return (null != super.getBooleanValue());
    }

    @Override
    protected boolean addSearchInfo() {
        return (null != super.getBooleanValue());
    }

    @Override
    public AbstractJAXBAttribute getJAXBAttribute(String langCode) {
        JAXBBooleanAttribute jaxbAttribute = (JAXBBooleanAttribute) super.createBaseJAXBAttribute();
        jaxbAttribute.setBoolean(this.getValue());
        return jaxbAttribute;
    }

}
