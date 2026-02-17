package org.entando.entando.apsadmin.system;

import com.agiletec.aps.system.ApsSystemUtils;
import com.agiletec.aps.util.ApsTenantApplicationUtils;
import org.apache.struts2.ActionInvocation;
import org.apache.struts2.interceptor.AbstractInterceptor;
import org.slf4j.MDC;

public class MDCStrutsTenantInterceptor extends AbstractInterceptor {

    private static final String MDC_KEY_TENANT = "tenant";

    @Override
    public String intercept(ActionInvocation invocation) throws Exception {
        //try {
        //  ESB-805 - Adding logs for tenant
        ApsSystemUtils.ApsDeepDebug.print("TENANT", String.format("%s  - intercept - tenant %s",
                this.getClass().getSimpleName(), ApsTenantApplicationUtils.getTenant().orElse("primary")));
        MDC.put(MDC_KEY_TENANT, ApsTenantApplicationUtils.getTenant().orElse(""));
        return invocation.invoke();
        //} finally {
        // ESB-805 - Removed to preserve tenant
        //  MDC.remove(MDC_KEY_TENANT);
        //}
    }
}
