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

import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.common.entity.model.attribute.DateAttribute;
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
import java.util.List;
import org.entando.entando.plugins.jacms.aps.system.services.content.IContentService;
import org.entando.entando.web.utils.OAuth2TestUtils;

import static org.hamcrest.CoreMatchers.is;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.common.entity.IEntityTypesConfigurer;
import com.agiletec.aps.system.common.entity.model.attribute.ITextAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.TextAttribute;
import com.agiletec.aps.system.common.searchengine.IndexableAttributeInterface;
import java.util.Calendar;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.entando.entando.plugins.jpsolr.SolrTestUtils;
import org.entando.entando.plugins.jpsolr.aps.system.solr.ISolrSearchEngineManager;
import org.entando.entando.plugins.jpsolr.web.AbstractControllerIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author E.Santoboni
 */
public class SearchByDoubleFiltersControllerTest extends AbstractControllerIntegrationTest {

    private static final String TEXT_FOR_TEST = "Entando is the leading modular application platform for building enterprise applications on Kubernetes";

    @Autowired
    private IContentManager contentManager;

    @Autowired
    private ICmsSearchEngineManager searchEngineManager;

    @BeforeAll
    public static void setup() throws Exception {
        SolrTestUtils.startContainer();
        AbstractControllerIntegrationTest.setup();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        SolrTestUtils.stopContainer();
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        try {
            ((ISolrSearchEngineManager) this.searchEngineManager).refreshCmsFields();
            Thread thread = this.searchEngineManager.startReloadContentsReferences();
            thread.join();
        } catch (Exception e) {
            throw e;
        }
    }

