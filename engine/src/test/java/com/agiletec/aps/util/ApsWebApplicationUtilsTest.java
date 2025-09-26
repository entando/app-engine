/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package com.agiletec.aps.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.entando.entando.aps.system.services.tenants.TenantDataAccessor;
import org.entando.entando.aps.system.services.tenants.TenantManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.WebApplicationContext;
import jakarta.servlet.jsp.PageContext;

@ExtendWith(MockitoExtension.class)
class ApsWebApplicationUtilsTest {

    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private HttpSession httpSession;
    @Mock
    private ServletContext servletContext;
    @Mock
    private WebApplicationContext wac;
    @Mock
    private PageContext pageContext;

    @Test
    void shouldExtractBeanWithTypeFromDifferentContextAndManageError(){
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> {ApsWebApplicationUtils.getBean(TenantManager.class, servletContext);});

        Mockito.when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(wac);

        Mockito.when(wac.getBean(TenantManager.class))
                .thenReturn(null);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> {ApsWebApplicationUtils.getBean(TenantManager.class, servletContext);});

        Mockito.when(wac.getBean(TenantManager.class)).thenReturn(new TenantManager("{}", new ObjectMapper(), new TenantDataAccessor()));
        TenantManager tm = ApsWebApplicationUtils.getBean(TenantManager.class, servletContext);
        Assertions.assertNotNull(tm);

        Mockito.when(pageContext.getServletContext()).thenReturn(servletContext);
        tm = ApsWebApplicationUtils.getBean(TenantManager.class, pageContext);
        Assertions.assertNotNull(tm);

        Mockito.when(httpServletRequest.getSession()).thenReturn(httpSession);
        Mockito.when(httpSession.getServletContext()).thenReturn(servletContext);
        tm = ApsWebApplicationUtils.getBean(TenantManager.class, httpServletRequest);
        Assertions.assertNotNull(tm);
    }

    @Test
    void shouldExtractBeanWithNameAndTypeFromDifferentContextAndManageError(){
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> {ApsWebApplicationUtils.getBean("tenantManager", TenantManager.class, servletContext);});

        Mockito.when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(wac);

        Mockito.when(wac.getBean("tenantManager", TenantManager.class))
                .thenReturn(null);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> {ApsWebApplicationUtils.getBean("tenantManager", TenantManager.class, servletContext);});

        Mockito.when(wac.getBean("tenantManager", TenantManager.class))
                .thenReturn(new TenantManager("{}", new ObjectMapper(), new TenantDataAccessor()));
        TenantManager tm = ApsWebApplicationUtils.getBean("tenantManager", TenantManager.class, servletContext);
        Assertions.assertNotNull(tm);

        Mockito.when(pageContext.getServletContext()).thenReturn(servletContext);
        tm = ApsWebApplicationUtils.getBean("tenantManager", TenantManager.class, pageContext);
        Assertions.assertNotNull(tm);

        Mockito.when(httpServletRequest.getSession()).thenReturn(httpSession);
        Mockito.when(httpSession.getServletContext()).thenReturn(servletContext);
        tm = ApsWebApplicationUtils.getBean("tenantManager", TenantManager.class, httpServletRequest);
        Assertions.assertNotNull(tm);
    }

}
