/*
 * Copyright 2023-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.system.services.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.entando.entando.aps.system.services.IComponentDto;
import org.entando.entando.aps.system.services.storage.model.BasicFileAttributeViewDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileBrowserServiceTest {
    
    @Mock
    private IStorageManager storageManager;

    @InjectMocks
    private FileBrowserService fileBrowserService;
    
    @Test
    void shouldFindComponentDto() throws Exception {
        Mockito.lenient().when(this.storageManager.getAttributes("/path/myFile", false))
                .thenReturn(Mockito.mock(BasicFileAttributeView.class));
        IComponentDto dto = this.fileBrowserService.getComponentDto("/path/myFile");
        assertThat(dto).isNotNull()
                .isInstanceOf(BasicFileAttributeViewDto.class);
    }
    
}
