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
package org.entando.entando.web.system;

import com.agiletec.aps.system.services.role.Permission;
import lombok.extern.slf4j.Slf4j;
import org.entando.entando.aps.system.services.systemconfiguration.ISystemConfigurationService;
import org.entando.entando.web.common.annotation.RestAccessControl;
import org.entando.entando.web.common.model.SimpleRestResponse;
import org.entando.entando.web.system.model.SystemConfigurationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class CurrentSystemController {

    @Autowired
    private ISystemConfigurationService systemConfigurationService;

    @RestAccessControl(permission = Permission.ENTER_BACKEND)
    @GetMapping(value = "/currentSystemConfiguration", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleRestResponse<SystemConfigurationDto>> getCurrentSystemConfiguration() {
        log.debug("Getting current system configuration");
        SystemConfigurationDto resp = systemConfigurationService.getSystemConfiguration();
        return new ResponseEntity<>(new SimpleRestResponse<>(resp), HttpStatus.OK);
    }

}
