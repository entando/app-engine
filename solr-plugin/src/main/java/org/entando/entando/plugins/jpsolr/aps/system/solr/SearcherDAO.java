/*
 * Copyright 2020-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

import com.agiletec.aps.system.common.tree.ITreeNode;
import com.agiletec.aps.system.common.tree.ITreeNodeManager;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.util.DateConverter;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ISearcherDAO;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.NumericSearchEngineFilter;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.entando.entando.aps.system.services.searchengine.*;

import java.io.*;
import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;

/**
 * Data Access Object dedita alle operazioni di ricerca ad uso del motore di ricerca interno.
 *
 * @author E.Santoboni
 */
public class SearcherDAO implements ISearcherDAO {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(SearcherDAO.class);

    private String solrAddress;

    private String solrCore;

    private ITreeNodeManager treeNodeManager;
    private ILangManager langManager;
    
    private SolrClient getSolrClient() {
        return new HttpSolrClient.Builder(this.solrAddress)
                .withConnectionTimeout(10000)
                .withSocketTimeout(60000)
                .build();
    }

    @Override
    public void init(File dir) throws EntException {
        // nothing to do
    }
    
    @Override
    public List<String> searchContentsId(SearchEngineFilter[] filters,
            SearchEngineFilter[] categories, Collection<String> allowedGroups) throws EntException {
        return this.searchContents(filters, categories, allowedGroups, false).getContentsId();
    }

    @Override
    public FacetedContentsResult searchFacetedContents(SearchEngineFilter[] filters,
            SearchEngineFilter[] categories, Collection<String> allowedGroups) throws EntException {
        return this.searchContents(filters, categories, allowedGroups, true);
    }

    protected FacetedContentsResult searchContents(SearchEngineFilter[] filters,
            SearchEngineFilter[] categories, Collection<String> allowedGroups, boolean faceted) throws EntException {
        FacetedContentsResult result = new FacetedContentsResult();
        List<String> contentsId = new ArrayList<>();
        SolrClient client = this.getSolrClient();
        try {
            Query query = null;
            if ((null == filters || filters.length == 0)
                    && (null == categories || categories.length == 0)
                    && (allowedGroups != null && allowedGroups.contains(Group.ADMINS_GROUP_NAME))) {
                query = new MatchAllDocsQuery();
            } else {
                query = this.createQuery(filters, categories, allowedGroups);
            }
            SolrQuery solrQuery = new SolrQuery(query.toString());
            solrQuery.addField(SolrFields.SOLR_CONTENT_ID_FIELD_NAME);
            if (faceted) {
                solrQuery.addField(SolrFields.SOLR_CONTENT_CATEGORY_FIELD_NAME);
            }
            solrQuery.setRows(10000);
            if (null != filters) {
                for (int i = 0; i < filters.length; i++) {
                    SearchEngineFilter filter = filters[i];
                    if (null != filter.getOrder()) {
                        String fieldKey = this.getFilterKey(filter);
                        boolean revert = filter.getOrder().toString().equalsIgnoreCase("DESC");
                        solrQuery.addSort(fieldKey, (revert) ? ORDER.desc: ORDER.asc);
                    }
                }
            }
            QueryResponse response = client.query(this.getSolrCore(), solrQuery);
            SolrDocumentList documents = response.getResults();
            Map<String, Integer> occurrences = new HashMap<>();
            for (SolrDocument doc : documents) {
                String id = doc.get(SolrFields.SOLR_CONTENT_ID_FIELD_NAME).toString();
                contentsId.add(id);
                if (faceted) {
                    List<Object> categoryPaths = (List<Object>) doc.get(SolrFields.SOLR_CONTENT_CATEGORY_FIELD_NAME);
                    if (null != categoryPaths) {
                        Set<String> codes = new HashSet<>();
                        for (int i = 0; i < categoryPaths.size(); i++) {
                            String categoryPath = categoryPaths.get(i).toString();
                            String[] paths = categoryPath.split(SolrFields.SOLR_CONTENT_CATEGORY_SEPARATOR);
                            codes.addAll(Arrays.asList(paths));
                        }
                        Iterator<String> iter = codes.iterator();
                        while (iter.hasNext()) {
                            String code = iter.next();
                            Integer value = occurrences.get(code);
                            if (null == value) {
                                value = 0;
                            }
                            occurrences.put(code, (value + 1));
                        }
                    }
                }
            }
            result.setOccurrences(occurrences);
            result.setContentsId(contentsId);
        } catch (IndexNotFoundException inf) {
            logger.error("no index was found in the Directory", inf);
        } catch (Throwable t) {
            logger.error("Error extracting documents", t);
            throw new EntException("Error extracting documents", t);
        } finally {
            if (null != client) {
                try {
                    client.close();
                } catch (IOException ex) {
                    throw new EntException("Error closing client", ex);
                }
            }
        }
        return result;
    }
    
