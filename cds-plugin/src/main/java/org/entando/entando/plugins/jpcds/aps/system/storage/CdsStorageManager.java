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

import org.entando.entando.aps.system.services.storage.model.DiskInfoDto;
import com.agiletec.aps.system.SystemConstants;
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
import lombok.AccessLevel;
import lombok.Getter;
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
import org.entando.entando.ent.exception.EntResourceNotFoundException;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.entando.entando.plugins.jpcds.aps.system.storage.CdsUrlUtils.EntSubPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.entando.entando.aps.system.services.storage.CdsActive;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
@Service(SystemConstants.STORAGE_MANAGER)
@CdsActive(true)
public class CdsStorageManager implements IStorageManager {

    private static final String ERROR_VALIDATING_PATH_MSG = "Error validating path";
    
    @Getter(AccessLevel.PROTECTED)
    private transient ITenantManager tenantManager;
    @Getter(AccessLevel.PROTECTED)
    private transient CdsConfiguration configuration;
    @Getter(AccessLevel.PROTECTED)
    private transient CdsRemoteCaller caller;

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
            Optional<TenantConfig> config = this.getTenantConfig();
            if(StringUtils.isBlank(subPath)){
                throw new EntRuntimeException(ERROR_VALIDATING_PATH_MSG);
            }
            this.validateAndReturnResourcePath(config, subPath, false, isProtectedResource);
            URI apiUrl = CdsUrlUtils.buildCdsInternalApiUrl(config, configuration, "/upload/");
            CdsCreateResponseDto response = this.caller.executePostCall(apiUrl,
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
            Optional<TenantConfig> config = this.getTenantConfig();
            if (StringUtils.isBlank(subPath)){
                throw new EntRuntimeException(ERROR_VALIDATING_PATH_MSG);
            }
            this.validateAndReturnResourcePath(config, subPath, true, isProtectedResource);
            URI apiUrl = EntUrlBuilder.builder()
                            .url(CdsUrlUtils.buildCdsInternalApiUrl(config, configuration))
                            .path("/delete/")
                            .path(CdsUrlUtils.getSection(isProtectedResource, config, this.configuration, true))
                            .path(subPath)
                            .build();
            return this.caller.executeDeleteCall(apiUrl, config, false);
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
            Optional<TenantConfig> config = this.getTenantConfig();
            if (StringUtils.isBlank(subPath)) {
                throw new EntRuntimeException(ERROR_VALIDATING_PATH_MSG);
            }
            if (!this.exists(subPath, isProtectedResource)) {
                throw new EntResourceNotFoundException(
                        String.format("File \"%s\", protected \"%s\", Not Found", subPath, isProtectedResource));
            }
            this.validateAndReturnResourcePath(config, subPath, true, isProtectedResource);
            url = (isProtectedResource) ?
                    CdsUrlUtils.buildCdsInternalApiUrl(config, configuration)  :
                    CdsUrlUtils.buildCdsExternalPublicResourceUrl(config, configuration);
            url = EntUrlBuilder.builder()
                    .url(url)
                    .path(CdsUrlUtils.getSection(isProtectedResource, config, this.configuration, true))
                    .path(subPath).build();
            Optional<ByteArrayInputStream> is = caller.getFile(url, config, isProtectedResource);
            return is.orElse(new ByteArrayInputStream(new byte[0]));
        } catch (EntResourceNotFoundException | EntRuntimeException ert) {
            throw ert;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                log.info("File Not found - uri {}", url);
                return null;
            }
            throw new EntResourceNotFoundException(ERROR_EXTRACTING_FILE, e);
        } catch (Exception e) {
            throw new EntResourceNotFoundException(ERROR_EXTRACTING_FILE, e);
        }
    }

    @Override
    public DiskInfoDto getDiskInfo() throws EntException {
        final String ERROR_EXTRACTING_DISK_INFO = "Error extracting disk info: url %s";
        URI url = null;
        try {
            Optional<TenantConfig> config = getTenantConfig();
            url = CdsUrlUtils.buildCdsInternalApiUrl(config, configuration, "utils", "diskinfo");
            Optional<DiskInfoDto> is = caller.getDiskInfo(url, config);
            return is.orElseThrow(IOException::new);
        } catch (EntRuntimeException ert) {
            throw ert;
        } catch (Exception e) {
            String errorMessage = String.format(ERROR_EXTRACTING_DISK_INFO, Optional.ofNullable(url).map(URI::toString).orElse("null"));
            throw new EntResourceNotFoundException(errorMessage, e);
        }
    }
    
    @Override
    public String getResourceUrl(String subPath, boolean isProtectedResource) {
        try {
            Optional<TenantConfig> config = this.getTenantConfig();
            return this.validateAndReturnResourcePath(config, subPath, false, isProtectedResource);
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

    @Override
    public boolean move(String subPathSource, boolean isProtectedResourceSource, String subPathDest, boolean isProtectedResourceDest) throws EntException {
        if (!this.exists(subPathSource, isProtectedResourceSource)) {
            log.error(String.format(
                    "Source File does not exists - path '%s' protected '%s'",
                    subPathSource, isProtectedResourceSource));
            return false;
        }
        if (this.exists(subPathDest, isProtectedResourceDest)) {
            log.error(String.format(
                    "Destination already exists - path '%s' protected '%s'",
                    subPathDest, isProtectedResourceDest));
            return false;
        }
        try {
            InputStream stream = this.getStream(subPathSource, isProtectedResourceSource);
            this.saveFile(subPathDest, isProtectedResourceDest, stream);
            this.deleteFile(subPathSource, isProtectedResourceSource);
        } catch (Exception e) {
            throw new EntException("Error moving file", e);
        }
        return true;
    }

    // when frontend  wants to retrieve public or protected folder contents it gets request with an empty subpath
    private boolean isSubPathPresent(String[] filenames, String subPath){
        if (StringUtils.isEmpty(subPath)) {
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
        this.validateAndReturnResourcePath(config, subPath, true, isProtectedResource);

        URI apiUrl = EntUrlBuilder.builder()
                .url(CdsUrlUtils.buildCdsInternalApiUrl(config, configuration).toString())
                .path("/list/")
                .path(CdsUrlUtils.getSection(isProtectedResource, config, this.configuration, true))
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
    

    private String validateAndReturnResourcePath(Optional<TenantConfig> config, String resourceRelativePath, boolean privateCall, boolean privateUrl) {
        try {
            String baseUrl = EntUrlBuilder.builder()
                    .url(CdsUrlUtils.fetchBaseUrl(config, configuration, privateUrl))
                    .path(CdsUrlUtils.getSection(privateUrl, config, this.configuration, privateCall))
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

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        WebApplicationContext ctx = ContextLoader.getCurrentWebApplicationContext();
        if (ctx == null) {
            log.warn("Null WebApplicationContext during deserialization");
            return;
        }
        this.tenantManager = ctx.getBean(ITenantManager.class);
        this.configuration = ctx.getBean(CdsConfiguration.class);
        this.caller = ctx.getBean(CdsRemoteCaller.class);
    }
    
}
