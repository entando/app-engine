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
package org.entando.entando.plugins.jpcds.aps.system.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.entando.entando.aps.system.services.tenants.TenantConfig;
import org.entando.entando.plugins.jpcds.aps.system.storage.CdsUrlUtils.EntSubPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CdsUtilsTest {

    @Test
    void shouldExtractPathAndFilename() throws Exception {
        EntSubPath subPath = CdsUrlUtils.extractPathAndFilename("/folder1/folder2/file.txt");
        Assertions.assertEquals("file.txt", subPath.getFileName());
        Assertions.assertEquals("/folder1/folder2", subPath.getPath());

        subPath = CdsUrlUtils.extractPathAndFilename("file.txt");
        Assertions.assertEquals("file.txt", subPath.getFileName());
        Assertions.assertEquals("", subPath.getPath());

        subPath = CdsUrlUtils.extractPathAndFilename("/folder/");
        Assertions.assertEquals("", subPath.getFileName());
        Assertions.assertEquals("/folder", subPath.getPath());

        subPath = CdsUrlUtils.extractPathAndFilename("../../folder/file.txt");
        Assertions.assertEquals("file.txt", subPath.getFileName());
        Assertions.assertEquals("../../folder", subPath.getPath());

        subPath = CdsUrlUtils.extractPathAndFilename("");
        Assertions.assertEquals("", subPath.getFileName());
        Assertions.assertEquals("", subPath.getPath());

        subPath = CdsUrlUtils.extractPathAndFilename(null);
        Assertions.assertEquals("", subPath.getFileName());
        Assertions.assertEquals("", subPath.getPath());
    }
    
    @Test
    void shouldWorkFineWithToString() {
        EntSubPath subPath1 = new EntSubPath("","my-filename");
        EntSubPath subPath2 = new EntSubPath("","my-filename");
        Assertions.assertEquals(subPath1.toString(),subPath2.toString());
    }
    
    @Test
    void shouldExtractRigthSection() {
        Assertions.assertEquals("/protected", CdsUrlUtils.getSection(true, null, null, true));
        Assertions.assertEquals("/protected", CdsUrlUtils.getSection(true, null, Mockito.mock(CdsConfiguration.class), true));
        
        CdsConfiguration cdsConfiguration = new CdsConfiguration();
        cdsConfiguration.setCdsPublicPath("");
        Assertions.assertEquals("", CdsUrlUtils.getSection(false, Optional.ofNullable(null), cdsConfiguration, false));
        
        cdsConfiguration.setCdsPublicPath("/public");
        Assertions.assertEquals("/public", CdsUrlUtils.getSection(false, Optional.ofNullable(null), cdsConfiguration, false));
        
        Map<String, String> tenantParams = new HashMap<>();
        TenantConfig tenantConfig = new TenantConfig(tenantParams);
        Assertions.assertEquals("", CdsUrlUtils.getSection(false, Optional.ofNullable(tenantConfig), cdsConfiguration, true));
        
        tenantParams.put("cdsPublicPath", "/customPath");
        Assertions.assertEquals("/customPath", CdsUrlUtils.getSection(false, Optional.ofNullable(tenantConfig), cdsConfiguration, false));
        Assertions.assertEquals("/protected", CdsUrlUtils.getSection(true, Optional.ofNullable(tenantConfig), cdsConfiguration, true));
    }

}
