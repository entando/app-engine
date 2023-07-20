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
package org.entando.entando.web.component;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.services.user.UserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.entando.entando.web.AbstractControllerIntegrationTest;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class ComponentControllerIntegrationTest extends AbstractControllerIntegrationTest {
    
    private ObjectMapper mapper = new ObjectMapper();
    
    @Test
    void validateRequestWithMissingFieldsOnUsageApi() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        List<Map<String, String>> request = List.of(
                Map.of("type", "category", "test", "wrong"), 
                Map.of("types", "pageModel", "code", "service"));
        String payload = mapper.writeValueAsString(request);
        ResultActions result = mockMvc.perform(
                post("/components/usageDetails")
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.payload", Matchers.hasSize(0)))
                .andExpect(jsonPath("$.metaData", Matchers.hasSize(0)))
                .andExpect(jsonPath("$.errors", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.errors[0].code", is("1")))
                .andExpect(jsonPath("$.errors[1].code", is("1")));
    }
    
    @Test
    void validateRequestWithMissingFieldsOnDeleteApi() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        List<Map<String, String>> request = List.of(
                Map.of("type", "category", "test", "wrong"), 
                Map.of("types", "pageModel", "code", "service"));
        String payload = mapper.writeValueAsString(request);
        ResultActions result = mockMvc.perform(
                delete("/components/allInternals")
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.payload", Matchers.hasSize(0)))
                .andExpect(jsonPath("$.metaData", Matchers.hasSize(0)))
                .andExpect(jsonPath("$.errors", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.errors[0].code", is("1")))
                .andExpect(jsonPath("$.errors[1].code", is("1")));
    }
    
    @Test
    void validateRequestWithWrongType() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        List<Map<String, String>> request = List.of(
                Map.of("type", "category", "code", "general"), 
                Map.of("type", "pageModel", "codes", "service"), 
                Map.of("type", "wrongType", "code", "service"), 
                Map.of("type", "page", "code", "pagina_1"));
        String payload = mapper.writeValueAsString(request);
        ResultActions result = mockMvc.perform(
                post("/components/usageDetails")
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.payload", Matchers.hasSize(0)))
                .andExpect(jsonPath("$.metaData", Matchers.hasSize(0)))
                .andExpect(jsonPath("$.errors", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.errors[0].code", is("1")))
                .andExpect(jsonPath("$.errors[1].code", is("2")));
    }
    
    @Test
    void extractUsageDetailsForInexistentElement() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        List<Map<String, String>> request = List.of(
                Map.of("type", "pageModel", "code", "non-existent"));
        String payload = mapper.writeValueAsString(request);
        ResultActions result = mockMvc.perform(
                post("/components/usageDetails")
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.size()", is(3)));
        result.andExpect(jsonPath("$.metaData.size()", is(0)));
        result.andExpect(jsonPath("$.errors.size()", is(0)));
        result.andExpect(jsonPath("$.payload.size()", is(1)));
        result.andExpect(jsonPath("$.payload[0].type", is("pageModel")));
        result.andExpect(jsonPath("$.payload[0].code", is("non-existent")));
        result.andExpect(jsonPath("$.payload[0].exist", is(false)));
        result.andExpect(jsonPath("$.payload[0].usage").doesNotExist());
        result.andExpect(jsonPath("$.payload[0].references.size()", is(0)));
    }
    
    @Test
    void extractPageModelUsageDetails() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        List<Map<String, String>> request = List.of(
                Map.of("type", "pageModel", "code", "service"));
        String payload = mapper.writeValueAsString(request);
        ResultActions result = mockMvc.perform(
                post("/components/usageDetails")
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        
        result.andExpect(jsonPath("$.size()", is(3)));
        result.andExpect(jsonPath("$.metaData.size()", is(0)));
        result.andExpect(jsonPath("$.errors.size()", is(0)));
        result.andExpect(jsonPath("$.payload.size()", is(1)));
        result.andExpect(jsonPath("$.payload[0].type", is("pageModel")));
        result.andExpect(jsonPath("$.payload[0].code", is("service")));
        result.andExpect(jsonPath("$.payload[0].exist", is(true)));
        result.andExpect(jsonPath("$.payload[0].usage", is(10)));
        result.andExpect(jsonPath("$.payload[0].references.size()", is(10)));
        int onlinePages = 0;
        List<String> pages = List.of("errorpage", "login", "notfound", "primapagina", "service");
        for (int i = 0; i < 10; i++) {
            result.andExpect(jsonPath("$.payload[0].references[" + i + "].size()", is(3)));
            result.andExpect(jsonPath("$.payload[0].references[" + i + "].type", is("page")));
            String pageCode = JsonPath.read(bodyResult, "$.payload[0].references[" + i + "].code");
            Assertions.assertTrue(pages.contains(pageCode));
            if (JsonPath.read(bodyResult, "$.payload[0].references[" + i + "].online")) {
                onlinePages++;
            }
        }
        Assertions.assertEquals(5, onlinePages);
    }
    
    @Test
    void extractCategoryUsageDetails() throws Exception {
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
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.size()", is(3)));
        result.andExpect(jsonPath("$.metaData.size()", is(0)));
        result.andExpect(jsonPath("$.errors.size()", is(0)));
        result.andExpect(jsonPath("$.payload.size()", is(1)));
        result.andExpect(jsonPath("$.payload[0].type", is("category")));
        result.andExpect(jsonPath("$.payload[0].code", is("general")));
        result.andExpect(jsonPath("$.payload[0].exist", is(true)));
        result.andExpect(jsonPath("$.payload[0].usage", is(3)));
        result.andExpect(jsonPath("$.payload[0].references.size()", is(3)));
        List<String> categories = List.of("general_cat1", "general_cat2", "general_cat3");
        for (int i = 0; i < 3; i++) {
            result.andExpect(jsonPath("$.payload[0].references[" + i + "].size()", is(2)));
            result.andExpect(jsonPath("$.payload[0].references[" + i + "].type", is("category")));
            String categoryCode = JsonPath.read(bodyResult, "$.payload[0].references[" + i + "].code");
            Assertions.assertTrue(categories.contains(categoryCode));
        }
    }
    
    @Test
    void extractPageUsageDetails() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        List<Map<String, String>> request = List.of(
                Map.of("type", "page", "code", "pagina_1"));
        String payload = mapper.writeValueAsString(request);
        ResultActions result = mockMvc.perform(
                post("/components/usageDetails")
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.size()", is(3)));
        result.andExpect(jsonPath("$.metaData.size()", is(0)));
        result.andExpect(jsonPath("$.errors.size()", is(0)));
        result.andExpect(jsonPath("$.payload.size()", is(1)));
        result.andExpect(jsonPath("$.payload[0].type", is("page")));
        result.andExpect(jsonPath("$.payload[0].code", is("pagina_1")));
        result.andExpect(jsonPath("$.payload[0].exist", is(true)));
        result.andExpect(jsonPath("$.payload[0].usage", is(5)));
        result.andExpect(jsonPath("$.payload[0].online", is(false)));
        result.andExpect(jsonPath("$.payload[0].references.size()", is(5)));
        List<String> categories = List.of("pagina_1", "pagina_11", "pagina_12");
        int online = 0;
        for (int i = 0; i < 5; i++) {
            result.andExpect(jsonPath("$.payload[0].references[" + i + "].size()", is(3)));
            result.andExpect(jsonPath("$.payload[0].references[" + i + "].type", is("page")));
            String pageCode = JsonPath.read(bodyResult, "$.payload[0].references[" + i + "].code");
            Assertions.assertTrue(categories.contains(pageCode));
            boolean isOnline = JsonPath.read(bodyResult, "$.payload[0].references[" + i + "].online");
            if (isOnline) {
                online++;
            }
        }
        Assertions.assertEquals(3, online);
    }
    
    @Test
    void extractGroupUsageDetails() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        List<Map<String, String>> request = List.of(
                Map.of("type", "group", "code", "customers"));
        String payload = mapper.writeValueAsString(request);
        ResultActions result = mockMvc.perform(
                post("/components/usageDetails")
                        .content(payload)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.size()", is(3)));
        result.andExpect(jsonPath("$.metaData.size()", is(0)));
        result.andExpect(jsonPath("$.errors.size()", is(0)));
        result.andExpect(jsonPath("$.payload.size()", is(1)));
        result.andExpect(jsonPath("$.payload[0].type", is("group")));
        result.andExpect(jsonPath("$.payload[0].code", is("customers")));
        result.andExpect(jsonPath("$.payload[0].exist", is(true)));
        result.andExpect(jsonPath("$.payload[0].online").doesNotExist());
        int size = JsonPath.read(bodyResult, "$.payload[0].usage");
        Assertions.assertEquals(9, size);
        result.andExpect(jsonPath("$.payload[0].references.size()", is(size)));
        List<String> users = List.of("pageManagerCustomers", "supervisorCustomers", "editorCustomers", 
                "supervisorCoach", "editorCoach", "pageManagerCoach");
        List<String> pages = List.of("customers_page", "customer_subpage_1", "customer_subpage_2");
        for (int i = 0; i < size; i++) {
            String type = JsonPath.read(bodyResult, "$.payload[0].references[" + i + "].type");
            String code = JsonPath.read(bodyResult, "$.payload[0].references[" + i + "].code");
            if (type.equals("user")) {
                result.andExpect(jsonPath("$.payload[0].references[" + i + "].size()", is(2)));
                Assertions.assertTrue(users.contains(code));
                result.andExpect(jsonPath("$.payload[0].references[" + i + "].online").doesNotExist());
            } else {
                result.andExpect(jsonPath("$.payload[0].references[" + i + "].size()", is(3)));
                Assertions.assertEquals("page", type);
                Assertions.assertTrue(pages.contains(code));
                result.andExpect(jsonPath("$.payload[0].references[" + i + "].online").exists());
            }
        }
    }
    
}
