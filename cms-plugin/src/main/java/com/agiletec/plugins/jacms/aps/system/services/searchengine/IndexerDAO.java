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
package com.agiletec.plugins.jacms.aps.system.services.searchengine;

import com.agiletec.aps.system.common.entity.model.*;
import com.agiletec.aps.system.common.entity.model.attribute.AbstractComplexAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.DateAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.NumberAttribute;
import com.agiletec.aps.system.common.searchengine.IndexableAttributeInterface;
import org.entando.entando.aps.system.common.entity.search.SearchFieldType;
import com.agiletec.aps.system.common.tree.ITreeNode;
import com.agiletec.aps.system.common.tree.ITreeNodeManager;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.category.Category;
import com.agiletec.aps.system.services.lang.*;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.util.BytesRef;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;

/**
 * Data Access Object dedita alla indicizzazione di documenti.
 */
public class IndexerDAO implements IIndexerDAO {

    private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(IndexerDAO.class);

    private Directory dir;

    private ILangManager langManager;

    private ITreeNodeManager treeNodeManager;

    /**
     * Inizializzazione dell'indicizzatore.
     *
     * @param dir La cartella locale contenitore dei dati persistenti.
     * @throws EntException In caso di errore
     */
    @Override
    public void init(File dir) throws EntException {
        try {
            this.dir = FSDirectory.open(dir.toPath(), SimpleFSLockFactory.INSTANCE);
        } catch (Throwable t) {
            _logger.error("Error creating directory", t);
            throw new EntException("Error creating directory", t);
        }
        _logger.debug("Indexer: search engine index ok.");
    }

    @Override
    public synchronized void add(IApsEntity entity) throws EntException {
        IndexWriter writer = null;
        try {
            writer = new IndexWriter(this.dir, this.getIndexWriterConfig());
            Document document = this.createDocument(entity);
            writer.addDocument(document);
        } catch (Throwable t) {
            _logger.error("Errore saving entity {}", entity.getId(), t);
            throw new EntException("Error saving entity", t);
        } finally {
            if (null != writer) {
                try {
                    writer.close();
                } catch (IOException ex) {
                    _logger.error("Error closing IndexWriter", ex);
                }
            }
        }
    }

    protected Document createDocument(IApsEntity entity) throws EntException {
        Document document = new Document();
        document.add(new StringField(CONTENT_ID_FIELD_NAME,
                entity.getId(), Field.Store.YES));
        document.add(new TextField(CONTENT_TYPE_FIELD_NAME,
                entity.getTypeCode(), Field.Store.YES));
        document.add(new StringField(CONTENT_GROUP_FIELD_NAME,
                entity.getMainGroup(), Field.Store.YES));
        Iterator<String> iterGroups = entity.getGroups().iterator();
        while (iterGroups.hasNext()) {
            String groupName = (String) iterGroups.next();
            document.add(new StringField(CONTENT_GROUP_FIELD_NAME,
                    groupName, Field.Store.YES));
        }
        if (entity instanceof Content) {
            if (null != entity.getDescription()) {
                document.add(new SortedDocValuesField(CONTENT_DESCRIPTION_FIELD_NAME + SORTERED_FIELD_SUFFIX, new BytesRef(entity.getDescription())));
                document.add(new TextField(CONTENT_DESCRIPTION_FIELD_NAME, entity.getDescription(), Field.Store.YES));
            }
            document.add(new TextField(CONTENT_TYPE_CODE_FIELD_NAME, entity.getTypeCode(), Field.Store.YES));
            document.add(new TextField(CONTENT_MAIN_GROUP_FIELD_NAME, entity.getMainGroup(), Field.Store.YES));
            Date creation = ((Content) entity).getCreated();
            Date lastModify = (null != ((Content) entity).getLastModified()) ? ((Content) entity).getLastModified() : creation;
            if (null != creation) {
                String value = DateTools.timeToString(creation.getTime(), DateTools.Resolution.MINUTE);
                document.add(new SortedDocValuesField(CONTENT_CREATION_FIELD_NAME + SORTERED_FIELD_SUFFIX, new BytesRef(value)));
                document.add(new TextField(CONTENT_CREATION_FIELD_NAME, value.toLowerCase(), Field.Store.YES));
            }
            if (null != lastModify) {
                String value = DateTools.timeToString(lastModify.getTime(), DateTools.Resolution.MINUTE);
                document.add(new SortedDocValuesField(CONTENT_LAST_MODIFY_FIELD_NAME + SORTERED_FIELD_SUFFIX, new BytesRef(value)));
                document.add(new TextField(CONTENT_LAST_MODIFY_FIELD_NAME, value.toLowerCase(), Field.Store.YES));
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
                this.scanComplexAttribute(document, currentAttribute, currentLang, true);
            }
        }
        List<Category> categories = ((Content) entity).getCategories();
        if (null != categories && !categories.isEmpty()) {
            for (int i = 0; i < categories.size(); i++) {
                ITreeNode category = categories.get(i);
                this.indexCategory(document, category);
            }
        }
        return document;
    }

