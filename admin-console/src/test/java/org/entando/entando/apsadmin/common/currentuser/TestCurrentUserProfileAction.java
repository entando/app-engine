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
package org.entando.entando.apsadmin.common.currentuser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.entando.entando.aps.system.services.userprofile.model.IUserProfile;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.User;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author E.Santoboni
 */
class TestCurrentUserProfileAction extends ApsAdminBaseTestCase {
    
	@Test
	void testEditProfile_1() throws Throwable {
    	this.setUserOnSession(USERNAME_FOR_TEST);
        this.initAction("/do/currentuser/profile", "edit");
        String result = this.executeAction();
        assertEquals("currentUserWithoutProfile", result);
        IUserProfile currentUserProfile = (IUserProfile) this.getRequest().getSession().getAttribute(ICurrentUserProfileAction.SESSION_PARAM_NAME_CURRENT_PROFILE);
        assertNull(currentUserProfile);
    }
    
    @Test
	void testEditProfile_2() throws Throwable {
    	this.setUserOnSession("editorCustomers");
        this.initAction("/do/currentuser/profile", "edit");
        String result = this.executeAction();
        assertEquals(Action.SUCCESS, result);
        IUserProfile currentUserProfile = (IUserProfile) this.getRequest().getSession().getAttribute(ICurrentUserProfileAction.SESSION_PARAM_NAME_CURRENT_PROFILE);
        assertNotNull(currentUserProfile);
        assertEquals("editorCustomers", currentUserProfile.getUsername());
    }
    
    @Test
	void testValidateProfile() throws Throwable {
    	this.setUserOnSession("editorCustomers");
        this.initAction("/do/currentuser/profile", "edit");
        String result = this.executeAction();
        assertEquals(Action.SUCCESS, result);
		
        this.initAction("/do/currentuser/profile", "save");
		this.addParameter("Monotext:fullname", "");
        this.addParameter("Monotext:email", "");
        result = this.executeAction();
        assertEquals(Action.INPUT, result);
		
        ActionSupport action = this.getAction();
        assertEquals(2, action.getFieldErrors().size());
		
        this.initAction("/do/currentuser/profile", "save");
        this.addParameter("Monotext:fullname", "Ronald Rossi");
        this.addParameter("Monotext:email", "");
        this.addParameter("Date:birthdate", "25/09/1972");
        this.addParameter("Monotext:language", "it");
        result = this.executeAction();
        assertEquals(Action.INPUT, result);
		
        action = this.getAction();
        assertEquals(1, action.getFieldErrors().size());
        assertEquals(1, ((List<String>) action.getFieldErrors().get("Monotext:email")).size());

        IUserProfile currentUserProfile = (IUserProfile) this.getRequest().getSession().getAttribute(ICurrentUserProfileAction.SESSION_PARAM_NAME_CURRENT_PROFILE);
        assertNotNull(currentUserProfile);
        assertEquals("editorCustomers", currentUserProfile.getUsername());
        assertEquals("Ronald Rossi", currentUserProfile.getValue("fullname"));
    }
    
    @BeforeEach
    private void init() throws Exception {
        try {
            this._userManager = (IUserManager) this.getService(SystemConstants.USER_MANAGER);
            this._authorizationManager = (IAuthorizationManager) this.getService(SystemConstants.AUTHORIZATION_SERVICE);
            User user = this.createUserForTest(USERNAME_FOR_TEST);
            this._userManager.addUser(user);
			this._authorizationManager.addUserAuthorization(USERNAME_FOR_TEST, Group.FREE_GROUP_NAME, "editor");
        } catch (Throwable t) {
            throw new Exception(t);
        }
    }
    
	@AfterEach
    protected void destroy() throws Exception {
        this._userManager.removeUser(USERNAME_FOR_TEST);
    }
    
    protected User createUserForTest(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(username);
        return user;
    }
    
    private IUserManager _userManager;
	private IAuthorizationManager _authorizationManager;
	
    private static final String USERNAME_FOR_TEST = "userprofile_testUser";
    
}