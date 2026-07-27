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
package org.entando.entando.plugins.jpsolr.web.content.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrSearchEngineFilter;
import org.entando.entando.web.common.model.Filter;
import org.entando.entando.web.common.model.FilterOperator;
import org.entando.entando.web.common.model.FilterType;
import org.entando.entando.web.common.model.RestEntityListRequest;

/**
 * @author E.Santoboni
 */
@EqualsAndHashCode(callSuper=true)
@ToString
public class AdvRestContentListRequest extends RestEntityListRequest {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(AdvRestContentListRequest.class);

    private String lang;

    private String[] csvCategories;
    private String text;
    private String searchOption;
    private boolean includeAttachments;

    private boolean guestUser;

    private SolrFilter[][] doubleFilters;

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String[] getCsvCategories() {
        return csvCategories;
    }

    public void setCsvCategories(String[] csvCategories) {
        this.csvCategories = csvCategories;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSearchOption() {
        return searchOption;
    }

    public void setSearchOption(String searchOption) {
        this.searchOption = searchOption;
    }

    public boolean isIncludeAttachments() {
        return includeAttachments;
    }

    public void setIncludeAttachments(boolean includeAttachments) {
        this.includeAttachments = includeAttachments;
    }

    public boolean isGuestUser() {
        return guestUser;
    }

    public void setGuestUser(boolean guestUser) {
        this.guestUser = guestUser;
    }

    public SolrSearchEngineFilter[] extractCategoryFilters() {
        SolrSearchEngineFilter[] categoryFilters = new SolrSearchEngineFilter[]{};
        if (null != this.getCsvCategories()) {
            for (String csv : this.getCsvCategories()) {
                List<String> codes = Arrays.asList(csv.split(","));
                SolrSearchEngineFilter<List<String>> searchFilter = new SolrSearchEngineFilter<>("category", false,
                        codes);
                categoryFilters = ArrayUtils.add(categoryFilters, searchFilter);
            }
        }
        return categoryFilters;
    }

    public SolrSearchEngineFilter[][] extractDoubleFilters(String langCode) {
        SolrSearchEngineFilter[][] doubleSearchFilters = new SolrSearchEngineFilter[][]{};
        SolrFilter[][] df = this.getDoubleFilters();
        if (null != df) {
            for (SolrFilter[] internalFilters : df) {
                SolrSearchEngineFilter[] internalSearchFilters = new SolrSearchEngineFilter[]{};
                for (SolrFilter internalFilter : internalFilters) {
                    if (this.isEmptyFilter(internalFilter)) {
                        logger.warn("Discarding empty double filter with no attribute to search on");
                        continue;
                    }
                    SolrSearchEngineFilter<?> searchFilter = this.buildSearchFilter(internalFilter, langCode);
                    internalSearchFilters = ArrayUtils.add(internalSearchFilters, searchFilter);
                }
                doubleSearchFilters = ArrayUtils.add(doubleSearchFilters, internalSearchFilters);
            }
        }
        return doubleSearchFilters;
    }

    public SolrSearchEngineFilter[] extractFilters(String langCode) {
        SolrSearchEngineFilter[] searchFilters = new SolrSearchEngineFilter[]{};
        SolrFilter[] filters = this.getFilters();
        if (null != filters) {
            for (SolrFilter filter : filters) {
                if (this.isEmptyFilter(filter)) {
                    logger.warn("Discarding empty filter with no attribute to search on");
                    continue;
                }
                SolrSearchEngineFilter<?> searchFilter = this.buildSearchFilter(filter, langCode);
                searchFilters = ArrayUtils.add(searchFilters, searchFilter);
            }
        }
        if (!StringUtils.isBlank(this.getText())) {
            SearchEngineFilter.TextSearchOption textSearchOption = this.extractTextOption(this.getSearchOption());
            SolrSearchEngineFilter<String> searchFilter
                    = new SolrSearchEngineFilter<>(langCode, this.getText(), textSearchOption);
            searchFilter.setFullTextSearch(true);
            searchFilter.setIncludeAttachments(this.isIncludeAttachments());
            searchFilters = ArrayUtils.add(searchFilters, searchFilter);
        }
        if (null != this.getPageSize() && this.getPageSize() > 0) {
            SolrSearchEngineFilter<?> pageFilter = new SolrSearchEngineFilter<>(this.getPageSize(), this.getOffset());
            searchFilters = ArrayUtils.add(searchFilters, pageFilter);
        }
        return searchFilters;
    }

    private Integer getOffset() {
        int page = this.getPage() - 1;
        if (null == this.getPage() || this.getPage() == 0) {
            return 0;
        }
        return this.getPageSize() * page;
    }

    /**
     * A non-full-text filter has no field to build a Solr query key from when it carries
     * neither an entity attribute nor a metadata attribute. Building a key from such a filter
     * would fail with "Error: Key required". Full-text filters are never in this state: they
     * search on the value, not on a key.
     */
    private boolean hasNoSearchField(SolrFilter filter) {
        return !filter.isFullText()
                && StringUtils.isBlank(filter.getEntityAttr())
                && StringUtils.isBlank(filter.getAttribute());
    }

    /**
     * Whether the filter expresses any search intent, i.e. a value or an allowed-values set.
     * Used to tell a meaningless placeholder apart from a filter the client actually meant to
     * apply. Operator and order are deliberately ignored: {@code operator} always defaults to a
     * non-blank value ("like") so it is not a reliable signal, and an order without a field
     * expresses no restriction on the result set.
     */
    private boolean carriesIntent(SolrFilter filter) {
        return StringUtils.isNotBlank(filter.getValue())
                || (filter.getAllowedValues() != null && filter.getAllowedValues().length > 0);
    }

    /**
     * A pure no-op placeholder: no field to search on and no intent at all. These come from
     * sparse array binding (e.g. {@code filters[99]} leaves indexes 0-98 as empty objects) and
     * are silently discarded, because they never expressed a restriction - dropping them cannot
     * broaden the result set.
     */
    private boolean isEmptyFilter(SolrFilter filter) {
        return filter == null || (this.hasNoSearchField(filter) && !this.carriesIntent(filter));
    }

    /**
     * A filter the client meant to apply (it carries intent) but which has no field to search
     * on. It cannot be honoured and must not be silently dropped - doing so would return a
     * broader result set than requested as if it were a success - so it is reported as a bad
     * request instead.
     */
    private boolean isMalformedFilter(SolrFilter filter) {
        return filter != null && this.hasNoSearchField(filter) && this.carriesIntent(filter);
    }

    /**
     * @return true if any filter (single or double) carries search intent but has no field to
     * search on, which the caller should reject as a bad request.
     */
    public boolean hasMalformedFilters() {
        SolrFilter[] filters = this.getFilters();
        if (null != filters && Arrays.stream(filters).anyMatch(this::isMalformedFilter)) {
            return true;
        }
        SolrFilter[][] df = this.getDoubleFilters();
        if (null != df) {
            for (SolrFilter[] internalFilters : df) {
                if (null != internalFilters
                        && Arrays.stream(internalFilters).anyMatch(this::isMalformedFilter)) {
                    return true;
                }
            }
        }
        return false;
    }

    private SolrSearchEngineFilter<?> buildSearchFilter(SolrFilter filter, String langCode) {
        SolrSearchEngineFilter<Object> searchFilter;
        boolean isAttribute = !StringUtils.isEmpty(filter.getEntityAttr());
        String key = isAttribute ? filter.getEntityAttr() : filter.getAttribute();
        Object objectValue = this.extractFilterValue(filter);
        if (filter.isFullText()) {
            SearchEngineFilter.TextSearchOption textSearchOption = this.extractTextOption(filter.getSearchOption());
            searchFilter = new SolrSearchEngineFilter<>(langCode, objectValue.toString(), textSearchOption);
            searchFilter.setFullTextSearch(true);
            searchFilter.setIncludeAttachments(this.isIncludeAttachments());
        } else {
            if (FilterOperator.GREATER.getValue().equalsIgnoreCase(filter.getOperator())) {
                searchFilter = new SolrSearchEngineFilter<>(key, isAttribute);
                searchFilter.setStart(objectValue);
            } else if (FilterOperator.LOWER.getValue().equalsIgnoreCase(filter.getOperator())) {
                searchFilter = new SolrSearchEngineFilter<>(key, isAttribute);
                searchFilter.setEnd(objectValue);
            } else {
                searchFilter = new SolrSearchEngineFilter<>(key, isAttribute, objectValue);
                if (null != objectValue
                        && !StringUtils.isBlank(objectValue.toString())) {
                    if (FilterOperator.NOT_EQUAL.getValue().equalsIgnoreCase(filter.getOperator())) {
                        searchFilter.setNotOption(true);
                    } else if (FilterOperator.LIKE.getValue().equalsIgnoreCase(filter.getOperator())) {
                        searchFilter.setLikeOption(true);
                    }
                }
            }
        }
        searchFilter.setOrder(filter.getOrder());
        if (isAttribute) {
            searchFilter.setLangCode(langCode);
        }
        if (null != filter.getRelevancy()) {
            searchFilter.setRelevancy(filter.getRelevancy());
        }
        return searchFilter;
    }

    private SearchEngineFilter.TextSearchOption extractTextOption(String param) {
        SearchEngineFilter.TextSearchOption textSearchOption = SearchEngineFilter.TextSearchOption.AT_LEAST_ONE_WORD;
        if (!StringUtils.isBlank(param)) {
            if (param.equalsIgnoreCase("exact")) {
                textSearchOption = SearchEngineFilter.TextSearchOption.EXACT;
            } else if (param.equalsIgnoreCase("all")) {
                textSearchOption = SearchEngineFilter.TextSearchOption.ALL_WORDS;
            }
        }
        return textSearchOption;
    }

    protected Object extractFilterValue(Filter filter) {
        FilterType filterType = FilterType.STRING;
        if (filter.getType() != null) {
            filterType = FilterType.parse(filter.getType().toLowerCase());
        }
        if (filter.getAllowedValues() != null && filter.getAllowedValues().length > 0) {
            return Arrays.stream(filter.getAllowedValues()).map(filterType::parseFilterValue)
                    .collect(Collectors.toList());
        }
        if (StringUtils.isBlank(filter.getValue())) {
            return null;
        }
        return filterType.parseFilterValue(filter.getValue());
    }

    @Override
    public SolrFilter[] getFilters() {
        Filter[] filters = super.getFilters();
        if (null == filters) {
            return null;
        }
        List<Filter> newFilters = Arrays.asList(filters).stream().map(f -> {
            if (f instanceof SolrFilter) {
                return f;
            } else {
                SolrFilter solrFilter = new SolrFilter(f.getAttribute(), f.getValue(), f.getOperator());
                solrFilter.setAllowedValues(f.getAllowedValues());
                solrFilter.setEntityAttr(f.getEntityAttr());
                solrFilter.setOrder(f.getOrder());
                solrFilter.setType(f.getType());
                return solrFilter;
            }
        }).collect(Collectors.toList());
        return newFilters.toArray(new SolrFilter[newFilters.size()]);
    }

    public SolrFilter[][] getDoubleFilters() {
        return doubleFilters;
    }

    public void setDoubleFilters(SolrFilter[][] doubleFilters) {
        this.doubleFilters = doubleFilters;
    }
}
