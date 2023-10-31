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
package org.entando.entando.web.userprofile;

import com.agiletec.aps.system.services.role.Permission;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.imageio.ImageIO;
import org.entando.entando.aps.system.exception.ResourceNotFoundException;
import org.entando.entando.aps.system.exception.RestServerError;
import org.entando.entando.aps.system.services.entity.model.EntityDto;
import org.entando.entando.aps.system.services.storage.IFileBrowserService;
import org.entando.entando.aps.system.services.userprofile.IUserProfileManager;
import org.entando.entando.aps.system.services.userprofile.IUserProfileService;
import org.entando.entando.aps.system.services.userprofile.model.IUserProfile;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.web.common.annotation.RestAccessControl;
import org.entando.entando.web.common.exceptions.ValidationGenericException;
import org.entando.entando.web.common.model.RestResponse;
import org.entando.entando.web.common.model.SimpleRestResponse;
import org.entando.entando.web.entity.validator.EntityValidator;
import org.entando.entando.web.filebrowser.model.FileBrowserFileRequest;
import org.entando.entando.web.userprofile.model.ProfileAvatarRequest;
import org.entando.entando.web.userprofile.validator.ProfileAvatarValidator;
import org.entando.entando.web.userprofile.validator.ProfileValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * @author E.Santoboni
 */
@RestController
public class ProfileController {

    private final EntLogger logger = EntLogFactory.getSanitizedLogger(this.getClass());

    @Autowired
    private IUserProfileService userProfileService;

    @Autowired
    private ProfileValidator profileValidator;

    @Autowired
    private ProfileAvatarValidator profileAvatarValidator;

    @Autowired
    private IUserManager userManager;

    @Autowired
    private IUserProfileManager userProfileManager;

    @Autowired
    private IFileBrowserService fileBrowserService;

    public static final String FILE_NAME = "fileName";

    public static final String PROTECTED_FOLDER = "protectedFolder";

    private static final Object DEFAULT_AVATAR_PATH = "static/profile";

    public static final String PREV_PATH = "prevPath";

    protected IUserProfileService getUserProfileService() {
        return userProfileService;
    }

