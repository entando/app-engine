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

import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.services.lang.Lang;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public void addAttribute(AttributeInterface attribute, Map<String, Map<String, Serializable>> currentField, List<Lang> languages) {
        AttributeSettings settings = new AttributeSettings(attribute, languages);
        this.getAttributeSettings().add(settings);
        settings.setCurrentConfig(currentField);
        if (attribute.hasSearchField()) {
            // The expected type is the attribute's own declaration, translated once by SolrFields -
            // the same call the schema checker and the indexer make, so the three cannot disagree.
            Map<String, Serializable> newField = new HashMap<>();
            newField.put(SOLR_FIELD_TYPE, SolrFields.solrType(attribute.getSearchFieldType()));
            newField.put(SOLR_FIELD_MULTIVALUED, false);
            settings.setExpectedConfig(newField);
        }
    }

    /**
     * Registers a searchable boolean-like attribute nested inside a Composite attribute, reported under
     * its full path {@code <composite>_<child>} rather than its own name. The path is what the schema
     * field and the index actually use, so it is also the only identifier that distinguishes two
     * same-named children of different Composites - reporting the bare child name made those two rows
     * indistinguishable in the settings screen and in {@code GET /config}.
     *
     * <p>Single-valued: a Composite occurs at most once per document per lang (List/Monolist ancestry is
     * excluded by the caller), so the nested field is never repeated.</p>
     *
     * @param attribute the nested boolean-like attribute; its type drives the expected Solr type.
     * @param path the full path key, matching the keys of {@code currentField} minus the lang prefix.
     * @param currentField the schema fields found for this path, keyed by {@code <lang>_<path>}.
     * @param languages the languages a complete configuration must cover.
     */
    public void addNestedSearchAttribute(AttributeInterface attribute, String path,
            Map<String, Map<String, Serializable>> currentField, List<Lang> languages) {
        AttributeSettings settings = new AttributeSettings(attribute, path, languages);
        this.getAttributeSettings().add(settings);
        settings.setCurrentConfig(currentField);
        Map<String, Serializable> newField = new HashMap<>();
        newField.put(SOLR_FIELD_TYPE, SolrFields.solrType(attribute.getSearchFieldType()));
        newField.put(SOLR_FIELD_MULTIVALUED, false);
        settings.setExpectedConfig(newField);
    }

    public boolean isValid() {
        return this.getAttributeSettings().stream().allMatch(AttributeSettings::isValid);
    }

    public static class AttributeSettings implements Serializable {

        private String code;
        private String typeCode;
        private Map<String, Map<String, Serializable>> currentConfig;
        private Map<String, Serializable> expectedConfig;
        private final List<String> expectedLanguages;

        public AttributeSettings(AttributeInterface attribute, List<Lang> expectedLanguages) {
            this(attribute, attribute.getName(), expectedLanguages);
        }

        /**
         * @param attribute the attribute being reported.
         * @param code how it is addressed in the schema and the index - its own name at top level, its
         * full path when nested in a Composite. Taking it explicitly keeps this in step with the keys of
         * {@code currentConfig}, which are always {@code <lang>_<code>}.
         * @param expectedLanguages the languages a complete configuration must cover.
         */
        public AttributeSettings(AttributeInterface attribute, String code, List<Lang> expectedLanguages) {
            this.setCode(code);
            this.setTypeCode(attribute.getType());
            this.expectedLanguages = expectedLanguages.stream().map(Lang::getCode).collect(Collectors.toList());
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
            } else if (null == this.getCurrentConfig() || this.getCurrentConfig().isEmpty() || !this.languagesMatch()) {
                return false;
            } else {
                return this.getCurrentConfig().values().stream().allMatch(m ->
                        m.get(SOLR_FIELD_TYPE).equals(this.getExpectedConfig().get(SOLR_FIELD_TYPE)) &&
                                m.getOrDefault(SOLR_FIELD_MULTIVALUED, false)
                                        .equals(this.getExpectedConfig().get(SOLR_FIELD_MULTIVALUED))
                );
            }
        }

        private boolean languagesMatch() {
            if (this.getCurrentConfig().size() != this.expectedLanguages.size()) {
                return false;
            }
            for (String fieldName : this.getCurrentConfig().keySet()) {
                String extractedLang = fieldName.substring(0, fieldName.indexOf("_"));
                if (!this.expectedLanguages.contains(extractedLang)) {
                    return false;
                }
            }
            return true;
        }
    }

}
