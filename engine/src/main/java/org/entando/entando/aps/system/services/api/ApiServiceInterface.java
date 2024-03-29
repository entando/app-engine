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
package org.entando.entando.aps.system.services.api;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.aps.util.ApsProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.beanutils.BeanComparator;
import org.entando.entando.aps.system.services.api.model.ApiException;
import org.entando.entando.aps.system.services.api.model.ApiMethodParameter;
import org.entando.entando.aps.system.services.api.model.ApiService;
import org.entando.entando.aps.system.services.api.model.ServiceInfo;
import org.entando.entando.aps.system.services.api.model.ServiceParameterInfo;
import org.entando.entando.aps.system.services.api.server.IResponseBuilder;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.springframework.http.HttpStatus;

/**
 * @author E.Santoboni
 */
public class ApiServiceInterface {

	private static final EntLogger _logger =  EntLogFactory.getSanitizedLogger(ApiServiceInterface.class);
	
    public ArrayList<ServiceInfo> getServices(Properties properties) throws ApiException {
        ArrayList<ServiceInfo> services = new ArrayList<ServiceInfo>();
        try {
            String defaultLangCode = this.getLangManager().getDefaultLang().getCode();
            String langCode = properties.getProperty(SystemConstants.API_LANG_CODE_PARAMETER);
            String tagParamValue = properties.getProperty("tag");
            //String myentandoParamValue = properties.getProperty("myentando");
            //Boolean myentando = (null != myentandoParamValue && myentandoParamValue.trim().length() > 0) ? Boolean.valueOf(myentandoParamValue) : null;
            langCode = (null != langCode && null != this.getLangManager().getLang(langCode)) ? langCode : defaultLangCode;
			Map<String, ApiService> masterServices = this.getApiCatalogManager().getServices(tagParamValue/*, myentando*/);
            Iterator<ApiService> iter = masterServices.values().iterator();
            while (iter.hasNext()) {
                ApiService service = (ApiService) iter.next();
                if (service.isActive() && !service.isHidden() && this.checkServiceAuthorization(service, properties, false)) {
                    ServiceInfo smallService = this.createServiceInfo(service, langCode, defaultLangCode);
                    services.add(smallService);
                }
            }
            BeanComparator comparator = new BeanComparator("description");
            Collections.sort(services, comparator);
        } catch (Throwable t) {
        	_logger.error("Error extracting services", t);
            throw new ApiException(IApiErrorCodes.SERVER_ERROR, "Internal error");
        }
        return services;
    }
    
    protected ServiceInfo createServiceInfo(ApiService service, String langCode, String defaultLangCode) {
        String description = service.getDescription().getProperty(langCode);
        if (null == description || description.trim().length() == 0) {
            description = service.getDescription().getProperty(defaultLangCode);
        }
        ServiceInfo smallService = new ServiceInfo(service.getKey(), description, service.getTag()/*, service.isMyEntando()*/);
        String[] freeParameters = service.getFreeParameters();
        if (null != freeParameters && freeParameters.length > 0) {
            for (int i = 0; i < freeParameters.length; i++) {
                String freeParameter = freeParameters[i];
                ApiMethodParameter apiParameter = service.getMaster().getParameter(freeParameter);
                if (null != apiParameter) {
                    ServiceParameterInfo spi = new ServiceParameterInfo(apiParameter);
                    ApsProperties serviceParameters = service.getParameters();
                    String defaultValue = (null != serviceParameters) ? serviceParameters.getProperty(freeParameter) : null;
                    if (null != defaultValue) {
                        spi.setDefaultValue(defaultValue);
                        spi.setRequired(false);
                    }
                    smallService.addParameter(spi);
                }
            }
        }
        return smallService;
    }
    
    public Object getService(Properties properties) throws ApiException {
        Object response;
        String key = properties.getProperty("key");
        try {
            ApiService service = this.getApiCatalogManager().getApiService(key);
            if (null == service) {
                throw new ApiException(IApiErrorCodes.API_SERVICE_INVALID, "Service '" + key + "' does not exist");
            }
            if (!service.isActive()) {
                throw new ApiException(IApiErrorCodes.API_SERVICE_ACTIVE_FALSE, "Service '" + key + "' is not active");
            }
			this.checkServiceAuthorization(service, properties, true);
            Properties serviceParameters = new Properties();
            serviceParameters.putAll(service.getParameters());
            Iterator<Object> paramIter = properties.keySet().iterator();
            List<String> reservedParameters = SystemConstants.API_RESERVED_PARAMETERS;
            while (paramIter.hasNext()) {
                Object paramName = paramIter.next();
				String paramNameString = paramName.toString();
                if (reservedParameters.contains(paramNameString) || service.isFreeParameter(paramNameString)) {
                    serviceParameters.put(paramNameString, properties.get(paramName));
                }
            }
            response = this.getResponseBuilder().createResponse(service.getMaster(), serviceParameters);
        } catch (ApiException e) {
            throw e;
        } catch (Throwable t) {
        	_logger.error("Error invocating service - key '{}'", key, t);
            throw new ApiException(IApiErrorCodes.SERVER_ERROR, "Internal error");
        }
        return response;
    }
	
	protected boolean checkServiceAuthorization(ApiService service, Properties properties, boolean throwApiException) throws ApiException, Throwable {
		if (!service.getRequiredAuth()) {
			return true;
		}
		try {
			UserDetails user = (UserDetails) properties.get(SystemConstants.API_USER_PARAMETER);
            if (null == user) {
                throw new ApiException(IApiErrorCodes.API_AUTHENTICATION_REQUIRED, 
						"Authentication is mandatory for service '" + service.getKey() + "'", HttpStatus.UNAUTHORIZED);
            }
			if ((null != service.getRequiredGroup() && !this.getAuthorizationManager().isAuthOnGroup(user, service.getRequiredGroup())) 
					|| (null != service.getRequiredPermission() && !this.getAuthorizationManager().isAuthOnPermission(user, service.getRequiredPermission()))) {
				throw new ApiException(IApiErrorCodes.API_AUTHORIZATION_REQUIRED, 
						"Permission denied for service '" + service.getKey() + "'", HttpStatus.FORBIDDEN);
			}
		} catch (ApiException ae) {
			if (throwApiException) {
				throw ae;
			}
			return false;
		} catch (Throwable t) {
			_logger.error("Error checking auth for service - key '{}'", service.getKey(), t);
            throw t;
		}
		return true;
	}
	
	protected IAuthorizationManager getAuthorizationManager() {
		return _authorizationManager;
	}
	public void setAuthorizationManager(IAuthorizationManager authorizationManager) {
		this._authorizationManager = authorizationManager;
	}
	
    protected IApiCatalogManager getApiCatalogManager() {
        return _apiCatalogManager;
    }
    public void setApiCatalogManager(IApiCatalogManager apiCatalogManager) {
        this._apiCatalogManager = apiCatalogManager;
    }
	
    protected ILangManager getLangManager() {
        return _langManager;
    }
    public void setLangManager(ILangManager langManager) {
        this._langManager = langManager;
    }
	
    protected IResponseBuilder getResponseBuilder() {
        return _responseBuilder;
    }
    public void setResponseBuilder(IResponseBuilder responseBuilder) {
        this._responseBuilder = responseBuilder;
    }
    
	private IAuthorizationManager _authorizationManager;
    private IApiCatalogManager _apiCatalogManager;
    private ILangManager _langManager;
    private IResponseBuilder _responseBuilder;
    
}
