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
package org.entando.entando.apsadmin.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.user.IUserManager;
import java.util.List;

import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import org.apache.struts2.action.Action;
import org.entando.entando.aps.system.services.userprofile.IUserProfileManager;
import org.entando.entando.aps.system.services.userprofile.model.IUserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author E.Santoboni
 */
class TestUserFinderAction extends ApsAdminBaseTestCase {

    private IUserManager userManager;
    private IUserProfileManager userProfileManager;

    @BeforeEach
    private void init() {
        this.userManager = (IUserManager) this.getService(SystemConstants.USER_MANAGER);
        this.userProfileManager = (IUserProfileManager) this.getService(SystemConstants.USER_PROFILE_MANAGER);
    }

    @Test
    void testListWithUserNotAllowed() throws Throwable {
        String result = this.executeList("developersConf");
        assertEquals("apslogin", result);
    }

    @Test
    void testList() throws Throwable {
        String result = this.executeList("admin");
        assertEquals(Action.SUCCESS, result);
        UserProfileFinderAction userFinderAction = (UserProfileFinderAction) this.getAction();
        List<String> usernames = userFinderAction.getSearchResult();
        assertFalse(usernames.isEmpty());
        assertTrue(usernames.size() >= 8);
        String username = "test_profile";
        assertFalse(usernames.contains(username));
        try {
            IUserProfile profile = this.userProfileManager.getDefaultProfileType();
            profile.setId(username);
            userProfileManager.addProfile(username, profile);
            assertNotNull(userProfileManager.getProfile(username));
            result = this.executeList("admin");
            assertEquals(Action.SUCCESS, result);
            List<String> newUsernames = userFinderAction.getSearchResult();
            assertEquals(usernames.size() + 1, newUsernames.size());
            assertTrue(newUsernames.contains(username));
        } catch (Exception e) {
            throw e;
        } finally {
            this.userManager.removeUser(username);
            assertNull(userManager.getUser(username));
            assertNull(userProfileManager.getProfile(username));
        }
    }

    @Test
    void testSearchUsers() throws Throwable {
        String result = this.executeSearch("admin", "ustomer");
        assertEquals(Action.SUCCESS, result);
        UserProfileFinderAction userFinderAction = (UserProfileFinderAction) this.getAction();
        List<String> users = userFinderAction.getSearchResult();
        assertEquals(3, users.size());

        result = this.executeSearch("admin", "anager");
        assertEquals(Action.SUCCESS, result);
        userFinderAction = (UserProfileFinderAction) this.getAction();
        users = userFinderAction.getSearchResult();
        assertEquals(2, users.size());

        result = this.executeSearch("admin", "");
        assertEquals(Action.SUCCESS, result);
        userFinderAction = (UserProfileFinderAction) this.getAction();
        users = userFinderAction.getSearchResult();
        assertTrue(users.size() >= 8);

        result = this.executeSearch("admin", null);
        assertEquals(Action.SUCCESS, result);
        userFinderAction = (UserProfileFinderAction) this.getAction();
        users = userFinderAction.getSearchResult();
        assertTrue(users.size() >= 8);
    }

    private String executeList(String currentUser) throws Throwable {
        this.setUserOnSession(currentUser);
        this.initAction("/do/User", "list");
        return this.executeAction();
    }

    private String executeSearch(String currentUser, String text) throws Throwable {
        this.setUserOnSession(currentUser);
        this.initAction("/do/User", "search");
        if (null != text) {
            this.addParameter("username", text);
        }
        return this.executeAction();
    }

}
