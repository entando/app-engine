/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jacms.aps.system.services.api.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "cms")
@JsonRootName(value = "cms")
@XmlType(propOrder = {"id", "status"})
public class JAXBCmsResult {
    
    @XmlElement(name = "id", required = true)
    @JsonProperty("id")
    public String getId() {
        return _id;
    }
    public void setId(String id) {
        this._id = id;
    }
    
    @XmlElement(name = "status", required = true)
    @JsonProperty("status")
    public String getStatus() {
        return _status;
    }
    public void setStatus(String status) {
        this._status = status;
    }
     
    private String _id;
    private String _status;
    
}
