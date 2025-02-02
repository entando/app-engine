/*
 * Copyright 2024-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.web.page;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.PageMetadata;
import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.aps.system.services.role.Permission;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.aps.util.ApsProperties;
import org.entando.entando.aps.system.services.page.IPageAuthorizationService;
import org.entando.entando.aps.system.services.page.IPageService;
import org.entando.entando.aps.system.services.page.model.PageConfigurationDto;
import org.entando.entando.web.AbstractControllerTest;
import org.entando.entando.web.page.validator.PageConfigurationValidator;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PageConfigurationControllerTest extends AbstractControllerTest {
    
    @Mock
    private PageConfigurationValidator validator;

    @Mock
    private IPageService pageService;

    @Mock
    private IPageAuthorizationService authorizationService;
    
    @InjectMocks
    private PageConfigurationController controller;
    
    @BeforeEach
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(entandoOauth2Interceptor)
                .setMessageConverters(getMessageConverters())
                .setHandlerExceptionResolvers(createHandlerExceptionResolver())
                .build();
    }
    
    @Test
    void shouldReturnWidgetsWithConfiguration() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .withAuthorization(Group.FREE_GROUP_NAME, "managePages", Permission.MANAGE_PAGES)
                .build();
        String accessToken = mockOAuthInterceptor(user);
        
        IPage page = Mockito.mock(IPage.class);
        Mockito.when(page.getMetadata()).thenReturn(new PageMetadata());
        Widget widget = new Widget();
        widget.setTypeCode("custom_type");
        ApsProperties properties = new ApsProperties();
        properties.put("maxElemForItem",15);
        properties.put("title_en", "all offices");
        properties.put("title_it", "Tutti gli uffici");
        properties.put("userFilters", "(attributeFilter=true;key=title)+(attributeFilter=true;key=arguments;value=;test)");
        properties.put("layout", 2);
        properties.put("filters", "(attributeFilter=true;key=typology;value=amm_03)+(order=ASC;attributeFilter=true;key=title)");
        properties.put("contentType", "ORG");
        properties.put("modelId", "220021");
        widget.setConfig(properties);
        Widget[] widgets = {widget};
        Mockito.when(page.getWidgets()).thenReturn(widgets);
        PageConfigurationDto pageConfiguration = new PageConfigurationDto(page, IPageService.STATUS_DRAFT);
        Mockito.when(this.authorizationService.canView(Mockito.any(UserDetails.class),Mockito.anyString(), Mockito.anyBoolean())).thenReturn(true);
        Mockito.when(this.pageService.getPageConfiguration("test_page", IPageService.STATUS_DRAFT)).thenReturn(pageConfiguration);
        
        ResultActions result = mockMvc.perform(
                get("/pages/{pageCode}/widgets", "test_page")
                        .param("status", IPageService.STATUS_DRAFT)
                        .header("Authorization", "Bearer " + accessToken)
        );
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.size()", is(1)))
                .andExpect(jsonPath("$.payload[0].code", is("custom_type")))
                .andExpect(jsonPath("$.payload[0].config.size()", is(8)))
                .andExpect(jsonPath("$.payload[0].config.maxElemForItem", is("15")))
                .andExpect(jsonPath("$.payload[0].config.title_en", is("all offices")))
                .andExpect(jsonPath("$.payload[0].config.title_it", is("Tutti gli uffici")))
                .andExpect(jsonPath("$.payload[0].config.userFilters.size()", is(2)))
                .andExpect(jsonPath("$.payload[0].config.userFilters[0].size()", is(2)))
                .andExpect(jsonPath("$.payload[0].config.userFilters[0].attributeFilter", is(true)))
                .andExpect(jsonPath("$.payload[0].config.userFilters[0].key", is("title")))
                .andExpect(jsonPath("$.payload[0].config.userFilters[1].size()", is(3)))
                .andExpect(jsonPath("$.payload[0].config.userFilters[1].attributeFilter", is(true)))
                .andExpect(jsonPath("$.payload[0].config.userFilters[1].value", is("")))
                .andExpect(jsonPath("$.payload[0].config.userFilters[1].key", is("arguments")))
                .andExpect(jsonPath("$.payload[0].config.layout", is("2")))
                .andExpect(jsonPath("$.payload[0].config.filters.size()", is(2)))
                .andExpect(jsonPath("$.payload[0].config.filters[0].size()", is(3)))
                .andExpect(jsonPath("$.payload[0].config.filters[0].attributeFilter", is(true)))
                .andExpect(jsonPath("$.payload[0].config.filters[0].key", is("typology")))
                .andExpect(jsonPath("$.payload[0].config.filters[0].value", is("amm_03")))
                .andExpect(jsonPath("$.payload[0].config.filters[1].size()", is(3)))
                .andExpect(jsonPath("$.payload[0].config.filters[1].order", is("ASC")))
                .andExpect(jsonPath("$.payload[0].config.filters[1].attributeFilter", is(true)))
                .andExpect(jsonPath("$.payload[0].config.filters[1].key", is("title")))
                .andExpect(jsonPath("$.payload[0].config.contentType", is("ORG")))
                .andExpect(jsonPath("$.payload[0].config.modelId", is("220021")));
    }
    
}
