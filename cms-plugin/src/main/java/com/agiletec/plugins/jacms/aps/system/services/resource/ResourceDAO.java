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
package com.agiletec.plugins.jacms.aps.system.services.resource;

import com.agiletec.aps.system.common.AbstractSearcherDAO;
import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.services.category.Category;
import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ResourceInterface;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ResourceRecordVO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.cache.ICacheInfoManager;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

/**
 * Data Access Object per gli oggetti risorsa.
 *
 * @author E.Santoboni - W.Ambu
 */
public class ResourceDAO extends AbstractSearcherDAO implements IResourceDAO {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(ResourceDAO.class);
    
    private ICategoryManager categoryManager;

    private final String LOAD_RESOURCE_VO
            = "SELECT restype, descr, maingroup, resourcexml, masterfilename, creationdate, lastmodified, owner, folderpath, correlationcode FROM resources WHERE resid = ? ";

    private static final String LOAD_RESOURCE_VO_BY_CODE
            = "SELECT resid, restype, descr, maingroup, resourcexml, masterfilename, creationdate, lastmodified, owner, folderpath, correlationcode FROM resources WHERE correlationcode = ? ";

    private final String ADD_RESOURCE
            = "INSERT INTO resources (resid, restype, descr, maingroup, resourcexml, masterfilename, creationdate, lastmodified, owner, folderpath, correlationcode) "
            + "VALUES ( ? , ? , ? , ? , ? , ? , ? , ? , ?, ?, ?)";

    private final String UPDATE_RESOURCE
            = "UPDATE resources SET restype = ? , descr = ? , maingroup = ? , resourcexml = ? , masterfilename = ? , lastmodified = ?, folderpath = ? WHERE resid = ? ";

    private final String DELETE_CONTENTS_REFERENCE
            = "DELETE FROM contentrelations WHERE refresource = ? ";

    private final String DELETE_RESOURCE
            = "DELETE FROM resources WHERE resid = ? ";

    private final String ADD_RESOURCE_REL_RECORD
            = "INSERT INTO resourcerelations (resid, refcategory) VALUES ( ? , ? )";

    private final String DELETE_RESOURCE_REL_RECORD
            = "DELETE FROM resourcerelations WHERE resid = ? ";

    protected ICategoryManager getCategoryManager() {
        return categoryManager;
    }

    public void setCategoryManager(ICategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }

    /**
     * Carica una risorsa nel db.
     *
     * @param resource La risorsa da caricare nel db.
     */
    @Override
    public void addResource(ResourceInterface resource) {
        Connection conn = null;
        try {
            conn = this.getConnection();
            conn.setAutoCommit(false);
            this.executeAddResource(resource, conn);
            conn.commit();
        } catch (Throwable t) {
            this.executeRollback(conn);
            logger.error("Error adding resource", t);
            throw new EntRuntimeException("Error adding resource", t);
        } finally {
            closeConnection(conn);
        }
    }

    protected void executeAddResource(ResourceInterface resource, Connection conn) throws EntException {
        this.addResourceRecord(resource, conn);
        this.addCategoryRelationsRecord(resource, conn);
    }

    protected void addResourceRecord(ResourceInterface resource, Connection conn) throws EntException {
        PreparedStatement stat = null;
        try {
            stat = conn.prepareStatement(ADD_RESOURCE);
            stat.setString(1, resource.getId());
            stat.setString(2, resource.getType());
            stat.setString(3, resource.getDescription().trim());
            stat.setString(4, resource.getMainGroup());
            stat.setString(5, resource.getXML());
            stat.setString(6, resource.getMasterFileName().trim());
            Date creationDate = (null != resource.getCreationDate())
                    ? resource.getCreationDate() : new Date();
            stat.setTimestamp(7, new java.sql.Timestamp(creationDate.getTime()));
            stat.setTimestamp(8, new java.sql.Timestamp(creationDate.getTime()));
            stat.setString(9, resource.getOwner());
            stat.setString(10, resource.getFolderPath());
            stat.setString(11, resource.getCorrelationCode());
            stat.executeUpdate();
        } catch (Throwable t) {
            logger.error("Error adding resource record", t);
            throw new EntRuntimeException("Error adding resource record", t);
        } finally {
            closeDaoResources(null, stat);
        }
    }

