package org.entando.entando.web.filter;

import com.agiletec.aps.system.services.user.UserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
class MDCUserFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @InjectMocks
    private MDCUserFilter filter;

    @Test
    void shouldSetUserToMDCFromRequestAttribute() throws Exception {
        try (MockedStatic<MDC> mdc = Mockito.mockStatic(MDC.class)) {
            UserDetails user = mockUser();
            Mockito.when(request.getAttribute("user")).thenReturn(user);
            filter.doFilter(request, response, chain);
            mdc.verify(() -> MDC.put("user", "admin"));
            mdc.verify(() -> MDC.remove("user"));
            Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.any());
        }
    }

    @Test
    void shouldSetUserToMDCFromSessionAttribute() throws Exception {
        try (MockedStatic<MDC> mdc = Mockito.mockStatic(MDC.class)) {
            HttpSession session = Mockito.mock(HttpSession.class);
            Mockito.when(request.getSession(false)).thenReturn(session);
            UserDetails user = mockUser();
            Mockito.when(session.getAttribute("user")).thenReturn(user);
            filter.doFilter(request, response, chain);
            mdc.verify(() -> MDC.put("user", "admin"));
            mdc.verify(() -> MDC.remove("user"));
            Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.any());
        }
    }

    @Test
    void shouldSetGuestUserToMDC() throws Exception {
        try (MockedStatic<MDC> mdc = Mockito.mockStatic(MDC.class)) {
            filter.doFilter(request, response, chain);
            mdc.verify(() -> MDC.put("user", "guest"));
            mdc.verify(() -> MDC.remove("user"));
            Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.any());
        }
    }

    @Test
    void shouldRemoveMDCKeyInCaseOfException() throws Exception {
        try (MockedStatic<MDC> mdc = Mockito.mockStatic(MDC.class)) {
            Mockito.doThrow(NullPointerException.class).when(chain).doFilter(Mockito.any(), Mockito.any());
            Assertions.assertThrows(NullPointerException.class, () -> filter.doFilter(request, response, chain));
            mdc.verify(() -> MDC.put("user", "guest"));
            mdc.verify(() -> MDC.remove("user"));
            Mockito.verify(chain, Mockito.times(1)).doFilter(Mockito.any(), Mockito.any());
        }
    }

    private UserDetails mockUser() {
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        Mockito.when(userDetails.getUsername()).thenReturn("admin");
        return userDetails;
    }
}
