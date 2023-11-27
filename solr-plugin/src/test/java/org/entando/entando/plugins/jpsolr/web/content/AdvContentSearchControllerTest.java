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
package org.entando.entando.plugins.jpsolr.web.content;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.common.entity.model.attribute.DateAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.ITextAttribute;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.role.Permission;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.aps.util.DateConverter;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.entando.entando.plugins.jacms.aps.system.services.content.IContentService;
import org.entando.entando.plugins.jpsolr.aps.system.solr.ISolrSearchEngineManager;
import org.entando.entando.plugins.jpsolr.web.AbstractControllerIntegrationTest;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * @author E.Santoboni
 */
class AdvContentSearchControllerTest extends AbstractControllerIntegrationTest {

    @Autowired
    private IContentManager contentManager;

    @Autowired
    private ISolrSearchEngineManager searchEngineManager;

    @BeforeAll
    public static void setup() throws Exception {
        AbstractControllerIntegrationTest.setup();
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        try {
            this.searchEngineManager.refreshCmsFields();
            Thread thread = this.searchEngineManager.startReloadContentsReferences();
            thread.join();
        } catch (Exception e) {
            throw e;
        }
    }

    @Test
    void testGetContents() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        int totalPayloadSize = JsonPath.read(bodyResult, "$.payload.size()");
        Assertions.assertEquals(24, totalPayloadSize);

