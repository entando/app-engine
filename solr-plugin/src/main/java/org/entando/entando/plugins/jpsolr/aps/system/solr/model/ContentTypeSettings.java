/*
 * Copyright 2021-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpsolr.aps.system.solr.model;

import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.DateAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.NumberAttribute;
import com.agiletec.aps.system.common.searchengine.IndexableAttributeInterface;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author E.Santoboni
 */
public class ContentTypeSettings implements Serializable {
    
    private String typeCode;
    private String typeDescription;
    private List<AttributeSettings> attributeSettings = new ArrayList<>();

    public ContentTypeSettings(String typeCode, String typeDescription) {
        this.setTypeCode(typeCode);
        this.setTypeDescription(typeDescription);
    }
    
    public String getTypeCode() {
        return typeCode;
    }
    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getTypeDescription() {
        return typeDescription;
    }
    public void setTypeDescription(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    public List<AttributeSettings> getAttributeSettings() {
        return attributeSettings;
    }
    public void setAttributeSettings(List<AttributeSettings> attributeSettings) {
        this.attributeSettings = attributeSettings;
    }
    
    public void addAttribute(AttributeInterface attribute, Map<String, Map<String, Object>> currentField) {
        AttributeSettings settings = new AttributeSettings(attribute);
        this.getAttributeSettings().add(settings);
        settings.setCurrentConfig(currentField);
        if (attribute instanceof IndexableAttributeInterface
                || ((attribute instanceof DateAttribute || attribute instanceof NumberAttribute) && attribute.isSearchable())) {
            String type = null;
            if (attribute instanceof DateAttribute) {
                type = "pdates";
            } else if (attribute instanceof NumberAttribute) {
                type = "plongs";
            } else if (attribute instanceof BooleanAttribute) {
                type = "boolean";
            } else {
                type = "text_gen_sort";
            }
            Map<String, Object> newField = new HashMap<>();
            newField.put("type", type);
            newField.put("multiValued", false);
            settings.setExpectedConfig(newField);
        }
    }
    
    public boolean isValid() {
        Optional<AttributeSettings> optional = this.getAttributeSettings().stream().filter(s -> !s.isValid()).findFirst();
        return !optional.isPresent();
    }
    
    public static class AttributeSettings implements Serializable {
        
        private String code;
        private String typeCode;
        private Map<String, Map<String, Object>> currentConfig;
        private Map<String, Object> expectedConfig;
        
        public AttributeSettings(AttributeInterface attribute) {
            this.setCode(attribute.getName());
            this.setTypeCode(attribute.getType());
        }
        
        public String getCode() {
            return code;
        }
        public void setCode(String code) {
            this.code = code;
        }

        public String getTypeCode() {
            return typeCode;
        }
        public void setTypeCode(String typeCode) {
            this.typeCode = typeCode;
        }
        
        public Map<String, Map<String, Object>> getCurrentConfig() {
            return currentConfig;
        }
        public void setCurrentConfig(Map<String, Map<String, Object>>currentConfig) {
            this.currentConfig = currentConfig;
        }

        public Map<String, Object> getExpectedConfig() {
            return expectedConfig;
        }
        public void setExpectedConfig(Map<String, Object> expectedConfig) {
            this.expectedConfig = expectedConfig;
        }
        
        public boolean isValid() {
            if (null == this.getExpectedConfig()) {
                return true;
            } else if (null == this.getCurrentConfig() || this.getCurrentConfig().isEmpty()) {
                return false;
            } else {
                Optional<Map<String, Object>> optional = this.getCurrentConfig().values().stream().filter(m -> {
                    return (!m.get("type").equals(this.getExpectedConfig().get("type")) || !m.getOrDefault("multiValued", false).equals(this.getExpectedConfig().get("multiValued")));
                }).findFirst();
                return !optional.isPresent();
            }
        }
        
    }
    
}
