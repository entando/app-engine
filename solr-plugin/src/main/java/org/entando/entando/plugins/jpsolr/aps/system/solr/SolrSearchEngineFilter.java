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

import java.util.List;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter;

/**
 * @author E.Santoboni
 */
public class SolrSearchEngineFilter<T extends Object> extends SearchEngineFilter<T> {
    
    private boolean includeAttachments;

    public SolrSearchEngineFilter(String key, T value) {
        super(key, value);
    }

    public SolrSearchEngineFilter(String key, T value, TextSearchOption textSearchOption) {
        super(key, value, textSearchOption);
    }

    public SolrSearchEngineFilter(String key, List<T> allowedValues, TextSearchOption textSearchOption) {
        super(key, allowedValues, textSearchOption);
    }

    public SolrSearchEngineFilter(String key, T start, T end) {
        super(key, start, end);
    }

    public SolrSearchEngineFilter(String key, boolean attributeFilter) {
        super(key, attributeFilter);
    }

    public SolrSearchEngineFilter(String key, boolean attributeFilter, T value) {
        super(key, attributeFilter, value);
    }

    public SolrSearchEngineFilter(String key, boolean attributeFilter, T value, TextSearchOption textSearchOption) {
        super(key, attributeFilter, value, textSearchOption);
    }

    public boolean isIncludeAttachments() {
        return includeAttachments;
    }
    public void setIncludeAttachments(boolean includeAttachments) {
        this.includeAttachments = includeAttachments;
    }
    
}
