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
package org.entando.entando.plugins.jacms.aps.system.services.resource;

import static org.assertj.core.api.Assertions.assertThat;

import com.agiletec.plugins.jacms.aps.system.services.resource.IResourceManager;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ResourceInterface;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ImageResource;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ResourceInstance;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.util.IImageDimensionReader;
import org.entando.entando.aps.system.services.IComponentDto;
import org.entando.entando.plugins.jacms.web.resource.model.AssetDto;
import org.entando.entando.plugins.jacms.web.resource.model.ImageAssetDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourcesServiceTest {
    
    @Mock
    private IResourceManager resourceManager;
    
    @Mock
    private IImageDimensionReader imageDimensionManager;

    @InjectMocks
    private ResourcesService resourcesService;
    
    @Test
    void shouldFindComponentDto() throws Exception {
        ResourceInterface resource = Mockito.mock(ImageResource.class);
        Mockito.when(resource.getType()).thenReturn("Image");
        Mockito.when(resource.getDefaultInstance()).thenReturn(Mockito.mock(ResourceInstance.class));
        Mockito.when(this.resourceManager.loadResource("id")).thenReturn(resource);
        IComponentDto dto = this.resourcesService.getComponentDto("id");
        assertThat(dto).isNotNull()
                .isInstanceOf(AssetDto.class)
                .isInstanceOf(ImageAssetDto.class);
    }
    
}