    /**
     * Aggiorna una risorsa nel database.
     *
     * @param resource La risorsa da aggiornare nel db.
     */
    @Override
    @CacheEvict(value = ICacheInfoManager.DEFAULT_CACHE_NAME, key = "'jacms_resource_'.concat(#resource.id)", condition = "null != #resource")
    public void updateResource(ResourceInterface resource) {
        Connection conn = null;
        try {
            conn = this.getConnection();
            conn.setAutoCommit(false);
            this.executeUpdateResource(resource, conn);
            conn.commit();
        } catch (Throwable t) {
            this.executeRollback(conn);
            logger.error("Error updating resource", t);
            throw new EntRuntimeException("Error updating resource", t);
        } finally {
            closeConnection(conn);
        }
    }

    protected void executeUpdateResource(ResourceInterface resource, Connection conn) throws EntException {
        this.deleteRecordsById(resource.getId(), DELETE_RESOURCE_REL_RECORD, conn);
        this.updateResourceRecord(resource, conn);
        this.addCategoryRelationsRecord(resource, conn);
    }

    protected void updateResourceRecord(ResourceInterface resource, Connection conn) throws EntException {
        PreparedStatement stat = null;
        try {
            stat = conn.prepareStatement(UPDATE_RESOURCE);
            stat.setString(1, resource.getType());
            stat.setString(2, resource.getDescription().trim());
            stat.setString(3, resource.getMainGroup());
            stat.setString(4, resource.getXML());
            stat.setString(5, resource.getMasterFileName().trim());
            if (null != resource.getLastModified()) {
                stat.setTimestamp(6, new java.sql.Timestamp(resource.getLastModified().getTime()));
            } else {
                stat.setTimestamp(6, new java.sql.Timestamp(new java.util.Date().getTime()));
            }
            stat.setString(7, resource.getFolderPath());
            stat.setString(8, resource.getId());
            stat.executeUpdate();
        } catch (Throwable t) {
            logger.error("Error updating resource record", t);
            throw new EntRuntimeException("Error updating resource record", t);
        } finally {
            closeDaoResources(null, stat);
        }
    }

    /**
     * Cancella una risorsa dal db.
     *
     * @param id L'identificativo della risorsa da cancellare.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = ICacheInfoManager.DEFAULT_CACHE_NAME,
                    key = "'jacms_resource_'.concat(#id)",
                    condition = "null != #id"),
            @CacheEvict(value = ICacheInfoManager.DEFAULT_CACHE_NAME,
                    key = "'jacms_resource_code_'.concat(#code)",
                    condition = "null != #code")
    })
    public void deleteResource(String id, String code) {
        Connection conn = null;
        try {
            conn = this.getConnection();
            conn.setAutoCommit(false);
            this.executeDeleteResource(id, conn);
            conn.commit();
        } catch (Throwable t) {
            this.executeRollback(conn);
            logger.error("Error deleting resource {}", id, t);
            throw new EntRuntimeException("Error deleting resource " + id, t);
        } finally {
            this.closeConnection(conn);
        }
    }

    protected void executeDeleteResource(String resourceId, Connection conn) throws EntException {
        PreparedStatement stat = null;
        try {
            this.deleteRecordsById(resourceId, DELETE_RESOURCE_REL_RECORD, conn);
            this.deleteRecordsById(resourceId, DELETE_CONTENTS_REFERENCE, conn);
            stat = conn.prepareStatement(DELETE_RESOURCE);
            stat.setString(1, resourceId);
            stat.executeUpdate();
        } catch (Throwable t) {
            logger.error("Error deleting resource {}", resourceId, t);
            throw new EntRuntimeException("Error deleting resource " + resourceId, t);
        } finally {
            closeDaoResources(null, stat);
        }
    }

    /**
     * Carica una lista di identificativi di risorse in base al tipo, ad una
     * parola chiave e dalla categoria della risorsa.
     *
     * @param type Tipo di risorsa da cercare.
     * @param text Testo immesso per il raffronto con la descrizione della
     * risorsa. null o stringa vuota nel caso non si voglia ricercare le risorse
     * per parola chiave.
     * @param categoryCode Il codice della categoria delle risorse. null o
     * stringa vuota nel caso non si voglia ricercare le risorse per categoria.
     * @param groupCodes I codici dei gruppi utenti consentiti tramite il quale
     * filtrare le risorse. Nel caso che la collezione di codici sia nulla o
     * vuota, non verr?? eseguito la selezione per gruppi.
     * @return La lista di identificativi di risorse.
     */
    @Override
    public List<String> searchResourcesId(String type, String text, String categoryCode, Collection<String> groupCodes) {
        return this.searchResourcesId(type, text, null, categoryCode, groupCodes);
    }
    
