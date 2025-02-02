/*
 * Copyright 2024-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.services.user.UserDetails;
import java.util.List;
import org.entando.entando.aps.system.exception.ResourceNotFoundException;
import org.entando.entando.aps.system.exception.RestServerError;
import org.entando.entando.aps.system.services.storage.IFileBrowserService;
import org.entando.entando.aps.system.services.storage.model.BasicFileAttributeViewDto;
import org.entando.entando.aps.system.services.userpreferences.IUserPreferencesManager;
import org.entando.entando.aps.system.services.userprofile.model.AvatarDto;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.web.userprofile.model.ProfileAvatarRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;

@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {
    
    @Mock
    private IFileBrowserService fileBrowserService;
    @Mock
    private IUserPreferencesManager userPreferencesManager;

    IAvatarService avatarService;

    @BeforeEach
    void init() {
        avatarService = new AvatarService(fileBrowserService, userPreferencesManager);
    }

    @Test
    void shouldGetAvatarDataReturnAvatarInfo() throws EntException {
        UserDetails user = Mockito.mock(UserDetails.class);
        when(user.getUsername()).thenReturn("username_test");
        when(userPreferencesManager.isUserGravatarEnabled(user.getUsername())).thenReturn(false);
        BasicFileAttributeViewDto dto = this.createMockFileAttributeDto("username_test", "png");
        when(fileBrowserService.exists("static/profile")).thenReturn(true);
        when(fileBrowserService.browseFolder("static/profile", false)).thenReturn(List.of(dto));
        when(fileBrowserService.getFileStream(any(), any())).thenReturn(new byte[0]);
        AvatarDto avatarData = avatarService.getAvatarData(user);
        assertEquals("username_test.png", avatarData.getFilename());
        assertEquals("static/profile/username_test.png", avatarData.getCurrentPath());
        Assertions.assertFalse(avatarData.isGravatar());
    }

    @Test
    void shouldGetAvatarDataReturnAvatarInfoWithGravatarEnabled() throws EntException {
        UserDetails user = Mockito.mock(UserDetails.class);
        when(user.getUsername()).thenReturn("username_test");
        when(userPreferencesManager.isUserGravatarEnabled(user.getUsername())).thenReturn(true);
        AvatarDto avatarData = avatarService.getAvatarData(user);
        verify(fileBrowserService, Mockito.times(0)).browseFolder(any(), any());
        verify(fileBrowserService, Mockito.times(0)).getFileStream(any(), any());
        Assertions.assertNull(avatarData.getFilename());
        Assertions.assertNull(avatarData.getCurrentPath());
        Assertions.assertTrue(avatarData.isGravatar());
    }

    @Test
    void shouldGetAvatarDataThrowResourceNotFoundExceptionIfNoImageIsPresent() throws EntException {
        assertThrows(ResourceNotFoundException.class, () -> avatarService.getAvatarData(mock(UserDetails.class)));
    }
    
    @Test
    void shouldUpdateAvatarAddProfilePictureFromTheRequest() throws EntException {
        BasicFileAttributeViewDto dtoDirectory = new BasicFileAttributeViewDto();
        dtoDirectory.setDirectory(true);
        dtoDirectory.setName("folder");
        BasicFileAttributeViewDto dto = this.createMockFileAttributeDto("test_username", "jpg");
        when(fileBrowserService.exists("static/profile")).thenReturn(true);
        when(fileBrowserService.browseFolder(Mockito.anyString(), Mockito.eq(false))).thenReturn(List.of(dtoDirectory, dto));
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("test_username");
        avatarService.updateAvatar(mock(ProfileAvatarRequest.class), userDetails, mock(BindingResult.class));
        verify(userPreferencesManager, Mockito.times(1)).updateUserGravatarPreference("test_username", false);
        verify(fileBrowserService, Mockito.times(1)).deleteFile(any(), any());
        verify(fileBrowserService, Mockito.times(1)).addFile(any(), any());
    }
    
    @Test
    void shouldUpdateAvatarWithGravatar() throws EntException {
        ProfileAvatarRequest request = new ProfileAvatarRequest(null, null, true);
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("test_username_gravatar");
        when(fileBrowserService.exists("static/profile")).thenReturn(false);
        avatarService.updateAvatar(request, userDetails, mock(BindingResult.class));
        verify(userPreferencesManager, Mockito.times(1)).updateUserGravatarPreference("test_username_gravatar", true);
        verify(fileBrowserService, Mockito.times(0)).deleteFile(any(), any());
        verify(fileBrowserService, Mockito.times(0)).addFile(any(), any());
    }
    
    @Test
    void shouldDeleteAvatar() throws EntException {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("user1");
        BasicFileAttributeViewDto dto = this.createMockFileAttributeDto("user1", "png");
        when(fileBrowserService.exists("static/profile")).thenReturn(true);
        when(fileBrowserService.browseFolder(Mockito.anyString(), Mockito.eq(false))).thenReturn(List.of(dto));
        avatarService.deleteAvatar(userDetails, mock(BindingResult.class));
        verify(fileBrowserService, Mockito.times(1)).deleteFile(any(), any());
        verify(userPreferencesManager, Mockito.times(1)).updateUserGravatarPreference("user1", false);
    }
    
    @Test
    void shouldDeleteAvatarThrowException() throws EntException {
        // set user details to return desired username
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("user1");
        BasicFileAttributeViewDto dto = this.createMockFileAttributeDto("user1", "jpg");
        when(fileBrowserService.exists("static/profile")).thenReturn(true);
        when(fileBrowserService.browseFolder(Mockito.anyString(), Mockito.eq(false))).thenReturn(List.of(dto));
        Mockito.doThrow(RuntimeException.class).when(fileBrowserService).deleteFile(Mockito.any(), Mockito.eq(false));
        assertThrows(RestServerError.class,
                () -> avatarService.deleteAvatar(userDetails, mock(BindingResult.class)));
    }
    
    private BasicFileAttributeViewDto createMockFileAttributeDto(String username, String fileExtention) {
        BasicFileAttributeViewDto dto = new BasicFileAttributeViewDto();
        dto.setDirectory(Boolean.FALSE);
        dto.setName(username + "." + fileExtention);
        return dto;
    }
    
}
