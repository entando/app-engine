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
package org.entando.entando.apsadmin.system;

import com.agiletec.aps.system.EntThreadLocal;
import com.agiletec.aps.util.ApsTenantApplicationUtils;
import org.apache.struts2.ActionInvocation;
import org.apache.struts2.interceptor.AbstractInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.struts2.ServletActionContext;

public class MultitenancyStrutsInterceptor extends AbstractInterceptor {

    @Override
    public String intercept(ActionInvocation invocation) throws Exception {

        try {
            EntThreadLocal.clear();

            HttpServletRequest request = ServletActionContext.getRequest();
            ApsTenantApplicationUtils.extractCurrentTenantCode(request)
                    .ifPresentOrElse(ApsTenantApplicationUtils::setTenant,ApsTenantApplicationUtils::removeTenant);

            return invocation.invoke();

        } finally {
            ApsTenantApplicationUtils.removeTenant();
        }
    }
}