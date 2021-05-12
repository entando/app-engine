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
package org.entando.entando.plugins.jpsolr.aps.system.solr;

import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields;
import com.agiletec.aps.system.common.IManager;
import com.agiletec.aps.system.common.entity.event.EntityTypesChangingEvent;
import com.agiletec.aps.system.common.entity.event.EntityTypesChangingObserver;
import com.agiletec.aps.system.common.entity.model.SmallEntityType;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.DateAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.NumberAttribute;
import com.agiletec.aps.system.common.searchengine.IndexableAttributeInterface;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.plugins.jacms.aps.system.services.content.event.PublicContentChangedObserver;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.ContentTypeSettings;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author E.Santoboni
 */
public class SearchEngineManager extends com.agiletec.plugins.jacms.aps.system.services.searchengine.SearchEngineManager 
        implements ISolrSearchEngineManager, PublicContentChangedObserver, EntityTypesChangingObserver {
    
    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(SearchEngineManager.class);
    
    @Autowired
    private ILangManager langManager;
    /*
    @Override
    public List<Map<String, Object>> getFields() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean addField(Map<String, Object> properties) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean updateField(Map<String, Object> properties) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean deleteField(String fieldName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    */

    @Override
    public List<ContentTypeSettings> getContentTypesSettings() throws EntException {
        List<ContentTypeSettings> list = new ArrayList<>();
        try {
            List<Map<String, Object>> fields = ((ISolrSearchEngineDAOFactory) this.getFactory()).getFields();
            List<SmallEntityType> entityTypes = this.getContentManager().getSmallEntityTypes();
            for (int i = 0; i < entityTypes.size(); i++) {
                SmallEntityType entityType = entityTypes.get(i);
                ContentTypeSettings typeSettings = new ContentTypeSettings(entityType.getCode(), entityType.getDescription());
                list.add(typeSettings);
                Content prototype = this.getContentManager().createContentType(entityType.getCode());
                Iterator<AttributeInterface> iterAttribute = prototype.getAttributeList().iterator();
                while (iterAttribute.hasNext()) {
                    AttributeInterface attribute = iterAttribute.next();
                    Map<String, Map<String, Object>> currentConfig = new HashMap<>();
                    List<Lang> langs = this.getLangManager().getLangs();
                    for (int j = 0; j < langs.size(); j++) {
                        Lang lang = (Lang) langs.get(j);
                        String fieldName = lang.getCode().toLowerCase() + "_" + attribute.getName();
                        Map<String, Object> currentField = fields.stream().filter(f -> f.get("name").equals(fieldName)).findFirst().orElse(null);
                        if (null != currentField) {
                            currentConfig.put(fieldName, currentField);
                        }
                    }
                    typeSettings.addAttribute(attribute, currentConfig);
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting config", e);
            throw new EntException("Error", e);
        }
        return list;
    }
    
    @Override
    public void refreshCmsFields() throws EntException {
        try {
            List<Map<String, Object>> fields = ((ISolrSearchEngineDAOFactory) this.getFactory()).getFields();
            this.checkLangFields(fields);
            List<SmallEntityType> entityTypes = this.getContentManager().getSmallEntityTypes();
            Map<String, Map<String, Object>> checkedFields = new HashMap<>();
            for (int i = 0; i < entityTypes.size(); i++) {
                if (i == 0) {
                    this.refreshBaseFields(fields, checkedFields);
                }
                SmallEntityType entityType = entityTypes.get(i);
                this.refreshEntityType(fields, checkedFields, entityType.getCode());
            }
        } catch (Exception e) {
            logger.error("Error refreshing config", e);
            throw new EntException("Error", e);
        }
    }

    @Override
    public void refreshContentType(String typeCode) throws EntException {
        try {
            List<Map<String, Object>> fields = ((ISolrSearchEngineDAOFactory) this.getFactory()).getFields();
            this.checkLangFields(fields);
            this.refreshBaseFields(fields, null);
            this.refreshEntityType(fields, null, typeCode);
        } catch (Exception e) {
            logger.error("Error refreshing contentType " + typeCode, e);
            throw new EntException("Error", e);
        }
    }
    
    private void refreshBaseFields(List<Map<String, Object>> fields, Map<String, Map<String, Object>> checkedFields) {
        this.checkField(fields, checkedFields, SolrFields.SOLR_CONTENT_ID_FIELD_NAME, "string");
        this.checkField(fields, checkedFields, SolrFields.SOLR_CONTENT_TYPE_FIELD_NAME, "text_general");
        this.checkField(fields, checkedFields, SolrFields.SOLR_CONTENT_GROUP_FIELD_NAME, "text_general", true);
        this.checkField(fields, checkedFields, SolrFields.SOLR_CONTENT_DESCRIPTION_FIELD_NAME, "text_gen_sort");
        this.checkField(fields, checkedFields, SolrFields.SOLR_CONTENT_MAIN_GROUP_FIELD_NAME, "text_general");
        this.checkField(fields, checkedFields, SolrFields.SOLR_CONTENT_CATEGORY_FIELD_NAME, "text_general", true);
    }
    
    protected void refreshEntityType(List<Map<String, Object>> currentFields, 
            Map<String, Map<String, Object>> checkedFields, String entityTypeCode) {
        Content prototype = this.getContentManager().createContentType(entityTypeCode);
        if (null == prototype) {
            logger.warn("Type '" + entityTypeCode + "' does not exists");
            return;
        }
        Iterator<AttributeInterface> iterAttribute = prototype.getAttributeList().iterator();
        while (iterAttribute.hasNext()) {
            AttributeInterface currentAttribute = iterAttribute.next();
            List<Lang> langs = this.getLangManager().getLangs();
            for (int j = 0; j < langs.size(); j++) {
                Lang currentLang = (Lang) langs.get(j);
                this.checkAttribute(currentFields, checkedFields, currentAttribute, currentLang);
            }
        }
    }

    private void checkAttribute(List<Map<String, Object>> currentFields, 
            Map<String, Map<String, Object>> checkedFields, AttributeInterface attribute, Lang lang) {
        attribute.setRenderingLang(lang.getCode());
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
            String fieldName = lang.getCode().toLowerCase() + "_" + attribute.getName();
            fieldName = fieldName.replaceAll(":", "_");
            this.checkField(currentFields, checkedFields, fieldName, type);
            if (null == attribute.getRoles()) {
                return;
            }
            for (int i = 0; i < attribute.getRoles().length; i++) {
                String roleFieldName = lang.getCode().toLowerCase() + "_" + attribute.getRoles()[i];
                roleFieldName = roleFieldName.replaceAll(":", "_");
                this.checkField(currentFields, null, roleFieldName, type);
            }
        }
    }
    
    private void checkField(List<Map<String, Object>> currentFields, 
            Map<String, Map<String, Object>> checkedFields, String fieldName, String type) {
        this.checkField(currentFields, checkedFields, fieldName, type, false);
    }
    
    private void checkField(List<Map<String, Object>> currentFields, 
            Map<String, Map<String, Object>> checkedFields, String fieldName, String type, boolean multiValue) {
        Map<String, Object> currentField = currentFields.stream().filter(f -> f.get("name").equals(fieldName)).findFirst().orElse(null);
        if (null != currentField) {
            if (currentField.get("type").equals(type)
                    && ((null == currentField.get("multiValued") && multiValue) || (null != currentField.get("multiValued") && currentField.get("multiValued").equals(multiValue)))) {
                return;
            } else {
                logger.warn("Field '" + fieldName + "' already exists but with different configuration!"
                        + " - type '" + currentField.get("type") + "' to '" + type + "'"
                        + " - multiValued '" + currentField.get("multiValued") + "' to '" + multiValue + "'");
            }
        } 
        Map<String, Object> newField = new HashMap<>();
        newField.put("name", fieldName);
        newField.put("type", type);
        newField.put("multiValued", multiValue);
        if (null == currentField) {
            ((ISolrSearchEngineDAOFactory) this.getFactory()).addField(newField);
        } else if (!type.equals(currentField.get("type")) || !Boolean.valueOf(multiValue).equals((Boolean) currentField.get("multiValued"))) {
            ((ISolrSearchEngineDAOFactory) this.getFactory()).replaceField(newField);
        }
        if (null != checkedFields) {
            checkedFields.put(fieldName, newField);
        }
    }
    
    @Override
    public void deleteIndexedEntity(String entityId) throws EntException {
        try {
            this.getIndexerDao().delete(SolrFields.SOLR_CONTENT_ID_FIELD_NAME, entityId);
        } catch (EntException e) {
            logger.error("Error deleting content {} from index", entityId, e);
            throw e;
        }
    }
    
    @Override
    public void updateFromEntityTypesChanging(EntityTypesChangingEvent event) {
        super.updateFromEntityTypesChanging(event);
        List<Map<String, Object>> fields = ((ISolrSearchEngineDAOFactory) this.getFactory()).getFields();
        this.checkLangFields(fields);
        if (((IManager) this.getContentManager()).getName().equals(event.getEntityManagerName()) 
                && event.getOperationCode() != EntityTypesChangingEvent.REMOVE_OPERATION_CODE) {
            String typeCode = event.getNewEntityType().getTypeCode();
            this.refreshEntityType(fields, new HashMap<String, Map<String, Object>>(), typeCode);
        }
    }
    
    private void checkLangFields(List<Map<String, Object>> fields) {
        List<Lang> langs = this.getLangManager().getLangs();
        for (int j = 0; j < langs.size(); j++) {
            Lang currentLang = langs.get(j);
            this.checkField(fields, null, currentLang.getCode(), "text_general", true);
        }
    }

    protected ILangManager getLangManager() {
        return langManager;
    }
    public void setLangManager(ILangManager langManager) {
        this.langManager = langManager;
    }
    
}
