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
package org.entando.entando.plugins.jpsolr.web.content;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import java.util.Arrays;
import javax.servlet.http.HttpSession;
import org.entando.entando.aps.system.services.searchengine.FacetedContentsResult;
import org.entando.entando.plugins.jpsolr.aps.system.content.IAdvContentFacetManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.entando.entando.web.common.model.PagedMetadata;
import org.entando.entando.web.common.model.PagedRestResponse;
import org.entando.entando.web.common.model.RestResponse;
import org.entando.entando.web.common.validator.AbstractPaginationValidator;
import org.springframework.validation.Errors;

/**
 * @author E.Santoboni
 */
@RestController
@RequestMapping(value = "/plugins/advcontentsearch")
public class AdvContentSearchController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String ERRCODE_REFERENCED_ONLINE_CONTENT = "2";
    public static final String ERRCODE_UNAUTHORIZED_CONTENT = "3";
    public static final String ERRCODE_DELETE_PUBLIC_PAGE = "5";
    public static final String ERRCODE_INVALID_MODEL = "6";
    public static final String ERRCODE_INVALID_LANG_CODE = "7";

    @Autowired
    private HttpSession httpSession;
    
    @Autowired
    private IAdvContentFacetManager advContentFacetManager;

    public IAdvContentFacetManager getAdvContentFacetManager() {
        return advContentFacetManager;
    }

    public void setAdvContentFacetManager(IAdvContentFacetManager advContentFacetManager) {
        this.advContentFacetManager = advContentFacetManager;
    }
    
    protected AbstractPaginationValidator getPaginationValidator() {
        return new AbstractPaginationValidator() {
            @Override
            public boolean supports(Class<?> type) {
                return true;
            }

            @Override
            public void validate(Object o, Errors errors) {
                //nothing to do
            }

            @Override
            protected String getDefaultSortProperty() {
                return IContentManager.CONTENT_CREATION_DATE_FILTER_KEY;
            }

            @Override
            public boolean isValidField(String fieldName, Class<?> type) {
                if (fieldName.contains(".")) {
                    return true;
                } else {
                    return Arrays.asList(IContentManager.METADATA_FILTER_KEYS).contains(fieldName);
                }
            }

        };
    }

    @RequestMapping(value = "/contents", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PagedRestResponse<String>> getContents(AdvRestContentListRequest requestList) {
        logger.debug("getting contents with request {}", requestList);
        UserDetails currentUser = this.extractCurrentUser();
        this.getPaginationValidator().validateRestListRequest(requestList, String.class);
        PagedMetadata<String> result = this.getAdvContentFacetManager().getContents(requestList, currentUser);
        boolean isGuest = (null == currentUser || currentUser.getUsername().equalsIgnoreCase(SystemConstants.GUEST_USER_NAME));
        result.getAdditionalParams().put("guestUser", String.valueOf(isGuest));
        return new ResponseEntity<>(new PagedRestResponse<>(result), HttpStatus.OK);
    }

    @RequestMapping(value = "/facetedcontents", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RestResponse<FacetedContentsResult, AdvRestContentListRequest>> getFacetedContents(AdvRestContentListRequest requestList) {
        logger.debug("getting contents with request {}", requestList);
        this.getPaginationValidator().validateRestListRequest(requestList, String.class);
        UserDetails currentUser = this.extractCurrentUser();
        FacetedContentsResult result = this.getAdvContentFacetManager().getFacetedContents(requestList, currentUser);
        boolean isGuest = (null == currentUser || currentUser.getUsername().equalsIgnoreCase(SystemConstants.GUEST_USER_NAME));
        requestList.setGuestUser(isGuest);
        return new ResponseEntity<>(new RestResponse<>(result, requestList), HttpStatus.OK);
    }

    protected UserDetails extractCurrentUser() {
        return (UserDetails) this.httpSession.getAttribute("user");
    }

}