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
    void testWithContentSchedulerInstalled() throws Throwable {
        testWithPlugins(true, false);
    }

    @Test
    void testWithContentSchedulerNotInstalled() throws Throwable {
        testWithPlugins(false, false);
    }

    @Test
    void testWithContentWorkflowInstalled() throws Throwable {
        testWithPlugins(false, true);
    }

    @Test
    void testWithAllPluginsInstalled() throws Throwable {
        testWithPlugins(true, true);
    }

    private void testWithPlugins(boolean schedulerInstalled, boolean workflowInstalled) throws Throwable {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        Component schedulerComponent = new Component(
                buildComponentElement(CONTENT_SCHEDULER_CODE, Map.of(
                        "appBuilderMenu.id", "menu-scheduler",
                        "appBuilderMenu.href", "do/jpcontentscheduler/config/viewItem.action"
                )), Collections.emptyMap());

        Component workflowComponent = new Component(
                buildComponentElement(CONTENT_WORKFLOW_CODE, Map.of(
                        "appBuilderMenu.id", "menu-workflow",
                        "appBuilderMenu.href", "do/jpcontentworkflow/Workflow/list.action"
                )), Collections.emptyMap());

        List<Component> listComponents = new ArrayList<>();
        if (schedulerInstalled) {
            listComponents.add(schedulerComponent);
        }
        if (workflowInstalled) {
            listComponents.add(workflowComponent);
        }
        when(componentManager.getCurrentComponents()).thenReturn(listComponents);

        ResultActions result = mockMvc.perform(
                get("/system/report")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.payload", hasSize(listComponents.size())));

        int index = 0;
        if (schedulerInstalled) {
            result.andExpect(jsonPath("$.payload[" + index + "]['appBuilderMenu.id']", is("menu-scheduler")))
                    .andExpect(jsonPath("$.payload[" + index + "]['appBuilderMenu.href']", is("do/jpcontentscheduler/config/viewItem.action")));
            index++;
        }
        if (workflowInstalled) {
            result.andExpect(jsonPath("$.payload[" + index + "]['appBuilderMenu.id']", is("menu-workflow")))
                    .andExpect(jsonPath("$.payload[" + index + "]['appBuilderMenu.href']", is("do/jpcontentworkflow/Workflow/list.action")));
        }
    }

    private static org.jdom2.Element buildComponentElement(String code, Map<String, String> properties) {
        org.jdom2.Element root = new org.jdom2.Element("component");
        root.addContent(new org.jdom2.Element("code").setText(code));
        root.addContent(new org.jdom2.Element("description").setText(code));
        if (properties != null && !properties.isEmpty()) {
            org.jdom2.Element propsElement = new org.jdom2.Element("properties");
            properties.forEach((key, value) -> {
                org.jdom2.Element prop = new org.jdom2.Element("property");
                prop.setAttribute("key", key);
                prop.setAttribute("value", value);
                propsElement.addContent(prop);
            });
            root.addContent(propsElement);
        }
        return root;
    }
}