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
package com.agiletec.aps.tags;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.url.IURLManager;
import com.agiletec.aps.system.services.url.PageURL;
import com.agiletec.aps.tags.util.IParameterParentTag;
import com.agiletec.aps.util.ApsWebApplicationUtils;

/**
 * Build the URL to call a functionality of a servlet defined within the system.
 * This tag can use subtag ParameterTag to add url parameters.
 * The parameter 'path' must contain a path relative to the context of web-application
 * (es.: /do/myAction or /WEB-INF/jsp/myJSP.jsp)
 * @author M.Casari - E.Santoboni
 */
public class ActionURLTag extends TagSupport implements IParameterParentTag {

	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(ActionURLTag.class);
	
	@Override
	public int doStartTag() throws JspException {
		return EVAL_BODY_INCLUDE;
	}
	
	@Override
	public int doEndTag() throws JspException {	
		ServletRequest request =  this.pageContext.getRequest();
		RequestContext reqCtx = (RequestContext) request.getAttribute(RequestContext.REQCTX);
		IURLManager urlManager = (IURLManager) ApsWebApplicationUtils.getBean(SystemConstants.URL_MANAGER, this.pageContext);
		try {
			PageURL pageUrl = urlManager.createURL(reqCtx);
			IPage currPage = (IPage) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE);
			Integer currentFrame = (Integer) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_FRAME);
			pageUrl.setPage(currPage);
			pageUrl.addParam(InternalServletTag.REQUEST_PARAM_ACTIONPATH, this.getPath());
			pageUrl.addParam(InternalServletTag.REQUEST_PARAM_FRAMEDEST, currentFrame.toString());
			if (getEscapeAmp() != null) {
				pageUrl.setEscapeAmp(getEscapeAmp());
			}
			if (null != this.getParameters()) {
				Iterator<String> iter = this.getParameters().keySet().iterator();
				while (iter.hasNext()) {
					String name = (String) iter.next();
					pageUrl.addParam(name, this.getParameters().get(name));
				}
			}
			String path = pageUrl.getURL();
			if (null != this.getVar()) {
				this.pageContext.setAttribute(this.getVar(), path);
			} else {
				String encodedPath = org.owasp.encoder.Encode.forHtmlContent(path);
				this.pageContext.getOut().print(encodedPath);
			}
		} catch (IOException e) {
			_logger.error("Error closing tag", e);
			//ApsSystemUtils.logThrowable(e, this, "doEndTag");
			throw new JspException("Error closing tag ", e);
		}
		this.release();
		return EVAL_PAGE;
	}
	
	@Override
	public void release() {
		this.setPath(null);
		this.setVar(null);
		this._parameters = null;
	}
	
	/**
	 * Return the path related to the action or the page to invoke.
	 * @return The requested path
	 */
	public String getPath() {
		return _path;
	}
	
	/**
	 * Set the path related to the action or the page to invoke.
	 * @param path The path to invoke
	 */
	public void setPath(String path) {
		this._path = path;
	}

	/**
	 * Decide whether to encode the '&'
	 * @param escapeAmp
	 */
	public void setEscapeAmp(Boolean escapeAmp) {
		this._escapeAmp = escapeAmp;
	}

	public Map<String, String> getParameters() {
		return this._parameters;
	}
	
	@Override
	public void addParameter(String name, String value) {
		if (null == this._parameters) {
			this._parameters = new HashMap<String, String>();
		}
		this._parameters.put(name, value);
	}
	
	public String getVar() {
		return _var;
	}
	public void setVar(String var) {
		this._var = var;
	}

	public Boolean getEscapeAmp() {
		return _escapeAmp;
	}

	private String _path;
	private String _var;
	private Boolean _escapeAmp;
	
	private Map<String, String> _parameters;
	
}
