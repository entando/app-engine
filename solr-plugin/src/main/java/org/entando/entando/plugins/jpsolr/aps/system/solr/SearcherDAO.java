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

import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrSearchEngineFilter;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields;
import com.agiletec.aps.system.common.tree.ITreeNode;
import com.agiletec.aps.system.common.tree.ITreeNodeManager;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.util.DateConverter;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.NumericSearchEngineFilter;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.entando.entando.aps.system.services.searchengine.*;

import java.io.*;
import java.util.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;

/**
 * @author E.Santoboni
 */
public class SearcherDAO implements ISolrSearcherDAO {

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

    @Override
    public FacetedContentsResult searchFacetedContents(SearchEngineFilter[][] filters, SearchEngineFilter[] categories, Collection<String> allowedGroups) throws EntException {
        Query query = null;
        SearchEngineFilter[] filterForSorting = new SearchEngineFilter[0];
        if ((null == filters || filters.length == 0)
                && (null == categories || categories.length == 0)
                && (allowedGroups != null && allowedGroups.contains(Group.ADMINS_GROUP_NAME))) {
            query = new MatchAllDocsQuery();
        } else {
            query = this.createDoubleQuery(filters, categories, allowedGroups);
        }
        if (null != filters) {
            for (int i = 0; i < filters.length; i++) {
                SearchEngineFilter[] firstBlock = filters[i];
                for (int j = 0; j < firstBlock.length; j++) {
                    SearchEngineFilter filter = firstBlock[j];
                    if (null != filter.getOrder() || null != this.getRelevance(filter)) {
                        filterForSorting = ArrayUtils.add(filterForSorting, filter);
                    }
                }
            }
        }
        return this.executeQuery(query, filterForSorting, true);
    }

    protected FacetedContentsResult searchContents(SearchEngineFilter[] filters,
            SearchEngineFilter[] categories, Collection<String> allowedGroups, boolean faceted) throws EntException {
        Query query = null;
        if ((null == filters || filters.length == 0)
                && (null == categories || categories.length == 0)
                && (allowedGroups != null && allowedGroups.contains(Group.ADMINS_GROUP_NAME))) {
            query = new MatchAllDocsQuery();
        } else {
            query = this.createQuery(filters, categories, allowedGroups);
        }
        return this.executeQuery(query, filters, faceted);
    }
    
