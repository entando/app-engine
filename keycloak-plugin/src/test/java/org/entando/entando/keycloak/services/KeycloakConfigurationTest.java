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
package org.entando.entando.keycloak.services;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.entando.entando.aps.system.services.tenants.TenantConfig;
import org.entando.entando.aps.system.services.tenants.TenantManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeycloakConfigurationTest {

    @Mock
    private TenantManager tenantManager;

    @BeforeEach
    void setUp(){
        Mockito.reset(tenantManager);
    }

    @AfterAll
    public static void tearDown(){
        ApsTenantApplicationUtils.removeTenant();
    }

    @Test
    void shouldConfigManageDefaultWhenTenantNotPresent() {
        KeycloakConfiguration kc = new KeycloakConfiguration();
        kc.setTenantManager(tenantManager);

        // we need this because there is no filter or http request
        ApsTenantApplicationUtils.setTenant("my-test-tenant");

        Mockito.when(tenantManager.getConfigOfReadyTenant("my-test-tenant")).thenReturn(Optional.empty());
        kc.setTenantManager(tenantManager);


        kc.setEnabled(true);
        kc.setClientId("default-client-id");
        kc.setClientSecret("default-client-secret");
        kc.setAuthUrl("/default-auth-url");
        kc.setSecureUris("/default-secure-uris");
        kc.setDefaultAuthorizations("default-auths");
        kc.setRealm("default-realm");
        kc.setPublicClientId("default-public-client-id");

        Assertions.assertTrue(kc.isEnabled());
        Assertions.assertEquals("default-client-id", kc.getClientId());
        Assertions.assertEquals("default-client-secret",kc.getClientSecret());
        Assertions.assertEquals("/default-auth-url", kc.getAuthUrl());
        Assertions.assertEquals("/default-secure-uris", kc.getSecureUris());
        Assertions.assertEquals("default-auths", kc.getDefaultAuthorizations());
        Assertions.assertEquals("default-realm", kc.getRealm());
        Assertions.assertEquals("default-public-client-id", kc.getPublicClientId());

    }
    @Test
    void shouldConfigManageTenantsOrThrows(){
        Map<String,String> map = Map.of(
                "kcEnabled","True",
                "kcRealm","tenant-realm",
                "kcClientId","tenant-client-id",
                "kcClientSecret","default-client-secret",
                "kcPublicClientId","tenant-public-client-id",
                "kcAuthUrl","/tenant-auth-url",
                "kcSecureUris","/tenant-secure-uris",
                "kcDefaultAuthorizations", "tenant-default-auth");

        ApsTenantApplicationUtils.setTenant("my-test-tenant");
        KeycloakConfiguration kc = new KeycloakConfiguration();
        TenantConfig tc = new TenantConfig(map);
        Mockito.when(tenantManager.getConfigOfReadyTenant("my-test-tenant")).thenReturn(Optional.ofNullable(tc));
        kc.setTenantManager(tenantManager);

        Assertions.assertTrue(kc.isEnabled());
        Assertions.assertEquals("tenant-realm", kc.getRealm());
        Assertions.assertEquals("tenant-client-id", kc.getClientId());
        Assertions.assertEquals("default-client-secret", kc.getClientSecret());
        Assertions.assertEquals("tenant-public-client-id", kc.getPublicClientId());
        Assertions.assertEquals("tenant-client-id", kc.getClientId());
        Assertions.assertEquals("/tenant-auth-url", kc.getAuthUrl());
        Assertions.assertEquals("/tenant-secure-uris", kc.getSecureUris());
        Assertions.assertEquals("tenant-default-auth", kc.getDefaultAuthorizations());

    }

    @Test
    void shouldConfigManageThrows(){
        ApsTenantApplicationUtils.setTenant("my-test-tenant");
        KeycloakConfiguration kc = new KeycloakConfiguration();
        TenantConfig tc = new TenantConfig(new HashMap<>());
        Mockito.when(tenantManager.getConfigOfReadyTenant("my-test-tenant")).thenReturn(Optional.ofNullable(tc));
        kc.setTenantManager(tenantManager);

        Assertions.assertFalse(kc.isEnabled());
        Assertions.assertThrows(IllegalArgumentException.class, () -> kc.getRealm());
        Assertions.assertThrows(IllegalArgumentException.class, () -> kc.getClientId());
        Assertions.assertThrows(IllegalArgumentException.class, () -> kc.getClientSecret());
        Assertions.assertThrows(IllegalArgumentException.class, () -> kc.getPublicClientId());
        Assertions.assertThrows(IllegalArgumentException.class, () -> kc.getAuthUrl());
        Assertions.assertNull(kc.getSecureUris());
        Assertions.assertNull(kc.getDefaultAuthorizations());
    }

    @Test
    void shouldConfigManageThrowsErrorWithConfigsEmpty(){
        Map<String,String> map = Map.of(
                "kcEnabled"," ",
                "kcRealm","",
                "kcClientId"," ",
                "kcClientSecret","",
                "kcPublicClientId"," ",
                "kcAuthUrl","",
                "kcSecureUris","",
                "kcDefaultAuthorizations", "");

        ApsTenantApplicationUtils.setTenant("my-test-tenant");
        KeycloakConfiguration kc = new KeycloakConfiguration();
        TenantConfig tc = new TenantConfig(map);
        Mockito.when(tenantManager.getConfigOfReadyTenant("my-test-tenant")).thenReturn(Optional.ofNullable(tc));
        kc.setTenantManager(tenantManager);

        Assertions.assertFalse(kc.isEnabled());
        Assertions.assertThrows(IllegalArgumentException.class, () -> kc.getRealm());
        Assertions.assertThrows(IllegalArgumentException.class, () -> kc.getClientId());
        Assertions.assertThrows(IllegalArgumentException.class, () -> kc.getClientSecret());
        Assertions.assertThrows(IllegalArgumentException.class, () -> kc.getPublicClientId());
        Assertions.assertThrows(IllegalArgumentException.class, () -> kc.getAuthUrl());
        Assertions.assertNull(kc.getSecureUris());
        Assertions.assertNull(kc.getDefaultAuthorizations());
    }

}
