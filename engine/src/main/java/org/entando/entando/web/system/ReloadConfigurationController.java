/*
 * Copyright 2018-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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

import java.util.HashMap;
import java.util.Map;

import com.agiletec.aps.system.services.reload.ReloadConfigThread;
import jakarta.servlet.http.HttpServletRequest;

import com.agiletec.aps.system.services.role.Permission;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.web.common.annotation.RestAccessControl;
import org.entando.entando.web.common.model.SimpleRestResponse;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static com.agiletec.aps.system.services.reload.ReloadConfigThread.RELOAD_THREAD;

@RestController
@RequestMapping(value = "/reloadConfiguration")
public class ReloadConfigurationController {

    private final String FAILURE_RELOADING_RESULT = "fail";
    private final String SUCCESS_RELOADING_RESULT = "success";
    private final String PROGRESS_RELOADING_RESULT = "progress";
    private final String WARNING_RELOADING_RESULT = "waiting";

    private final EntLogger logger = EntLogFactory.getSanitizedLogger(getClass());

    @RestAccessControl(permission = Permission.SUPERUSER)
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleRestResponse<ReloadConfigResponse>> reloadConfiguration(HttpServletRequest request) throws Throwable {
        logger.debug("reload configuration: start..");
        Map<String, String> result = new HashMap<>();
        String status;
        Integer progress = null;
        Map<String, String> info = null;

        try {
            if (!ApsWebApplicationUtils.isReloadInProgress()) {
                ReloadConfigThread rct = new ReloadConfigThread(request);
                logger.info("Starting reload configuration thread");
                rct.start();
            } else {
                logger.info("Reload operation already in progress!");
            }
            status = PROGRESS_RELOADING_RESULT;
            progress = getReloadProgress();
        } catch (Exception e) {
            logger.error("unexpected error while launching system reload", e);
            status = FAILURE_RELOADING_RESULT;
        }

        HttpStatus httpStatus = status.equalsIgnoreCase(FAILURE_RELOADING_RESULT) ?
                HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.OK;

        return new ResponseEntity<>(
                new SimpleRestResponse<>(new ReloadConfigResponse(status, progress, info)),
                httpStatus);
    }

    @RestAccessControl(permission = Permission.SUPERUSER)
    @RequestMapping(path = "/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleRestResponse<ReloadConfigResponse>> reloadStatus(HttpServletRequest request) throws Throwable {

        ReloadConfigResponse response = updateReloadStatusResult();
        HttpStatus httpStatus = response.status.equalsIgnoreCase(FAILURE_RELOADING_RESULT) ?
                HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.OK;

        return new ResponseEntity<>(new SimpleRestResponse<>(response), httpStatus);

    }

    public boolean isReloadingErrorDetect() {
        return ApsWebApplicationUtils.getReloadInfo()
                .values()
                .stream()
                .anyMatch(StringUtils::isNotBlank);
    }

    public int getReloadProgress() {
        return ApsWebApplicationUtils.getReloadProgress();
    }

    public Map<String, String> getReloadInfo() {
        return new HashMap<>(ApsWebApplicationUtils.getReloadInfo());
    }

    private ReloadConfigResponse updateReloadStatusResult() {
        String status;
        if (ApsWebApplicationUtils.isReloadInProgress()) {
            status = PROGRESS_RELOADING_RESULT;
        } else if (ApsWebApplicationUtils.getReloadInfo().containsKey(RELOAD_THREAD)) {
            status = FAILURE_RELOADING_RESULT;
        } else if (isReloadingErrorDetect()) {
            status = WARNING_RELOADING_RESULT;
        } else {
            status = SUCCESS_RELOADING_RESULT;
        }
        return new ReloadConfigResponse(status, getReloadProgress(), getReloadInfo());
    }

    public record ReloadConfigResponse(String status, Integer percentage, Map<String,String> info) { }

}
