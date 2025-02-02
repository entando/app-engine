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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.entando.entando.aps.system.exception.ResourceNotFoundException;
import org.entando.entando.aps.system.services.entity.model.EntityDto;
import org.entando.entando.aps.system.services.userprofile.IAvatarService;
import org.entando.entando.aps.system.services.userprofile.IUserProfileManager;
import org.entando.entando.aps.system.services.userprofile.IUserProfileService;
import org.entando.entando.aps.system.services.userprofile.model.AvatarDto;
import org.entando.entando.aps.system.services.userprofile.model.IUserProfile;
import org.entando.entando.web.AbstractControllerTest;
import org.entando.entando.web.userprofile.model.ProfileAvatarRequest;
import org.entando.entando.web.userprofile.validator.ProfileAvatarValidator;
import org.entando.entando.web.userprofile.validator.ProfileValidator;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest extends AbstractControllerTest {

    @Mock
    private ProfileValidator profileValidator;

    @Mock
    private IUserManager userManager;

    @Mock
    private IUserProfileService userProfileService;

    @Mock
    private IUserProfileManager userProfileManager;

    private ProfileAvatarValidator profileAvatarValidator;

    @Mock
    private IAvatarService avatarService;

    @BeforeEach
    public void setUp() throws Exception {
        profileAvatarValidator = new ProfileAvatarValidator(userProfileManager);
        ProfileController controller = new ProfileController(userProfileService, profileValidator,
                profileAvatarValidator, userManager,
                userProfileManager, avatarService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(entandoOauth2Interceptor)
                .setMessageConverters(getMessageConverters())
                .setHandlerExceptionResolvers(createHandlerExceptionResolver())
                .build();
    }

    @Test
    void shouldGetExistingProfile() throws Exception {
        when(this.profileValidator.existProfile("user_with_profile")).thenReturn(true);
        when(this.userProfileService.getUserProfile("user_with_profile")).thenReturn(new EntityDto());
        ResultActions result = performGetUserProfiles("user_with_profile");
        result.andExpect(status().isOk());
    }

    @Test
    void shouldGetNewlyCreatedProfile() throws Exception {
        when(this.userManager.getUser("user_without_profile")).thenReturn(Mockito.mock(UserDetails.class));
        when(this.userProfileManager.getDefaultProfileType()).thenReturn(Mockito.mock(IUserProfile.class));
        ResultActions result = performGetUserProfiles("user_without_profile");
        result.andExpect(status().isOk());
    }

    @Test
    void testUnexistingProfile() throws Exception {
        ResultActions result = performGetUserProfiles("user_without_profile");
        result.andExpect(status().isNotFound());
    }

    @Test
    void testAddProfile() throws Exception {
        when(this.userProfileService.addUserProfile(any(EntityDto.class), any(BindingResult.class)))
                .thenReturn(new EntityDto());

        String mockJson = "{\n"
                + "    \"id\": \"user\",\n"
                + "    \"typeCode\": \"" + SystemConstants.DEFAULT_PROFILE_TYPE_CODE + "\",\n"
                + "    \"attributes\": [\n"
                + "         {\"code\": \"fullname\", \"value\": \"User\"},\n"
                + "         {\"code\": \"email\", \"value\": \"test@example.com\"}\n"
                + "    ]}";

        ResultActions result = performPostUserProfiles(mockJson);
        result.andExpect(status().isOk());
    }

    @Test
    void testUpdateProfile() throws Exception {
        when(this.userProfileService.updateUserProfile(any(EntityDto.class), any(BindingResult.class)))
                .thenReturn(new EntityDto());

        String mockJson = "{\n"
                + "    \"id\": \"user\",\n"
                + "    \"typeCode\": \"" + SystemConstants.DEFAULT_PROFILE_TYPE_CODE + "\",\n"
                + "    \"attributes\": [\n"
                + "         {\"code\": \"fullname\", \"value\": \"User Renamed\"},\n"
                + "         {\"code\": \"email\", \"value\": \"test@example.com\"}\n"
                + "    ]}";

        ResultActions result = performPutUserProfiles("user", mockJson);
        result.andExpect(status().isOk());
    }

    @Test
    void shouldGetAvatarReturn404IfImageNotExists() throws Exception {
        String accessToken = this.createAccessToken();
        // simulate an image not found
        when(avatarService.getAvatarData(any())).thenThrow(
                new ResourceNotFoundException("1", "image", "static/profile/jack_bauer.png"));

        ResultActions result = mockMvc.perform(
                get("/userProfiles/avatar?fileName=myFile.png")
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].message").value(
                        "a image with static/profile/jack_bauer.png code could not be found"));
    }

    @Test
    void shouldGetAvatarReturn200AndWellFormedResponseIfImageExists() throws Exception {
        String accessToken = this.createAccessToken();
        AvatarDto avatarDto = AvatarDto.builder()
                .base64(new byte[0])
                .protectedFolder(false)
                .currentPath("static/profile/jack_bauer.png")
                .filename("jack_bauer.png")
                .prevPath("static/profile")
                .build();
        when(avatarService.getAvatarData(any())).thenReturn(avatarDto);

        ResultActions result = mockMvc.perform(
                get("/userProfiles/avatar?fileName=myFile.png")
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.protectedFolder").value(avatarDto.isProtectedFolder()))
                .andExpect(jsonPath("$.payload.isDirectory").value(false))
                .andExpect(jsonPath("$.payload.path").value(avatarDto.getCurrentPath()))
                .andExpect(jsonPath("$.payload.filename").value(avatarDto.getFilename()))
                .andExpect(jsonPath("$.payload.base64").value(""))
                .andExpect(jsonPath("$.metaData.prevPath").value(avatarDto.getPrevPath()));

    }

    @Test
    void shouldPostAvatarReturn400OnIllegalInput() throws Exception {
        String accessToken = this.createAccessToken();
        Answer<Void> ans = invocation -> {
            Object[] args = invocation.getArguments();
            ((BindingResult) args[1]).rejectValue("filename", "1", new String[]{"fileName_without_extension"},
                    "fileBrowser.filename.invalidFilename");
            return null;
        };
        ProfileAvatarRequest profileAvatarRequest = new ProfileAvatarRequest("fileName_without_extension",
                new byte[1], false);
        ResultActions result = mockMvc.perform(
                post("/userProfiles/avatar")
                        .content(new ObjectMapper().writeValueAsString(profileAvatarRequest))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isBadRequest());
    }

    @Test
    void shouldPostAvatarReturn200OnRightInput() throws Exception {
        String accessToken = this.createAccessToken();
        ProfileAvatarRequest profileAvatarRequest = new ProfileAvatarRequest("myFile.png", 
                IOUtils.toByteArray(new ClassPathResource("userprofile/image.png").getInputStream()), false);
        when(avatarService.updateAvatar(any(), any(), any())).thenReturn("jack_bauer.png");
        ResultActions result = mockMvc.perform(
                post("/userProfiles/avatar")
                        .content(new ObjectMapper().writeValueAsString(profileAvatarRequest))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.filename").value("jack_bauer.png"));
    }

    @Test
    void shouldPostAvatarReturn400OnFileServiceAddFailureIfFileAlreadyPresent() throws Exception {
        String accessToken = this.createAccessToken();
        Answer<Void> ans = invocation -> {
            Object[] args = invocation.getArguments();
            ((BindingResult) args[2]).reject("2", new String[]{"static/profile/jack-bauer.png", "false"},
                    "fileBrowser.file.exists");
            return null;
        };
        Mockito.lenient().doAnswer(ans).when(avatarService).updateAvatar(any(), any(), any());
        ProfileAvatarRequest profileAvatarRequest = new ProfileAvatarRequest("image.png",
                new byte[1], false);
        ResultActions result = mockMvc.perform(
                post("/userProfiles/avatar")
                        .content(new ObjectMapper().writeValueAsString(profileAvatarRequest))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isBadRequest());
    }


    @ParameterizedTest
    @MethodSource("provideValuesFor400")
    void shouldPostAvatarReturn400(String request, String expectedErrorCode) throws Exception {
        String accessToken = this.createAccessToken();
        ResultActions result = mockMvc.perform(
                post("/userProfiles/avatar")
                        .content(request)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].code").value(expectedErrorCode));
    }
    
    @Test
    void shouldDeleteAvatarReturn200() throws Exception {
        String accessToken = this.createAccessToken();
        ResultActions result = mockMvc.perform(
                delete("/userProfiles/avatar")
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.username").value("jack_bauer"))
                .andExpect(jsonPath("$.errors.size()", CoreMatchers.is(0)))
                .andExpect(jsonPath("$.metaData.size()", CoreMatchers.is(0)));
    }
    
    private static Stream<Arguments> provideValuesFor400() {
        return Stream.of(
                Arguments.of("{\"filenam\":\"image.png\",\"base64\":\"AA==\"}", "52"),
                Arguments.of("{\"base64\":\"AA==\"}", "52"),
                Arguments.of("{\"filename\":\"\",\"base64\":\"AA==\"}", "52"),
                Arguments.of("{\"filename\":\"image.png\",\"base6\":\"AA==\"}", "53"),
                Arguments.of("{\"filename\":\"image.png\"}", "53"),
                Arguments.of("{\"filename\":\"image.png\",\"base64\":\"\"}", "2")
        );
    }

    private ResultActions performGetUserProfiles(String username) throws Exception {
        String accessToken = this.createAccessToken();
        return mockMvc.perform(
                get("/userProfiles/{username}", username)
                        .header("Authorization", "Bearer " + accessToken));
    }

    private ResultActions performPostUserProfiles(String jsonContent) throws Exception {
        String accessToken = this.createAccessToken();
        return mockMvc.perform(
                post("/userProfiles")
                        .content(jsonContent)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));

    }

    private ResultActions performPutUserProfiles(String username, String jsonContent) throws Exception {
        String accessToken = this.createAccessToken();
        return mockMvc.perform(
                put("/userProfiles/{username}", username)
                        .content(jsonContent)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
    }

    private String createAccessToken() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        return mockOAuthInterceptor(user);
    }
    
}
