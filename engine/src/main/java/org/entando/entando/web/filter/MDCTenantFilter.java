package org.entando.entando.web.filter;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Slf4j
public class MDCTenantFilter extends HttpFilter {

    private static final String MDC_KEY_TENANT = "tenant";

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String tenant = ApsTenantApplicationUtils.getTenant()
                    .orElseGet(() -> ApsTenantApplicationUtils.extractCurrentTenantCode(request).orElse(""));
            log.trace("Adding to MDC the key:'{}' with value:'{}'", MDC_KEY_TENANT, tenant);
            MDC.put(MDC_KEY_TENANT, tenant);
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY_TENANT);
        }
    }
}
