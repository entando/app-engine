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
package com.agiletec.plugins.jacms.aps.system.services.content.model.attribute;

import com.agiletec.aps.system.common.entity.model.attribute.JAXBTextAttribute;
import java.io.Serializable;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

/**
 * @author E.Santoboni
 */
@XmlType(propOrder = {"resource"})
@JsonRootName("resource")
@XmlSeeAlso({JAXBResourceValue.class, HashMap.class})
public class JAXBResourceAttribute extends JAXBTextAttribute implements Serializable {

    @XmlElement(name = "resource", required = false)
    @JsonProperty("resource")
    public JAXBResourceValue getResource() {
        return resource;
    }

    public void setResource(JAXBResourceValue resource) {
        this.resource = resource;
    }

    private JAXBResourceValue resource;

}
