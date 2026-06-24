package org.entando.entando.apsadmin.system;

import com.agiletec.aps.system.ApsSystemUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.struts2.ActionInvocation;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.dispatcher.HttpParameters;
import org.apache.struts2.interceptor.AbstractInterceptor;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;

public class RequestLoggingInterceptor extends AbstractInterceptor {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(RequestLoggingInterceptor.class);

    private static final java.util.regex.Pattern HTML_INJECTION_PATTERN =
        java.util.regex.Pattern.compile(
            "<[a-zA-Z][a-zA-Z0-9]*[^>]*>.*?</[a-zA-Z][^>]*>|<[^>]*>|javascript\\s*:|on[a-zA-Z]+\\s*=",
            java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL
        );

    @Override
    public String intercept(ActionInvocation invocation) throws Exception {
        long start = System.nanoTime();
        try {
            HttpServletRequest request = ServletActionContext.getRequest();
            if (request.getRequestURI().contains("/do/")) {
                String requestUri = request.getRequestURI();
                RequestLoggingInterceptorConfig config = getInterceptorConfig(request);
                Set<String> excluded = new HashSet<>();
                if (config != null) {
                    for (RequestLoggingInterceptorConfig.Exclusion e : config.getExclusions()) {
                        if (e.matchesAction(requestUri) && e.getParameters() != null) {
                            for (String param : e.getParameters()) {
                                excluded.add(param);
                                ApsSystemUtils.ApsDeepDebug.print("ACTIONINTERCEPTOR",
                                    "Parameter '" + param + "' excluded from sanitization for: " + requestUri);
                            }
                        }
                    }
                }
                SanitizingRequestWrapper wrapper = new SanitizingRequestWrapper(request, excluded);
                invocation.getInvocationContext()
                    .withServletRequest(wrapper)
                    .withParameters(HttpParameters.create(wrapper.getParameterMap()).build());
            }
        } catch (Exception ex) {
            logger.error("Error in RequestLoggingInterceptor security check", ex);
        }
        String result = invocation.invoke();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
//        logger.info("Request {} completed in {} ms", ServletActionContext.getRequest().getRequestURI(), elapsedMs);
        ApsSystemUtils.ApsDeepDebug.print("ACTIONINTERCEPTOR", "Request completed in " + elapsedMs + " ms!");
        return result;
    }

    private static String[] stripValues(String name, String[] values) {
        if (values == null) {
            return null;
        }
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null && HTML_INJECTION_PATTERN.matcher(values[i]).find()) {
//                logger.warn("HTML injection stripped from parameter '{}'", name);
                ApsSystemUtils.ApsDeepDebug.print("ACTIONINTERCEPTOR", "HTML injection stripped from parameter '" + name + "'");
                result[i] = HTML_INJECTION_PATTERN.matcher(values[i]).replaceAll("")
                        .replace("\"", "").replace("'", "");
                ApsSystemUtils.ApsDeepDebug.print("ACTIONINTERCEPTOR", "result: '" + result[i] + "'");
            } else {
                result[i] = values[i];
            }
        }
        return result;
    }

    private static final class SanitizingRequestWrapper extends HttpServletRequestWrapper {

        private final Set<String> excluded;

        SanitizingRequestWrapper(HttpServletRequest request, Set<String> excluded) {
            super(request);
            this.excluded = excluded;
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            if (name.contains(":") || value == null || excluded.contains(name)) {
                return value;
            }
            if (!HTML_INJECTION_PATTERN.matcher(value).find()) {
                return value;
            }
            return HTML_INJECTION_PATTERN.matcher(value).replaceAll("")
                    .replace("\"", "").replace("'", "");
        }

        @Override
        public String[] getParameterValues(String name) {
            return (name.contains(":") || excluded.contains(name))
                ? super.getParameterValues(name)
                : stripValues(name, super.getParameterValues(name));
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            Map<String, String[]> result = new HashMap<>(super.getParameterMap().size());
            for (Map.Entry<String, String[]> entry : super.getParameterMap().entrySet()) {
                result.put(entry.getKey(),
                    (entry.getKey().contains(":") || excluded.contains(entry.getKey()))
                        ? entry.getValue()
                        : stripValues(entry.getKey(), entry.getValue()));
            }
            return Collections.unmodifiableMap(result);
        }
    }

    private static RequestLoggingInterceptorConfig getInterceptorConfig(HttpServletRequest request) {
        try {
            return (RequestLoggingInterceptorConfig)
                ApsWebApplicationUtils.getBean("RequestLoggingInterceptorConfig", request);
        } catch (Exception ex) {
            return null;
        }
    }
}
