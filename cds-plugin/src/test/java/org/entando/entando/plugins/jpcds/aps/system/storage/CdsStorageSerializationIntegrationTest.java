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
package org.entando.entando.plugins.jpcds.aps.system.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(MockitoExtension.class)
class CdsStorageSerializationIntegrationTest {

    private CdsStorageManager cdsStorageManager;
    
    @BeforeEach
    public void init() {
        CdsRemoteCaller cdsRemoteCaller = new CdsRemoteCaller(Mockito.mock(RestTemplate.class), 
                Mockito.mock(RestTemplate.class), Mockito.mock(CdsConfiguration.class));
        cdsStorageManager = new CdsStorageManager(cdsRemoteCaller, Mockito.mock(ITenantManager.class), Mockito.mock(CdsConfiguration.class));
    }
    
    @Test
    void testSerializeStorageManager() throws Exception {
        Assertions.assertNotNull(this.cdsStorageManager.getCaller());
        Assertions.assertNotNull(this.cdsStorageManager.getTenantManager());
        Assertions.assertNotNull(this.cdsStorageManager.getConfiguration());
        CdsStorageManager badProcessed = testSerializeAndDeserializeNullApplicationContext(cdsStorageManager);
        Assertions.assertNull(badProcessed.getCaller());
        Assertions.assertNull(badProcessed.getTenantManager());
        Assertions.assertNull(badProcessed.getConfiguration());
        
        CdsStorageManager processed = testSerializeAndDeserializeMockApplicationContext(cdsStorageManager);
        Assertions.assertNotNull(processed.getCaller());
        Assertions.assertNotNull(processed.getTenantManager());
        Assertions.assertNotNull(processed.getConfiguration());
    }
    
    private <T> T testSerializeAndDeserializeNullApplicationContext(T object) throws Exception {
        try (MockedStatic<ContextLoader> contextLoader = Mockito.mockStatic(ContextLoader.class)) {
            contextLoader.when(() -> ContextLoader.getCurrentWebApplicationContext()).thenReturn(null);
            return testSerializeAndDeserialize(object);
        }
    }
    
    private <T> T testSerializeAndDeserializeMockApplicationContext(T object) throws Exception {
        try (MockedStatic<ContextLoader> contextLoader = Mockito.mockStatic(ContextLoader.class)) {
            WebApplicationContext ctx = Mockito.mock(WebApplicationContext.class);
            contextLoader.when(() -> ContextLoader.getCurrentWebApplicationContext()).thenReturn(ctx);
            Mockito.when(ctx.getBean(ITenantManager.class)).thenReturn(Mockito.mock(ITenantManager.class));
            Mockito.when(ctx.getBean(CdsConfiguration.class)).thenReturn(Mockito.mock(CdsConfiguration.class));
            Mockito.when(ctx.getBean(CdsRemoteCaller.class)).thenReturn(Mockito.mock(CdsRemoteCaller.class));
            return testSerializeAndDeserialize(object);
        }
    }
    
    private <T> T testSerializeAndDeserialize(T object) throws Exception {
        byte[] data;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream(); ObjectOutputStream objectOutputStream = new ObjectOutputStream(os)) {
            objectOutputStream.writeObject(object);
            data = os.toByteArray();
        }
        try (ByteArrayInputStream is = new ByteArrayInputStream(data); ObjectInputStream objectInputStream = new ObjectInputStream(is)) {
            return (T) objectInputStream.readObject();
        }
    }
    
}
