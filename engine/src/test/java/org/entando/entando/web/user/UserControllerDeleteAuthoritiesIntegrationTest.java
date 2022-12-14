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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.User;
import com.agiletec.aps.system.services.user.UserDetails;
import org.entando.entando.TestEntandoJndiUtils;
import org.entando.entando.aps.system.services.oauth2.IApiOAuth2TokenManager;
import org.entando.entando.web.common.interceptor.EntandoOauth2Interceptor;
import org.entando.entando.web.user.validator.UserValidator;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CorsFilter;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
    "classpath*:spring/testpropertyPlaceholder.xml",
    "classpath*:spring/baseSystemConfig.xml",
    "classpath*:spring/aps/**/**.xml",
    "classpath*:spring/plugins/**/aps/**/**.xml",
    "classpath*:spring/web/**.xml",})
@WebAppConfiguration(value = "")
class UserControllerDeleteAuthoritiesIntegrationTest {

    protected MockMvc mockMvc;

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected IAuthenticationProviderManager authenticationProviderManager;

    @Autowired
    protected IAuthorizationManager authorizationManager;

    @Autowired
    IUserManager userManager;

    @Mock
    protected IApiOAuth2TokenManager apiOAuth2TokenManager;

    @Autowired
    @InjectMocks
    protected EntandoOauth2Interceptor entandoOauth2Interceptor;

    @Autowired
    protected CorsFilter corsFilter;

    @BeforeAll
    public static void setup() throws Exception {
        TestEntandoJndiUtils.setupJndi();
    }

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

       mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(corsFilter)
                .build();

        //workaround for dirty context
        entandoOauth2Interceptor.setAuthenticationProviderManager(authenticationProviderManager);
    }

    @Test
    void testDeleteAuthorities() throws Exception {
        String accessToken = "ok";
        when(apiOAuth2TokenManager.readAccessToken(Mockito.anyString())).thenReturn(OAuth2TestUtils.getOAuth2Token("admin", "ok"));

        String username = "valid.username_ok";
        String password = "valid.123_ok";
        String groupName = "coach";
        String roleName = "pageManager";
        try {
            this.addUserWithAuthorization(username, password, groupName, roleName);
            UserDetails targetUser = this.authenticationProviderManager.getUser(username);
            boolean hasAuthorities = this.authorizationManager.isAuthOnGroupAndRole(targetUser, groupName, roleName, false);
            assertThat(hasAuthorities, is(true));

            ResultActions result = this.executeDeleteUserAuthorities(username, accessToken);
            result.andExpect(status().isOk());

            targetUser = this.authenticationProviderManager.getUser(username);
            hasAuthorities = this.authorizationManager.isAuthOnGroupAndRole(targetUser, groupName, roleName, false);
            assertThat(hasAuthorities, is(false));
        } catch (Throwable e) {
            throw e;
        } finally {
            this.authorizationManager.deleteUserAuthorizations(username);
            this.userManager.removeUser(username);
        }
    }

    @Test
    void testDeleteAuthoritiesSameUser() throws Exception {
        String currentUserName = "admin";
        String accessToken = "ok";
        when(apiOAuth2TokenManager.readAccessToken(Mockito.anyString())).thenReturn(OAuth2TestUtils.getOAuth2Token(currentUserName, "ok"));
        try {
            ResultActions result = this.executeDeleteUserAuthorities(currentUserName, accessToken);
            result.andExpect(status().isForbidden());
            result.andExpect(jsonPath("$.errors[0].code", is(UserValidator.ERRCODE_SELF_UPDATE)));
            assertThat(this.authorizationManager.getUserAuthorizations(currentUserName).size(), is(Matchers.greaterThanOrEqualTo(1)));
        } catch (Throwable e) {
            throw e;
        }
    }

    protected void addUserWithAuthorization(String username, String password, String groupName, String roleName) throws EntException {
        User testUser = new User();
        testUser.setUsername(username);
        testUser.setPassword(password);
        this.userManager.addUser(testUser);
        this.authorizationManager.addUserAuthorization(username, groupName, roleName);
    }

    private ResultActions executeDeleteUserAuthorities(String username, String accessToken) throws Exception {
        ResultActions result = mockMvc
                .perform(delete("/users/{username}/authorities", new Object[]{username})
                        .header("Authorization", "Bearer " + accessToken));
        return result;
    }

}
