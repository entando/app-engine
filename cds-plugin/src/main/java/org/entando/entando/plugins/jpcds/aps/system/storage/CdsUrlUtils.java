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

import java.net.URI;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.tenants.TenantConfig;
import org.entando.entando.aps.util.UrlUtils.EntUrlBuilder;

@Slf4j
public final class CdsUrlUtils {

    private static final String CDS_PUBLIC_URL_TENANT_PARAM = "cdsPublicUrl";
    private static final String CDS_PRIVATE_URL_TENANT_PARAM = "cdsPrivateUrl";
    private static final String CDS_PUBLIC_PATH_TENANT_PARAM = "cdsPublicPath";
    private static final String CDS_INTERNAL_PUBLIC_SECTION_TENANT_PARAM = "cdsInternalPublicSection";
    private static final String CDS_PATH_TENANT_PARAM = "cdsPath";
    private static final String URL_SEP = "/";
    private static final String DEFAULT_SECTION_PUBLIC = "";
    private static final String DEFAULT_SECTION_PRIVATE = "/protected";

    private CdsUrlUtils(){
    }
    
    public static String getSection(boolean isProtectedResource, Optional<TenantConfig> config, CdsConfiguration configuration, boolean internalCall) {
        if (isProtectedResource) {
            return DEFAULT_SECTION_PRIVATE;
        }
        if (internalCall) {
            return config.map(c -> c.getProperty(CDS_INTERNAL_PUBLIC_SECTION_TENANT_PARAM).orElse(DEFAULT_SECTION_PUBLIC)).orElse(configuration.getCdsInternalPublicSection());
        } else {
            return config.map(c -> c.getProperty(CDS_PUBLIC_PATH_TENANT_PARAM).orElse(DEFAULT_SECTION_PUBLIC)).orElse(configuration.getCdsPublicPath());
        }
    }
    
    public static URI buildCdsExternalPublicResourceUrl(Optional<TenantConfig> config, CdsConfiguration configuration, String ... paths){
        log.debug("Trying to build CDS external public url with  is tenant config empty:'{}', CDS primary configuration public url:'{}' and paths:'{}'",
                config.isEmpty(),
                configuration.getCdsPublicUrl(),
                paths);
        String publicUrl = config.flatMap(c -> c.getProperty(CDS_PUBLIC_URL_TENANT_PARAM)).orElse(configuration.getCdsPublicUrl());
        return EntUrlBuilder.builder().url(publicUrl).paths(paths).build();
    }

    public static URI buildCdsInternalApiUrl(Optional<TenantConfig> config, CdsConfiguration configuration, String ... paths){
        log.debug("Trying to build CDS internal api url with is tenant config empty:'{}', CDS primary configuration private url:'{}' and path:'{}' and paths:'{}'",
                config.isEmpty(),
                configuration.getCdsPrivateUrl(),
                configuration.getCdsPath(),
                paths);
        String apiUrl = config.flatMap(c -> c.getProperty(CDS_PRIVATE_URL_TENANT_PARAM)).orElse(configuration.getCdsPrivateUrl());
        String basePath = config.flatMap(c -> c.getProperty(CDS_PATH_TENANT_PARAM)).orElse(configuration.getCdsPath());
        return EntUrlBuilder.builder().url(apiUrl).path(basePath).paths(paths).build();
    }

    public static String fetchBaseUrl(Optional<TenantConfig> config, CdsConfiguration configuration, boolean usePrivateUrl) {
        log.debug("Trying to fetch base CDS url with is tenant config empty:'{}', CDS primary configuration private url:'{}' public url:'{}' and usePrivateUrl:'{}'",
                config.isEmpty(),
                configuration.getCdsPrivateUrl(),
                configuration.getCdsPublicUrl(),
                usePrivateUrl);

        String privateUrl = config.flatMap(c -> c.getProperty(CDS_PRIVATE_URL_TENANT_PARAM)).orElse(configuration.getCdsPrivateUrl());
        String publicUrl = config.flatMap(c -> c.getProperty(CDS_PUBLIC_URL_TENANT_PARAM)).orElse(configuration.getCdsPublicUrl());

        String baseUrl = usePrivateUrl ? privateUrl : publicUrl;

        return StringUtils.removeEnd(baseUrl, URL_SEP);
    }

    public static EntSubPath extractPathAndFilename(String subPath) {
        if(StringUtils.isBlank(subPath)){
            subPath = "";
        }

        String path = "";
        String filename = subPath;
        int sepIndex = subPath.lastIndexOf(URL_SEP);
        if (sepIndex >= 0) {
            path = subPath.substring(0, sepIndex);
            filename = subPath.substring(sepIndex + 1);
        }
        return EntSubPath.builder().path(path).fileName(filename).build();

    }


    @Getter
    @Setter
    @ToString
    @Builder
    public static class EntSubPath {
        private String path;
        private String fileName;
    }

}
