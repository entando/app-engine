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
package com.agiletec.aps.system.services.controller.control;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.services.controller.ControllerManager;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.url.PageURL;

/**
 * Implementazione del sottoservizio di controllo che gestisce gli errori
 * @author M.Diana
 */
public class ErrorManager extends AbstractControlService {

	private static final Logger _logger = LoggerFactory.getLogger(ErrorManager.class);
	
	@Override
	public int service(RequestContext reqCtx, int status) {
		if (status == ControllerManager.CONTINUE || status == ControllerManager.OUTPUT) {
			return ControllerManager.OUTPUT;
		} 
		int retStatus = ControllerManager.INVALID_STATUS;
		_logger.debug("Intervention of the error service");
		try {
			PageURL url = this.getUrlManager().createURL(reqCtx);
			url.setPageCode(this.getErrorPageCode());
			String redirUrl = url.getURL();

			_logger.debug("Redirecting to " + redirUrl);
			reqCtx.clearError();
			reqCtx.addExtraParam(RequestContext.EXTRAPAR_REDIRECT_URL, redirUrl);
			retStatus = ControllerManager.REDIRECT;
		} catch (Throwable t) {
			_logger.debug("Error detected while processing the request", t);
			retStatus = ControllerManager.SYS_ERROR;
			reqCtx.setHTTPError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		}
		return retStatus;
	}
	
	protected String getErrorPageCode() {
		return this.getPageManager().getConfig(IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE);
	}
	
}
