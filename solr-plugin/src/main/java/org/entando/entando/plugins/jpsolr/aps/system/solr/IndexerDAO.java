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

import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AbstractComplexAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.DateAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.NumberAttribute;
import com.agiletec.aps.system.common.searchengine.IndexableAttributeInterface;
import com.agiletec.aps.system.common.tree.ITreeNode;
import com.agiletec.aps.system.common.tree.ITreeNodeManager;
import com.agiletec.aps.system.services.category.Category;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.ResourceAttributeInterface;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.IIndexerDAO;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields;

/**
 * Data Access Object dedita alla indicizzazione di documenti.
 */
public class IndexerDAO implements IIndexerDAO {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(IndexerDAO.class);

    private ILangManager langManager;

    private ITreeNodeManager treeNodeManager;

    private final SolrClient solrClient;
    private final String solrCore;

    public IndexerDAO(SolrClient solrClient, String solrCore) {
        this.solrClient = solrClient;
        this.solrCore = solrCore;
    }

    @Override
    public void init(File dir) throws EntException {
        // nothing to do
    }

    @Override
    public void close() {
        // nothing to do
    }

    @Override
    public synchronized void add(IApsEntity entity) throws EntException {
        try {
            SolrInputDocument document = this.createDocument(entity);
            UpdateResponse updateResponse = this.solrClient.add(this.solrCore, document);
            logger.debug("Add document Response {}", updateResponse.toString());
            this.solrClient.commit(this.solrCore);
        } catch (IOException | SolrServerException ex) {
            logger.error("Error saving entity {} calling solr server", entity.getId(), ex);
            throw new EntException("Error saving entity", ex);
        } catch (Exception t) {
           logger.error("Generic error saving entity {}", entity.getId(), t);
            throw new EntException("Error saving entity", t);
        }
    }

    public void addBulk(Stream<IApsEntity> entityStream) throws EntException {
        try {
            entityStream.forEach(entity -> {
                try {
                    SolrInputDocument document = this.createDocument(entity);
                    UpdateResponse updateResponse = this.solrClient.add(this.solrCore, document);
                    logger.debug("Add document Response {}", updateResponse.toString());
                } catch (IOException | SolrServerException ex) {
                    logger.error("Error saving entity {} calling solr server", entity.getId(), ex);
                }
            });
            this.solrClient.commit(this.solrCore);
        } catch (IOException | SolrServerException ex) {
            throw new EntException("Error saving entities", ex);
        } catch (Exception t) {
            logger.error("Generic error saving entities", t);
            throw new EntException("Error saving entities", t);
        }
    }

