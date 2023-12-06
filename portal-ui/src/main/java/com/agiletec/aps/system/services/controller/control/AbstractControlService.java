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

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.services.controller.ControllerManager;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.url.IURLManager;
import com.agiletec.aps.system.services.url.PageURL;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Classe di utilità che implementa un metodo per impostare una redirezione ed
 * un metodo per recuperare un parametro singolo dall'HttpServletRequest.
 * @author M.Diana - E.Santoboni
 */
public abstract class AbstractControlService implements ControlServiceInterface {

	private static final Logger _logger = LoggerFactory.getLogger(AbstractControlService.class);
	
	private transient IURLManager urlManager;
    private transient IPageManager pageManager;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        _logger.debug("{} ready", this.getClass().getName());
    }
	
	/**
	 * Imposta i parametri di una redirezione.
	 * @param redirDestPage Il codice della pagina su cui si vuole redirezionare.
	 * @param reqCtx Il contesto di richiesta.
	 * @return L'indicativo del tipo di redirezione in uscita del controlService. 
	 * Può essere una delle costanti definite in ControllerManager.
	 */
	protected int redirect(String redirDestPage, RequestContext reqCtx) {
		return this.redirect(redirDestPage, null, reqCtx);
	}
	
	protected int redirect(String redirDestPage, Map<String, String> params, RequestContext reqCtx) {
		int retStatus;
		try {
			String redirPar = this.getParameter(RequestContext.PAR_REDIRECT_FLAG, reqCtx);
			if (redirPar == null || "".equals(redirPar)) {
				PageURL url = this.getUrlManager().createURL(reqCtx);
				url.setPageCode(redirDestPage);
				if (null != params && !params.isEmpty()) {
					Iterator<String> iter = params.keySet().iterator();
					while (iter.hasNext()) {
						String key = iter.next();
						url.addParam(key, params.get(key));
					}
				}
				url.addParam(RequestContext.PAR_REDIRECT_FLAG, "1");
				url.setEscapeAmp(false);
				String redirUrl = url.getURL();
				_logger.debug("Redirecting to {}", redirUrl);
				return this.redirectUrl(redirUrl, reqCtx);
			} else {
				reqCtx.setHTTPError(HttpServletResponse.SC_BAD_REQUEST);
				retStatus = ControllerManager.ERROR;
			}
		} catch (Throwable t) {
			retStatus = ControllerManager.SYS_ERROR;
			reqCtx.setHTTPError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			_logger.error("Error on creation redirect to page {}", redirDestPage, t);
		}
		return retStatus;
	}
	
	protected int redirectUrl(String urlDest, RequestContext reqCtx) {
		int retStatus;
		try {
			reqCtx.clearError();
			reqCtx.addExtraParam(RequestContext.EXTRAPAR_REDIRECT_URL, urlDest);
			retStatus = ControllerManager.REDIRECT;
		} catch (Throwable t) {
			retStatus = ControllerManager.SYS_ERROR;
			reqCtx.setHTTPError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			_logger.error("Error on creation redirect to url {}", urlDest, t);
		}
		return retStatus;
	}
	
	/**
	 * Recupera un parametro della richiesta.
	 * @param name Il nome del parametro.
	 * @param reqCtx Il contesto di richiesta.
	 * @return Il valore del parametro
	 */
	protected String getParameter(String name, RequestContext reqCtx){
		return reqCtx.getRequest().getParameter(name);
	}
	
	protected IURLManager getUrlManager() {
		return urlManager;
	}
	@Autowired
	public void setUrlManager(IURLManager urlManager) {
		this.urlManager = urlManager;
	}

	protected IPageManager getPageManager() {
		return pageManager;
	}
	@Autowired
	public void setPageManager(IPageManager pageManager) {
		this.pageManager = pageManager;
	}
	
}
