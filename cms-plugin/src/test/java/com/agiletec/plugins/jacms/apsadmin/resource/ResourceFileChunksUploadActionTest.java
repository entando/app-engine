package com.agiletec.plugins.jacms.apsadmin.resource;

import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.apsadmin.system.BaseAction;
import com.agiletec.plugins.jacms.aps.system.services.resource.IResourceManager;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ResourceInterface;
import org.apache.struts2.action.Action;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceFileChunksUploadActionTest {

    @Mock
    private ConfigInterface configManager;
    @Mock
    private IResourceManager resourceManager;

    @InjectMocks
    @Spy
    private ResourceFileChunksUploadAction action;

    @Test
    void validationShouldFailIfResourceTypeCodeIsNull() {
        Mockito.doReturn("error").when(action).getText("error.resource.filename.uploadError", new String[]{null});
        action.validate();
        Assertions.assertFalse(action.isValid());
    }

    @Test
    void validationShouldFailIfResourceTypeCodeIsNotSupported() {
        action.setFileName("fileName");
        action.setResourceTypeCode("Custom");
        Mockito.doReturn("error").when(action).getText("error.resource.filename.uploadError", new String[]{"fileName"});
        action.validate();
        Assertions.assertFalse(action.isValid());
    }

    @Test
    void validationShouldFailIfTotalSizeIsGreaterThanMaxSize() {
        action.setFileName("fileName");
        action.setResourceTypeCode("Attach");
        action.setTotalSize(50000000);
        Mockito.doReturn("error").when(action).getText("error.resource.file.tooBig", new String[]{"fileName"});
        action.validate();
        Assertions.assertFalse(action.isValid());
    }

    @Test
    void validationShouldFailIfFileExtensionIsNotSupported() {
        mockAllowedFileTypes("Image", "jpg", "png");
        action.setFileName("fileName.exe");
        action.setResourceTypeCode("Image");
        Mockito.doReturn("error").when(action).getText("error.resource.file.wrongFormat", new String[]{"fileName.exe"});
        action.validate();
        Assertions.assertFalse(action.isValid());
    }

    @Test
    void uploadShouldFailIfActionIsNotValid() {
        Assertions.assertEquals(Action.INPUT, action.upload());
    }

    @Test
    void uploadShouldFailInCaseOfIOError() {
        try (MockedStatic<FileUtils> fileUtils = Mockito.mockStatic(FileUtils.class)) {
            fileUtils.when(() -> FileUtils.readFileToByteArray(Mockito.any())).thenThrow(IOException.class);
            File file = Mockito.mock(File.class);
            action.setFileName("file.pdf");
            action.setResourceTypeCode("Attach");
            action.setTotalSize(1000);
            action.setStart(100l);
            action.setEnd(1000l);
            action.setFileUpload(file);
            mockAllowedFileTypes("Attach", "txt", "pdf");
            action.validate();
            Mockito.doReturn("error").when(action)
                    .getText("error.resource.filename.uploadError", new String[]{"file.pdf"});
            Mockito.doNothing().when(action).addActionError("error");
            Assertions.assertTrue(action.isValid());
            Assertions.assertEquals(BaseAction.FAILURE, action.upload());
        }
    }

    @Test
    void uploadShouldFailIfTotalSizeWasForged() throws IOException {
        try (MockedStatic<FileUtils> fileUtils = Mockito.mockStatic(FileUtils.class)) {
            fileUtils.when(() -> FileUtils.readFileToByteArray(Mockito.any())).thenReturn(new byte[50000000]);
            File file = Mockito.spy(Files.createTempFile(null, null).toFile());
            action.setFileName("file.pdf");
            action.setResourceTypeCode("Attach");
            action.setTotalSize(1000);
            action.setStart(100l);
            action.setEnd(1000l);
            action.setFileUpload(file);
            mockAllowedFileTypes("Attach", "txt", "pdf");
            action.validate();
            Mockito.doReturn("error").when(action)
                    .getText("error.resource.file.tooBig", new String[]{"file.pdf"});
            Mockito.doNothing().when(action).addActionError("error");
            Assertions.assertTrue(action.isValid());
            Assertions.assertEquals(Action.INPUT, action.upload());
        }
    }

    private void mockAllowedFileTypes(String typeCode, String... allowedExtension) {
        ResourceInterface resourceInterface = Mockito.mock(ResourceInterface.class);
        Mockito.when(resourceInterface.getAllowedFileTypes()).thenReturn(allowedExtension);
        Mockito.when(resourceManager.createResourceType(typeCode)).thenReturn(resourceInterface);
        action.setResourceManager(resourceManager);
    }
}