    protected SolrInputDocument createDocument(IApsEntity entity) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(SolrFields.SOLR_CONTENT_ID_FIELD_NAME, entity.getId());
        document.addField(SolrFields.SOLR_CONTENT_TYPE_CODE_FIELD_NAME, entity.getTypeCode());
        document.addField(SolrFields.SOLR_CONTENT_MAIN_GROUP_FIELD_NAME, entity.getMainGroup());
        document.addField(SolrFields.SOLR_CONTENT_GROUP_FIELD_NAME, entity.getMainGroup());
        Iterator<String> iterGroups = entity.getGroups().iterator();
        while (iterGroups.hasNext()) {
            String groupName = iterGroups.next();
            document.addField(SolrFields.SOLR_CONTENT_GROUP_FIELD_NAME, groupName);
        }
        if (entity instanceof Content) {
            if (null != entity.getDescription()) {
                document.addField(SolrFields.SOLR_CONTENT_DESCRIPTION_FIELD_NAME, entity.getDescription());
            }
            Date creation = ((Content) entity).getCreated();
            Date lastModify = (null != ((Content) entity).getLastModified()) ? ((Content) entity).getLastModified() : creation;
            if (null != creation) {
                document.addField(SolrFields.SOLR_CONTENT_CREATION_FIELD_NAME, creation);
            }
            if (null != lastModify) {
                document.addField(SolrFields.SOLR_CONTENT_LAST_MODIFY_FIELD_NAME, lastModify);
            }
        }
        List<AttributeInterface> attributes = entity.getAttributeList();
        for (int j = 0; j < attributes.size(); j++) {
            AttributeInterface currentAttribute = attributes.get(j);
            Object value = currentAttribute.getValue();
            if (null == value) {
                continue;
            }
            List<Lang> langs = this.getLangManager().getLangs();
            for (int i = 0; i < langs.size(); i++) {
                Lang currentLang = langs.get(i);
                this.indexAttribute(document, currentAttribute, currentLang);
            }
        }
        this.indexCategories(entity, document);
        return document;
    }
    
    protected void indexCategories(IApsEntity entity, SolrInputDocument document) {
        List<Category> categories = ((Content) entity).getCategories();
        if (null != categories && !categories.isEmpty()) {
            Set<String> codes = new HashSet<>();
            for (int i = 0; i < categories.size(); i++) {
                ITreeNode category = categories.get(i);
                this.extractCategoryCodes(category, codes);
            }
            codes.stream().forEach(c -> document.addField(SolrFields.SOLR_CONTENT_CATEGORY_FIELD_NAME, c));
        }
    }
    
    protected void extractCategoryCodes(ITreeNode category, Set<String> codes) {
        if (null == category || category.isRoot()) {
            return;
        }
        codes.add(category.getCode());
        ITreeNode parentCategory = this.getTreeNodeManager().getNode(category.getParentCode());
        this.extractCategoryCodes(parentCategory, codes);
    }

    protected void indexAttribute(SolrInputDocument document, AttributeInterface attribute, Lang lang) {
        attribute.setRenderingLang(lang.getCode());
        if (!attribute.isSimple()) {
            this.indexComplexAttribute(document, (AbstractComplexAttribute) attribute, lang);
            return;
        }
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
                this.addFieldForFullTextSearch(document, attribute, lang, valueToIndex);
            }
            if (attribute instanceof ResourceAttributeInterface) {
                return;
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

    private void indexComplexAttribute(SolrInputDocument document, AbstractComplexAttribute complexAttribute, Lang lang) {
        List<AttributeInterface> elements = complexAttribute.getAttributes();
        for (int i = 0; i < elements.size(); i++) {
            AttributeInterface attribute = elements.get(i);
            attribute.setRenderingLang(lang.getCode());
            if (!attribute.isSimple()) {
                this.indexComplexAttribute(document, (AbstractComplexAttribute) attribute, lang);
            } else if (attribute instanceof ResourceAttributeInterface) {
                String valueToIndex = ((IndexableAttributeInterface) attribute).getIndexeableFieldValue();
                this.addFieldForFullTextSearch(document, attribute, lang, valueToIndex);
            }
        }
    }

    protected void addFieldForFullTextSearch(SolrInputDocument document, AttributeInterface attribute, Lang lang, Object valueToIndex) {
        // full text search
        String fieldName = lang.getCode();
        if (attribute instanceof ResourceAttributeInterface) {
            fieldName += SolrFields.ATTACHMENT_FIELD_SUFFIX;
        }
        String indexingType = attribute.getIndexingType();
        if (null != indexingType
                && (IndexableAttributeInterface.INDEXING_TYPE_UNSTORED.equalsIgnoreCase(indexingType) || IndexableAttributeInterface.INDEXING_TYPE_TEXT.equalsIgnoreCase(indexingType))) {
            document.addField(fieldName, valueToIndex);
        }
    }
    
    private void indexValue(SolrInputDocument document, String fieldName, Object valueToIndex) {
        fieldName = fieldName.replaceAll(":", "_");
        document.addField(fieldName, valueToIndex);
    }
    
    @Override
    public synchronized void delete(String name, String value) throws EntException {
        try {
            UpdateResponse updateResponse = (name.equals(SolrFields.SOLR_CONTENT_ID_FIELD_NAME)) ?
                    this.solrClient.deleteById(this.solrCore, value) :
                    this.solrClient.deleteByQuery(this.solrCore, name + ":" + value);
            logger.debug("Delete document Response {}", updateResponse.toString());
            this.solrClient.commit(this.solrCore);
        } catch (IOException | SolrServerException ex) {
            logger.error("Error deleting entity {}:{} calling solr server", name, value, ex);
            throw new EntException("Error deleting entity", ex);
        } catch (Exception t) {
           logger.error("Generic error deleting entity {}:{}", name, value, t);
            throw new EntException("Error deleting entity", t);
        }
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
