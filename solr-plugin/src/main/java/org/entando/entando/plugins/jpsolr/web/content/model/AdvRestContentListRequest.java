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
            for (int i = 0; i < this.getCsvCategories().length; i++) {
                String csv = this.getCsvCategories()[i];
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
            for (int i = 0; i < df.length; i++) {
                SolrFilter[] internalFilters = df[i];
                SolrSearchEngineFilter[] internalSearchFilters = new SolrSearchEngineFilter[]{};
                for (int j = 0; j < internalFilters.length; j++) {
                    SolrFilter internalFilter = internalFilters[j];
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
            for (int i = 0; i < filters.length; i++) {
                SolrFilter filter = filters[i];
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

    /**
     * Set the filters
     *
     * @param filters the filters to set.
     * @deprecated Wrong name for an array, use setFilters method
     */
    @Deprecated
    @Override
    public void setFilter(Filter[] filters) {
        this.setFilters(filters);
    }

    /**
     * Return the filters.
     *
     * @return the filters.
     * @deprecated Wrong name for an array, use getFilters method.
     */
    @Deprecated
    @Override
    public Filter[] getFilter() {
        return this.getFilters();
    }

    public SolrFilter[][] getDoubleFilters() {
        return doubleFilters;
    }

    public void setDoubleFilters(SolrFilter[][] doubleFilters) {
        this.doubleFilters = doubleFilters;
    }
}
