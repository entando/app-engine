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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.agiletec.aps.system.SystemConstants;

import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.User;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.aps.util.DateConverter;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import com.agiletec.apsadmin.system.ApsAdminSystemConstants;
import org.apache.struts2.action.Action;
import org.entando.entando.aps.system.services.userprofile.IUserProfileManager;
import org.entando.entando.aps.system.services.userprofile.model.IUserProfile;

import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author E.Santoboni, E.Mezzano
 */
class TestUserAction extends ApsAdminBaseTestCase {

    private IUserManager userManager;
    private IUserProfileManager userProfileManager;

    @BeforeEach
    private void init() {
        this.userManager = (IUserManager) this.getService(SystemConstants.USER_MANAGER);
        this.userProfileManager = (IUserProfileManager) this.getService(SystemConstants.USER_PROFILE_MANAGER);
    }

    @Test
    void testNew() throws Throwable {
        String result = this.executeNew("developersConf");
        assertEquals("apslogin", result);

        result = this.executeNew("admin");
        assertEquals(Action.SUCCESS, result);
        UserAction action = (UserAction) this.getAction();
        assertNull(action.getUsername());
    }

    @Test
    void testFailureEdit() throws Throwable {
        String result = this.executeEdit("admin", "not-exists");
        assertEquals("userList", result);
        Collection<String> actionErrors = this.getAction().getActionErrors();
        assertEquals(1, actionErrors.size());
        
        result = this.executeEdit("admin", "admin");
        assertEquals(Action.SUCCESS, result);
    }

    @Test
    void testEdit() throws Throwable {
        String result = this.executeEdit("admin", "mainEditor");
        assertEquals(Action.SUCCESS, result);
        UserAction action = (UserAction) this.getAction();
        assertNotNull(action.getUsername());
    }

    @Test
    void testAddNew_1() throws Throwable {
        String username = "usernameForTest";
        String password = "password";
        try {
            this.setUserOnSession("admin");
            this.initAction("/do/User", "save");
            this.addParameter("strutsAction", String.valueOf(ApsAdminSystemConstants.ADD));
            this.addParameter("username", username);
            this.addParameter("password", password);
            this.addParameter("passwordConfirm", password);
            this.addParameter("active", "true");
            this.addParameter("profileTypeCode", SystemConstants.DEFAULT_PROFILE_TYPE_CODE);
            String result = this.executeAction();
            assertEquals(Action.SUCCESS, result);

            UserDetails extracted = this.userManager.getUser(username, password);
            assertNotNull(extracted);
            assertFalse(extracted.isDisabled());
        } catch (Throwable t) {
            throw t;
        } finally {
            this.userManager.removeUser(username);
        }
    }

    @Test
    void testAddGuestUsername() throws Throwable {
        String username = "guest";
        String password = "password";
        try {
            this.setUserOnSession("admin");
            this.initAction("/do/User", "save");
            this.addParameter("strutsAction", String.valueOf(ApsAdminSystemConstants.ADD));
            this.addParameter("username", username);
            this.addParameter("password", password);
            this.addParameter("passwordConfirm", password);
            this.addParameter("active", "true");
            this.addParameter("profileTypeCode", SystemConstants.DEFAULT_PROFILE_TYPE_CODE);
            String result = this.executeAction();
            assertEquals(Action.INPUT, result);
            Map<String, List<String>> fieldErrors = this.getAction().getFieldErrors();
            assertEquals(1, fieldErrors.size());
            List<String> errors = fieldErrors.get("username");
            assertEquals(1, errors.size());
        } catch (Throwable t) {
            throw t;
        }
    }

