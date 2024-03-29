/*
 * Copyright 2023-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.system.services.searchengine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SolrEnvironmentVariablesTest {

    @Test
    void testActive() {
        Assertions.assertTrue(SolrEnvironmentVariables.active());
    }

    @Test
    void testSolrAddress() {
        Assertions.assertEquals("http://localhost:8983/solr", SolrEnvironmentVariables.solrAddress());
    }

    @Test
    void testSolrCore() {
        Assertions.assertEquals("entando", SolrEnvironmentVariables.solrCore());
    }

}
