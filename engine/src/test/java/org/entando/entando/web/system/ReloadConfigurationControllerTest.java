/*
 * Copyright 2018-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.web.system;

import com.agiletec.aps.system.services.user.UserDetails;
import org.entando.entando.web.AbstractControllerIntegrationTest;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReloadConfigurationControllerTest extends AbstractControllerIntegrationTest {

    @Autowired
    @InjectMocks
    private ReloadConfigurationController controller;

    @Test
    void should_execute_reload_and_have_headers() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc
                .perform(post("/reloadConfiguration")
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk());
        testCors("/reloadConfiguration", HttpMethod.POST);
    }

    @Test
    void shouldReturnProgressStatusWhenReloadConfigurationStarts() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        ResultActions result = mockMvc
                .perform(post("/reloadConfiguration")
                        .header("Authorization", "Bearer " + accessToken));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.status").value("progress"))
                .andExpect(jsonPath("$.payload.percentage").exists());
    }

    @Test
    void shouldReturnOkWhenReloadAlreadyInProgress() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        // Start first reload
        mockMvc.perform(post("/reloadConfiguration")
                .header("Authorization", "Bearer " + accessToken));

        // Try to start another reload while first is in progress
        ResultActions result = mockMvc
                .perform(post("/reloadConfiguration")
                        .header("Authorization", "Bearer " + accessToken));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.status").value("progress"));
    }

    @Test
    void shouldGetReloadStatus() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        ResultActions result = mockMvc
                .perform(get("/reloadConfiguration/status")
                        .header("Authorization", "Bearer " + accessToken));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").exists())
                .andExpect(jsonPath("$.payload.status").exists())
                .andExpect(jsonPath("$.payload.percentage").exists());
    }

    @Test
    void testGetReloadProgressDuringActiveReload() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        // Start a reload
        mockMvc.perform(post("/reloadConfiguration")
                .header("Authorization", "Bearer " + accessToken));

        // Check progress immediately while reload might be in progress
        int progress = controller.getReloadProgress();

        // Progress can be either:
        // - Between 0 and 100 if we catch it during active reload
        // - -1 if the reload completed very quickly
        assertTrue((progress >= 0 && progress <= 100) || progress == -1,
            "Progress should be between 0-100 (active reload) or -1 (completed), but was: " + progress);
    }

    @Test
    void testGetReloadInfo() {
        Map<String, String> info = controller.getReloadInfo();
        assertNotNull(info);
    }

    @Test
    void testIsReloadingErrorDetectWhenNoErrors() {
        // When there are no errors, should return false
        boolean hasError = controller.isReloadingErrorDetect();
        // We can't assert the exact value since it depends on the system state
        assertFalse(hasError);
    }

    @Test
    void testReloadConfigResponseCreation() {
        // Test the record creation
        Map<String, String> info = new HashMap<>();
        info.put("test", "value");

        ReloadConfigurationController.ReloadConfigResponse response =
                new ReloadConfigurationController.ReloadConfigResponse("success", 100, info);

        assertEquals("success", response.status());
        assertEquals(100, response.percentage());
        assertEquals(info, response.info());
    }

    @Test
    void testReloadConfigResponseWithNullPercentage() {
        ReloadConfigurationController.ReloadConfigResponse response =
                new ReloadConfigurationController.ReloadConfigResponse("success", null, null);

        assertEquals("success", response.status());
        assertNull(response.percentage());
        assertNull(response.info());
    }

    @Test
    void testReloadStatusCorsHeaders() throws Exception {
        testCors("/reloadConfiguration/status", HttpMethod.GET);
    }

    @Test
    void shouldRequireAuthenticationForReloadConfiguration() throws Exception {
        ResultActions result = mockMvc
                .perform(post("/reloadConfiguration"));

        result.andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireAuthenticationForReloadStatus() throws Exception {
        ResultActions result = mockMvc
                .perform(get("/reloadConfiguration/status"));

        result.andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRequireSuperuserPermissionForReloadConfiguration() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("user", "0x24").build();
        String accessToken = mockOAuthInterceptor(user);

        ResultActions result = mockMvc
                .perform(post("/reloadConfiguration")
                        .header("Authorization", "Bearer " + accessToken));

        result.andExpect(status().isForbidden());
    }

    @Test
    void shouldRequireSuperuserPermissionForReloadStatus() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("user", "0x24").build();
        String accessToken = mockOAuthInterceptor(user);

        ResultActions result = mockMvc
                .perform(get("/reloadConfiguration/status")
                        .header("Authorization", "Bearer " + accessToken));

        result.andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnJsonContentType() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        ResultActions result = mockMvc
                .perform(post("/reloadConfiguration")
                        .header("Authorization", "Bearer " + accessToken));

        result.andExpect(content().contentType("application/json"));
    }

    @Test
    void shouldReturnJsonContentTypeForStatus() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        ResultActions result = mockMvc
                .perform(get("/reloadConfiguration/status")
                        .header("Authorization", "Bearer " + accessToken));

        result.andExpect(content().contentType("application/json"));
    }

    @Test
    void shouldHandleMultipleStatusChecks() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        // Check status multiple times
        for (int i = 0; i < 3; i++) {
            ResultActions result = mockMvc
                    .perform(get("/reloadConfiguration/status")
                            .header("Authorization", "Bearer " + accessToken));

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.payload").exists());
        }
    }

    @Test
    void testReloadInfoReturnsNewInstance() {
        Map<String, String> info1 = controller.getReloadInfo();
        Map<String, String> info2 = controller.getReloadInfo();

        // Should return new instances each time
        assertNotSame(info1, info2, "getReloadInfo should return a new HashMap instance");
    }

    @Test
    void shouldReturnPayloadWithAllRequiredFields() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        ResultActions result = mockMvc
                .perform(get("/reloadConfiguration/status")
                        .header("Authorization", "Bearer " + accessToken));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.status").exists())
                .andExpect(jsonPath("$.payload.percentage").exists())
                .andExpect(jsonPath("$.payload.info").exists());
    }

}
