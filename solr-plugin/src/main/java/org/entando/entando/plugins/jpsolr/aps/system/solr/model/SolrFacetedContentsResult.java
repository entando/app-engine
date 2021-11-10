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
package org.entando.entando.plugins.jpsolr.aps.system.solr.model;

import org.entando.entando.aps.system.services.searchengine.FacetedContentsResult;

/**
 * @author E.Santoboni
 */
public class SolrFacetedContentsResult extends FacetedContentsResult {
    
    private Integer totalSize;

    public SolrFacetedContentsResult() {
        super();
    }
    
    public Integer getTotalSize() {
        return totalSize;
    }
    public void setTotalSize(Integer totalSize) {
        this.totalSize = totalSize;
    }
    
}
