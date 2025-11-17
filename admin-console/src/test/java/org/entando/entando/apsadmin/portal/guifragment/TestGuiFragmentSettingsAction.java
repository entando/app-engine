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
package org.entando.entando.apsadmin.portal.guifragment;

import java.util.Map;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import org.apache.struts2.action.Action;
import java.util.Iterator;
import org.entando.entando.aps.system.services.guifragment.IGuiFragmentManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author E.Santoboni
 */
class TestGuiFragmentSettingsAction extends ApsAdminBaseTestCase {

    private ConfigInterface configManager;

    private IGuiFragmentManager guiFragmentManager;

    @Test
	void testConfigSystemParams() throws Throwable {
        this.setUserOnSession("admin");
        this.initAction("/do/Portal/GuiFragment", "systemParams");
        String result = this.executeAction();
        Assertions.assertEquals(Action.SUCCESS, result);
        Assertions.assertTrue(this.getAction() instanceof GuiFragmentSettingAction);
    }

    @Test
    void testUpdateConfigParams() throws Throwable {
        Map<String, String> initialParameters = this.guiFragmentManager.getParams();
        try {
            this.setUserOnSession("admin");
            this.initAction("/do/Portal/GuiFragment", "updateSystemParams");
            this.addParameter(IGuiFragmentManager.CONFIG_PARAM_EDIT_EMPTY_FRAGMENT_ENABLED, "true");
            this.addParameter("newParam", "valueOfNewParameter");
            String result = this.executeAction();
            Assertions.assertEquals(Action.SUCCESS, result);
            Assertions.assertEquals("true", this.configManager.getParam(IGuiFragmentManager.CONFIG_PARAM_EDIT_EMPTY_FRAGMENT_ENABLED));
            Assertions.assertNull(this.configManager.getParam("newParam"));
            Map<String, String> updatedParameters = this.guiFragmentManager.getParams();
            Assertions.assertEquals("true", updatedParameters.get(IGuiFragmentManager.CONFIG_PARAM_EDIT_EMPTY_FRAGMENT_ENABLED));
            Assertions.assertTrue(!updatedParameters.containsKey("newParam"));
        } catch (Exception e) {
            throw e;
        } finally {
            this.guiFragmentManager.updateParams(initialParameters);
            Map<String, String> restoredParameters = this.guiFragmentManager.getParams();
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
        this.guiFragmentManager = (IGuiFragmentManager) this.getApplicationContext().getBean(IGuiFragmentManager.class);
    }

}
