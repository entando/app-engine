/*
 * Copyright 2020-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpsolr;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.entando.entando.plugins.jpsolr.aps.system.solr.AdvContentSearchTest;
import org.entando.entando.plugins.jpsolr.aps.system.solr.FacetSearchEngineManagerIntegrationTest;
import org.entando.entando.plugins.jpsolr.aps.system.solr.SearchEngineManagerIntegrationTest;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for Solr connector");

        System.out.println("Test for Solr connector");
        
        //
        //suite.addTestSuite(AdvContentSearchTest.class);
        //suite.addTestSuite(FacetSearchEngineManagerIntegrationTest.class);
        suite.addTestSuite(SearchEngineManagerIntegrationTest.class);

        return suite;
    }

}
