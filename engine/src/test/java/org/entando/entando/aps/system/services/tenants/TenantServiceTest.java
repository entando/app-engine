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

import static org.mockito.Mockito.when;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.entando.entando.aps.system.services.storage.IStorageManager;
import org.entando.entando.web.tenant.model.TenantDto;
import org.entando.entando.web.tenant.model.TenantStatsAndStatusesDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private ITenantManager tenantManager;
    @Mock
    private TenantDataAccessor tenantDataAccessor;
    @Mock
    private IStorageManager storageManager;

    private ITenantService tenantService;

    @BeforeEach
    public void setUp() {
        Mockito.reset(tenantManager, tenantDataAccessor, storageManager);
        tenantService = new TenantService(tenantManager, tenantDataAccessor, storageManager);
    }

    @Test
    void shouldCurrentTenantWorkFine() {
        try(MockedStatic<ApsTenantApplicationUtils> apsTenantApplicationUtils = Mockito.mockStatic(ApsTenantApplicationUtils.class)){
            // primary case of CDS disabled
            apsTenantApplicationUtils.when(() -> ApsTenantApplicationUtils.getTenant()).thenReturn(Optional.empty());
            when(storageManager.getResourceUrl("",false)).thenReturn("/cmsresource");
            when(tenantManager.getConfigOfReadyTenant("primary")).thenReturn(Optional.empty());

            TenantDto primary = tenantService.getCurrentTenant();
            Assertions.assertTrue(primary.isPrimary());
            Assertions.assertEquals("primary" ,primary.getCode());
            Assertions.assertNull(primary.getResourceRootUrl());
            Assertions.assertEquals("/cmsresource", primary.getResourceRootPath());

            // primary case of CDS enabled
            apsTenantApplicationUtils.reset();
            Mockito.reset(tenantManager, storageManager);
            apsTenantApplicationUtils.when(() -> ApsTenantApplicationUtils.getTenant()).thenReturn(Optional.empty());
            when(storageManager.getResourceUrl("",false)).thenReturn("http://cds-primary/public");
            when(tenantManager.getConfigOfReadyTenant("primary")).thenReturn(Optional.empty());

            TenantDto primaryCds = tenantService.getCurrentTenant();
            Assertions.assertTrue(primaryCds.isPrimary());
            Assertions.assertEquals("primary" ,primaryCds.getCode());
            Assertions.assertNull(primaryCds.getResourceRootPath());
            Assertions.assertEquals("http://cds-primary/public", primaryCds.getResourceRootUrl());

            // tenant case of CDS enabled
            apsTenantApplicationUtils.reset();
            Mockito.reset(tenantManager, storageManager);
            apsTenantApplicationUtils.when(() -> ApsTenantApplicationUtils.getTenant()).thenReturn(Optional.of("tenant1"));
            when(storageManager.getResourceUrl("",false)).thenReturn("http://cds-tenant1/public");
            TenantConfig tc = new TenantConfig(Map.of("tenantCode","tenant1"));
            when(tenantManager.getConfigOfReadyTenant("tenant1")).thenReturn(Optional.of(tc));

            TenantDto tenant = tenantService.getCurrentTenant();
            Assertions.assertFalse(tenant.isPrimary());
            Assertions.assertEquals("tenant1" ,tenant.getCode());
            Assertions.assertNull(tenant.getResourceRootPath());
            Assertions.assertEquals("http://cds-tenant1/public", tenant.getResourceRootUrl());


        }
    }

    @Test
    void shouldGetTenantWorkFineWithRightInput() {
        try(MockedStatic<ApsTenantApplicationUtils> apsTenantApplicationUtils = Mockito.mockStatic(ApsTenantApplicationUtils.class)){
            // primary case of CDS disabled
            apsTenantApplicationUtils.when(() -> ApsTenantApplicationUtils.getTenant()).thenReturn(Optional.empty());
            when(storageManager.getResourceUrl("",false)).thenReturn("/cmsresource");
            when(tenantManager.getConfigOfReadyTenant("primary")).thenReturn(Optional.empty());

            Optional<TenantDto> primaryRes = tenantService.getTenant("primary");
            Assertions.assertTrue(primaryRes.isPresent());
            TenantDto primary = primaryRes.get();
            Assertions.assertEquals("primary" ,primary.getCode());
            Assertions.assertNull(primary.getResourceRootUrl());
            Assertions.assertEquals("/cmsresource", primary.getResourceRootPath());

            // primary case of CDS enabled
            apsTenantApplicationUtils.reset();
            Mockito.reset(tenantManager, storageManager);
            apsTenantApplicationUtils.when(() -> ApsTenantApplicationUtils.getTenant()).thenReturn(Optional.empty());
            when(storageManager.getResourceUrl("",false)).thenReturn("http://cds-primary/public");
            when(tenantManager.getConfigOfReadyTenant("primary")).thenReturn(Optional.empty());

            Optional<TenantDto> primaryCdsRes = tenantService.getTenant("primary");
            Assertions.assertTrue(primaryCdsRes.isPresent());
            TenantDto primaryCds = primaryCdsRes.get();
            Assertions.assertTrue(primaryCds.isPrimary());
            Assertions.assertEquals("primary" ,primaryCds.getCode());
            Assertions.assertNull(primaryCds.getResourceRootPath());
            Assertions.assertEquals("http://cds-primary/public", primaryCds.getResourceRootUrl());

            // tenant case of CDS enabled
            apsTenantApplicationUtils.reset();
            Mockito.reset(tenantManager, storageManager);
            apsTenantApplicationUtils.when(() -> ApsTenantApplicationUtils.getTenant()).thenReturn(Optional.of("tenant1"));
            when(storageManager.getResourceUrl("",false)).thenReturn("http://cds-tenant1/public");
            TenantConfig tc = new TenantConfig(Map.of("tenantCode","tenant1"));
            when(tenantManager.getConfigOfReadyTenant("tenant1")).thenReturn(Optional.of(tc));

            Optional<TenantDto> tenantRes = tenantService.getTenant("tenant1");
            Assertions.assertTrue(tenantRes.isPresent());
            TenantDto tenant = tenantRes.get();
            Assertions.assertFalse(tenant.isPrimary());
            Assertions.assertEquals("tenant1" ,tenant.getCode());
            Assertions.assertNull(tenant.getResourceRootPath());
            Assertions.assertEquals("http://cds-tenant1/public", tenant.getResourceRootUrl());


        }
    }

    @Test
    void shouldGetTenantWorkFineWithWrongInput() {
        Optional<TenantDto> tenantRes = tenantService.getTenant(null);
        Assertions.assertTrue(tenantRes.isEmpty());
        tenantRes = tenantService.getTenant("");
        Assertions.assertTrue(tenantRes.isEmpty());
        tenantRes = tenantService.getTenant("xxx");
        Assertions.assertTrue(tenantRes.isEmpty());

    }

    @Test
    void shouldGetTenantsWorkFine() {
        // CDS disabled
        List<TenantDto> tenants = tenantService.getTenants();
        Assertions.assertFalse(tenants.isEmpty());
        Assertions.assertEquals(1, tenants.size());
        Assertions.assertEquals("primary", tenants.get(0).getCode());

        // Cds enabled with 1 tenant
        TenantConfig tc = new TenantConfig(Map.of("tenantCode","tenant1"));
        when(tenantManager.getConfigOfReadyTenant("tenant1")).thenReturn(Optional.of(tc));
        when(tenantManager.getCodes()).thenReturn(Arrays.asList("tenant1"));


        tenants = tenantService.getTenants();
        Assertions.assertFalse(tenants.isEmpty());
        Assertions.assertEquals(2, tenants.size());
        Assertions.assertEquals("tenant1", tenants.get(0).getCode());
        Assertions.assertEquals("primary", tenants.get(1).getCode());

    }

    @Test
    void shouldGetTenantStatsAndStatusesWorkFine() {
        // CDS disabled

        // Cds enabled with 1 tenant
        when(tenantManager.getStatuses()).thenReturn(Map.of("t1",
                TenantStatus.UNKNOWN, "t2", TenantStatus.PENDING, "t3",  TenantStatus.READY, "t4", TenantStatus.FAILED));
        when(tenantDataAccessor.getTenantConfigs())
                .thenReturn(Map.of("t1",new TenantConfig(Map.of("tenantCode", "t1")),
                        "t2",new TenantConfig(Map.of("tenantCode", "t2")),
                        "t3",new TenantConfig(Map.of("tenantCode", "t3")),
                        "t4",new TenantConfig(Map.of("tenantCode", "t4",  "initializationAtStartRequired", "true"))
                ));
        
        TenantStatsAndStatusesDto tenants = tenantService.getTenantStatsAndStatuses();
        Assertions.assertNotNull(tenants);
        Assertions.assertEquals(4, tenants.getStats().getCount());
        Assertions.assertEquals(1, tenants.getStats().getPending());
        Assertions.assertEquals(1, tenants.getStats().getUnknown());
        Assertions.assertEquals(1, tenants.getStats().getReady());
        Assertions.assertEquals(1, tenants.getStats().getFailed());
        Assertions.assertEquals(TenantStatus.UNKNOWN, tenants.getAdditionalStatuses().get("t1"));
        Assertions.assertNull(tenants.getMandatoryStatuses().get("t2"));
        Assertions.assertEquals(TenantStatus.PENDING, tenants.getAdditionalStatuses().get("t2"));
        Assertions.assertNull(tenants.getAdditionalStatuses().get("t4"));
        Assertions.assertEquals(TenantStatus.FAILED, tenants.getMandatoryStatuses().get("t4"));

    }

}
