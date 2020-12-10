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
package org.entando.entando.plugins.jpsolr.aps.system.solr;

import com.agiletec.aps.system.common.entity.model.*;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.DateAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.NumberAttribute;
import com.agiletec.aps.system.common.searchengine.IndexableAttributeInterface;
import com.agiletec.aps.system.common.tree.ITreeNode;
import com.agiletec.aps.system.common.tree.ITreeNodeManager;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.category.Category;
import com.agiletec.aps.system.services.lang.*;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.IIndexerDAO;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;

/**
 * Data Access Object dedita alla indicizzazione di documenti.
 */
public class IndexerDAO implements IIndexerDAO {

    private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(IndexerDAO.class);
    
    private String solrAddress;

    private String solrCore;

    private ILangManager langManager;

    private ITreeNodeManager treeNodeManager;

    @Override
    public void init(File dir) throws EntException {
        // nothing to do
    }
    
    private SolrClient getSolrClient() {
        return new HttpSolrClient.Builder(this.solrAddress)
                .withConnectionTimeout(10000)
                .withSocketTimeout(60000)
                .build();
    }

    @Override
    public synchronized void add(IApsEntity entity) throws EntException {
        SolrClient client = null;
        try {
            client = this.getSolrClient();
            SolrInputDocument document = this.createDocument(entity);
            UpdateResponse updateResponse = client.add(this.getSolrCore(), document);
            client.commit(this.getSolrCore());
        } catch (Throwable t) {
            _logger.error("Error saving entity {}", entity.getId(), t);
            throw new EntException("Error saving entity", t);
        } finally {
            if (null != client) {
                try {
                    client.close();
                } catch (IOException ex) {
                    throw new EntException("Error closing client", ex);
                }
            }
        }
    }

    protected SolrInputDocument createDocument(IApsEntity entity) throws EntException {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(SolrFields.SOLR_CONTENT_ID_FIELD_NAME, entity.getId());
        document.addField(SolrFields.SOLR_CONTENT_TYPE_FIELD_NAME,entity.getTypeCode());
        document.addField(SolrFields.SOLR_CONTENT_GROUP_FIELD_NAME, entity.getMainGroup());
        Iterator<String> iterGroups = entity.getGroups().iterator();
        while (iterGroups.hasNext()) {
            String groupName = (String) iterGroups.next();
            document.addField(SolrFields.SOLR_CONTENT_GROUP_FIELD_NAME, groupName);
        }
        if (entity instanceof Content) {
            if (null != entity.getDescription()) {
                document.addField(SolrFields.SOLR_CONTENT_DESCRIPTION_FIELD_NAME, entity.getDescription());
            }
            document.addField(SolrFields.SOLR_CONTENT_TYPE_CODE_FIELD_NAME, entity.getTypeCode());
            document.addField(SolrFields.SOLR_CONTENT_MAIN_GROUP_FIELD_NAME, entity.getMainGroup());
            Date creation = ((Content) entity).getCreated();
            Date lastModify = (null != ((Content) entity).getLastModified()) ? ((Content) entity).getLastModified() : creation;
            if (null != creation) {
                document.addField(SolrFields.SOLR_CONTENT_CREATION_FIELD_NAME, creation);
            }
            if (null != lastModify) {
                document.addField(SolrFields.SOLR_CONTENT_LAST_MODIFY_FIELD_NAME, lastModify);
            }
        }
        Iterator<AttributeInterface> iterAttribute = entity.getAttributeList().iterator();
        while (iterAttribute.hasNext()) {
            AttributeInterface currentAttribute = iterAttribute.next();
            Object value = currentAttribute.getValue();
            if (null == value) {
                continue;
            }
            List<Lang> langs = this.getLangManager().getLangs();
            for (int i = 0; i < langs.size(); i++) {
                Lang currentLang = (Lang) langs.get(i);
                this.indexAttribute(document, currentAttribute, currentLang);
            }
        }
        List<Category> categories = entity.getCategories();
        if (null != categories && !categories.isEmpty()) {
            for (int i = 0; i < categories.size(); i++) {
                ITreeNode category = categories.get(i);
                this.indexCategory(document, category);
            }
        }
        return document;
    }

