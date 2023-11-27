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
package org.entando.entando.plugins.jpsolr;

import com.agiletec.aps.BaseTestCase;
import javax.servlet.ServletContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockServletContext;
import org.testcontainers.containers.GenericContainer;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public abstract class SolrBaseTestCase {
    
    private static GenericContainer solrContainer;
    
    private static ApplicationContext applicationContext;
    
    @SystemStub
    private static EnvironmentVariables environmentVariables;
    
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static void setApplicationContext(ApplicationContext applicationContextToSet) {
        applicationContext = applicationContextToSet;
    }
    
    @BeforeAll
    public static void startUp() throws Exception {
        solrContainer = SolrTestUtils.startContainer(solrContainer, environmentVariables);
        ServletContext srvCtx = new MockServletContext("", new FileSystemResourceLoader());
        applicationContext = new CustomConfigTestUtils().createApplicationContext(srvCtx);
        setApplicationContext(applicationContext);
    }

    @AfterAll
    public static void tearDown() throws Exception {
        BaseTestCase.tearDown();
    }
    
}
