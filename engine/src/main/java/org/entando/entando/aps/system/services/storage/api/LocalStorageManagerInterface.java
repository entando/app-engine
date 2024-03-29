/*
 * Copyright 2015-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.system.services.storage.api;

import com.agiletec.aps.system.SystemConstants;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.api.IApiErrorCodes;
import org.entando.entando.aps.system.services.api.IApiExportable;
import org.entando.entando.aps.system.services.api.model.ApiException;
import org.entando.entando.aps.system.services.api.model.LinkedListItem;
import org.entando.entando.aps.system.services.storage.BasicFileAttributeView;
import org.entando.entando.aps.system.services.storage.IStorageManager;
import org.entando.entando.aps.system.services.storage.StorageManagerUtil;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public class LocalStorageManagerInterface implements IApiExportable {

	private static final EntLogger logger = EntLogFactory.getSanitizedLogger(LocalStorageManagerInterface.class);

	private static final String INVALID_PARENT_DIRECTORY_ERROR_MESSAGE = "Invalid parent directory";

	private IStorageManager storageManager;
	private static final String PARAM_PATH = "path";
	private static final String PARAM_IS_PROTECTED = "protected";
	private static final String PARAM_DELETE_RECURSIVE = "recursive";

	public List<LinkedListItem> getListDirectory(Properties properties) throws Throwable {
		List<LinkedListItem> list = new ArrayList<>();
		String pathValue = properties.getProperty(PARAM_PATH);
		String protectedValue = properties.getProperty(PARAM_IS_PROTECTED);
		boolean isProtected = StringUtils.equalsIgnoreCase(protectedValue, "true");
		try {
			if (StringUtils.isNotBlank(pathValue)) pathValue = URLDecoder.decode(pathValue, "UTF-8");
			if (!StorageManagerUtil.isValidDirName(pathValue)) {
				throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "The path '" + pathValue + "' does not exists", HttpStatus.CONFLICT);
			}
			BasicFileAttributeView[] files = this.getStorageManager().listAttributes(pathValue, isProtected);
			for (BasicFileAttributeView fileAttributeView : files) {
				String url = this.getApiResourceURLWithParams(properties, fileAttributeView, pathValue, isProtected);
				LinkedListItem item = new LinkedListItem();
				item.setCode(this.buildResourcePath(fileAttributeView, pathValue));
				item.setUrl(url);
				list.add(item);
			}
		} catch (Throwable t) {
			logger.error("Error extracting storage resources in path: {} and protected flag: {} ", pathValue, isProtected, t);
			throw t;
		}
		return list;
	}

	public JAXBBasicFileAttributeView getFile(Properties properties) throws Throwable {
		JAXBBasicFileAttributeView apiResult;
		String pathValue = properties.getProperty(PARAM_PATH);
		String protectedValue = properties.getProperty(PARAM_IS_PROTECTED);
		boolean isProtected = StringUtils.equalsIgnoreCase(protectedValue, "true");
		try {
			if (StringUtils.isNotBlank(pathValue)) pathValue = URLDecoder.decode(pathValue, "UTF-8");
			BasicFileAttributeView result = this.getStorageManager().getAttributes(pathValue, isProtected);
			if (null == result) {
				throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "The path '" + pathValue + "' does not exist", HttpStatus.CONFLICT);
			}
			
			InputStream fis = null;
			if (!result.isDirectory()) {
				fis = this.getStorageManager().getStream(pathValue, isProtected);
			}
			apiResult = new JAXBBasicFileAttributeView(result, fis);
		} catch (ApiException ae) {
			throw ae;
		} catch (Throwable t) {
			logger.error("Error extracting storage resource in path: {} and protected flag: {} ", pathValue, isProtected, t);
			throw t;
		}
		return apiResult;
	}
	
    public void addResource(JAXBStorageResource storageResource, Properties properties) throws Throwable {
		try {
		boolean isProtected = storageResource.isProtectedResource();
			//validate parent directory;
			String path = StringUtils.removeEnd(storageResource.getName(), "/");
			String parentFolder = FilenameUtils.getFullPathNoEndSeparator(path);
			if (StringUtils.isNotBlank(parentFolder)) parentFolder = URLDecoder.decode(parentFolder, "UTF-8");
			if (!StorageManagerUtil.isValidDirName(parentFolder)) {
				throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, INVALID_PARENT_DIRECTORY_ERROR_MESSAGE, HttpStatus.CONFLICT);
			}
			BasicFileAttributeView parentBasicFileAttributeView = this.getStorageManager().getAttributes(parentFolder, isProtected);
			if (null == parentBasicFileAttributeView || !parentBasicFileAttributeView.isDirectory()) {
				throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, INVALID_PARENT_DIRECTORY_ERROR_MESSAGE, HttpStatus.CONFLICT);
			}
			//validate exists
			BasicFileAttributeView basicFileAttributeView2 = this.getStorageManager().getAttributes(path, isProtected);
			if (null != basicFileAttributeView2) {
				throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "File already present", HttpStatus.CONFLICT);					
			}

			String filename = StringUtils.substringAfter(storageResource.getName(), parentFolder);
			if (storageResource.isDirectory()) {
				if (!StorageManagerUtil.isValidDirName(filename)) {
					throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "Invalid dir name", HttpStatus.CONFLICT);									
				}
				this.getStorageManager().createDirectory(storageResource.getName(), isProtected);
			} else {
				//validate file content
				if (!storageResource.isDirectory() && (null == storageResource.getBase64() || storageResource.getBase64().length == 0 )) {
					throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "A file cannot be empty", HttpStatus.CONFLICT);				
				}
				if (!StorageManagerUtil.isValidFilename(filename)) {
					throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "Invalid file name", HttpStatus.CONFLICT);
				}
				this.getStorageManager().saveFile(storageResource.getName(), isProtected,  new ByteArrayInputStream(storageResource.getBase64()));
			}
		} catch (ApiException ae) {

			throw ae;
		} catch (Throwable t) {
			logger.error("Error adding new storage resource", t);
			throw t;
		}
    }

    public void deleteResource(Properties properties) throws Throwable {
		String pathValue = properties.getProperty(PARAM_PATH);
		String protectedValue = properties.getProperty(PARAM_IS_PROTECTED);
		boolean isProtected = StringUtils.equalsIgnoreCase(protectedValue, "true");
    	try {    	
    		if (StringUtils.isNotBlank(pathValue)) pathValue = URLDecoder.decode(pathValue, "UTF-8");
			String path = StringUtils.removeEnd(pathValue, "/");
			String parentFolder = FilenameUtils.getFullPathNoEndSeparator(path);
			BasicFileAttributeView parentBasicFileAttributeView = this.getStorageManager().getAttributes(parentFolder, isProtected);
			if (null == parentBasicFileAttributeView || !parentBasicFileAttributeView.isDirectory()) {
				throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, INVALID_PARENT_DIRECTORY_ERROR_MESSAGE, HttpStatus.CONFLICT);
			}
			if (!StorageManagerUtil.isValidDirName(parentFolder)) {
				throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, INVALID_PARENT_DIRECTORY_ERROR_MESSAGE, HttpStatus.CONFLICT);
			}
    		BasicFileAttributeView basicFileAttributeView = this.getStorageManager().getAttributes(pathValue, isProtected);
    		if (null == basicFileAttributeView) {
    			throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "The file does not exists", HttpStatus.CONFLICT);			
    		}
    		String filename = StringUtils.substringAfter(pathValue, parentFolder);
    		if (basicFileAttributeView.isDirectory()) {
				if (!StorageManagerUtil.isValidDirName(filename)) {
					throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "Invalid dir name", HttpStatus.CONFLICT);									
				}
    			String recursiveDelete = properties.getProperty(PARAM_DELETE_RECURSIVE);
    			boolean isRecursiveDelete = StringUtils.equalsIgnoreCase(recursiveDelete, "true");
    			if (!isRecursiveDelete) {
    				String[] dirContents = this.getStorageManager().list(pathValue, isProtected);
    				if (null != dirContents && dirContents.length > 0) {
    					throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "The directory is not empty", HttpStatus.CONFLICT);			    					
    				}
    			}
    			this.getStorageManager().deleteDirectory(pathValue, isProtected);
    		} else {
    			//it's a file
    			if (!StorageManagerUtil.isValidFilename(filename)) {
    				throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "Invalid filename", HttpStatus.CONFLICT);									
    			}
    			this.getStorageManager().deleteFile(pathValue, isProtected);
    		}

    	} catch (ApiException ae) {
    		
    		throw ae;
    	} catch (Throwable t) {
    		logger.error("Error adding new storage resource", t);
    		throw t;
    	}
    }
	
	public String getApiResourceURLWithParams(Properties properties, BasicFileAttributeView fileAttributeView, String path, boolean isProtected) throws Throwable {
		String langCode = properties.getProperty(SystemConstants.API_LANG_CODE_PARAMETER); 
		MediaType mediaType = (MediaType) properties.get(SystemConstants.API_PRODUCES_MEDIA_TYPE_PARAMETER);
		String applicationBaseUrl = properties.getProperty(SystemConstants.API_APPLICATION_BASE_URL_PARAMETER);
		String url = this.getApiResourceUrl(fileAttributeView, applicationBaseUrl, langCode, mediaType);
		StringBuilder stringBuilder = new StringBuilder(url);
		
		String fullPath = this.buildResourcePath(fileAttributeView, path);
		if (StringUtils.isNotBlank(fullPath)) fullPath = URLDecoder.decode(fullPath, CharEncoding.UTF_8);
		stringBuilder.append("?").append(PARAM_PATH).append("=").append(fullPath);
		stringBuilder.append("&").append(PARAM_IS_PROTECTED).append("=").append(isProtected);
		return stringBuilder.toString();
	}
	
	@Override
	public String getApiResourceUrl(Object object, String applicationBaseUrl, String langCode, MediaType mediaType) {
		if (!(object instanceof BasicFileAttributeView) || null == applicationBaseUrl || null == langCode) {
			return null;
		}
		BasicFileAttributeView basicFileAttributeView = (BasicFileAttributeView) object;
		String type = basicFileAttributeView.isDirectory() ? "storage" : "storageResource";
		return String.format("%sapi/%s/%s/core/%s.%s", applicationBaseUrl,
				SystemConstants.LEGACY_API_PREFIX, langCode, type, getExtension(mediaType));
	}
	
	protected String buildResourcePath(BasicFileAttributeView fileAttributeView, String path) {
		String fullPath = null;
		if (StringUtils.isBlank(path)) {
			fullPath = "";
		} else {
			if (!path.endsWith("/")) path = path + "/";
			fullPath = path;
		}
		fullPath = fullPath + fileAttributeView.getName();
		return fullPath;
	}
	

	protected IStorageManager getStorageManager() {
		return storageManager;
	}
	public void setStorageManager(IStorageManager storageManager) {
		this.storageManager = storageManager;
	}
}
