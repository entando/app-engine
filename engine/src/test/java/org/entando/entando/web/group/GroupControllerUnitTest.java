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
package org.entando.entando.web.group;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.user.UserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.group.GroupService;
import org.entando.entando.aps.system.services.group.model.GroupDto;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.web.AbstractControllerTest;
import org.entando.entando.web.common.exceptions.ValidationConflictException;
import org.entando.entando.web.common.model.Filter;
import org.entando.entando.web.common.model.PagedMetadata;
import org.entando.entando.web.common.model.RestListRequest;
import org.entando.entando.web.group.model.GroupRequest;
import org.entando.entando.web.group.validator.GroupValidator;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;

@ExtendWith(MockitoExtension.class)
class GroupControllerUnitTest extends AbstractControllerTest {

    @Mock
    private GroupService groupService;

    @Mock
    private GroupValidator groupValidator;

    @InjectMocks
    private GroupController controller;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(entandoOauth2Interceptor)
                .setMessageConverters(getMessageConverters())
                .setHandlerExceptionResolvers(createHandlerExceptionResolver())
                .build();
    }

    @Test
    void testSeachGroups() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        when(groupService.getGroups(any(RestListRequest.class))).thenReturn(new PagedMetadata<GroupDto>());
        ResultActions result = mockMvc.perform(
                get("/groups").param("page", "1")
                .param("pageSize", "4")
                .header("Authorization", "Bearer " + accessToken)
        );

        result.andExpect(status().isOk());

        RestListRequest restListReq = new RestListRequest();
        restListReq.setPage(1);
        restListReq.setPageSize(4);
        Mockito.verify(groupService, Mockito.times(1)).getGroups(restListReq);
    }

    @Test
    void testSearchGroupsWithFilters() throws Exception {

        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        when(groupService.getGroups(any(RestListRequest.class))).thenReturn(new PagedMetadata<GroupDto>());

        ResultActions result = mockMvc.perform(
                get("/groups").param("page", "1")
                .param("pageSize", "4")
                .param("filter[0].attribute", "code")
                .param("filter[0].value", "free")
                .header("Authorization", "Bearer " + accessToken)
        );

        result.andExpect(status().isOk());

        RestListRequest restListReq = new RestListRequest();
        restListReq.setPage(1);
        restListReq.setPageSize(4);
        restListReq.addFilter(new Filter("code", "free"));
        Mockito.verify(groupService, Mockito.times(1)).getGroups(restListReq);
    }

    @Test
    void testValidatePermission() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .withGroup(Group.FREE_GROUP_NAME)
                .build();
        String accessToken = mockOAuthInterceptor(user);

        ResultActions result = mockMvc.perform(
                get("/groups")
                .header("Authorization", "Bearer " + accessToken)
        );

        result.andExpect(status().isForbidden());
    }

    @Test
    void testValidateOnUpdateWithInvalidPathAndPayload() throws EntException, Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        ObjectMapper mapper = new ObjectMapper();
        GroupRequest group = new GroupRequest();
        group.setCode("__helpdesk_");
        group.setName("Helpdesk");
        String payload = mapper.writeValueAsString(group);

        this.controller.setGroupValidator(new GroupValidator());
        ResultActions result = mockMvc.perform(
                put("/groups/{groupCode}",
                        "helpdesk")
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken));

        result.andExpect(status().isBadRequest());
    }

    @Test
    void testValidateOnDeleteReservedGroups() throws EntException, Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        String groupName = Group.FREE_GROUP_NAME;

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult("group", "group");
        bindingResult.reject(GroupValidator.ERRCODE_CANNOT_DELETE_RESERVED_GROUP, new String[]{groupName}, "group.cannot.delete.reserved");
        doThrow(new ValidationConflictException(bindingResult)).when(groupService).removeGroup(groupName);

        this.controller.setGroupValidator(new GroupValidator());
        ResultActions result = mockMvc.perform(
                delete("/groups/{groupName}",
                        groupName)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isConflict());
        result.andExpect(jsonPath("$.errors[0].code", is(GroupValidator.ERRCODE_CANNOT_DELETE_RESERVED_GROUP)));
    }

    @Test
    void testParamSize() throws EntException, Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        GroupRequest groupRequest = new GroupRequest();
        groupRequest.setCode(StringUtils.repeat("a", 21));
        groupRequest.setName(StringUtils.repeat("a", 51));

        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(groupRequest);

        this.controller.setGroupValidator(new GroupValidator());
        ResultActions result = mockMvc.perform(
                post("/groups")
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken));

        result.andExpect(status().isBadRequest());

    }

    @Test
    void testDeleteGroup() throws Exception {
        String code = "group_to_delete";
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        doNothing().when(groupService).removeGroup(Mockito.anyString());

        ResultActions result = mockMvc.perform(
                delete("/groups/{code}", code)
                .header("Authorization", "Bearer " + accessToken));

        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.code", is(code)));

    }
}
