/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.agiletec.plugins.jacms.apsadmin.resource;

import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.group.IGroupManager;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;
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
public class MultipleResourceActionTest {
    
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
    
    
    /*
    protected void fetchFileDescriptions() {
        fileDescriptions = fetchFields(FILE_DESCR_FIELD);
        logger.debug("fetchFileDescriptions {}", fileDescriptions);
    }

    protected void fetchFileUploadIDs() {
        fileUploadIDs = fetchFields(FILE_UPLOAD_ID_FIELD);
        logger.debug("fetchFileUploadIDs {}", fileUploadIDs);
    }

    protected void fetchFileUploadContentTypes() {
        fileUploadContentTypes = fetchFields(FILE_CONTENT_TYPE_FIELD);
        logger.debug("fetchFileUploadContentTypes {}", fileUploadContentTypes);
    }

    protected void fetchFileUploadFileNames() {
        fileUploadFileNames = fetchFields(FILE_NAME_FIELD);
        logger.debug("fetchFileUploadFileNames {}", fileUploadContentTypes);

    }
    
    public final static String FILE_DESCR_FIELD = "descr_";
    public final static String FILE_UPLOAD_ID_FIELD = "fileUploadId_";
    public final static String FILE_NAME_FIELD = "fileUploadName_";
    public final static String FILE_CONTENT_TYPE_FIELD = "fileUploadContentType_";
    
    
    */
    
    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        action.setServletRequest(this.request);
        action.setGroupManager(this.groupManager);
        action.setResourceManager(this.resourceManager);
        action.setCategoryManager(this.categoryManager);
        action.setResourceActionHelper(this.resourceActionHelper);
        Mockito.lenient().doReturn("text").when(action).getText(Mockito.anyString());
    }
    
    @Test
    public void validateRightForm() {
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
    public void validateLongDescription() {
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
    public void validateFileAlreadyPresentOnAddExecution() throws Throwable {
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
    public void validateFileAlreadyPresentOnEditExecution() throws Throwable {
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
    public void validateFileNotPresentOnEditExecution() throws Throwable {
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