    @Override
    public List<String> searchResourcesId(String type, String text, String filename, String categoryCode, Collection<String> groupCodes) {
        FieldSearchFilter[] filters = this.createFilters(type, text, filename, groupCodes);
        List<String> categories = (StringUtils.isBlank(categoryCode)) ? null : Arrays.asList(categoryCode);
        return this.searchResourcesId(filters, categories);
    }

    private FieldSearchFilter[] createFilters(String type, String text, String filename, Collection<String> groupCodes) {
        FieldSearchFilter[] filters = new FieldSearchFilter[0];
        if (null != type && type.trim().length() > 0) {
            FieldSearchFilter<String> filterToAdd = new FieldSearchFilter(IResourceManager.RESOURCE_TYPE_FILTER_KEY, type, false);
            filters = super.addFilter(filters, filterToAdd);
        }
        if (null != text && text.trim().length() > 0) {
            FieldSearchFilter<String> filterToAdd = new FieldSearchFilter(IResourceManager.RESOURCE_DESCR_FILTER_KEY, text, true);
            filters = super.addFilter(filters, filterToAdd);
        }
        if (null != filename && filename.trim().length() > 0) {
            FieldSearchFilter<String> filterToAdd = new FieldSearchFilter(IResourceManager.RESOURCE_FILENAME_FILTER_KEY, filename, true);
            filters = super.addFilter(filters, filterToAdd);
        }
        filters = this.addGroupFilter(filters, groupCodes);
        if (filters.length == 0) {
            return null;
        }
        return filters;
    }

    private FieldSearchFilter[] addGroupFilter(FieldSearchFilter[] filters, Collection<String> groupCodes) {
        if (groupCodes != null && groupCodes.size() > 0) {
            List<String> allowedValues = new ArrayList<>();
            allowedValues.addAll(groupCodes);
            FieldSearchFilter<String> filterToAdd = new FieldSearchFilter(IResourceManager.RESOURCE_MAIN_GROUP_FILTER_KEY, allowedValues, false);
            filters = super.addFilter(filters, filterToAdd);
        }
        return filters;
    }

    @Override
    @Deprecated
    public List<String> searchResourcesId(FieldSearchFilter[] filters, String categoryCode, Collection<String> groupCodes) {
        filters = this.addGroupFilter(filters, groupCodes);
        List<String> categories = (StringUtils.isBlank(categoryCode)) ? null : Arrays.asList(categoryCode);
        return this.searchResourcesId(filters, categories);
    }
    
    @Override
    public List<String> searchResourcesId(FieldSearchFilter[] filters, List<String> categories, Collection<String> groupCodes) {
        filters = this.addGroupFilter(filters, groupCodes);
        return this.searchResourcesId(filters, categories);
    }

    @Override
    public Integer countResources(FieldSearchFilter[] filters, List<String> categories, Collection<String> groupCodes) {
        Connection conn = null;
        int count = 0;
        PreparedStatement stat = null;
        ResultSet result = null;
        try {
            conn = this.getConnection();
            filters = this.addGroupFilter(filters, groupCodes);
            stat = this.buildStatement(filters, categories, true, conn);
            result = stat.executeQuery();
            if (result.next()) {
                count = result.getInt(1);
            }
        } catch (Throwable t) {
            logger.error("Error while loading the count of IDs", t);
            throw new EntRuntimeException("Error while loading the count of IDs", t);
        } finally {
            closeDaoResources(result, stat, conn);
        }
        return count;
    }
    
    @Override
    public List<String> searchResourcesId(FieldSearchFilter[] filters, List<String> categories) {
        Connection conn = null;
        List<String> resources = new ArrayList<>();
        PreparedStatement stat = null;
        ResultSet res = null;
        try {
            conn = this.getConnection();
            stat = this.buildStatement(filters, categories, false, conn);
            res = stat.executeQuery();
            while (res.next()) {
                String id = res.getString(this.getMasterTableIdFieldName());
                if (!resources.contains(id)) {
                    resources.add(id);
                }
            }
        } catch (Throwable t) {
            logger.error("Error loading resources id", t);
            throw new EntRuntimeException("Error loading resources id", t);
        } finally {
            closeDaoResources(res, stat, conn);
        }
        return resources;
    }

