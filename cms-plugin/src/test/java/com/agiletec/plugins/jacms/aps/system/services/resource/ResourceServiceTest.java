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
package com.agiletec.plugins.jacms.aps.system.services.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.agiletec.plugins.jacms.aps.system.services.resource.model.ImageResource;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ResourceInstance;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ResourceInterface;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ResourceDto;
import java.util.ArrayList;
import java.util.List;
import org.entando.entando.aps.system.services.IComponentDto;
import org.entando.entando.web.common.model.PagedMetadata;
import org.entando.entando.web.common.model.RestListRequest;
import org.entando.entando.web.component.ComponentUsageEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {
    
    @Mock
    private IResourceManager resourceManager;
    
    @InjectMocks
    private ResourceService resourceService;
    
    @BeforeEach
    void setUp() throws Exception {
        this.resourceService.setUp();
    }
    
    @Test
    void shouldFindComponentDto() throws Exception {
        ResourceInterface resource = Mockito.mock(ImageResource.class);
        Mockito.lenient().when(resource.getType()).thenReturn("Image");
        Mockito.lenient().when(resource.getDefaultInstance()).thenReturn(Mockito.mock(ResourceInstance.class));
        Mockito.lenient().when(this.resourceManager.loadResource("id")).thenReturn(resource);
        IComponentDto dto = this.resourceService.getComponentDto("id");
        assertThat(dto).isNotNull()
                .isInstanceOf(ResourceDto.class);
    }
    
    @Test
    void shouldFindUtilizers() {
        ResourceServiceUtilizer utilizer = Mockito.mock(ResourceServiceUtilizer.class);
        List<IComponentDto> components = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            IComponentDto dto = Mockito.mock(IComponentDto.class);
            components.add(dto);
        }
        when(utilizer.getResourceUtilizer(Mockito.anyString())).thenReturn(components);
        resourceService.setResourceServiceUtilizers(List.of(utilizer));
        
        PagedMetadata<ComponentUsageEntity> result = resourceService.getComponentUsageDetails("test", new RestListRequest());
        Assertions.assertEquals(9, result.getBody().size());
        
        int usage = resourceService.getComponentUsage("test");
        Assertions.assertEquals(result.getBody().size(), usage);
    }
    
}
