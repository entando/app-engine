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
import jakarta.servlet.http.HttpServletRequest;

import org.assertj.core.api.Assertions;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApsTenantApplicationUtilsTest {

    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private ITenantManager tenantManager;

    @Test
    void shouldExtractCorrectTenantDomainWithEveryInput(){

        try(MockedStatic<ApsWebApplicationUtils> apsWebApplicationUtils = Mockito.mockStatic(ApsWebApplicationUtils.class)){
            apsWebApplicationUtils.when(() -> ApsWebApplicationUtils.getBean(ITenantManager.class,httpServletRequest)).thenReturn(tenantManager);

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
