/*
 * Copyright 2024-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.apsadmin.filebrowser;

import org.entando.entando.aps.system.services.storage.IStorageManager;
import org.entando.entando.aps.system.services.storage.model.DiskInfoDto;
import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith({MockitoExtension.class,SystemStubsExtension.class})
class FileBrowserActionTest {
    
    @SystemStub
    private static EnvironmentVariables environmentVariables;
    
    @Mock
    private IStorageManager storageManager;
    
    @InjectMocks
    private FileBrowserAction fileBrowserAction;
    
    @Test
    void shouldReturnNullDiskInfo() throws Exception {
        environmentVariables.set("CDS_ENABLED", "false");
        DiskInfoDto dto = fileBrowserAction.getDiskInfo();
        Assertions.assertNull(dto);
        Mockito.verifyNoInteractions(this.storageManager);
    }
    
    @Test
    void shouldReturnDiskInfo() throws Exception {
        environmentVariables.set("CDS_ENABLED", "true");
        Mockito.when(this.storageManager.getDiskInfo()).thenReturn(Mockito.mock(DiskInfoDto.class));
        DiskInfoDto dto = fileBrowserAction.getDiskInfo();
        Assertions.assertNotNull(dto);
    }
    
    @Test
    void shouldReturnNulInvokingGetDiskInfoWithServiceError() throws Exception {
        environmentVariables.set("CDS_ENABLED", "true");
        Mockito.when(this.storageManager.getDiskInfo()).thenThrow(new EntException("Error"));
        DiskInfoDto dto = fileBrowserAction.getDiskInfo();
        Assertions.assertNull(dto);
    }
    
}
