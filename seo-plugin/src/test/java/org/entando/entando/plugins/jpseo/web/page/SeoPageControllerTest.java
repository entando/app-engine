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
package org.entando.entando.plugins.jpseo.web.page;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.role.Permission;
import com.agiletec.aps.system.services.user.UserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import jakarta.servlet.http.HttpSession;
import org.entando.entando.aps.system.services.page.PageAuthorizationService;
import org.entando.entando.plugins.jpseo.aps.system.services.page.SeoPageDto;
import org.entando.entando.plugins.jpseo.aps.system.services.page.SeoPageService;
import org.entando.entando.plugins.jpseo.web.page.model.SeoPageRequest;
import org.entando.entando.plugins.jpseo.web.page.validator.SeoPageValidator;
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

@ExtendWith(MockitoExtension.class)
class SeoPageControllerTest extends AbstractControllerTest {

    private static final String PAGE_CODE = "TST";
    private static final String STATUS_DRAFT = "draft";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private SeoPageValidator validator;

    @Mock
    private SeoPageService service;

    @Mock
    private PageAuthorizationService authorizationService;

    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private SeoPageController controller;

    @BeforeEach
    public void setUp() throws Exception {
        entandoOauth2Interceptor.setAuthorizationManager(authorizationManager);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(entandoOauth2Interceptor)
                .setMessageConverters(getMessageConverters())
                .setHandlerExceptionResolvers(createHandlerExceptionResolver())
                .build();
    }

    @Test
    void testGetSeoPage() throws Exception {
        SeoPageDto seoPageDto = new SeoPageDto();
        UserDetails user = this.createUser(true);
        Mockito.lenient().when(this.httpSession.getAttribute("user")).thenReturn(user);
        when(this.service.getPage(Mockito.eq(PAGE_CODE), Mockito.eq(STATUS_DRAFT), Mockito.any()))
                .thenReturn(seoPageDto);
        when(this.authorizationService.canView(user, PAGE_CODE, false)).thenReturn(true);
        ResultActions result = getSeoPage(PAGE_CODE, STATUS_DRAFT, user);
        result.andExpect(status().isOk());
    }

    @Test
    void testGetSeoPageNotAuthorized() throws Exception {
        SeoPageDto seoPageDto = new SeoPageDto();
        UserDetails user = this.createUser(true);
        Mockito.lenient().when(this.httpSession.getAttribute("user")).thenReturn(user);
        Mockito.lenient().when(this.service.getPage(Mockito.eq(PAGE_CODE), Mockito.eq(STATUS_DRAFT)))
                .thenReturn(seoPageDto);
        when(this.authorizationService.canView(user, PAGE_CODE, false)).thenReturn(false);
        ResultActions result = getSeoPage(PAGE_CODE, STATUS_DRAFT, user);
        result.andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyCreationOfPageIfUserDoesNotHavePermissionOnParentNode() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .withAuthorization("test_group", "managePages", Permission.MANAGE_PAGES)
                .build();
        String accessToken = mockOAuthInterceptor(user);

        Mockito.when(validator.getDraftPage("free_page")).thenReturn(Mockito.mock(IPage.class));
        Mockito.when(authorizationService.getGroupCodesForEditing(Mockito.any(UserDetails.class))).thenReturn(List.of("test_group"));
        Mockito.when(authorizationService.canEdit(Mockito.any(), Mockito.eq("free_page"))).thenReturn(false);

        SeoPageRequest seoPageRequest = new SeoPageRequest();
        seoPageRequest.setParentCode("free_page");
        seoPageRequest.setCode("new_page");
        seoPageRequest.setOwnerGroup("test_group");
        seoPageRequest.setPageModel("page_model");

        String requestBody = MAPPER.writeValueAsString(seoPageRequest);

        ResultActions result = mockMvc.perform(
                post("/plugins/seo/pages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isForbidden());
    }

    private ResultActions getSeoPage(String pageCode, String status, UserDetails user) throws Exception {
        String accessToken = mockOAuthInterceptor(user);
        String path = "/plugins/seo/pages/{pageCode}";
        return mockMvc.perform(
                get(path, pageCode)
                        .param("status", status)
                        .header("Authorization", "Bearer " + accessToken));
    }

    private UserDetails createUser(boolean adminAuth) {
        UserDetails user = (adminAuth) ? (new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .withAuthorization(Group.ADMINS_GROUP_NAME, "roletest", Permission.SUPERUSER)
                .build())
                : (new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                        .withAuthorization(Group.FREE_GROUP_NAME, "roletest", Permission.MANAGE_PAGES)
                        .build());
        return user;
    }

}
