/*
 * Copyright 2021-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpmail.web.emailconfig;

import com.agiletec.aps.system.services.role.Permission;
import com.agiletec.aps.system.services.user.UserDetails;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.HashMap;
import java.util.Map;
import jakarta.validation.Valid;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.plugins.jpmail.ent.system.services.SMTPServerConfigurationService;
import org.entando.entando.plugins.jpmail.ent.system.services.model.SMTPServerConfigurationDto;
import org.entando.entando.plugins.jpmail.web.emailconfig.validator.SMTPServerConfigurationValidator;
import org.entando.entando.web.common.annotation.RestAccessControl;
import org.entando.entando.web.common.exceptions.ValidationGenericException;
import org.entando.entando.web.common.model.SimpleRestResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = {"smtp-server-configuration-controller"})
@RequestMapping(value = "/plugins/emailSettings/SMTPServer")
public class SMTPServerConfigurationController {

    private final EntLogger logger = EntLogFactory.getSanitizedLogger(getClass());
    private static final String STATUS = "status";

    @Autowired
    private SMTPServerConfigurationService smtpServerConfigurationService;

    @Autowired
    private SMTPServerConfigurationValidator smtpServerConfigurationValidator;


    @ApiOperation("Get the SMTP Server Configuration")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @RestAccessControl(permission = Permission.SUPERUSER)
    public ResponseEntity<SimpleRestResponse<SMTPServerConfigurationDto>> getSMTPServerConfiguration() {
        logger.debug("Get SMTP Server Configuration");

        SMTPServerConfigurationDto response =  smtpServerConfigurationService.getSMTPServerConfiguration();
        return new ResponseEntity<>(new SimpleRestResponse<>(response), HttpStatus.OK);
    }

    @ApiOperation("Update the SMTP server configuration")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
    })
    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @RestAccessControl(permission = Permission.SUPERUSER)
    public ResponseEntity<SimpleRestResponse<SMTPServerConfigurationDto>> updateSMTPServer(@Valid @RequestBody SMTPServerConfigurationDto smtpServerConfiguration,
                                                                                           BindingResult bindingResult) {
        logger.debug("Update SMTP Server Configuration");

        smtpServerConfigurationValidator.validateProtocol(smtpServerConfiguration.getProtocol(), bindingResult);

        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }

        SMTPServerConfigurationDto response = smtpServerConfigurationService.updateSMTPServerConfiguration(smtpServerConfiguration);
        return new ResponseEntity<>(new SimpleRestResponse<>(response), HttpStatus.OK);
    }

    @ApiOperation("Send a test email to the logged user to check if the SMTP Configuration is correct")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "Conflict")
    })
    @PostMapping(value = "/sendTestEmail", produces = MediaType.APPLICATION_JSON_VALUE)
    @RestAccessControl(permission = Permission.SUPERUSER)
    public ResponseEntity<SimpleRestResponse<Map<String,String>>> sendTestEmail(@RequestAttribute("user") UserDetails user) {
        logger.debug("Send Test Email");
        HttpStatus status;

        BindingResult bindingResult = new BeanPropertyBindingResult(user, "user");
        smtpServerConfigurationValidator.validateSenderList(bindingResult);
        smtpServerConfigurationValidator.validateUserEmail(user, bindingResult);

        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }

        Map<String, String> response = new HashMap<>();
        final boolean emailTestSent = smtpServerConfigurationService.sendEmailTest(user);

        if (emailTestSent) {
            response.put(STATUS, HttpStatus.OK.toString());
            status = HttpStatus.OK;
        } else {
            response.put(STATUS, HttpStatus.CONFLICT.toString());
            status = HttpStatus.CONFLICT;
        }

        return new ResponseEntity<>(new SimpleRestResponse<>(response), status);
    }


    @ApiOperation("Test if the SMTP Configuration is correct")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "Conflict")
    })
    @PostMapping(value = "/testConfiguration", produces = MediaType.APPLICATION_JSON_VALUE)
    @RestAccessControl(permission = Permission.SUPERUSER)
    public ResponseEntity<SimpleRestResponse<Map<String,String>>> testSMTPServerConfiguration(@Valid @RequestBody SMTPServerConfigurationDto smtpServerConfiguration,
                                                                         BindingResult bindingResult) {
        logger.debug("Test SMTP Server Configuration");
        Map<String, String> response = new HashMap<>();
        HttpStatus status;

        smtpServerConfigurationValidator.validateProtocol(smtpServerConfiguration.getProtocol(), bindingResult);

        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }

        final boolean emailTestSent = smtpServerConfigurationService.testSMTPConfiguration(smtpServerConfiguration);

        if (emailTestSent) {
            response.put(STATUS, HttpStatus.OK.toString());
            status = HttpStatus.OK;
        } else {
            response.put(STATUS, HttpStatus.CONFLICT.toString());
            status = HttpStatus.CONFLICT;
        }
        return new ResponseEntity<>(new SimpleRestResponse<>(response), status);
    }

}