    public void setUserProfileService(IUserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    public ProfileValidator getProfileValidator() {
        return profileValidator;
    }

    public IFileBrowserService getFileBrowserService() {
        return fileBrowserService;
    }
    
    public void setProfileValidator(ProfileValidator profileValidator) {
        this.profileValidator = profileValidator;
    }

    @RestAccessControl(permission = {Permission.MANAGE_USER_PROFILES, Permission.MANAGE_USERS})
    @RequestMapping(value = "/userProfiles/{username}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleRestResponse<EntityDto>> getUserProfile(@PathVariable String username) throws JsonProcessingException {
        logger.debug("Requested profile -> {}", username);
        final EntityDto dto = getUserProfileEntityDto(username);
        logger.debug("Main Response -> {}", dto);
        return new ResponseEntity<>(new SimpleRestResponse<>(dto), HttpStatus.OK);
    }

    @RestAccessControl(permission = Permission.ENTER_BACKEND)
    @GetMapping(value = "/myUserProfile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleRestResponse<EntityDto>> getMyUserProfile(@RequestAttribute("user") UserDetails user) {
        final EntityDto userProfileEntityDto = getUserProfileEntityDto(user.getUsername());
        logger.debug("Main Response -> {}", userProfileEntityDto);
        return new ResponseEntity<>(new SimpleRestResponse<>(userProfileEntityDto), HttpStatus.OK);
    }

    private EntityDto getUserProfileEntityDto(final String username) {
        EntityDto dto;
        if (!this.getProfileValidator().existProfile(username)) {
            if (userExists(username)) {
                // if the user exists but the profile doesn't, creates an empty profile
                IUserProfile userProfile = createNewEmptyUserProfile(username);
                dto = new EntityDto(userProfile);
            } else {
                throw new ResourceNotFoundException(EntityValidator.ERRCODE_ENTITY_DOES_NOT_EXIST, "Profile", username);
            }
        } else {
            dto = this.getUserProfileService().getUserProfile(username);
        }
        return dto;
    }

    private boolean userExists(String username) {
        try {
            return userManager.getUser(username) != null;
        } catch (EntException e) {
            logger.error("Error in checking user existence {}", username, e);
            throw new RestServerError("Error in loading user", e);
        }
    }

    private IUserProfile createNewEmptyUserProfile(String username) {
        try {
            IUserProfile userProfile = userProfileManager.getDefaultProfileType();
            userProfileManager.addProfile(username, userProfile);
            return userProfile;
        } catch (EntException e) {
            logger.error("Error in creating empty user profile {}", username, e);
            throw new RestServerError("Error in loading user", e);
        }
    }

    @RestAccessControl(permission = Permission.MANAGE_USER_PROFILES)
    @RequestMapping(value = "/userProfiles", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleRestResponse<EntityDto>> addUserProfile(@Valid @RequestBody EntityDto bodyRequest, BindingResult bindingResult) {
        logger.debug("Add new user profile -> {}", bodyRequest);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        this.getProfileValidator().validate(bodyRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        EntityDto response = this.getUserProfileService().addUserProfile(bodyRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        return new ResponseEntity<>(new SimpleRestResponse<>(response), HttpStatus.OK);
    }

    @RestAccessControl(permission = {Permission.MANAGE_USER_PROFILES, Permission.MANAGE_USERS})
    @RequestMapping(value = "/userProfiles/{username}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleRestResponse<EntityDto>> updateUserProfile(@PathVariable String username,
            @Valid @RequestBody EntityDto bodyRequest, BindingResult bindingResult) {
        logger.debug("Update user profile -> {}", bodyRequest);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        this.getProfileValidator().validateBodyName(username, bodyRequest, bindingResult);
        EntityDto response = this.getUserProfileService().updateUserProfile(bodyRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        return new ResponseEntity<>(new SimpleRestResponse<>(response), HttpStatus.OK);
    }

    @PutMapping(value = "/myUserProfile", produces = MediaType.APPLICATION_JSON_VALUE)
    @RestAccessControl(permission = Permission.ENTER_BACKEND)
    public ResponseEntity<SimpleRestResponse<EntityDto>> updateMyUserProfile(@RequestAttribute("user") UserDetails user,
                                                                         @Valid @RequestBody EntityDto bodyRequest, BindingResult bindingResult) {
        logger.debug("Update profile for the logged user {} -> {}", user.getUsername(), bodyRequest);
        this.getProfileValidator().validateBodyName(user.getUsername(), bodyRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        EntityDto response = this.getUserProfileService().updateUserProfile(bodyRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        return new ResponseEntity<>(new SimpleRestResponse<>(response), HttpStatus.OK);
    }

    @RequestMapping(value = "/userProfiles/avatar", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RestResponse<Map<String, Object>, Map<String, Object>>> getFile(
            @RequestParam(value = FILE_NAME, required = false, defaultValue = "") String fileName) throws IOException {

        // set fixed params
        boolean protectedFolder = false;
        String currentPath = DEFAULT_AVATAR_PATH + "/" + fileName;
        // validate fileName using java NIO2 api (to avoid for instance \0)
        Paths.get(currentPath);
        // validate fileName to check if contains path to avoid directory listing
        if (fileName.contains("/")) {
            throw new IllegalArgumentException("The requested file name is not valid");
        }
        // get file from volume or else throw exception
        byte[] base64 = this.getFileBrowserService().getFileStream(currentPath, protectedFolder);
        // check if the desired file is an image, otherwise throw exception
        if (ImageIO.read(new ByteArrayInputStream(base64)) == null) {
            throw new IllegalArgumentException("The requested file is not an image");
        }

        // prepare output
        Map<String, Object> result = new HashMap<>();
        result.put(PROTECTED_FOLDER, protectedFolder);
        result.put("isDirectory", false);
        result.put("path", currentPath);
        result.put("filename", fileName);
        result.put("base64", base64);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put(PREV_PATH, DEFAULT_AVATAR_PATH);
        return new ResponseEntity<>(new RestResponse<>(result, metadata), HttpStatus.OK);
    }


    @RequestMapping(value = "/userProfiles/avatar", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RestResponse<Map<String, Object>, Map<String, Object>>> addFile(
            @Valid @RequestBody ProfileAvatarRequest request,
            BindingResult bindingResult) {
        return executeUpsert(request, bindingResult, this.getFileBrowserService()::addFile);

    }

    @RequestMapping(value = "/userProfiles/avatar", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RestResponse<Map<String, Object>, Map<String, Object>>> updateFile(
            @Valid @RequestBody ProfileAvatarRequest request, BindingResult bindingResult) {
        return executeUpsert(request, bindingResult, this.getFileBrowserService()::updateFile);
    }

    private ResponseEntity<RestResponse<Map<String, Object>, Map<String, Object>>> executeUpsert(
            ProfileAvatarRequest request, BindingResult bindingResult,
            BiConsumer<FileBrowserFileRequest, BindingResult> upsertFunction) {
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        // validate input dto to check for consistency of input
        profileAvatarValidator.validate(request, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        // prepare a FileBrowserFileRequest to use the api already available in the system
        FileBrowserFileRequest fileBrowserFileRequest = convertToFileBrowserFileRequest(request);
        // add the file to the volume
        upsertFunction.accept(fileBrowserFileRequest,bindingResult);
        // prepare and return a consistent response
        return this.composeAvatarUpsertResponse(request);
    }

    private static FileBrowserFileRequest convertToFileBrowserFileRequest(ProfileAvatarRequest request) {
        FileBrowserFileRequest fileBrowserFileRequest = new FileBrowserFileRequest();
        fileBrowserFileRequest.setFilename(request.getFilename());
        fileBrowserFileRequest.setPath(DEFAULT_AVATAR_PATH + "/" + request.getFilename());
        fileBrowserFileRequest.setProtectedFolder(false);
        fileBrowserFileRequest.setBase64(request.getBase64());
        return fileBrowserFileRequest;
    }

    public ResponseEntity<RestResponse<Map<String, Object>, Map<String, Object>>> composeAvatarUpsertResponse(
            ProfileAvatarRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put(PROTECTED_FOLDER, false);
        result.put("path", DEFAULT_AVATAR_PATH + "/" + request.getFilename());
        result.put("filename", request.getFilename());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(PREV_PATH, DEFAULT_AVATAR_PATH);
        return new ResponseEntity<>(new RestResponse<>(result, metadata), HttpStatus.OK);
    }
}
