/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package com.agiletec.apsadmin.admin;

import static com.agiletec.aps.system.services.reload.ReloadConfigThread.RELOAD_THREAD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.util.ApsTenantApplicationUtils;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import org.apache.struts2.action.Action;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.PageContext;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * @author E.Santoboni
 */
class TestBaseAdminAction extends ApsAdminBaseTestCase {

    private ConfigInterface configManager;
    private String oldConfigParam;

    @Test
    void testReloadConfig() throws Throwable {
        this.setUserOnSession("supervisorCoach");
        this.initAction("/do/BaseAdmin", "reloadConfig");
        String result = this.executeAction();
        assertEquals("userNotAllowed", result);

        this.setUserOnSession("admin");
        this.initAction("/do/BaseAdmin", "reloadConfig");
        result = this.executeAction();
        assertEquals(Action.SUCCESS, result);
        assertEquals(BaseAdminAction.PROGRESS_RELOADING_RESULT_CODE, ((BaseAdminAction) this.getAction()).getReloadingResult());
        verifySuccessfulReload();
    }

    @Test
    void testReloadConfigurationError() throws Throwable {
        try (MockedStatic<ApsWebApplicationUtils> mockAWAU = Mockito.mockStatic(ApsWebApplicationUtils.class);
             MockedStatic<ApsTenantApplicationUtils> apsTenantApplicationUtils = Mockito.mockStatic(ApsTenantApplicationUtils.class)) {
            apsTenantApplicationUtils.when(ApsTenantApplicationUtils::getTenant).thenReturn(Optional.empty());
            mockAWAU.when(ApsWebApplicationUtils::isReloadInProgress)
                    .thenThrow(new RuntimeException("error needed for testing, ignore me"));
            mockAWAU.when(ApsWebApplicationUtils::getReloadProgress).thenReturn(77);

            mockAWAU.when(() -> ApsWebApplicationUtils.getResources(anyString(), any(ServletContext.class)))
                    .thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getResources(anyString(), any(PageContext.class)))
                    .thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getBean(anyString(), any(HttpServletRequest.class)))
                    .thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getBean(anyString(), any(PageContext.class)))
                    .thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getWebApplicationContext(any(HttpServletRequest.class)))
                    .thenReturn(WebApplicationContextUtils.getWebApplicationContext(
                            this.getRequest().getSession().getServletContext()));
            mockAWAU.when(() -> ApsWebApplicationUtils.executeSystemRefresh(any(HttpServletRequest.class)))
                    .thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.executeSystemRefresh(any(ServletContext.class)))
                    .thenCallRealMethod();
            this.setUserOnSession("admin");
            this.initAction("/do/BaseAdmin", "reloadConfig");
            String result = this.executeAction();

            assertEquals("reloadError", result);
            assertEquals(BaseAdminAction.FAILURE_RELOADING_RESULT_CODE,
                    ((BaseAdminAction) this.getAction()).getReloadingResult());
            assertFalse(isReloadThreadError());
            assertTrue(ApsWebApplicationUtils.getReloadInfo().isEmpty());
        }
    }

    @Test
    void testDoubleReload() throws Throwable {
        try (MockedStatic<ApsWebApplicationUtils> mockAWAU = Mockito.mockStatic(ApsWebApplicationUtils.class);
             MockedStatic<ApsTenantApplicationUtils> apsTenantApplicationUtils = Mockito.mockStatic(ApsTenantApplicationUtils.class)) {
            apsTenantApplicationUtils.when(ApsTenantApplicationUtils::getTenant).thenReturn(Optional.empty());
            mockAWAU.when(ApsWebApplicationUtils::isReloadInProgress).thenReturn(true);
            mockAWAU.when(ApsWebApplicationUtils::getReloadProgress).thenReturn(77);

            mockAWAU.when(() -> ApsWebApplicationUtils.getResources(anyString(), any(ServletContext.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getResources(anyString(), any(PageContext.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getBean(anyString(), any(HttpServletRequest.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getBean(anyString(), any(PageContext.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getWebApplicationContext(any(HttpServletRequest.class)))
                    .thenReturn(WebApplicationContextUtils.getWebApplicationContext(this.getRequest().getSession().getServletContext()));
            mockAWAU.when(() -> ApsWebApplicationUtils.executeSystemRefresh(any(HttpServletRequest.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.executeSystemRefresh(any(ServletContext.class))).thenCallRealMethod();

            this.setUserOnSession("admin");
            this.initAction("/do/BaseAdmin", "reloadConfig");
            String result = this.executeAction();
            assertEquals(Action.SUCCESS, result);

            assertEquals(BaseAdminAction.PROGRESS_RELOADING_RESULT_CODE, ((BaseAdminAction) this.getAction()).getReloadingResult());
        }
    }

    @Test
    void testReloadStatusInProgress() throws Throwable {
        try (MockedStatic<ApsWebApplicationUtils> mockAWAU = Mockito.mockStatic(ApsWebApplicationUtils.class);
             MockedStatic<ApsTenantApplicationUtils> apsTenantApplicationUtils = Mockito.mockStatic(ApsTenantApplicationUtils.class)) {
            apsTenantApplicationUtils.when(ApsTenantApplicationUtils::getTenant).thenReturn(Optional.empty());
            mockAWAU.when(ApsWebApplicationUtils::isReloadInProgress).thenReturn(true);
            mockAWAU.when(ApsWebApplicationUtils::getReloadProgress).thenReturn(77);

            mockAWAU.when(() -> ApsWebApplicationUtils.getResources(anyString(), any(ServletContext.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getResources(anyString(), any(PageContext.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getBean(anyString(), any(HttpServletRequest.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getBean(anyString(), any(PageContext.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getWebApplicationContext(any(HttpServletRequest.class)))
                    .thenReturn(WebApplicationContextUtils.getWebApplicationContext(this.getRequest().getSession().getServletContext()));
            mockAWAU.when(() -> ApsWebApplicationUtils.executeSystemRefresh(any(HttpServletRequest.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.executeSystemRefresh(any(ServletContext.class))).thenCallRealMethod();

            this.setUserOnSession("admin");
            this.initAction("/do/BaseAdmin", "reloadStatus");
            String result = this.executeAction();
            assertEquals("inProgress", result);

            assertEquals(BaseAdminAction.PROGRESS_RELOADING_RESULT_CODE, ((BaseAdminAction) this.getAction()).getReloadingResult());
        }
    }

    @Test
    void testReloadStatusCompleted() throws Throwable {
        try (MockedStatic<ApsWebApplicationUtils> mockAWAU = Mockito.mockStatic(ApsWebApplicationUtils.class);
             MockedStatic<ApsTenantApplicationUtils> apsTenantApplicationUtils = Mockito.mockStatic(ApsTenantApplicationUtils.class)) {
            apsTenantApplicationUtils.when(ApsTenantApplicationUtils::getTenant).thenReturn(Optional.empty());
            mockAWAU.when(ApsWebApplicationUtils::isReloadInProgress).thenReturn(false);
            mockAWAU.when(ApsWebApplicationUtils::getReloadProgress).thenReturn(0);

            mockAWAU.when(() -> ApsWebApplicationUtils.getResources(anyString(), any(ServletContext.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getResources(anyString(), any(PageContext.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getBean(anyString(), any(HttpServletRequest.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getBean(anyString(), any(PageContext.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getWebApplicationContext(any(HttpServletRequest.class)))
                    .thenReturn(WebApplicationContextUtils.getWebApplicationContext(this.getRequest().getSession().getServletContext()));
            mockAWAU.when(() -> ApsWebApplicationUtils.executeSystemRefresh(any(HttpServletRequest.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.executeSystemRefresh(any(ServletContext.class))).thenCallRealMethod();
            mockAWAU.when(ApsWebApplicationUtils::getReloadInfo).thenReturn(mockTestResultOk());

            this.setUserOnSession("admin");
            this.initAction("/do/BaseAdmin", "reloadStatus");
            String result = this.executeAction();
            assertEquals(Action.SUCCESS, result);
            assertFalse(((BaseAdminAction) this.getAction()).isReloadingErrorDetect());
            assertEquals(mockTestResultOk(), ((BaseAdminAction) this.getAction()).getReloadInfo());
            assertNotSame(mockTestResultOk(), ((BaseAdminAction) this.getAction()).getReloadInfo());
            assertEquals(BaseAdminAction.SUCCESS_RELOADING_RESULT_CODE, ((BaseAdminAction) this.getAction()).getReloadingResult());
        }
    }

    @Test
    void testReloadStatusSuccess() throws Throwable {
        assertFalse(ApsWebApplicationUtils.isReloadInProgress());

        this.setUserOnSession("supervisorCoach");
        this.initAction("/do/BaseAdmin", "reloadStatus");
        String result = this.executeAction();
        assertEquals("userNotAllowed", result);

        this.setUserOnSession("admin");
        this.initAction("/do/BaseAdmin", "reloadStatus");
        result = this.executeAction();
        assertEquals(Action.SUCCESS, result);
        assertEquals(BaseAdminAction.SUCCESS_RELOADING_RESULT_CODE, ((BaseAdminAction) this.getAction()).getReloadingResult());
    }

    @Test
    void testReloadStatusCompletedWithWarning() throws Throwable {
        try (MockedStatic<ApsWebApplicationUtils> mockAWAU = Mockito.mockStatic(ApsWebApplicationUtils.class);
             MockedStatic<ApsTenantApplicationUtils> apsTenantApplicationUtils = Mockito.mockStatic(ApsTenantApplicationUtils.class)) {
            apsTenantApplicationUtils.when(ApsTenantApplicationUtils::getTenant).thenReturn(Optional.empty());
            mockAWAU.when(ApsWebApplicationUtils::isReloadInProgress).thenCallRealMethod();
            mockAWAU.when(ApsWebApplicationUtils::getReloadProgress).thenCallRealMethod();

            mockAWAU.when(() -> ApsWebApplicationUtils.getResources(anyString(), any(ServletContext.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getResources(anyString(), any(PageContext.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getBean(anyString(), any(HttpServletRequest.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getBean(anyString(), any(PageContext.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getWebApplicationContext(any(HttpServletRequest.class)))
                    .thenReturn(WebApplicationContextUtils.getWebApplicationContext(this.getRequest().getSession().getServletContext()));
            mockAWAU.when(() -> ApsWebApplicationUtils.executeSystemRefresh(any(HttpServletRequest.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.executeSystemRefresh(any(ServletContext.class))).thenCallRealMethod();
            mockAWAU.when(ApsWebApplicationUtils::getReloadInfo).thenReturn(mockTestResultWarning());

            this.setUserOnSession("admin");
            this.initAction("/do/BaseAdmin", "reloadStatus");
            String result = this.executeAction();
            assertEquals(Action.SUCCESS, result);
            assertTrue(((BaseAdminAction) this.getAction()).isReloadingErrorDetect());
            assertEquals(BaseAdminAction.WARNING_RELOADING_RESULT_CODE, ((BaseAdminAction) this.getAction()).getReloadingResult());
        }
    }

    @Test
    void testReloadStatusWithThreadError() throws Throwable {
        try (MockedStatic<ApsWebApplicationUtils> mockAWAU = Mockito.mockStatic(ApsWebApplicationUtils.class);
             MockedStatic<ApsTenantApplicationUtils> apsTenantApplicationUtils = Mockito.mockStatic(ApsTenantApplicationUtils.class)) {
            apsTenantApplicationUtils.when(ApsTenantApplicationUtils::getTenant).thenReturn(Optional.empty());
            mockAWAU.when(ApsWebApplicationUtils::isReloadInProgress).thenCallRealMethod();
            mockAWAU.when(ApsWebApplicationUtils::getReloadProgress).thenCallRealMethod();

            mockAWAU.when(() -> ApsWebApplicationUtils.getResources(anyString(), any(ServletContext.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getResources(anyString(), any(PageContext.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getBean(anyString(), any(HttpServletRequest.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getBean(anyString(), any(PageContext.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.getWebApplicationContext(any(HttpServletRequest.class)))
                    .thenReturn(WebApplicationContextUtils.getWebApplicationContext(this.getRequest().getSession().getServletContext()));
            mockAWAU.when(() -> ApsWebApplicationUtils.executeSystemRefresh(any(HttpServletRequest.class))).thenCallRealMethod();
            mockAWAU.when(() -> ApsWebApplicationUtils.executeSystemRefresh(any(ServletContext.class))).thenCallRealMethod();
            mockAWAU.when(ApsWebApplicationUtils::getReloadInfo).thenReturn(mockTestResultThreadError());

            this.setUserOnSession("admin");
            this.initAction("/do/BaseAdmin", "reloadStatus");
            String result = this.executeAction();
            assertEquals(Action.SUCCESS, result);
            assertTrue(((BaseAdminAction) this.getAction()).isReloadingErrorDetect());
            assertEquals(BaseAdminAction.FAILURE_RELOADING_RESULT_CODE, ((BaseAdminAction) this.getAction()).getReloadingResult());
        }
    }

    @Test
    void testAjaxCall() throws Throwable {
        assertFalse(ApsWebApplicationUtils.isReloadInProgress());
        this.setUserOnSession("admin");
        this.initAction("/do/BaseAdmin", "reloadStatusJson");
        String result = this.executeAction();
        assertEquals(Action.SUCCESS, result);
        final int progress = ((BaseAdminAction) this.getAction()).getReloadProgress();
        assertEquals(-1, progress);
    }

    @Test
    void testReloadEntitiesReferences() throws Throwable {
        this.setUserOnSession("supervisorCoach");
        this.initAction("/do/BaseAdmin", "reloadEntitiesReferences");
        String result = this.executeAction();
        assertEquals("userNotAllowed", result);

        this.setUserOnSession("admin");
        this.initAction("/do/BaseAdmin", "reloadEntitiesReferences");
        result = this.executeAction();
        assertEquals(Action.SUCCESS, result);
        synchronized (this) {
            this.wait(3000);
        }
        super.waitNotifyingThread();
    }

    @Test
    void testConfigSystemParams() throws Throwable {
        this.setUserOnSession("admin");
        this.initAction("/do/BaseAdmin", "configSystemParams");
        String result = this.executeAction();
        assertEquals(Action.SUCCESS, result);

        BaseAdminAction action = (BaseAdminAction) this.getAction();
        Map<String, String> params = action.getSystemParams();
        assertTrue(params.size() >= 6);
        assertEquals("homepage", params.get(IPageManager.CONFIG_PARAM_HOMEPAGE_PAGE_CODE));
    }

    @Test
    void testUpdateConfigParams_1() throws Throwable {
        this.setUserOnSession("admin");
        this.initAction("/do/BaseAdmin", "updateSystemParams");
        this.addParameter(IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE, "newErrorPageCode");
        this.addParameter(IPageManager.CONFIG_PARAM_HOMEPAGE_PAGE_CODE, "newHomepageCode");
        String result = this.executeAction();
        assertEquals(Action.SUCCESS, result);

        assertEquals("newHomepageCode", this.configManager.getParam(IPageManager.CONFIG_PARAM_HOMEPAGE_PAGE_CODE));
        assertEquals("newErrorPageCode", this.configManager.getParam(IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE));
    }

    @Test
    void testUpdateConfigParams_2() throws Throwable {
        assertEquals("homepage", this.configManager.getParam(IPageManager.CONFIG_PARAM_HOMEPAGE_PAGE_CODE));
        assertEquals("errorpage", this.configManager.getParam(IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE));

        this.setUserOnSession("admin");
        this.initAction("/do/BaseAdmin", "updateSystemParams");
        this.addParameter("newCustomParameter", "parameterValue");
        this.addParameter(IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE, "newErrorPageCode");
        this.addParameter(IPageManager.CONFIG_PARAM_HOMEPAGE_PAGE_CODE, "newHomepageCode");
        String result = this.executeAction();
        assertEquals(Action.SUCCESS, result);

        assertEquals("newHomepageCode", this.configManager.getParam(IPageManager.CONFIG_PARAM_HOMEPAGE_PAGE_CODE));
        assertEquals("newErrorPageCode", this.configManager.getParam(IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE));
        assertNull(this.configManager.getParam("newCustomParameter"));

        this.initAction("/do/BaseAdmin", "updateSystemParams");
        this.addParameter("newCustomParameter", "parameterValue");
        this.addParameter("newCustomParameter_newParamMarker", "true");
        this.addParameter(IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE, "newErrorPageCode");
        this.addParameter(IPageManager.CONFIG_PARAM_HOMEPAGE_PAGE_CODE, "newHomepageCode");
        result = this.executeAction();
        assertEquals(Action.SUCCESS, result);

        assertEquals("newHomepageCode", this.configManager.getParam(IPageManager.CONFIG_PARAM_HOMEPAGE_PAGE_CODE));
        assertEquals("newErrorPageCode", this.configManager.getParam(IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE));
        assertNotNull(this.configManager.getParam("newCustomParameter"));
        assertEquals("parameterValue", this.configManager.getParam("newCustomParameter"));
    }

    @AfterEach
    void destroy() throws Exception {
        try {
            this.configManager.updateConfigItem(SystemConstants.CONFIG_ITEM_PARAMS, this.oldConfigParam);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @BeforeEach
    void init() {
        this.configManager = (ConfigInterface) this.getService(SystemConstants.BASE_CONFIG_MANAGER);
        this.oldConfigParam = this.configManager.getConfigItem(SystemConstants.CONFIG_ITEM_PARAMS);
    }


    private boolean isReloadThreadError() {
        return ApsWebApplicationUtils.getReloadInfo().containsKey(RELOAD_THREAD);
    }

    private boolean hasReloadError() {
        Optional<String> error = ApsWebApplicationUtils.getReloadInfo().values()
                .stream().filter(StringUtils::isNotBlank)
                .findFirst();
        return error.isPresent();
    }

    private void verifySuccessfulReload() {
        assertFalse(isReloadThreadError());
        assertFalse(hasReloadError());
    }

    private Map<String, String> mockTestResultOk() {
        Map<String, String> map = new HashMap<>();
        map.put("jacmsSearchEngineManager", "");
        map.put("jacmsResourceManager", "");
        map.put("UserProfileManager", "");
        return map;
    }

    private Map<String, String> mockTestResultWarning() {
        Map<String, String> map = new HashMap<>(mockTestResultOk());
        map.put("myService", "error message");
        return map;
    }

    private Map<String, String> mockTestResultThreadError() {
        Map<String, String> map = new HashMap<>(mockTestResultOk());
        map.put(RELOAD_THREAD, "thread error message");
        return map;
    }
}