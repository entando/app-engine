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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

import org.entando.entando.aps.system.services.api.IApiErrorCodes;
import org.entando.entando.aps.system.services.api.model.ApiException;

/**
 * @author E.Santoboni
 */
@XmlRootElement(name = "attributeType")
@JsonRootName(value = "attributeType")
@XmlType(propOrder = {"elementTypes"})
@XmlSeeAlso({DefaultJAXBAttributeType.class, JAXBEnumeratorAttributeType.class})
public class JAXBCompositeAttributeType extends DefaultJAXBAttributeType {

    @Override
    public AttributeInterface createAttribute(Map<String, AttributeInterface> attributes) throws ApiException {
        CompositeAttribute compositeAttribute = (CompositeAttribute) super.createAttribute(attributes);
        List<DefaultJAXBAttributeType> jaxbElementTypes = this.getElementTypes();
        if (null == jaxbElementTypes) {
            return compositeAttribute;
        }
        for (DefaultJAXBAttributeType jaxbElementType : jaxbElementTypes) {
            if (null == attributes.get(jaxbElementType.getType())) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR,
                        "Attribute Element '" + jaxbElementType.getName() + "' - Type '" + jaxbElementType.getType()
                                + "' does not exist");
            }
            AttributeInterface attributeElement = jaxbElementType.createAttribute(attributes);
            compositeAttribute.addAttribute(attributeElement);
        }
        return compositeAttribute;
    }

    @XmlElement(name = "compositeElementType", required = true)
    @XmlElementWrapper(name = "compositeElementTypes")
    @JsonProperty("compositeElementTypes")
    public List<DefaultJAXBAttributeType> getElementTypes() {
        return elementTypes;
    }

    public void setElementTypes(List<DefaultJAXBAttributeType> elementTypes) {
        this.elementTypes = elementTypes;
    }

    private List<DefaultJAXBAttributeType> elementTypes = new ArrayList<>();

}
