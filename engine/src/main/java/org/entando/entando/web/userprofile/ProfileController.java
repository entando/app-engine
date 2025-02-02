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
import java.util.HashMap;
import java.util.Map;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.entando.entando.aps.system.exception.ResourceNotFoundException;
import org.entando.entando.aps.system.exception.RestServerError;
import org.entando.entando.aps.system.services.entity.model.EntityDto;
import org.entando.entando.aps.system.services.userprofile.IAvatarService;
import org.entando.entando.aps.system.services.userprofile.IUserProfileManager;
import org.entando.entando.aps.system.services.userprofile.IUserProfileService;
import org.entando.entando.aps.system.services.userprofile.model.AvatarDto;
import org.entando.entando.aps.system.services.userprofile.model.IUserProfile;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.web.common.annotation.RestAccessControl;
import org.entando.entando.web.common.exceptions.ValidationGenericException;
import org.entando.entando.web.common.model.RestResponse;
import org.entando.entando.web.common.model.SimpleRestResponse;
import org.entando.entando.web.entity.validator.EntityValidator;
import org.entando.entando.web.userprofile.model.ProfileAvatarRequest;
import org.entando.entando.web.userprofile.validator.ProfileAvatarValidator;
import org.entando.entando.web.userprofile.validator.ProfileValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author E.Santoboni
 */
@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final EntLogger logger = EntLogFactory.getSanitizedLogger(this.getClass());

    private final IUserProfileService userProfileService;

    private final ProfileValidator profileValidator;

    private final ProfileAvatarValidator profileAvatarValidator;

    private final IUserManager userManager;

    private final IUserProfileManager userProfileManager;

    private final IAvatarService avatarService;

    public static final String PROTECTED_FOLDER = "protectedFolder";

    public static final String PREV_PATH = "prevPath";

    @RestAccessControl(permission = {Permission.MANAGE_USER_PROFILES, Permission.MANAGE_USERS})
    @RequestMapping(value = "/userProfiles/{username}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleRestResponse<EntityDto>> getUserProfile(@PathVariable String username) {
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
        if (!profileValidator.existProfile(username)) {
            if (userExists(username)) {
                // if the user exists but the profile doesn't, creates an empty profile
                IUserProfile userProfile = createNewEmptyUserProfile(username);
                dto = new EntityDto(userProfile);
            } else {
                throw new ResourceNotFoundException(EntityValidator.ERRCODE_ENTITY_DOES_NOT_EXIST, "Profile", username);
            }
        } else {
            dto = userProfileService.getUserProfile(username);
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
    public ResponseEntity<SimpleRestResponse<EntityDto>> addUserProfile(@Valid @RequestBody EntityDto bodyRequest,
            BindingResult bindingResult) {
        logger.debug("Add new user profile -> {}", bodyRequest);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        profileValidator.validate(bodyRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        EntityDto response = userProfileService.addUserProfile(bodyRequest, bindingResult);
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
        profileValidator.validateBodyName(username, bodyRequest, bindingResult);
        EntityDto response = userProfileService.updateUserProfile(bodyRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        return new ResponseEntity<>(new SimpleRestResponse<>(response), HttpStatus.OK);
    }

    @PutMapping(value = "/myUserProfile", produces = MediaType.APPLICATION_JSON_VALUE)
    @RestAccessControl(permission = Permission.ENTER_BACKEND)
    public ResponseEntity<SimpleRestResponse<EntityDto>> updateMyUserProfile(@RequestAttribute("user") UserDetails user,
            /*@Valid*/ @RequestBody EntityDto bodyRequest, BindingResult bindingResult) {
        logger.debug("Update profile for the logged user {} -> {}", user.getUsername(), bodyRequest);
        profileValidator.validateBodyName(user.getUsername(), bodyRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        EntityDto response = userProfileService.updateUserProfile(bodyRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        return new ResponseEntity<>(new SimpleRestResponse<>(response), HttpStatus.OK);
    }
    
    @GetMapping(path = "/userProfiles/avatar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RestResponse<Map<String, Object>, Map<String, Object>>> getAvatar(
            @RequestAttribute("user") UserDetails userDetails) {
        // request user profile picture
        AvatarDto avatarData = avatarService.getAvatarData(userDetails);
        // fill output
        Map<String, Object> result = new HashMap<>();
        result.put(PROTECTED_FOLDER, avatarData.isProtectedFolder());
        result.put("isDirectory", false);
        result.put("path", avatarData.getCurrentPath());
        result.put("filename", avatarData.getFilename());
        result.put("useGravatar", avatarData.isGravatar());
        result.put("base64", avatarData.getBase64());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(PREV_PATH, avatarData.getPrevPath());
        return new ResponseEntity<>(new RestResponse<>(result, metadata), HttpStatus.OK);
    }

    @PostMapping(path = "/userProfiles/avatar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleRestResponse<Map<String, Object>>> addAvatar(
            @Validated @RequestBody ProfileAvatarRequest request,
            @RequestAttribute("user") UserDetails user,
            BindingResult bindingResult) {
        // validate input dto to check for consistency of input
        profileAvatarValidator.validate(request, user, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        String pictureFileName = avatarService.updateAvatar(request, user, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationGenericException(bindingResult);
        }
        Map<String, Object> response = null != pictureFileName ? Map.of("filename", pictureFileName) : Map.of("useGravatar", true);
        return new ResponseEntity<>(new SimpleRestResponse<>(response), HttpStatus.OK);
    }

    @DeleteMapping(path = "/userProfiles/avatar")
    public ResponseEntity<SimpleRestResponse<Map<String, String>>> deleteAvatar(@RequestAttribute("user") UserDetails user) {
        avatarService.deleteAvatar(user, new MapBindingResult(new HashMap<>(), "user"));
        Map<String, String> payload = new HashMap<>();
        payload.put("username", user.getUsername());
        return new ResponseEntity<>(new SimpleRestResponse<>(payload), HttpStatus.OK);
    }
    
}
