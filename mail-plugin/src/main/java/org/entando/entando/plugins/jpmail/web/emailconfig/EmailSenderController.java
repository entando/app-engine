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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.plugins.jpmail.ent.system.services.EmailSenderService;
import org.entando.entando.plugins.jpmail.ent.system.services.model.EmailSenderDto;
import org.entando.entando.plugins.jpmail.web.emailconfig.validator.EmailSenderValidator;
import org.entando.entando.web.common.annotation.RestAccessControl;
import org.entando.entando.web.common.exceptions.ValidationGenericException;
import org.entando.entando.web.common.model.PagedMetadata;
import org.entando.entando.web.common.model.PagedRestResponse;
import org.entando.entando.web.common.model.RestListRequest;
import org.entando.entando.web.common.model.SimpleRestResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@Api(tags = {"email-sender-controller"})
@RequestMapping(value = "/plugins/emailSettings")
public class EmailSenderController {

    private final EntLogger logger = EntLogFactory.getSanitizedLogger(getClass());

    @Autowired
    private EmailSenderService emailSenderService;

    @Autowired
    private EmailSenderValidator emailSenderValidator;

    @ApiOperation("Get the email sender list")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden")
    })
    @GetMapping(value = "/senders",produces = MediaType.APPLICATION_JSON_VALUE)
    @RestAccessControl(permission = Permission.SUPERUSER)
    public ResponseEntity<PagedRestResponse<EmailSenderDto>> getEmailSenders(RestListRequest requestList) {
        logger.debug("Get email sender list");
        emailSenderValidator.validateRestListRequest(requestList, EmailSenderDto.class);
        PagedMetadata<EmailSenderDto> result = emailSenderService.getEmailSenders(requestList);
        return new ResponseEntity<>(new PagedRestResponse<>(result), HttpStatus.OK);
    }

    @ApiOperation("Get the email sender by code")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden")
    })
    @GetMapping(value = "/senders/{senderCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @RestAccessControl(permission = Permission.SUPERUSER)
    public ResponseEntity<SimpleRestResponse<EmailSenderDto>> getSender(@PathVariable String senderCode) {
        logger.debug("Get email sender by code {}", senderCode);
        EmailSenderDto response = emailSenderService.getEmailSender(senderCode);
        return new ResponseEntity<>(new SimpleRestResponse<>(response), HttpStatus.OK);
    }

    @ApiOperation("Update the email sender by code")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden")
    })
    @PutMapping(value = "/senders/{senderCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @RestAccessControl(permission = Permission.SUPERUSER)
    public ResponseEntity<SimpleRestResponse<EmailSenderDto>> updateEmailSender(@PathVariable String senderCode,
                                                                                @Valid @RequestBody EmailSenderDto emailSender,
                                                                                BindingResult bindingResult) {
        logger.debug("Update email sender {}", senderCode);

        emailSenderValidator.validateSenderExists(senderCode, bindingResult);
        emailSenderValidator.validateSenderCode(senderCode, emailSender.getCode(), bindingResult);

        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }

        EmailSenderDto response = emailSenderService.updateEmailSender(emailSender);
        return new ResponseEntity<>(new SimpleRestResponse<>(response), HttpStatus.OK);
    }

    @ApiOperation("Add an email sender")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden")
    })
    @PostMapping(value="/senders", produces = MediaType.APPLICATION_JSON_VALUE)
    @RestAccessControl(permission = Permission.SUPERUSER)
    public ResponseEntity<SimpleRestResponse<EmailSenderDto>> addEmailSender(
            @Valid @RequestBody EmailSenderDto emailSender,
            BindingResult bindingResult) {
        logger.debug("Add email sender");
        emailSenderValidator.validateSenderNotExists(emailSender.getCode(), bindingResult);

        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        EmailSenderDto response = emailSenderService.addEmailSender(emailSender);
        return new ResponseEntity<>(new SimpleRestResponse<>(response), HttpStatus.OK);
    }

    @ApiOperation("Delete an email sender")
    @ApiResponses({
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 401, message = "Unauthorized"),
            @ApiResponse(code = 403, message = "Forbidden"),
            @ApiResponse(code = 409, message = "Conflict")
    })

    @DeleteMapping(value = "/senders/{senderCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @RestAccessControl(permission = Permission.SUPERUSER)
    public ResponseEntity<SimpleRestResponse<Map<String,String>>> deleteEmailSender(@PathVariable String senderCode) {
        logger.debug("Deleting email sender {}", senderCode);

        final EmailSenderDto emailSender = emailSenderService.getEmailSender(senderCode);

        Map<String, String> response = new HashMap<>();
        response.put("senderCode", senderCode);

        if (null == emailSender) {
            return new ResponseEntity<>(new SimpleRestResponse<>(response), HttpStatus.NOT_FOUND);
        }
        else {
            emailSenderService.deleteEmailSender(senderCode);
            return new ResponseEntity<>(new SimpleRestResponse<>(response), HttpStatus.OK);
        }
    }

}
