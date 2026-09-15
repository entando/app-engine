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

import org.entando.entando.aps.system.common.entity.search.NestedSearchSupport;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AbstractComplexAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.searchengine.IndexableAttributeInterface;
import org.entando.entando.aps.system.common.entity.search.SearchFieldType;
import com.agiletec.aps.system.common.tree.ITreeNode;
import com.agiletec.aps.system.common.tree.ITreeNodeManager;
import com.agiletec.aps.system.services.category.Category;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.ResourceAttributeInterface;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data Access Object dedita alla indicizzazione di documenti.
 */
// NOTE: java:S2143 ("use the java.time API") is intentionally suppressed. This class uses legacy
// java.util.Date to interoperate with the content date model (getCreated/getLastModified); migrating
// the date stack to java.time is out of scope for ESB-1133 (boolean search) and tracked separately.
@SuppressWarnings("java:S2143")
public class IndexerDAO implements ISolrIndexerDAO {

    private static final Logger logger = LoggerFactory.getLogger(IndexerDAO.class);

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
            logger.debug("Add document Response {}", updateResponse);
            this.solrClient.commit(this.solrCore);
        } catch (IOException | SolrServerException ex) {
            logger.error("Error saving entity {} calling solr server", entity.getId());
            throw new EntException("Error saving entity", ex);
        } catch (Exception ex) {
            logger.error("Generic error saving entity {}", entity.getId());
            throw new EntException("Error saving entity", ex);
        }
    }

    @Override
    public void addBulk(Stream<IApsEntity> entityStream) throws EntException {
        try {
            entityStream.forEach(entity -> {
                try {
                    SolrInputDocument document = this.createDocument(entity);
                    UpdateResponse updateResponse = this.solrClient.add(this.solrCore, document);
                    logger.debug("Add document Response {}", updateResponse);
                } catch (IOException | SolrServerException ex) {
                    logger.error("Error saving entity {} calling solr server", entity.getId(), ex);
                }
            });
            this.solrClient.commit(this.solrCore);
        } catch (IOException | SolrServerException ex) {
            throw new EntException("Error saving entities", ex);
        }
    }

    protected SolrInputDocument createDocument(IApsEntity entity) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(SolrFields.SOLR_CONTENT_ID_FIELD_NAME, entity.getId());
        document.addField(SolrFields.SOLR_CONTENT_TYPE_CODE_FIELD_NAME, entity.getTypeCode());
        document.addField(SolrFields.SOLR_CONTENT_MAIN_GROUP_FIELD_NAME, entity.getMainGroup());
        document.addField(SolrFields.SOLR_CONTENT_GROUP_FIELD_NAME, entity.getMainGroup());
        for (String groupName : entity.getGroups()) {
            document.addField(SolrFields.SOLR_CONTENT_GROUP_FIELD_NAME, groupName);
        }
        this.addContentMetadata(entity, document);
        this.indexAttributes(entity, document);
        this.indexCategories(entity, document);
        return document;
    }

    private void addContentMetadata(IApsEntity entity, SolrInputDocument document) {
        if (!(entity instanceof Content)) {
            return;
        }
        Content content = (Content) entity;
        if (null != entity.getDescription()) {
            document.addField(SolrFields.SOLR_CONTENT_DESCRIPTION_FIELD_NAME, entity.getDescription());
        }
        Date creation = content.getCreated();
        Date lastModify = (null != content.getLastModified()) ? content.getLastModified() : creation;
        if (null != creation) {
            document.addField(SolrFields.SOLR_CONTENT_CREATION_FIELD_NAME, creation);
        }
        if (null != lastModify) {
            document.addField(SolrFields.SOLR_CONTENT_LAST_MODIFY_FIELD_NAME, lastModify);
        }
    }

    private void indexAttributes(IApsEntity entity, SolrInputDocument document) {
        for (AttributeInterface currentAttribute : entity.getAttributeList()) {
            Object value = currentAttribute.getValue();
            // An attribute with no value is skipped - except a three-state one, whose *unset* state is
            // itself a value to index (its getValue() returns null on purpose, unlike a plain Boolean
            // which coerces to false). Tested on the declared field type rather than on the class, so
            // this loop still names no subclass.
            //
            // NB: the test must be on the field TYPE and not on getSearchFieldValue() being non-null.
            // An empty Image/Attach/Link has a null getValue() but a non-null (empty-string)
            // indexable value, so keying off the value would stop skipping it and would add empty
            // entries to the full-text field that this method has never added.
            if (null == value && SearchFieldType.TRISTATE != currentAttribute.getSearchFieldType()) {
                continue;
            }
            for (Lang lang : this.getLangManager().getLangs()) {
                this.indexAttribute(document, currentAttribute, lang);
            }
        }
    }

    protected void indexCategories(IApsEntity entity, SolrInputDocument document) {
        List<Category> categories = ((Content) entity).getCategories();
        if (null != categories && !categories.isEmpty()) {
            Set<String> codes = new HashSet<>();
            for (ITreeNode category : categories) {
                this.extractCategoryCodes(category, codes);
            }
            codes.forEach(c -> document.addField(SolrFields.SOLR_CONTENT_CATEGORY_FIELD_NAME, c));
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
            // Two independent concerns, one pass each: every text descendant feeds the full-text field
            // (lists included), and every path-indexed boolean gets its own field. Which booleans those
            // are, and under which path, is the engine's decision - not this class's.
            this.indexComplexAttributeForFullText(document, (AbstractComplexAttribute) attribute, lang);
            NestedSearchSupport.forEachIndexableNested(attribute, (child, path) -> {
                child.setRenderingLang(lang.getCode());
                // getSearchFieldValue() never returns null for a boolean-like attribute: an unset
                // ThreeStateAttribute yields its "not set" literal, an unset Boolean/CheckBox
                // coerces to false.
                this.indexValue(document, lang.getCode().toLowerCase() + "_" + path,
                        child.getSearchFieldValue());
            });
            return;
        }
        if (attribute.hasSearchField()) {
            // Which attributes have a field, and what goes in it, is the attribute's own answer - not a
            // ladder of type tests repeated here, in the schema checker and in the settings report.
            Object valueToIndex = attribute.getSearchFieldValue();
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
            for (String role : attribute.getRoles()) {
                String roleFieldName = lang.getCode().toLowerCase() + "_" + role;
                this.indexValue(document, roleFieldName, valueToIndex);
            }
        }
    }

    /**
     * Route every text descendant of a complex attribute to the full-text {@code <lang>} field,
     * wherever it occurs - including inside a List/Monolist. No path is needed here: the full-text
     * field is named after the language alone. Per-attribute boolean fields are not this method's
     * business; {@code indexAttribute} asks the engine for those.
     */
    private void indexComplexAttributeForFullText(SolrInputDocument document,
            AbstractComplexAttribute complexAttribute, Lang lang) {
        for (AttributeInterface attribute : complexAttribute.getAttributes()) {
            attribute.setRenderingLang(lang.getCode());
            if (!attribute.isSimple()) {
                this.indexComplexAttributeForFullText(document, (AbstractComplexAttribute) attribute, lang);
            } else if (attribute instanceof IndexableAttributeInterface) {
                String valueToIndex = ((IndexableAttributeInterface) attribute).getIndexeableFieldValue();
                this.addFieldForFullTextSearch(document, attribute, lang, valueToIndex);
            }
        }
    }

    protected void addFieldForFullTextSearch(SolrInputDocument document, AttributeInterface attribute, Lang lang,
            Object valueToIndex) {
        // full text search
        String fieldName = lang.getCode();
        if (attribute instanceof ResourceAttributeInterface) {
            fieldName += SolrFields.ATTACHMENT_FIELD_SUFFIX;
        }
        String indexingType = attribute.getIndexingType();
        if (null != indexingType
                && !IndexableAttributeInterface.INDEXING_TYPE_NONE.equalsIgnoreCase(indexingType)) {
            document.addField(fieldName, valueToIndex);
        }
    }

    private void indexValue(SolrInputDocument document, String fieldName, Object valueToIndex) {
        fieldName = fieldName.replace(":", "_");
        logger.debug("Indexing attribute field '{}' with value '{}'", fieldName, valueToIndex);
        document.addField(fieldName, valueToIndex);
    }

    @Override
    public synchronized void delete(String name, String value) throws EntException {
        try {
            UpdateResponse updateResponse = (name.equals(SolrFields.SOLR_CONTENT_ID_FIELD_NAME)) ?
                    this.solrClient.deleteById(this.solrCore, value) :
                    this.solrClient.deleteByQuery(this.solrCore, name + ":" + value);
            logger.debug("Delete document Response {}", updateResponse);
            this.solrClient.commit(this.solrCore);
        } catch (IOException | SolrServerException ex) {
            logger.error("Error deleting entity {}:{} calling solr server", name, value);
            throw new EntException("Error deleting entity", ex);
        } catch (Exception ex) {
            logger.error("Generic error deleting entity {}:{}", name, value);
            throw new EntException("Error deleting entity", ex);
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

    @Override
    public boolean deleteAllDocuments() {
        try {
            solrClient.deleteByQuery(this.solrCore, "*:*");
            this.solrClient.commit(this.solrCore);
        } catch (IOException | SolrServerException ex) {
            logger.error("Error deleting documents", ex);
            return false;
        }
        return true;
    }
}
