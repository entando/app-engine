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
package com.agiletec.plugins.jacms.apsadmin.resource;

import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.group.IGroupManager;
import com.agiletec.apsadmin.system.ApsAdminSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.resource.IResourceManager;
import com.agiletec.plugins.jacms.apsadmin.resource.helper.IResourceActionHelper;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
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
class MultipleResourceActionTest {
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private IGroupManager groupManager;
    
    @Mock
    private IResourceManager resourceManager;
    
	@Mock
    private ICategoryManager categoryManager;
    
	@Mock
    private IResourceActionHelper resourceActionHelper;
    
    @InjectMocks
    @Spy
    private MultipleResourceAction action;
    
    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
        action.setServletRequest(this.request);
        action.setGroupManager(this.groupManager);
        action.setResourceManager(this.resourceManager);
        action.setCategoryManager(this.categoryManager);
        action.setResourceActionHelper(this.resourceActionHelper);
        Mockito.lenient().doReturn("text").when(action).getText(Mockito.anyString());
    }
    
    @Test
    void validateRightForm() {
        action.setMainGroup(Group.FREE_GROUP_NAME);
        action.setStrutsAction(ApsAdminSystemConstants.ADD);
        Map<String, String[]> parameterMap = Map.of(
                MultipleResourceAction.FILE_DESCR_FIELD + "0", new String[]{"Descrizione file"},
                MultipleResourceAction.FILE_UPLOAD_ID_FIELD + "0", new String[]{"0"},
                MultipleResourceAction.FILE_NAME_FIELD + "0", new String[]{"file.txt"},
                MultipleResourceAction.FILE_CONTENT_TYPE_FIELD + "0", new String[]{"text/plain"}
        );
        Mockito.when(this.request.getParameterMap()).thenReturn(parameterMap);
        action.validate();
        Assertions.assertTrue(action.getFieldErrors().isEmpty());
    }
    
    @Test
    void validateLongDescription() {
        action.setMainGroup(Group.FREE_GROUP_NAME);
        action.setStrutsAction(ApsAdminSystemConstants.ADD);
        Map<String, String[]> parameterMap = Map.of(
                MultipleResourceAction.FILE_DESCR_FIELD + "0", new String[]{"Descrizione file"},
                MultipleResourceAction.FILE_UPLOAD_ID_FIELD + "0", new String[]{"0"},
                MultipleResourceAction.FILE_NAME_FIELD + "0", new String[]{"file".repeat(200)+".txt"},
                MultipleResourceAction.FILE_CONTENT_TYPE_FIELD + "0", new String[]{"text/plain"}
        );
        Mockito.doReturn("text").when(action).getText(Mockito.anyString(), Mockito.anyList());
        Mockito.when(this.request.getParameterMap()).thenReturn(parameterMap);
        action.validate();
        Assertions.assertFalse(action.getFieldErrors().isEmpty());
        Assertions.assertEquals(1, action.getFieldErrors().get(MultipleResourceAction.FILE_NAME_FIELD + "0").size());
    }
    
    @Test
    void validateFileAlreadyPresentOnAddExecution() throws Throwable {
        action.setMainGroup(Group.FREE_GROUP_NAME);
        action.setStrutsAction(ApsAdminSystemConstants.ADD);
        Map<String, String[]> parameterMap = Map.of(
                MultipleResourceAction.FILE_DESCR_FIELD + "0", new String[]{"Descrizione file"},
                MultipleResourceAction.FILE_UPLOAD_ID_FIELD + "0", new String[]{"0"},
                MultipleResourceAction.FILE_NAME_FIELD + "0", new String[]{"file.txt"},
                MultipleResourceAction.FILE_CONTENT_TYPE_FIELD + "0", new String[]{"text/plain"}
        );
        Mockito.when(this.resourceManager.searchResourcesId(Mockito.any(), Mockito.anyList())).thenReturn(List.of("21"));
        Mockito.doReturn("text").when(action).getText(Mockito.anyString(), Mockito.any(String[].class));
        Mockito.when(this.request.getParameterMap()).thenReturn(parameterMap);
        action.validate();
        Assertions.assertFalse(action.getFieldErrors().isEmpty());
        Assertions.assertEquals(1, action.getFieldErrors().get(MultipleResourceAction.FILE_NAME_FIELD + "0").size());
    }
    
    @Test
    void validateFileAlreadyPresentOnEditExecution() throws Throwable {
        action.setMainGroup(Group.FREE_GROUP_NAME);
        action.setStrutsAction(ApsAdminSystemConstants.EDIT);
        action.setResourceId("100");
        Map<String, String[]> parameterMap = Map.of(
                MultipleResourceAction.FILE_DESCR_FIELD + "0", new String[]{"Descrizione file"},
                MultipleResourceAction.FILE_UPLOAD_ID_FIELD + "0", new String[]{"0"},
                MultipleResourceAction.FILE_NAME_FIELD + "0", new String[]{"file.txt"},
                MultipleResourceAction.FILE_CONTENT_TYPE_FIELD + "0", new String[]{"text/plain"}
        );
        Mockito.when(this.request.getParameterMap()).thenReturn(parameterMap);
        Mockito.doReturn("text").when(action).getText(Mockito.anyString(), Mockito.any(String[].class));
        
        Mockito.when(this.resourceManager.searchResourcesId(Mockito.any(), Mockito.anyList())).thenReturn(List.of("37"));
        action.validate();
        Assertions.assertFalse(action.getFieldErrors().isEmpty());
        Assertions.assertEquals(1, action.getFieldErrors().get(MultipleResourceAction.FILE_NAME_FIELD + "0").size());
    }
    
    @Test
    void validateFileNotPresentOnEditExecution() throws Throwable {
        action.setMainGroup(Group.FREE_GROUP_NAME);
        action.setStrutsAction(ApsAdminSystemConstants.EDIT);
        action.setResourceId("120");
        Map<String, String[]> parameterMap = Map.of(
                MultipleResourceAction.FILE_DESCR_FIELD + "0", new String[]{"Descrizione file"},
                MultipleResourceAction.FILE_UPLOAD_ID_FIELD + "0", new String[]{"0"},
                MultipleResourceAction.FILE_NAME_FIELD + "0", new String[]{"file.txt"},
                MultipleResourceAction.FILE_CONTENT_TYPE_FIELD + "0", new String[]{"text/plain"}
        );
        Mockito.when(this.request.getParameterMap()).thenReturn(parameterMap);
        Mockito.when(this.resourceManager.searchResourcesId(Mockito.any(), Mockito.anyList())).thenReturn(List.of("120"));
        action.validate();
        Assertions.assertTrue(action.getFieldErrors().isEmpty());
    }
    
}
