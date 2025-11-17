/*
 * Copyright 2022-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.apsadmin.tags;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import jakarta.servlet.jsp.JspException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionTagTenantAware extends org.apache.struts2.views.jsp.ActionTag {

    private static Logger logger = LoggerFactory.getLogger(ActionTagTenantAware.class);

    private String prevTenantCode;

    @Override
    public int doStartTag() throws JspException {
        ApsTenantApplicationUtils.getTenant()
                .filter(StringUtils::isNotBlank)
                .ifPresent(tenantCode -> this.prevTenantCode = tenantCode);
        logger.trace("Tag tenant aware start saved previous tenant code:'{}'", prevTenantCode);
        return super.doStartTag();
    }

    @Override
    public int doEndTag() throws JspException {
        int result = super.doEndTag();
        if (StringUtils.isNotBlank(this.prevTenantCode)) {
            ApsTenantApplicationUtils.setTenant(this.prevTenantCode);
            logger.trace("Tag tenant aware end reset previous tenant code:'{}'", prevTenantCode);
        }
        this.prevTenantCode = null;
        return result;
    }

}