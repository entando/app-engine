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

import org.entando.entando.web.common.model.Filter;

/**
 * @author E.Santoboni
 */
public class SolrFilter extends Filter {

    private Integer relevancy;
    private boolean fullText;
    private String searchOption;

    public SolrFilter() {
    }

    public SolrFilter(String attribute, String value) {
        super(attribute, value);
    }

    public SolrFilter(String attribute, String value, String operator) {
        super(attribute, value, operator);
    }

    public Integer getRelevancy() {
        return relevancy;
    }

    public void setRelevancy(Integer relevancy) {
        this.relevancy = relevancy;
    }

    public boolean isFullText() {
        return fullText;
    }

    public void setFullText(boolean fullText) {
        this.fullText = fullText;
    }

    public String getSearchOption() {
        return searchOption;
    }

    public void setSearchOption(String searchOption) {
        this.searchOption = searchOption;
    }

}
