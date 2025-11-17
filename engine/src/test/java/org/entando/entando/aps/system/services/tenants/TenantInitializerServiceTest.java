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
package org.entando.entando.aps.system.services.tenants;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.services.baseconfig.BaseConfigManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.ServletContext;
import org.entando.entando.aps.system.init.InitializerManager;
import org.entando.entando.aps.system.services.tenants.ITenantInitializerService.InitializationTenantFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(MockitoExtension.class)
class TenantInitializerServiceTest {

    @Mock
    private InitializerManager initializerManager;
    @Mock
    private ServletContext svCtx;
    @Mock
    private BaseConfigManager conf;
    @Mock
    private WebApplicationContext wac;

    private String tenantConfigsWithRequiredFieldTrue ="[{\n"
            + "    \"tenantCode\": \"TE_nant1\",\n"
            + "    \"initializationAtStartRequired\": true,\n"
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
            + "    \"dbPassword\": \"db_password_2\",\n"
            + "    \"fqdns\": \"tenant1.com\"\n,"
            + "    \"customField1\": \"custom_value_1\"\n,"
            + "    \"customField2\": \"custom_value_2\""
            + "}]";


    @Test
    void shouldStartAsynchInitializeTenantsPutReadyAllTenants() throws Throwable {
        TenantDataAccessor tenantDataAccessor = initTenantDataAccessor(TenantManagerTest.TENANT_CONFIGS);
        when(svCtx.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(wac);
        when(wac.getBean(BaseConfigManager.class)).thenReturn(conf);
        when(wac.getBeansOfType(RefreshableBeanTenantAware.class)).thenReturn(new HashMap<>());
        doNothing().when(initializerManager).initTenant(any(), any());
        ITenantInitializerService srv = new TenantInitializerService(tenantDataAccessor, initializerManager, null);
        srv.startTenantsInitialization(svCtx, null).join();

        Assertions.assertEquals(2, tenantDataAccessor.getTenantStatuses().values().stream().filter(TenantStatus.READY::equals).count());
    }


    @Test
    void shouldStartInitializeTenantsManageErrorsWithFailedStatusForNotRequiredTenants() throws Throwable {
        TenantDataAccessor tenantDataAccessor = initTenantDataAccessor(TenantManagerTest.TENANT_CONFIGS);
        doNothing().when(initializerManager).initTenant(any(), any());
        ITenantInitializerService srv = new TenantInitializerService(tenantDataAccessor, initializerManager, null);
        srv.startTenantsInitialization(svCtx, InitializationTenantFilter.NOT_REQUIRED_INIT_AT_START).join();

        Assertions.assertEquals(2, tenantDataAccessor.getTenantStatuses().values().stream().filter(TenantStatus.FAILED::equals).count());
    }

    @Test
    void shouldStartTenantsInitialization_FilterCorrectly() throws Throwable {
        TenantDataAccessor tenantDataAccessor = initTenantDataAccessor(TenantManagerTest.TENANT_CONFIGS);
        when(svCtx.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(wac);
        when(wac.getBean(BaseConfigManager.class)).thenReturn(conf);
        when(wac.getBeansOfType(RefreshableBeanTenantAware.class)).thenReturn(new HashMap<>());
        doNothing().when(initializerManager).initTenant(any(), any());
        ITenantInitializerService srv = new TenantInitializerService(tenantDataAccessor, initializerManager, null);
        srv.startTenantsInitialization(svCtx, InitializationTenantFilter.REQUIRED_INIT_AT_START).join();
        Assertions.assertEquals(0, tenantDataAccessor.getTenantStatuses().values().stream().filter(TenantStatus.READY::equals).count());
        srv.startTenantsInitialization(svCtx, InitializationTenantFilter.NOT_REQUIRED_INIT_AT_START).join();
        Assertions.assertEquals(2, tenantDataAccessor.getTenantStatuses().values().stream().filter(TenantStatus.READY::equals).count());
    }

    @Test
    void shouldStartTenantsInitialization_throwErrorForRequiredTenantsNotInitialized() throws Throwable {
        TenantDataAccessor tenantDataAccessor = initTenantDataAccessor(tenantConfigsWithRequiredFieldTrue);
        doThrow(new Exception("genericError")).when(initializerManager).initTenant(any(), any());
        ITenantInitializerService srv = new TenantInitializerService(tenantDataAccessor, initializerManager, null);
        Assertions.assertThrows(RuntimeException.class,
                () -> srv.startTenantsInitialization(svCtx, InitializationTenantFilter.REQUIRED_INIT_AT_START).join());
    }


    private TenantDataAccessor initTenantDataAccessor(String tenantsConfig) throws JsonProcessingException {
        TenantDataAccessor accessor = new TenantDataAccessor();
        ObjectMapper om = new ObjectMapper();
        List<TenantConfig> list = om.readValue(tenantsConfig, new TypeReference<List<Map<String,String>>>(){})
                        .stream()
                        .map(TenantConfig::new)
                        .collect(Collectors.toList());
        list.stream().forEach(tc -> accessor.getTenantConfigs().put(tc.getTenantCode(), tc));
        accessor.getTenantConfigs().keySet().stream()
                .forEach(k -> accessor.getTenantStatuses().put(k, TenantStatus.UNKNOWN));

        return accessor;
    }
}
