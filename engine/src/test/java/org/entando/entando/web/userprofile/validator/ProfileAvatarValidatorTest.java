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
package org.entando.entando.web.userprofile.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.user.UserDetails;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import javax.imageio.ImageIO;
import org.apache.commons.io.IOUtils;
import org.entando.entando.aps.system.services.userprofile.IUserProfileManager;
import org.entando.entando.aps.system.services.userprofile.model.IUserProfile;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.entando.entando.web.userprofile.model.ProfileAvatarRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

@ExtendWith(MockitoExtension.class)
class ProfileAvatarValidatorTest {
    
    @Mock
    private IUserProfileManager userProfileManager;
    
    @InjectMocks
    private ProfileAvatarValidator profileAvatarValidator;

    @Test
    void shouldSupportOnlyProfileAvatarRequest() {
        assertTrue(profileAvatarValidator.supports(ProfileAvatarRequest.class));
        assertFalse(profileAvatarValidator.supports(Object.class));
    }

    @Test
    void shouldNotValidateFileNamesMissingExtensions() throws IOException {
        ProfileAvatarRequest request = new ProfileAvatarRequest("missing_extension_filename",
                IOUtils.toByteArray(new ClassPathResource("userprofile/image.png").getInputStream()), false);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "profileAvatarRequest");
        profileAvatarValidator.validate(request, Mockito.mock(UserDetails.class), errors);
        assertEquals(1, errors.getErrorCount());
        assertEquals("fileBrowser.filename.invalidFilename", errors.getAllErrors().get(0).getDefaultMessage());
        assertEquals("missing_extension_filename", ((FieldError) errors.getAllErrors().get(0)).getRejectedValue());
    }

    @Test
    void shouldNotValidateUserInCaseOfMissingProfile() throws Exception {
        ProfileAvatarRequest request = new ProfileAvatarRequest(null, null, true);
        UserDetails user = Mockito.mock(UserDetails.class);
        Mockito.when(user.getUsername()).thenReturn("username_test");
        Mockito.when(userProfileManager.getProfile("username_test")).thenReturn(null);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "profileAvatarRequest");
        profileAvatarValidator.validate(request, user, errors);
        assertEquals(1, errors.getErrorCount());
        assertEquals("avatar.emailAttribute.missing", errors.getAllErrors().get(0).getDefaultMessage());
        assertEquals("useGravatar", ((FieldError) errors.getAllErrors().get(0)).getField());
    }
    
    @Test
    void shouldNotValidateUserInCaseOfMissingEmailAttribute() throws Exception {
        ProfileAvatarRequest request = new ProfileAvatarRequest(null, null, true);
        UserDetails user = Mockito.mock(UserDetails.class);
        Mockito.when(user.getUsername()).thenReturn("username_test");
        IUserProfile userProfile = Mockito.mock(IUserProfile.class);
        Mockito.when(userProfileManager.getProfile("username_test")).thenReturn(userProfile);
        Mockito.when(userProfile.getAttributeByRole(SystemConstants.USER_PROFILE_ATTRIBUTE_ROLE_MAIL)).thenReturn(null);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "profileAvatarRequest");
        profileAvatarValidator.validate(request, user, errors);
        assertEquals(1, errors.getErrorCount());
        assertEquals("avatar.emailAttribute.missing", errors.getAllErrors().get(0).getDefaultMessage());
        assertEquals("useGravatar", ((FieldError) errors.getAllErrors().get(0)).getField());
    }

    @Test
    void shouldThrowServerErrorInCaseOfErrorGettingUserProfile() throws Exception {
        ProfileAvatarRequest request = new ProfileAvatarRequest(null, null, true);
        UserDetails user = Mockito.mock(UserDetails.class);
        Mockito.when(user.getUsername()).thenReturn("username_test");
        Mockito.when(userProfileManager.getProfile("username_test")).thenThrow(EntException.class);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "profileAvatarRequest");
        assertThrows(EntRuntimeException.class, () -> profileAvatarValidator.validate(request, user, errors));
    }
    
    @Test
    void shouldNotValidateFilesOtherThanImages() {
        String notValidBase64Image = "bm90IGFuIGltYWdl";
        ProfileAvatarRequest request = new ProfileAvatarRequest("valid_filename.txt", notValidBase64Image.getBytes(), false);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "profileAvatarRequest");
        profileAvatarValidator.validate(request, Mockito.mock(UserDetails.class), errors);
        assertEquals(1, errors.getErrorCount());
        assertEquals("fileBrowser.file.invalidType", errors.getAllErrors().get(0).getDefaultMessage());
        assertEquals("base64", ((FieldError) errors.getAllErrors().get(0)).getField());
    }

    @Test
    void shouldThrowUncheckedIOExceptionIfImageReadingFails() throws IOException {
        ProfileAvatarRequest request = new ProfileAvatarRequest("image.png",
                IOUtils.toByteArray(new ClassPathResource("userprofile/image.png").getInputStream()), false);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "profileAvatarRequest");
        try (MockedStatic<ImageIO> mockStatic = Mockito.mockStatic(ImageIO.class)) {
            mockStatic.when(() -> ImageIO.read(any(InputStream.class))).thenThrow(IOException.class);
            assertThrows(UncheckedIOException.class, () -> profileAvatarValidator.validate(request, Mockito.mock(UserDetails.class), errors));
        }
    }

    @Test
    void shouldValidateAcceptValidImageWithValidFileName() throws IOException {
        ProfileAvatarRequest request = new ProfileAvatarRequest("image.png",
                IOUtils.toByteArray(new ClassPathResource("userprofile/image.png").getInputStream()), false);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "profileAvatarRequest");
        profileAvatarValidator.validate(request, Mockito.mock(UserDetails.class), errors);
        assertTrue(errors.getAllErrors().isEmpty());
    }
    
}
