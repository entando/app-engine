package org.entando.entando.aps.servlet.security;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.*;

public class CustomWrappedRequest extends HttpServletRequestWrapper
{

    public static final String VIRTUAL_CONTEXT = "virtual-context";
    private final String virtualContextPath;
    private Map<String, String> headersOverrides = new HashMap<>();

    /**
     * Create a new request wrapper that will merge additional parameters into
     * the request object without prematurely reading parameters from the
     * original request.
     */
    public CustomWrappedRequest(
            final HttpServletRequest request,
            String virtualContextPath,
            final Map<String, String[]> additionalParams
    ) {
        super(request);
        this.virtualContextPath = ((virtualContextPath.startsWith("/")) ? "" : "/") + virtualContextPath;
    }

    @Override
    public String getContextPath() {
        return (virtualContextPath != null)
                ? virtualContextPath
                : getOriginalContextPath();
    }

    @Override
    public String getServletPath() {
        return stripVirtualContextIfRequired(this.getOriginalServletPath());
    }

    public boolean hasVirtualContext() {
        return virtualContextPath != null;
    }

    public String getOriginalContextPath() {
        return super.getContextPath();
    }

    public String getOriginalServletPath() {
        return super.getServletPath();
    }

    private String stripVirtualContextIfRequired(String path) {
        return (virtualContextPath != null)
                ? path.replaceFirst("^" + virtualContextPath + "/", "/")
                : path;
    }

    public static CustomWrappedRequest getCustomizedRequest(ServletRequest request) {
        if (request instanceof CustomWrappedRequest) {
            return (CustomWrappedRequest) request;
        } else if (request instanceof HttpServletRequestWrapper) {
            ServletRequest child = ((HttpServletRequestWrapper) request).getRequest();
            return (request == child) ? null : getCustomizedRequest(child);
        } else {
            return null;
        }
    }

//    @Override
//    public String getHeader(String name) {
//        String res = this.headersOverrides.get(name);
//        return (res == null) ? res : super.getHeader(name);
//    }
//
//    public void overrideHeader(String name, String value) {
//        this.headersOverrides.put(name, value);
//    }
}
