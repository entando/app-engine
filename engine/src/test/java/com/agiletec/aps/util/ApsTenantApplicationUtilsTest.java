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

import java.util.Optional;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.assertj.core.api.Assertions;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(MockitoExtension.class)
class ApsTenantApplicationUtilsTest {

    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private ITenantManager tenantManager;

    @Test
    void shouldExtractCorrectTenantDomainWithEveryInput(){

        try(MockedStatic<ApsWebApplicationUtils> apsWebApplicationUtils = Mockito.mockStatic(ApsWebApplicationUtils.class)){
            apsWebApplicationUtils.when(() -> ApsWebApplicationUtils.getBean(Mockito.eq(ITenantManager.class), Mockito.<ServletContext>any())).thenReturn(tenantManager);

            Mockito.when(httpServletRequest.getHeader("X-ENTANDO-TENANTCODE")).thenReturn("tenant2");
            Mockito.when(httpServletRequest.getServerName()).thenReturn("www.test.com");
            Mockito.when(tenantManager.getTenantCodeByDomain("www.test.com")).thenReturn("wwwCode");
            Optional<String> res= ApsTenantApplicationUtils.extractCurrentTenantCode(httpServletRequest);
            Assertions.assertThat(res.get()).isNotEmpty().isEqualTo("tenant2");

            Mockito.reset(httpServletRequest);
            Mockito.when(httpServletRequest.getHeader("X-ENTANDO-TENANTCODE")).thenReturn("primary");
            Mockito.when(httpServletRequest.getServerName()).thenReturn("www.test.com");
            Mockito.when(tenantManager.getTenantCodeByDomain("www.test.com")).thenReturn("wwwCode");
            res= ApsTenantApplicationUtils.extractCurrentTenantCode(httpServletRequest);
            Assertions.assertThat(res).isNotPresent().isEmpty();

            Mockito.reset(httpServletRequest);
            Mockito.when(httpServletRequest.getServerName()).thenReturn("www.test.com");
            Mockito.when(tenantManager.getTenantCodeByDomain("www.test.com")).thenReturn("wwwCode");
            res= ApsTenantApplicationUtils.extractCurrentTenantCode(httpServletRequest);
            Assertions.assertThat(res.get()).isNotEmpty().isEqualTo("wwwCode");

            Mockito.reset(httpServletRequest);
            Mockito.when(httpServletRequest.getServerName()).thenReturn("code.test.com");
            Mockito.when(tenantManager.getTenantCodeByDomain("code.test.com")).thenReturn("tCode");
            res= ApsTenantApplicationUtils.extractCurrentTenantCode(httpServletRequest);
            Assertions.assertThat(res.get()).isNotEmpty().isEqualTo("tCode");

            Mockito.reset(httpServletRequest);
            Mockito.when(httpServletRequest.getServerName()).thenReturn("localhost");
            Mockito.when(tenantManager.getTenantCodeByDomain("localhost")).thenReturn(null);
            res= ApsTenantApplicationUtils.extractCurrentTenantCode(httpServletRequest);
            Assertions.assertThat(res).isNotPresent();
        }
    }

    @Test
    void shouldNotCreateSessionWhenExtractingTenantFromDomain() {
        MockServletContext servletContext = new MockServletContext();
        WebApplicationContext wac = Mockito.mock(WebApplicationContext.class);
        Mockito.when(wac.getBean(ITenantManager.class)).thenReturn(tenantManager);
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        request.setServerName("www.test.com");
        Mockito.when(tenantManager.getTenantCodeByDomain("www.test.com")).thenReturn("wwwCode");

        Optional<String> res = ApsTenantApplicationUtils.extractCurrentTenantCode(request);

        Assertions.assertThat(res).contains("wwwCode");
        Assertions.assertThat(request.getSession(false)).isNull();
    }

    @Test
    void shouldGetSetRemoveWorkCorrectTenantCodeWithEveryInput() {

        Optional<String> tenant = ApsTenantApplicationUtils.getTenant();
        Assertions.assertThat(tenant).isNotPresent();

        ApsTenantApplicationUtils.setTenant("tCode");
        tenant = ApsTenantApplicationUtils.getTenant();
        Assertions.assertThat(tenant.get()).isNotEmpty().isEqualTo("tCode");

        ApsTenantApplicationUtils.removeTenant();
        tenant = ApsTenantApplicationUtils.getTenant();
        Assertions.assertThat(tenant).isNotPresent();

    }


}
