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
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts2.ServletActionContext;
import org.entando.entando.aps.servlet.routing.VirtualContextHelper;

public class MultitenancyStrutsInterceptor extends AbstractInterceptor {

    @Override
    public String intercept(ActionInvocation invocation) throws Exception {

        try {
            EntThreadLocal.clear();

            HttpServletRequest request = ServletActionContext.getRequest();

            setupTenantInfo(request);
            setupVirtualContextInfo(request);

            return invocation.invoke();

        } finally {
            ApsTenantApplicationUtils.removeTenant();
            VirtualContextHelper.setThreadLocal_VirtualContextPath(null);
        }
    }

    private static void setupVirtualContextInfo(HttpServletRequest request) {
        VirtualContextHelper.setupThreadLocalStorage(request);
    }

    private static void setupTenantInfo(HttpServletRequest request) {
        ApsTenantApplicationUtils.extractCurrentTenantCode(request)
                .ifPresentOrElse(ApsTenantApplicationUtils::setTenant, ApsTenantApplicationUtils::removeTenant);
    }
}