/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.web.tenant;

import com.agiletec.aps.system.services.role.Permission;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.NotBlank;
import org.entando.entando.aps.system.exception.ResourceNotFoundException;
import org.entando.entando.aps.system.services.tenants.ITenantService;
import org.entando.entando.web.common.annotation.RestAccessControl;
import org.entando.entando.web.common.model.RestResponse;
import org.entando.entando.web.common.model.SimpleRestResponse;
import org.entando.entando.web.tenant.model.TenantDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TenantsController {

    private final Logger logger = LoggerFactory.getLogger(TenantsController.class);

    @Autowired
    private ITenantService tenantService;

    @RestAccessControl(permission = Permission.ENTER_BACKEND)
    @GetMapping(value = "/currentTenant", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleRestResponse<TenantDto>> getCurrentTenant() {
        logger.debug("Getting current tenant");
        TenantDto resp = tenantService.getCurrentTenant();
        return new ResponseEntity<>(new SimpleRestResponse<>(resp), HttpStatus.OK);
    }

    @RestAccessControl(permission = Permission.ENTER_BACKEND)
    @GetMapping(value = "/tenants/{tenantCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleRestResponse<TenantDto>> getTenant(@PathVariable @NotBlank String tenantCode) {
        logger.debug("Getting tenant from tenantCode:'{}'",tenantCode);
        TenantDto resp = tenantService.getTenant(tenantCode)
                .orElseThrow(() -> new ResourceNotFoundException("tenantCode","tenantCode"));

        return new ResponseEntity<>(new SimpleRestResponse<>(resp), HttpStatus.OK);
    }

    @RestAccessControl(permission = Permission.ENTER_BACKEND)
    @GetMapping(value = "/tenants", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RestResponse<List<TenantDto>, Map<String,String>>> getTenants() {
        logger.debug("Getting current tenant");
        List<TenantDto> resp = tenantService.getTenants();

        return new ResponseEntity<>(new RestResponse<>(resp, new HashMap<>()), HttpStatus.OK);
    }

}
