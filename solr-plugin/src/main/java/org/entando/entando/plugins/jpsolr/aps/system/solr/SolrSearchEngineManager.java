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

import static org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields.SOLR_FIELD_NAME;

import org.entando.entando.aps.system.common.entity.search.NestedSearchSupport;
import com.agiletec.aps.system.common.entity.event.EntityTypesChangingEvent;
import com.agiletec.aps.system.common.entity.event.EntityTypesChangingObserver;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.SmallEntityType;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
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
import com.google.common.util.concurrent.Striped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.entando.entando.aps.system.services.cache.ICacheInfoManager;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.entando.entando.plugins.jpsolr.aps.system.solr.SolrFieldsChecker.CheckFieldsResult;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.ContentTypeSettings;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFacetedContentsResult;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author E.Santoboni
 */
@Slf4j
// NOTE: java:S2143 ("use the java.time API") is intentionally suppressed. Legacy java.util.Date is
// used only to stamp a reload timestamp (new Date()); java.time migration is out of scope for
// ESB-1133 (boolean search) and tracked separately.
@SuppressWarnings("java:S2143")
public class SolrSearchEngineManager extends SearchEngineManager
        implements ISolrSearchEngineManager, PublicContentChangedObserver, EntityTypesChangingObserver,
        InitializingBean {

    public static final String RELOAD_FIELDS_NAME = "RELOAD_FIELDS";
    private static final String LAST_RELOAD_CACHE_PARAM_NAME = "SolrSearchEngine_lastReloadInfo";

    @Setter
    private transient ILangManager langManager;
    @Setter
    private transient ICacheInfoManager cacheInfoManager;
    @Setter
    private transient ISolrProxyTenantAware solrProxy;

    private static final Striped<Lock> tenantsLock = Striped.lazyWeakLock(64);

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            Thread t = new Thread(this::refreshAllTenantsFields);
            t.setName(RELOAD_FIELDS_NAME);
            t.start();
        } catch (Throwable t) {
            log.error("Unable to start refresh field thread", t);
        }
    }

    @Override
    public void init() throws Exception {
        log.info("** Solr Search Engine active **");
    }

    @Override
    public void refresh() throws Throwable {
        this.release();
        this.setLastReloadInfo(null);
        this.setStatus(STATUS_READY);
        this.solrProxy.init();
        this.init();
    }

    @Override
    protected void release() {
        try {
            this.solrProxy.close();
        } catch (Exception ex) {
            log.error("Error closing Solr resources {}", ex);
        }
    }

    @Override
    public void destroy() {
        try {
            this.solrProxy.destroy();
        } catch (Exception ex) {
            log.error("Error destroying Solr resources {}", ex);
        }
    }

    private void refreshAllTenantsFields() {
        for (ISolrResourcesAccessor tenantResources : this.solrProxy.getAllSolrTenantsResources()) {
            Lock lock = tenantsLock.get(tenantResources.getSolrCore());
            lock.lock();
            try {
                boolean refresh = this.getContentTypesSettings(tenantResources.getSolrSchemaDAO())
                        .stream().anyMatch(settings -> !settings.isValid());
                if (refresh) {
                    log.info("Refreshing CMS fields for core '{}'", tenantResources.getSolrCore());
                    refreshCmsFields(tenantResources);
                }
            } catch (EntException ex) {
                log.error("Error refreshing CMS fields", ex);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    protected ISearchEngineDAOFactory getFactory() {
        throw new UnsupportedOperationException("Solr search engine manager doesn't need a DAO factory");
    }

    @Override
    protected ISearcherDAO getSearcherDao() {
        try {
            return this.solrProxy.getSearcherDAO();
        } catch (Exception e) {
            throw new EntRuntimeException("Error extracting searcher", e);
        }
    }

    @Override
    protected IIndexerDAO getIndexerDao() {
        try {
            return this.solrProxy.getIndexerDAO();
        } catch (Exception e) {
            throw new EntRuntimeException("Error extracting indexer", e);
        }
    }

    @Override
    public List<ContentTypeSettings> getContentTypesSettings() throws EntException {
        return getContentTypesSettings(this.solrProxy.getSolrSchemaDAO());
    }

    private List<ContentTypeSettings> getContentTypesSettings(ISolrSchemaDAO schemaDAO) throws EntException {
        List<ContentTypeSettings> list = new ArrayList<>();
        try {
            List<Map<String, ?>> fields = schemaDAO.getFields();
            for (SmallEntityType entityType : this.getContentManager().getSmallEntityTypes()) {
                ContentTypeSettings typeSettings = new ContentTypeSettings(entityType.getCode(),
                        entityType.getDescription());
                list.add(typeSettings);
                Content prototype = this.getContentManager().createContentType(entityType.getCode());
                List<Lang> languages = this.langManager.getLangs();
                for (AttributeInterface attribute : prototype.getAttributeList()) {
                    Map<String, Map<String, Serializable>> currentConfig =
                            this.buildCurrentFieldConfig(attribute.getName(), languages, fields);
                    typeSettings.addAttribute(attribute, currentConfig, languages);
                    if (!attribute.isSimple()) {
                        this.addNestedSearchSettings(attribute, fields, languages, typeSettings);
                    }
                }
            }
        } catch (Exception e) {
            throw new EntException("Error extracting config", e);
        }
        return list;
    }

    private Map<String, Map<String, Serializable>> buildCurrentFieldConfig(String attributeName,
            List<Lang> languages, List<Map<String, ?>> fields) {
        Map<String, Map<String, Serializable>> currentConfig = new HashMap<>();
        for (Lang lang : languages) {
            String fieldName = lang.getCode().toLowerCase() + "_" + attributeName;
            fields.stream()
                    .filter(f -> f.get(SOLR_FIELD_NAME).equals(fieldName))
                    .findFirst().ifPresent(currentField ->
                            currentConfig.put(fieldName, (Map<String, Serializable>) currentField));
        }
        return currentConfig;
    }

    /**
     * Report every path-indexed nested boolean of this complex attribute in the content-type settings,
     * under the same full path the schema and the index use (e.g.
     * {@code en_complexAttrName_boolAttrName}), so the admin "content types settings" endpoint and the
     * lazy schema-refresh validity check ({@code isValid()}) stay in sync with them. The set of
     * eligible attributes and their paths come from the engine's shared traversal, which is what
     * guarantees the three Solr consumers agree.
     */
    private void addNestedSearchSettings(AttributeInterface attribute, List<Map<String, ?>> fields,
            List<Lang> languages, ContentTypeSettings typeSettings) {
        NestedSearchSupport.forEachIndexableNested(attribute, (child, path) -> {
            Map<String, Map<String, Serializable>> currentConfig =
                    this.buildCurrentFieldConfig(path, languages, fields);
            typeSettings.addNestedSearchAttribute(child, path, currentConfig, languages);
        });
    }

    @Override
    public void refreshCmsFields() throws EntException {
        refreshCmsFields(solrProxy.getSolrTenantResources());
    }

    private void refreshCmsFields(ISolrResourcesAccessor tenantResources) throws EntException {
        Lock lock = tenantsLock.get(tenantResources.getSolrCore());
        lock.lock();
        try {
            List<Map<String, ?>> fields = tenantResources.getSolrSchemaDAO().getFields();
            List<AttributeInterface> attributes = this.getContentManager().getSmallEntityTypes().stream()
                    .flatMap(entityType -> getAttributesToCheck(entityType.getCode()).stream())
                    .collect(Collectors.toList());
            refreshFields(fields, attributes);
        } catch (Exception ex) {
            throw new EntException("Error refreshing config", ex);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void refreshContentType(String typeCode) throws EntException {
        Lock lock = tenantsLock.get(this.solrProxy.getSolrCore());
        lock.lock();
        try {
            List<Map<String, ?>> fields = this.solrProxy.getSolrSchemaDAO().getFields();
            refreshFields(fields, getAttributesToCheck(typeCode));
        } catch (Exception ex) {
            throw new EntException("Error refreshing contentType " + typeCode, ex);
        } finally {
            lock.unlock();
        }
    }

    private List<AttributeInterface> getAttributesToCheck(String entityTypeCode) {
        Content prototype = this.getContentManager().createContentType(entityTypeCode);
        if (null == prototype) {
            log.warn("Type '{}' does not exists", entityTypeCode);
            return List.of();
        }
        return prototype.getAttributeList();
    }

    @Override
    public void deleteIndexedEntity(String entityId) throws EntException {
        Lock lock = tenantsLock.get(this.solrProxy.getSolrCore());
        lock.lock();
        try {
            this.getIndexerDao().delete(SolrFields.SOLR_CONTENT_ID_FIELD_NAME, entityId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void updateFromEntityTypesChanging(EntityTypesChangingEvent event) {
        Lock lock = tenantsLock.get(this.solrProxy.getSolrCore());
        lock.lock();
        try {
            super.updateFromEntityTypesChanging(event);
            List<Map<String, ?>> fields = this.solrProxy.getSolrSchemaDAO().getFields();
            List<AttributeInterface> attributes;
            if (this.getContentManager().getName().equals(event.getEntityManagerName())
                    && event.getOperationCode() != EntityTypesChangingEvent.REMOVE_OPERATION_CODE) {
                String typeCode = event.getNewEntityType().getTypeCode();
                attributes = getAttributesToCheck(typeCode);
            } else {
                attributes = List.of();
            }
            refreshFields(fields, attributes);
        } finally {
            lock.unlock();
        }
    }

    private void refreshFields(List<Map<String, ?>> fields, List<AttributeInterface> attributes) {
        refreshFields(fields, attributes, solrProxy.getSolrSchemaDAO());
    }

    private void refreshFields(List<Map<String, ?>> fields, List<AttributeInterface> attributes,
            ISolrSchemaDAO schemaDAO) {
        SolrFieldsChecker fieldsChecker = new SolrFieldsChecker(fields, attributes, langManager.getLangs());
        CheckFieldsResult result = fieldsChecker.checkFields();
        if (result.needsUpdate()) {
            schemaDAO.updateFields(result.getFieldsToAdd(), result.getFieldsToReplace());
        }
    }

    @Override
    public Thread startReloadContentsReferencesByType(String typeCode) throws EntException {
        Lock lock = tenantsLock.get(this.solrProxy.getSolrCore());
        lock.lock();
        try {
            return this.startReloadContentsReferencesPrivate(typeCode);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Thread startReloadContentsReferences() throws EntException {
        Lock lock = tenantsLock.get(this.solrProxy.getSolrCore());
        lock.lock();
        try {
            this.solrProxy.getIndexerDAO().deleteAllDocuments();
            return this.startReloadContentsReferencesPrivate(null);
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Deprecated(since = "7.2.0")
    public Thread startReloadContentsReferences(String subDirectory) throws EntException {
        throw new UnsupportedOperationException("This method is not supported if Solr is active");
    }

    @Override
    public void addEntityToIndex(IApsEntity entity) throws EntException {
        Lock lock = tenantsLock.get(this.solrProxy.getSolrCore());
        lock.lock();
        try {
            super.addEntityToIndex(entity);
        } finally {
            lock.unlock();
        }
    }

    private synchronized Thread startReloadContentsReferencesPrivate(String typeCode) throws EntException {
        SolrIndexLoaderThread loaderThread = null;
        if (this.solrProxy.getIndexStatus().canReloadIndexes()) {
            try {
                IIndexerDAO newIndexer = this.solrProxy.getIndexerDAO();
                loaderThread = new SolrIndexLoaderThread(typeCode, this, this.getContentManager(), newIndexer);
                String threadName = ICmsSearchEngineManager.RELOAD_THREAD_NAME_PREFIX
                        + DateConverter.getFormattedDate(new Date(), "yyyyMMddHHmmss")
                        + typeCode;
                loaderThread.setName(threadName);
                this.solrProxy.getIndexStatus().setReloadInProgress();
                loaderThread.start();
                log.info("Reload Contents References job started");
            } catch (Throwable ex) {
                this.solrProxy.getIndexStatus().rollbackStatus();
                throw new EntException("Error reloading Contents References", ex);
            }
        } else {
            log.info("Reload Contents References job suspended: current status: {}", this.getStatus());
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
        this.solrProxy.getIndexStatus().setReadyIfPossible();
    }

    @Override
    public LastReloadInfo getLastReloadInfo() {
        return (SolrLastReloadInfo) this.cacheInfoManager.getFromCache(ICacheInfoManager.DEFAULT_CACHE_NAME,
                this.getLastReloadCacheKey());
    }

    private String getLastReloadCacheKey() {
        String suffix = ApsTenantApplicationUtils.getTenant().orElse("_primary_");
        return LAST_RELOAD_CACHE_PARAM_NAME + "_" + suffix;
    }

    @Override
    public int getStatus() {
        return this.solrProxy.getIndexStatus().getValue();
    }

    @Override
    protected void setStatus(int status) {
        this.solrProxy.getIndexStatus().setValue(status);
    }
}
