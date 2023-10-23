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

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.storage.BasicFileAttributeView;
import org.entando.entando.aps.system.services.storage.IStorageManager;
import org.entando.entando.aps.system.services.storage.StorageManagerUtil;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.aps.system.services.tenants.TenantConfig;
import org.entando.entando.aps.util.UrlUtils.EntUrlBuilder;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.exception.EntCDSResourceNotFoundException;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.entando.entando.plugins.jpcds.aps.system.storage.CdsUrlUtils.EntSubPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.entando.entando.aps.system.services.storage.CdsActive;

@Slf4j
@Service("StorageManager")
@CdsActive(true)
public class CdsStorageManager implements IStorageManager {

    private static final String ERROR_VALIDATING_PATH_MSG = "Error validating path";
    private final transient ITenantManager tenantManager;
    private final transient CdsConfiguration configuration;
    private final transient CdsRemoteCaller caller;


    @Autowired
    public CdsStorageManager(CdsRemoteCaller caller, ITenantManager tenantManager, CdsConfiguration configuration) {
        log.info("** Enabled CDS Storage Manager **");
        this.caller = caller;
        this.tenantManager = tenantManager;
        this.configuration = configuration;
    }

    @Override
    public void createDirectory(String subPath, boolean isProtectedResource) throws EntException {
        this.create(subPath, isProtectedResource, Optional.empty());
    }
    
    @Override
    public void saveFile(String subPath, boolean isProtectedResource, InputStream is) throws EntException, IOException {
        this.create(subPath, isProtectedResource, Optional.ofNullable(is));
    }
    
    private void create(String subPath, boolean isProtectedResource, Optional<InputStream> fileInputStream) {
        try {
            Optional<TenantConfig> config = getTenantConfig();
            if(StringUtils.isBlank(subPath)){
                throw new EntRuntimeException(ERROR_VALIDATING_PATH_MSG);
            }

            this.validateAndReturnResourcePath(config, subPath, isProtectedResource);

            URI apiUrl = CdsUrlUtils.buildCdsInternalApiUrl(config, configuration, "/upload/");
            CdsCreateResponseDto response = caller.executePostCall(apiUrl,
                    subPath,
                    isProtectedResource,
                    fileInputStream,
                    config,
                    false);

            if (!response.isStatusOk()) {
                throw new EntRuntimeException("Invalid status - Response " + response.isStatusOk());
            }
        } catch (EntRuntimeException ert) {
            throw ert;
        } catch (Exception e) {
            throw new EntRuntimeException("Error saving file/directory", e);
        }
    }

    @Override
    public void deleteDirectory(String subPath, boolean isProtectedResource) throws EntException {
        this.deleteFile(subPath, isProtectedResource); //same behavior
    }

    @Override
    public boolean deleteFile(String subPath, boolean isProtectedResource) {
        try {
            Optional<TenantConfig> config = getTenantConfig();
            if(StringUtils.isBlank(subPath)){
                throw new EntRuntimeException(ERROR_VALIDATING_PATH_MSG);
            }

            this.validateAndReturnResourcePath(config, subPath, isProtectedResource);

            URI apiUrl = EntUrlBuilder.builder()
                            .url(CdsUrlUtils.buildCdsInternalApiUrl(config, configuration))
                            .path("/delete/")
                            .path(CdsUrlUtils.getInternalSection(isProtectedResource))
                            .path(subPath)
                            .build();

            return caller.executeDeleteCall(apiUrl, config, false);

        } catch (EntRuntimeException ert) {
            throw ert;
        } catch (Exception e) {
            throw new EntRuntimeException("Error deleting file", e);
        }
    }
    