    protected Query createQuery(SearchEngineFilter[] filters,
            SearchEngineFilter[] categories, Collection<String> allowedGroups) {
        BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();
        if (filters != null && filters.length > 0) {
            for (int i = 0; i < filters.length; i++) {
                SearchEngineFilter filter = filters[i];
                Query fieldQuery = this.createQuery(filter);
                if (null != fieldQuery) {
                    mainQuery.add(fieldQuery, BooleanClause.Occur.MUST);
                }
            }
        }
        if (allowedGroups == null) {
            allowedGroups = new HashSet<>();
        }
        if (!allowedGroups.contains(Group.ADMINS_GROUP_NAME)) {
            if (!allowedGroups.contains(Group.FREE_GROUP_NAME)) {
                allowedGroups.add(Group.FREE_GROUP_NAME);
            }
            BooleanQuery.Builder groupsQuery = new BooleanQuery.Builder();
            Iterator<String> iterGroups = allowedGroups.iterator();
            while (iterGroups.hasNext()) {
                String group = iterGroups.next();
                TermQuery groupQuery = new TermQuery(new Term(SolrFields.SOLR_CONTENT_GROUP_FIELD_NAME, group));
                groupsQuery.add(groupQuery, BooleanClause.Occur.SHOULD);
            }
            mainQuery.add(groupsQuery.build(), BooleanClause.Occur.MUST);
        }
        if (null != categories && categories.length > 0) {
            BooleanQuery.Builder categoriesQuery = new BooleanQuery.Builder();
            for (int i = 0; i < categories.length; i++) {
                SearchEngineFilter categoryFilter = categories[i];
                List<String> allowedValues = categoryFilter.getAllowedValues();
                if (null != allowedValues && allowedValues.size() > 0) {
                    BooleanQuery.Builder singleCategoriesQuery = new BooleanQuery.Builder();
                    for (int j = 0; j < allowedValues.size(); j++) {
                        String singleCategory = allowedValues.get(j);
                        ITreeNode treeNode = this.getTreeNodeManager().getNode(singleCategory);
                        if (null != treeNode) {
                            String path = treeNode.getPath(SolrFields.SOLR_CONTENT_CATEGORY_SEPARATOR, false, this.getTreeNodeManager());
                            TermQuery categoryQuery = new TermQuery(new Term(SolrFields.SOLR_CONTENT_CATEGORY_FIELD_NAME, path));
                            singleCategoriesQuery.add(categoryQuery, BooleanClause.Occur.SHOULD);
                        }
                    }
                    categoriesQuery.add(singleCategoriesQuery.build(), BooleanClause.Occur.MUST);
                } else if (null != categoryFilter.getValue()) {
                    ITreeNode treeNode = this.getTreeNodeManager().getNode(categoryFilter.getValue().toString());
                    if (null != treeNode) {
                        String path = treeNode.getPath(SolrFields.SOLR_CONTENT_CATEGORY_SEPARATOR, false, this.getTreeNodeManager());
                        TermQuery categoryQuery = new TermQuery(new Term(SolrFields.SOLR_CONTENT_CATEGORY_FIELD_NAME, path));
                        categoriesQuery.add(categoryQuery, BooleanClause.Occur.MUST);
                    }
                }
            }
            mainQuery.add(categoriesQuery.build(), BooleanClause.Occur.MUST);
        }
        return mainQuery.build();
    }

