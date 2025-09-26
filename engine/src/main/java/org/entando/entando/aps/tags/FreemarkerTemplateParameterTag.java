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
package org.entando.entando.aps.tags;

import com.agiletec.aps.system.RequestContext;
import freemarker.core.Environment;
import freemarker.ext.beans.StringModel;
import freemarker.ext.servlet.AllHttpScopesHashModel;
import freemarker.template.TemplateModel;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.TagSupport;
import org.entando.entando.aps.system.services.controller.executor.ExecutorBeanContainer;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;

/**
 * Add a parameter into the Freemarker's TemplateModel Map
 * @author E.Santoboni
 * @deprecated remove it from every freemarker template (page template and fragments) and substitute @wp.fragments with #include directive
 */
public class FreemarkerTemplateParameterTag extends TagSupport {
	
	private static final EntLogger _logger =  EntLogFactory.getSanitizedLogger(FreemarkerTemplateParameterTag.class);
	
	@Override
    public int doStartTag() throws JspException {
		_logger.warn("** TAG FreemarkerTemplateParameterTag DEPRECATED ** - "
				+ "remove it from every freemarker template (page template and fragments) and substitute @wp.fragments with #include directive");
		ServletRequest request = this.pageContext.getRequest();
		RequestContext reqCtx = (RequestContext) request.getAttribute(RequestContext.REQCTX);
		try {
			ExecutorBeanContainer ebc = reqCtx.getExecutorBeanContainer();
			TemplateModel templateModel = ebc.getTemplateModel();
			if (!(templateModel instanceof AllHttpScopesHashModel)) {
				return EVAL_BODY_INCLUDE;
			}
			AllHttpScopesHashModel hashModel = (AllHttpScopesHashModel) templateModel;
			Object object = this.pageContext.getAttribute(this.getValueName());
			if (null == object) {
				Environment environment = Environment.getCurrentEnvironment();
				if (null != environment) {
					Object wrapper = environment.getVariable(this.getValueName());
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
				hashModel.put(this.getVar(), object);
			}
        } catch (Throwable t) {
        	_logger.error("error in doStartTag", t);
            throw new JspException("Error during tag initialization", t);
        }
        return EVAL_BODY_INCLUDE;
    }
	
	@Override
	public int doEndTag() throws JspException {
		if (this.isRemoveOnEndTag()) {
			ServletRequest request = this.pageContext.getRequest();
			RequestContext reqCtx = (RequestContext) request.getAttribute(RequestContext.REQCTX);
			try {
				ExecutorBeanContainer ebc = reqCtx.getExecutorBeanContainer();
				TemplateModel templateModel = ebc.getTemplateModel();
				if (templateModel instanceof AllHttpScopesHashModel) {
					AllHttpScopesHashModel hashModel = (AllHttpScopesHashModel) templateModel;
					hashModel.remove(this.getVar());
				}
			} catch (Throwable t) {
				_logger.error("error in doEndTag", t);
				throw new JspException("Error evaluating di end tag", t);
			}
		}
		this.release();
		return EVAL_PAGE;
	}
	
	@Override
	public void release() {
		this.setVar(null);
		this.setValueName(null);
		this.setRemoveOnEndTag(false);
	}
	
	public String getVar() {
		return _var;
	}
	public void setVar(String var) {
		this._var = var;
	}
	
	public String getValueName() {
		return valueName;
	}
	public void setValueName(String valueName) {
		this.valueName = valueName;
	}
	
	public boolean isRemoveOnEndTag() {
		return _removeOnEndTag;
	}
	public void setRemoveOnEndTag(boolean removeOnEndTag) {
		this._removeOnEndTag = removeOnEndTag;
	}
	
	private String _var;
	private String valueName;
	private boolean _removeOnEndTag = false;
	
}