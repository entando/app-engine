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

import static org.entando.entando.aps.system.services.tenants.ITenantManager.PRIMARY_CODE;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.storage.IStorageManager;
import org.entando.entando.web.tenant.model.TenantDto;
import org.entando.entando.web.tenant.model.TenantStatsAndStatusesDto;
import org.entando.entando.web.tenant.model.TenantStatsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class TenantService implements ITenantService {

    private final ITenantManager tenantManager;
    private final TenantDataAccessor tenantAccessor;
    private final IStorageManager storageManager;

    @Autowired
    public TenantService(ITenantManager tenantManager, TenantDataAccessor accessor, IStorageManager storageManager){
        this.tenantManager = tenantManager;
        this.tenantAccessor = accessor;
        this.storageManager = storageManager;
    }

    public TenantDto getCurrentTenant() {
        return tenantManager.getConfigOfReadyTenant(ApsTenantApplicationUtils.getTenant().orElse(PRIMARY_CODE))
                .map(this::mapTenantToTenantDto).orElseGet(this::mapPrimaryToTenantDto);
    }

    public Optional<TenantDto> getTenant(String tenantCode) {
        return tenantManager.getConfigOfReadyTenant(tenantCode)
                .map(this::mapTenantToTenantDto).or(() -> Optional.ofNullable(tenantCode)
                        .filter(PRIMARY_CODE::equals).map(t -> mapPrimaryToTenantDto()));
    }

    public List<TenantDto> getTenants() {
        List<TenantDto> tenants = tenantManager.getCodes().stream()
                .map(tenantManager::getConfigOfReadyTenant)
                .flatMap(Optional::stream)
                .map(this::mapTenantToTenantDto).collect(Collectors.toList());

        tenants.add(mapPrimaryToTenantDto());

        return tenants;

    }

    @Override
    public TenantStatsAndStatusesDto getTenantStatsAndStatuses() {
        Map<String, TenantStatus> statuses = tenantManager.getStatuses();
        Map<String, TenantStatus> mandatory = fetchByFilteringConfig(statuses, true);
        Map<String, TenantStatus> additionals = fetchByFilteringConfig(statuses, false);

        return TenantStatsAndStatusesDto.builder()
                .mandatoryStatuses(mandatory)
                .additionalStatuses(additionals)
                .stats(calculate(statuses)).build();
    }

    private Map<String, TenantStatus> fetchByFilteringConfig(Map<String, TenantStatus> statuses, boolean isMandatory) {
        return statuses.keySet().stream()
                .map(code -> tenantAccessor.getTenantConfigs().get(code))
                .filter(Objects::nonNull)
                .filter(tc -> isMandatory == tc.isInitializationAtStartRequired())
                .map(TenantConfig::getTenantCode)
                .collect(Collectors.toMap(k -> k, statuses::get));
    }

    private TenantStatsDto calculate(Map<String, TenantStatus> statuses) {
        TenantStatsDto d = new TenantStatsDto();
        statuses.values().stream().forEach(s -> {
            d.setCount(d.getCount()+1);
            switch (s) {
                case UNKNOWN:
                    d.setUnknown(d.getUnknown() + 1);
                    break;
                case FAILED:
                    d.setFailed(d.getFailed() + 1);
                    break;
                case PENDING:
                    d.setPending(d.getPending() + 1);
                    break;
                case READY:
                    d.setReady(d.getReady() + 1);
                    break;
            }
        });
        return d;
    }

    private TenantDto mapTenantToTenantDto(TenantConfig config){
        return TenantDto.builder()
                .code(config.getTenantCode()).primary(false)
                .resourceRootUrl(storageManager.getResourceUrl("", false))
                .build();
    }

    private TenantDto mapPrimaryToTenantDto(){
        String url = storageManager.getResourceUrl("", false);
        String resourceRootUrl = null;
        String resourceRootPath = null;

        if(isUrl(url)) {
            resourceRootUrl = url;
        } else {
            resourceRootPath = url;
        }

        return TenantDto.builder()
                .code(PRIMARY_CODE).primary(true)
                .resourceRootUrl(resourceRootUrl)
                .resourceRootPath(resourceRootPath)
                .build();
    }

    private boolean isUrl(String something){
        try {
            if(StringUtils.isBlank(URI.create(something).getScheme())){
                throw new RuntimeException("schema for resourceUrl is blank, url is just path");
            }
            return true;

        } catch(Exception ex) {
            log.debug("error for url:'{}'", something, ex);
            return false;
        }

    }
}
