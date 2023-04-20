/*
 * Copyright 2021-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpsolr.web.config;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.services.user.UserDetails;
import java.util.List;
import org.entando.entando.plugins.jpsolr.aps.system.solr.SolrSearchEngineManager;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.ContentTypeSettings;
import org.entando.entando.web.AbstractControllerTest;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @author E.Santoboni
 */
@ExtendWith(MockitoExtension.class)
class SolrConfigControllerTest extends AbstractControllerTest {

    @Mock
    SolrSearchEngineManager searchEngineManager;

    @InjectMocks
    private SolrConfigController controller;

    @BeforeEach
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(entandoOauth2Interceptor)
                .setMessageConverters(getMessageConverters())
                .setHandlerExceptionResolvers(createHandlerExceptionResolver())
                .build();
    }

    @Test
    void shouldGetConfig() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        ContentTypeSettings settings = new ContentTypeSettings("NWS", "News");
        Mockito.when(searchEngineManager.getContentTypesSettings()).thenReturn(List.of(settings));

        ResultActions result = mockMvc.perform(get("/plugins/solr/config")
                .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.payload[0].typeCode", is("NWS")))
                .andExpect(jsonPath("$.payload[0].typeDescription", is("News")))
                .andExpect(jsonPath("$.payload[0].valid", is(true)));
    }

    @Test
    void getConfigShouldReturnForbiddenToNonAdminUser() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("simple_user", "0x24").build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc.perform(get("/plugins/solr/config")
                .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isForbidden());
    }

    @Test
    void shouldReloadReferences() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        mockMvc.perform(post("/plugins/solr/config/NWS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.contentTypeCode", is("NWS")))
                .andExpect(jsonPath("$.payload.status", is("success")));

        Mockito.verify(searchEngineManager, Mockito.times(1)).refreshContentType("NWS");
    }

    @Test
    void postConfigShouldReturnForbiddenToNonAdminUser() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("simple_user", "0x24").build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc.perform(post("/plugins/solr/config/NWS")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isForbidden());
    }
}
