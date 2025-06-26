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
package com.agiletec.apsadmin.system.dispatcher;

import com.agiletec.apsadmin.system.ApsAdminSystemConstants;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.struts2.dispatcher.Dispatcher;
import org.apache.struts2.dispatcher.HostConfig;

import javax.servlet.ServletContext;

/**
 * Extension of the InitOperations class used by Struts2 main filter.
 * The extension lets to place the base configuration for Struts2 (the definition of the configuration files) 
 * outside the filter configuration but always within the Deployment Descriptor (web.xml).
 * The name of the configuration parameter (init-param of web.xml) is defined in the system constants 
 * in the interface {@link ApsAdminSystemConstants}.
 * @author E.Santoboni
 */
public class InitOperations extends org.apache.struts2.dispatcher.InitOperations {

	@Override
	protected Dispatcher createDispatcher(HostConfig filterConfig) {
		Map<String, String> params = new HashMap();
		Iterator<String> parameterNames = filterConfig.getInitParameterNames();

		while (parameterNames.hasNext()) {
			String name = parameterNames.next();
			String value = filterConfig.getInitParameter(name);
			params.put(name, value);
		}

		ServletContext servletContext = filterConfig.getServletContext();
		String struts2Config = servletContext.getInitParameter(ApsAdminSystemConstants.STRUTS2_CONFIG_INIT_PARAM_NAME);
		if (null != struts2Config) {
			params.put("config", struts2Config);
		}

		return new Dispatcher(servletContext, params);
	}

}