    private PreparedStatement buildStatement(FieldSearchFilter[] filters, List<String> categories, boolean isCount, Connection conn) {
        String query = this.createQueryString(filters, categories, isCount);
        PreparedStatement stat = null;
        try {
            stat = conn.prepareStatement(query);
            int index = 0;
            if (null != categories && categories.size() > 0) {
                for (String category : categories) {
                    stat.setString(++index, category);
                }
            }
            index = this.addMetadataFieldFilterStatementBlock(filters, index, stat);
        } catch (Throwable t) {
            logger.error("Error while creating the statement", t);
            throw new EntRuntimeException("Error while creating the statement", t);
        }
        return stat;
    }

    private String createQueryString(FieldSearchFilter[] filters, List<String> categories, boolean isCount) {
        StringBuffer query = this.createBaseQueryBlock(filters, false, isCount, categories);
        this.appendMetadataFieldFilterQueryBlocks(filters, query, false);
        if (!isCount) {
            super.appendOrderQueryBlocks(filters, query, false);
            this.appendLimitQueryBlock(filters, query);
        }
        return query.toString();
    }

    private StringBuffer createBaseQueryBlock(FieldSearchFilter[] filters, boolean selectAll, boolean isCount, List<String> categories) {
        StringBuffer query = super.createBaseQueryBlock(filters, isCount, selectAll);
        if (categories != null) {
            for (int i = 0; i < categories.size(); i++) {
                query.append(String.format(
                        "INNER JOIN resourcerelations res%d ON resources.resid = res%d.resid AND res%d.refcategory= ? ",
                        i, i, i));
            }
        }
        return query;
    }

    /**
     * Carica un record di risorse in funzione dell'id Risorsa. Questo record ??
     * necessario per l'estrazione della risorse in oggetto tipo
     * AbstractResource da parte del ResourceManager.
     *
     * @param id L'identificativo della risorsa.
     * @return Il record della risorsa.
     */
    @Override
    @Cacheable(value = ICacheInfoManager.DEFAULT_CACHE_NAME, key = "'jacms_resource_'.concat(#id)",
            condition = "null != #id", unless = "null == #result")
    public ResourceRecordVO loadResourceVo(String id) {
        Connection conn = null;
        ResourceRecordVO resourceVo = null;
        PreparedStatement stat = null;
        ResultSet res = null;
        try {
            conn = this.getConnection();
            stat = conn.prepareStatement(LOAD_RESOURCE_VO);
            stat.setString(1, id);
            res = stat.executeQuery();
            if (res.next()) {
                resourceVo = new ResourceRecordVO();
                resourceVo.setId(id);
                resourceVo.setResourceType(res.getString(1));
                resourceVo.setDescr(res.getString(2));
                resourceVo.setMainGroup(res.getString(3));
                resourceVo.setXml(res.getString(4));
                resourceVo.setMasterFileName(res.getString(5));
                resourceVo.setCreationDate(res.getTimestamp(6));
                resourceVo.setLastModified(res.getTimestamp(7));
                resourceVo.setOwner(res.getString(8));
                resourceVo.setFolderPath(res.getString(9));
                resourceVo.setCorrelationCode(res.getString(10));
            }
        } catch (Exception t) {
            logger.error("Error loading resource {}", id, t);
            throw new EntRuntimeException("Error loading resource" + id, t);
        } finally {
            closeDaoResources(res, stat, conn);
        }
        return resourceVo;
    }

