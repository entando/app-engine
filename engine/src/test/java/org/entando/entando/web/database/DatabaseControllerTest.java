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
package org.entando.entando.web.database;

import com.agiletec.aps.system.services.user.UserDetails;
import org.entando.entando.aps.system.init.DatabaseManager;
import org.entando.entando.aps.system.init.IComponentManager;
import org.entando.entando.aps.system.init.model.DataSourceDumpReport;
import org.entando.entando.aps.system.services.database.DatabaseService;
import org.entando.entando.web.AbstractControllerTest;
import org.entando.entando.web.database.validator.DatabaseValidator;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatabaseControllerTest extends AbstractControllerTest {

    private MockMvc mockMvc;

    @Spy
    private DatabaseService databaseService = new DatabaseService();

    @Mock
    private DatabaseValidator databaseValidator;

    @Mock
    private DatabaseManager databaseManager;

    @Mock
    private IComponentManager componentManager;

    @InjectMocks
    private DatabaseController controller;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(entandoOauth2Interceptor)
                .setMessageConverters(getMessageConverters())
                .setHandlerExceptionResolvers(createHandlerExceptionResolver())
                .build();
        databaseService.setDatabaseManager(this.databaseManager);
        databaseService.setComponentManager(this.componentManager);
        controller.setDatabaseService(databaseService);
    }

    @Test
    void getReports() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        ResultActions result = mockMvc.perform(get("/database")
                .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk());
    }

    @Test
    void getReport_1() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        String xml = null;
        DataSourceDumpReport report = new DataSourceDumpReport(xml);
        when(databaseManager.getBackupReport(ArgumentMatchers.anyString())).thenReturn(report);

        ResultActions result = mockMvc.perform(
                get("/database/report/{reportCode}", new Object[]{"develop"})
                .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk());
    }

    @Test
    void getReport_2() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        when(databaseManager.getBackupReport(ArgumentMatchers.anyString())).thenReturn(null);
        ResultActions result = mockMvc.perform(
                get("/database/report/{reportCode}", new Object[]{"develop"})
                .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isNotFound());
    }

    @Test
    void startBackup() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        String xml = null;
        DataSourceDumpReport report = new DataSourceDumpReport(xml);
        Mockito.lenient().when(databaseManager.getBackupReport(ArgumentMatchers.anyString())).thenReturn(report);
        ResultActions result = mockMvc.perform(
                post("/database/startBackup").content("{}")
                .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk());
        Mockito.verify(databaseService, Mockito.times(1)).startDatabaseBackup();
    }

    @Test
    void startRestore_1() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        String xml = null;
        DataSourceDumpReport report = new DataSourceDumpReport(xml);
        when(databaseManager.getBackupReport(ArgumentMatchers.anyString())).thenReturn(report);
        ResultActions result = mockMvc.perform(
                put("/database/restoreBackup/{reportCode}", "reportCode").content("{}")
                .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk());
        Mockito.verify(databaseService, Mockito.times(1)).startDatabaseRestore("reportCode");
        Mockito.verify(databaseManager, Mockito.times(1)).dropAndRestoreBackup("reportCode");
    }

    @Test
    void startRestore_2() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        when(databaseManager.getBackupReport(ArgumentMatchers.anyString())).thenReturn(null);
        ResultActions result = mockMvc.perform(
                put("/database/restoreBackup/{reportCode}", "reportCode").content("{}")
                .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isNotFound());
        Mockito.verify(databaseService, Mockito.times(1)).startDatabaseRestore("reportCode");
        Mockito.verify(databaseManager, Mockito.times(0)).dropAndRestoreBackup("reportCode");
    }

    @Test
    void deleteReport() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);
        String xml = null;
        DataSourceDumpReport report = new DataSourceDumpReport(xml);
        Mockito.lenient().when(databaseManager.getBackupReport(ArgumentMatchers.anyString())).thenReturn(report);
        ResultActions result = mockMvc.perform(
                delete("/database/report/{reportCode}", new Object[]{"reportCode"})
                .header("Authorization", "Bearer " + accessToken));
        result.andExpect(status().isOk());
        Mockito.verify(databaseService, Mockito.times(1)).deleteDumpReport("reportCode");
    }

}
