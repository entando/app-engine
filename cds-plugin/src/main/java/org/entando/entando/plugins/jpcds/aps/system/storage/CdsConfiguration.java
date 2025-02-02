/*
 * Copyright 2022-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.entando.entando.aps.system.services.storage.CdsActive;

@ToString
@Setter
@Getter
@Component
@CdsActive(true)
public class CdsConfiguration {
    
    @Value("${resourceRootURL}")
    private String baseURL;
    @Value("${resourceDiskRootFolder}")
    private String baseDiskRoot;
    @Value("${protectedResourceDiskRootFolder}")
    private String protectedBaseDiskRoot;
    @Value("${protectedResourceRootURL}")
    private String protectedBaseURL;
    @Value("${CDS_ENABLED:false}")
    private boolean enabled;
    @Value("${CDS_PUBLIC_URL:https://cds.entando.org}")
    private String cdsPublicUrl;
    @Value("${CDS_PUBLIC_PATH:}")
    private String cdsPublicPath;
    @Value("${CDS_INTERNAL_PUBLIC_SECTION:}")
    private String cdsInternalPublicSection;
    @Value("${CDS_PRIVATE_URL:https://cds.entando.org}")
    private String cdsPrivateUrl;
    @Value("${CDS_PATH:/api/v1}")
    private String cdsPath;
    @Value("${KEYCLOAK_AUTH_URL:http://localhost:8081/auth}")
    private String kcAuthUrl;
    @Value("${KEYCLOAK_REALM:entando}")
    private String kcRealm;
    @Value("${KEYCLOAK_CLIENT_ID:entando-app}")
    private String kcClientId;
    @Value("${KEYCLOAK_CLIENT_SECRET:}")
    private String kcClientSecret;
    
}
