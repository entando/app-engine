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
package org.entando.entando.plugins.jacms.aps.system.services.api;

import com.agiletec.plugins.jacms.aps.system.services.contentmodel.ContentModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.entando.entando.aps.system.services.api.IApiErrorCodes;
import org.entando.entando.aps.system.services.api.model.LegacyApiError;
import org.entando.entando.aps.system.services.api.model.ApiException;
import org.entando.entando.aps.system.services.api.model.StringListApiResponse;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.springframework.http.HttpStatus;

/**
 * @author E.Santoboni
 */
public class ApiContentModelInterface extends AbstractCmsApiInterface {

	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(ApiContentModelInterface.class);
	
	public StringListApiResponse getModels(Properties properties) throws EntException {
		StringListApiResponse response = new StringListApiResponse();
		try {
			List<ContentModel> models;
			String contentTypeParam = properties.getProperty("contentType");
			String contentType = (null != contentTypeParam && contentTypeParam.trim().length() > 0) ? contentTypeParam.trim() : null;
			if (null != contentType && null == this.getContentManager().getSmallContentTypesMap().get(contentType)) {
				LegacyApiError error = new LegacyApiError(IApiErrorCodes.API_PARAMETER_VALIDATION_ERROR, "Content Type " + contentType + " does not exist", HttpStatus.CONFLICT);
				response.addError(error);
				contentType = null;
			}
			if (null != contentType) {
				models = this.getContentModelManager().getModelsForContentType(contentType);
			} else {
				models = this.getContentModelManager().getContentModels();
			}
			List<String> list = new ArrayList<>();
			if (null != models) {
				for (int i = 0; i < models.size(); i++) {
					ContentModel model = models.get(i);
					list.add(String.valueOf(model.getId()));
				}
			}
			response.setResult(list, null);
		} catch (Throwable t) {
			_logger.error("Error loading models", t);
            throw new EntException("Error loading models", t);
        }
		return response;
	}
	
	public ContentModel getModel(Properties properties) throws ApiException {
		String idString = properties.getProperty("id");
        int id;
        try {
            id = Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            throw new ApiException(IApiErrorCodes.API_PARAMETER_VALIDATION_ERROR, "Invalid number format for 'id' parameter - '" + idString + "'", HttpStatus.CONFLICT);
        }
        ContentModel model = this.getContentModelManager().getContentModel(id);
        if (null == model) {
            throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "Model with id '" + idString + "' does not exist", HttpStatus.CONFLICT);
        }
        return model;
	}
	
	public void addModel(ContentModel model) throws ApiException, EntException {
		if (null != this.getContentModelManager().getContentModel(model.getId())) {
            throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "Model with id " + model.getId() + " already exists", HttpStatus.CONFLICT);
        }
		if (null == this.getContentManager().getSmallContentTypesMap().get(model.getContentType())) {
			throw new ApiException(IApiErrorCodes.API_PARAMETER_VALIDATION_ERROR, "Content Type " + model.getContentType() + " does not exist", HttpStatus.CONFLICT);
		}
		try {
            this.getContentModelManager().addContentModel(model);
        } catch (Throwable t) {
        	_logger.error("Error adding model", t);
            throw new EntException("Error adding model", t);
        }
	}
	
	public void updateModel(ContentModel model) throws ApiException, EntException {
		ContentModel oldModel = this.getContentModelManager().getContentModel(model.getId());
		if (null == oldModel) {
            throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "Model with id " + model.getId() + " does not exist", HttpStatus.CONFLICT);
        }
		if (!oldModel.getContentType().equals(model.getContentType())) {
			throw new ApiException(IApiErrorCodes.API_PARAMETER_VALIDATION_ERROR, 
					"Content Type code can't be changed - it has to be '" + oldModel.getContentType() + "'", HttpStatus.CONFLICT);
		}
		try {
            this.getContentModelManager().updateContentModel(model);
        } catch (Throwable t) {
        	_logger.error("Error updating model", t);
            throw new EntException("Error updating model", t);
        }
	}
	
	public void deleteModel(Properties properties) throws ApiException, EntException {
		String idString = properties.getProperty("id");
        int id;
        try {
            id = Integer.parseInt(idString);
        } catch (NumberFormatException e) {
            throw new ApiException(IApiErrorCodes.API_PARAMETER_VALIDATION_ERROR, "Invalid number format for 'id' parameter - '" + idString + "'", HttpStatus.CONFLICT);
        }
        ContentModel model = this.getContentModelManager().getContentModel(id);
        if (null == model) {
            throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "Model with id '" + idString + "' does not exist", HttpStatus.CONFLICT);
        }
		try {
            this.getContentModelManager().removeContentModel(model);
        } catch (Throwable t) {
        	_logger.error("Error deleting model", t);
            throw new EntException("Error deleting model", t);
        }
	}
	
}
