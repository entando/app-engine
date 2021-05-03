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
package org.entando.entando.plugins.jpsolr.web.content;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter;
import org.entando.entando.web.common.model.Filter;
import org.entando.entando.web.common.model.FilterOperator;
import org.entando.entando.web.common.model.FilterType;
import org.entando.entando.web.common.model.RestEntityListRequest;

/**
 *
 * @author E.Santoboni
 */
public class AdvRestContentListRequest extends RestEntityListRequest {

    private String lang;

    private String[] csvCategories;
    private String text;
    
    private boolean guestUser;

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

    public boolean isGuestUser() {
        return guestUser;
    }

    public void setGuestUser(boolean guestUser) {
        this.guestUser = guestUser;
    }
    
    public SearchEngineFilter[] extractCategoryFilters() {
        SearchEngineFilter[] categoryFilters = new SearchEngineFilter[]{};
        if (null != this.getCsvCategories()) {
            for (int i = 0; i < this.getCsvCategories().length; i++) {
                String csv = this.getCsvCategories()[i];
                List<String> codes = Arrays.asList(csv.split(","));
                SearchEngineFilter searchFilter = new SearchEngineFilter("category", false, codes);
                categoryFilters = ArrayUtils.add(categoryFilters, searchFilter);
            }
        }
        return categoryFilters;
    }
    
    public SearchEngineFilter[] extractFilters(String langCode) {
        SearchEngineFilter[] searchFilters = new SearchEngineFilter[]{};
        if (null != super.getFilters()) {
            for (int i = 0; i < super.getFilters().length; i++) {
                Filter filter = super.getFilters()[i];
                SearchEngineFilter searchFilter = null;
                boolean isAttribute = !StringUtils.isEmpty(filter.getEntityAttr());
                String key = isAttribute ? filter.getEntityAttr() : filter.getAttribute();
                Object objectValue = this.extractFilterValue(filter);
                if (FilterOperator.GREATER.getValue().equalsIgnoreCase(filter.getOperator())) {
                    searchFilter = SearchEngineFilter.createRangeFilter(key, isAttribute, objectValue, null);
                } else if (FilterOperator.LOWER.getValue().equalsIgnoreCase(filter.getOperator())) {
                    searchFilter = SearchEngineFilter.createRangeFilter(key, isAttribute, null, objectValue);
                } else {
                    searchFilter = new SearchEngineFilter(key, isAttribute, objectValue);
                    if (null != objectValue && 
                            !StringUtils.isBlank(objectValue.toString()) 
                            && FilterOperator.LIKE.getValue().equalsIgnoreCase(filter.getOperator())) {
                        searchFilter.setLikeOption(true);
                    }
                }
                searchFilter.setOrder(filter.getOrder());
                if (isAttribute) {
                    searchFilter.setLangCode(langCode);
                }
                searchFilters = ArrayUtils.add(searchFilters, searchFilter);
            }
        }
        if (!StringUtils.isBlank(this.getText())) {
            SearchEngineFilter searchFilter = new SearchEngineFilter(langCode, this.getText(), SearchEngineFilter.TextSearchOption.AT_LEAST_ONE_WORD);
            searchFilter.setFullTextSearch(true);
            searchFilters = ArrayUtils.add(searchFilters, searchFilter);
        }
        return searchFilters;
    }
    
    protected Object extractFilterValue(Filter filter) {
        FilterType filterType = FilterType.STRING;
        if (filter.getType() != null) {
            filterType = FilterType.parse(filter.getType().toLowerCase());
        }
        if (filter.getAllowedValues() != null && filter.getAllowedValues().length > 0) {
            return Arrays.stream(filter.getAllowedValues()).map(filterType::parseFilterValue).collect(Collectors.toList());
        }
        if (StringUtils.isBlank(filter.getValue())) {
            return null;
        }
        return filterType.parseFilterValue(filter.getValue());
    }
    
}
