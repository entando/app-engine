package org.entando.entando.web.filter;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import java.util.Optional;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

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
            tenantUtils.when(() -> ApsTenantApplicationUtils.getTenant()).thenReturn(Optional.of("currentTenant"));
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
            tenantUtils.when(() -> ApsTenantApplicationUtils.getTenant()).thenReturn(Optional.empty());
            filter.doFilter(request, response, chain);
            mdc.verify(() -> MDC.put("tenant", ""));
            mdc.verify(() -> MDC.remove("tenant"));
            Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.any());
        }
    }

    @Test
    void shouldNotCreateSessionWhenResolvingTenantFromDomain() throws Exception {
        ITenantManager tenantManager = Mockito.mock(ITenantManager.class);
        Mockito.when(tenantManager.getTenantCodeByDomain("tenant1.test.com")).thenReturn("tenant1");
        WebApplicationContext wac = Mockito.mock(WebApplicationContext.class);
        Mockito.when(wac.getBean(ITenantManager.class)).thenReturn(tenantManager);
        MockServletContext servletContext = new MockServletContext();
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
        MockHttpServletRequest mockRequest = new MockHttpServletRequest(servletContext);
        mockRequest.setServerName("tenant1.test.com");
        String[] tenantInChain = new String[1];
        Mockito.doAnswer(invocation -> {
            tenantInChain[0] = MDC.get("tenant");
            return null;
        }).when(chain).doFilter(Mockito.any(), Mockito.any());

        filter.doFilter(mockRequest, new MockHttpServletResponse(), chain);

        Assertions.assertEquals("tenant1", tenantInChain[0]);
        Assertions.assertNull(mockRequest.getSession(false));
    }

    @Test
    void shouldRemoveMDCKeyInCaseOfException() throws Exception {
        try (MockedStatic<MDC> mdc = Mockito.mockStatic(MDC.class);
                MockedStatic<ApsTenantApplicationUtils> tenantUtils
                        = Mockito.mockStatic(ApsTenantApplicationUtils.class)) {
            tenantUtils.when(() -> ApsTenantApplicationUtils.getTenant()).thenReturn(Optional.empty());
            Mockito.doThrow(NullPointerException.class).when(chain).doFilter(Mockito.any(), Mockito.any());
            Assertions.assertThrows(NullPointerException.class, () -> filter.doFilter(request, response, chain));
            mdc.verify(() -> MDC.put("tenant", ""));
            mdc.verify(() -> MDC.remove("tenant"));
            Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.any());
        }
    }
}
