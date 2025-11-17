package org.entando.entando.aps.servlet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.url.IURLManager;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
class ExceptionHandlerServletTest {

    private static final String CUSTOM_ERROR_PAGE_CODE = "custom_error_page";

    @Mock
    private IPageManager pageManager;
    @Mock
    private ILangManager langManager;
    @Mock
    private IURLManager urlManager;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ServletContext servletContext;
    @Mock
    private RequestDispatcher requestDispatcher;

    @InjectMocks
    private ExceptionHandlerServlet servlet;

    @BeforeEach
    void setUp() {
        Mockito.when(request.getServletContext()).thenReturn(servletContext);
    }

    @Test
    void shouldDisplayCustomErrorPageIfConfigured() throws Exception {
        Mockito.when(pageManager.getConfig(IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE))
                .thenReturn(CUSTOM_ERROR_PAGE_CODE);
        Mockito.when(pageManager.getOnlinePage(CUSTOM_ERROR_PAGE_CODE)).thenReturn(Mockito.mock(IPage.class));
        Mockito.when(urlManager.createURL(any(), any(), any(), anyBoolean(), any()))
                .thenReturn("http://localhost:8080/entando-de-app/en/custom_error_page.page");
        Mockito.when(urlManager.getApplicationBaseURL(any())).thenReturn("http://localhost:8080/entando-de-app/");
        Mockito.when(servletContext.getRequestDispatcher("/en/custom_error_page.page")).thenReturn(requestDispatcher);

        servlet.doGet(request, response);

        Mockito.verify(requestDispatcher).forward(any(), any());
    }

    @Test
    void shouldFallbackToDefaultErrorPageIfCustomErrorPageIsNotConfigured() throws Exception {
        Mockito.when(servletContext.getRequestDispatcher("/error.jsp")).thenReturn(requestDispatcher);
        servlet.doGet(request, response);
        Mockito.verify(requestDispatcher).forward(any(), any());
    }

    @Test
    void shouldFallbackToDefaultErrorPageIfConfiguredCustomErrorPageIsNotFound() throws Exception {
        Mockito.when(pageManager.getConfig(IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE))
                .thenReturn(CUSTOM_ERROR_PAGE_CODE);
        Mockito.when(servletContext.getRequestDispatcher("/error.jsp")).thenReturn(requestDispatcher);
        servlet.doGet(request, response);
        Mockito.verify(requestDispatcher).forward(any(), any());
    }

    @Test
    void shouldFallbackToDefaultErrorPageIfErrorHappensWhileDisplayingConfiguredCustomErrorPage() throws Exception {
        Mockito.when(pageManager.getConfig(IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE))
                .thenReturn(CUSTOM_ERROR_PAGE_CODE);
        Mockito.doThrow(NullPointerException.class).when(pageManager).getOnlinePage(CUSTOM_ERROR_PAGE_CODE);
        Mockito.when(servletContext.getRequestDispatcher("/error.jsp")).thenReturn(requestDispatcher);
        servlet.doGet(request, response);
        Mockito.verify(requestDispatcher).forward(any(), any());
    }

    /**
     * See CWE-600: Uncaught Exception in Servlet (https://cwe.mitre.org/data/definitions/600.html)
     */
    @Test
    void shouldSuppressErrorInLoadingDefaultErrorPage() throws Exception {
        servlet.doGet(request, response);
        Mockito.verify(servletContext).getRequestDispatcher("/error.jsp");
    }

    @Test
    void shouldRetrieveCurrentLangFromRequestContext() throws Exception {
        RequestContext reqCtx = new RequestContext();
        Lang currentLang = new Lang();
        currentLang.setCode("it");
        reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG, currentLang);
        Mockito.when(request.getAttribute(RequestContext.REQCTX)).thenReturn(reqCtx);
        Mockito.when(pageManager.getConfig(IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE))
                .thenReturn(CUSTOM_ERROR_PAGE_CODE);
        Mockito.when(pageManager.getOnlinePage(CUSTOM_ERROR_PAGE_CODE)).thenReturn(Mockito.mock(IPage.class));
        Mockito.when(urlManager.createURL(any(), any(), any(), anyBoolean(), any()))
                .thenReturn("http://localhost:8080/entando-de-app/it/custom_error_page.page");
        Mockito.when(urlManager.getApplicationBaseURL(any())).thenReturn("http://localhost:8080/entando-de-app/");
        Mockito.when(servletContext.getRequestDispatcher("/it/custom_error_page.page")).thenReturn(requestDispatcher);

        servlet.doGet(request, response);

        Mockito.verify(requestDispatcher).forward(any(), any());
    }
}
