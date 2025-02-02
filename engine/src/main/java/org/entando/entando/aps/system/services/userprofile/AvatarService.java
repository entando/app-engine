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
package org.entando.entando.aps.system.services.userprofile;

import com.agiletec.aps.system.services.user.UserDetails;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.exception.ResourceNotFoundException;
import org.entando.entando.aps.system.exception.RestServerError;
import org.entando.entando.aps.system.services.storage.IFileBrowserService;
import org.entando.entando.aps.system.services.storage.model.BasicFileAttributeViewDto;
import org.entando.entando.aps.system.services.userpreferences.IUserPreferencesManager;
import org.entando.entando.aps.system.services.userprofile.model.AvatarDto;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.web.entity.validator.EntityValidator;
import org.entando.entando.web.filebrowser.model.FileBrowserFileRequest;
import org.entando.entando.web.userprofile.model.ProfileAvatarRequest;
import org.springframework.validation.BindingResult;

@Slf4j
@RequiredArgsConstructor
public class AvatarService implements IAvatarService {
    
    private final IFileBrowserService fileBrowserService;
    private final IUserPreferencesManager userPreferencesManager;

    // CONSTANTS
    private static final String DEFAULT_AVATAR_PATH = "static/profile";

    @Override
    public AvatarDto getAvatarData(UserDetails userDetails) {
        try {
            boolean isGravatarEnabled = this.userPreferencesManager.isUserGravatarEnabled(userDetails.getUsername());
            if (isGravatarEnabled) {
                return AvatarDto.builder().gravatar(true).build();
            }
            String fileName = this.getAvatarFilenameByUsername(userDetails.getUsername());
            if (StringUtils.isEmpty(fileName)) {
                throw new ResourceNotFoundException(EntityValidator.ERRCODE_ENTITY_DOES_NOT_EXIST, "image",
                    userDetails.getUsername());
            }
            boolean protectedFolder = false;
            String currentPath = Paths.get(DEFAULT_AVATAR_PATH, fileName).toString();
            // get file from volume or else throw exception
            byte[] base64 = fileBrowserService.getFileStream(currentPath, protectedFolder);
            // return an informative object
            return AvatarDto.builder()
                    .filename(fileName)
                    .currentPath(currentPath)
                    .protectedFolder(protectedFolder)
                    .prevPath(DEFAULT_AVATAR_PATH)
                    .base64(base64)
                    .build();
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error extracting avatar", e);
            throw new RestServerError("Error extracting avatar", e);
        }
    }

    @Override
    public String updateAvatar(ProfileAvatarRequest request, UserDetails userDetails, BindingResult bindingResult) {
        try {
            String username = userDetails.getUsername();
            // remove previous image if present
            deletePrevUserAvatarFromFileSystemIfPresent(username);
            this.userPreferencesManager.updateUserGravatarPreference(userDetails.getUsername(), request.isUseGravatar());
            if (request.isUseGravatar()) {
                return null;
            }
            FileBrowserFileRequest fileBrowserFileRequest = addProfileImageToFileSystem(request, userDetails, bindingResult);
            return fileBrowserFileRequest.getFilename();
        } catch (Exception e) {
            log.error("Error updating avatar", e);
            throw new RestServerError("Error updating avatar", e);
        }
    }

    @Override
    public void deleteAvatar(UserDetails userDetails, BindingResult bindingResult) {
        try {
            String username = userDetails.getUsername();
            this.deletePrevUserAvatarFromFileSystemIfPresent(username);
            this.userPreferencesManager.updateUserGravatarPreference(userDetails.getUsername(), false);
        } catch (Exception e) {
            log.error("Error deleting avatar", e);
            throw new RestServerError("Error deleting avatar", e);
        }
    }

    @Override
    public void deleteAvatar(String username) throws EntException {
        this.deletePrevUserAvatarFromFileSystemIfPresent(username);
    }

    //------------------------ Utility methods ------------------------------------//

    private FileBrowserFileRequest addProfileImageToFileSystem(
            ProfileAvatarRequest request, UserDetails userDetails, BindingResult bindingResult) {
        // prepare a FileBrowserFileRequest to use the api already available in the system
        FileBrowserFileRequest fileBrowserFileRequest = convertToFileBrowserFileRequest(request, userDetails);
        // add the file to the volume
        fileBrowserService.addFile(fileBrowserFileRequest, bindingResult);
        return fileBrowserFileRequest;
    }

    private void deletePrevUserAvatarFromFileSystemIfPresent(String username) throws EntException {
        String filename = this.getAvatarFilenameByUsername(username);
        if (null == filename) {
            return;
        }
        String profilePicturePath = Paths.get(DEFAULT_AVATAR_PATH, filename).toString();
        fileBrowserService.deleteFile(profilePicturePath, false);
    }

    private String getAvatarFilenameByUsername(String username) throws EntException {
        if (!fileBrowserService.exists(DEFAULT_AVATAR_PATH)) {
            return null;
        }
        List<BasicFileAttributeViewDto> fileAttributes = fileBrowserService.browseFolder(DEFAULT_AVATAR_PATH, Boolean.FALSE);
        Optional<String> fileAvatar = fileAttributes.stream().filter(bfa -> !bfa.getDirectory())
                .filter(bfa -> {
                    int lastDotIndex = bfa.getName().lastIndexOf('.');
                    String name = (lastDotIndex > 0) ? bfa.getName().substring(0, lastDotIndex) : bfa.getName();
                    return name.equalsIgnoreCase(username);
                }).findAny().map(bfa -> bfa.getName());
        return fileAvatar.orElse(null);
    }

    private static FileBrowserFileRequest convertToFileBrowserFileRequest(ProfileAvatarRequest request,
            UserDetails userDetails) {
        FileBrowserFileRequest fileBrowserFileRequest = new FileBrowserFileRequest();
        String imageExtension = FilenameUtils.getExtension(request.getFilename());
        String filename = String.format("%s.%s", userDetails.getUsername(), imageExtension);
        fileBrowserFileRequest.setFilename(filename);
        fileBrowserFileRequest.setPath(Paths.get(DEFAULT_AVATAR_PATH, filename).toString());
        fileBrowserFileRequest.setProtectedFolder(false);
        fileBrowserFileRequest.setBase64(request.getBase64());
        return fileBrowserFileRequest;
    }

}
