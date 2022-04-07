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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFacetedContentsResult;

/**
 * @author E.Santoboni
 */
public class SolrFacetedPagedMetadata {

    private int page;
    private int pageSize;
    private int lastPage;
    private int totalItems;
    private String sort;
    private String direction;
    
    @JsonInclude(Include.NON_NULL)
    private String lang;
    private String[] csvCategories = new String[0];
    @JsonInclude(Include.NON_NULL)
    private String text;
    @JsonInclude(Include.NON_NULL)
    private String searchOption;
    @JsonInclude(Include.NON_NULL)
    private boolean includeAttachments;
    private SolrFilter[][] doubleFilters = new SolrFilter[0][0];
    
    private SolrFilter[] filters = new SolrFilter[0];
    
    private Map<String, String> additionalParams = new HashMap<>();

    @JsonIgnore
    private int actualSize;

    @JsonIgnore
    private SolrFacetedContentsResult body;

    public SolrFacetedPagedMetadata() {
    }

    public SolrFacetedPagedMetadata(AdvRestContentListRequest req, Integer totalItems) {
        if (0 == req.getPageSize()) {
            // no pagination
            this.actualSize = totalItems;
        } else {
            this.actualSize = req.getPageSize();
        }
        this.page = req.getPage();
        this.pageSize = req.getPageSize();
        Double pages = Math.ceil(new Double(totalItems) / new Double(this.actualSize));
        this.lastPage = pages.intValue() == 0 ? 1 : pages.intValue();
        this.totalItems = totalItems;
        this.setSort(req.getSort());
        this.setDirection(req.getDirection());
        if (null != req.getFilters()) {
            this.setFilters(req.getFilters());
        }
        if (null != req.getDoubleFilters()) {
            this.setDoubleFilters(req.getDoubleFilters());
        }
        if (null != req.getCsvCategories()) {
            this.setCsvCategories(req.getCsvCategories());
        }
        this.setLang(req.getLang());
        this.setText(req.getText());
        this.setSearchOption(req.getSearchOption());
        this.setIncludeAttachments(req.isIncludeAttachments());
        this.setLang(req.getLang());
        this.setLang(req.getLang());
        this.setLang(req.getLang());
    }

    public int getPage() {
        return page;
    }
    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }
    public void setPageSize(int size) {
        this.pageSize = size;
    }

    public int getLastPage() {
        return lastPage;
    }
    public void setLastPage(int last) {
        this.lastPage = last;
    }
    public SolrFacetedContentsResult getBody() {
        return body;
    }

    public void setBody(SolrFacetedContentsResult body) {
        this.body = body;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public SolrFilter[] getFilters() {
        return filters;
    }

    public void setFilters(SolrFilter[] filters) {
        this.filters = filters;
    }

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

    public SolrFilter[][] getDoubleFilters() {
        return doubleFilters;
    }

    public void setDoubleFilters(SolrFilter[][] doubleFilters) {
        this.doubleFilters = doubleFilters;
    }
    
    public int getActualSize() {
        return actualSize;
    }

    public void setActualSize(int actualSize) {
        this.actualSize = actualSize;
    }

    public Map<String, String> getAdditionalParams() {
        return additionalParams;
    }

    public void setAdditionalParams(Map<String, String> additionalParams) {
        this.additionalParams = additionalParams;
    }

    public void addAdditionalParams(String key, String value) {
        this.additionalParams.put(key, value);
    }
/*
    @Override
    public int hashCode() {
        int result = Objects.hash(page, pageSize, lastPage, totalItems, sort, direction, actualSize, body);
        result = 31 * result + Arrays.hashCode(filters);
        return result;
    }
*/
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("page", page)
                .append("pageSize", pageSize)
                .append("lastPage", lastPage)
                .append("totalItems", totalItems)
                .append("sort", sort)
                .append("direction", direction)
                .append("filters", filters)
                .append("additionalParams", additionalParams)
                .append("actualSize", actualSize)
                .append("body", body)
                .toString();
    }
    
}