    @Test
    void testAddNew_2() throws Throwable {
        String username = "user.name_for_test"; // the dot '.' is accepted in the username as well as the underscore '_'
        String password = "password.for_test"; // the dot '.' is accepted in the password as well as the underscore '_'
        try {
            this.setUserOnSession("admin");
            this.initAction("/do/User", "save");
            this.addParameter("strutsAction", String.valueOf(ApsAdminSystemConstants.ADD));
            this.addParameter("username", username);
            this.addParameter("password", password);
            this.addParameter("passwordConfirm", password);
            this.addParameter("active", "true");
            this.addParameter("profileTypeCode", SystemConstants.DEFAULT_PROFILE_TYPE_CODE);
            String result = this.executeAction();
            assertEquals(Action.SUCCESS, result);

            UserDetails extracted = this.userManager.getUser(username, password);
            assertNotNull(extracted);
            assertFalse(extracted.isDisabled());
        } catch (Throwable t) {
            throw t;
        } finally {
            this.userManager.removeUser(username);
            assertNull(this.userManager.getUser(username));
        }
    }

    @Test
    void testAddEditDelete() throws Throwable {
        String username = "username";
        String password = "password";
        String newPassword = "pluto1234";
        try {
            this.addUser(username, password);
            this.setUserOnSession("admin");
            this.initAction("/do/User", "save");
            this.addParameter("strutsAction", String.valueOf(ApsAdminSystemConstants.EDIT));
            this.addParameter("username", username);
            this.addParameter("password", newPassword);
            this.addParameter("passwordConfirm", newPassword);
            this.addParameter("active", "true");
            String result = this.executeAction();
            assertEquals(Action.SUCCESS, result);

            UserDetails extracted = this.userManager.getUser(username, newPassword);
            assertNotNull(extracted);
            assertFalse(extracted.isDisabled());

            this.initAction("/do/User", "delete");
            this.addParameter("username", username);
            result = this.executeAction();
            assertEquals(Action.SUCCESS, result);
            extracted = this.userManager.getUser(username);
            assertNull(extracted);
        } catch (Throwable t) {
            this.userManager.removeUser(username);
            throw t;
        }
    }

    @Test
    void testFailureDisableAdminUser() throws Throwable {
        String username = SystemConstants.ADMIN_USER_NAME;
        UserDetails adminUser = this.userManager.getUser(username);
        try {
            this.setUserOnSession("admin");
            this.initAction("/do/User", "save");
            this.addParameter("strutsAction", String.valueOf(ApsAdminSystemConstants.EDIT));
            this.addParameter("username", username);
            this.addParameter("active", "false");
            String result = this.executeAction();
            assertEquals(Action.INPUT, result);
            Map<String, List<String>> fieldErrors = this.getAction().getFieldErrors();
            assertEquals(1, fieldErrors.size());
            List<String> errors = fieldErrors.get("active");
            assertEquals(1, errors.size());
        } catch (Throwable t) {
            this.userManager.updateUser(adminUser);
            throw t;
        }
    }

    @Test
    void testSaveNewFailure() throws Throwable {
        UserDetails oldUser = this.userManager.getUser("admin");
        try {
            // permessi non disponibili
            String result = this.executeSaveNew("developersConf", "username", "password", "password");
            assertEquals("apslogin", result);

            // username già presente
            result = this.executeSaveNew("admin", "admin", "password", "password");
            this.verifyErrors(result, 1, "username", 1);

            // username non valorizzato
            result = this.executeSaveNew("admin", "", "password", "password");
            this.verifyErrors(result, 1, "username", 1);

            // username too short
            result = this.executeSaveNew("admin", "usr", "password", "password");
            this.verifyErrors(result, 1, "username", 1);

            // username troppo lunga
            result = this.executeSaveNew("admin", "usernameDecisamenteTroppoLunga", "password", "password");
            this.verifyErrors(result, 1, "username", 1);

            // username con caratteri non consentiti
            result = this.executeSaveNew("admin", "user name", "password", "password");
            this.verifyErrors(result, 1, "username", 1);

            // username con caratteri non consentiti - 2
            result = this.executeSaveNew("admin", "user\name", "password", "password");
            this.verifyErrors(result, 1, "username", 1);

            // password troppo corta
            result = this.executeSaveNew("admin", "test", "p", "p");
            this.verifyErrors(result, 1, "password", 1);

            // password troppo lunga
            result = this.executeSaveNew("admin", "test", "passwordDecisamenteTroppoLunga", "passwordDecisamenteTroppoLunga");
            this.verifyErrors(result, 1, "password", 1);

            // password con caratteri non consentiti
            result = this.executeSaveNew("admin", "test", "&345bnr67fg$", "&345bnr67fg$");
            this.verifyErrors(result, 1, "password", 1);
        } catch (RuntimeException e) {
            this.userManager.updateUser(oldUser);
            this.userManager.changePassword(oldUser.getUsername(), oldUser.getUsername());
            throw e;
        }
    }

