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

import static org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields.SOLR_FIELD_MULTIVALUED;
import static org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields.SOLR_FIELD_TYPE;
import static org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields.SOLR_FIELD_TYPE_BOOLEAN;
import static org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields.SOLR_FIELD_TYPE_PDATES;
import static org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields.SOLR_FIELD_TYPE_PLONGS;
import static org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields.SOLR_FIELD_TYPE_TEXT_GEN_SORT;

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

    public void addAttribute(AttributeInterface attribute, Map<String, Map<String, Serializable>> currentField) {
        AttributeSettings settings = new AttributeSettings(attribute);
        this.getAttributeSettings().add(settings);
        settings.setCurrentConfig(currentField);
        if (attribute instanceof IndexableAttributeInterface
                || ((attribute instanceof DateAttribute || attribute instanceof NumberAttribute)
                && attribute.isSearchable())) {
            String type = null;
            if (attribute instanceof DateAttribute) {
                type = SOLR_FIELD_TYPE_PDATES;
            } else if (attribute instanceof NumberAttribute) {
                type = SOLR_FIELD_TYPE_PLONGS;
            } else if (attribute instanceof BooleanAttribute) {
                type = SOLR_FIELD_TYPE_BOOLEAN;
            } else {
                type = SOLR_FIELD_TYPE_TEXT_GEN_SORT;
            }
            Map<String, Serializable> newField = new HashMap<>();
            newField.put(SOLR_FIELD_TYPE, type);
            newField.put(SOLR_FIELD_MULTIVALUED, false);
            settings.setExpectedConfig(newField);
        }
    }

    public boolean isValid() {
        return this.getAttributeSettings().stream().allMatch(AttributeSettings::isValid);
    }

    public static class AttributeSettings implements Serializable {

        private String code;
        private String typeCode;
        private Map<String, Map<String, Serializable>> currentConfig;
        private Map<String, Serializable> expectedConfig;

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

        public Map<String, Map<String, Serializable>> getCurrentConfig() {
            return currentConfig;
        }

        public void setCurrentConfig(Map<String, Map<String, Serializable>> currentConfig) {
            this.currentConfig = currentConfig;
        }

        public Map<String, Serializable> getExpectedConfig() {
            return expectedConfig;
        }

        public void setExpectedConfig(Map<String, Serializable> expectedConfig) {
            this.expectedConfig = expectedConfig;
        }

        public boolean isValid() {
            if (null == this.getExpectedConfig()) {
                return true;
            } else if (null == this.getCurrentConfig() || this.getCurrentConfig().isEmpty()) {
                return false;
            } else {
                return this.getCurrentConfig().values().stream().allMatch(m ->
                        m.get(SOLR_FIELD_TYPE).equals(this.getExpectedConfig().get(SOLR_FIELD_TYPE)) &&
                                m.getOrDefault(SOLR_FIELD_MULTIVALUED, false)
                                        .equals(this.getExpectedConfig().get(SOLR_FIELD_MULTIVALUED))
                );
            }
        }
    }

}
