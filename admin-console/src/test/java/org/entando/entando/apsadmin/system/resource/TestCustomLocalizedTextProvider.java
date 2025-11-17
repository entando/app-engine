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
package org.entando.entando.apsadmin.system.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import org.apache.struts2.action.Action;
import org.apache.struts2.ActionSupport;
import org.junit.jupiter.api.Test;

class TestCustomLocalizedTextProvider extends ApsAdminBaseTestCase {

    @Test
    void testExtractGlobalLabels_1() throws Throwable {
        ActionSupport action = this.executeList("admin", "en");
        String localLabel = action.getText("title.userManagement.searchUsers");
        assertNotNull(localLabel);
        assertEquals("Search for and choose more users", localLabel);
        String globalLabel = action.getText("licensing.type");
        assertNotNull(globalLabel);
        assertEquals("GNU/LGPL license, Version 2.1", globalLabel);
    }

    @Test
    void testExtractGlobalLabels_2() throws Throwable {
        ActionSupport action = this.executeList("admin", "it");
        String localLabel = action.getText("title.userManagement.searchUsers");
        assertNotNull(localLabel);
        assertEquals("Cerca e scegli altri utenti", localLabel);
        String globalLabel = action.getText("licensing.type");
        assertNotNull(globalLabel);
        assertEquals("Licenza GNU/LGPL Versione 2.1", globalLabel);
    }

    private ActionSupport executeList(String currentUser, String langCode) throws Throwable {
        this.setUserOnSession(currentUser);
        this.initAction("/do/User", "list");
        super.addParameter("request_locale", langCode);
        String result = this.executeAction();
        assertEquals(Action.SUCCESS, result);
        return super.getAction();
    }

}
