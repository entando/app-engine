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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SolrFacetedContentsResultTest {

    @Test
    void totalSizeDefaultsToZero() {
        // A brand-new result (e.g. returned when SearcherDAO.executeQuery swallows a Solr
        // exception) must report a non-null total. Otherwise PagedMetadata throws an NPE while
        // building the response, turning a swallowed Solr error into an HTTP 500.
        Assertions.assertEquals(0, new SolrFacetedContentsResult().getTotalSize());
    }
}
