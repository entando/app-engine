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
package org.entando.entando.aps.system.services.tenants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.dbcp2.BasicDataSource;
import org.assertj.core.api.Assertions;
import org.entando.entando.aps.system.init.InitializerManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantManagerTest {

    public final static String TENANT_CONFIGS = "[{\n"
            + "    \"tenantCode\": \"TE_nant1\",\n"
            + "    \"kcEnabled\": true,\n"
            + "    \"fqdns\": \"tenant1.com,tenant2.com\",\n"
            + "    \"context\": \"common-context\"\n,"
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
            + "}]\n";

    private String tenantConfigsWithCustomFields="[{\n"
            + "    \"tenantCode\": \"TE_nant1\",\n"
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
            + "    \"context\": \"context1\"\n,"
            + "    \"customField1\": \"custom_value_1\"\n,"
            + "    \"customField2\": \"custom_value_2\""
            + "}]";

    private String tenantWithPrimaryCodeConfigs="[{\n"
            + "    \"tenantCode\": \"primary\",\n"
            + "    \"fqdns\": \"tenant1.com,tenant2.com\",\n"
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
            + "}]\n";

    private String tenantConfigsWithoutContextField="[{\n"
            + "    \"tenantCode\": \"TE_nant1\",\n"
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
            + "    \"fqdns\": \"tenant1.com\"\n"
            + "}]";

    private String tenantConfigsWithEmptyContextField="[{\n"
            + "    \"tenantCode\": \"TE_nant1\",\n"
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
            + "    \"fqdns\": \"tenant1.com\",\n"
            + "    \"context\": \"\"\n"
            + "}]";

    private String errorToCheck = "The tenant '%s' is not ready ('UNKNOWN'), please visit health status endpoint to check";

    @Mock
    InitializerManager initializerManager;

    @Test
    void shouldAllOperationWorkFineWithConfigMapsWithCustomFields() throws Throwable {
        TenantManager tm = setUpTM(tenantConfigsWithCustomFields);

        Optional<TenantConfig> otc = tm.getConfigOfReadyTenant("TE_nant1");
        assertThat(otc).isNotEmpty();
        TenantConfig tc = otc.get();
        Optional<String> customValue1 = tc.getProperty("customField1");
        Optional<String> customValue2 = tc.getProperty("customField2");
        Optional<String> customValue3 = tc.getProperty("customField3");
        assertThat(customValue1).isNotEmpty().hasValue("custom_value_1");
        assertThat(customValue2).isNotEmpty().hasValue("custom_value_2");
        assertThat(customValue3).isEmpty();
    }

    @Test
    void shouldAllOperationWorkFineWithCorrectInput() throws Throwable {
        TenantManager tm = setUpTM(tenantConfigsWithCustomFields);

        Optional<TenantConfig> otc = tm.getConfigOfReadyTenant("TE_nant1");
        assertThat(otc).isNotEmpty();
        TenantConfig tc = otc.get();
        assertThat(tc.isKcEnabled()).isTrue();
        otc = tm.getTenantConfigByDomainAndContext("tenant1.com", "context1");
        assertThat(otc).isNotEmpty();
        tc = otc.get();
        assertThat(tc.getFqdns()).contains("tenant1.com");
        assertThat(tc.getContext()).isEqualTo("context1");
        BasicDataSource ds = (BasicDataSource)tm.getDatasource("TE_nant1");
        assertThat(ds.getDriverClassName()).isEqualTo("org.postgresql.Driver");
        assertThat(tm.exists("pippo")).isFalse();
        tm.release();

        ds = (BasicDataSource)tm.getDatasource("TE_nant_not_found");
        assertThat(ds).isNull();

    }

    @Test
    void shouldAllOperationWorkFineWithBadInput() throws Throwable {
        TenantManager tm = new TenantManager("[\"pippo\"pippo]", new ObjectMapper(), new TenantDataAccessor());
        Assertions.catchThrowableOfType(() -> tm.afterPropertiesSet(), JsonMappingException.class);

        Optional<TenantConfig> otc = tm.getConfigOfReadyTenant("TE_nant1");
        assertThat(otc).isEmpty();

        otc = tm.getTenantConfigByDomainAndContext("tenant2.com", "context2");
        assertThat(otc).isEmpty();

        BasicDataSource ds = (BasicDataSource)tm.getDatasource("TE_nant_not_found");
        assertThat(ds).isNull();
    }

    @Test
    void shouldInitThrowExceptionWithTenantCodeWithValuePrimary() throws Throwable {
        TenantManager tm = new TenantManager(tenantWithPrimaryCodeConfigs, new ObjectMapper(), new TenantDataAccessor());
        RuntimeException ex = Assertions.catchThrowableOfType(() -> tm.afterPropertiesSet(), RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("You cannot use 'primary' as tenant code");

        Optional<TenantConfig> otc = tm.getConfigOfReadyTenant("TE_nant1");
        assertThat(otc).isEmpty();

        otc = tm.getTenantConfigByDomainAndContext("tenant2.com", "context2");
        assertThat(otc).isEmpty();

        BasicDataSource ds = (BasicDataSource)tm.getDatasource("TE_nant_not_found");
        assertThat(ds).isNull();
    }

    @Test
    void shouldOperationThrowExceptionWithTenantNotInitiated() throws Throwable {
        TenantDataAccessor data = new TenantDataAccessor();
        data.getTenantStatuses().put("tenant2", TenantStatus.READY);
        TenantManager tm = new TenantManager(TENANT_CONFIGS, new ObjectMapper(), new TenantDataAccessor());
        tm.afterPropertiesSet();

        RuntimeException ex = Assertions.catchThrowableOfType(() -> tm.getConfigOfReadyTenant("TE_nant1"), RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo(String.format(errorToCheck, "TE_nant1"));

        ex = Assertions.catchThrowableOfType(() -> tm.getTenantConfigByDomainAndContext("tenant2.com", "common-context"), RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo(String.format(errorToCheck, "TE_nant1"));
        
        Optional<TenantConfig> otc1 = tm.getConfig("TE_nant1");
        assertThat(otc1).isNotEmpty();
        TenantConfig tc = otc1.get();
        assertThat(tc.isKcEnabled()).isTrue();
        assertThat(tc.getTenantCode()).isEqualTo("TE_nant1");

        BasicDataSource ds = (BasicDataSource)tm.getDatasource("TE_nant1");
        assertThat(ds.getDriverClassName()).isEqualTo("org.postgresql.Driver");

        Optional<TenantConfig> otc = tm.getConfigOfReadyTenant("TE_pippo123");
        assertThat(otc).isEmpty();

        otc = tm.getTenantConfigByDomainAndContext("pippo123.com", "pippo");
        assertThat(otc).isEmpty();

        ds = (BasicDataSource)tm.getDatasource("TE_nant_not_found");
        assertThat(ds).isNull();
    }

    @Test
    void shouldAllOperationWorkFineWithConfigMapsWithoutContextField() throws Throwable {
        TenantManager tm = setUpTM(tenantConfigsWithoutContextField);

        Optional<TenantConfig> otc = tm.getConfigOfReadyTenant("TE_nant1");
        assertThat(otc).isNotEmpty();
        TenantConfig tc = otc.get();

        Optional<String> contextValue = tc.getProperty("context");
        assertThat(contextValue).isEmpty();

        otc = tm.getTenantConfigByDomainAndContext("tenant1.com", null);
        assertThat(otc).isNotEmpty();

        otc = tm.getTenantConfigByDomainAndContext("tenant1.com", "");
        assertThat(otc).isNotEmpty();

        otc = tm.getTenantConfigByDomainAndContext("tenant1.com", "something");
        assertThat(otc).isNotEmpty();
    }

    @Test
    void shouldAllOperationWorkFineWithConfigMapsWithEmptyContextField() throws Throwable {
        TenantManager tm = setUpTM(tenantConfigsWithEmptyContextField);

        Optional<TenantConfig> otc = tm.getConfigOfReadyTenant("TE_nant1");
        assertThat(otc).isNotEmpty();
        TenantConfig tc = otc.get();

        Optional<String> contextValue = tc.getProperty("context");
        assertThat(contextValue).isNotEmpty().hasValue("");

        otc = tm.getTenantConfigByDomainAndContext("tenant1.com", null);
        assertThat(otc).isEmpty();

        otc = tm.getTenantConfigByDomainAndContext("tenant1.com", "");
        assertThat(otc).isNotEmpty();

        otc = tm.getTenantConfigByDomainAndContext("tenant1.com", "something");
        assertThat(otc).isEmpty();
    }

    @Test
    void testGetTenantCodeByDomain_exactMatch() throws Exception {
        TenantManager tm = setUpTM(TENANT_CONFIGS);

        String tenantCode = tm.getTenantCodeByDomain("tenant1.com");
        assertThat(tenantCode).isEqualTo("TE_nant1");

        tenantCode = tm.getTenantCodeByDomain("tenant2.com");
        assertThat(tenantCode).isEqualTo("TE_nant1");

        tenantCode = tm.getTenantCodeByDomain("unexistent.domain");
        assertThat(tenantCode).isNull();

        tenantCode = tm.getTenantCodeByDomain(null);
        assertThat(tenantCode).isNull();
    }

    private TenantManager setUpTM(String tenantConfigsWithCustomFields) throws Exception {
        TenantDataAccessor data = new TenantDataAccessor();
        TenantManager tm = new TenantManager(tenantConfigsWithCustomFields, new ObjectMapper(), data);
        tm.afterPropertiesSet();
        Map<String, TenantStatus> statuses = data.getTenantStatuses();
        data.getTenantStatuses().keySet().stream().forEach(k -> statuses.put(k, TenantStatus.READY));
        return tm;
    }
}
