package org.entando.entando.apsadmin.system;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import org.apache.struts2.ActionInvocation;
import org.apache.struts2.interceptor.AbstractInterceptor;
import org.slf4j.MDC;

public class MDCStrutsTenantInterceptor extends AbstractInterceptor {

    private static final String MDC_KEY_TENANT = "tenant";

    @Override
    public String intercept(ActionInvocation invocation) throws Exception {
        try {
            MDC.put(MDC_KEY_TENANT, ApsTenantApplicationUtils.getTenant().orElse(""));
            return invocation.invoke();
        } finally {
            MDC.remove(MDC_KEY_TENANT);
        }
    }
}
