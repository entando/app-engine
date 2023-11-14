/*
 * Copyright 2022-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TenantManager implements ITenantManager, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(TenantManager.class);
    
    private final String tenantsConfigAsString;
    private final ObjectMapper objectMapper;
    private final TenantDataAccessor tenantDataAccessor;
    
    @Autowired
    public TenantManager(@Value("${ENTANDO_TENANTS:}") String s, ObjectMapper o, TenantDataAccessor tenantDataAccessor){
        this.tenantsConfigAsString = s;
        this.objectMapper = o;
        this.tenantDataAccessor = tenantDataAccessor;
    }

    protected void release() {
        try {
            tenantDataAccessor.getTenantDataSources().values().forEach(this::destroyDataSource);
            initTenantsCodes();
            tenantDataAccessor.getTenantDataSources().clear();
        } catch (Exception e) {
            logger.error("Error releasing resources", e);
        }
    }

    private void destroyDataSource(DataSource dataSource) {
        try {
            if (dataSource instanceof BasicDataSource) {
                ((BasicDataSource) dataSource).close();
            }
        } catch(Exception ex) {
            logger.error("Error closing connection", ex);
        }
    }

    @Override
    public boolean exists(String tenantCode) {
        return this.getCodes().contains(tenantCode);
    }

    @Override
    public List<String> getCodes() {
        return new ArrayList<>(tenantDataAccessor.getTenantConfigs().keySet());
    }

    @Override
    public Map<String,TenantStatus> getStatuses() {
        return Map.copyOf(tenantDataAccessor.getTenantStatuses());
    }

    @Override
    public String getTenantCodeByDomain(String domain) {
        String tenantCode = tenantDataAccessor.getTenantConfigs().values().stream()
                .filter(v -> v.getFqdns().contains(domain))
                .map(tc -> tc.getTenantCode())
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse(getCodes().stream().filter(code -> StringUtils.equals(code, domain)).findFirst().orElse(null));
        if (logger.isDebugEnabled()) {
            logger.debug("From domain:'{}' retrieved tenantCode:'{}' from codes:'{}'",
                    domain, tenantCode, getCodes().stream().collect(Collectors.joining(",")));
        }
        return identityIfStatusReadyOrThrow(tenantCode);
    }

    @Override
    public Optional<TenantConfig> getTenantConfigByDomain(String domain) {
        return this.getConfigOfReadyTenant(this.getTenantCodeByDomain(domain));
    }

    @Override
    public DataSource getDatasource(String tenantCode) {
        return tenantDataAccessor.getTenantDatasource(tenantCode);
    }

    @Override
    public Optional<TenantConfig> getConfigOfReadyTenant(String tenantCode) {
        return Optional.ofNullable(tenantCode).map(this::identityIfStatusReadyOrThrow).map(tenantDataAccessor.getTenantConfigs()::get);
    }

    @Override
    public Optional<TenantConfig> getConfig(String tenantCode) {
        return Optional.ofNullable(tenantCode).map(tenantDataAccessor.getTenantConfigs()::get);
    }
    
    private void initTenantsCodes() throws Exception {
        if (!StringUtils.isBlank(this.tenantsConfigAsString)) {
            List<TenantConfig> list = this.objectMapper.readValue(tenantsConfigAsString, new TypeReference<List<Map<String,String>>>(){})
                    .stream()
                    .map(TenantConfig::new)
                    .collect(Collectors.toList());
            list.stream().filter(tc -> PRIMARY_CODE.equalsIgnoreCase(tc.getTenantCode())).findFirst().ifPresent(tc -> {
                logger.error("You cannot use 'primary' as tenant code");
                throw new RuntimeException("You cannot use 'primary' as tenant code");
            });
            list.stream().forEach(tc -> tenantDataAccessor.getTenantConfigs().put(tc.getTenantCode(), tc));
        }
    }

    private void initTenantStatuses() {
        tenantDataAccessor.getTenantConfigs().keySet().stream()
                .forEach(k -> tenantDataAccessor.getTenantStatuses().put(k, TenantStatus.UNKNOWN));
    }

    private String identityIfStatusReadyOrThrow(String tenantCode){
        if( tenantCode == null
                || !tenantDataAccessor.getTenantConfigs().containsKey(tenantCode)
                || TenantStatus.READY.equals(tenantDataAccessor.getTenantStatuses().get(tenantCode)) ) {
            return tenantCode;
        }
        throw new RuntimeException(String.format("Error status for tenant with code '%s' is not ready please visit health status endpoint to check", tenantCode));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initTenantsCodes();
        initTenantStatuses();
    }
}
