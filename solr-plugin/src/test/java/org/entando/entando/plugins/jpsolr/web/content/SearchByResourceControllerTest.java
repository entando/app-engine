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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.common.entity.IEntityTypesConfigurer;
import com.agiletec.aps.system.common.entity.model.attribute.MonoListAttribute;
import com.agiletec.aps.system.common.searchengine.IndexableAttributeInterface;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.AttachAttribute;
import com.agiletec.plugins.jacms.aps.system.services.resource.IResourceManager;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.BaseResourceDataBean;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ResourceInterface;
import com.jayway.jsonpath.JsonPath;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.entando.entando.plugins.jpsolr.web.AbstractControllerIntegrationTest;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

/**
 * @author E.Santoboni
 */
public class SearchByResourceControllerTest extends AbstractControllerIntegrationTest {

    private static List<String> fileNames = Arrays.asList("architecture", "kubernetes", "overview");
    
    @Autowired
    private IContentManager contentManager;

    @Autowired
    private IResourceManager resourceManager;

    @BeforeAll
    public static void setup() throws Exception {
        AbstractControllerIntegrationTest.setup();
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Content testType = new Content();
        testType.setTypeCode("SLR");
        testType.setTypeDescription("Solr Type");
        AttachAttribute attach = (AttachAttribute) this.contentManager.getEntityAttributePrototypes().get("Attach");
        attach.setName("document");
        attach.setIndexingType(IndexableAttributeInterface.INDEXING_TYPE_TEXT);
        testType.addAttribute(attach);
        AttachAttribute attachElement = (AttachAttribute) this.contentManager.getEntityAttributePrototypes().get("Attach");
        MonoListAttribute list = (MonoListAttribute) this.contentManager.getEntityAttributePrototypes().get("Monolist");
        list.setName("documents");
        attachElement.setName("documents");
        attachElement.setIndexingType(IndexableAttributeInterface.INDEXING_TYPE_TEXT);
        list.setNestedAttributeType(attachElement);
        list.setIndexingType(IndexableAttributeInterface.INDEXING_TYPE_TEXT);
        testType.addAttribute(list);
        ((IEntityTypesConfigurer) contentManager).addEntityPrototype(testType);
        super.waitNotifyingThread();
    }

    @AfterEach
    public void dispose() throws Exception {
        ((IEntityTypesConfigurer) contentManager).removeEntityPrototype("SLR");
        this.waitNotifyingThread();
    }

    @Test
    void testSearchByResourceAttribute() throws Exception {
        List<ResourceInterface> addedResource = null;
        List<String> addedContentIds = new ArrayList<>();
        try {
            List<BaseResourceDataBean> dataBeans = fileNames.stream().map(name -> this.createResourceDataBean(name)).collect(Collectors.toList());
            addedResource = this.resourceManager.addResources(dataBeans);
            for (int i = 0; i < addedResource.size(); i++) {
                ResourceInterface resource = addedResource.get(i);
                Content prototype = this.contentManager.createContentType("SLR");
                prototype.setDescription(resource.getDescription());
                prototype.setMainGroup(Group.FREE_GROUP_NAME);
                AttachAttribute attach = (AttachAttribute) prototype.getAttribute("document");
                attach.setResource(resource, "it");
                attach.setResource(resource, "en");
                attach.setText("Descrizione risorsa", "it");
                attach.setText("Resource description", "en");
                this.contentManager.insertOnLineContent(prototype);
                addedContentIds.add(prototype.getId());
            }
            super.waitNotifyingThread();
            this.executeTests(addedContentIds);
        } catch (Exception e) {
            throw e;
        } finally {
            this.deleteResources(addedContentIds, addedResource);
        }
    }

    @Test
    void testSearchByResourceAttributeElement() throws Exception {
        List<ResourceInterface> addedResource = null;
        List<String> addedContentIds = new ArrayList<>();
        try {
            List<BaseResourceDataBean> dataBeans = fileNames.stream().map(name -> this.createResourceDataBean(name)).collect(Collectors.toList());
            addedResource = this.resourceManager.addResources(dataBeans);
            for (int i = 0; i < addedResource.size(); i++) {
                ResourceInterface resource = addedResource.get(i);
                Content prototype = this.contentManager.createContentType("SLR");
                prototype.setDescription(resource.getDescription());
                prototype.setMainGroup(Group.FREE_GROUP_NAME);
                MonoListAttribute monolist = (MonoListAttribute) prototype.getAttribute("documents");
                AttachAttribute attach = (AttachAttribute) monolist.addAttribute();
                attach.setResource(resource, "it");
                attach.setResource(resource, "en");
                attach.setText("Descrizione risorsa", "it");
                attach.setText("Resource description", "en");
                this.contentManager.insertOnLineContent(prototype);
                addedContentIds.add(prototype.getId());
            }
            super.waitNotifyingThread();
            this.executeTests(addedContentIds);
        } catch (Exception e) {
            throw e;
        } finally {
            this.deleteResources(addedContentIds, addedResource);
        }
    }

    private BaseResourceDataBean createResourceDataBean(String fileName) {
        File file = new File("src/test/resources/document/" + fileName + ".txt");
        BaseResourceDataBean bean = new BaseResourceDataBean();
        bean.setFile(file);
        bean.setFileName(fileName + ".txt");
        bean.setDescr(fileName);
        bean.setMainGroup(Group.FREE_GROUP_NAME);
        bean.setResourceType(JacmsSystemConstants.RESOURE_ATTACH_CODE);
        bean.setMimeType("text/plain");
        return bean;
    }

