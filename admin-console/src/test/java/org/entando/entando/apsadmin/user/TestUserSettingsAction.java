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
package org.entando.entando.apsadmin.user;

import java.util.Map;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import org.apache.struts2.action.Action;
import java.util.Iterator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author E.Santoboni
 */
class TestUserSettingsAction extends ApsAdminBaseTestCase {

    private ConfigInterface configManager;

    private IUserManager userManager;

    @Test
	void testConfigSystemParams() throws Throwable {
        this.setUserOnSession("admin");
        this.initAction("/do/User", "systemParams");
        String result = this.executeAction();
        Assertions.assertEquals(Action.SUCCESS, result);
        Assertions.assertTrue(this.getAction() instanceof UserSettingsAction);
    }

    @Test
    void testUpdateConfigParams() throws Throwable {
        Map<String, String> initialParameters = this.userManager.getParams();
        try {
            this.setUserOnSession("admin");
            this.initAction("/do/User", "updateSystemParams");
            this.addParameter(IUserManager.CONFIG_PARAM_PM_MM_LAST_ACCESS, "7");
            this.addParameter(IUserManager.CONFIG_PARAM_PM_MM_LAST_PASSWORD_CHANGE, "4");
            this.addParameter("newParam", "valueOfNewParameter");
            String result = this.executeAction();
            Assertions.assertEquals(Action.SUCCESS, result);
            Assertions.assertEquals("7", this.configManager.getParam(IUserManager.CONFIG_PARAM_PM_MM_LAST_ACCESS));
            Assertions.assertEquals("4", this.configManager.getParam(IUserManager.CONFIG_PARAM_PM_MM_LAST_PASSWORD_CHANGE));
            Assertions.assertNull(this.configManager.getParam("newParam"));
            Map<String, String> updatedParameters = this.userManager.getParams();
            Assertions.assertEquals("7", updatedParameters.get(IUserManager.CONFIG_PARAM_PM_MM_LAST_ACCESS));
            Assertions.assertEquals("4", updatedParameters.get(IUserManager.CONFIG_PARAM_PM_MM_LAST_PASSWORD_CHANGE));
            Assertions.assertTrue(!updatedParameters.containsKey("newParam"));
        } catch (Exception e) {
            throw e;
        } finally {
            this.userManager.updateParams(initialParameters);
            Map<String, String> restoredParameters = this.userManager.getParams();
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
        this.userManager = (IUserManager) this.getApplicationContext().getBean(IUserManager.class);
    }

}
