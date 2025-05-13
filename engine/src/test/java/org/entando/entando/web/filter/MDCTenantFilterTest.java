package org.entando.entando.web.filter;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
class MDCTenantFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @InjectMocks
    private MDCTenantFilter filter;

    @Test
    void shouldSetCurrentTenantToMDC() throws Exception {
        try (MockedStatic<MDC> mdc = Mockito.mockStatic(MDC.class);
                MockedStatic<ApsTenantApplicationUtils> tenantUtils
                        = Mockito.mockStatic(ApsTenantApplicationUtils.class)) {
            tenantUtils.when(ApsTenantApplicationUtils::getTenant).thenReturn(Optional.of("currentTenant"));
            filter.doFilter(request, response, chain);
            mdc.verify(() -> MDC.put("tenant", "currentTenant"));
            mdc.verify(() -> MDC.remove("tenant"));
            Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.any());
        }
    }

    @Test
    void shouldSetPrimaryTenantToMDC() throws Exception {
        try (MockedStatic<MDC> mdc = Mockito.mockStatic(MDC.class);
                MockedStatic<ApsTenantApplicationUtils> tenantUtils
                        = Mockito.mockStatic(ApsTenantApplicationUtils.class)) {
            tenantUtils.when(ApsTenantApplicationUtils::getTenant).thenReturn(Optional.empty());
            filter.doFilter(request, response, chain);
            mdc.verify(() -> MDC.put("tenant", ""));
            mdc.verify(() -> MDC.remove("tenant"));
            Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.any());
        }
    }

    @Test
    void shouldRemoveMDCKeyInCaseOfException() throws Exception {
        try (MockedStatic<MDC> mdc = Mockito.mockStatic(MDC.class);
                MockedStatic<ApsTenantApplicationUtils> tenantUtils
                        = Mockito.mockStatic(ApsTenantApplicationUtils.class)) {
            tenantUtils.when(ApsTenantApplicationUtils::getTenant).thenReturn(Optional.empty());
            Mockito.doThrow(NullPointerException.class).when(chain).doFilter(Mockito.any(), Mockito.any());
            Assertions.assertThrows(NullPointerException.class, () -> filter.doFilter(request, response, chain));
            mdc.verify(() -> MDC.put("tenant", ""));
            mdc.verify(() -> MDC.remove("tenant"));
            Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.any());
        }
    }

}
