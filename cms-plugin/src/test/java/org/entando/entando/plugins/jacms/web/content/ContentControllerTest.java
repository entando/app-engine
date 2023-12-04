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
package org.entando.entando.plugins.jacms.web.content;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.role.Permission;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.plugins.jacms.aps.system.services.content.model.ContentDto;
import org.entando.entando.plugins.jacms.aps.system.services.content.IContentService;
import org.entando.entando.plugins.jacms.web.content.validator.ContentValidator;
import org.entando.entando.web.AbstractControllerTest;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;

@ExtendWith(MockitoExtension.class)
class ContentControllerTest extends AbstractControllerTest {

    @Mock
    private ContentValidator contentValidator;

    @Mock
    private IContentService contentService;

    @InjectMocks
    private ContentController controller;

    @BeforeEach
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(entandoOauth2Interceptor)
                .setMessageConverters(getMessageConverters())
                .setHandlerExceptionResolvers(createHandlerExceptionResolver())
                .build();
    }

    @Test
    public void shouldGetExistingContent() throws Exception {
        UserDetails user = this.createUser(true);
        when(this.contentValidator.existContent("ART123", IContentService.STATUS_DRAFT)).thenReturn(true);
        when(this.contentService.getContent(Mockito.eq("ART123"), Mockito.isNull(),
                Mockito.eq("draft"), Mockito.isNull(), Mockito.anyBoolean(), Mockito.any(UserDetails.class))).thenReturn(new ContentDto());
        ResultActions result = performGetContent("ART123", null, false, null, user);
        result.andExpect(status().isOk());
    }

    @Test
    void testUnexistingContent() throws Exception {
        UserDetails user = this.createUser(true);
        when(this.contentValidator.existContent("ART098", IContentService.STATUS_ONLINE)).thenReturn(false);
        ResultActions result = performGetContent("ART098", null, true, null, user);
        result.andExpect(status().isNotFound());
    }

    @Test
    void testAddContent() throws Exception {
        UserDetails user = this.createUser(true);
        when(this.contentService.addContent(Mockito.any(ContentDto.class), Mockito.any(UserDetails.class), Mockito.any(BindingResult.class)))
                .thenReturn(new ContentDto());
        String mockJson = "[{\n"
                + "    \"id\": \"ART123\",\n"
                + "    \"typeCode\": \"ART\",\n"
                + "    \"attributes\": [\n"
                + "         {\"code\": \"code1\", \"value\": \"value1\"},\n"
                + "         {\"code\": \"code2\", \"value\": \"value2\"}\n"
                + "    ]}]";
        ResultActions result = this.performPostContent(mockJson, user);
        result.andExpect(status().isOk());
    }
    @Test
    void testUpdateContent() throws Exception {
        UserDetails user = this.createUser(true);
        when(this.contentService.updateContent(Mockito.any(ContentDto.class), Mockito.any(UserDetails.class), Mockito.any(BindingResult.class)))
                .thenReturn(new ContentDto());
        String mockJson = "{\n"
                + "    \"id\": \"ART123\",\n"
                + "    \"typeCode\": \"ART\",\n"
                + "    \"attributes\": [\n"
                + "         {\"code\": \"code1\", \"value\": \"value1\"},\n"
                + "         {\"code\": \"code2\", \"value\": \"value2\"}\n"
                + "    ]}";
        ResultActions result = this.performPutContent("user", mockJson, user);
        result.andExpect(status().isOk());
    }

    @Test
    void shouldReturn422OnInvalidTypeCode() throws Exception {
        UserDetails user = this.createUser(true);

        doAnswer(invocation -> {
            Errors errors = invocation.getArgument(1);
            errors.reject("5", "entity.typeCode.invalid");
            return null;
        }).when(contentValidator).validateTypeCode(any(), any(Errors.class));

        String mockJson = "[{\n"
                + "    \"id\": \"ART123\",\n"
                + "    \"typeCode\": \"XXX\",\n"
                + "    \"attributes\": [\n"
                + "         {\"code\": \"code1\", \"value\": \"value1\"},\n"
                + "         {\"code\": \"code2\", \"value\": \"value2\"}\n"
                + "    ]}]";
        ResultActions result = this.performPostContent(mockJson, user);
        result.andExpect(status().isUnprocessableEntity());
    }

    private ResultActions performGetContent(String code, String modelId,
            boolean online, String langCode, UserDetails user) throws Exception {
        String accessToken = mockOAuthInterceptor(user);
        String path = "/plugins/cms/contents/{code}";
        if (null != modelId) {
            path += "/model/" + modelId;
        }
        path += "?status=" + ((online) ? IContentService.STATUS_ONLINE : IContentService.STATUS_DRAFT);
        if (null != langCode) {
            path += "&lang=" + langCode;
        }
        return mockMvc.perform(
                get(path, code)
                .header("Authorization", "Bearer " + accessToken));
    }

    private ResultActions performPostContent(String jsonContent, UserDetails user) throws Exception {
        String accessToken = mockOAuthInterceptor(user);
        return mockMvc.perform(
                post("/plugins/cms/contents")
                .content(jsonContent)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("Authorization", "Bearer " + accessToken));
    }

    private ResultActions performPutContent(String code, String jsonContent, UserDetails user) throws Exception {
        String accessToken = mockOAuthInterceptor(user);
        return mockMvc.perform(
                put("/plugins/cms/contents/{code}", code)
                .content(jsonContent)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("Authorization", "Bearer " + accessToken));
    }

    private UserDetails createUser(boolean adminAuth) throws Exception {
        UserDetails user = (adminAuth) ? (new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .withAuthorization(Group.ADMINS_GROUP_NAME, "roletest", Permission.CONTENT_EDITOR)
                .build())
                : (new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .withAuthorization(Group.FREE_GROUP_NAME, "roletest", Permission.MANAGE_PAGES)
                .build());
        return user;
    }

}
