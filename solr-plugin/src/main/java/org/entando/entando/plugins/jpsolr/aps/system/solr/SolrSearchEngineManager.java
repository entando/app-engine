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

import static org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields.SOLR_FIELD_MULTIVALUED;
import static org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields.SOLR_FIELD_NAME;
import static org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields.SOLR_FIELD_TYPE;

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
import com.agiletec.aps.util.ApsTenantApplicationUtils;
import com.agiletec.aps.util.DateConverter;
import com.agiletec.plugins.jacms.aps.system.services.content.event.PublicContentChangedObserver;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.IIndexerDAO;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ISearchEngineDAOFactory;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ISearcherDAO;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.LastReloadInfo;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.SearchEngineManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.entando.entando.aps.system.services.cache.ICacheInfoManager;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.ContentTypeSettings;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFacetedContentsResult;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * @author E.Santoboni
 */
@Service
@Primary
public class SolrSearchEngineManager extends SearchEngineManager
        implements ISolrSearchEngineManager, PublicContentChangedObserver, EntityTypesChangingObserver {

    private static final String LAST_RELOAD_CACHE_PARAM_NAME = "SolrSearchEngine_lastReloadInfo";

    private static final Logger logger = LoggerFactory.getLogger(SolrSearchEngineManager.class);

    @Autowired
    private transient ILangManager langManager;
    @Autowired
    private transient ICacheInfoManager cacheInfoManager;
    @Autowired
    private transient ISolrSearchEngineDAOFactory factory;

    @Override
    @PostConstruct
    public void init() throws Exception {
        logger.debug("{} ready. Initialized", this.getClass().getName());
    }

    @Override
    protected ISearcherDAO getSearcherDao() {
        try {
            return this.factory.getSearcher();
        } catch (Exception e) {
            throw new EntRuntimeException("Error extracting searcher", e);
        }
    }

    @Override
    protected IIndexerDAO getIndexerDao() {
        try {
            return this.factory.getIndexer();
        } catch (Exception e) {
            throw new EntRuntimeException("Error extracting indexer", e);
        }
    }

    @Override
    protected ISearchEngineDAOFactory getFactory() {
        return this.factory;
    }

    @Override
    public List<ContentTypeSettings> getContentTypesSettings() throws EntException {
        List<ContentTypeSettings> list = new ArrayList<>();
        try {
            List<Map<String, Serializable>> fields = this.factory.getFields();
            for (SmallEntityType entityType : this.getContentManager().getSmallEntityTypes()) {
                ContentTypeSettings typeSettings = new ContentTypeSettings(entityType.getCode(),
                        entityType.getDescription());
                list.add(typeSettings);
                Content prototype = this.getContentManager().createContentType(entityType.getCode());
                for (AttributeInterface attribute : prototype.getAttributeList()) {
                    Map<String, Map<String, Serializable>> currentConfig = new HashMap<>();
                    for (Lang lang : this.langManager.getLangs()) {
                        String fieldName = lang.getCode().toLowerCase() + "_" + attribute.getName();
                        Map<String, Serializable> currentField = fields.stream()
                                .filter(f -> f.get(SOLR_FIELD_NAME).equals(fieldName))
                                .findFirst().orElse(null);
                        if (null != currentField) {
                            currentConfig.put(fieldName, currentField);
                        }
                    }
                    typeSettings.addAttribute(attribute, currentConfig);
                }
            }
        } catch (Exception e) {
            throw new EntException("Error extracting config", e);
        }
        return list;
    }

    @Override
    public void refreshCmsFields() throws EntException {
        try {
            List<Map<String, Serializable>> fields = this.factory.getFields();
            this.checkLangFields(fields);
            this.refreshBaseFields(fields, null);
            Map<String, Map<String, Serializable>> checkedFields = new HashMap<>();
            for (SmallEntityType entityType : this.getContentManager().getSmallEntityTypes()) {
                fields = this.factory.getFields();
                this.refreshEntityType(fields, checkedFields, entityType.getCode());
            }
        } catch (Exception ex) {
            throw new EntException("Error refreshing config", ex);
        }
    }

    @Override
    public void refreshContentType(String typeCode) throws EntException {
        try {
            List<Map<String, Serializable>> fields = this.factory.getFields();
            this.checkLangFields(fields);
            this.refreshBaseFields(fields, null);
            this.refreshEntityType(fields, null, typeCode);
        } catch (Exception ex) {
            throw new EntException("Error refreshing contentType " + typeCode, ex);
        }
    }

    private void refreshBaseFields(List<Map<String, Serializable>> fields,
            Map<String, Map<String, Serializable>> checkedFields) {
        this.checkField(fields, checkedFields, SolrFields.SOLR_CONTENT_ID_FIELD_NAME, SolrFields.TYPE_STRING);
        this.checkField(fields, checkedFields, SolrFields.SOLR_CONTENT_TYPE_CODE_FIELD_NAME,
                SolrFields.TYPE_TEXT_GENERAL);
        this.checkField(fields, checkedFields, SolrFields.SOLR_CONTENT_GROUP_FIELD_NAME, SolrFields.TYPE_TEXT_GENERAL,
                true);
        this.checkField(fields, checkedFields, SolrFields.SOLR_CONTENT_DESCRIPTION_FIELD_NAME,
                SolrFields.TYPE_TEXT_GEN_SORT);
        this.checkField(fields, checkedFields, SolrFields.SOLR_CONTENT_MAIN_GROUP_FIELD_NAME,
                SolrFields.TYPE_TEXT_GENERAL);
        this.checkField(fields, checkedFields, SolrFields.SOLR_CONTENT_CATEGORY_FIELD_NAME,
                SolrFields.TYPE_TEXT_GENERAL, true);
        this.checkField(fields, checkedFields, SolrFields.SOLR_CONTENT_CREATION_FIELD_NAME, SolrFields.TYPE_PDATES,
                false);
        this.checkField(fields, checkedFields, SolrFields.SOLR_CONTENT_LAST_MODIFY_FIELD_NAME, SolrFields.TYPE_PDATES,
                false);
    }

    protected void refreshEntityType(List<Map<String, Serializable>> currentFields,
            Map<String, Map<String, Serializable>> checkedFields, String entityTypeCode) {
        Content prototype = this.getContentManager().createContentType(entityTypeCode);
        if (null == prototype) {
            logger.warn("Type '{}' does not exists", entityTypeCode);
            return;
        }
        for (AttributeInterface currentAttribute : prototype.getAttributeList()) {
            for (Lang lang : this.langManager.getLangs()) {
                this.checkAttribute(currentFields, checkedFields, currentAttribute, lang);
            }
        }
    }

    private void checkAttribute(List<Map<String, Serializable>> currentFields,
            Map<String, Map<String, Serializable>> checkedFields, AttributeInterface attribute, Lang lang) {
        attribute.setRenderingLang(lang.getCode());
        if (attribute instanceof IndexableAttributeInterface
                || ((attribute instanceof DateAttribute || attribute instanceof NumberAttribute)
                && attribute.isSearchable())) {
            String type;
            if (attribute instanceof DateAttribute) {
                type = SolrFields.TYPE_PDATES;
            } else if (attribute instanceof NumberAttribute) {
                type = SolrFields.TYPE_PLONGS;
            } else if (attribute instanceof BooleanAttribute) {
                type = SolrFields.TYPE_BOOLEAN;
            } else {
                type = SolrFields.TYPE_TEXT_GEN_SORT;
            }
            String fieldName = lang.getCode().toLowerCase() + "_" + attribute.getName();
            fieldName = fieldName.replace(":", "_");
            this.checkField(currentFields, checkedFields, fieldName, type);
            if (null == attribute.getRoles()) {
                return;
            }
            for (String role : attribute.getRoles()) {
                String roleFieldName = lang.getCode().toLowerCase() + "_" + role;
                roleFieldName = roleFieldName.replace(":", "_");
                this.checkField(currentFields, null, roleFieldName, type);
            }
        }
    }

    private void checkField(List<Map<String, Serializable>> currentFields,
            Map<String, Map<String, Serializable>> checkedFields, String fieldName, String type) {
        this.checkField(currentFields, checkedFields, fieldName, type, false);
    }

    private void checkField(List<Map<String, Serializable>> currentFields,
            Map<String, Map<String, Serializable>> checkedFields, String fieldName, String type, boolean multiValue) {
        Map<String, Serializable> currentField = currentFields.stream()
                .filter(f -> f.get(SOLR_FIELD_NAME).equals(fieldName))
                .findFirst().orElse(null);
        if (null != currentField) {
            if (currentField.get(SOLR_FIELD_TYPE).equals(type)
                    && ((null == currentField.get(SOLR_FIELD_MULTIVALUED) && multiValue) || (
                    null != currentField.get(SOLR_FIELD_MULTIVALUED) && currentField.get(SOLR_FIELD_MULTIVALUED)
                            .equals(multiValue)))) {
                return;
            } else {
                logger.warn(
                        "Field '{}' already exists but with different configuration! - type '{}' to '{}' - multiValued '{}' to '{}'",
                        fieldName, currentField.get(SOLR_FIELD_TYPE), type, currentField.get(SOLR_FIELD_MULTIVALUED),
                        multiValue);
            }
        }
        Map<String, Serializable> newField = new HashMap<>();
        newField.put(SOLR_FIELD_NAME, fieldName);
        newField.put(SOLR_FIELD_TYPE, type);
        newField.put(SOLR_FIELD_MULTIVALUED, multiValue);
        if (null == currentField) {
            this.factory.addField(newField);
        } else if (!type.equals(currentField.get(SOLR_FIELD_TYPE)) || !Boolean.valueOf(multiValue)
                .equals(currentField.get(SOLR_FIELD_MULTIVALUED))) {
            this.factory.replaceField(newField);
        }
        if (null != checkedFields) {
            checkedFields.put(fieldName, newField);
        }
    }

    @Override
    public void deleteIndexedEntity(String entityId) throws EntException {
        this.getIndexerDao().delete(SolrFields.SOLR_CONTENT_ID_FIELD_NAME, entityId);
    }

    @Override
    public void updateFromEntityTypesChanging(EntityTypesChangingEvent event) {
        super.updateFromEntityTypesChanging(event);
        List<Map<String, Serializable>> fields = this.factory.getFields();
        this.checkLangFields(fields);
        this.refreshBaseFields(fields, null);
        if (this.getContentManager().getName().equals(event.getEntityManagerName())
                && event.getOperationCode() != EntityTypesChangingEvent.REMOVE_OPERATION_CODE) {
            String typeCode = event.getNewEntityType().getTypeCode();
            this.refreshEntityType(fields, new HashMap<>(), typeCode);
        }
    }

    private void checkLangFields(List<Map<String, Serializable>> fields) {
        for (Lang lang : this.langManager.getLangs()) {
            this.checkField(fields, null, lang.getCode(), SolrFields.TYPE_TEXT_GENERAL, true);
        }
    }

    @Override
    public Thread startReloadContentsReferencesByType(String typeCode) throws EntException {
        return this.startReloadContentsReferencesPrivate(typeCode);
    }

    @Override
    public Thread startReloadContentsReferences() throws EntException {
        ((ISolrSearchEngineDAOFactory) this.factory).deleteAllDocuments();
        return this.startReloadContentsReferencesPrivate(null);
    }

    private Thread startReloadContentsReferencesPrivate(String typeCode) throws EntException {
        SolrIndexLoaderThread loaderThread = null;
        if (this.getStatus() == STATUS_READY || this.getStatus() == STATUS_NEED_TO_RELOAD_INDEXES) {
            try {
                IIndexerDAO newIndexer = this.factory.getIndexer();
                loaderThread = new SolrIndexLoaderThread(typeCode, this, this.getContentManager(), newIndexer);
                String threadName = ICmsSearchEngineManager.RELOAD_THREAD_NAME_PREFIX
                        + DateConverter.getFormattedDate(new Date(), "yyyyMMddHHmmss")
                        + typeCode;
                loaderThread.setName(threadName);
                this.setStatus(STATUS_RELOADING_INDEXES_IN_PROGRESS);
                loaderThread.start();
                logger.info("Reload Contents References job started");
            } catch (RuntimeException ex) {
                throw new EntException("Error reloading Contents References", ex);
            }
        } else {
            logger.info("Reload Contents References job suspended: current status: {}", this.getStatus());
        }
        return loaderThread;
    }

    @Override
    public SolrFacetedContentsResult searchFacetedEntities(SearchEngineFilter[][] filters,
            SearchEngineFilter[] categories, Collection<String> allowedGroups) throws EntException {
        return ((ISolrSearcherDAO) this.getSearcherDao()).searchFacetedContents(filters, categories, allowedGroups);
    }

    @Override
    public void notifyEndingIndexLoading(LastReloadInfo info, IIndexerDAO newIndexerDAO) {
        this.cacheInfoManager.putInCache(ICacheInfoManager.DEFAULT_CACHE_NAME, this.getLastReloadCacheKey(), info);
        if (this.getStatus() != STATUS_NEED_TO_RELOAD_INDEXES) {
            this.setStatus(STATUS_READY);
        }
    }

    @Override
    public LastReloadInfo getLastReloadInfo() {
        return (SolrLastReloadInfo) this.cacheInfoManager.getFromCache(ICacheInfoManager.DEFAULT_CACHE_NAME, this.getLastReloadCacheKey());
    }

    private String getLastReloadCacheKey() {
        String suffix = ApsTenantApplicationUtils.getTenant().orElse("_primary_");
        return LAST_RELOAD_CACHE_PARAM_NAME + "_" + suffix;
    }
}
