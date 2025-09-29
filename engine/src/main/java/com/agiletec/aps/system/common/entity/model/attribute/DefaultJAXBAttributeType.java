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

import com.agiletec.aps.system.common.entity.model.attribute.util.BaseAttributeValidationRules;
import com.agiletec.aps.system.common.entity.model.attribute.util.DateAttributeValidationRules;
import com.agiletec.aps.system.common.entity.model.attribute.util.IAttributeValidationRules;
import com.agiletec.aps.system.common.entity.model.attribute.util.NumberAttributeValidationRules;
import com.agiletec.aps.system.common.entity.model.attribute.util.OgnlValidationRule;
import com.agiletec.aps.system.common.entity.model.attribute.util.TextAttributeValidationRules;
import com.agiletec.aps.system.common.searchengine.IndexableAttributeInterface;
import com.fasterxml.jackson.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;
import lombok.extern.slf4j.Slf4j;
import org.entando.entando.aps.system.services.api.IApiErrorCodes;
import org.entando.entando.aps.system.services.api.model.ApiException;

/**
 * @author E.Santoboni
 */
@Slf4j
@XmlRootElement(name = "attributeType")
@JsonRootName(value = "attributeType")
@XmlType(propOrder = {"name", "names", "description", "type", "roles", "searchable", "indexable", "validationRules"})
@XmlSeeAlso({ArrayList.class, BaseAttributeValidationRules.class, DateAttributeValidationRules.class,
        NumberAttributeValidationRules.class, TextAttributeValidationRules.class, OgnlValidationRule.class})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "classType", defaultImpl = DefaultJAXBAttributeType.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = JAXBCompositeAttributeType.class),
        @JsonSubTypes.Type(value = JAXBEnumeratorAttributeType.class),
        @JsonSubTypes.Type(value = JAXBListAttributeType.class)
})
public class DefaultJAXBAttributeType {

    @XmlElement(name = "name", required = true)
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = "names", required = false)
    @JsonProperty("names")
    public Map<String, String> getNames() {
        return names;
    }

    public void setNames(Map<String, String> names) {
        this.names = names;
    }

    @XmlElement(name = "description", required = false)
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @XmlElement(name = "type", required = true)
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlElement(name = "role", required = false)
    @XmlElementWrapper(name = "roles")
    @JsonProperty("roles")
    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Boolean getIndexable() {
        return indexable;
    }

    public void setIndexable(Boolean indexable) {
        this.indexable = indexable;
    }

    public Boolean getSearchable() {
        return searchable;
    }

    public void setSearchable(Boolean searchable) {
        this.searchable = searchable;
    }

    public IAttributeValidationRules getValidationRules() {
        return validationRules;
    }

    public void setValidationRules(IAttributeValidationRules validationRules) {
        this.validationRules = validationRules;
    }

    public AttributeInterface createAttribute(Map<String, AttributeInterface> attributes) throws ApiException {
        AttributeInterface attribute;
        try {
            AttributeInterface master = attributes.get(this.getType());
            if (null == master) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR,
                        "Attribute Type '" + this.getType() + "' does not exist");
            }
            attribute = (AttributeInterface) master.getAttributePrototype();
            Pattern pattern = Pattern.compile("\\w+");
            Matcher matcher = pattern.matcher(this.getName());
            if (null == this.getName() || !matcher.matches()) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR,
                        "Invalid name '" + this.getName() + "' of Attribute Type '" + this.getType() + "'");
            }
            attribute.setName(this.getName());
            attribute.setDescription(this.getDescription());
            attribute.setRoles(this.toArray(this.getRoles()));
            if (null != this.getSearchable()) {
                attribute.setSearchable(this.getSearchable());
            }
            if (null != this.getIndexable()) {
                attribute.setIndexingType(IndexableAttributeInterface.INDEXING_TYPE_TEXT);
            }
            attribute.setValidationRules(this.getValidationRules());
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception ex) {
            log.error("Error creating attribute '{}'", this.getName(), ex);
            throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR,
                    "Error creating attribute '" + this.getName() + "'");
        }
        return attribute;
    }

    private String[] toArray(List<String> strings) {
        if (null == strings || strings.isEmpty()) {
            return new String[]{};
        }
        String[] array = new String[strings.size()];
        for (int i = 0; i < strings.size(); i++) {
            array[i] = strings.get(i);
        }
        return array;
    }

    private String name;
    private Map<String, String> names;
    private String description;
    private String type;
    private List<String> roles;

    private Boolean searchable;
    private Boolean indexable;

    private IAttributeValidationRules validationRules;

}
