/*
 * Copyright 2024-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package com.agiletec.plugins.jacms.apsadmin.content;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.group.IGroupManager;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.apsadmin.system.BaseAction;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.apsadmin.content.helper.IContentActionHelper;
import com.opensymphony.xwork2.ActionSupport;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.entando.entando.aps.system.services.userpreferences.IUserPreferencesManager;
import org.entando.entando.aps.system.services.userpreferences.UserPreferences;
import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntroNewContentActionTest {
    
    @Mock
    private UserDetails currentUser;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession session;
    @Mock
    private ILangManager langManager;
    @Mock
    private IGroupManager groupManager;
    @Mock
    private IUserPreferencesManager userPreferencesManager;
    @Mock
    private IContentManager contentManager;
    @Mock
	private IContentActionHelper contentActionHelper;
    @Mock
    private IAuthorizationManager authorizationManager;

    @Spy
    @InjectMocks
    private IntroNewContentAction action;
    
    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
        action.setServletRequest(this.request);
        action.setGroupManager(this.groupManager);
        action.setLangManager(this.langManager);
        action.setContentManager(this.contentManager);
        action.setContentActionHelper(this.contentActionHelper);
        action.setLangManager(this.langManager);
        action.setUserPreferencesManager(this.userPreferencesManager);
        action.setAuthorizationManager(this.authorizationManager);
        Mockito.when(this.request.getSession()).thenReturn(this.session);
        Mockito.when(this.request.getSession().getAttribute(SystemConstants.SESSIONPARAM_CURRENT_USER)).thenReturn(this.currentUser);
        Mockito.when(this.currentUser.getUsername()).thenReturn("username");
    }
    
    @Test
    void showldReturnErrorWithMissingContentType() {
        Mockito.doReturn("invalid content type").when(action).getText(Mockito.anyString());
        action.setContentTypeCode("XXX");
        Mockito.when(this.contentManager.createContentType("XXX")).thenReturn(null);
        String result = this.action.createNew();
        Assertions.assertEquals(ActionSupport.INPUT, result);
        Assertions.assertEquals(1, action.getFieldErrors().size());
        Assertions.assertEquals(1, action.getFieldErrors().get("contentTypeCode").size());
        Assertions.assertEquals("invalid content type", action.getFieldErrors().get("contentTypeCode").get(0));
    }
    
    @Test
    void showldExecuteCreateNewContentWithoutPreferences() {
        action.setContentTypeCode("XXX");
        Content prototype = new Content();
        Mockito.when(this.contentManager.createContentType("XXX")).thenReturn(prototype);
        String result = this.action.createNew();
        Assertions.assertEquals(ActionSupport.SUCCESS, result);
        Assertions.assertEquals(0, action.getFieldErrors().size());
        Assertions.assertNull(prototype.getMainGroup());
    }
    
    @Test
    void showldExecuteCreateNewContentWithPreferences() throws Exception {
        action.setContentTypeCode("XXX");
        Content prototype = new Content();
        Mockito.when(this.contentManager.createContentType("XXX")).thenReturn(prototype);
        UserPreferences userPreferences = new UserPreferences();
        userPreferences.setDefaultContentOwnerGroup("main_group");
        userPreferences.setDefaultContentJoinGroups("join1;join2");
        Mockito.when(this.userPreferencesManager.getUserPreferences(Mockito.anyString())).thenReturn(userPreferences);
        Mockito.when(this.groupManager.getGroup(Mockito.anyString())).thenReturn(Mockito.mock(Group.class));
        String result = this.action.createNew();
        Assertions.assertEquals(ActionSupport.SUCCESS, result);
        Assertions.assertEquals(0, action.getFieldErrors().size());
        Mockito.verify(this.session, Mockito.times(1))
                .setAttribute(ContentActionConstants.SESSION_PARAM_NAME_CURRENT_CONTENT_GROUP, "main_group");
        Assertions.assertEquals(2, prototype.getGroups().size());
        Assertions.assertTrue(prototype.getGroups().contains("join1"));
        Assertions.assertTrue(prototype.getGroups().contains("join2"));
    }
    
    @Test
    void showldExecuteCreateNewContentWithEmptyPreferences() throws Exception {
        action.setContentTypeCode("XXX");
        Content prototype = new Content();
        Mockito.when(this.contentManager.createContentType("XXX")).thenReturn(prototype);
        Mockito.when(this.userPreferencesManager.getUserPreferences(Mockito.anyString())).thenReturn(new UserPreferences());
        String result = this.action.createNew();
        Assertions.assertEquals(ActionSupport.SUCCESS, result);
        Assertions.assertEquals(0, action.getFieldErrors().size());
        Assertions.assertNull(prototype.getMainGroup());
        Assertions.assertEquals(0, prototype.getGroups().size());
    }
    
    @Test
    void showldExecuteCreateNewContentWithErrorOnPreferences() throws Exception {
        action.setContentTypeCode("XXX");
        Content prototype = new Content();
        Mockito.when(this.contentManager.createContentType("XXX")).thenReturn(prototype);
        Mockito.when(this.userPreferencesManager.getUserPreferences(Mockito.anyString())).thenThrow(new EntException("Error"));
        String result = this.action.createNew();
        Assertions.assertEquals(BaseAction.FAILURE, result);
        Assertions.assertEquals(0, action.getFieldErrors().size());
        Assertions.assertNull(prototype.getMainGroup());
        Assertions.assertEquals(0, prototype.getGroups().size());
    }
    
}
