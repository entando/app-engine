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
package com.agiletec.plugins.jacms.aps.system.services.resource.model;

import org.entando.entando.ent.exception.EntException;
import com.agiletec.plugins.jacms.aps.system.services.resource.parse.ResourceDOM;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import java.io.InputStream;
import org.entando.entando.ent.exception.EntResourceNotFoundException;
import org.entando.entando.ent.exception.EntResourceNotFoundRuntimeException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;

/**
 * Base abstract class for implementation of single instance resource objects.
 */
public abstract class AbstractMonoInstanceResource extends AbstractResource {

	private static final EntLogger logger = EntLogFactory.getSanitizedLogger(AbstractMonoInstanceResource.class);
	public static final String ERROR_ON_EXTRACTING_RESOURCE_STREAM = "Error on extracting resource Stream";

	private ResourceInstance instance;

	/**
     * Implementazione del metodo isMultiInstance() di AbstractResource.
     * Restituisce sempre false in quanto questa classe astratta è 
     * alla base di tutte le risorse SingleInstance.
     * @return false in quanto la risorsa è composta da una singola istanza. 
     */
	@Override
	public boolean isMultiInstance() {
    	return false;
    }
	
	@Override
	public InputStream getResourceStream(int size, String langCode) {
		return this.getResourceStream();
	}

	@Override
	public InputStream getResourceStream() {
		ResourceInstance resourceInstance = instance;
		String subPath = super.getDiskSubFolder() + resourceInstance.getFileName();
		try {
			return this.getStorageManager().getStream(subPath, this.isProtectedResource());
		} catch (EntResourceNotFoundException e) {
			throw new EntResourceNotFoundRuntimeException(ERROR_ON_EXTRACTING_RESOURCE_STREAM, e);
		} catch (Throwable t) {
			logger.error(ERROR_ON_EXTRACTING_RESOURCE_STREAM, t);
			throw new RuntimeException(ERROR_ON_EXTRACTING_RESOURCE_STREAM, t);
		}
	}
    
	@Override
	public void deleteResourceInstances() throws EntException {
		try {
			if (instance == null) {
				logger.debug("Null instance for resource {}", getId());
				return;
			}
			String docName = instance.getFileName();
		    String subPath = this.getDiskSubFolder() + docName;
			this.getStorageManager().deleteFile(subPath, this.isProtectedResource());
		} catch (Throwable t) {
			logger.error("Error on deleting resource instances", t);
			throw new EntException("Error on deleting resource instances", t);
		}
	}
    
	/**
     * Setta l'istanza alla risorsa.
     * @param instance L'istanza da settare alla risorsa.
     */
	@Override
	public void addInstance(ResourceInstance instance) {
    	this.instance = instance;
    }
    
    /**
     * Restituisce l'istanza della risorsa.
     * @return L'istanza della risorsa.
     */
    public ResourceInstance getInstance() {
    	return instance;
    }
	
	@Override
	public ResourceInstance getDefaultInstance() {
		return this.getInstance();
	}
    
    @Override
	public String getXML() {
        ResourceDOM resourceDom = this.getResourceDOM();
        resourceDom.addInstance(instance.getJDOMElement());
        return resourceDom.getXMLDocument();
    }
    
    String getNewInstanceFileName(String masterFileName) throws Throwable {
		String baseName = getUniqueBaseName(masterFileName);

		String extension = FilenameUtils.getExtension(masterFileName);
		if (StringUtils.isNotEmpty(extension)) {
			baseName += "." + extension;
		}

    	return baseName;
	}
}