    private Query createQuery(SearchEngineFilter filter) {
        BooleanQuery.Builder fieldQuery = null;
        String key = this.getFilterKey(filter);
        Object value = filter.getValue();
        List<?> allowedValues = filter.getAllowedValues();
        if (null != allowedValues && !allowedValues.isEmpty()) {
            fieldQuery = new BooleanQuery.Builder();
            SearchEngineFilter.TextSearchOption option = filter.getTextSearchOption();
            if (null == option) {
                option = SearchEngineFilter.TextSearchOption.AT_LEAST_ONE_WORD;
            }
            //To be improved to manage different type
            for (int j = 0; j < allowedValues.size(); j++) {
                String singleValue = allowedValues.get(j).toString();
                if (filter instanceof NumericSearchEngineFilter) {
                    TermQuery term = new TermQuery(new Term(key, singleValue));
                    fieldQuery.add(term, BooleanClause.Occur.SHOULD);
                } else {
                    //NOTE: search for lower case....
                    String[] values = singleValue.split("\\s+");
                    if (!option.equals(SearchEngineFilter.TextSearchOption.EXACT)) {
                        BooleanQuery.Builder singleOptionFieldQuery = new BooleanQuery.Builder();
                        BooleanClause.Occur bc = BooleanClause.Occur.SHOULD;
                        if (option.equals(SearchEngineFilter.TextSearchOption.ALL_WORDS)) {
                            bc = BooleanClause.Occur.MUST;
                        } else if (option.equals(SearchEngineFilter.TextSearchOption.ANY_WORD)) {
                            bc = BooleanClause.Occur.MUST_NOT;
                        }
                        for (int i = 0; i < values.length; i++) {
                            Query queryTerm = this.getTermQueryForTextSearch(key, values[i], filter.isLikeOption());
                            singleOptionFieldQuery.add(queryTerm, bc);
                        }
                        fieldQuery.add(singleOptionFieldQuery.build(), BooleanClause.Occur.SHOULD);
                    } else {
                        PhraseQuery.Builder phraseQuery = new PhraseQuery.Builder();
                        for (int i = 0; i < values.length; i++) {
                            phraseQuery.add(new Term(key, values[i].toLowerCase()), i);
                        }
                        fieldQuery.add(phraseQuery.build(), BooleanClause.Occur.SHOULD);
                    }
                }
            }
        } else if (null != filter.getStart() || null != filter.getEnd()) {
            fieldQuery = new BooleanQuery.Builder();
            Query query = null;
            if (filter.getStart() instanceof Date || filter.getEnd() instanceof Date) {
                String format = SolrFields.SOLR_SEARCH_DATE_FORMAT;
                String start = (null != filter.getStart()) ? DateConverter.getFormattedDate((Date) filter.getStart(), format) : SolrFields.SOLR_DATE_MIN;
                String end = (null != filter.getEnd()) ? DateConverter.getFormattedDate((Date) filter.getEnd(), format) : SolrFields.SOLR_DATE_MAX;
                query = TermRangeQuery.newStringRange(key, start, end, false, false);
            } else if (filter.getStart() instanceof Number || filter.getEnd() instanceof Number) {
                Long lowerValue = (null != filter.getStart()) ? ((Number) filter.getStart()).longValue() : Long.MIN_VALUE;
                Long upperValue = (null != filter.getEnd()) ? ((Number) filter.getEnd()).longValue() : Long.MAX_VALUE;
                query = LongPoint.newRangeQuery(key, lowerValue, upperValue);
            } else {
                String start = (null != filter.getStart()) ? filter.getStart().toString().toLowerCase() : "A";
                String end = (null != filter.getEnd()) ? filter.getEnd().toString().toLowerCase() + "z" : null;
                query = TermRangeQuery.newStringRange(key, start, end, true, true);
            }
            fieldQuery.add(query, BooleanClause.Occur.MUST);
        } else if (null != value) {
            fieldQuery = new BooleanQuery.Builder();
            if (value instanceof String) {
                //NOTE: search for lower case....
                SearchEngineFilter.TextSearchOption option = filter.getTextSearchOption();
                if (null == option) {
                    option = SearchEngineFilter.TextSearchOption.AT_LEAST_ONE_WORD;
                }
                String stringValue = value.toString();
                String[] values = stringValue.split("\\s+");
                if (!option.equals(SearchEngineFilter.TextSearchOption.EXACT)) {
                    BooleanClause.Occur bc = BooleanClause.Occur.SHOULD;
                    if (option.equals(SearchEngineFilter.TextSearchOption.ALL_WORDS)) {
                        bc = BooleanClause.Occur.MUST;
                    } else if (option.equals(SearchEngineFilter.TextSearchOption.ANY_WORD)) {
                        bc = BooleanClause.Occur.MUST_NOT;
                    }
                    for (int i = 0; i < values.length; i++) {
                        Query queryTerm = this.getTermQueryForTextSearch(key, values[i], filter.isLikeOption());
                        fieldQuery.add(queryTerm, bc);
                    }
                } else {
                    PhraseQuery.Builder phraseQuery = new PhraseQuery.Builder();
                    for (int i = 0; i < values.length; i++) {
                        phraseQuery.add(new Term(key, values[i].toLowerCase()), i);
                    }
                    return phraseQuery.build();
                }
            } else if (value instanceof Date) {
                String toString = DateConverter.getFormattedDate((Date) value, SolrFields.SOLR_SEARCH_DATE_FORMAT);
                TermQuery term = new TermQuery(new Term(key, toString));
                fieldQuery.add(term, BooleanClause.Occur.MUST);
            } else if (value instanceof Number) {
                TermQuery term = new TermQuery(new Term(key, value.toString()));
                fieldQuery.add(term, BooleanClause.Occur.MUST);
            }
        } else {
            fieldQuery = new BooleanQuery.Builder();
            Term term = new Term(key, "*");
            Query queryTerm = new WildcardQuery(term);
            fieldQuery.add(queryTerm, BooleanClause.Occur.MUST);
        }
        return fieldQuery.build();
    }

