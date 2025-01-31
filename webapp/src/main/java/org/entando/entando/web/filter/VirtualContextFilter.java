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
import com.agiletec.aps.system.SystemConstants;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.web.CustomWrappedRequest;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

public class VirtualContextFilter implements Filter {

    private static final EntLogger log = EntLogFactory.getSanitizedLogger(VirtualContextFilter.class);

    public VirtualContextFilter() {
    }

    @Override
    public void doFilter(final ServletRequest servletRequest,
                         final ServletResponse servletResponse,
                         final FilterChain chain) throws IOException, ServletException {
        try {
            EntThreadLocal.clear();

            System.out.println(" * FILTER: ContextPath: `" + ((HttpServletRequest)servletRequest).getContextPath() + "`");
            System.out.println(" * FILTER: ServletPath: " + ((HttpServletRequest)servletRequest).getServletPath());

            HttpServletRequest customRequest = customizeRequest((HttpServletRequest) servletRequest);

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

        List<String> virtualContexts = getVirtualContexts();
        System.out.println(" * FILTER: Virtual Contexts Enabled: " + virtualContexts);

        if(!virtualContexts.isEmpty()) {
            String[] parts = request.getServletPath().split("/");
            if (parts.length >= 3 && virtualContexts.contains(parts[1])) {
                String virtualContextPath = "/" + parts[1];
                System.out.println(" * FILTER: virtualContextPath: " + virtualContextPath);
                param.put(CustomWrappedRequest.VIRTUAL_CONTEXT, new String[]{virtualContextPath});
            } else {
                // Lanciare special error
                System.out.println(" * * * * * * * * * * * * PATH NON GESTITO * * * * * * * * * * * * ");
            }
        }

        return new CustomWrappedRequest(request, param);
    }

    private List<String> getVirtualContexts() {
        String virtualContextsAsString = System.getenv(SystemConstants.ENTANDO_VIRTUAL_CONTEXTS);
        if(virtualContextsAsString != null) {
            return Arrays.asList(virtualContextsAsString.split(SystemConstants.SEPARATOR_CONTEXTS));
        }
        return Collections.emptyList();
    }

}
