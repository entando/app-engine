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
package org.entando.entando.web.system;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.services.user.UserDetails;
import org.entando.entando.aps.system.init.ComponentManager;
import org.entando.entando.aps.system.init.model.Component;
import org.entando.entando.web.AbstractControllerTest;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

@ExtendWith(MockitoExtension.class)
class SystemControllerTest extends AbstractControllerTest {

    static final String CONTENT_SCHEDULER_CODE = "jpcontentscheduler";
    static final String CONTENT_WORKFLOW_CODE = "jpcontentworkflow";
    static final String WEBDYNAMICFORM_CODE = "jpwebdynamicform";

    @Mock
    private ComponentManager componentManager;

    @InjectMocks
    private SystemController controller;

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
    void testNoPluginsInstalled() throws Throwable {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        when(componentManager.getCurrentComponents()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/system/legacy-components-menu")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload", hasSize(0)));
    }

    @Test
    void testCmsPluginsAggregatedIntoOneEntry() throws Throwable {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        Component schedulerComponent = new Component(
                buildComponentElementWithMenuItems(CONTENT_SCHEDULER_CODE,
                        Map.of("appBuilderMenu.hook", "cms"),
                        List.of(Map.of("id", "menu-scheduler", "defaultLabel", "Scheduler",
                                "href", "do/jpcontentscheduler/config/viewItem.action",
                                "requiredPermission", "editContents|validateContents"))
                ), Collections.emptyMap());

        Component workflowComponent = new Component(
                buildComponentElementWithMenuItems(CONTENT_WORKFLOW_CODE,
                        Map.of("appBuilderMenu.hook", "cms"),
                        List.of(Map.of("id", "menu-workflow", "defaultLabel", "Workflow",
                                "href", "do/jpcontentworkflow/Workflow/list.action",
                                "requiredPermission", "editContents|validateContents"))
                ), Collections.emptyMap());

        when(componentManager.getCurrentComponents()).thenReturn(List.of(schedulerComponent, workflowComponent));

        mockMvc.perform(get("/system/legacy-components-menu")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload", hasSize(1)))
                .andExpect(jsonPath("$.payload[0]['appBuilderMenu.hook']", is("cms")))
                .andExpect(jsonPath("$.payload[0]['appBuilderMenu.items']", hasSize(2)))
                .andExpect(jsonPath("$.payload[0]['appBuilderMenu.items'][0].id", is("menu-scheduler")))
                .andExpect(jsonPath("$.payload[0]['appBuilderMenu.items'][1].id", is("menu-workflow")));
    }

    @Test
    void testLegacyPluginsKeptSeparate() throws Throwable {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        Component wdf1 = new Component(
                buildComponentElementWithMenuItems(WEBDYNAMICFORM_CODE,
                        Map.of("appBuilderMenu.hook", "legacyPlugins",
                                "appBuilderMenu.pluginId", "jpwebdynamicform",
                                "appBuilderMenu.pluginLabel", "Web Dynamic Forms"),
                        List.of(Map.of("id", "wdf-messages", "defaultLabel", "Message List",
                                "href", "do/jpwebdynamicform/Message/Operator/list.action",
                                "requiredPermission", "superuser"))
                ), Collections.emptyMap());

        Component wdf2 = new Component(
                buildComponentElementWithMenuItems("jpotherplugin",
                        Map.of("appBuilderMenu.hook", "legacyPlugins",
                                "appBuilderMenu.pluginId", "jpotherplugin",
                                "appBuilderMenu.pluginLabel", "Other Plugin"),
                        List.of(Map.of("id", "other-config", "defaultLabel", "Config",
                                "href", "do/jpotherplugin/config.action",
                                "requiredPermission", "superuser"))
                ), Collections.emptyMap());

        when(componentManager.getCurrentComponents()).thenReturn(List.of(wdf1, wdf2));

        mockMvc.perform(get("/system/legacy-components-menu")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload", hasSize(2)))
                .andExpect(jsonPath("$.payload[0]['appBuilderMenu.pluginId']", is("jpwebdynamicform")))
                .andExpect(jsonPath("$.payload[0]['appBuilderMenu.items']", hasSize(1)))
                .andExpect(jsonPath("$.payload[0]['appBuilderMenu.items'][0].id", is("wdf-messages")))
                .andExpect(jsonPath("$.payload[1]['appBuilderMenu.pluginId']", is("jpotherplugin")))
                .andExpect(jsonPath("$.payload[1]['appBuilderMenu.items']", hasSize(1)))
                .andExpect(jsonPath("$.payload[1]['appBuilderMenu.items'][0].id", is("other-config")));
    }

    @Test
    void testMixedCmsAndLegacyPlugins() throws Throwable {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        Component schedulerComponent = new Component(
                buildComponentElementWithMenuItems(CONTENT_SCHEDULER_CODE,
                        Map.of("appBuilderMenu.hook", "cms"),
                        List.of(Map.of("id", "menu-scheduler", "defaultLabel", "Scheduler",
                                "href", "do/jpcontentscheduler/config/viewItem.action",
                                "requiredPermission", "editContents|validateContents"))
                ), Collections.emptyMap());

        Component workflowComponent = new Component(
                buildComponentElementWithMenuItems(CONTENT_WORKFLOW_CODE,
                        Map.of("appBuilderMenu.hook", "cms"),
                        List.of(Map.of("id", "menu-workflow", "defaultLabel", "Workflow",
                                "href", "do/jpcontentworkflow/Workflow/list.action",
                                "requiredPermission", "editContents|validateContents"))
                ), Collections.emptyMap());

        Component wdfComponent = new Component(
                buildComponentElementWithMenuItems(WEBDYNAMICFORM_CODE,
                        Map.of("appBuilderMenu.hook", "legacyPlugins",
                                "appBuilderMenu.pluginId", "jpwebdynamicform",
                                "appBuilderMenu.pluginLabel", "Web Dynamic Forms"),
                        List.of(
                                Map.of("id", "wdf-messages", "defaultLabel", "Message List",
                                        "href", "do/jpwebdynamicform/Message/Operator/list.action",
                                        "requiredPermission", "superuser"),
                                Map.of("id", "wdf-config", "defaultLabel", "Configuration",
                                        "href", "do/jpwebdynamicform/Message/Config/list.action",
                                        "requiredPermission", "superuser")
                        )
                ), Collections.emptyMap());

        when(componentManager.getCurrentComponents()).thenReturn(
                List.of(schedulerComponent, workflowComponent, wdfComponent));

        mockMvc.perform(get("/system/legacy-components-menu")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload", hasSize(2)))
                // cms aggregated
                .andExpect(jsonPath("$.payload[0]['appBuilderMenu.hook']", is("cms")))
                .andExpect(jsonPath("$.payload[0]['appBuilderMenu.items']", hasSize(2)))
                .andExpect(jsonPath("$.payload[0]['appBuilderMenu.items'][0].id", is("menu-scheduler")))
                .andExpect(jsonPath("$.payload[0]['appBuilderMenu.items'][1].id", is("menu-workflow")))
                // legacy plugin separate
                .andExpect(jsonPath("$.payload[1]['appBuilderMenu.hook']", is("legacyPlugins")))
                .andExpect(jsonPath("$.payload[1]['appBuilderMenu.pluginId']", is("jpwebdynamicform")))
                .andExpect(jsonPath("$.payload[1]['appBuilderMenu.items']", hasSize(2)));
    }

    private static org.jdom2.Element buildComponentElement(String code, Map<String, String> properties) {
        return buildComponentElementWithMenuItems(code, properties, null);
    }

    private static org.jdom2.Element buildComponentElementWithMenuItems(
            String code, Map<String, String> properties, List<Map<String, String>> menuItems) {
        org.jdom2.Element root = new org.jdom2.Element("component");
        root.addContent(new org.jdom2.Element("code").setText(code));
        root.addContent(new org.jdom2.Element("description").setText(code));
        boolean hasProperties = properties != null && !properties.isEmpty();
        boolean hasMenuItems = menuItems != null && !menuItems.isEmpty();
        if (hasProperties || hasMenuItems) {
            org.jdom2.Element propsElement = new org.jdom2.Element("properties");
            if (hasProperties) {
                properties.forEach((key, value) -> {
                    org.jdom2.Element prop = new org.jdom2.Element("property");
                    prop.setAttribute("key", key);
                    prop.setAttribute("value", value);
                    propsElement.addContent(prop);
                });
            }
            if (hasMenuItems) {
                menuItems.forEach(item -> {
                    org.jdom2.Element menuItemEl = new org.jdom2.Element("menuItem");
                    item.forEach(menuItemEl::setAttribute);
                    propsElement.addContent(menuItemEl);
                });
            }
            root.addContent(propsElement);
        }
        return root;
    }
}