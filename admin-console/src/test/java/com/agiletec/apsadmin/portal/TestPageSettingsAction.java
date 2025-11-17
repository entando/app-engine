/*
 * Copyright 2021-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package com.agiletec.apsadmin.portal;

import java.util.Map;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import org.apache.struts2.action.Action;
import java.util.Iterator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author E.Santoboni
 */
class TestPageSettingsAction extends ApsAdminBaseTestCase {

    private ConfigInterface configManager;

    private IPageManager pageManager;

    @Test
	void testConfigSystemParams() throws Throwable {
        this.setUserOnSession("admin");
        this.initAction("/do/Page", "systemParams");
        String result = this.executeAction();
        Assertions.assertEquals(Action.SUCCESS, result);

        PageSettingsAction action = (PageSettingsAction) this.getAction();
        Map<String, String> params = action.getSystemParams();
        Assertions.assertEquals(10, params.size());
        Assertions.assertEquals("homepage", params.get(IPageManager.CONFIG_PARAM_HOMEPAGE_PAGE_CODE));
    }

    @Test
    void testUpdateConfigParams() throws Throwable {
        Map<String, String> initialParameters = this.pageManager.getParams();
        try {
            this.setUserOnSession("admin");
            this.initAction("/do/Page", "updateSystemParams");
            this.addParameter(IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE, "newErrorPageCode");
            this.addParameter(IPageManager.CONFIG_PARAM_HOMEPAGE_PAGE_CODE, "newHomepageCode");
            this.addParameter("newParam", "valueOfNewParameter");
            String result = this.executeAction();
            Assertions.assertEquals(Action.SUCCESS, result);
            Assertions.assertEquals("newHomepageCode", this.configManager.getParam(IPageManager.CONFIG_PARAM_HOMEPAGE_PAGE_CODE));
            Assertions.assertEquals("newErrorPageCode", this.configManager.getParam(IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE));
            Assertions.assertNull(this.configManager.getParam("newParam"));
            Map<String, String> updatedParameters = this.pageManager.getParams();
            Assertions.assertEquals("newHomepageCode", updatedParameters.get(IPageManager.CONFIG_PARAM_HOMEPAGE_PAGE_CODE));
            Assertions.assertEquals("newErrorPageCode", updatedParameters.get(IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE));
            Assertions.assertTrue(!updatedParameters.containsKey("newParam"));
        } catch (Exception e) {
            throw e;
        } finally {
            this.pageManager.updateParams(initialParameters);
            Map<String, String> restoredParameters = this.pageManager.getParams();
            Iterator<Map.Entry<String, String>> iter = restoredParameters.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, String> entry = iter.next();
                Assertions.assertEquals(initialParameters.get(entry.getKey()), entry.getValue());
            }
        }
    }
    
    @BeforeEach
    private void init() {
        this.configManager = (ConfigInterface) this.getService(SystemConstants.BASE_CONFIG_MANAGER);
        this.pageManager = (IPageManager) this.getApplicationContext().getBean(IPageManager.class);
    }

}
