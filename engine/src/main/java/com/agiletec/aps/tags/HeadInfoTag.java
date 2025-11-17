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

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.tags.util.HeadInfoContainer;
import freemarker.core.Environment;
import freemarker.ext.beans.StringModel;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.TagSupport;

/**
 * Tag for the declaration of the informations to insert in the header of the HTML page
 */
public class HeadInfoTag extends TagSupport {

	private String variable;
	private String info;
	private String type;

	@Override
	public int doEndTag() throws JspException {
		ServletRequest request =  this.pageContext.getRequest();
		RequestContext reqCtx = (RequestContext) request.getAttribute(RequestContext.REQCTX);
		try {
			HeadInfoContainer headInfo  = (HeadInfoContainer) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_HEAD_INFO_CONTAINER);
			if (type != null && (info != null || variable != null)){
				String infoValue;
				if (this.info != null) {
					infoValue = this.info;
				} else {
					infoValue = this.extractAttribute();
				}
				headInfo.addInfo(type, infoValue);
			}
		} catch (Exception t) {
			throw new JspException("Error closing tag ", t);
		}
		return super.doEndTag();
	}

    private String extractAttribute() throws JspException {
		if (null == this.getVar()) {
			return null;
		}
		String objectToString = null;
		try {
			Object object = this.pageContext.getAttribute(this.getVar());
			if (null == object) {
				Environment environment = Environment.getCurrentEnvironment();
				if (null != environment) {
					Object wrapper = environment.getVariable(this.getVar());
					if (null != wrapper) {
						if (wrapper instanceof StringModel) {
							object = ((StringModel) wrapper).getWrappedObject();
						} else {
							object = wrapper;
						}
					}
				}
			}
			if (null != object) {
				objectToString = object.toString();
			}
        } catch (Exception ex) {
            throw new JspException("Error extracting freemarker attribute", ex);
        }
		return objectToString;
    }

	@Override
	public void release() {
		type = null;
		variable = null;
		info = null;
	}

	public String getInfo() {
		return info;
	}
	public void setInfo(String info) {
		this.info = info;
	}

	public void setVar(String variable) {
		this.variable = variable;
	}
	public String getVar() {
		return variable;
	}

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}

}