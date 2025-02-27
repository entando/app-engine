/*
 * Copyright 2025-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

import lombok.Getter;
import lombok.Setter;
import org.entando.entando.aps.servlet.routing.VirtualContextHelper;
import org.entando.entando.aps.util.UrlUtils;
import org.entando.entando.aps.util.UrlUtils.EntUrlBuilder;
import org.entando.entando.ent.util.EntLogging;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * customUrlTag
 * <pre>
 *     Allows generating a tenant link with arbitrary base url/path and path.
 *     Handles:
 *     - absolute urls (user is responsible for valid tenant FQDN)
 *     - relative urls
 *     - tenant virtual-context is present
 * </pre>
 */
@Getter
@Setter
public class EntandoModuleUrlTag extends TagSupport {
    String baseUrl;
    String path;
    boolean withVirtualContext = true;

    @Override
    public int doEndTag() throws JspException {
        try {
            pageContext.getOut().print(this.composePath());
        } catch (Exception e) {
            throw new JspException("Error closing the tag", e);
        }
        return EVAL_PAGE;
    }

    private String composePath() {
        return ((UrlUtils.isNotAbsoluteURI(baseUrl) && !baseUrl.startsWith("/") && withVirtualContext)
                ? EntUrlBuilder.builder().paths(baseUrl, path)
                : EntUrlBuilder.builder().url(baseUrl).paths(this.getVirtualContextPath(), path)).build().toString();
    }

    private String getVirtualContextPath() {
        String res = VirtualContextHelper.getThreadLocal_VirtualContextPath();
        return (res == null || res.isEmpty()) ? "" : res;
    }

}
