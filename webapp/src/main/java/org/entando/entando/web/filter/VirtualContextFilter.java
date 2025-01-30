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
package org.entando.entando.web.filter;

import com.agiletec.aps.system.EntThreadLocal;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.web.CustomWrappedRequest;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class VirtualContextFilter implements Filter {

    private static final EntLogger log = EntLogFactory.getSanitizedLogger(VirtualContextFilter.class);

    public static final String VIRTUAL_CONTEXT = "virtual-context";

    public VirtualContextFilter() {
    }

    @Override
    public void doFilter(final ServletRequest servletRequest,
                         final ServletResponse servletResponse,
                         final FilterChain chain) throws IOException, ServletException {
        try {
            EntThreadLocal.clear();

            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletRequest customRequest = customizeRequest(request);

            chain.doFilter(customRequest, servletResponse);
        } finally {
            // moved here to clean context everytime also if we have an exception
            EntThreadLocal.destroy();
        }
    }

    /**
     * This method modifies on the fly, by wrapping it, the REQUEST
     *
     * @param request the original request
     * @return the wrapped request if modification criteria are met, the original request otherwise
     */
    private HttpServletRequest customizeRequest(HttpServletRequest request) {
        Map<String, String[]> param = new TreeMap<>();

        // Virtual Context
        log.error("request.getContextPath(): `{}`", request.getContextPath());
        if(request.getContextPath().equals("")) {

            String[] parts = request.getServletPath().split("/");
            if(parts.length > 0) {
                log.error("parts[1]: {}", parts[1]);
                switch (parts[1]) {
                    case "api":
                    case "altro_path_noto_1":
                    case "altro_path_noto_2":
                        break;
                    case "opendata":
                        param.put(VIRTUAL_CONTEXT, new String[]{parts[1]});
                    default:
                        System.out.println("#### " + request.getContextPath());
                }
            }
        }

        return new CustomWrappedRequest(request, param);
    }

}
