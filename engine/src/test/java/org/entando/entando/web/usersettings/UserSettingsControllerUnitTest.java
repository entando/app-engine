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
package org.entando.entando.web.usersettings;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.services.user.UserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.entando.entando.aps.system.services.usersettings.UserSettingsService;
import org.entando.entando.web.AbstractControllerTest;
import org.entando.entando.web.usersettings.model.UserSettingsRequest;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserSettingsControllerUnitTest extends AbstractControllerTest {

    @Mock
    private UserSettingsService userSettingsService;

    @InjectMocks
    private UserSettingsController controller;

    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(entandoOauth2Interceptor)
                .setMessageConverters(getMessageConverters())
                .setHandlerExceptionResolvers(createHandlerExceptionResolver())
                .build();
        mapper = new ObjectMapper();
    }

    @Test
    void testUpdateSettingsWithEmptyParams() throws Throwable {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        UserSettingsRequest userSettingsRequest = new UserSettingsRequest();

        ResultActions result = mockMvc.perform(
                put("/userSettings")
                        .content(mapper.writeValueAsString(userSettingsRequest))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));

        result.andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateSettingsWithInvalidParams() throws Throwable {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        UserSettingsRequest userSettingsRequest = new UserSettingsRequest();
        userSettingsRequest.setMaxMonthsSinceLastAccess(-6);
        userSettingsRequest.setMaxMonthsSinceLastPasswordChange(-3);

        ResultActions result = mockMvc.perform(
                put("/userSettings")
                        .content(mapper.writeValueAsString(userSettingsRequest))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));

        result.andExpect(status().isBadRequest());
    }

}
