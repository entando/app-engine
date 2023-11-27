package com.agiletec.plugins.jacms.aps.system.services.resource.model;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.entando.entando.aps.system.services.storage.IStorageManager;
import org.entando.entando.ent.exception.EntResourceNotFoundException;
import org.entando.entando.ent.exception.EntResourceNotFoundRuntimeException;
import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AttachResourceTest {

    @Test
    void getResourceStreamShouldFireEntCDSResourceNotFoundExceptionIfAnErrorOccurInCDSManager() throws EntException {
        AttachResource attachResource = new AttachResource();
        ResourceInstance resourceInstance = mock(ResourceInstance.class);
        when(resourceInstance.getFileName()).thenReturn("");
        attachResource.addInstance(resourceInstance);
        IStorageManager storageManager = mock(IStorageManager.class);
        attachResource.setStorageManager(storageManager);
        when(storageManager.getStream(any(), anyBoolean())).thenThrow(EntResourceNotFoundException.class);
        Assertions.assertThrows(EntResourceNotFoundRuntimeException.class,
                () -> attachResource.getResourceStream(1, ""));
    }

    @Test
    void getResourceStreamShouldFireRuntimeExceptionIfAnErrorOccurInAnyManagerButCDSManager() throws EntException {
        AttachResource attachResource = new AttachResource();
        ResourceInstance resourceInstance = mock(ResourceInstance.class);
        when(resourceInstance.getFileName()).thenReturn("");
        attachResource.addInstance(resourceInstance);
        IStorageManager storageManager = mock(IStorageManager.class);
        attachResource.setStorageManager(storageManager);
        when(storageManager.getStream(any(), anyBoolean())).thenThrow(EntException.class);
        Assertions.assertThrows(RuntimeException.class,
                () -> attachResource.getResourceStream(1, ""));
    }

}
