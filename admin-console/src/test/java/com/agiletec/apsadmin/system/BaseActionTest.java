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
package com.agiletec.apsadmin.system;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import java.util.Map;
import java.util.Optional;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.aps.system.services.tenants.TenantConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaseActionTest {
    
    @Mock
    private ITenantManager tenantManager;
    
    @InjectMocks
    private BaseAction baseAction;
    
    @Test
    void shouldCurrentTenantWorkFine() {
        try(MockedStatic<ApsTenantApplicationUtils> apsTenantApplicationUtils = Mockito.mockStatic(ApsTenantApplicationUtils.class)){
            apsTenantApplicationUtils.when(() -> ApsTenantApplicationUtils.getTenant()).thenReturn(Optional.empty());
            TenantConfig config = this.baseAction.getCurrentTenantConfig();
            Assertions.assertNull(config);
            Mockito.verifyNoInteractions(this.tenantManager);
            
            apsTenantApplicationUtils.reset();
            Mockito.reset(tenantManager);
            apsTenantApplicationUtils.when(() -> ApsTenantApplicationUtils.getTenant()).thenReturn(Optional.of("tenantx"));
            Mockito.when(tenantManager.getConfigOfReadyTenant("tenantx")).thenReturn(Optional.empty());
            config = this.baseAction.getCurrentTenantConfig();
            Assertions.assertNull(config);
            Mockito.verify(tenantManager, Mockito.times(1)).getConfigOfReadyTenant("tenantx");
            
            apsTenantApplicationUtils.reset();
            Mockito.reset(tenantManager);
            apsTenantApplicationUtils.when(() -> ApsTenantApplicationUtils.getTenant()).thenReturn(Optional.of("tenanty"));
            TenantConfig tc = new TenantConfig(Map.of("tenantCode","tenanty"));
            Mockito.when(tenantManager.getConfigOfReadyTenant("tenanty")).thenReturn(Optional.of(tc));
            config = this.baseAction.getCurrentTenantConfig();
            Assertions.assertNotNull(config);
            Mockito.verify(tenantManager, Mockito.times(1)).getConfigOfReadyTenant("tenanty");
        }
    }
    
}