    @Override
    public InputStream getStream(String subPath, boolean isProtectedResource) throws EntException {
        final String ERROR_EXTRACTING_FILE = "Error extracting file";
        URI url = null;
        try {
            Optional<TenantConfig> config = getTenantConfig();
            if(StringUtils.isBlank(subPath)){
                throw new EntRuntimeException(ERROR_VALIDATING_PATH_MSG);
            }

            this.validateAndReturnResourcePath(config, subPath, isProtectedResource);

            url = (isProtectedResource) ?
                    CdsUrlUtils.buildCdsInternalApiUrl(config, configuration)  :
                    CdsUrlUtils.buildCdsExternalPublicResourceUrl(config, configuration);

            url = EntUrlBuilder.builder()
                    .url(url)
                    .path(CdsUrlUtils.getInternalSection(isProtectedResource))
                    .path(subPath).build();

            Optional<ByteArrayInputStream> is = caller.getFile(url, config, isProtectedResource);
            return is.orElseThrow(IOException::new);

        } catch (EntRuntimeException ert) {
            throw ert;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.info("File Not found - uri {}", url);
                return null;
            }  
            throw new EntCDSResourceNotFoundException(ERROR_EXTRACTING_FILE, e);
        } catch (Exception e) {
            throw new EntCDSResourceNotFoundException(ERROR_EXTRACTING_FILE, e);
        }
    }

    @Override
    public String getResourceUrl(String subPath, boolean isProtectedResource) {
        try {
            Optional<TenantConfig> config = getTenantConfig();
            return this.validateAndReturnResourcePath(config, subPath, isProtectedResource);
        } catch (Exception e) {
            throw new EntRuntimeException("Error extracting resource url", e);
        }
    }

    @Override
    public boolean exists(String subPath, boolean isProtectedResource) {
        EntSubPath subPathParsed = CdsUrlUtils.extractPathAndFilename(subPath);
        String[] filenames = this.list(subPathParsed.getPath(), isProtectedResource);
        return (null != filenames && isSubPathPresent(filenames,subPathParsed.getFileName()));
    }

    // when frontend  wants to retrieve public or protected folder contents it gets request with an empty subpath
    private boolean isSubPathPresent(String[] filenames, String subPath){
        if(StringUtils.isEmpty(subPath)) {
            return filenames.length > 0;
        } else {
            return Arrays.asList(filenames).contains(subPath);
        }
    }

    @Override
    public BasicFileAttributeView getAttributes(String subPath, boolean isProtectedResource) {
        EntSubPath subPathParsed = CdsUrlUtils.extractPathAndFilename(subPath);
        return listAttributes(subPathParsed.getPath(), isProtectedResource, CdsFilter.ALL)
                .stream()
                .filter(attr -> attr.getName().equals(subPathParsed.getFileName()))
                .findFirst().orElse(null);
    }


    @Override
    public String[] list(String subPath, boolean isProtectedResource) {
        return this.listString(subPath, isProtectedResource, CdsFilter.ALL);
    }

    @Override
    public String[] listDirectory(String subPath, boolean isProtectedResource) {
        return this.listString(subPath, isProtectedResource, CdsFilter.DIRECTORY);
    }

    @Override
    public String[] listFile(String subPath, boolean isProtectedResource) {
        return this.listString(subPath, isProtectedResource, CdsFilter.FILE);
    }
    
    protected String[] listString(String subPath, boolean isProtectedResource, CdsFilter filter) {
        return listAttributes(subPath, isProtectedResource, filter).stream()
                .map(bfa -> bfa.getName()).collect(Collectors.toList())
                .toArray(String[]::new);
    }
    
    @Override
    public BasicFileAttributeView[] listAttributes(String subPath, boolean isProtectedResource) {
        return listAttributes(subPath, isProtectedResource, CdsFilter.ALL).toArray(BasicFileAttributeView[]::new);
    }
    
    @Override
    public BasicFileAttributeView[] listDirectoryAttributes(String subPath, boolean isProtectedResource) {
        return listAttributes(subPath, isProtectedResource, CdsFilter.DIRECTORY).toArray(BasicFileAttributeView[]::new);
    }

    @Override
    public BasicFileAttributeView[] listFileAttributes(String subPath, boolean isProtectedResource) {
        return listAttributes(subPath, isProtectedResource, CdsFilter.FILE).toArray(BasicFileAttributeView[]::new);
    }
    
    private List<BasicFileAttributeView> listAttributes(String subPath, boolean isProtectedResource, CdsFilter filter) {
        Optional<TenantConfig> config = this.getTenantConfig();
        this.validateAndReturnResourcePath(config, subPath, isProtectedResource);

        URI apiUrl = EntUrlBuilder.builder()
                .url(CdsUrlUtils.buildCdsInternalApiUrl(config, configuration).toString())
                .path("/list/")
                .path(CdsUrlUtils.getInternalSection(isProtectedResource))
                .path(subPath)
                .build();

        Optional<CdsFileAttributeViewDto[]> cdsFileList = caller.getFileAttributeView(apiUrl, config);

        return remapAndSort(cdsFileList, filter);
    }

    private List<BasicFileAttributeView> remapAndSort(Optional<CdsFileAttributeViewDto[]> cdsFileList, CdsFilter filter){
        return Arrays.asList(cdsFileList.orElse(new CdsFileAttributeViewDto[]{})).stream()
                .filter(cds -> cdsTypeFilter(filter, cds))
                .map(cdsFileAttribute -> {
                    BasicFileAttributeView bfa = new BasicFileAttributeView();
                    bfa.setName(cdsFileAttribute.getName());
                    bfa.setDirectory(cdsFileAttribute.getDirectory());
                    bfa.setLastModifiedTime(cdsFileAttribute.getDate());
                    bfa.setSize(cdsFileAttribute.getSize());
                    return bfa;
                }).sorted().collect(Collectors.toList());
    }

    private boolean cdsTypeFilter(CdsFilter filter, CdsFileAttributeViewDto obj) {
        switch(filter){
            case FILE:
                return !obj.getDirectory();
            case DIRECTORY:
                return obj.getDirectory();
            case ALL:
            default:
                return true;
        }
    }

    @Override
    public String readFile(String subPath, boolean isProtectedResource) throws EntException {
        try {
            InputStream stream = this.getStream(subPath, isProtectedResource);
            // remove the use of FileTextReader (it add a newline)
            // used a faster way https://stackoverflow.com/questions/309424/how-do-i-read-convert-an-inputstream-into-a-string-in-java
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        } catch (EntRuntimeException ert) {
            throw ert;
        } catch (IOException ex) {
            throw new EntException("Error extracting text", ex);
        }
    }

    @Override
    public void editFile(String subPath, boolean isProtectedResource, InputStream is) throws EntException {
        this.create(subPath, isProtectedResource, Optional.ofNullable(is));
    }

    @Override
    public String createFullPath(String subPath, boolean isProtectedResource) {
        return getResourceUrl(subPath, isProtectedResource);
    }
    
    private Optional<TenantConfig> getTenantConfig() {
        return ApsTenantApplicationUtils.getTenant()
                .filter(StringUtils::isNotBlank)
                .flatMap(tenantManager::getConfig);
    }
    

    private String validateAndReturnResourcePath(Optional<TenantConfig> config, String resourceRelativePath, boolean privateUrl) {
        try {
            String baseUrl = EntUrlBuilder.builder()
                    .url(CdsUrlUtils.fetchBaseUrl(config, configuration, privateUrl))
                    .path(CdsUrlUtils.getInternalSection(privateUrl)) // << this is part of base url because we want check path traversal!!
                    .build().toString();

            String fullPath = EntUrlBuilder.builder()
                    .url(baseUrl)
                    .path(resourceRelativePath)
                    .build().toString();

            if (!StorageManagerUtil.doesPathContainsPath(baseUrl, fullPath, true)) {
                throw mkPathValidationErr(baseUrl, fullPath);
            }

            return fullPath;
        } catch (IOException e) {
            throw new EntRuntimeException(ERROR_VALIDATING_PATH_MSG, e);
        }
    }

	private EntRuntimeException mkPathValidationErr(String diskRoot, String fullPath) {
		return new EntRuntimeException(
				String.format("Path validation failed: \"%s\" not in \"%s\"", fullPath, diskRoot)
		);
	}

    @Override
    public boolean isDirectory(String subPath, boolean isProtectedResource) {
        EntSubPath entSubPathParse = CdsUrlUtils.extractPathAndFilename(subPath);
        BasicFileAttributeView[] attributes = listDirectoryAttributes(subPath, isProtectedResource);
        for(BasicFileAttributeView attr: attributes) {
            if(entSubPathParse.getPath().equals(attr.getName())) {
                return true;
            }
        }
        return false;
    }

    public enum CdsFilter {
        FILE,
        DIRECTORY,
        ALL;
    }
}