    protected FacetedContentsResult executeQuery(Query query, 
            SearchEngineFilter[] filters, boolean faceted) throws EntException {
        FacetedContentsResult result = new FacetedContentsResult();
        List<String> contentsId = new ArrayList<>();
        Map<String, Integer> occurrences = new HashMap<>();
        result.setOccurrences(occurrences);
        result.setContentsId(contentsId);
        SolrClient client = this.getSolrClient();
        try {
            SolrQuery solrQuery = new SolrQuery(query.toString());
            solrQuery.addField(SolrFields.SOLR_CONTENT_ID_FIELD_NAME);
            if (faceted) {
                solrQuery.addField(SolrFields.SOLR_CONTENT_CATEGORY_FIELD_NAME);
            }
            solrQuery.setRows(10000);
            if (null != filters) {
                for (int i = 0; i < filters.length; i++) {
                    SearchEngineFilter filter = filters[i];
                    if (null != this.getRelevance(filter)) {
                        solrQuery.addSort("score", ORDER.desc);
                    } else if (null != filter.getOrder()) {
                        String fieldKey = this.getFilterKey(filter);
                        boolean revert = filter.getOrder().toString().equalsIgnoreCase("DESC");
                        solrQuery.addSort(fieldKey, (revert) ? ORDER.desc: ORDER.asc);
                    }
                }
            }
            QueryResponse response = client.query(this.getSolrCore(), solrQuery);
            SolrDocumentList documents = response.getResults();
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
        } catch (SolrException inf) {
            logger.error("Solr exception", inf);
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
    
    protected Query createDoubleQuery(SearchEngineFilter[][] filters,
            SearchEngineFilter[] categories, Collection<String> allowedGroups) {
        BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();
        if (filters != null && filters.length > 0) {
            for (int i = 0; i < filters.length; i++) {
                SearchEngineFilter[] internalFilters = filters[i];
                BooleanQuery.Builder internalMainQuery = new BooleanQuery.Builder();
                for (int j = 0; j < internalFilters.length; j++) {
                    SearchEngineFilter internalFilter = internalFilters[j];
                    Query fieldQuery = this.createQueryByFilter(internalFilter);
                    if (null != fieldQuery) {
                        internalMainQuery.add(fieldQuery, BooleanClause.Occur.SHOULD);
                    }
                }
                mainQuery.add(internalMainQuery.build(), BooleanClause.Occur.MUST);
            }
        }
        this.addGroupsQueryBlock(mainQuery, allowedGroups);
        this.addCategoriesQueryBlock(mainQuery, categories);
        return mainQuery.build();
    }
    
    protected Query createQuery(SearchEngineFilter[] filters,
            SearchEngineFilter[] categories, Collection<String> allowedGroups) {
        BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();
        if (filters != null && filters.length > 0) {
            for (int i = 0; i < filters.length; i++) {
                SearchEngineFilter filter = filters[i];
                Query fieldQuery = this.createQueryByFilter(filter);
                if (null != fieldQuery) {
                    mainQuery.add(fieldQuery, BooleanClause.Occur.MUST);
                }
            }
        }
        this.addGroupsQueryBlock(mainQuery, allowedGroups);
        this.addCategoriesQueryBlock(mainQuery, categories);
        return mainQuery.build();
    }

    protected void addGroupsQueryBlock(BooleanQuery.Builder mainQuery, Collection<String> allowedGroups) {
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
    }
    
    protected void addCategoriesQueryBlock(BooleanQuery.Builder mainQuery, SearchEngineFilter[] categories) {
        if (null != categories && categories.length > 0) {
            BooleanQuery.Builder categoriesQuery = new BooleanQuery.Builder();
            for (int i = 0; i < categories.length; i++) {
                SearchEngineFilter categoryFilter = categories[i];
                List<String> allowedValues = categoryFilter.getAllowedValues();
                if (null != allowedValues && !allowedValues.isEmpty()) {
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
    }

    protected Query createQueryByFilter(SearchEngineFilter filter) {
        BooleanQuery.Builder fieldQuery = null;
        String key = this.getFilterKey(filter);
        String attachmentKey = key + SolrFields.ATTACHMENT_FIELD_SUFFIX;
        Object value = filter.getValue();
        List<?> allowedValues = filter.getAllowedValues();
        Integer relevanceValue = this.getRelevance(filter);
        String relevance = (null != relevanceValue) ? "^"+relevanceValue : "";
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
                    TermQuery term = new TermQuery(new Term(key, singleValue + relevance));
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
                            logger.debug("'ANY_WORD' option deprecated - used 'AT_LEAST_ONE_WORD'");
                            //bc = BooleanClause.Occur.MUST_NOT;
                        }
                        for (int i = 0; i < values.length; i++) {
                            Query queryTerm = this.getTermQueryForTextSearch(key, values[i], filter.isLikeOption(), relevance);
                            singleOptionFieldQuery.add(queryTerm, bc);
                        }
                        fieldQuery.add(singleOptionFieldQuery.build(), BooleanClause.Occur.SHOULD);
                    } else {
                        PhraseQuery.Builder phraseQuery = new PhraseQuery.Builder();
                        for (int i = 0; i < values.length; i++) {
                            phraseQuery.add(new Term(key, values[i].toLowerCase() + relevance), i);
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
                query = TermRangeQuery.newStringRange(key, start + relevance, end + relevance, false, false);
            } else if (filter.getStart() instanceof Number || filter.getEnd() instanceof Number) {
                Long lowerValue = (null != filter.getStart()) ? ((Number) filter.getStart()).longValue() : Long.MIN_VALUE;
                Long upperValue = (null != filter.getEnd()) ? ((Number) filter.getEnd()).longValue() : Long.MAX_VALUE;
                query = LongPoint.newRangeQuery(key, lowerValue, upperValue);
            } else {
                String start = (null != filter.getStart()) ? filter.getStart().toString().toLowerCase() : "A";
                String end = (null != filter.getEnd()) ? filter.getEnd().toString().toLowerCase() + "z" : null;
                query = TermRangeQuery.newStringRange(key, start + relevance, end + relevance, true, true);
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
                        logger.debug("'ANY_WORD' option deprecated - used 'AT_LEAST_ONE_WORD'");
                        //bc = BooleanClause.Occur.MUST_NOT;
                    }
                    for (int i = 0; i < values.length; i++) {
                        Query queryTerm = this.getTermQueryForTextSearch(key, values[i], filter.isLikeOption(), relevance);
						if ((filter instanceof SolrSearchEngineFilter) && ((SolrSearchEngineFilter)filter).isIncludeAttachments()) {
							BooleanQuery.Builder compositeQuery = new BooleanQuery.Builder ();
							compositeQuery.add(queryTerm, BooleanClause.Occur.SHOULD);
							TermQuery termAttachment = new TermQuery(new Term(attachmentKey, values[i].toLowerCase() + relevance));
							compositeQuery.add(termAttachment, BooleanClause.Occur.SHOULD);
							fieldQuery.add(compositeQuery.build(), bc);
						} else {
							fieldQuery.add(queryTerm, bc);
						}
                    }
                } else {
                    PhraseQuery.Builder phraseQuery = new PhraseQuery.Builder();
                    for (int i = 0; i < values.length; i++) {
                        phraseQuery.add(new Term(key, values[i].toLowerCase() + relevance), i);
                    }
					if ((filter instanceof SolrSearchEngineFilter) && ((SolrSearchEngineFilter)filter).isIncludeAttachments()) {
						fieldQuery.add(phraseQuery.build(), BooleanClause.Occur.SHOULD);
						PhraseQuery.Builder phraseQuery2 = new PhraseQuery.Builder();
						for (int i = 0; i < values.length; i++) {
							//NOTE: search lower case....
							phraseQuery2.add(new Term(attachmentKey, values[i].toLowerCase() + relevance));
						}
						fieldQuery.add(phraseQuery2.build(), BooleanClause.Occur.SHOULD);
					} else {
						return phraseQuery.build();
					}
                }
            } else if (value instanceof Date) {
                String toString = DateConverter.getFormattedDate((Date) value, SolrFields.SOLR_SEARCH_DATE_FORMAT);
                TermQuery term = new TermQuery(new Term(key, toString + relevance));
                fieldQuery.add(term, BooleanClause.Occur.MUST);
            } else if (value instanceof Number) {
                TermQuery term = new TermQuery(new Term(key, value.toString() + relevance));
                fieldQuery.add(term, BooleanClause.Occur.MUST);
            }
        } else {
            fieldQuery = new BooleanQuery.Builder();
            Term term = new Term(key, "*" + relevance);
            Query queryTerm = new WildcardQuery(term);
            fieldQuery.add(queryTerm, BooleanClause.Occur.MUST);
        }
        return fieldQuery.build();
    }
    
    private Integer getRelevance(SearchEngineFilter filter) {
        if (filter instanceof SolrSearchEngineFilter 
                && null != ((SolrSearchEngineFilter)filter).getRelevancy() 
                && ((SolrSearchEngineFilter)filter).getRelevancy() > 1) {
            return ((SolrSearchEngineFilter)filter).getRelevancy();
        }
        return null;
    }

    protected Query getTermQueryForTextSearch(String key, String value, boolean isLikeSearch, String relevance) {
        //NOTE: search for lower case....
        String stringValue = value.toLowerCase();
        boolean useWildCard = false;
        if (value.startsWith("*") || value.endsWith("*")) {
            useWildCard = true;
        } else if (isLikeSearch) {
            stringValue = "*" + stringValue + "*";
            useWildCard = true;
        }
        Term term = new Term(key, stringValue + relevance);
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
