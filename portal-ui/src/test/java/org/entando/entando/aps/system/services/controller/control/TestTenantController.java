/*
 * Copyright 2022-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.system.services.controller.control;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.RefreshableBean;
import com.agiletec.aps.system.services.baseconfig.BaseConfigManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import jakarta.servlet.ServletContext;
import org.entando.entando.aps.system.init.InitializerManager;
import org.entando.entando.aps.system.services.tenants.ITenantInitializerService;
import org.entando.entando.aps.system.services.tenants.ITenantInitializerService.InitializationTenantFilter;
import org.entando.entando.aps.system.services.tenants.RefreshableBeanTenantAware;
import org.entando.entando.aps.system.services.tenants.TenantInitializerService;
import org.entando.entando.aps.system.services.tenants.TenantDataAccessor;
import org.entando.entando.aps.system.services.tenants.TenantManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.mock.web.MockHttpServletRequest;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.EntThreadLocal;
import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.services.controller.ControllerManager;
import com.agiletec.aps.system.services.controller.control.ControlServiceInterface;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author E.Santoboni
 */
class TestTenantController extends BaseTestCase {

    private ControlServiceInterface tenantController;
    private static final String ENTANDO_TENANTS = "[{\n"
            + "    \"tenantCode\": \"tenant1\",\n"
            + "    \"fqdns\": \"tenant1.test.serv.run\",\n"
            + "    \"kcEnabled\": true,\n"
            + "    \"kcAuthUrl\": \"http://tenant1.test.nip.io/auth\",\n"
            + "    \"kcRealm\": \"tenant1\",\n"
            + "    \"kcClientId\": \"quickstart\",\n"
            + "    \"kcClientSecret\": \"secret1\",\n"
            + "    \"kcPublicClientId\": \"entando-web\",\n"
            + "    \"kcSecureUris\": \"\",\n"
            + "    \"kcDefaultAuthorizations\": \"\",\n"
            + "    \"dbDriverClassName\": \"org.postgresql.Driver\",\n"
            + "    \"dbUrl\": \"jdbc:postgresql://testDbServer:5432/tenantDb1\",\n"
            + "    \"dbUsername\": \"db_user_2\",\n"
            + "    \"dbPassword\": \"db_password_2\"\n"
            + "}, {\n"
            + "    \"tenantCode\": \"tenant2\",\n"
            + "    \"kcEnabled\": true,\n"
            + "    \"kcAuthUrl\": \"http://tenant2.test.nip.io/auth\",\n"
            + "    \"kcRealm\": \"tenant2\",\n"
            + "    \"kcClientId\": \"quickstart\",\n"
            + "    \"kcClientSecret\": \"secret2\",\n"
            + "    \"kcPublicClientId\": \"entando-web\",\n"
            + "    \"kcSecureUris\": \"\",\n"
            + "    \"kcDefaultAuthorizations\": \"\",\n"
            + "    \"dbDriverClassName\": \"org.postgresql.Driver\",\n"
            + "    \"dbUrl\": \"jdbc:postgresql://testDbServer:5432/tenantDb2\",\n"
            + "    \"dbUsername\": \"db_user_1\",\n"
            + "    \"dbPassword\": \"db_password_1\"\n"
            + "}]";
    @BeforeAll
    public static void setUp() throws Exception {
        BaseTestCase.setUp();
        recreateTenantManager(ENTANDO_TENANTS);
    }

    @AfterAll
    public static void tearDown() throws Exception {
        BaseTestCase.tearDown();
        recreateTenantManager("");

    }

    @BeforeEach
    void init() throws Exception {
        try {
            this.tenantController = this.getApplicationContext().getBean(TenantController.class);
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }

    @Test
    void testService_1() throws EntException {
        EntThreadLocal.clear();
        RequestContext reqCtx = this.createExtRequestContext("tenant1.test.serv.run", this.getApplicationContext());
        int status = this.tenantController.service(reqCtx, ControllerManager.CONTINUE);
        Assertions.assertEquals(ControllerManager.CONTINUE, status);
        Assertions.assertEquals("tenant1", EntThreadLocal.get(ITenantManager.THREAD_LOCAL_TENANT_CODE));
    }

    @Test
    void testService_2() throws EntException {
        EntThreadLocal.clear();
        RequestContext reqCtx = this.createExtRequestContext("test.serv.run", this.getApplicationContext());
        int status = this.tenantController.service(reqCtx, ControllerManager.CONTINUE);
        Assertions.assertEquals(ControllerManager.CONTINUE, status);
        Assertions.assertNull(EntThreadLocal.get(ITenantManager.THREAD_LOCAL_TENANT_CODE));
    }

    public RequestContext createExtRequestContext(String serverName, ApplicationContext applicationContext) {
        RequestContext reqCtx = this.getRequestContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName(serverName);
        request.addHeader("Host", serverName);
        request.setContextPath("/Entando");
        request.setAttribute(RequestContext.REQCTX, reqCtx);
        request.setSession(reqCtx.getRequest().getSession());
        reqCtx.setRequest(request);
        return reqCtx;
    }


    private static void recreateTenantManager(String tenants) throws Exception {
        ApplicationContext applicationContext = BaseTestCase.getApplicationContext();
        DefaultSingletonBeanRegistry registry = (DefaultSingletonBeanRegistry) applicationContext.getAutowireCapableBeanFactory();

        ObjectMapper om = applicationContext.getBean(ObjectMapper.class);
        TenantDataAccessor tenantData = applicationContext.getBean(TenantDataAccessor.class);
        TenantManager tm = new TenantManager(tenants, om, tenantData);
        tm.afterPropertiesSet();
        InitializerManager im = Mockito.mock(InitializerManager.class);
        WebApplicationContext wac = Mockito.mock(WebApplicationContext.class);
        BaseConfigManager conf = Mockito.mock(BaseConfigManager.class);
        ServletContext svCtx = Mockito.mock(ServletContext.class);
        when(svCtx.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(wac);
        when(wac.getBean(BaseConfigManager.class)).thenReturn(conf);
        when(wac.getBeansOfType(RefreshableBeanTenantAware.class)).thenReturn(new HashMap<>());
        doNothing().when(im).initTenant(any(), any());
        ITenantInitializerService srv = new TenantInitializerService(tenantData, im, null);
        srv.startTenantsInitialization(svCtx, InitializationTenantFilter.NOT_REQUIRED_INIT_AT_START).join();

        registry.destroySingleton("tenantManager");
        registry.registerSingleton("tenantManager", tm);
    }
}