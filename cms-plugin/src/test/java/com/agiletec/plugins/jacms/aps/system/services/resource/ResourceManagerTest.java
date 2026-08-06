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
package com.agiletec.plugins.jacms.aps.system.services.resource;

import com.agiletec.plugins.jacms.aps.system.services.resource.cache.IResourceManagerCacheWrapper;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.AttachResource;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.BaseResourceDataBean;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ImageResource;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ResourceInterface;
import java.util.HashMap;
import java.util.Map;
import org.entando.entando.ent.exception.EntException;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceManagerTest {

    @Mock
    private IResourceManagerCacheWrapper cacheWrapper;

    @Mock
    private IResourceDAO resourceDAO;

    @InjectMocks
    private ResourceManager resourceManager;

    @BeforeEach
    void setUp() {
        AttachResource mockAttachResource = mock(AttachResource.class);
        lenient().when(mockAttachResource.getType()).thenReturn("Attach");
        lenient().when(mockAttachResource.getResourcePrototype()).thenReturn(mockAttachResource);
        ImageResource mockImageResource = mock(ImageResource.class);
        lenient().when(mockImageResource.getResourcePrototype()).thenReturn(mockImageResource);
        lenient().when(mockImageResource.getType()).thenReturn("Image");
        Map<String, ResourceInterface> types = new HashMap<>();
        types.put("Image", mockImageResource);
        types.put("Attach", mockAttachResource);
        this.resourceManager.setResourceTypes(types);
    }

    @Test
    void status_should_be_ready_on_init() {
        when(cacheWrapper.getStatus()).thenReturn(IResourceManager.STATUS_READY);
        int status = this.resourceManager.getStatus();
        assertThat(status, is(IResourceManager.STATUS_READY));
    }

    @Test
    void createResourceType() {
        ResourceInterface type = this.resourceManager.createResourceType("Image");
        Assertions.assertNotNull(type);
        Assertions.assertEquals("Image", type.getType());
    }

    @Test
    void updateResourceShouldThrowWhenResourceNotFound() {
        BaseResourceDataBean bean = new BaseResourceDataBean();
        bean.setResourceId("missing-id");
        Assertions.assertThrows(EntException.class, () -> this.resourceManager.updateResource(bean));
    }

    @Test
    void refreshMasterFileNamesShouldNotThrowWhenResourceNotFound() {
        Assertions.assertDoesNotThrow(() -> this.resourceManager.refreshMasterFileNames("missing-id"));
    }

    @Test
    void refreshResourceInstancesShouldNotThrowWhenResourceNotFound() {
        Assertions.assertDoesNotThrow(() -> this.resourceManager.refreshResourceInstances("missing-id"));
    }

}
