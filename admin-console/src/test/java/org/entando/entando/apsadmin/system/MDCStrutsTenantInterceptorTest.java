package org.entando.entando.apsadmin.system;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import org.apache.struts2.ActionInvocation;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class MDCStrutsTenantInterceptorTest {

    @Mock
    private ActionInvocation invocation;

    @InjectMocks
    private MDCStrutsTenantInterceptor interceptor;

    @Test
    void shouldSetCurrentTenantToMDC() throws Exception {
        try (MockedStatic<MDC> mdc = Mockito.mockStatic(MDC.class);
                MockedStatic<ApsTenantApplicationUtils> tenantUtils
                        = Mockito.mockStatic(ApsTenantApplicationUtils.class)) {
            tenantUtils.when(() -> ApsTenantApplicationUtils.getTenant()).thenReturn(Optional.of("currentTenant"));
            interceptor.intercept(invocation);
            mdc.verify(() -> MDC.put("tenant", "currentTenant"));
            mdc.verify(() -> MDC.remove("tenant"));
            Mockito.verify(invocation, Mockito.times(1)).invoke();
        }
    }

    @Test
    void shouldSetPrimaryTenantToMDC() throws Exception {
        try (MockedStatic<MDC> mdc = Mockito.mockStatic(MDC.class);
                MockedStatic<ApsTenantApplicationUtils> tenantUtils
                        = Mockito.mockStatic(ApsTenantApplicationUtils.class)) {
            tenantUtils.when(() -> ApsTenantApplicationUtils.getTenant()).thenReturn(Optional.empty());
            interceptor.intercept(invocation);
            mdc.verify(() -> MDC.put("tenant", ""));
            mdc.verify(() -> MDC.remove("tenant"));
            Mockito.verify(invocation, Mockito.times(1)).invoke();
        }
    }

    @Test
    void shouldRemoveMDCKeyInCaseOfException() throws Exception {
        try (MockedStatic<MDC> mdc = Mockito.mockStatic(MDC.class);
                MockedStatic<ApsTenantApplicationUtils> tenantUtils
                        = Mockito.mockStatic(ApsTenantApplicationUtils.class)) {
            tenantUtils.when(() -> ApsTenantApplicationUtils.getTenant()).thenReturn(Optional.empty());
            Mockito.doThrow(NullPointerException.class).when(invocation).invoke();
            Assertions.assertThrows(NullPointerException.class, () -> interceptor.intercept(invocation));
            mdc.verify(() -> MDC.put("tenant", ""));
            mdc.verify(() -> MDC.remove("tenant"));
            Mockito.verify(invocation, Mockito.times(1)).invoke();
        }
    }
}
