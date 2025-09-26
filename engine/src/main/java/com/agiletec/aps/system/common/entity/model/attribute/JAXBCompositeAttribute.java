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

import com.fasterxml.jackson.annotation.JsonAlias;
import java.io.Serializable;
import java.util.Map;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlType;

/**
 * @author E.Santoboni
 */
@XmlType(propOrder = {"attributes"})
public class JAXBCompositeAttribute extends AbstractJAXBAttribute implements Serializable {

    @XmlElementWrapper(name = "attributes")
    @XmlElement(name = "attribute")
    @JsonAlias({"attributes", "attribute"})
    public Map<String, AbstractJAXBAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, AbstractJAXBAttribute> attributes) {
        this.attributes = attributes;
    }

    private Map<String, AbstractJAXBAttribute> attributes;

}
