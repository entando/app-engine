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
package org.entando.entando.jpversioning.web.configuration;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.role.Permission;
import com.agiletec.aps.system.services.user.UserDetails;
import javax.servlet.http.HttpSession;
import org.entando.entando.plugins.jpversioning.services.configuration.VersioningConfigurationService;
import org.entando.entando.plugins.jpversioning.web.configuration.VersioningConfigurationController;
import org.entando.entando.plugins.jpversioning.web.configuration.model.VersioningConfigurationDTO;
import org.entando.entando.web.AbstractControllerTest;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
public class VersioningConfigurationControllerTest extends AbstractControllerTest {

    @Mock
    private VersioningConfigurationService service;

    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private VersioningConfigurationController controller;

    @BeforeEach
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(entandoOauth2Interceptor)
                .setMessageConverters(getMessageConverters())
                .setHandlerExceptionResolvers(createHandlerExceptionResolver())
                .build();
    }

    @Test
    void testGetExistingContentVersioning() throws Exception {
        VersioningConfigurationDTO configuration = new VersioningConfigurationDTO();
        UserDetails user = this.createUser(true);
        Mockito.lenient().when(this.httpSession.getAttribute("user")).thenReturn(user);
        when(this.service.getVersioningConfiguration())
                .thenReturn(configuration);
        ResultActions result = getVersioningConfiguration(user);
        result.andExpect(status().isOk());
    }

    @Test
    void testNotAuthorizedGetExistingContentVersioning() throws Exception {
        VersioningConfigurationDTO configuration = new VersioningConfigurationDTO();
        UserDetails user = this.createUser(false);
        Mockito.lenient().when(this.httpSession.getAttribute("user")).thenReturn(user);
        Mockito.lenient().when(this.service.getVersioningConfiguration())
                .thenReturn(configuration);
        ResultActions result = getVersioningConfiguration(user);
        result.andExpect(status().isForbidden());
    }

    @Test
    void testPutExistingContentVersioning() throws Exception {
        VersioningConfigurationDTO configuration = new VersioningConfigurationDTO();
        UserDetails user = this.createUser(true);
        Mockito.lenient().when(this.httpSession.getAttribute("user")).thenReturn(user);
        Mockito.lenient().when(this.service.putVersioningConfiguration(Mockito.any(VersioningConfigurationDTO.class)))
                .thenReturn(configuration);
        ResultActions result = getVersioningConfiguration(user);
        result.andExpect(status().isOk());
    }

    @Test
    void testNotAuthorizedPutExistingContentVersioning() throws Exception {
        VersioningConfigurationDTO configuration = new VersioningConfigurationDTO();
        UserDetails user = this.createUser(false);
        Mockito.lenient().when(this.httpSession.getAttribute("user")).thenReturn(user);
        Mockito.lenient().when(this.service.putVersioningConfiguration(Mockito.any(VersioningConfigurationDTO.class)))
                .thenReturn(configuration);
        ResultActions result = putVersioningConfiguration(user);
        result.andExpect(status().isForbidden());
    }

    private ResultActions getVersioningConfiguration(UserDetails user) throws Exception {
        String accessToken = mockOAuthInterceptor(user);
        String path = "/plugins/versioning/configuration";
        return mockMvc.perform(
                get(path)
                        .header("Authorization", "Bearer " + accessToken));
    }
    private ResultActions putVersioningConfiguration(UserDetails user) throws Exception {
        String accessToken = mockOAuthInterceptor(user);
        String path = "/plugins/versioning/configuration";
        return mockMvc.perform(
                get(path)
                        .header("Authorization", "Bearer " + accessToken));
    }

    private UserDetails createUser(boolean adminAuth) throws Exception {
        UserDetails user = (adminAuth) ? (new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .withAuthorization(Group.ADMINS_GROUP_NAME, "roletest", Permission.SUPERUSER)
                .build())
                : (new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                        .withAuthorization(Group.FREE_GROUP_NAME, "roletest", Permission.MANAGE_PAGES)
                        .build());
        return user;
    }
}