    protected Query getTermQueryForTextSearch(String key, String value, boolean isLikeSearch) {
        //NOTE: search for lower case....
        String stringValue = value.toLowerCase();
        boolean useWildCard = false;
        if (value.startsWith("*") || value.endsWith("*")) {
            useWildCard = true;
        } else if (isLikeSearch) {
            stringValue = "*" + stringValue + "*";
            useWildCard = true;
        }
        Term term = new Term(key, stringValue);
        return (useWildCard) ? new WildcardQuery(term) : new TermQuery(term);
    }

    protected String getFilterKey(SearchEngineFilter filter) {
        String key = filter.getKey().replaceAll(":", "_");
        if (filter.isFullTextSearch()) {
            return key;
        }
        if (!filter.isAttributeFilter()
                && !(key.startsWith(SolrFields.SOLR_FIELD_PREFIX))) {
            key = SolrFields.SOLR_FIELD_PREFIX + key;
        } else if (filter.isAttributeFilter()) {
            String insertedLangCode = filter.getLangCode();
            String langCode = (StringUtils.isBlank(insertedLangCode)) ? this.getLangManager().getDefaultLang().getCode() : insertedLangCode;
            key = langCode.toLowerCase() + "_" + key;
        }
        return key;
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

    public ITreeNodeManager getTreeNodeManager() {
        return treeNodeManager;
    }

    @Override
    public void setTreeNodeManager(ITreeNodeManager treeNodeManager) {
        this.treeNodeManager = treeNodeManager;
    }

    protected ILangManager getLangManager() {
        return langManager;
    }

    @Override
    public void setLangManager(ILangManager langManager) {
        this.langManager = langManager;
    }

}