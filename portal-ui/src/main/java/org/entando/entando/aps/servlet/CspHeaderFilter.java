/*
 * Copyright 2026-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.servlet;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * Applies the Content-Security-Policy header to back-office requests
 * (/do/*, /struts/*, /ExtStr2/do/*), which do not route through the
 * {@link ControllerServlet} and would otherwise get no CSP at all.
 * <p>
 * The policy is read from the "cspBackofficeConfig" system parameter and honors
 * the same "cspEnabled" switch used for the portal. An optional "{nonce}"
 * placeholder in the policy is replaced with a per-request secure random token,
 * also exposed as the "cspNonceToken" request attribute for use in JSPs.
 * When the "cspBackofficeReportOnly" parameter is true, the policy is sent as
 * Content-Security-Policy-Report-Only, allowing violation monitoring without
 * enforcement during rollout.
 */
public class CspHeaderFilter implements Filter {

    public static final String CSP_HEADER_NAME = "Content-Security-Policy";
    public static final String CSP_REPORT_ONLY_HEADER_NAME = "Content-Security-Policy-Report-Only";
    public static final String NONCE_PLACEHOLDER = "{nonce}";
    public static final String REQUEST_ATTR_CSP_NONCE = "cspNonceToken";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            this.addCspHeader((HttpServletRequest) request, (HttpServletResponse) response);
        }
        chain.doFilter(request, response);
    }

    protected void addCspHeader(HttpServletRequest request, HttpServletResponse response) {
        ConfigInterface configManager = this.getConfigManager(request);
        if (!Boolean.parseBoolean(configManager.getParam(SystemConstants.PAR_CSP_ENABLED))) {
            return;
        }
        String policy = configManager.getParam(SystemConstants.PAR_CSP_BACKOFFICE_CONFIG);
        if (StringUtils.isBlank(policy)) {
            return;
        }
        if (policy.contains(NONCE_PLACEHOLDER)) {
            String nonce = CspNonceGenerator.createNonce();
            request.setAttribute(REQUEST_ATTR_CSP_NONCE, nonce);
            policy = policy.replace(NONCE_PLACEHOLDER, nonce);
        }
        boolean reportOnly = Boolean.parseBoolean(configManager.getParam(SystemConstants.PAR_CSP_BACKOFFICE_REPORT_ONLY));
        response.setHeader(reportOnly ? CSP_REPORT_ONLY_HEADER_NAME : CSP_HEADER_NAME, policy);
    }

    protected ConfigInterface getConfigManager(HttpServletRequest request) {
        return (ConfigInterface) ApsWebApplicationUtils.getBean(SystemConstants.BASE_CONFIG_MANAGER, request);
    }

}
