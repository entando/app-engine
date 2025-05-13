package org.entando.entando.web.filter;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MDCTenantFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private RequestDispatcher dispatcher;

    @InjectMocks
    private MDCTenantFilter filter;

    @Test
    void shouldSetCurrentTenantToMDC() throws Exception {
        try (MockedStatic<MDC> mdc = Mockito.mockStatic(MDC.class);
                MockedStatic<ApsTenantApplicationUtils> tenantUtils
                        = Mockito.mockStatic(ApsTenantApplicationUtils.class)) {
            doReturn("/sub-path").when(this.request).getServletPath();
            tenantUtils.when(ApsTenantApplicationUtils::getTenant).thenReturn(Optional.of("currentTenant"));
            filter.doFilter(request, response, chain);
            mdc.verify(() -> MDC.put("tenant", "currentTenant"));
            mdc.verify(() -> MDC.remove("tenant"));
            Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.any());
            verifyNoInteractions(this.dispatcher);
        }
    }

    @Test
    void onTenantRootShouldSetCurrentTenantToMDCAndDispatch() throws Exception {
        try (MockedStatic<MDC> mdc = Mockito.mockStatic(MDC.class);
             MockedStatic<ApsTenantApplicationUtils> tenantUtils
                     = Mockito.mockStatic(ApsTenantApplicationUtils.class)) {
            doReturn("/").when(this.request).getServletPath();
            tenantUtils.when(ApsTenantApplicationUtils::getTenant).thenReturn(Optional.of("currentTenant"));
            doReturn(this.dispatcher).when(this.request).getRequestDispatcher(any());
            filter.doFilter(request, response, chain);
            mdc.verify(() -> MDC.put("tenant", "currentTenant"));
            mdc.verify(() -> MDC.remove("tenant"));
            Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.any());
            verify(this.request).getRequestDispatcher("/index.jsp");
            verify(this.dispatcher).forward(this.request, this.response);
        }
    }

    @Test
    void shouldSetPrimaryTenantToMDC() throws Exception {
        try (MockedStatic<MDC> mdc = Mockito.mockStatic(MDC.class);
                MockedStatic<ApsTenantApplicationUtils> tenantUtils
                        = Mockito.mockStatic(ApsTenantApplicationUtils.class)) {
            doReturn("/sub-path").when(this.request).getServletPath();
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
            doReturn("/sub-path").when(this.request).getServletPath();
            tenantUtils.when(ApsTenantApplicationUtils::getTenant).thenReturn(Optional.empty());
            Mockito.doThrow(NullPointerException.class).when(chain).doFilter(Mockito.any(), Mockito.any());
            Assertions.assertThrows(NullPointerException.class, () -> filter.doFilter(request, response, chain));
            mdc.verify(() -> MDC.put("tenant", ""));
            mdc.verify(() -> MDC.remove("tenant"));
            Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.any());
        }
    }

}
