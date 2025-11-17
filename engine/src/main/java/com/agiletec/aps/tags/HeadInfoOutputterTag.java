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

import java.util.List;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.TagSupport;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.tags.util.HeadInfoContainer;

/**
 * Iterates over various information in HTML header displaying them. It retrieves data from a "HeadInfoContainer" object
 * located in the RequestContext. This tag works in conjunction with other sub-tag specific to the information type
 * Please note that the body can contain only a sub-tag, or information, at once.<br/> This tag must be used <b>only</b>
 * in a page template.
 */
@SuppressWarnings("serial")
public class HeadInfoOutputterTag extends TagSupport {

    private List<String> infos;
    private String type;
    private int index;

    @Override
    public int doStartTag() throws JspException {
        ServletRequest request = this.pageContext.getRequest();
        RequestContext reqCtx = (RequestContext) request.getAttribute(RequestContext.REQCTX);
        HeadInfoContainer headInfo = (HeadInfoContainer) reqCtx.getExtraParam(
                SystemConstants.EXTRAPAR_HEAD_INFO_CONTAINER);
        int retVal;
        List<String> infoValues = headInfo.getInfos(this.getType());
        if (infoValues != null) {
            this.infos = infoValues;
            this.index = 0;
            retVal = EVAL_BODY_INCLUDE;
        } else {
            retVal = SKIP_BODY;
        }
        return retVal;
    }

    @Override
    public int doAfterBody() throws JspException {
        int retVal;
        this.index++;
        if (this.index >= this.infos.size()) {
            retVal = SKIP_BODY;
        } else {
            retVal = EVAL_BODY_AGAIN;
        }
        return retVal;
    }

    public String getCurrentInfo() {
        return this.infos.get(this.index);
    }

    @Override
    public void release() {
        this.type = null;
        this.infos = null;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
