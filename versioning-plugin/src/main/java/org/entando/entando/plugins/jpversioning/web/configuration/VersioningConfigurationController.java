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
package org.entando.entando.plugins.jpversioning.web.configuration;

import jakarta.servlet.http.HttpSession;
import org.entando.entando.plugins.jpversioning.services.configuration.VersioningConfigurationService;
import org.entando.entando.plugins.jpversioning.web.configuration.model.VersioningConfigurationDTO;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/plugins/versioning/configuration")
public class VersioningConfigurationController implements IVersioningConfiguration {

    private final EntLogger logger = EntLogFactory.getSanitizedLogger(getClass());

    @Autowired
    private VersioningConfigurationService versioningConfigurationService;

    @Autowired
    private HttpSession httpSession;

    @Override
    public ResponseEntity<VersioningConfigurationDTO> getVersioningConfiguration() {
        logger.debug("REST request - get Versioning Configuration");
        final VersioningConfigurationDTO versioningConfiguration = versioningConfigurationService.getVersioningConfiguration();
        return new ResponseEntity<>(versioningConfiguration, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<VersioningConfigurationDTO> putVersioningConfiguration(VersioningConfigurationDTO versioningConfigurationDTO) {
        logger.debug("REST request - put Versioning Configuration");
        final VersioningConfigurationDTO versioningConfiguration = versioningConfigurationService.putVersioningConfiguration(versioningConfigurationDTO);
        return new ResponseEntity<>(versioningConfiguration, HttpStatus.OK);
    }

}
