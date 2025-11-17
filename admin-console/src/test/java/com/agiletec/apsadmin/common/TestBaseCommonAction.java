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
package com.agiletec.apsadmin.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import org.apache.struts2.action.Action;
import org.apache.struts2.ActionSupport;
import org.entando.entando.aps.system.services.tenants.TenantConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author E.Santoboni
 */
class TestBaseCommonAction extends ApsAdminBaseTestCase {
    
    @Test
	void testIntro() throws Throwable {
        this.initAction("/do", "main");
        this.setUserOnSession("admin");
        String result = super.executeAction();
        assertEquals(Action.SUCCESS, result);
        
        ActionSupport action = this.getAction();
        Assertions.assertTrue(action instanceof BaseCommonAction);
        Lang currentLang = ((BaseCommonAction) action).getCurrentLang();
        assertNotNull(currentLang);
        assertEquals("en", currentLang.getCode());
        TenantConfig tenantConfig = ((BaseCommonAction) action).getCurrentTenantConfig();
        assertNull(tenantConfig);
    }

    @Test
	void testGoChangePasswordPage() throws Throwable {
        this.initAction("/do/CurrentUser", "editProfile");
        this.setUserOnSession("admin");
        String result = super.executeAction();
        assertEquals(Action.SUCCESS, result);
    }

    @Test
	void testFailureUpdate() throws Throwable {
        String username = "editorCoach";
        String rightOldPassword = "editorCoach";
        this.setUserOnSession(username);
        UserDetails oldUser = this._userManager.getUser(username);
        try {

            // oldPassword non valorizzata
            String result = this.executeUpdate("", "password", "password");
            this.verifyErrors(result, 1, "oldPassword", 1);

            // oldPassword errata
            result = this.executeUpdate("wrongOldPassword", "password", "password");
            this.verifyErrors(result, 1, "oldPassword", 1);

            // Conferma errata
            result = this.executeUpdate(rightOldPassword, "password", "badConfirm");
            this.verifyErrors(result, 1, "password", 1);

            // password uguale alla vecchia
            result = this.executeUpdate(rightOldPassword, rightOldPassword, rightOldPassword);
            this.verifyErrors(result, 1, "password", 1);

            // password con caratteri non validi corta
            result = this.executeUpdate(rightOldPassword, "p123%456$hj", "p123%456$hj");
            this.verifyErrors(result, 1, "password", 1);

            // password troppo corta
            result = this.executeUpdate(rightOldPassword, "p", "p");
            this.verifyErrors(result, 1, "password", 1);

            // password troppo lunga
            result = this.executeUpdate(rightOldPassword, "passwordDecisamenteTroppoLunga", "passwordDecisamenteTroppoLunga");
            this.verifyErrors(result, 1, "password", 1);
        } catch (RuntimeException e) {
            this._userManager.updateUser(oldUser);
            this._userManager.changePassword(oldUser.getUsername(), oldUser.getUsername());
            throw e;
        }
    }

    private void verifyErrors(String result, int extectedFieldErrors, String fieldWithErrors, int expectedErrorsOnField) {
        assertEquals(Action.INPUT, result);
        Map<String, List<String>> fieldErrors = this.getAction().getFieldErrors();
        assertEquals(extectedFieldErrors, fieldErrors.size());
        List<String> errors = fieldErrors.get(fieldWithErrors);
        assertEquals(expectedErrorsOnField, errors.size());
    }

    @Test
	void testSuccessfulUpdate() throws Throwable {
        String username = "editorCoach";
        String rightOldPassword = "editorCoach";
        String newPassword = "newPassword";
        this.setUserOnSession(username);
        UserDetails oldUser = this._userManager.getUser(username);
        try {
            String result = this.executeUpdate(rightOldPassword, newPassword, newPassword);
            assertEquals(Action.SUCCESS, result);

            UserDetails updatedUser = this._userManager.getUser(username, newPassword);
            assertNotNull(updatedUser);
        } catch (RuntimeException e) {
            throw e;
        } finally {
            this._userManager.changePassword(oldUser.getUsername(), oldUser.getUsername());
        }
    }

    private String executeUpdate(String oldPassword, String password, String passwordConfirm) throws Throwable {
        this.initAction("/do/CurrentUser", "changePassword");
        this.addParameter("oldPassword", oldPassword);
        this.addParameter("password", password);
        this.addParameter("passwordConfirm", passwordConfirm);
        return this.executeAction();
    }

    @BeforeEach
    private void init() {
        this._userManager = (IUserManager) this.getService(SystemConstants.USER_MANAGER);
    }

    private IUserManager _userManager;

}
