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

import com.agiletec.aps.system.EntThreadLocal;
import com.agiletec.aps.system.SystemConstants;
import org.entando.entando.aps.servlet.security.CustomWrappedRequest;
import org.entando.entando.aps.util.UrlUtils;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

public class VirtualContextHelper {

    private static final EntLogger log = EntLogFactory.getSanitizedLogger(VirtualContextHelper.class);
    public static final String ENABLED_VIRTUAL_CONTEXTS = System.getenv(SystemConstants.ENTANDO_VIRTUAL_CONTEXTS);


    /**
     * Returns a customized wrapper of the provided request
     */
    public static HttpServletRequest customizeRequest(final HttpServletRequest originalRequest) {
        HttpServletRequest customizedRequest = applyVirtualContext(originalRequest);
        debugRequest(originalRequest, customizedRequest);
        return customizedRequest;
    }

    /**
     * This method modifies on the fly, by wrapping it, the REQUEST
     *
     * @param request the original request
     * @return the wrapped request if modification criteria are met, the original request otherwise
     */
    private static HttpServletRequest applyVirtualContext(HttpServletRequest request) {
        List<String> allowedVirtualContexts = getVirtualContexts();

        if (allowedVirtualContexts.isEmpty()) {
            return request;
        }

        String[] parts = request.getServletPath().split("/");
        String requestVirtualContext = (parts.length >= 2) ? parts[1] : null;

        if (requestVirtualContext == null || !allowedVirtualContexts.contains(requestVirtualContext)) {
            return request;
        }

        return new CustomWrappedRequest(request, requestVirtualContext, new TreeMap<>());
    }

    public static List<String> getVirtualContexts() {
        String virtualContextsAsString = ENABLED_VIRTUAL_CONTEXTS;
        if (virtualContextsAsString != null) {
            return Arrays.stream(virtualContextsAsString.split(SystemConstants.SEPARATOR_CONTEXTS, -1)).map(
                    vc -> vc.equals(SystemConstants.VIRTUAL_CONTEXT_ROOT_ALIAS) ? "" : vc
            ).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public static void setupThreadLocalStorage(HttpServletRequest request) {
        try {
            CustomWrappedRequest customizedRequest = CustomWrappedRequest.getCustomizedRequest(request);
            if (customizedRequest != null && customizedRequest.hasVirtualContext()) {
                setThreadLocal_VirtualContextPath(customizedRequest.getContextPath());
            }
        } catch (Exception e) {
            log.error("Error setting the thread local storage", e);
        }
    }

    public static void setThreadLocal_VirtualContextPath(String virtualContextPath) {
        EntThreadLocal.set(THREAD_LOCAL_VIRTUAL_CONTEXT, virtualContextPath);
    }

    public static String getThreadLocal_VirtualContextPath() {
        return (String) EntThreadLocal.get(THREAD_LOCAL_VIRTUAL_CONTEXT);
    }

    private static final String THREAD_LOCAL_VIRTUAL_CONTEXT = "threadLocal_VirtualContextPath";

    private static void debugRequest(HttpServletRequest originalRequest, HttpServletRequest customizedRequest) {
        log.trace("Original ServletPath: {}", originalRequest.getServletPath());
        log.trace("Original ContextPath: {}", originalRequest.getContextPath());
        log.trace("Customized ContextPath: {}", customizedRequest.getContextPath());
        log.trace("Customized ServletPath: {}", customizedRequest.getServletPath());
    }

    public static String contextPathToContext(String contextPath) {
        if (contextPath == null) return null;
        return contextPath.replaceAll("/$", "").replaceAll("^/", "");
    }

    /**
     * Prepends the current request's virtual-context path (e.g. {@code /tenant1}) to an app-local,
     * relative URL, so that resources and links resolve to the correct tenant when virtual contexts
     * are enabled. This is the single, canonical way to virtual-context-scope a generated URL and it
     * must be used by every place that builds an app-local URL (tags, resource model, redirects, ...).
     *
     * <p>Rules:
     * <ul>
     *   <li>absolute URIs (http(s)://, //host, CDS URLs) are returned unchanged;</li>
     *   <li>when there is no current virtual context (primary/master tenant) the path is returned unchanged;</li>
     *   <li>a path already scoped to the current virtual context is not prefixed twice;</li>
     *   <li>the result keeps a single leading slash.</li>
     * </ul>
     *
     * @param path an app-local URL (e.g. {@code /protected/42/0/it/} or {@code /resources/...}) or an absolute URL
     * @return the URL prefixed with the current virtual context when applicable, otherwise unchanged
     */
    public static String addVirtualContextToPath(String path) {
        if (path == null || !UrlUtils.isNotAbsoluteURI(path)) {
            return path;
        }
        String virtualContext = getThreadLocal_VirtualContextPath();
        if (virtualContext == null || virtualContext.isEmpty()) {
            return path;
        }
        String prefix = virtualContext.startsWith("/") ? virtualContext : "/" + virtualContext;
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        if (normalizedPath.equals(prefix) || normalizedPath.startsWith(prefix + "/")) {
            return normalizedPath; // already virtual-context scoped: do not prefix twice
        }
        return prefix + normalizedPath;
    }
}
