package org.entando.entando.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

public class CustomWrappedRequest extends HttpServletRequestWrapper
{

    public static final String VIRTUAL_CONTEXT = "virtual";

    private final Map<String, String[]> modifiableParameters;
    private Map<String, String[]> allParameters = null;

    /**
     * Create a new request wrapper that will merge additional parameters into
     * the request object without prematurely reading parameters from the
     * original request.
     *
     * @param request
     * @param additionalParams
     */
    public CustomWrappedRequest(final HttpServletRequest request,
                                final Map<String, String[]> additionalParams)
    {
        super(request);
        modifiableParameters = new TreeMap<String, String[]>();
        modifiableParameters.putAll(additionalParams);
    }

    @Override
    public String getParameter(final String name)
    {
        String[] strings = getParameterMap().get(name);
        if (strings != null)
        {
            return strings[0];
        }
        return super.getParameter(name);
    }

    @Override
    public Map<String, String[]> getParameterMap()
    {
        if (allParameters == null)
        {
            allParameters = new TreeMap<String, String[]>();
            allParameters.putAll(super.getParameterMap());
            allParameters.putAll(modifiableParameters);
        }
        //Return an unmodifiable collection because we need to uphold the interface contract.
        return Collections.unmodifiableMap(allParameters);
    }

    @Override
    public Enumeration<String> getParameterNames()
    {
        return Collections.enumeration(getParameterMap().keySet());
    }

    @Override
    public String[] getParameterValues(final String name)
    {
        return getParameterMap().get(name);
    }

    @Override
    public String getContextPath() {
        String origContextPath = super.getContextPath();

        if(getParameterMap().get(CustomWrappedRequest.VIRTUAL_CONTEXT) != null) {
            String virtualContextPath = getParameterMap().get(CustomWrappedRequest.VIRTUAL_CONTEXT)[0];
            System.out.println(" ** context path: `" + virtualContextPath + "`");
            return virtualContextPath;
        }

        System.out.println(" ** context path: `" + origContextPath + "`");
        return origContextPath;
    }

    @Override
    public String getServletPath() {
        String origPath = super.getServletPath();

        if(getParameterMap().get(CustomWrappedRequest.VIRTUAL_CONTEXT) != null) {
            String virtualPath = origPath.replaceFirst(getParameterMap().get(CustomWrappedRequest.VIRTUAL_CONTEXT)[0], "");
            System.out.println(" ** virtualPath: " + virtualPath );
            return virtualPath;
        }
        System.out.println(" ** currentPath: " + origPath);

        return origPath;
    }

}
