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

import com.agiletec.aps.system.common.tree.ITreeNode;
import com.agiletec.aps.system.common.tree.ITreeNodeManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.util.DateConverter;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.NumericSearchEngineFilter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFacetedContentsResult;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrSearchEngineFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author E.Santoboni
 */
public class SearcherDAO implements ISolrSearcherDAO {

    private static final Logger logger = LoggerFactory.getLogger(SearcherDAO.class);

    private ITreeNodeManager treeNodeManager;
    private ILangManager langManager;

    private final SolrClient solrClient;
    private final String solrCore;

    public SearcherDAO(SolrClient solrClient, String solrCore) {
        this.solrClient = solrClient;
        this.solrCore = solrCore;
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
    public SolrFacetedContentsResult searchFacetedContents(SearchEngineFilter[] filters,
            SearchEngineFilter[] categories, Collection<String> allowedGroups) throws EntException {
        return this.searchContents(filters, categories, allowedGroups, true);
    }

    @Override
    public SolrFacetedContentsResult searchFacetedContents(SearchEngineFilter[][] filters,
            SearchEngineFilter[] categories, Collection<String> allowedGroups) throws EntException {
        SearchEngineFilter[] singleFilters = new SearchEngineFilter[0];
        if (null != filters) {
            for (SearchEngineFilter[] firstBlock : filters) {
                for (SearchEngineFilter<?> singleFilter : firstBlock) {
                    singleFilters = ArrayUtils.add(singleFilters, singleFilter);
                }
            }
        }
        SearchEngineFilter<?> filterForPagination = this.extractPaginationFilter(singleFilters);
        Query query;
        if ((singleFilters.length == 0 || (singleFilters.length == 1 && null != filterForPagination))
                && (null == categories || categories.length == 0)
                && (allowedGroups != null && allowedGroups.contains(Group.ADMINS_GROUP_NAME))) {
            query = new MatchAllDocsQuery();
        } else {
            query = this.createDoubleQuery(filters, categories, allowedGroups);
        }
        return this.executeQuery(query, singleFilters, true);
    }

    protected SolrFacetedContentsResult searchContents(SearchEngineFilter[] filters,
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

    protected SolrFacetedContentsResult executeQuery(Query query,
            SearchEngineFilter[] filters, boolean faceted) throws EntException {
        SolrFacetedContentsResult result = new SolrFacetedContentsResult();
        List<String> contentsId = new ArrayList<>();
        Map<String, Integer> occurrences = new HashMap<>();
        result.setOccurrences(occurrences);
        result.setContentsId(contentsId);
        try {
            SolrQuery solrQuery = new SolrQuery(query.toString());
            solrQuery.addField(SolrFields.SOLR_CONTENT_ID_FIELD_NAME);
            SearchEngineFilter<?> filterForPagination = this.extractPaginationFilter(filters);
            if (null != filterForPagination) {
                solrQuery.setStart(filterForPagination.getOffset());
                solrQuery.setRows(filterForPagination.getLimit());
            } else {
                solrQuery.setRows(100);
            }
            solrQuery.addFacetField(SolrFields.SOLR_CONTENT_CATEGORY_FIELD_NAME);
            if (null != filters) {
                for (SearchEngineFilter<?> filter : filters) {
                    if (null != this.getRelevance(filter)) {
                        solrQuery.addSort("score", ORDER.desc);
                    } else if (null != filter.getOrder()) {
                        String fieldKey = this.getFilterKey(filter);
                        boolean revert = filter.getOrder().toString().equalsIgnoreCase("DESC");
                        solrQuery.addSort(fieldKey, (revert) ? ORDER.desc : ORDER.asc);
                    }
                }
            }
            QueryResponse response = this.solrClient.query(this.solrCore, solrQuery);
            SolrDocumentList documents = response.getResults();
            result.setTotalSize(Math.toIntExact(documents.getNumFound()));
            for (SolrDocument doc : documents) {
                String id = doc.get(SolrFields.SOLR_CONTENT_ID_FIELD_NAME).toString();
                contentsId.add(id);
            }
            if (faceted) {
                for (FacetField facetField : response.getFacetFields()) {
                    List<FacetField.Count> facetInfo = facetField.getValues();
                    for (FacetField.Count facetInstance : facetInfo) {
                        if (facetInstance.getCount() != 0l) {
                            occurrences.put(facetInstance.getName(), Math.toIntExact(facetInstance.getCount()));
                        }
                    }
                }
            }
        } catch (SolrException inf) {
            logger.error("Solr exception", inf);
        } catch (IOException | SolrServerException | RuntimeException ex) {
            throw new EntException("Error extracting documents", ex);
        }
        return result;
    }

    private SearchEngineFilter<?> extractPaginationFilter(SearchEngineFilter[] filters) {
        return (null != filters) ? Arrays.asList(filters).stream().filter(f -> (null != f.getLimit() && f.getLimit() > 0
                && null != f.getOffset() && f.getOffset() > -1)).findAny().orElse(null) : null;
    }

    protected Query createDoubleQuery(SearchEngineFilter[][] filters,
            SearchEngineFilter[] categories, Collection<String> allowedGroups) {
        BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();
        if (filters != null && filters.length > 0) {
            for (SearchEngineFilter[] internalFilters : filters) {
                if (internalFilters.length == 1) {
                    SearchEngineFilter<?> internalFilter = internalFilters[0];
                    BooleanClause.Occur occur =
                            (internalFilter.isNotOption()) ? BooleanClause.Occur.MUST_NOT : BooleanClause.Occur.MUST;
                    this.createAndAddQuery(mainQuery, internalFilter, occur);
                } else {
                    BooleanQuery.Builder internalMainQuery = new BooleanQuery.Builder();
                    boolean addedFilter = false;
                    for (SearchEngineFilter<?> internalFilter : internalFilters) {
                        BooleanClause.Occur occur = (internalFilter.isNotOption()) ? BooleanClause.Occur.MUST_NOT
                                : BooleanClause.Occur.SHOULD;
                        if (this.createAndAddQuery(internalMainQuery, internalFilter, occur)) {
                            addedFilter = true;
                        }
                    }
                    if (addedFilter) {
                        mainQuery.add(internalMainQuery.build(), BooleanClause.Occur.MUST);
                    }
                }
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
            for (SearchEngineFilter<?> filter : filters) {
                BooleanClause.Occur occur =
                        (filter.isNotOption()) ? BooleanClause.Occur.MUST_NOT : BooleanClause.Occur.MUST;
                this.createAndAddQuery(mainQuery, filter, occur);
            }
        }
        this.addGroupsQueryBlock(mainQuery, allowedGroups);
        this.addCategoriesQueryBlock(mainQuery, categories);
        return mainQuery.build();
    }

    protected boolean createAndAddQuery(BooleanQuery.Builder mainQuery, SearchEngineFilter<?> filter,
            BooleanClause.Occur occurs) {
        Query fieldQuery = this.createQueryByFilter(filter);
        if (null != fieldQuery) {
            mainQuery.add(fieldQuery, occurs);
            return true;
        }
        return false;
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
            for (String group : allowedGroups) {
                TermQuery groupQuery = new TermQuery(new Term(SolrFields.SOLR_CONTENT_GROUP_FIELD_NAME, group));
                groupsQuery.add(groupQuery, BooleanClause.Occur.SHOULD);
            }
            mainQuery.add(groupsQuery.build(), BooleanClause.Occur.MUST);
        }
    }

    protected void addCategoriesQueryBlock(BooleanQuery.Builder mainQuery, SearchEngineFilter<String>[] categories) {
        if (null != categories && categories.length > 0) {
            BooleanQuery.Builder categoriesQuery = new BooleanQuery.Builder();
            for (SearchEngineFilter<String> categoryFilter : categories) {
                List<String> allowedValues = categoryFilter.getAllowedValues();
                if (null != allowedValues && !allowedValues.isEmpty()) {
                    BooleanQuery.Builder singleCategoriesQuery = new BooleanQuery.Builder();
                    for (String singleCategory : allowedValues) {
                        ITreeNode treeNode = this.getTreeNodeManager().getNode(singleCategory);
                        if (null != treeNode) {
                            TermQuery categoryQuery = new TermQuery(
                                    new Term(SolrFields.SOLR_CONTENT_CATEGORY_FIELD_NAME, treeNode.getCode()));
                            singleCategoriesQuery.add(categoryQuery, BooleanClause.Occur.SHOULD);
                        }
                    }
                    categoriesQuery.add(singleCategoriesQuery.build(), BooleanClause.Occur.MUST);
                } else if (null != categoryFilter.getValue()) {
                    ITreeNode treeNode = this.getTreeNodeManager().getNode(categoryFilter.getValue());
                    if (null != treeNode) {
                        TermQuery categoryQuery = new TermQuery(
                                new Term(SolrFields.SOLR_CONTENT_CATEGORY_FIELD_NAME, treeNode.getCode()));
                        categoriesQuery.add(categoryQuery, BooleanClause.Occur.MUST);
                    }
                }
            }
            mainQuery.add(categoriesQuery.build(), BooleanClause.Occur.MUST);
        }
    }

    protected Query createQueryByFilter(SearchEngineFilter<?> filter) {
        if (null == filter.getKey() || this.isPaginationFilter(filter)) {
            return null;
        }
        BooleanQuery.Builder fieldQuery = null;
        String key = this.getFilterKey(filter);
        String attachmentKey = key + SolrFields.ATTACHMENT_FIELD_SUFFIX;
        Object value = filter.getValue();
        List<?> allowedValues = filter.getAllowedValues();
        Integer relevanceValue = this.getRelevance(filter);
        String relevance = (null != relevanceValue) ? "^" + relevanceValue : "";
        if (null != allowedValues && !allowedValues.isEmpty()) {
            fieldQuery = new BooleanQuery.Builder();
            SearchEngineFilter.TextSearchOption option = filter.getTextSearchOption();
            if (null == option) {
                option = SearchEngineFilter.TextSearchOption.AT_LEAST_ONE_WORD;
            }
            //To be improved to manage different type
            for (Object singleValue : allowedValues) {
                if (filter instanceof NumericSearchEngineFilter) {
                    TermQuery term = new TermQuery(new Term(key, singleValue + relevance));
                    fieldQuery.add(term, BooleanClause.Occur.SHOULD);
                } else {
                    //NOTE: search for lower case....
                    String[] values = singleValue.toString().split("\\s+");
                    if (!option.equals(SearchEngineFilter.TextSearchOption.EXACT)) {
                        BooleanQuery.Builder singleOptionFieldQuery = new BooleanQuery.Builder();
                        BooleanClause.Occur bc = BooleanClause.Occur.SHOULD;
                        if (option.equals(SearchEngineFilter.TextSearchOption.ALL_WORDS)) {
                            bc = BooleanClause.Occur.MUST;
                        } else if (option.equals(SearchEngineFilter.TextSearchOption.ANY_WORD)) {
                            logger.debug("'ANY_WORD' option deprecated - used 'AT_LEAST_ONE_WORD'");
                        }
                        for (String val : values) {
                            Query queryTerm = this.getTermQueryForTextSearch(key, val, filter.isLikeOption(),
                                    relevance);
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
                String format = SolrFields.SOLR_SEARCH_DATE_RANGE_FORMAT;
                String start =
                        (null != filter.getStart()) ? DateConverter.getFormattedDate((Date) filter.getStart(), format)
                                : SolrFields.SOLR_DATE_MIN;
                String end = (null != filter.getEnd()) ? DateConverter.getFormattedDate((Date) filter.getEnd(), format)
                        : SolrFields.SOLR_DATE_MAX;
                query = TermRangeQuery.newStringRange(key, start + relevance, end + relevance, true, true);
            } else if (filter.getStart() instanceof Number || filter.getEnd() instanceof Number) {
                Long lowerValue =
                        (null != filter.getStart()) ? ((Number) filter.getStart()).longValue() : Long.MIN_VALUE;
                Long upperValue = (null != filter.getEnd()) ? ((Number) filter.getEnd()).longValue() : Long.MAX_VALUE;
                query = LongPoint.newRangeQuery(key, lowerValue, upperValue);
            } else {
                String start = (null != filter.getStart()) ? filter.getStart().toString().toLowerCase() : "A";
                String end = (null != filter.getEnd()) ? filter.getEnd().toString().toLowerCase() + "z" : null;
                query = TermRangeQuery.newStringRange(key, start + relevance, (null != end) ? (end + relevance) : null,
                        true, true);
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
                    }
                    for (String val : values) {
                        Query queryTerm = this.getTermQueryForTextSearch(key, val, filter.isLikeOption(),
                                relevance);
                        if ((filter instanceof SolrSearchEngineFilter)
                                && ((SolrSearchEngineFilter) filter).isIncludeAttachments()) {
                            BooleanQuery.Builder compositeQuery = new BooleanQuery.Builder();
                            compositeQuery.add(queryTerm, BooleanClause.Occur.SHOULD);
                            TermQuery termAttachment = new TermQuery(
                                    new Term(attachmentKey, val.toLowerCase() + relevance));
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
                    if ((filter instanceof SolrSearchEngineFilter)
                            && ((SolrSearchEngineFilter) filter).isIncludeAttachments()) {
                        fieldQuery.add(phraseQuery.build(), BooleanClause.Occur.SHOULD);
                        PhraseQuery.Builder phraseQuery2 = new PhraseQuery.Builder();
                        for (String val : values) {
                            //NOTE: search lower case....
                            phraseQuery2.add(new Term(attachmentKey, val.toLowerCase() + relevance));
                        }
                        fieldQuery.add(phraseQuery2.build(), BooleanClause.Occur.SHOULD);
                    } else {
                        return phraseQuery.build();
                    }
                }
            } else if (value instanceof Date) {
                String toString = DateConverter.getFormattedDate((Date) value,
                        SolrFields.SOLR_SEARCH_DATE_VALUE_FORMAT);
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

    private boolean isPaginationFilter(SearchEngineFilter<?> filter) {
        return (filter instanceof SolrSearchEngineFilter
                && ((SolrSearchEngineFilter) filter).isPaginationFilter());
    }

    private Integer getRelevance(SearchEngineFilter<?> filter) {
        if (filter instanceof SolrSearchEngineFilter
                && null != ((SolrSearchEngineFilter) filter).getRelevancy()
                && ((SolrSearchEngineFilter) filter).getRelevancy() > 1) {
            return ((SolrSearchEngineFilter) filter).getRelevancy();
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

    protected String getFilterKey(SearchEngineFilter<?> filter) {
        String key = filter.getKey().replace(":", "_");
        if (filter.isFullTextSearch()) {
            return key;
        }
        if (!filter.isAttributeFilter()
                && !(key.startsWith(SolrFields.SOLR_FIELD_PREFIX))) {
            key = SolrFields.SOLR_FIELD_PREFIX + key;
        } else if (filter.isAttributeFilter()) {
            String insertedLangCode = filter.getLangCode();
            String langCode = (StringUtils.isBlank(insertedLangCode)) ? this.getLangManager().getDefaultLang().getCode()
                    : insertedLangCode;
            key = langCode.toLowerCase() + "_" + key;
        }
        return key;
    }

    @Override
    public void close() {
        // nothing to do
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