    @Test
    public void testGetFacetedContentsByTypes() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions facetedResult = mockMvc
                .perform(get("/plugins/advcontentsearch/facetedcontents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].value", "EVN")
                        .param("filters[1].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[1].operator", "eq")
                        .param("filters[1].value", "ART")
                        .sessionAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String bodyResult = facetedResult.andReturn().getResponse().getContentAsString();
        facetedResult.andExpect(status().isOk());
        int payloadSize = JsonPath.read(bodyResult, "$.payload.contentsId.size()");
        Assertions.assertEquals(0, payloadSize);

        List<String> expectedContentsId = Arrays.asList("ART1", "ART180", "ART187", "ART121",
                "ART122", "ART104", "ART102", "ART111", "ART120", "ART112",
                "EVN25", "EVN41", "EVN103", "EVN193", "EVN20",
                "EVN194", "EVN191", "EVN21", "EVN24", "EVN23", "EVN192");

        facetedResult = mockMvc
                .perform(get("/plugins/advcontentsearch/facetedcontents")
                        .param("filters[0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("filters[0].operator", "eq")
                        .param("filters[0].allowedValues[0]", "ART")
                        .param("filters[0].allowedValues[1]", "EVN")
                        .sessionAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        String facetedBodyResult = facetedResult.andReturn().getResponse().getContentAsString();
        facetedResult.andExpect(status().isOk());
        payloadSize = JsonPath.read(facetedBodyResult, "$.payload.contentsId.size()");
        Assertions.assertEquals(expectedContentsId.size(), payloadSize);
        for (int i = 0; i < expectedContentsId.size(); i++) {
            String extractedId = JsonPath.read(facetedBodyResult, "$.payload.contentsId[" + i + "]");
            Assertions.assertTrue(expectedContentsId.contains(extractedId));
        }

        facetedResult = mockMvc
                .perform(get("/plugins/advcontentsearch/facetedcontents")
                        .param("doubleFilters[0][0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("doubleFilters[0][0].operator", "eq")
                        .param("doubleFilters[0][0].value", "EVN")
                        .param("doubleFilters[0][1].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                        .param("doubleFilters[0][1].operator", "eq")
                        .param("doubleFilters[0][1].value", "ART")
                        .sessionAttr("user", user)
                        .header("Authorization", "Bearer " + accessToken));
        facetedBodyResult = facetedResult.andReturn().getResponse().getContentAsString();
        facetedResult.andExpect(status().isOk());
        payloadSize = JsonPath.read(facetedBodyResult, "$.payload.contentsId.size()");
        Assertions.assertEquals(expectedContentsId.size(), payloadSize);
        for (int i = 0; i < expectedContentsId.size(); i++) {
            String extractedId = JsonPath.read(facetedBodyResult, "$.payload.contentsId[" + i + "]");
            Assertions.assertTrue(expectedContentsId.contains(extractedId));
        }
    }

    @Test
    public void testGetFacetedContentsByTitle() throws Exception {
        List<String> ids = null;
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        try {
            ids = this.addContentsForTestByTitle(TEXT_FOR_TEST);
            ResultActions facetedResult = mockMvc
                    .perform(get("/plugins/advcontentsearch/facetedcontents")
                            .param("filters[0].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                            .param("filters[0].order", FieldSearchFilter.DESC_ORDER)
                            .param("doubleFilters[0][0].entityAttr", JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE)
                            .param("doubleFilters[0][0].operator", "like")
                            .param("doubleFilters[0][0].value", "enterprise")
                            .param("doubleFilters[0][1].entityAttr", JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE)
                            .param("doubleFilters[0][1].operator", "like")
                            .param("doubleFilters[0][1].value", "Entando")
                            .sessionAttr("user", user)
                            .header("Authorization", "Bearer " + accessToken));
            String bodyResult = facetedResult.andReturn().getResponse().getContentAsString();
            facetedResult.andExpect(status().isOk());
            int payloadSize = JsonPath.read(bodyResult, "$.payload.contentsId.size()");
            Assertions.assertEquals(ids.size(), payloadSize);
            for (int i = 0; i < ids.size(); i++) {
                String expectedId = ids.get(ids.size() - i - 1);
                facetedResult.andExpect(jsonPath("$.payload.contentsId[" + i + "]", is(expectedId)));
            }

            facetedResult = mockMvc
                    .perform(get("/plugins/advcontentsearch/facetedcontents")
                            .param("filters[0].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                            .param("filters[0].order", FieldSearchFilter.DESC_ORDER)
                            .param("doubleFilters[0][0].entityAttr", JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE)
                            .param("doubleFilters[0][0].operator", "like")
                            .param("doubleFilters[0][0].value", "enterprise")
                            .param("doubleFilters[0][1].entityAttr", JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE)
                            .param("doubleFilters[0][1].operator", "like")
                            .param("doubleFilters[0][1].value", "Entando")
                            .param("doubleFilters[0][2].entityAttr", JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE)
                            .param("doubleFilters[0][2].operator", "like")
                            .param("doubleFilters[0][2].value", "Fragole")
                            .sessionAttr("user", user)
                            .header("Authorization", "Bearer " + accessToken));
            bodyResult = facetedResult.andReturn().getResponse().getContentAsString();
            facetedResult.andExpect(status().isOk());
            payloadSize = JsonPath.read(bodyResult, "$.payload.contentsId.size()");
            Assertions.assertEquals(ids.size() + 1, payloadSize);
            for (int i = 0; i < ids.size(); i++) {
                String expectedId = ids.get(ids.size() - i - 1);
                facetedResult.andExpect(jsonPath("$.payload.contentsId[" + i + "]", is(expectedId)));
            }
            facetedResult.andExpect(jsonPath("$.payload.contentsId[" + ids.size() + "]", is("EVN21")));

            List<String> purged = ids.stream().filter(id -> !id.startsWith("ART")).collect(Collectors.toList());
            facetedResult = mockMvc
                    .perform(get("/plugins/advcontentsearch/facetedcontents")
                            .param("filters[0].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                            .param("filters[0].order", FieldSearchFilter.DESC_ORDER)
                            
                            .param("doubleFilters[0][0].entityAttr", JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE)
                            .param("doubleFilters[0][0].operator", "like")
                            .param("doubleFilters[0][0].value", "enterprise")
                            .param("doubleFilters[0][1].entityAttr", JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE)
                            .param("doubleFilters[0][1].operator", "like")
                            .param("doubleFilters[0][1].value", "Entando")
                            .param("doubleFilters[0][2].entityAttr", JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE)
                            .param("doubleFilters[0][2].operator", "like")
                            .param("doubleFilters[0][2].value", "Fragole")
                            
                            .param("doubleFilters[1][0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                            .param("doubleFilters[1][0].operator", "eq")
                            .param("doubleFilters[1][0].value", "EVN")
                            .param("doubleFilters[1][1].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                            .param("doubleFilters[1][1].operator", "eq")
                            .param("doubleFilters[1][1].value", "ALL")
                            
                            .sessionAttr("user", user)
                            .header("Authorization", "Bearer " + accessToken));
            
            facetedResult.andExpect(status().isOk());
            facetedResult.andExpect(jsonPath("$.payload.contentsId.size()", is(purged.size() + 1)));
            facetedResult.andExpect(jsonPath("$.payload.totalSize", is(purged.size() + 1)));
            for (int i = 0; i < purged.size(); i++) {
                String expectedId = purged.get(purged.size() - i - 1);
                facetedResult.andExpect(jsonPath("$.payload.contentsId[" + i + "]", is(expectedId)));
            }
            facetedResult.andExpect(jsonPath("$.payload.contentsId[" + purged.size() + "]", is("EVN21")));

            facetedResult = mockMvc
                    .perform(get("/plugins/advcontentsearch/facetedcontents")
                            .param("filters[0].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                            .param("filters[0].order", FieldSearchFilter.DESC_ORDER)
                            
                            .param("doubleFilters[0][0].entityAttr", JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE)
                            .param("doubleFilters[0][0].operator", "like")
                            .param("doubleFilters[0][0].value", "enterprise")
                            .param("doubleFilters[0][1].entityAttr", JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE)
                            .param("doubleFilters[0][1].operator", "like")
                            .param("doubleFilters[0][1].value", "Entando")
                            .param("doubleFilters[0][2].entityAttr", JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE)
                            .param("doubleFilters[0][2].operator", "like")
                            .param("doubleFilters[0][2].value", "Fragole")
                            
                            .param("doubleFilters[1][0].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                            .param("doubleFilters[1][0].operator", "eq")
                            .param("doubleFilters[1][0].value", "EVN")
                            .param("doubleFilters[1][1].attribute", IContentManager.ENTITY_TYPE_CODE_FILTER_KEY)
                            .param("doubleFilters[1][1].operator", "eq")
                            .param("doubleFilters[1][1].value", "ALL")
                            
                            .param("page", "2")
                            .param("pageSize", "3")
                            
                            .sessionAttr("user", user)
                            .header("Authorization", "Bearer " + accessToken));
            
            facetedResult.andExpect(status().isOk());
            facetedResult.andExpect(jsonPath("$.payload.contentsId.size()", is(3)));
            facetedResult.andExpect(jsonPath("$.payload.totalSize", is(purged.size() + 1)));
            for (int i = 0; i < 3; i++) {
                String expectedId = purged.get(purged.size() - i - 4);
                facetedResult.andExpect(jsonPath("$.payload.contentsId[" + i + "]", is(expectedId)));
            }
            facetedResult.andExpect(jsonPath("$.metaData.page", is(2)));
            facetedResult.andExpect(jsonPath("$.metaData.pageSize", is(3)));
            facetedResult.andExpect(jsonPath("$.metaData.lastPage", is(4)));
            facetedResult.andExpect(jsonPath("$.metaData.totalItems", is(purged.size() + 1)));
        } catch (Exception e) {
            throw e;
        } finally {
            if (null != ids) {
                for (int i = 0; i < ids.size(); i++) {
                    String id = ids.get(i);
                    Content content = this.contentManager.loadContent(id, false);
                    if (null != content) {
                        this.contentManager.removeOnLineContent(content);
                        this.contentManager.deleteContent(id);
                    }
                }
            }
        }
    }

    private List<String> addContentsForTestByTitle(String title) throws Exception {
        List<String> ids = new ArrayList<>();
        String[] types = {"ALL", "ART", "EVN"};
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < types.length; j++) {
                String type = types[j];
                Content content = this.contentManager.createContentType(type);
                content.setDescription(type + " - " + i + " - " + title);
                content.setMainGroup(Group.FREE_GROUP_NAME);
                ITextAttribute titleAttribute = (ITextAttribute) content.getAttributeByRole(JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE);
                titleAttribute.setText(title, "it");
                titleAttribute.setText(title, "en");
                this.contentManager.insertOnLineContent(content);
                ids.add(content.getId());
                synchronized (this) {
                    this.wait(100);
                }
            }
        }
        synchronized (this) {
            this.wait(1000);
        }
        return ids;
    }

    @Test
    public void testGetFacetedContentsByRelevance() throws Exception {
        List<String> ids = null;
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        try {
            ids = this.addContentsForTestByRelevance("RLV");
            ResultActions result = mockMvc
                    .perform(get("/plugins/advcontentsearch/facetedcontents")
                            .param("doubleFilters[0][0].entityAttr", "date")
                            .param("doubleFilters[0][0].order", "ASC")
                            .sessionAttr("user", user)
                            .header("Authorization", "Bearer " + accessToken));
            String bodyResult = result.andReturn().getResponse().getContentAsString();
            int payloadSize = JsonPath.read(bodyResult, "$.payload.contentsId.size()");
            Assertions.assertEquals(ids.size(), payloadSize);
            for (int i = 0; i < ids.size(); i++) {
                String expectedId = ids.get(ids.size() - i - 1);
                result.andExpect(jsonPath("$.payload.contentsId[" + i + "]", is(expectedId)));
            }

            List<String> expectedByTitle = Arrays.asList(new String[]{ids.get(0), ids.get(3), ids.get(6), ids.get(9), ids.get(12)});
            List<String> expectedBySubitle = Arrays.asList(new String[]{ids.get(1), ids.get(4), ids.get(7), ids.get(10), ids.get(13)});
            List<String> expectedByTextBody = Arrays.asList(new String[]{ids.get(2), ids.get(5), ids.get(8), ids.get(11), ids.get(14)});
            ResultActions facetedResult = mockMvc
                    .perform(get("/plugins/advcontentsearch/facetedcontents")
                            .param("doubleFilters[0][0].entityAttr", "title")
                            .param("doubleFilters[0][0].operator", "like")
                            .param("doubleFilters[0][0].value", "Entando")
                            .sessionAttr("user", user)
                            .header("Authorization", "Bearer " + accessToken));
            this.checkResult(facetedResult, expectedByTitle);

            facetedResult = mockMvc
                    .perform(get("/plugins/advcontentsearch/facetedcontents")
                            .param("doubleFilters[0][0].entityAttr", "subtitle")
                            .param("doubleFilters[0][0].operator", "like")
                            .param("doubleFilters[0][0].value", "Entando")
                            .sessionAttr("user", user)
                            .header("Authorization", "Bearer " + accessToken));
            this.checkResult(facetedResult, expectedBySubitle);

            facetedResult = mockMvc
                    .perform(get("/plugins/advcontentsearch/facetedcontents")
                            .param("doubleFilters[0][0].entityAttr", "title")
                            .param("doubleFilters[0][0].operator", "like")
                            .param("doubleFilters[0][0].value", "Entando")
                            .param("doubleFilters[0][1].entityAttr", "textbody")
                            .param("doubleFilters[0][1].operator", "like")
                            .param("doubleFilters[0][1].value", "Entando")
                            .sessionAttr("user", user)
                            .header("Authorization", "Bearer " + accessToken));
            List<String> expected_custom = Stream.concat(expectedByTitle.stream(), expectedByTextBody.stream()).distinct().collect(Collectors.toList());
            this.checkResult(facetedResult, expected_custom);

            facetedResult = mockMvc
                    .perform(get("/plugins/advcontentsearch/facetedcontents")
                            .param("doubleFilters[0][0].entityAttr", "title")
                            .param("doubleFilters[0][0].operator", "like")
                            .param("doubleFilters[0][0].value", "Entando")
                            .param("doubleFilters[0][0].relevancy", "3")
                            .param("doubleFilters[0][1].entityAttr", "subtitle")
                            .param("doubleFilters[0][1].operator", "like")
                            .param("doubleFilters[0][1].value", "Entando")
                            .param("doubleFilters[0][1].relevancy", "2")
                            .param("doubleFilters[0][2].entityAttr", "textbody")
                            .param("doubleFilters[0][2].operator", "like")
                            .param("doubleFilters[0][2].value", "Entando")
                            //.param("doubleFilters[0][2].relevancy", "1") //implicit
                            .sessionAttr("user", user)
                            .header("Authorization", "Bearer " + accessToken));
            bodyResult = facetedResult.andReturn().getResponse().getContentAsString();
            facetedResult.andExpect(status().isOk());
            payloadSize = JsonPath.read(bodyResult, "$.payload.contentsId.size()");
            Assertions.assertEquals(ids.size(), payloadSize);
            for (int i = 0; i < ids.size(); i++) {
                String id = JsonPath.read(bodyResult, "$.payload.contentsId[" + i + "]");
                if (i < 5) {
                    Assertions.assertTrue(expectedByTitle.contains(id));
                } else if (i > 9) {
                    Assertions.assertTrue(expectedByTextBody.contains(id));
                } else {
                    Assertions.assertTrue(expectedBySubitle.contains(id));
                }
            }

            facetedResult = mockMvc
                    .perform(get("/plugins/advcontentsearch/facetedcontents")
                            .param("doubleFilters[0][0].entityAttr", "title")
                            .param("doubleFilters[0][0].operator", "like")
                            .param("doubleFilters[0][0].value", "Entando")
                            .param("doubleFilters[0][0].relevancy", "3")
                            .param("doubleFilters[0][1].entityAttr", "subtitle")
                            .param("doubleFilters[0][1].operator", "like")
                            .param("doubleFilters[0][1].value", "Entando")
                            .param("doubleFilters[0][1].relevancy", "2")
                            .param("doubleFilters[0][2].entityAttr", "textbody")
                            .param("doubleFilters[0][2].operator", "like")
                            .param("doubleFilters[0][2].value", "Entando")
                            //.param("doubleFilters[0][2].relevancy", "1") //implicit

                            .param("doubleFilters[1][0].entityAttr", "date")
                            .param("doubleFilters[1][0].order", "DESC")
                            .sessionAttr("user", user)
                            .header("Authorization", "Bearer " + accessToken));
            bodyResult = facetedResult.andReturn().getResponse().getContentAsString();
            facetedResult.andExpect(status().isOk());
            payloadSize = JsonPath.read(bodyResult, "$.payload.contentsId.size()");
            Assertions.assertEquals(ids.size(), payloadSize);
            for (int i = 0; i < ids.size(); i++) {
                String id = JsonPath.read(bodyResult, "$.payload.contentsId[" + i + "]");
                if (i < 5) {
                    Assertions.assertEquals(expectedByTitle.get(i), id);
                } else if (i > 9) {
                    Assertions.assertEquals(expectedByTextBody.get(i - 10), id);
                } else {
                    Assertions.assertEquals(expectedBySubitle.get(i - 5), id);
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (null != ids) {
                for (int i = 0; i < ids.size(); i++) {
                    String id = ids.get(i);
                    Content content = this.contentManager.loadContent(id, false);
                    if (null != content) {
                        this.contentManager.removeOnLineContent(content);
                        this.contentManager.deleteContent(id);
                    }
                }
            }
            ((IEntityTypesConfigurer) contentManager).removeEntityPrototype("RLV");
            this.waitNotifyingThread();
        }
    }

    private void checkResult(ResultActions facetedResult, List<String> expected) throws Exception {
        String bodyResult = facetedResult.andReturn().getResponse().getContentAsString();
        facetedResult.andExpect(status().isOk());
        int payloadSize = JsonPath.read(bodyResult, "$.payload.contentsId.size()");
        Assertions.assertEquals(expected.size(), payloadSize);
        for (int i = 0; i < expected.size(); i++) {
            String id = JsonPath.read(bodyResult, "$.payload.contentsId[" + i + "]");
            Assertions.assertTrue(expected.contains(id));
        }
    }

    private List<String> addContentsForTestByRelevance(String typeCode) throws Exception {
        List<String> ids = new ArrayList<>();
        Content testType = new Content();
        testType.setTypeCode(typeCode);
        testType.setTypeDescription(typeCode + " Type");
        String[] attributeCodes = {"title", "subtitle", "textbody"};
        for (int i = 0; i < attributeCodes.length; i++) {
            String attributeCode = attributeCodes[i];
            TextAttribute attribute = (TextAttribute) this.contentManager.getEntityAttributePrototypes().get("Text");
            attribute.setName(attributeCode);
            attribute.setIndexingType(IndexableAttributeInterface.INDEXING_TYPE_TEXT);
            testType.addAttribute(attribute);
        }
        DateAttribute dateAttribute = (DateAttribute) this.contentManager.getEntityAttributePrototypes().get("Date");
        dateAttribute.setIndexingType(IndexableAttributeInterface.INDEXING_TYPE_TEXT);
        dateAttribute.setSearchable(true);
        dateAttribute.setName("date");
        testType.addAttribute(dateAttribute);
        ((IEntityTypesConfigurer) contentManager).addEntityPrototype(testType);
        super.waitNotifyingThread();

        Calendar dateToSet = Calendar.getInstance();
        for (int i = 0; i < 15; i++) {
            Content content = this.contentManager.createContentType(typeCode);
            content.setDescription(typeCode + " - " + i);
            content.setMainGroup(Group.FREE_GROUP_NAME);
            String attributeToSet = null;
            if (i % 3 == 0) {
                attributeToSet = "title";
            } else if ((i + 2) % 3 == 0) {
                attributeToSet = "subtitle";
            } else {
                attributeToSet = "textbody";
            }
            ITextAttribute titleAttribute = (ITextAttribute) content.getAttribute(attributeToSet);
            titleAttribute.setText(TEXT_FOR_TEST, "it");
            titleAttribute.setText(TEXT_FOR_TEST, "en");
            DateAttribute dateAttributeToSet = (DateAttribute) content.getAttribute("date");
            dateAttributeToSet.setDate(dateToSet.getTime());
            this.contentManager.insertOnLineContent(content);
            ids.add(content.getId());
            synchronized (this) {
                this.wait(100);
            }
            dateToSet.add(Calendar.HOUR, -1);
        }
        synchronized (this) {
            this.wait(1000);
        }
        return ids;
    }

}