    protected void indexAttribute(SolrInputDocument document, AttributeInterface attribute, Lang lang) {
        attribute.setRenderingLang(lang.getCode());
        if (attribute instanceof IndexableAttributeInterface
                || ((attribute instanceof DateAttribute || attribute instanceof NumberAttribute) && attribute.isSearchable())) {
            Object valueToIndex = null;
            if (attribute instanceof DateAttribute) {
                valueToIndex = ((DateAttribute) attribute).getDate();
            } else if (attribute instanceof NumberAttribute) {
                valueToIndex = ((NumberAttribute)attribute).getValue();
                if (null != valueToIndex) {
                    valueToIndex = ((BigDecimal) valueToIndex).intValue();
                }
            } else {
                valueToIndex = ((IndexableAttributeInterface) attribute).getIndexeableFieldValue();
            }
            if (null == valueToIndex) {
                return;
            }
            if (attribute instanceof IndexableAttributeInterface) {
                // full text search
                String indexingType = attribute.getIndexingType();
                if (null != indexingType
                        && IndexableAttributeInterface.INDEXING_TYPE_UNSTORED.equalsIgnoreCase(indexingType)) {
                    document.addField(lang.getCode(), valueToIndex);
                }
                if (null != indexingType
                        && IndexableAttributeInterface.INDEXING_TYPE_TEXT.equalsIgnoreCase(indexingType)) {
                    document.addField(lang.getCode(), valueToIndex);
                }
            }
            String fieldName = lang.getCode().toLowerCase() + "_" + attribute.getName();
            this.indexValue(document, fieldName, valueToIndex);
            if (null == attribute.getRoles()) {
                return;
            }
            for (int i = 0; i < attribute.getRoles().length; i++) {
                String roleFieldName = lang.getCode().toLowerCase() + "_" + attribute.getRoles()[i];
                this.indexValue(document, roleFieldName, valueToIndex);
            }
        }
    }
    
    private void indexValue(SolrInputDocument document, String fieldName, Object valueToIndex) {
        fieldName = fieldName.replaceAll(":", "_");
        document.addField(fieldName, valueToIndex);
    }

    protected void indexCategory(SolrInputDocument document, ITreeNode categoryToIndex) {
        if (null == categoryToIndex || categoryToIndex.isRoot()) {
            return;
        }
        document.addField(SolrFields.SOLR_CONTENT_CATEGORY_FIELD_NAME,
                categoryToIndex.getPath(SolrFields.SOLR_CONTENT_CATEGORY_SEPARATOR, false, this.getTreeNodeManager()));
        ITreeNode parentCategory = this.getTreeNodeManager().getNode(categoryToIndex.getParentCode());
        this.indexCategory(document, parentCategory);
    }

    @Override
    public synchronized void delete(String name, String value) throws EntException {
        SolrClient client = null;
        try {
            client = this.getSolrClient();
            UpdateResponse updateResponse = (name.equals(SolrFields.SOLR_CONTENT_ID_FIELD_NAME)) ? 
                    client.deleteById(this.getSolrCore(), value) : 
                    client.deleteByQuery(this.getSolrCore(), name + ":" + value);
            client.commit(this.getSolrCore());
        } catch (Throwable t) {
            _logger.error("Error deleting document {} : {}", name, value, t);
            throw new EntException("Error deleting entity", t);
        } finally {
            if (null != client) {
                try {
                    client.close();
                } catch (IOException ex) {
                    throw new EntException("Error closing client", ex);
                }
            }
        }
    }

    @Override
    public void close() {
        // nothing to do
    }
    
    protected String getSolrAddress() {
        return solrAddress;
    }
    protected void setSolrAddress(String solrAddress) {
        this.solrAddress = solrAddress;
    }

    protected String getSolrCore() {
        return solrCore;
    }
    protected void setSolrCore(String solrCore) {
        this.solrCore = solrCore;
    }
    
    protected ILangManager getLangManager() {
        return langManager;
    }
    @Override
    public void setLangManager(ILangManager langManager) {
        this.langManager = langManager;
    }

    public ITreeNodeManager getTreeNodeManager() {
        return treeNodeManager;
    }
    @Override
    public void setTreeNodeManager(ITreeNodeManager treeNodeManager) {
        this.treeNodeManager = treeNodeManager;
    }

}