    protected void indexAttribute(Document document, AttributeInterface attribute, Lang lang) {
        attribute.setRenderingLang(lang.getCode());
        // Only attributes that carry indexable content get a Lucene field. The condition used to
        // also admit a searchable Date or Number attribute, which could never widen it: both types
        // implement IndexableAttributeInterface themselves, so that clause was dropped as dead.
        //
        // DELIBERATE DIVERGENCE from the Solr engine: boolean-like attributes (Boolean, CheckBox,
        // ThreeState) are NOT indexed here, and must not be. On this engine attribute filtering is
        // served by the DB search tables (contentsearch/workcontentsearch), which is where a boolean's
        // searchable flag writes its row; Lucene only provides full text. Switching this test to the
        // engine-wide attribute.hasSearchField() would therefore start writing boolean fields into
        // every Lucene document - a change of index contents, not a refactor.
        if (attribute instanceof IndexableAttributeInterface) {
            Object[] values = this.extractValuesToIndex(attribute);
            if (null == values[0]) {
                return;
            }
            String valueToIndex = (String) values[0];
            Long number = (Long) values[1];
            if (attribute instanceof IndexableAttributeInterface) {
                // full text search
                String indexingType = attribute.getIndexingType();
                if (null != indexingType
                        && IndexableAttributeInterface.INDEXING_TYPE_UNSTORED.equalsIgnoreCase(indexingType)) {
                    document.add(new TextField(lang.getCode(), valueToIndex, Field.Store.NO));
                }
                if (null != indexingType
                        && IndexableAttributeInterface.INDEXING_TYPE_TEXT.equalsIgnoreCase(indexingType)) {
                    document.add(new TextField(lang.getCode(), valueToIndex, Field.Store.YES));
                }
            }
            boolean isDate = (SearchFieldType.DATE == attribute.getSearchFieldType());
            String fieldName = lang.getCode().toLowerCase() + "_" + attribute.getName();
            this.indexValue(document, fieldName, valueToIndex, number, isDate);
            if (null == attribute.getRoles()) {
                return;
            }
            for (int i = 0; i < attribute.getRoles().length; i++) {
                String roleFieldName = lang.getCode().toLowerCase() + "_" + attribute.getRoles()[i];
                this.indexValue(document, roleFieldName, valueToIndex, number, isDate);
            }
        }
    }
    
    protected void scanComplexAttribute(Document document, AttributeInterface attribute, Lang lang, boolean first) {
        attribute.setRenderingLang(lang.getCode());
        if (!first && attribute.isSimple()) {
            String indexingType = attribute.getIndexingType();
            boolean toIndex = ((attribute instanceof IndexableAttributeInterface) && 
                    null != indexingType && !IndexableAttributeInterface.INDEXING_TYPE_NONE.equals(indexingType)); 
            if (toIndex) {
                Object[] values = this.extractValuesToIndex(attribute);
                if (null == values[0]) {
                    return;
                }
                String valueToIndex = (String) values[0];
                Store store = (IndexableAttributeInterface.INDEXING_TYPE_TEXT.equalsIgnoreCase(indexingType)) ? Field.Store.YES : Field.Store.NO;
                document.add(new TextField(lang.getCode(), valueToIndex, store));
            }
        } else if (!attribute.isSimple()) {
            AbstractComplexAttribute complex = (AbstractComplexAttribute) attribute;
            List<AttributeInterface> list = complex.getAttributes();
            for (int i = 0; i < list.size(); i++) {
                this.scanComplexAttribute(document, list.get(i), lang, false);
            }
        }
    }
    