    private String executeSaveNew(String currentUser, String username, String password, String passwordConfirm) throws Throwable {
        this.setUserOnSession(currentUser);
        this.initAction("/do/User", "save");
        this.addParameter("strutsAction", String.valueOf(ApsAdminSystemConstants.ADD));
        this.addParameter("username", username);
        this.addParameter("password", password);
        this.addParameter("passwordConfirm", passwordConfirm);
        this.addParameter("profileTypeCode", SystemConstants.DEFAULT_PROFILE_TYPE_CODE);
        return this.executeAction();
    }

    @Test
    void testSaveEditFailure() throws Throwable {
        this.setUserOnSession("admin");
        String usernameForTest = "testUserName";
        this.addUser(usernameForTest, "password");
        try {
            // password non valorizzata
            String result = this.executeSaveEdit("admin", usernameForTest, "", "password");
            this.verifyErrors(result, 1, "password", 1);

            // passwordConfirm non valorizzata
            result = this.executeSaveEdit("admin", usernameForTest, "password", "");
            this.verifyErrors(result, 1, "password", 1);

            // password e passwordConfirm non identiche
            result = this.executeSaveEdit("admin", usernameForTest, "password", "passwordConfirm");
            this.verifyErrors(result, 1, "password", 1);

            // password troppo lunga
            result = this.executeSaveEdit("admin", usernameForTest, "passwordDecisamenteTroppoLunga", "passwordDecisamenteTroppoLunga");
            this.verifyErrors(result, 1, "password", 1);

            // password con caratteri speciali
            result = this.executeSaveEdit("admin", usernameForTest, "pass&word", "pass&word");
            this.verifyErrors(result, 1, "password", 1);
        } catch (Throwable t) {
            throw t;
        } finally {
            this.userManager.removeUser(usernameForTest);
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
    void testTrash() throws Throwable {
        String username = "username";
        String password = "password";
        try {
            this.addUser(username, password);

            String result = this.executeTrash("admin", username);
            assertEquals(Action.SUCCESS, result);
            assertNotNull(this.userManager.getUser(username));
        } catch (Throwable t) {
            throw t;
        } finally {
            this.userManager.removeUser(username);
        }
    }

    @Test
    void testFailureTrash() throws Throwable {
        String result = this.executeTrash("developersConf", "editor");
        assertEquals("apslogin", result);

        result = this.executeTrash("admin", "admin");
        assertEquals("userList", result);
        Collection<String> actionErrors = this.getAction().getActionErrors();
        assertEquals(1, actionErrors.size());
    }

    @Test
    void testDelete() throws Throwable {
        String username = "user.name_to_delete";
        String password = "password";
        try {
            this.addUser(username, password);
            String result = this.executeDelete("admin", username);
            assertEquals(Action.SUCCESS, result);
            assertNull(this.userManager.getUser(username));
        } catch (Throwable t) {
            throw t;
        } finally {
            this.userManager.removeUser(username);
        }
    }

    @Test
    void testDeleteUserWithProfile() throws Throwable {
        String username = "test_profile";
        try {
            IUserProfile profile = this.userProfileManager.getDefaultProfileType();
            profile.setId("test_profile");
            User user = new User();
            user.setUsername(username);
            user.setPassword(username);
            user.setProfile(profile);
            this.userManager.addUser(user);
            assertNotNull(userManager.getUser(username));
            assertNotNull(userProfileManager.getProfile(username));

            String result = this.executeDelete("admin", username);
            assertEquals(Action.SUCCESS, result);

            assertNull(userManager.getUser(username));
            assertNull(userProfileManager.getProfile(username));

            userProfileManager.addProfile(username, profile);
            assertNull(userManager.getUser(username));
            assertNotNull(userProfileManager.getProfile(username));

            result = this.executeDelete("admin", username);
            assertEquals(Action.SUCCESS, result);

            assertNull(userManager.getUser(username));
            assertNull(userProfileManager.getProfile(username));
        } catch (Exception e) {
            throw e;
        } finally {
            this.userManager.removeUser(username);
            assertNull(userManager.getUser(username));
            assertNull(userProfileManager.getProfile(username));
        }
    }

    @Test
    void testFailureDelete() throws Throwable {
        String result = this.executeDelete("developersConf", "editor");
        assertEquals("apslogin", result);

        result = this.executeDelete("admin", "admin");
        assertEquals("userList", result);
        Collection<String> actionErrors = this.getAction().getActionErrors();
        assertEquals(1, actionErrors.size());

        assertNull(this.userManager.getUser("not_exists"));
        result = this.executeDelete("admin", "not_exists");
        assertEquals("userList", result);
        actionErrors = this.getAction().getActionErrors();
        assertEquals(1, actionErrors.size());
    }

    @Test
    void testResetUser() throws Throwable {
        String username = "username";
        String password = "password";
        String datePattern = "ddMMyyyy";
        String today = DateConverter.getFormattedDate(new Date(), datePattern);
        try {
            this.addUser(username, password);
            User extracted = (User) this.userManager.getUser(username, password);
            assertEquals(today, DateConverter.getFormattedDate(extracted.getCreationDate(), datePattern));
            assertNull(extracted.getLastAccess());
            assertNull(extracted.getLastPasswordChange());

            this.setUserOnSession("admin");
            this.initAction("/do/User", "save");
            this.addParameter("strutsAction", String.valueOf(ApsAdminSystemConstants.EDIT));
            this.addParameter("username", username);
            this.addParameter("reset", "true");
            String result = this.executeAction();
            assertEquals(Action.SUCCESS, result);

            extracted = (User) this.userManager.getUser(username, password);
            assertNotNull(extracted);
            assertEquals(today, DateConverter.getFormattedDate(extracted.getCreationDate(), datePattern));
            assertEquals(today, DateConverter.getFormattedDate(extracted.getLastAccess(), datePattern));
            assertEquals(today, DateConverter.getFormattedDate(extracted.getLastPasswordChange(), datePattern));
        } catch (Throwable t) {
            throw t;
        } finally {
            this.userManager.removeUser(username);
        }
    }

    private String executeNew(String currentUser) throws Throwable {
        this.setUserOnSession(currentUser);
        this.initAction("/do/User", "new");
        return this.executeAction();
    }

    private String executeEdit(String currentUser, String username) throws Throwable {
        this.setUserOnSession(currentUser);
        this.initAction("/do/User", "edit");
        this.addParameter("username", username);
        return this.executeAction();
    }

    private String executeTrash(String currentUser, String username) throws Throwable {
        this.setUserOnSession(currentUser);
        this.initAction("/do/User", "trash");
        this.addParameter("username", username);
        return this.executeAction();
    }

    private String executeDelete(String currentUser, String username) throws Throwable {
        this.setUserOnSession(currentUser);
        this.initAction("/do/User", "delete");
        this.addParameter("username", username);
        return this.executeAction();
    }

    private String executeSaveEdit(String currentUser, String username, String password, String passwordConfirm) throws Throwable {
        this.setUserOnSession(currentUser);
        this.initAction("/do/User", "save");
        this.addParameter("strutsAction", String.valueOf(ApsAdminSystemConstants.EDIT));
        this.addParameter("username", username);
        this.addParameter("password", password);
        this.addParameter("passwordConfirm", passwordConfirm);
        return this.executeAction();
    }

    private void addUser(String username, String password) throws EntException {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        this.userManager.addUser(user);
    }

}
