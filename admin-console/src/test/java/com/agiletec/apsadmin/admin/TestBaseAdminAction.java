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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import org.apache.struts2.action.Action;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        synchronized (this) {
            this.wait(3000);
        }
        assertEquals(BaseAdminAction.SUCCESS_RELOADING_RESULT_CODE, ((BaseAdminAction) this.getAction()).getReloadingResult());
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
    protected void destroy() throws Exception {
        this.configManager.updateConfigItem(SystemConstants.CONFIG_ITEM_PARAMS, this.oldConfigParam);
    }

    @BeforeEach
    private void init() {
        this.configManager = (ConfigInterface) this.getService(SystemConstants.BASE_CONFIG_MANAGER);
        this.oldConfigParam = this.configManager.getConfigItem(SystemConstants.CONFIG_ITEM_PARAMS);
    }

}