        result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].value", "EVN")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        int evnPayloadSize = JsonPath.read(bodyResult, "$.payload.size()");
        Assertions.assertEquals(11, evnPayloadSize);

        ResultActions facetedResult = mockMvc
                .perform(get("/plugins/advcontentsearch/facetedcontents")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String facetedBodyResult = facetedResult.andReturn().getResponse().getContentAsString();
        facetedResult.andExpect(status().isOk());
        int totalFacetedPayloadSize = JsonPath.read(facetedBodyResult, "$.payload.contentsId.size()");
        Assertions.assertEquals(totalPayloadSize, totalFacetedPayloadSize);
        int occurrencesPayloadSize = JsonPath.read(facetedBodyResult, "$.payload.occurrences.size()");
        Assertions.assertEquals(6, occurrencesPayloadSize);

        facetedResult = mockMvc
                .perform(get("/plugins/advcontentsearch/facetedcontents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].value", "EVN")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        facetedBodyResult = facetedResult.andReturn().getResponse().getContentAsString();
        facetedResult.andExpect(status().isOk());
        int evnFacetedPayloadSize = JsonPath.read(facetedBodyResult, "$.payload.contentsId.size()");
        Assertions.assertEquals(evnPayloadSize, evnFacetedPayloadSize);
        int evnOccurrencesPayloadSize = JsonPath.read(facetedBodyResult, "$.payload.occurrences.size()");
        Assertions.assertEquals(4, evnOccurrencesPayloadSize);
    }

    @Test
    void testGetContentsByGuestUser_1() throws Exception {
        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].value", "EVN"));
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        int payloadSize = JsonPath.read(bodyResult, "$.payload.size()");
        Assertions.assertEquals(9, payloadSize);

        ResultActions facetedResult = mockMvc
                .perform(get("/plugins/advcontentsearch/facetedcontents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].value", "EVN"));
        String facetedBodyResult = facetedResult.andReturn().getResponse().getContentAsString();
        facetedResult.andExpect(status().isOk());
        int facetedPayloadSize = JsonPath.read(facetedBodyResult, "$.payload.contentsId.size()");
        Assertions.assertEquals(payloadSize, facetedPayloadSize);
        int occurrencesPayloadSize = JsonPath.read(facetedBodyResult, "$.payload.occurrences.size()");
        Assertions.assertEquals(4, occurrencesPayloadSize);
    }

    @Test
    void testGetContentsByGuestUser_2() throws Exception {
        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents"));
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        System.out.println(bodyResult);
        int payloadSize = JsonPath.read(bodyResult, "$.payload.size()");
        Assertions.assertEquals(15, payloadSize);

        ResultActions evnResult = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].value", "EVN"));
        String evnBodyResult = evnResult.andReturn().getResponse().getContentAsString();
        evnResult.andExpect(status().isOk());
        int evnPayloadSize = JsonPath.read(evnBodyResult, "$.payload.size()");
        for (int i = 0; i < evnPayloadSize; i++) {
            String extractedId = JsonPath.read(evnBodyResult, "$.payload[" + i + "]");
            Assertions.assertTrue(extractedId.startsWith("EVN"));
        }

        ResultActions noEvnResult = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "not")
                        .param("filters[0].value", "EVN"));
        String noEvnBodyResult = noEvnResult.andReturn().getResponse().getContentAsString();
        noEvnResult.andExpect(status().isOk());
        int noEvnPayloadSize = JsonPath.read(noEvnBodyResult, "$.payload.size()");
        for (int i = 0; i < noEvnPayloadSize; i++) {
            String extractedId = JsonPath.read(noEvnBodyResult, "$.payload[" + i + "]");
            Assertions.assertFalse(extractedId.startsWith("EVN"));
        }
        Assertions.assertEquals((noEvnPayloadSize + evnPayloadSize), payloadSize);
    }

    @Test
    void testGetReturnsList() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .withAuthorization(Group.FREE_GROUP_NAME, "tempRole", Permission.BACKOFFICE).build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8));
        result.andExpect(status().isOk());
        result.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8));
        result.andExpect(jsonPath("$.metaData.pageSize").value("100"));
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        int payloadSize = JsonPath.read(bodyResult, "$.payload.size()");
        result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("status", IContentService.STATUS_ONLINE)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8));
        String bodyResult2 = result.andReturn().getResponse().getContentAsString();
        int payloadSize2 = JsonPath.read(bodyResult2, "$.payload.size()");
        Assertions.assertEquals(payloadSize2, payloadSize);
    }

    @Test
    void testLoadPublicEvents_1() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .withAuthorization(Group.FREE_GROUP_NAME, "tempRole", Permission.BACKOFFICE).build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                        .param("filters[0].order", FieldSearchFilter.DESC_ORDER)
                        .param("filters[1].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[1].operator", "eq")
                        .param("filters[1].value", "EVN")
                        .param("pageSize", "20")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        List<String> expectedFreeContentsId = Arrays.asList("EVN194", "EVN193",
                "EVN24", "EVN23", "EVN25", "EVN20", "EVN21", "EVN192", "EVN191");
        int payloadSize = JsonPath.read(bodyResult, "$.payload.size()");
        Assertions.assertEquals(expectedFreeContentsId.size(), payloadSize);
        for (int i = 0; i < expectedFreeContentsId.size(); i++) {
            String extractedId = JsonPath.read(bodyResult, "$.payload[" + i + "]");
            Assertions.assertTrue(expectedFreeContentsId.contains(extractedId));
        }

        user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .withAuthorization("coach", "tempRole", Permission.BACKOFFICE).build();
        accessToken = mockOAuthInterceptor(user);
        result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].value", "EVN")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        payloadSize = JsonPath.read(bodyResult, "$.payload.size()");
        List<String> newExpectedFreeContentsId = new ArrayList<>(expectedFreeContentsId);
        newExpectedFreeContentsId.add("EVN103");
        newExpectedFreeContentsId.add("EVN41");
        Assertions.assertEquals(newExpectedFreeContentsId.size(), payloadSize);
        for (int i = 0; i < payloadSize; i++) {
            String extractedId = JsonPath.read(bodyResult, "$.payload[" + i + "]");
            Assertions.assertTrue(newExpectedFreeContentsId.contains(extractedId));
        }
    }

    @Test
    void testLoadPublicEvents_1_paginated() throws Exception {
        List<String> expectedContentsId = Arrays.asList("EVN21", "EVN41",
                "EVN25", "EVN24", "EVN23", "EVN20", "EVN103", "EVN194", "EVN193", "EVN192", "EVN191");
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                        .param("filters[0].order", FieldSearchFilter.DESC_ORDER)
                        .param("filters[1].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[1].operator", "eq")
                        .param("filters[1].value", "EVN")
                        .param("page", "1")
                        .param("pageSize", "5")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk());

        result.andExpect(jsonPath("$.payload.size()", is(5)));
        for (int i = 0; i < 5; i++) {
            result.andExpect(jsonPath("$.payload.[" + i + "]", is(expectedContentsId.get(i))));
        }
        result.andExpect(jsonPath("$.metaData.page", is(1)));
        result.andExpect(jsonPath("$.metaData.pageSize", is(5)));
        result.andExpect(jsonPath("$.metaData.lastPage", is(3)));
        result.andExpect(jsonPath("$.metaData.totalItems", is(expectedContentsId.size())));

        result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                        .param("filters[0].order", FieldSearchFilter.DESC_ORDER)
                        .param("filters[1].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[1].operator", "eq")
                        .param("filters[1].value", "EVN")
                        .param("page", "2")
                        .param("pageSize", "6")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk());

        result.andExpect(jsonPath("$.payload.size()", is(5)));
        for (int i = 0; i < 5; i++) {
            result.andExpect(jsonPath("$.payload.[" + i + "]", is(expectedContentsId.get(i + 6))));
        }
        result.andExpect(jsonPath("$.metaData.page", is(2)));
        result.andExpect(jsonPath("$.metaData.pageSize", is(6)));
        result.andExpect(jsonPath("$.metaData.lastPage", is(2)));
        result.andExpect(jsonPath("$.metaData.totalItems", is(expectedContentsId.size())));
    }

    @Test
    void testLoadPublicEvents_2() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].entityAttr", "DataInizio")
                        .param("filters[0].operator", "gt")
                        .param("filters[0].type", "date")
                        .param("filters[0].value", "1997-06-10 01:00:00")
                        .param("filters[1].entityAttr", "DataInizio")
                        .param("filters[1].operator", "lt")
                        .param("filters[1].type", "date")
                        .param("filters[1].value", "2020-09-19 01:00:00")
                        .param("filters[1].order", FieldSearchFilter.DESC_ORDER)
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        String[] expected = {"EVN193", "EVN24", "EVN23", "EVN41", "EVN25",
                "EVN20", "EVN21", "EVN103", "EVN192"};
        int payloadSize = JsonPath.read(bodyResult, "$.payload.size()");
        Assertions.assertEquals(expected.length, payloadSize);
        for (int i = 0; i < expected.length; i++) {
            String expectedId = expected[i];
            String extractedId = JsonPath.read(bodyResult, "$.payload[" + i + "]");
            Assertions.assertEquals(expectedId, extractedId);
        }
    }

    @Test
    void testLoadPublicEvents_3() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .withAuthorization("coach", "tempRole", Permission.BACKOFFICE).build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                        .param("filters[0].order", FieldSearchFilter.DESC_ORDER)
                        .param("filters[1].entityAttr", "DataInizio")
                        .param("filters[1].operator", "gt")
                        .param("filters[1].type", "date")
                        .param("filters[1].value", "1997-06-10 01:00:00")
                        .param("filters[2].entityAttr", "DataInizio")
                        .param("filters[2].operator", "lt")
                        .param("filters[2].type", "date")
                        .param("filters[2].value", "2020-09-19 01:00:00")
                        .param("filters[3].entityAttr", "Titolo")
                        .param("filters[3].value", "Titolo")
                        .param("filters[3].operator", "eq")
                        .param("lang", "it")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        String[] expected = {"EVN103", "EVN193", "EVN192"};
        int payloadSize = JsonPath.read(bodyResult, "$.payload.size()");
        Assertions.assertEquals(expected.length, payloadSize);
        for (int i = 0; i < expected.length; i++) {
            String expectedId = expected[i];
            String extractedId = JsonPath.read(bodyResult, "$.payload[" + i + "]");
            Assertions.assertEquals(expectedId, extractedId);
        }
    }

    @Test
    void testLoadPublicEvents_4() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].value", "EVN")
                        .param("filters[1].entityAttr", "Titolo")
                        .param("filters[1].value", "ostr")
                        .param("filters[1].operator", "eq")
                        .param("filters[1].order", FieldSearchFilter.DESC_ORDER)
                        .param("lang", "it")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult1 = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        int payloadSize = JsonPath.read(bodyResult1, "$.payload.size()");
        Assertions.assertEquals(0, payloadSize);

        result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].value", "EVN")
                        .param("filters[1].entityAttr", "Titolo")
                        .param("filters[1].value", "Mostr*")
                        .param("filters[1].operator", "eq")
                        .param("filters[1].order", FieldSearchFilter.DESC_ORDER)
                        .param("lang", "it")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult2 = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        String[] expected2 = {"EVN20", "EVN21"};
        int payloadSize2 = JsonPath.read(bodyResult2, "$.payload.size()");
        Assertions.assertEquals(expected2.length, payloadSize2);
        for (int i = 0; i < expected2.length; i++) {
            String expectedId2 = expected2[i];
            String extractedId2 = JsonPath.read(bodyResult2, "$.payload[" + i + "]");
            Assertions.assertEquals(expectedId2, extractedId2);
        }

        result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].value", "EVN")
                        .param("filters[1].entityAttr", "Titolo")
                        .param("filters[1].value", "ostr")
                        .param("filters[1].operator", "like")
                        .param("filters[1].order", FieldSearchFilter.DESC_ORDER)
                        .param("lang", "it")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult3 = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        int payloadSize3 = JsonPath.read(bodyResult3, "$.payload.size()");
        Assertions.assertEquals(expected2.length, payloadSize3);
        for (int i = 0; i < expected2.length; i++) {
            String expectedId3 = expected2[i];
            String extractedId3 = JsonPath.read(bodyResult3, "$.payload[" + i + "]");
            Assertions.assertEquals(expectedId3, extractedId3);
        }
    }

    @Test
    void testLoadOrderedPublicEvents_1() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .withAuthorization(Group.FREE_GROUP_NAME, "tempRole", Permission.BACKOFFICE).build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].value", "EVN")
                        .param("filters[1].attribute", IContentManager.CONTENT_DESCR_FILTER_KEY)
                        .param("filters[1].order", FieldSearchFilter.ASC_ORDER)
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        String[] expectedFreeContentsId = {"EVN24", "EVN23",
                "EVN191", "EVN192", "EVN193", "EVN194", "EVN20", "EVN21", "EVN25"};
        int payloadSize = JsonPath.read(bodyResult, "$.payload.size()");
        Assertions.assertEquals(expectedFreeContentsId.length, payloadSize);
        for (int i = 0; i < expectedFreeContentsId.length; i++) {
            String expectedId = expectedFreeContentsId[i];
            String extractedId = JsonPath.read(bodyResult, "$.payload[" + i + "]");
            Assertions.assertEquals(expectedId, extractedId);
        }
    }

    @Test
    void testLoadOrderedPublicEvents_2() throws Exception {
        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].value", "EVN")
                        .param("filters[1].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                        .param("filters[1].order", FieldSearchFilter.DESC_ORDER));
        result.andExpect(status().isOk());
        String[] expectedFreeOrderedContentsId_1 = {"EVN191", "EVN192",
                "EVN193", "EVN194", "EVN20", "EVN23", "EVN24", "EVN25", "EVN21"};
        result.andExpect(jsonPath("$.payload", Matchers.hasSize(expectedFreeOrderedContentsId_1.length)));
        for (int i = 0; i < expectedFreeOrderedContentsId_1.length; i++) {
            String expectedId = expectedFreeOrderedContentsId_1[expectedFreeOrderedContentsId_1.length - i - 1];
            result.andExpect(jsonPath("$.payload[" + i + "]", is(expectedId)));
        }

        result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("langCode", "it")
                        .param("text", "Titolo Evento 4")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].value", "EVN")
                        .param("filters[1].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                        .param("filters[1].order", FieldSearchFilter.ASC_ORDER));
        result.andExpect(status().isOk());
        String[] expectedFreeOrderedContentsId_2 = {"EVN191", "EVN192", "EVN193", "EVN194", "EVN24"};
        result.andExpect(jsonPath("$.payload", Matchers.hasSize(expectedFreeOrderedContentsId_2.length)));
        for (int i = 0; i < expectedFreeOrderedContentsId_2.length; i++) {
            String expectedId = expectedFreeOrderedContentsId_2[i];
            result.andExpect(jsonPath("$.payload[" + i + "]", is(expectedId)));
        }
    }

    @Test
    void testLoadOrderedPublicEvents_3() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .withAuthorization(Group.FREE_GROUP_NAME, "tempRole", Permission.BACKOFFICE).build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].entityAttr", "DataInizio")
                        .param("filters[0].order", "DESC")
                        .param("filters[1].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[1].operator", "eq")
                        .param("filters[1].value", "EVN")
                        .param("pageSize", "5")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        String[] expectedFreeOrderedContentsId_1 = {"EVN194", "EVN193", "EVN24",
                "EVN23", "EVN25"};
        int payloadSize = JsonPath.read(bodyResult, "$.payload.size()");
        Assertions.assertEquals(expectedFreeOrderedContentsId_1.length, payloadSize);
        for (int i = 0; i < expectedFreeOrderedContentsId_1.length; i++) {
            String expectedId = expectedFreeOrderedContentsId_1[i];
            String extractedId = JsonPath.read(bodyResult, "$.payload[" + i + "]");
            Assertions.assertEquals(expectedId, extractedId);
        }

        result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].entityAttr", "DataInizio")
                        .param("filters[0].order", FieldSearchFilter.ASC_ORDER)
                        .param("filters[1].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[1].operator", "eq")
                        .param("filters[1].value", "EVN")
                        .param("pageSize", "6")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        String[] expectedFreeOrderedContentsId_2 = {"EVN191", "EVN192", "EVN21", "EVN20", "EVN25", "EVN23"};
        int payloadSize_2 = JsonPath.read(bodyResult, "$.payload.size()");
        Assertions.assertEquals(expectedFreeOrderedContentsId_2.length, payloadSize_2);
        for (int i = 0; i < expectedFreeOrderedContentsId_2.length; i++) {
            String expectedId = expectedFreeOrderedContentsId_2[i];
            String extractedId = JsonPath.read(bodyResult, "$.payload[" + i + "]");
            Assertions.assertEquals(expectedId, extractedId);
        }
    }

    @Test
    void testLoadOrderedPublicEvents_4() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .withAuthorization(Group.FREE_GROUP_NAME, "tempRole", Permission.BACKOFFICE).build();
        String accessToken = mockOAuthInterceptor(user);
        Content masterContent = this.contentManager.loadContent("EVN193", true);
        masterContent.setId(null);
        masterContent.setDescription("Cloned content for test");
        DateAttribute dateAttribute = (DateAttribute) masterContent.getAttribute("DataInizio");
        dateAttribute.setDate(DateConverter.parseDate("17/06/2019", "dd/MM/yyyy"));
        try {
            this.contentManager.saveContent(masterContent);
            this.contentManager.insertOnLineContent(masterContent);

            super.waitNotifyingThread();
            super.waitThreads(ICmsSearchEngineManager.RELOAD_THREAD_NAME_PREFIX);

            ResultActions result = mockMvc
                    .perform(get("/plugins/advcontentsearch/contents")
                            .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                            .param("filters[0].operator", "eq")
                            .param("filters[0].value", "EVN")
                            .param("filters[1].entityAttr", "DataInizio")
                            .param("filters[1].order", FieldSearchFilter.DESC_ORDER)
                            .requestAttr("user", user)
                            .header("Authorization", "Bearer " + accessToken));
            String bodyResult = result.andReturn().getResponse().getContentAsString();

            result.andExpect(status().isOk());
            String[] expectedFreeOrderedContentsId = {"EVN194", masterContent.getId(),
                    "EVN193", "EVN24", "EVN23", "EVN25", "EVN20", "EVN21", "EVN192", "EVN191"};
            int payloadSize = JsonPath.read(bodyResult, "$.payload.size()");
            Assertions.assertEquals(expectedFreeOrderedContentsId.length, payloadSize);
            for (int i = 0; i < expectedFreeOrderedContentsId.length; i++) {
                String expectedId = expectedFreeOrderedContentsId[i];
                String extractedId = JsonPath.read(bodyResult, "$.payload[" + i + "]");
                Assertions.assertEquals(expectedId, extractedId);
            }
        } finally {
            if (null != masterContent.getId() && !"EVN193".equals(masterContent.getId())) {
                this.contentManager.removeOnLineContent(masterContent);
                this.contentManager.deleteContent(masterContent);
            }
            synchronized (this) {
                this.wait(500);
            }
            super.waitNotifyingThread();
        }
    }

    @Test
    void testLoadPublicFreeARTContents() throws Exception {
        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].value", "ART"));
        result.andExpect(status().isOk());
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        List<String> expectedFreeContentsId = Arrays.asList("ART1", "ART180", "ART187", "ART121");
        result.andExpect(jsonPath("$.payload", Matchers.hasSize(expectedFreeContentsId.size())));
        for (int i = 0; i < expectedFreeContentsId.size(); i++) {
            String extractedId = JsonPath.read(bodyResult, "$.payload[" + i + "]");
            Assertions.assertTrue(expectedFreeContentsId.contains(extractedId));
        }

        ResultActions facetedResult = mockMvc
                .perform(get("/plugins/advcontentsearch/facetedcontents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].value", "ART"));
        String facetedBodyResult = facetedResult.andReturn().getResponse().getContentAsString();
        facetedResult.andExpect(status().isOk());
        int payloadSize = JsonPath.read(facetedBodyResult, "$.payload.contentsId.size()");
        Assertions.assertEquals(expectedFreeContentsId.size(), payloadSize);
        for (int i = 0; i < expectedFreeContentsId.size(); i++) {
            String extractedId = JsonPath.read(facetedBodyResult, "$.payload.contentsId[" + i + "]");
            Assertions.assertTrue(expectedFreeContentsId.contains(extractedId));
        }
        int occurrencesPayloadSize = JsonPath.read(facetedBodyResult, "$.payload.occurrences.size()");
        Assertions.assertEquals(1, occurrencesPayloadSize);
    }

    @Test
    void testLoadPublicARTContents() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].value", "ART")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk());
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        List<String> expectedContentsId = Arrays.asList("ART1", "ART180", "ART187", "ART121",
                "ART122", "ART104", "ART102", "ART111", "ART120", "ART112");
        result.andExpect(jsonPath("$.payload", Matchers.hasSize(expectedContentsId.size())));
        for (int i = 0; i < expectedContentsId.size(); i++) {
            String extractedId = JsonPath.read(bodyResult, "$.payload[" + i + "]");
            Assertions.assertTrue(expectedContentsId.contains(extractedId));
        }

        ResultActions facetedResult = mockMvc
                .perform(get("/plugins/advcontentsearch/facetedcontents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].value", "ART")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String facetedBodyResult = facetedResult.andReturn().getResponse().getContentAsString();
        facetedResult.andExpect(status().isOk());

        int payloadSize = JsonPath.read(facetedBodyResult, "$.payload.contentsId.size()");
        Assertions.assertEquals(expectedContentsId.size(), payloadSize);
        for (int i = 0; i < expectedContentsId.size(); i++) {
            String extractedId = JsonPath.read(facetedBodyResult, "$.payload.contentsId[" + i + "]");
            Assertions.assertTrue(expectedContentsId.contains(extractedId));
        }
        int occurrencesPayloadSize = JsonPath.read(facetedBodyResult, "$.payload.occurrences.size()");
        Assertions.assertEquals(5, occurrencesPayloadSize);
    }

    @Test
    void testLoadPublic_ART_EVN_Contents() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].allowedValues[0]", "EVN")
                        .param("filters[0].allowedValues[1]", "ART")
                        .param("filters[1].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                        .param("filters[1].order", FieldSearchFilter.DESC_ORDER)
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk());
        String bodyResult = result.andReturn().getResponse().getContentAsString();

        List<String> expectedContentsId = Arrays.asList("ART1", "ART180", "ART187", "ART121",
                "ART122", "ART104", "ART102", "ART111", "ART120", "ART112", "EVN25", "EVN41",
                "EVN103", "EVN193", "EVN20", "EVN194", "EVN191", "EVN21", "EVN24", "EVN23", "EVN192");
        List<String> orderedIds = new ArrayList<>();
        result.andExpect(jsonPath("$.payload", Matchers.hasSize(expectedContentsId.size())));
        for (int i = 0; i < expectedContentsId.size(); i++) {
            String extractedId = JsonPath.read(bodyResult, "$.payload[" + i + "]");
            Assertions.assertTrue(expectedContentsId.contains(extractedId));
            orderedIds.add(extractedId);
        }

        ResultActions facetedResult = mockMvc
                .perform(get("/plugins/advcontentsearch/facetedcontents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].allowedValues[0]", "ART")
                        .param("filters[0].allowedValues[1]", "EVN")
                        .param("filters[1].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                        .param("filters[1].order", FieldSearchFilter.DESC_ORDER)
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        facetedResult.andExpect(status().isOk());

        Map<String, Integer> occurrences = new HashMap<>();
        occurrences.put("general", 6);
        occurrences.put("evento", 2);
        occurrences.put("cat1", 1);
        occurrences.put("general_cat2", 3);
        occurrences.put("general_cat1", 4);
        occurrences.put("general_cat3", 2);
        facetedResult.andExpect(jsonPath("$.payload.totalSize", is(21)));
        facetedResult.andExpect(jsonPath("$.payload.contentsId.size()", is(expectedContentsId.size())));
        for (int i = 0; i < expectedContentsId.size(); i++) {
            facetedResult.andExpect(jsonPath("$.payload.contentsId[" + i + "]", is(orderedIds.get(i))));
        }
        List<String> keys = new ArrayList<>(occurrences.keySet());
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            facetedResult.andExpect(jsonPath("$.payload.occurrences." + key, is(occurrences.get(key))));
        }

        facetedResult = mockMvc
                .perform(get("/plugins/advcontentsearch/facetedcontents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].allowedValues[0]", "ART")
                        .param("filters[0].allowedValues[1]", "EVN")
                        .param("filters[1].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                        .param("filters[1].order", FieldSearchFilter.DESC_ORDER)
                        .param("page", "2")
                        .param("pageSize", "3")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        facetedResult.andExpect(jsonPath("$.payload.totalSize", is(21)));
        facetedResult.andExpect(jsonPath("$.payload.contentsId.size()", is(3)));
        for (int i = 0; i < 3; i++) {
            facetedResult.andExpect(jsonPath("$.payload.contentsId[" + i + "]", is(orderedIds.get(i + 3))));
        }
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            facetedResult.andExpect(jsonPath("$.payload.occurrences." + key, is(occurrences.get(key))));
        }
    }

    @Test
    void testLoadPublicContentsForCategory_1() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .withAuthorization(Group.FREE_GROUP_NAME, "tempRole", Permission.BACKOFFICE).build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("csvCategories[0]", "evento")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        List<String> expectedFreeContentsId = Arrays.asList("EVN192", "EVN193");
        int payloadSize = JsonPath.read(bodyResult, "$.payload.size()");
        Assertions.assertEquals(expectedFreeContentsId.size(), payloadSize);
        for (int i = 0; i < expectedFreeContentsId.size(); i++) {
            String extractedId = JsonPath.read(bodyResult, "$.payload[" + i + "]");
            Assertions.assertTrue(expectedFreeContentsId.contains(extractedId));
        }

        result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("csvCategories[0]", "evento")
                        .param("filters[0].entityAttr", "DataInizio")
                        .param("filters[0].operator", "lt")
                        .param("filters[0].type", "date")
                        .param("filters[0].value", "2005-02-13 01:00:00")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        int newPayloadSize = JsonPath.read(bodyResult, "$.payload.size()");
        Assertions.assertEquals(1, newPayloadSize);
        String extractedId = JsonPath.read(bodyResult, "$.payload[0]");
        Assertions.assertEquals("EVN192", extractedId);
    }

    @Test
    void testLoadPublicEventsForCategory_2() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("csvCategories[0]", "general_cat3")
                        .param("csvCategories[1]", "general_cat2")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        int payloadSize = JsonPath.read(bodyResult, "$.payload.size()");
        Assertions.assertEquals(1, payloadSize);
        String singleId = JsonPath.read(bodyResult, "$.payload[0]");
        Assertions.assertEquals("ART120", singleId);

        result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("csvCategories[0]", "general_cat3,general_cat2")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        bodyResult = result.andReturn().getResponse().getContentAsString();
        result.andExpect(status().isOk());
        List<String> expectedFreeContentsId = Arrays.asList("ART111", "ART120", "ART122", "EVN25");
        int newPayloadSize = JsonPath.read(bodyResult, "$.payload.size()");
        Assertions.assertEquals(expectedFreeContentsId.size(), newPayloadSize);
        for (int i = 0; i < expectedFreeContentsId.size(); i++) {
            String extractedId = JsonPath.read(bodyResult, "$.payload[" + i + "]");
            Assertions.assertTrue(expectedFreeContentsId.contains(extractedId));
        }
    }

    @Test
    void testLoadPublicFreeContentsByRole() throws Exception {
        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].entityAttr", JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE)
                        .param("filters[0].value", "Mostra")
                        .param("filters[0].operator", "eq"));
        result.andExpect(status().isOk());
        String bodyResult = result.andReturn().getResponse().getContentAsString();
        List<String> expectedFreeContentsId = Arrays.asList("EVN20", "EVN21");
        result.andExpect(jsonPath("$.payload", Matchers.hasSize(expectedFreeContentsId.size())));
        for (int i = 0; i < expectedFreeContentsId.size(); i++) {
            String extractedId = JsonPath.read(bodyResult, "$.payload[" + i + "]");
            Assertions.assertTrue(expectedFreeContentsId.contains(extractedId));
        }

        ResultActions facetedResult = mockMvc
                .perform(get("/plugins/advcontentsearch/facetedcontents")
                        .param("filters[0].entityAttr", JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE)
                        .param("filters[0].value", "Mostra"));
        String facetedBodyResult = facetedResult.andReturn().getResponse().getContentAsString();
        facetedResult.andExpect(status().isOk());
        int payloadSize = JsonPath.read(facetedBodyResult, "$.payload.contentsId.size()");
        Assertions.assertEquals(expectedFreeContentsId.size(), payloadSize);
        for (int i = 0; i < expectedFreeContentsId.size(); i++) {
            String extractedId = JsonPath.read(facetedBodyResult, "$.payload.contentsId[" + i + "]");
            Assertions.assertTrue(expectedFreeContentsId.contains(extractedId));
        }
    }

    @Test
    void testLoadPublicFreeOrderedContentsByRole() throws Exception {
        List<String> expectedFreeContentsId = Arrays.asList("EVN24", "EVN23", "ART1", "EVN21", "EVN20", "EVN41",
                "EVN25",
                "EVN191", "EVN192", "EVN193", "EVN103", "ART104", "ART102", "ART111", "ART112", "ART120", "ART121",
                "ART122", "EVN194");
        // NOTA : "ART180" e "ART187" non hanno titolo
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].entityAttr", JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE)
                        .param("filters[0].order", FieldSearchFilter.ASC_ORDER)
                        .param("filters[1].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[1].operator", "eq")
                        .param("filters[1].allowedValues[0]", "ART")
                        .param("filters[1].allowedValues[1]", "EVN")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk());
        String bodyResult = result.andReturn().getResponse().getContentAsString();

        result.andExpect(jsonPath("$.payload", Matchers.hasSize(expectedFreeContentsId.size())));
        for (int i = 0; i < expectedFreeContentsId.size(); i++) {
            String extractedId = JsonPath.read(bodyResult, "$.payload[" + i + "]");
            Assertions.assertEquals(extractedId, expectedFreeContentsId.get(i));
        }

        result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].entityAttr", JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE)
                        .param("filters[0].order", FieldSearchFilter.DESC_ORDER)
                        .param("filters[1].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[1].operator", "eq")
                        .param("filters[1].allowedValues[0]", "ART")
                        .param("filters[1].allowedValues[1]", "EVN")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        bodyResult = result.andReturn().getResponse().getContentAsString();
        for (int i = 0; i < expectedFreeContentsId.size(); i++) {
            String extractedId = JsonPath.read(bodyResult, "$.payload[" + i + "]");
            Assertions.assertEquals(extractedId, expectedFreeContentsId.get(expectedFreeContentsId.size() - i - 1));
        }
    }

    @Test
    void testLoadPublicFreeOrderedContentsByRole_paginated() throws Exception {
        List<String> expectedFreeContentsId = Arrays.asList("EVN24", "EVN23", "ART1", "EVN21", "EVN20", "EVN41",
                "EVN25",
                "EVN191", "EVN192", "EVN193", "EVN103", "ART104", "ART102", "ART111", "ART112", "ART120", "ART121",
                "ART122", "EVN194");

        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].entityAttr", JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE)
                        .param("filters[0].order", FieldSearchFilter.ASC_ORDER)
                        .param("filters[1].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[1].operator", "eq")
                        .param("filters[1].allowedValues[0]", "ART")
                        .param("filters[1].allowedValues[1]", "EVN")
                        .param("page", "1")
                        .param("pageSize", "5")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk());

        result.andExpect(jsonPath("$.payload.size()", is(5)));
        for (int i = 0; i < 5; i++) {
            result.andExpect(jsonPath("$.payload.[" + i + "]", is(expectedFreeContentsId.get(i))));
        }
        result.andExpect(jsonPath("$.metaData.page", is(1)));
        result.andExpect(jsonPath("$.metaData.pageSize", is(5)));
        result.andExpect(jsonPath("$.metaData.lastPage", is(4)));
        result.andExpect(jsonPath("$.metaData.totalItems", is(expectedFreeContentsId.size())));

        result = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].entityAttr", JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE)
                        .param("filters[0].order", FieldSearchFilter.ASC_ORDER)
                        .param("filters[1].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[1].operator", "eq")
                        .param("filters[1].allowedValues[0]", "ART")
                        .param("filters[1].allowedValues[1]", "EVN")
                        .param("page", "2")
                        .param("pageSize", "6")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.payload.size()", is(6)));
        for (int i = 0; i < 5; i++) {
            result.andExpect(jsonPath("$.payload.[" + i + "]", is(expectedFreeContentsId.get(i + 6))));
        }
        result.andExpect(jsonPath("$.metaData.page", is(2)));
        result.andExpect(jsonPath("$.metaData.pageSize", is(6)));
        result.andExpect(jsonPath("$.metaData.lastPage", is(4)));
        result.andExpect(jsonPath("$.metaData.totalItems", is(expectedFreeContentsId.size())));
    }

    @Test
    void testLoadPublic_ART_EVN_Contents_orderedByRole() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        ResultActions result_1 = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].allowedValues[0]", "EVN")
                        .param("filters[0].allowedValues[1]", "ART")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        result_1.andExpect(status().isOk());
        String bodyResult_1 = result_1.andReturn().getResponse().getContentAsString();
        List<String> expectedContentsId = Arrays.asList("ART1", "ART180", "ART187", "ART121",
                "ART122", "ART104", "ART102", "ART111", "ART120", "ART112",
                "EVN25", "EVN41", "EVN103", "EVN193", "EVN20",
                "EVN194", "EVN191", "EVN21", "EVN24", "EVN23", "EVN192");
        int firstPayloadSize = JsonPath.read(bodyResult_1, "$.payload.size()");
        Assertions.assertEquals(expectedContentsId.size(), firstPayloadSize);
        for (int i = 0; i < expectedContentsId.size(); i++) {
            String extractedId = JsonPath.read(bodyResult_1, "$.payload[" + i + "]");
            Assertions.assertTrue(expectedContentsId.contains(extractedId));
        }

        ResultActions result_2 = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].allowedValues[0]", "ART")
                        .param("filters[0].allowedValues[1]", "EVN")
                        .param("filters[1].entityAttr", "jacms:title")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult_2 = result_2.andReturn().getResponse().getContentAsString();
        List<String> expectedContentsId_2 = Arrays.asList("ART1", "ART121",
                "ART122", "ART104", "ART102", "ART111", "ART120", "ART112",
                "EVN25", "EVN41", "EVN103", "EVN193", "EVN20",
                "EVN194", "EVN191", "EVN21", "EVN24", "EVN23", "EVN192");
        result_2.andExpect(status().isOk());
        int payloadSize = JsonPath.read(bodyResult_2, "$.payload.size()");
        Assertions.assertEquals(expectedContentsId_2.size(), payloadSize);
        for (int i = 0; i < expectedContentsId_2.size(); i++) {
            String extractedId = JsonPath.read(bodyResult_2, "$.payload[" + i + "]");
            Assertions.assertTrue(expectedContentsId_2.contains(extractedId));
        }

        ResultActions result_3 = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].allowedValues[0]", "ART")
                        .param("filters[0].allowedValues[1]", "EVN")
                        .param("filters[1].entityAttr", "jacms:title")
                        .param("filters[1].order", "DESC")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult_3 = result_3.andReturn().getResponse().getContentAsString();
        String[] expectedContentsId_3 = {"EVN194", "ART122", "ART121", "ART120",
                "ART112", "ART111", "ART102", "ART104", "EVN103", "EVN193", "EVN192", "EVN191", "EVN25",
                "EVN41", "EVN20", "EVN21", "ART1", "EVN23", "EVN24"};
        result_3.andExpect(status().isOk());
        int payloadSize_3 = JsonPath.read(bodyResult_3, "$.payload.size()");
        Assertions.assertEquals(expectedContentsId_3.length, payloadSize_3);
        for (int i = 0; i < expectedContentsId_3.length; i++) {
            String extractedId = JsonPath.read(bodyResult_3, "$.payload[" + i + "]");
            Assertions.assertEquals(extractedId, expectedContentsId_3[i]);
        }

        ResultActions result_4 = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].allowedValues[0]", "ART")
                        .param("filters[0].allowedValues[1]", "EVN")
                        .param("filters[1].entityAttr", "jacms:title")
                        .param("filters[1].order", "ASC")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult_4 = result_4.andReturn().getResponse().getContentAsString();
        for (int i = 0; i < expectedContentsId_3.length; i++) {
            String extractedId = JsonPath.read(bodyResult_4, "$.payload[" + i + "]");
            Assertions.assertEquals(extractedId, expectedContentsId_3[expectedContentsId_3.length - i - 1]);
        }

        ResultActions result_5_en = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("lang", "en")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].allowedValues[0]", "ART")
                        .param("filters[0].allowedValues[1]", "EVN")
                        .param("filters[1].entityAttr", "jacms:title")
                        .param("filters[1].order", "ASC")
                        .requestAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult_5_en = result_5_en.andReturn().getResponse().getContentAsString();
        String[] expectedContentsId_5_en = {"EVN41", "EVN24", "EVN103", "EVN23", "EVN21",
                "ART1", "EVN194", "EVN192", "EVN191", "EVN193", "ART120", "ART121",
                "ART104", "ART102", "ART111", "ART112", "ART122", "EVN25", "EVN20"};
        for (int i = 0; i < expectedContentsId_5_en.length; i++) {
            String extractedId = JsonPath.read(bodyResult_5_en, "$.payload[" + i + "]");
            Content content = this.contentManager.loadContent(extractedId, true);
            ITextAttribute textAttribute = (ITextAttribute) content.getAttributeByRole("jacms:title");
            String title = textAttribute.getTextForLang("en");
            if (null == title) {
                title = textAttribute.getTextForLang("it");
            }
            Assertions.assertEquals(extractedId, expectedContentsId_5_en[i]);
        }
    }

    @Test
    @Disabled("Issue into Tests in PR")
    void testSearchByOption() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        ResultActions result_1 = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("text", "ciliegia").param("lang", "it")
                        .param("filters[0].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                        .param("filters[0].order", FieldSearchFilter.ASC_ORDER)
                        .requestAttr("user", user).header("Authorization", "Bearer " + accessToken));
        result_1.andExpect(status().isOk());
        String bodyResult_1 = result_1.andReturn().getResponse().getContentAsString();
        List<String> expectedContentsId = Arrays.asList("EVN41");
        int firstPayloadSize = JsonPath.read(bodyResult_1, "$.payload.size()");
        Assertions.assertEquals(expectedContentsId.size(), firstPayloadSize);
        for (int i = 0; i < expectedContentsId.size(); i++) {
            String extractedId = JsonPath.read(bodyResult_1, "$.payload[" + i + "]");
            Assertions.assertEquals(expectedContentsId.get(i), extractedId);
        }
        String content1Id = null;
        String content2Id = null;
        try {
            Content content1 = this.contentManager.loadContent("EVN41", true);
            content1.setId(null);
            content1.setDescription("Mostra della ciliegia");
            ITextAttribute title1 = (ITextAttribute) content1.getAttribute("Titolo");
            title1.setText("La mostra della ciliegia", "it");
            title1.setText("The cherry festival", "en");
            ITextAttribute textBody1 = (ITextAttribute) content1.getAttribute("CorpoTesto");
            textBody1.setText("Rinomata la mostra della ciliegia che si tiene ogni anno in un paese florido", "it");
            textBody1.setText("The cherry festival held every year in a thriving country is renowned", "en");
            this.contentManager.insertOnLineContent(content1);
            content1Id = content1.getId();

            Content content2 = this.contentManager.loadContent("EVN41", true);
            content2.setId(null);
            content2.setDescription("Sagra della ciliegia");
            ITextAttribute title2 = (ITextAttribute) content2.getAttribute("Titolo");
            title2.setText("La forma della ciliegia", "it");
            title2.setText("The shape of the cherry", "en");
            ITextAttribute textBody2 = (ITextAttribute) content2.getAttribute("CorpoTesto");
            textBody2.setText(
                    "La ciliegia, normalmente sferica, di 0,7-2 centimetri di diametro, può assumere anche la forma a cuore o di sfera leggermente allungata",
                    "it");
            textBody2.setText(
                    "The cherry, normally spherical, 0.7-2 centimeters in diameter, can also take the shape of a heart or a slightly elongated sphere",
                    "en");
            this.contentManager.insertOnLineContent(content2);
            content2Id = content2.getId();
            synchronized (this) {
                this.wait(1000);
            }
            waitNotifyingThread();
            // "exact", "all", "one", "any"

            List<String> orders = Arrays.asList(FieldSearchFilter.ASC_ORDER, FieldSearchFilter.DESC_ORDER);
            for (int k = 0; k < orders.size(); k++) {
                String order = orders.get(k);
                ResultActions result_2 = mockMvc
                        .perform(get("/plugins/advcontentsearch/contents")
                                .param("text", "mostra ciliegia").param("lang", "it").param("searchOption", "one")
                                .param("filters[0].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                                .param("filters[0].order", order)
                                .requestAttr("user", user).header("Authorization", "Bearer " + accessToken));
                result_2.andExpect(status().isOk());
                String bodyResult_2 = result_2.andReturn().getResponse().getContentAsString();
                List<String> expectedContentsId2 = Arrays.asList("EVN20", "EVN41", "EVN21", content1Id, content2Id);
                int payloadSize2 = JsonPath.read(bodyResult_2, "$.payload.size()");
                Assertions.assertEquals(expectedContentsId2.size(), payloadSize2);
                for (int i = 0; i < expectedContentsId2.size(); i++) {
                    String extractedId = JsonPath.read(bodyResult_2, "$.payload[" + i + "]");
                    if (order.equals(FieldSearchFilter.ASC_ORDER)) {
                        Assertions.assertEquals(expectedContentsId2.get(i), extractedId);
                    } else {
                        Assertions.assertEquals(expectedContentsId2.get(payloadSize2 - i - 1), extractedId);
                    }
                }
            }

            ResultActions result_3 = mockMvc
                    .perform(get("/plugins/advcontentsearch/contents")
                            .param("text", "mostra ciliegia").param("lang", "it").param("searchOption", "all")
                            .requestAttr("user", user).header("Authorization", "Bearer " + accessToken));
            result_3.andExpect(status().isOk());
            String bodyResult_3 = result_3.andReturn().getResponse().getContentAsString();
            int payloadSize3 = JsonPath.read(bodyResult_3, "$.payload.size()");
            Assertions.assertEquals(1, payloadSize3);
            String extractedId = JsonPath.read(bodyResult_3, "$.payload[0]");
            Assertions.assertEquals(content1Id, extractedId);

            ResultActions result_4 = mockMvc
                    .perform(get("/plugins/advcontentsearch/contents")
                            .param("text", "mostra ciliegia").param("lang", "it").param("searchOption", "exact")
                            .requestAttr("user", user).header("Authorization", "Bearer " + accessToken));
            result_4.andExpect(status().isOk());
            String bodyResult_4 = result_4.andReturn().getResponse().getContentAsString();
            int payloadSize4 = JsonPath.read(bodyResult_4, "$.payload.size()");
            Assertions.assertEquals(0, payloadSize4);

            ResultActions result_5 = mockMvc
                    .perform(get("/plugins/advcontentsearch/contents")
                            .param("text", "mostra della ciliegia").param("lang", "it").param("searchOption", "exact")
                            .requestAttr("user", user).header("Authorization", "Bearer " + accessToken));
            result_5.andExpect(status().isOk());
            String bodyResult_5 = result_5.andReturn().getResponse().getContentAsString();
            int payloadSize5 = JsonPath.read(bodyResult_5, "$.payload.size()");
            Assertions.assertEquals(1, payloadSize5);
            String extractedId_5 = JsonPath.read(bodyResult_5, "$.payload[0]");
            Assertions.assertEquals(content1Id, extractedId_5);

        } catch (Exception e) {
            throw e;
        } finally {
            Content content1 = this.contentManager.loadContent(content1Id, false);
            if (null != content1) {
                this.contentManager.removeOnLineContent(content1);
                this.contentManager.deleteContent(content1Id);
            }
            Content content2 = this.contentManager.loadContent(content2Id, false);
            if (null != content2) {
                this.contentManager.removeOnLineContent(content2);
                this.contentManager.deleteContent(content2Id);
            }
        }
        synchronized (this) {
            this.wait(1000);
        }
        super.waitNotifyingThread();
    }

}
