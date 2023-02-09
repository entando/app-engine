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
package org.entando.entando.plugins.jpsolr.aps.tags;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.url.IURLManager;
import com.agiletec.aps.system.services.url.PageURL;
import com.agiletec.aps.tags.util.IParameterParentTag;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.commons.collections.CollectionUtils;

/**
 * Generates the URL to a portal page. The URL is either displayed or placed in a variable. The URL depends on the page
 * attributes and the given language otherwise the current values are used. Use the sub-tag "ParameterTag" to insert
 * parameters in the query string.
 */
public class URLTag extends TagSupport implements IParameterParentTag {

    /**
     * Prepares a PageURL object; this object may comprehend several sub-tags
     */
    @Override
    public int doStartTag() throws JspException {
        ServletRequest request = this.pageContext.getRequest();
        RequestContext reqCtx = (RequestContext) request.getAttribute(RequestContext.REQCTX);
        try {
            IURLManager urlManager =
                    (IURLManager) ApsWebApplicationUtils.getBean(SystemConstants.URL_MANAGER, this.pageContext);
            this.pageUrl = urlManager.createURL(reqCtx);
            if (pageCode != null) {
                pageUrl.setPageCode(pageCode);
            }
            if (langCode != null) {
                pageUrl.setLangCode(langCode);
            }
            this.pageUrl.setEscapeAmp(this.isEscapeAmp());
            if (this.isParamRepeat()) {
                List<String> exclusion = this.getParametersToExclude();
                pageUrl.setParamRepeat(exclusion);
            }
        } catch (RuntimeException ex) {
            throw new JspException("Error during tag initialization", ex);
        }
        return EVAL_BODY_INCLUDE;
    }

    /**
     * Completes the URL generation making it available for immediate output or placing it in a variable
     */
    @Override
    public int doEndTag() throws JspException {
        String url = pageUrl.getURL();
        if (this.getVar() != null) {
            this.pageContext.setAttribute(this.getVar(), url);
        } else {
            try {
                String encodedUrl = org.owasp.encoder.Encode.forHtmlContent(url);
                this.pageContext.getOut().print(encodedUrl);
            } catch (IOException | RuntimeException ex) {
                throw new JspException("Error closing tag", ex);
            }
        }
        return EVAL_PAGE;
    }

    @Override
    public void addParameter(String name, String value) {
        this.pageUrl.addParam(name, value);
    }

    protected List<String> getParametersToExclude() {
        List<String> parameters = new ArrayList<>();
        String csv = this.getExcludeParameters();
        if (null != csv && csv.trim().length() > 0) {
            CollectionUtils.addAll(parameters, csv.split(","));
        }
        parameters.add(SystemConstants.LOGIN_PASSWORD_PARAM_NAME);
        return parameters;
    }

    @Override
    public void release() {
        this.langCode = null;
        this.pageCode = null;
        this.varName = null;
        this.paramRepeat = false;
        this.escapeAmp = true;
        this.pageUrl = null;
        this.excludeParameters = null;
    }

    /**
     * Return the language code
     *
     * @return The literal code
     */
    public String getLang() {
        return langCode;
    }

    /**
     * Set the language code
     *
     * @param lang the literal code
     */
    public void setLang(String lang) {
        this.langCode = lang;
    }

    /**
     * Return the page code
     *
     * @return The page code
     */
    public String getPage() {
        return pageCode;
    }

    /**
     * Set the page code
     *
     * @param page The page code
     */
    public void setPage(String page) {
        this.pageCode = page;
    }

    /**
     * Return the name of the variable containing the generated URL.
     *
     * @return The name of the variable
     */
    public String getVar() {
        return varName;
    }

    /**
     * Set the name of the variable containing the generated URL.
     *
     * @param varName The name of the variable
     */
    public void setVar(String varName) {
        this.varName = varName;
    }

    /**
     * Repeats the parameters of the previous request when true, false otherwise.
     *
     * @return Returns the parRepeat.
     */
    public boolean isParamRepeat() {
        return paramRepeat;
    }

    /**
     * Toggles the repetition of the previous query string parameters
     *
     * @param paramRepeat True enables the repetition, false otherwise.
     */
    public void setParamRepeat(boolean paramRepeat) {
        this.paramRepeat = paramRepeat;
    }

    public boolean isEscapeAmp() {
        return escapeAmp;
    }

    public void setEscapeAmp(boolean escapeAmp) {
        this.escapeAmp = escapeAmp;
    }

    /**
     * Gets list of parameter names (comma separated) to exclude from repeating. By default, this attribute excludes
     * only the password parameter of the login form.
     *
     * @return the exclude list.
     */
    public String getExcludeParameters() {
        return excludeParameters;
    }

    /**
     * Sets the list of parameter names (comma separated) to exclude from repeating. By default, this attribute excludes
     * only the password parameter of the login form.
     *
     * @param excludeParameters the excludes list (comma separated).
     */
    public void setExcludeParameters(String excludeParameters) {
        this.excludeParameters = excludeParameters;
    }

    private String langCode;
    private String pageCode;
    private String varName;
    private boolean paramRepeat;
    private boolean escapeAmp = true;
    private PageURL pageUrl;
    private String excludeParameters;

}
