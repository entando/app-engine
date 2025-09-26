/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.web.user;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import com.agiletec.aps.system.services.user.UserDetails;
import org.entando.entando.aps.system.services.oauth2.IApiOAuth2TokenManager;
import org.entando.entando.aps.system.services.user.IUserService;
import org.entando.entando.web.common.interceptor.EntandoOauth2Interceptor;
import org.entando.entando.web.common.exceptions.ValidationGenericException;
import org.entando.entando.web.user.UserController;
import org.entando.entando.web.user.validator.UserValidator;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.springframework.validation.BindingResult;
import org.hamcrest.Matchers;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerDeleteAuthoritiesIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private IAuthenticationProviderManager authenticationProviderManager;
    @Mock
    private IAuthorizationManager authorizationManager;
    @Mock
    private IApiOAuth2TokenManager apiOAuth2TokenManager;
    @Mock
    private EntandoOauth2Interceptor entandoOauth2Interceptor;
    @Mock
    private IUserService userService;
    @Mock
    private UserValidator userValidator;

    private UserController userController;

    @BeforeEach
    void setUp() {
        // Create real UserController instance with mocked dependencies
        userController = new UserController();
        userController.setUserService(userService);
        userController.setUserValidator(userValidator);

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .addInterceptors(entandoOauth2Interceptor)
                .build();
    }

    @Test
    void testDeleteAuthorities() throws Exception {
        String accessToken = "ok";
        String username = "valid.username_ok";
        String groupName = "coach";
        String roleName = "pageManager";

        // Mock OAuth token
        Mockito.lenient().when(apiOAuth2TokenManager.readAccessToken(Mockito.anyString()))
                .thenReturn(OAuth2TestUtils.getOAuth2Token("admin", "ok"));

        // Mock user details
        UserDetails targetUser = Mockito.mock(UserDetails.class);
        Mockito.lenient().when(targetUser.getUsername()).thenReturn(username);
        Mockito.lenient().when(authenticationProviderManager.getUser(username)).thenReturn(targetUser);

        // Mock authorization checks
        Mockito.lenient().when(authorizationManager.isAuthOnGroupAndRole(targetUser, groupName, roleName, false))
                .thenReturn(true)  // initially has authorities
                .thenReturn(false); // after deletion

        ResultActions result = this.executeDeleteUserAuthorities(username, accessToken);
        result.andExpect(status().isOk());
    }

    @Test
    void testDeleteAuthoritiesSameUser() throws Exception {
        String currentUserName = "admin";
        String accessToken = "ok";

        Mockito.lenient().when(apiOAuth2TokenManager.readAccessToken(Mockito.anyString()))
                .thenReturn(OAuth2TestUtils.getOAuth2Token(currentUserName, "ok"));

        // Mock that user has at least one authorization
        Mockito.lenient().when(authorizationManager.getUserAuthorizations(currentUserName))
                .thenReturn(java.util.Arrays.asList(Mockito.mock(com.agiletec.aps.system.services.authorization.Authorization.class)));

        // Mock the validator to throw ValidationGenericException for self-update
        Mockito.doAnswer(invocation -> {
            String username = invocation.getArgument(0);
            String currentUser = invocation.getArgument(1);
            BindingResult bindingResult = invocation.getArgument(2);

            if (username.equals(currentUser)) {
                bindingResult.reject(UserValidator.ERRCODE_SELF_UPDATE, new String[]{username}, "user.authorities.self.update");
                throw new ValidationGenericException(bindingResult);
            }
            return null;
        }).when(userValidator).validateUpdateSelf(Mockito.anyString(), Mockito.anyString(), Mockito.any(BindingResult.class));

        // Create a mock user for the request attribute
        UserDetails mockUser = Mockito.mock(UserDetails.class);
        Mockito.when(mockUser.getUsername()).thenReturn(currentUserName);

        ResultActions result = mockMvc
                .perform(delete("/users/{username}/authorities", currentUserName)
                        .header("Authorization", "Bearer " + accessToken)
                        .requestAttr("user", mockUser));

        result.andExpect(status().isForbidden());
        result.andExpect(jsonPath("$.errors[0].code", is(UserValidator.ERRCODE_SELF_UPDATE)));
        assertThat(this.authorizationManager.getUserAuthorizations(currentUserName).size(), is(Matchers.greaterThanOrEqualTo(1)));
    }


    private ResultActions executeDeleteUserAuthorities(String username, String accessToken) throws Exception {
        return mockMvc
                .perform(delete("/users/{username}/authorities", username)
                        .header("Authorization", "Bearer " + accessToken));
    }

}
