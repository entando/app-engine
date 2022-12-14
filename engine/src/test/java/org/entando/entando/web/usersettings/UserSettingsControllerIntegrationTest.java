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

import java.util.Map;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.baseconfig.SystemParamsUtils;
import com.agiletec.aps.system.services.user.UserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.entando.entando.aps.system.services.usersettings.IUserSettingsService;
import org.entando.entando.aps.system.services.usersettings.model.UserSettingsDto;
import org.entando.entando.web.AbstractControllerIntegrationTest;
import org.entando.entando.web.usersettings.model.UserSettingsRequest;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserSettingsControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired
    private IUserSettingsService userSettingsService;

    @Autowired
    private ConfigInterface configInterface;

    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        mapper = new ObjectMapper();
    }

    @Test
    void testGetSettings() throws Throwable {

        Map<String, String> params = this.getSystemParams();

        assertEquals("false", params.get(UserSettingsDto.EXTENDED_PRIVACY_MODULE_ENABLED));
        assertEquals("false", params.getOrDefault(UserSettingsDto.GRAVATAR_INTEGRATION_ENABLED, "false"));
        assertEquals("6", params.get(UserSettingsDto.MAX_MONTHS_SINCE_LASTACCESS));
        assertEquals("3", params.get(UserSettingsDto.MAX_MONTHS_SINCE_LASTPASSWORDCHANGE));

        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        ResultActions result = mockMvc.perform(
                get("/userSettings")
                        .header("Authorization", "Bearer " + accessToken));

        result.andExpect(status().isOk());

        result.andExpect(jsonPath("$.payload.restrictionsActive", is(Boolean.parseBoolean(params.get(UserSettingsDto.EXTENDED_PRIVACY_MODULE_ENABLED)))));
        result.andExpect(jsonPath("$.payload.enableGravatarIntegration", is(Boolean.parseBoolean(params.get(UserSettingsDto.GRAVATAR_INTEGRATION_ENABLED)))));
        result.andExpect(jsonPath("$.payload.lastAccessPasswordExpirationMonths", is(Integer.valueOf(params.get(UserSettingsDto.MAX_MONTHS_SINCE_LASTACCESS)))));
        result.andExpect(jsonPath("$.payload.maxMonthsPasswordValid", is(Integer.valueOf(params.get(UserSettingsDto.MAX_MONTHS_SINCE_LASTPASSWORDCHANGE)))));

    }

    @Test
    void testUpdateSettings() throws Throwable {
        String xmlParams = this.configInterface.getConfigItem(SystemConstants.CONFIG_ITEM_PARAMS);
        try {

            Map<String, String> params = this.getSystemParams();

            assertEquals("false", params.get(UserSettingsDto.EXTENDED_PRIVACY_MODULE_ENABLED));
            assertEquals("false", params.getOrDefault(UserSettingsDto.GRAVATAR_INTEGRATION_ENABLED, "false"));
            assertEquals("6", params.get(UserSettingsDto.MAX_MONTHS_SINCE_LASTACCESS));
            assertEquals("3", params.get(UserSettingsDto.MAX_MONTHS_SINCE_LASTPASSWORDCHANGE));

            UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
            String accessToken = mockOAuthInterceptor(user);

            ResultActions result = mockMvc.perform(
                    get("/userSettings")
                            .header("Authorization", "Bearer " + accessToken));

            result.andExpect(status().isOk());


            result.andExpect(jsonPath("$.payload.restrictionsActive",
                    is(Boolean.parseBoolean(params.get(UserSettingsDto.EXTENDED_PRIVACY_MODULE_ENABLED)))));

            result.andExpect(jsonPath("$.payload.enableGravatarIntegration",
                    is(Boolean.parseBoolean(params.get(UserSettingsDto.GRAVATAR_INTEGRATION_ENABLED)))));

            result.andExpect(jsonPath("$.payload.lastAccessPasswordExpirationMonths",
                    is(Integer.valueOf(params.get(UserSettingsDto.MAX_MONTHS_SINCE_LASTACCESS)))));

            result.andExpect(jsonPath("$.payload.maxMonthsPasswordValid",
                    is(Integer.valueOf(params.get(UserSettingsDto.MAX_MONTHS_SINCE_LASTPASSWORDCHANGE)))));

            //-------------
            UserSettingsRequest userSettingsRequest = new UserSettingsRequest();
            userSettingsRequest.setExtendedPrivacyModuleEnabled(true);
            userSettingsRequest.setGravatarIntegrationEnabled(true);
            userSettingsRequest.setMaxMonthsSinceLastAccess(60);
            userSettingsRequest.setMaxMonthsSinceLastPasswordChange(30);

            result = mockMvc.perform(
                    put("/userSettings")
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(mapper.writeValueAsString(userSettingsRequest))
                            .header("Authorization", "Bearer " + accessToken));

            result.andExpect(status().isOk());

            result.andExpect(jsonPath("$.payload.restrictionsActive", is(true)));
            result.andExpect(jsonPath("$.payload.enableGravatarIntegration", is(true)));
            result.andExpect(jsonPath("$.payload.lastAccessPasswordExpirationMonths", is(60)));
            result.andExpect(jsonPath("$.payload.maxMonthsPasswordValid", is(30)));

            //-------------
            userSettingsRequest = new UserSettingsRequest();
            userSettingsRequest.setExtendedPrivacyModuleEnabled(false);
            userSettingsRequest.setGravatarIntegrationEnabled(false);
            userSettingsRequest.setMaxMonthsSinceLastAccess(6);
            userSettingsRequest.setMaxMonthsSinceLastPasswordChange(3);

            result = mockMvc.perform(
                    put("/userSettings")
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(mapper.writeValueAsString(userSettingsRequest))
                            .header("Authorization", "Bearer " + accessToken));

            result.andExpect(status().isOk());

        } finally {
            this.configInterface.updateConfigItem(SystemConstants.CONFIG_ITEM_PARAMS, xmlParams);
        }

    }

    private Map<String, String> getSystemParams() throws Throwable {
        String xmlParams = this.configInterface.getConfigItem(SystemConstants.CONFIG_ITEM_PARAMS);
        return SystemParamsUtils.getParams(xmlParams);
    }

}