    /**
     * The pair this engine needs for one attribute: the string to index, and its numeric form when the
     * attribute has one (used for range queries and numeric sorting).
     *
     * <p>The dispatch is on the attribute's <b>declared</b> {@link SearchFieldType}, narrowed by a
     * pattern match on the class that supplies the value. A type declaring {@code DATE} or
     * {@code NUMBER} without extending {@code DateAttribute}/{@code NumberAttribute} is indexed as text
     * rather than failing: the declaration says how the value should be queried, not where it is read
     * from.</p>
     *
     * <p>The <i>formatting</i> stays this engine's own, deliberately. Lucene needs a minute-resolution
     * date string and a {@code long} for the numeric field, whereas
     * {@link AttributeInterface#getSearchFieldValue()} hands back the shape the per-attribute field
     * takes ({@code Date}, {@code Integer}), which is not what these two Lucene fields want - and whose
     * {@code int} narrowing would truncate a number that does not fit 32 bits.</p>
     */
    protected Object[] extractValuesToIndex(AttributeInterface attribute) {
        Object[] values = new Object[2];
        String valueToIndex = null;
        Long number = null;
        SearchFieldType searchFieldType = attribute.getSearchFieldType();
        if (SearchFieldType.DATE == searchFieldType && attribute instanceof DateAttribute dateAttribute) {
            Date date = dateAttribute.getDate();
            number = (null != date) ? date.getTime() : null;
            valueToIndex = (null != number) ? DateTools.timeToString(number, DateTools.Resolution.MINUTE) : valueToIndex;
        } else if (SearchFieldType.NUMBER == searchFieldType
                && attribute instanceof NumberAttribute numberAttribute) {
            BigDecimal value = numberAttribute.getValue();
            number = (null != value) ? value.longValue() : null;
            valueToIndex = (null != number) ? String.valueOf(number) : valueToIndex;
        } else {
            valueToIndex = ((IndexableAttributeInterface) attribute).getIndexeableFieldValue();
        }
        values[0] = valueToIndex;
        values[1] = number;
        return values;
    }
    
    private void indexValue(Document document, String fieldName, String valueToIndex, Long number, boolean isDate) {
        document.add(new TextField(fieldName, valueToIndex.toLowerCase(), Field.Store.YES));
        boolean sortString = true;
        if (null != number) {
            document.add(new LongPoint(fieldName, number));
            if (!isDate) {
                document.add(new SortedNumericDocValuesField(fieldName + SORTERED_FIELD_SUFFIX, number));
                sortString = false;
            }
        }
        if (sortString) {
            String sortableValue = (valueToIndex.length() > 100) ? valueToIndex.substring(0, 99) : valueToIndex;
            document.add(new SortedDocValuesField(fieldName + SORTERED_FIELD_SUFFIX, new BytesRef(sortableValue)));
        }
    }

    protected void indexCategory(Document document, ITreeNode categoryToIndex) {
        if (null == categoryToIndex || categoryToIndex.isRoot()) {
            return;
        }
        document.add(new StringField(CONTENT_CATEGORY_FIELD_NAME,
                categoryToIndex.getPath(CONTENT_CATEGORY_SEPARATOR, false, this.getTreeNodeManager()), Field.Store.YES));
        ITreeNode parentCategory = this.getTreeNodeManager().getNode(categoryToIndex.getParentCode());
        this.indexCategory(document, parentCategory);
    }

    /**
     * Cancella un documento.
     *
     * @param name Il nome del campo Field da utilizzare per recupero del documento.
     * @param value La chiave mediante il quale è stato indicizzato il documento.
     * @throws EntException In caso di errore
     */
    @Override
    public synchronized void delete(String name, String value) throws EntException {
        try {
            IndexWriter writer = new IndexWriter(this.dir, this.getIndexWriterConfig());
            writer.deleteDocuments(new Term(name, value));
            writer.close();
        } catch (IOException e) {
            _logger.error("Error deleting document", e);
            throw new EntException("Error deleting document", e);
        }
    }

    @Override
    public void close() {
        // nothing to do
    }

    private Analyzer getAnalyzer() {
        return new StandardAnalyzer();
    }

    private IndexWriterConfig getIndexWriterConfig() {
        return new IndexWriterConfig(this.getAnalyzer());
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
