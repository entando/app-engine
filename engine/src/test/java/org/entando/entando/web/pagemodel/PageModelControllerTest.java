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
package org.entando.entando.web.pagemodel;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.common.model.dao.SearcherDaoPaginatedResult;
import com.agiletec.aps.system.services.pagemodel.PageModel;
import com.agiletec.aps.system.services.user.UserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.entando.entando.aps.system.services.pagemodel.PageModelService;
import org.entando.entando.aps.system.services.pagemodel.model.PageModelDto;
import org.entando.entando.aps.system.services.pagemodel.model.PageModelDtoBuilder;
import org.entando.entando.web.AbstractControllerTest;
import org.entando.entando.web.common.model.PagedMetadata;
import org.entando.entando.web.common.model.RestListRequest;
import org.entando.entando.web.pagemodel.model.PageModelConfigurationRequest;
import org.entando.entando.web.pagemodel.model.PageModelFrameReq;
import org.entando.entando.web.pagemodel.model.PageModelRequest;
import org.entando.entando.web.pagemodel.validator.PageModelValidator;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PageModelControllerTest extends AbstractControllerTest {

    private static final String PAGE_MODEL_CODE = "TEST_PM";

    private String accessToken;
    private ObjectMapper jsonMapper;
    private PageModelDtoBuilder dtoBuilder;

    @Mock
    private PageModelService pageModelService;

    @Spy
    private PageModelValidator pageModelValidator;

    @InjectMocks
    private PageModelController controller;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(entandoOauth2Interceptor)
                .setMessageConverters(getMessageConverters())
                .setHandlerExceptionResolvers(createHandlerExceptionResolver())
                .build();

        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        accessToken = mockOAuthInterceptor(user);
        jsonMapper = new ObjectMapper();
        dtoBuilder = new PageModelDtoBuilder();
    }

    @Test
    void getAllPageModelsReturnOk() throws Exception {

        when(pageModelService.getPageModels(any(RestListRequest.class), any())).thenReturn(pagedMetadata());

        ResultActions result = mockMvc.perform(
                get("/pageModels")
                        .header("Authorization", "Bearer " + accessToken))
                                      .andExpect(status().isOk())
                                      .andExpect(jsonPath("$.metaData.totalItems", is(1)));

        RestListRequest restListReq = new RestListRequest();

        verify(pageModelService, times(1)).getPageModels(eq(restListReq), any());
    }

    private PagedMetadata<PageModelDto> pagedMetadata() {
        return createPagedMetadata(ImmutableList.of(pageModel()));
    }

    private PagedMetadata<PageModelDto> createPagedMetadata(List<PageModel> pageModels) {
        SearcherDaoPaginatedResult<PageModel> paginatedResult =
                new SearcherDaoPaginatedResult<>(pageModels);

        PagedMetadata<PageModelDto> metadata = new PagedMetadata<>(new RestListRequest(), paginatedResult);

        metadata.setBody(bodyFrom(pageModels));

        return metadata;
    }

    private List<PageModelDto> bodyFrom(List<PageModel> pageModels) {
        List<PageModelDto> pageModelDtos = new ArrayList<>();

        for (PageModel pageModel : pageModels) {
            pageModelDtos.add(dtoBuilder.convert(pageModel));
        }

        return pageModelDtos;
    }

    private PageModel pageModel() {
        PageModel pageModel = new PageModel();
        pageModel.setCode(PAGE_MODEL_CODE);
        return pageModel;
    }

    @Test
    void addPageModelEmptyReturnBadRequest() throws Exception {
        PageModelRequest pageModel = new PageModelRequest();

        mockMvc.perform(
                post("/pageModels")
                        .content(jsonMapper.writeValueAsString(pageModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors.length()", is(3)));
    }

    @Test
    void addPageModelWithInvalidFirstFrameReturnBadRequest() throws Exception {

        PageModelRequest pageModel = pageModelWithInvalidFirstFrame();

        mockMvc.perform(
                post("/pageModels")
                        .content(jsonMapper.writeValueAsString(pageModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors.length()", is(1)));
    }

    private PageModelRequest pageModelWithInvalidFirstFrame() {
        PageModelFrameReq frame0 = new PageModelFrameReq();
        frame0.setPos(1);

        PageModelConfigurationRequest configuration = new PageModelConfigurationRequest();
        configuration.add(frame0);

        PageModelRequest pageModel = new PageModelRequest();
        pageModel.setCode("test");
        pageModel.setDescr("test_descr");
        pageModel.setConfiguration(configuration);

        return pageModel;
    }

    @Test
    void addPageModeWithInvalidLastFrameReturnBadRequest() throws Exception {

        PageModelRequest pageModel = pageModelWithInvalidLastFrame();

        mockMvc.perform(
                post("/pageModels")
                        .content(jsonMapper.writeValueAsString(pageModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors.length()", is(1)));
    }

    private PageModelRequest pageModelWithInvalidLastFrame() {
        PageModelFrameReq frame0 = new PageModelFrameReq(0, "descr_0");
        PageModelFrameReq frame1 = new PageModelFrameReq(2, "descr_1");

        PageModelConfigurationRequest configuration = new PageModelConfigurationRequest();
        configuration.add(frame0);
        configuration.add(frame1);

        PageModelRequest pageModel = new PageModelRequest();
        pageModel.setCode("test");
        pageModel.setDescr("test_descr");
        pageModel.setConfiguration(configuration);

        return pageModel;
    }

    @Test 
    void addPageModelWithMultipleInvalidFramesReturnBadRequest() throws Exception {

        PageModelRequest pageModel = pageModelWithMultipleInvalidFrames();

        mockMvc.perform(
                post("/pageModels")
                        .content(jsonMapper.writeValueAsString(pageModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors.length()", is(1)));
    }

    private PageModelRequest pageModelWithMultipleInvalidFrames() {
        PageModelRequest pageModel = new PageModelRequest();
        pageModel.setCode("test");
        pageModel.setDescr("test_descr");

        PageModelFrameReq frame0 = new PageModelFrameReq(0, "descr_0");
        PageModelFrameReq frame1 = new PageModelFrameReq(0, "descr_1");
        PageModelFrameReq frame2 = new PageModelFrameReq(2, "descr_2");

        pageModel.getConfiguration().add(frame0);
        pageModel.getConfiguration().add(frame1);
        pageModel.getConfiguration().add(frame2);
        return pageModel;
    }

    @Test 
    void addPageModelWithFrameMissingDescriptionReturnBadRequest() throws Exception {

        PageModelRequest pageModel = pageModelWithFrameMissingDescription();

        mockMvc.perform(
                post("/pageModels")
                        .content(jsonMapper.writeValueAsString(pageModel))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
               .andExpect(status().isBadRequest());
    }

    private PageModelRequest pageModelWithFrameMissingDescription() {
        PageModelFrameReq validFrame = new PageModelFrameReq(0, "descr_0");
        PageModelFrameReq frameMissingDescription = new PageModelFrameReq(1, null);

        PageModelConfigurationRequest configuration = new PageModelConfigurationRequest();
        configuration.add(validFrame);
        configuration.add(frameMissingDescription);

        PageModelRequest pageModel = new PageModelRequest();
        pageModel.setCode("test");
        pageModel.setDescr("test_descr");
        pageModel.setConfiguration(configuration);
        return pageModel;
    }

    @Test
    void addSimpleValidPageModelReturnOK() throws Exception {
        ResultActions result = mockMvc.perform(
                post("/pageModels")
                        .content(simplePageModelJson())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk());

        verify(pageModelService, times(1)).addPageModel(any(PageModelRequest.class));
    }

    private String simplePageModelJson() {
        return " {\n"
                    + "    \"code\": \"test\",\n"
                    + "    \"descr\": \"test\",\n"
                    + "    \"configuration\": {\n"
                    + "        \"frames\": [{\n"
                    + "            \"pos\": 0,\n"
                    + "            \"descr\": \"test_frame\",\n"
                    + "            \"mainFrame\": false,\n"
                    + "            \"defaultWidget\": null,\n"
                    + "            \"sketch\": {\n"
                    + "                 \"x1\": 0,"
                    + "                 \"y1\": 0,"
                    + "                 \"x2\": 2,"
                    + "                 \"y2\": 0"
                    + "              }\n"
                    + "        }]\n"
                    + "    },\n"
                    + "    \"pluginCode\": null,\n"
                    + "    \"template\": \"ciao\"\n"
                    + " }";
    }

    @Test
    void addComplexValidPageModelReturnOk() throws Exception {

        mockMvc.perform(
                post("/pageModels")
                        .content(complexPageModelJson())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken))
               .andExpect(status().isOk());

        verify(pageModelService, times(1)).addPageModel(any());
    }

    private String complexPageModelJson() {
        return "{\n"
                    + "    \"code\": \"home\",\n"
                    + "    \"descr\": \"Home Page\",\n"
                    + "    \"configuration\": {\n"
                    + "        \"frames\": [{\n"
                    + "                \"pos\": \"0\",\n"
                    + "                \"descr\": \"Navbar\",\n"
                    + "                \"sketch\": {\n"
                    + "                    \"x1\": \"0\",\n"
                    + "                    \"y1\": \"0\",\n"
                    + "                    \"x2\": \"2\",\n"
                    + "                    \"y2\": \"0\"\n"
                    + "                }\n"
                    + "            },\n"
                    + "            {\n"
                    + "                \"pos\": \"1\",\n"
                    + "                \"descr\": \"Navbar 2\",\n"
                    + "                \"sketch\": {\n"
                    + "                    \"x1\": \"3\",\n"
                    + "                    \"y1\": \"0\",\n"
                    + "                    \"x2\": \"5\",\n"
                    + "                    \"y2\": \"0\"\n"
                    + "                }\n"
                    + "            }\n"
                    + "        ]\n"
                    + "    },\n"
                    + "    \"template\": \"<html></html>\"\n"
                    + "}";
    }
}
