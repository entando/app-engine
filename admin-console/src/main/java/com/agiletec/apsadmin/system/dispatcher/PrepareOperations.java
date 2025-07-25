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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.dispatcher.Dispatcher;
import org.apache.struts2.dispatcher.mapper.ActionMapper;
import org.apache.struts2.dispatcher.mapper.ActionMapping;

/**
 * @author E.Santoboni
 */
public class PrepareOperations extends org.apache.struts2.dispatcher.PrepareOperations {
	
	public PrepareOperations(Dispatcher dispatcher) {
        super(dispatcher);
		this.dispatcher = dispatcher;
    }
	
	@Override
	public ActionMapping findActionMapping(HttpServletRequest request, HttpServletResponse response, boolean forceLookup) {
        ActionMapping mapping = null;
        Object mappingAttr = request.getAttribute(STRUTS_ACTION_MAPPING_KEY);
        if (mappingAttr != null && !forceLookup) {
            if (!"noActionMapping".equals(mappingAttr)) {
                mapping = (ActionMapping) mappingAttr;
            }
        } else {
            try {
                ActionMapper mapper = this.dispatcher.getActionMapper();
                String entandoActionName = EntandoActionUtils.extractEntandoActionName(request);
                mapping = mapper.getMapping(request, this.dispatcher.getConfigurationManager());
				if (null != entandoActionName) {
					mapping.setName(entandoActionName);
				}
                if (mapping != null) {
                    request.setAttribute(STRUTS_ACTION_MAPPING_KEY, mapping);
                } else {
                    request.setAttribute(STRUTS_ACTION_MAPPING_KEY, "noActionMapping");
                }
            } catch (Exception ex) {
                if (this.dispatcher.isHandleException() || this.dispatcher.isDevMode()) {
                    this.dispatcher.sendError(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
                }
            }
        }
        return mapping;
    }
	
	public Dispatcher getDispatcher() {
		return this.dispatcher;
	}
	
	private Dispatcher dispatcher;
	
	private static final String STRUTS_ACTION_MAPPING_KEY = "struts.actionMapping";
	
}
