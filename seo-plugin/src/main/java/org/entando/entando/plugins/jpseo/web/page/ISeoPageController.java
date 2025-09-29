/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version v3.0 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.plugins.jpseo.web.page;

import com.agiletec.aps.system.services.role.Permission;
import com.agiletec.aps.system.services.user.UserDetails;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Map;
import jakarta.validation.Valid;
import org.entando.entando.aps.system.services.page.IPageService;
import org.entando.entando.aps.system.services.page.model.PageDto;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.plugins.jpseo.aps.system.services.page.SeoPageDto;
import org.entando.entando.plugins.jpseo.web.page.model.SeoPageRequest;
import org.entando.entando.web.common.annotation.ActivityStreamAuditable;
import org.entando.entando.web.common.annotation.RestAccessControl;
import org.entando.entando.web.common.model.RestResponse;
import org.entando.entando.web.common.model.SimpleRestResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Api(tags = {"seo-page-controller"})
@ApiResponses({
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 201, message = "Created"),
        @ApiResponse(code = 400, message = "Bad Request"),
        @ApiResponse(code = 403, message = "Forbidden"),
        @ApiResponse(code = 409, message = "Conflict"),
        @ApiResponse(code = 401, message = "Unauthorized"),
        @ApiResponse(code = 500, message = "Server Error")
})
public interface ISeoPageController {

    @RestAccessControl(permission = Permission.MANAGE_PAGES)
    @GetMapping("{pageCode}")
    ResponseEntity<RestResponse<PageDto, Map<String, String>>> getSeoPage(@RequestAttribute("user") UserDetails user,
            @PathVariable String pageCode,
            @RequestParam(value = "status", required = false, defaultValue = IPageService.STATUS_DRAFT) String status);

    @ActivityStreamAuditable
    @RestAccessControl(permission = Permission.MANAGE_PAGES)
    @PostMapping()
    ResponseEntity<SimpleRestResponse<SeoPageDto>> addPage(@RequestAttribute("user") UserDetails user,
            @Valid @RequestBody SeoPageRequest pageRequest, BindingResult bindingResult) throws EntException;

    @ActivityStreamAuditable
    @RestAccessControl(permission = Permission.MANAGE_PAGES)
    @PutMapping("{pageCode}")
    ResponseEntity<RestResponse<SeoPageDto, Map<String, String>>> updatePage(
            @RequestAttribute("user") UserDetails user, @PathVariable String pageCode,
            @Valid @RequestBody SeoPageRequest pageRequest, BindingResult bindingResult);
}
