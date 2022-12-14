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
package org.entando.entando.web.guifragment;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.user.UserDetails;
import org.entando.entando.aps.system.services.guifragment.GuiFragment;
import org.entando.entando.aps.system.services.guifragment.IGuiFragmentManager;
import org.entando.entando.web.AbstractControllerIntegrationTest;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;

class GuiFragmentSettingsControllerIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigInterface configManager;

    @Autowired
    private IGuiFragmentManager guiFragmentManager;

    @Test
    void testGetConfiguration() throws Exception {
        String value = this.guiFragmentManager.getConfig(IGuiFragmentManager.CONFIG_PARAM_EDIT_EMPTY_FRAGMENT_ENABLED);
        Assertions.assertTrue(null == value || value.equalsIgnoreCase("false"));
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc.perform(
                get("/fragmentsSettings")
                .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.errors", Matchers.hasSize(0)));
        result.andExpect(jsonPath("$.payload." + GuiFragmentSettingsController.RESULT_PARAM_NAME, is(false)));
    }

    @Test
    void testUpdateConfiguration() throws Exception {
        String value = this.guiFragmentManager.getConfig(IGuiFragmentManager.CONFIG_PARAM_EDIT_EMPTY_FRAGMENT_ENABLED);
        Assertions.assertTrue(null == value || value.equalsIgnoreCase("false"));
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        try {
            String payload = "{\"enableEditingWhenEmptyDefaultGui\":true}";
            ResultActions result = this.executePut(payload, accessToken, status().isOk());
            result.andExpect(jsonPath("$.errors", Matchers.hasSize(0)));
            result.andExpect(jsonPath("$.payload." + GuiFragmentSettingsController.RESULT_PARAM_NAME, is(true)));
            value = this.guiFragmentManager.getConfig(IGuiFragmentManager.CONFIG_PARAM_EDIT_EMPTY_FRAGMENT_ENABLED);
            Assertions.assertEquals("true", value);
            value = this.configManager.getParam(IGuiFragmentManager.CONFIG_PARAM_EDIT_EMPTY_FRAGMENT_ENABLED);
            Assertions.assertEquals("true", value);

            payload = "{\"enableEditingWhenEmptyDefaultGui\":false}";
            result = this.executePut(payload, accessToken, status().isOk());
            result.andExpect(jsonPath("$.errors", Matchers.hasSize(0)));
            result.andExpect(jsonPath("$.payload." + GuiFragmentSettingsController.RESULT_PARAM_NAME, is(false)));
            value = this.guiFragmentManager.getConfig(IGuiFragmentManager.CONFIG_PARAM_EDIT_EMPTY_FRAGMENT_ENABLED);
            Assertions.assertEquals("false", value);

        } catch (Exception e) {
            throw e;
        } finally {
            this.configManager.updateParam(IGuiFragmentManager.CONFIG_PARAM_EDIT_EMPTY_FRAGMENT_ENABLED, "false");
            value = this.configManager.getParam(IGuiFragmentManager.CONFIG_PARAM_EDIT_EMPTY_FRAGMENT_ENABLED);
            Assertions.assertEquals("false", value);
            value = this.guiFragmentManager.getConfig(IGuiFragmentManager.CONFIG_PARAM_EDIT_EMPTY_FRAGMENT_ENABLED);
            Assertions.assertEquals("false", value);
        }
    }

    @Test
    void testGetPlugins() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = this.executeGet("/fragments/info/plugins", accessToken, status().isOk());
        result.andExpect(jsonPath("$.payload.size()", is(0)));
    }

    @Test
    void testGetPluginsMapping() throws Exception {
        String code = "info";
        GuiFragment fragment = new GuiFragment();
        fragment.setCode(code);
        fragment.setDefaultGui("hello world");
        this.guiFragmentManager.addGuiFragment(fragment);
        try {
            UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
            String accessToken = mockOAuthInterceptor(user);

            ResultActions result = this.executeGet("/fragments/info/plugins", accessToken, status().isOk());
            result.andExpect(jsonPath("$.payload.size()", is(0)));

            ResultActions resultFragment = this.executeGet("/fragments/info", accessToken, status().isOk());
            resultFragment.andExpect(jsonPath("$.payload.code", is(code)));
            resultFragment.andExpect(jsonPath("$.payload.locked", is(false)));
        } finally {
            this.guiFragmentManager.deleteGuiFragment(code);
        }
    }

    private ResultActions executeGet(String uri, String accessToken, ResultMatcher rm) throws Exception {
        ResultActions result = mockMvc
                .perform(get(uri)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(rm);
        return result;
    }

    private ResultActions executePut(String body, String accessToken, ResultMatcher rm) throws Exception {
        ResultActions result = mockMvc
                .perform(put("/fragmentsSettings").content(body)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(rm);
        return result;
    }

}
