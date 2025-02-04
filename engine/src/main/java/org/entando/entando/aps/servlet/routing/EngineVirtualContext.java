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
package org.entando.entando.aps.servlet.routing;

import com.agiletec.aps.system.SystemConstants;
import org.entando.entando.aps.servlet.security.CustomWrappedRequest;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.springframework.lang.NonNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class EngineVirtualContext {

    private static final EntLogger log = EntLogFactory.getSanitizedLogger(EngineVirtualContext.class);
    public static final String ENABLED_VIRTUAL_CONTEXTS = System.getenv(SystemConstants.ENTANDO_VIRTUAL_CONTEXTS);


    public static HttpServletRequest applyVirtualContext(final HttpServletRequest servletRequest,
                                                         @NonNull final HttpServletResponse servletResponse) {
        debugBefore(servletRequest);
        HttpServletRequest customRequest = customizeRequest(servletRequest);
        debugAfter(customRequest);
        return customRequest;
    }

    private static void debugAfter(HttpServletRequest customRequest) {
        System.out.println(" * FILTER: Virtual ContextPath: `" + customRequest.getContextPath() + "`");
        System.out.println(" * FILTER: Virtual ServletPath: " + customRequest.getServletPath());
    }

    private static void debugBefore(HttpServletRequest servletRequest) {
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

        System.out.println(" * FILTER: Real ContextPath: `" + servletRequest.getContextPath() + "`");
        System.out.println(" * FILTER: Real ServletPath: " + servletRequest.getServletPath());
    }

    /**
     * This method modifies on the fly, by wrapping it, the REQUEST
     *
     * @param request the original request
     * @return the wrapped request if modification criteria are met, the original request otherwise
     */
    public static HttpServletRequest customizeRequest(HttpServletRequest request) {
        Map<String, String[]> param = new TreeMap<>();

        List<String> virtualContexts = getVirtualContexts();
        System.out.println(" * FILTER: Virtual Contexts Enabled: " + virtualContexts);

        if (!virtualContexts.isEmpty()) {

            String[] parts = request.getServletPath().split("/");
            if (parts.length >= 2 && virtualContexts.contains(parts[1])) {
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

    public static List<String> getVirtualContexts() {
        String virtualContextsAsString = ENABLED_VIRTUAL_CONTEXTS;
        if (virtualContextsAsString != null) {
            return Arrays.asList(virtualContextsAsString.split(SystemConstants.SEPARATOR_CONTEXTS, -1));
        }
        return Collections.emptyList();
    }

}
