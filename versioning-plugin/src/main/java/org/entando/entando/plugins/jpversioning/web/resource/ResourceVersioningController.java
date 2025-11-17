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
package org.entando.entando.plugins.jpversioning.web.resource;

import com.agiletec.aps.system.services.user.UserDetails;
import jakarta.servlet.http.HttpSession;
import org.entando.entando.aps.util.HttpSessionHelper;
import org.entando.entando.plugins.jpversioning.services.resource.ResourcesVersioningService;
import org.entando.entando.plugins.jpversioning.web.resource.model.ResourceDTO;
import org.entando.entando.plugins.jpversioning.web.resource.model.ResourceDownloadDTO;
import org.entando.entando.web.common.model.PagedMetadata;
import org.entando.entando.web.common.model.PagedRestResponse;
import org.entando.entando.web.common.model.RestListRequest;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/plugins/versioning/resources")
public class ResourceVersioningController implements IResourceVersioning {

    private final EntLogger logger = EntLogFactory.getSanitizedLogger(getClass());

    @Autowired
    private ResourcesVersioningService resourcesVersioningService;

    @Override
    public ResponseEntity<PagedRestResponse<ResourceDTO>> listTrashedResources(String resourceTypeCode,
            RestListRequest requestList, UserDetails userDetails) {
        logger.debug("REST request - list trashed resources for resourceTypeCode: {} and with request: {}",
                resourceTypeCode, requestList);
        PagedMetadata<ResourceDTO> result = resourcesVersioningService
                .getTrashedResources(resourceTypeCode, requestList, userDetails);
        return new ResponseEntity<>(new PagedRestResponse<>(result), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ResourceDTO> recoverResource(String resourceId) {
        logger.debug("REST request - recover resource: {}", resourceId);
        ResourceDTO result = resourcesVersioningService.recoverResource(resourceId);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ResourceDTO> deleteTrashedResource(String resourceId) {
        logger.debug("REST request - deleting trashed resource: {}", resourceId);
        ResourceDTO result = resourcesVersioningService.deleteTrashedResource(resourceId);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @Override
    public ResponseEntity getTrashedResource(String resourceId, Integer size, UserDetails userDetails) {
        logger.debug("REST request - get trashed resource id: {} and size: {}", resourceId, size);
        ResourceDownloadDTO result = resourcesVersioningService.getTrashedResource(resourceId, size, userDetails);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + result.getFilename());
        headers.add(HttpHeaders.CONTENT_TYPE, result.getType());
        return new ResponseEntity(result.getBytes(), headers, HttpStatus.OK);
    }
}
