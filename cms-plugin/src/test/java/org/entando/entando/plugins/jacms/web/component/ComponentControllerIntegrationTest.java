/*
 * Copyright 2023-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jacms.web.component;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.services.user.UserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.entando.entando.web.AbstractControllerIntegrationTest;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

public class ComponentControllerIntegrationTest extends AbstractControllerIntegrationTest {
    
    private ObjectMapper mapper = new ObjectMapper();
    
    @Test
    void extractCategoryUsageDetailsShouldReturnContents() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        List<Map<String, String>> request = List.of(
                Map.of("type", "category", "code", "general"));
        String payload = mapper.writeValueAsString(request);
        ResultActions result = mockMvc.perform(
                post("/components/usageDetails")
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        System.out.println(result.andReturn().getResponse().getContentAsString());
        result.andExpect(status().isOk());
    }
    
    @Test
    void extractCategoryUsageDetailsShouldReturnResources() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        List<Map<String, String>> request = List.of(
                Map.of("type", "category", "code", "resource_root"));
        String payload = mapper.writeValueAsString(request);
        ResultActions result = mockMvc.perform(
                post("/components/usageDetails")
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        System.out.println(result.andReturn().getResponse().getContentAsString());
        result.andExpect(status().isOk());
    }
    
    
    @Test
    void extractPageUsageDetails_2() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        List<Map<String, String>> request = List.of(
                Map.of("type", "page", "code", "pagina_11"));
        String payload = mapper.writeValueAsString(request);
        ResultActions result = mockMvc.perform(
                post("/components/usageDetails")
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        System.out.println(result.andReturn().getResponse().getContentAsString());
        result.andExpect(status().isOk());
    }
    
}