    private void executeTests(List<String> addedContentIds) throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        ResultActions result_0 = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("text", "kubernetes")
                        .sessionAttr("user", user).header("Authorization", "Bearer " + accessToken));
        result_0.andExpect(status().isOk());
        String bodyResult_0 = result_0.andReturn().getResponse().getContentAsString();
        int payloadSize_0 = JsonPath.read(bodyResult_0, "$.payload.size()");
        Assertions.assertEquals(0, payloadSize_0);

        ResultActions result_1 = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("text", "kubernetes")
                        .param("includeAttachments", "true")
                        .param("filters[0].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                        .param("filters[0].order", FieldSearchFilter.ASC_ORDER)
                        .sessionAttr("user", user).header("Authorization", "Bearer " + accessToken));
        result_1.andExpect(status().isOk());
        String bodyResult_1 = result_1.andReturn().getResponse().getContentAsString();
        List<String> expectedContentsId = Arrays.asList(addedContentIds.get(1), addedContentIds.get(2));
        int payloadSize_1 = JsonPath.read(bodyResult_1, "$.payload.size()");
        Assertions.assertEquals(expectedContentsId.size(), payloadSize_1);
        for (int i = 0; i < expectedContentsId.size(); i++) {
            String extractedId = JsonPath.read(bodyResult_1, "$.payload[" + i + "]");
            Assertions.assertEquals(expectedContentsId.get(i), extractedId);
        }

        ResultActions result_2 = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("text", "kubernetes entando")
                        .param("includeAttachments", "true")
                        .param("filters[0].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                        .param("filters[0].order", FieldSearchFilter.ASC_ORDER)
                        .sessionAttr("user", user).header("Authorization", "Bearer " + accessToken));
        result_2.andExpect(status().isOk());
        String bodyResult_2 = result_2.andReturn().getResponse().getContentAsString();
        List<String> expectedContentsId_2 = addedContentIds;
        int payloadSize_2 = JsonPath.read(bodyResult_2, "$.payload.size()");
        Assertions.assertEquals(expectedContentsId_2.size(), payloadSize_2);
        for (int i = 0; i < expectedContentsId_2.size(); i++) {
            String extractedId = JsonPath.read(bodyResult_2, "$.payload[" + i + "]");
            Assertions.assertEquals(expectedContentsId_2.get(i), extractedId);
        }

        ResultActions result_3 = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("text", "kubernetes entando").param("searchOption", "all")
                        .param("includeAttachments", "true")
                        .param("filters[0].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                        .param("filters[0].order", FieldSearchFilter.ASC_ORDER)
                        .sessionAttr("user", user).header("Authorization", "Bearer " + accessToken));
        result_3.andExpect(status().isOk());
        String bodyResult_3 = result_3.andReturn().getResponse().getContentAsString();
        int payloadSize_3 = JsonPath.read(bodyResult_3, "$.payload.size()");
        Assertions.assertEquals(1, payloadSize_3);
        String extractedId_3 = JsonPath.read(bodyResult_3, "$.payload[0]");
        Assertions.assertEquals(addedContentIds.get(2), extractedId_3);

        ResultActions result_4 = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("text", "kubernetes entando").param("searchOption", "exact")
                        .param("includeAttachments", "true")
                        .param("filters[0].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                        .param("filters[0].order", FieldSearchFilter.ASC_ORDER)
                        .sessionAttr("user", user).header("Authorization", "Bearer " + accessToken));
        result_4.andExpect(status().isOk());
        String bodyResult_4 = result_4.andReturn().getResponse().getContentAsString();
        int payloadSize_4 = JsonPath.read(bodyResult_4, "$.payload.size()");
        Assertions.assertEquals(0, payloadSize_4);

        ResultActions result_5 = mockMvc
                .perform(get("/plugins/advcontentsearch/contents")
                        .param("text", "advanced data modeling").param("searchOption", "exact")
                        .param("includeAttachments", "true")
                        .param("filters[0].attribute", IContentManager.CONTENT_CREATION_DATE_FILTER_KEY)
                        .param("filters[0].order", FieldSearchFilter.ASC_ORDER)
                        .sessionAttr("user", user).header("Authorization", "Bearer " + accessToken));
        result_5.andExpect(status().isOk());
        String bodyResult_5 = result_5.andReturn().getResponse().getContentAsString();
        int payloadSize_5 = JsonPath.read(bodyResult_5, "$.payload.size()");
        Assertions.assertEquals(1, payloadSize_5);
        String extractedId_5 = JsonPath.read(bodyResult_5, "$.payload[0]");
        Assertions.assertEquals(addedContentIds.get(0), extractedId_5);
    }

    private void deleteResources(List<String> addedContentIds, List<ResourceInterface> addedResource) throws Exception {
        for (int i = 0; i < addedContentIds.size(); i++) {
            String id = addedContentIds.get(i);
            Content content = this.contentManager.loadContent(id, false);
            if (null != content) {
                this.contentManager.removeOnLineContent(content);
                this.contentManager.deleteContent(id);
            }
        }
        if (null != addedResource) {
            for (int i = 0; i < addedResource.size(); i++) {
                ResourceInterface resource = addedResource.get(i);
                this.resourceManager.deleteResource(resource);
            }
        }
    }

}