    @Override
    @Cacheable(value = ICacheInfoManager.DEFAULT_CACHE_NAME, key = "'jacms_resource_code_'.concat(#code)",
            condition = "null != #code", unless = "null == #result")
    public ResourceRecordVO loadResourceVoByCorrelationCode(String code) {
        Connection conn = null;
        ResourceRecordVO resourceVo = null;
        PreparedStatement stat = null;
        ResultSet res = null;
        try {
            conn = this.getConnection();
            stat = conn.prepareStatement(LOAD_RESOURCE_VO_BY_CODE);
            stat.setString(1, code);
            res = stat.executeQuery();
            if (res.next()) {
                resourceVo = new ResourceRecordVO();
                resourceVo.setId(res.getString(1));
                resourceVo.setResourceType(res.getString(2));
                resourceVo.setDescr(res.getString(3));
                resourceVo.setMainGroup(res.getString(4));
                resourceVo.setXml(res.getString(5));
                resourceVo.setMasterFileName(res.getString(6));
                resourceVo.setCreationDate(res.getTimestamp(7));
                resourceVo.setLastModified(res.getTimestamp(8));
                resourceVo.setOwner(res.getString(9));
                resourceVo.setFolderPath(res.getString(10));
                resourceVo.setCorrelationCode(res.getString(11));
            }
        } catch (Exception t) {
            logger.error("Error loading resource {}", code, t);
            throw new EntRuntimeException("Error loading resource" + code, t);
        } finally {
            closeDaoResources(res, stat, conn);
        }
        return resourceVo;
    }

    /**
     * Metodo di servizio. Aggiunge un record nella tabella resourcerelations
     * per ogni categoria della risorsa.
     *
     * @param resource La risorsa del quale referenziare le categorie.
     * @param conn La connessione con il db.
     * @throws EntException
     */
    protected void addCategoryRelationsRecord(ResourceInterface resource, Connection conn) throws EntException {
        if (resource.getCategories().size() > 0) {
            PreparedStatement stat = null;
            try {
                Set<String> codes = this.getCategoryCodes(resource);
                stat = conn.prepareStatement(ADD_RESOURCE_REL_RECORD);
                Iterator<String> codeIter = codes.iterator();
                while (codeIter.hasNext()) {
                    String code = (String) codeIter.next();
                    stat.setString(1, resource.getId());
                    stat.setString(2, code);
                    stat.addBatch();
                    stat.clearParameters();
                }
                stat.executeBatch();
            } catch (Exception t) {
                logger.error("Error adding resourcerelations record for {}", resource.getId(), t);
                throw new EntRuntimeException("Error adding resourcerelations record for " + resource.getId(), t);
            } finally {
                closeDaoResources(null, stat);
            }
        }
    }

    /**
     * Restituisce la lista di codici di categorie associate ad una risorsa. La
     * risorsa viene sempre referenziata con la categoria "root" della tipologia
     * relativa (che corrisponde al codice della tipologia).
     *
     * @param resource La risorsa da inserire o da modificare.
     * @return Il set di codici di categorie.
     */
    private Set<String> getCategoryCodes(ResourceInterface resource) {
        Set<String> codes = new HashSet<>();
        Iterator<Category> categoryIter = resource.getCategories().iterator();
        while (categoryIter.hasNext()) {
            Category category = (Category) categoryIter.next();
            this.addCategoryCode(resource, category, codes);
        }
        return codes;
    }

    private void addCategoryCode(ResourceInterface resource, Category category, Set<String> codes) {
        if (category.getCode().equals(category.getParentCode())) {
            return;
        }
        codes.add(category.getCode());
        Category parentCategory = this.getCategoryManager().getCategory(category.getParentCode());
        if (null != parentCategory) {
            this.addCategoryCode(resource, parentCategory, codes);
        }
    }
    
    protected void deleteRecordsById(String resourceId, String query, Connection conn) {
        super.executeQueryWithoutResultset(conn, query, resourceId);
    }
    
    /* ESTENSIONE SPOSTAMENTO NODI */
    @Override
    public void updateResourceRelations(ResourceInterface resource) {
        Connection conn = null;
        try {
            conn = this.getConnection();
            conn.setAutoCommit(false);
            this.deleteRecordsById(resource.getId(), DELETE_RESOURCE_REL_RECORD, conn);
            this.addCategoryRelationsRecord(resource, conn);
            conn.commit();
        } catch (Exception t) {
            this.executeRollback(conn);
            this.processDaoException(t, "Error updating resource category relations", "updateResourceRelations");
        } finally {
            closeConnection(conn);
        }
    }

    @Override
    protected String getMasterTableName() {
        return "resources";
    }

    @Override
    protected String getMasterTableIdFieldName() {
        return "resid";
    }

    @Override
    protected String getTableFieldName(String metadataFieldKey) {
        return metadataFieldKey;
    }

}
