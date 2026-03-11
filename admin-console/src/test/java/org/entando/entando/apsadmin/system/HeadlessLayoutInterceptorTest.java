package org.entando.entando.apsadmin.system;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.struts2.ActionContext;
import org.apache.struts2.ActionInvocation;
import org.apache.struts2.interceptor.PreResultListener;
import org.apache.struts2.ServletActionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HeadlessLayoutInterceptorTest {

    private HeadlessLayoutInterceptor interceptor;

    @Mock
    private ActionInvocation invocation;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ConfigInterface configManager;
    @Mock
    private ActionContext actionContext;

    @BeforeEach
    void setUp() {
        interceptor = spy(new HeadlessLayoutInterceptor());
    }

    @Test
    void shouldSkipHeadlessModeWhenParameterIsMissing() throws Exception {
        doReturn(true).when(interceptor).isHeadlessFeatureFlagEnabled();
        try (MockedStatic<ServletActionContext> sac = Mockito.mockStatic(ServletActionContext.class);
             MockedStatic<ApsWebApplicationUtils> awau = Mockito.mockStatic(ApsWebApplicationUtils.class)) {

            sac.when(ServletActionContext::getRequest).thenReturn(request);
            awau.when(() -> ApsWebApplicationUtils.getBean(
                    eq(SystemConstants.BASE_CONFIG_MANAGER), eq(request))).thenReturn(configManager);

            when(request.getParameter(HeadlessLayoutInterceptor.HEADLESS_PARAM)).thenReturn(null);
            when(configManager.getParam("appBuilderIntegrationEnabled")).thenReturn("true");
            when(invocation.invoke()).thenReturn("success");

            String result = interceptor.intercept(invocation);

            assertEquals("success", result);
            verify(request, never()).setAttribute(eq(HeadlessLayoutInterceptor.HEADLESS_MODE_ATTR), any());
            verify(invocation, never()).addPreResultListener(any());
        }
    }

    @Test
    void shouldSkipHeadlessModeWhenParameterIsFalse() throws Exception {
        doReturn(true).when(interceptor).isHeadlessFeatureFlagEnabled();
        try (MockedStatic<ServletActionContext> sac = Mockito.mockStatic(ServletActionContext.class);
             MockedStatic<ApsWebApplicationUtils> awau = Mockito.mockStatic(ApsWebApplicationUtils.class)) {

            sac.when(ServletActionContext::getRequest).thenReturn(request);
            awau.when(() -> ApsWebApplicationUtils.getBean(
                    eq(SystemConstants.BASE_CONFIG_MANAGER), eq(request))).thenReturn(configManager);

            when(request.getParameter(HeadlessLayoutInterceptor.HEADLESS_PARAM)).thenReturn("false");
            when(configManager.getParam("appBuilderIntegrationEnabled")).thenReturn("true");
            when(invocation.invoke()).thenReturn("success");

            String result = interceptor.intercept(invocation);

            assertEquals("success", result);
            verify(request, never()).setAttribute(eq(HeadlessLayoutInterceptor.HEADLESS_MODE_ATTR), any());
        }
    }

    @Test
    void shouldActivateHeadlessModeWhenAllConditionsMet() throws Exception {
        doReturn(true).when(interceptor).isHeadlessFeatureFlagEnabled();
        try (MockedStatic<ServletActionContext> sac = Mockito.mockStatic(ServletActionContext.class);
             MockedStatic<ActionContext> ac = Mockito.mockStatic(ActionContext.class);
             MockedStatic<ApsWebApplicationUtils> awau = Mockito.mockStatic(ApsWebApplicationUtils.class)) {

            sac.when(ServletActionContext::getRequest).thenReturn(request);
            sac.when(ServletActionContext::getResponse).thenReturn(response);
            ac.when(ActionContext::getContext).thenReturn(actionContext);
            awau.when(() -> ApsWebApplicationUtils.getBean(
                    eq(SystemConstants.BASE_CONFIG_MANAGER), eq(request))).thenReturn(configManager);

            when(request.getParameter(HeadlessLayoutInterceptor.HEADLESS_PARAM)).thenReturn("true");
            when(configManager.getParam("appBuilderIntegrationEnabled")).thenReturn("true");
            when(invocation.invoke()).thenReturn("input");

            String result = interceptor.intercept(invocation);

            assertEquals("input", result);
            verify(request).setAttribute(HeadlessLayoutInterceptor.HEADLESS_MODE_ATTR, Boolean.TRUE);
            verify(actionContext).withServletResponse(any(HeadlessLayoutInterceptor.HeadlessSaveResponse.class));
            verify(invocation).addPreResultListener(any(PreResultListener.class));
        }
    }

    @Test
    void shouldSkipHeadlessModeWhenAppBuilderIntegrationDisabled() throws Exception {
        doReturn(true).when(interceptor).isHeadlessFeatureFlagEnabled();
        try (MockedStatic<ServletActionContext> sac = Mockito.mockStatic(ServletActionContext.class);
             MockedStatic<ApsWebApplicationUtils> awau = Mockito.mockStatic(ApsWebApplicationUtils.class)) {

            sac.when(ServletActionContext::getRequest).thenReturn(request);
            awau.when(() -> ApsWebApplicationUtils.getBean(
                    eq(SystemConstants.BASE_CONFIG_MANAGER), eq(request))).thenReturn(configManager);

            when(configManager.getParam("appBuilderIntegrationEnabled")).thenReturn("false");
            when(invocation.invoke()).thenReturn("success");

            String result = interceptor.intercept(invocation);

            assertEquals("success", result);
            verify(request, never()).setAttribute(eq(HeadlessLayoutInterceptor.HEADLESS_MODE_ATTR), any());
            verify(request, never()).getParameter(anyString());
        }
    }

    @Test
    void shouldSkipHeadlessModeWhenFeatureFlagDisabled() throws Exception {
        // Feature flag defaults to false in test env — short-circuits before getParameter
        try (MockedStatic<ServletActionContext> sac = Mockito.mockStatic(ServletActionContext.class)) {

            sac.when(ServletActionContext::getRequest).thenReturn(request);
            when(invocation.invoke()).thenReturn("success");

            String result = interceptor.intercept(invocation);

            assertEquals("success", result);
            verify(request, never()).setAttribute(eq(HeadlessLayoutInterceptor.HEADLESS_MODE_ATTR), any());
            verify(request, never()).getParameter(anyString());
        }
    }

    @Test
    void preResultListenerShouldWriteSuccessForConfigureResult() throws Exception {
        doReturn(true).when(interceptor).isHeadlessFeatureFlagEnabled();
        try (MockedStatic<ServletActionContext> sac = Mockito.mockStatic(ServletActionContext.class);
             MockedStatic<ActionContext> ac = Mockito.mockStatic(ActionContext.class);
             MockedStatic<ApsWebApplicationUtils> awau = Mockito.mockStatic(ApsWebApplicationUtils.class)) {

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            sac.when(ServletActionContext::getRequest).thenReturn(request);
            sac.when(ServletActionContext::getResponse).thenReturn(response);
            ac.when(ActionContext::getContext).thenReturn(actionContext);
            awau.when(() -> ApsWebApplicationUtils.getBean(
                    eq(SystemConstants.BASE_CONFIG_MANAGER), eq(request))).thenReturn(configManager);

            when(request.getParameter(HeadlessLayoutInterceptor.HEADLESS_PARAM)).thenReturn("true");
            when(configManager.getParam("appBuilderIntegrationEnabled")).thenReturn("true");
            when(response.getWriter()).thenReturn(printWriter);
            when(invocation.invoke()).thenReturn("configure");

            ArgumentCaptor<PreResultListener> listenerCaptor = ArgumentCaptor.forClass(PreResultListener.class);

            interceptor.intercept(invocation);

            verify(invocation).addPreResultListener(listenerCaptor.capture());

            // Simulate Struts calling the PreResultListener with "configure" result
            listenerCaptor.getValue().beforeResult(invocation, "configure");

            String output = stringWriter.toString();
            assertTrue(output.contains("entando.widgetConfigSaved"),
                    "Should send postMessage for save success");
            assertTrue(output.contains("window.parent.postMessage"),
                    "Should post to parent frame");
        }
    }

    @Test
    void preResultListenerShouldWriteSuccessForPageTreeResult() throws Exception {
        doReturn(true).when(interceptor).isHeadlessFeatureFlagEnabled();
        try (MockedStatic<ServletActionContext> sac = Mockito.mockStatic(ServletActionContext.class);
             MockedStatic<ActionContext> ac = Mockito.mockStatic(ActionContext.class);
             MockedStatic<ApsWebApplicationUtils> awau = Mockito.mockStatic(ApsWebApplicationUtils.class)) {

            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            sac.when(ServletActionContext::getRequest).thenReturn(request);
            sac.when(ServletActionContext::getResponse).thenReturn(response);
            ac.when(ActionContext::getContext).thenReturn(actionContext);
            awau.when(() -> ApsWebApplicationUtils.getBean(
                    eq(SystemConstants.BASE_CONFIG_MANAGER), eq(request))).thenReturn(configManager);

            when(request.getParameter(HeadlessLayoutInterceptor.HEADLESS_PARAM)).thenReturn("true");
            when(configManager.getParam("appBuilderIntegrationEnabled")).thenReturn("true");
            when(response.getWriter()).thenReturn(printWriter);
            when(invocation.invoke()).thenReturn("pageTree");

            ArgumentCaptor<PreResultListener> listenerCaptor = ArgumentCaptor.forClass(PreResultListener.class);

            interceptor.intercept(invocation);

            verify(invocation).addPreResultListener(listenerCaptor.capture());

            listenerCaptor.getValue().beforeResult(invocation, "pageTree");

            String output = stringWriter.toString();
            assertTrue(output.contains("entando.widgetConfigSaved"));
        }
    }

    @Test
    void preResultListenerShouldNotWriteForInputResult() throws Exception {
        doReturn(true).when(interceptor).isHeadlessFeatureFlagEnabled();
        try (MockedStatic<ServletActionContext> sac = Mockito.mockStatic(ServletActionContext.class);
             MockedStatic<ActionContext> ac = Mockito.mockStatic(ActionContext.class);
             MockedStatic<ApsWebApplicationUtils> awau = Mockito.mockStatic(ApsWebApplicationUtils.class)) {

            sac.when(ServletActionContext::getRequest).thenReturn(request);
            sac.when(ServletActionContext::getResponse).thenReturn(response);
            ac.when(ActionContext::getContext).thenReturn(actionContext);
            awau.when(() -> ApsWebApplicationUtils.getBean(
                    eq(SystemConstants.BASE_CONFIG_MANAGER), eq(request))).thenReturn(configManager);

            when(request.getParameter(HeadlessLayoutInterceptor.HEADLESS_PARAM)).thenReturn("true");
            when(configManager.getParam("appBuilderIntegrationEnabled")).thenReturn("true");
            when(invocation.invoke()).thenReturn("input");

            ArgumentCaptor<PreResultListener> listenerCaptor = ArgumentCaptor.forClass(PreResultListener.class);

            interceptor.intercept(invocation);

            verify(invocation).addPreResultListener(listenerCaptor.capture());

            // "input" is validation error — should NOT trigger save success
            listenerCaptor.getValue().beforeResult(invocation, "input");

            // Response writer should never have been touched
            verify(response, never()).getWriter();
        }
    }

    @Nested
    class HeadlessSaveResponseTest {

        @Mock
        private HttpServletResponse delegate;

        @Test
        void writeSuccessAndBlockShouldWritePostMessageHtml() throws IOException {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(delegate.getWriter()).thenReturn(printWriter);

            HeadlessLayoutInterceptor.HeadlessSaveResponse wrapper =
                    new HeadlessLayoutInterceptor.HeadlessSaveResponse(delegate);

            wrapper.writeSuccessAndBlock();

            String output = stringWriter.toString();
            assertTrue(output.contains("entando.widgetConfigSaved"));
            assertTrue(output.contains("window.parent.postMessage"));
            verify(delegate).resetBuffer();
            verify(delegate).setContentType("text/html;charset=UTF-8");
            verify(delegate).setStatus(HttpServletResponse.SC_OK);
            verify(delegate).flushBuffer();
        }

        @Test
        void writeSuccessAndBlockShouldBeIdempotent() throws IOException {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(delegate.getWriter()).thenReturn(printWriter);

            HeadlessLayoutInterceptor.HeadlessSaveResponse wrapper =
                    new HeadlessLayoutInterceptor.HeadlessSaveResponse(delegate);

            wrapper.writeSuccessAndBlock();
            wrapper.writeSuccessAndBlock();

            // resetBuffer should only be called once
            verify(delegate, times(1)).resetBuffer();
            verify(delegate, times(1)).flushBuffer();
        }

        @Test
        void sendRedirectShouldTriggerWriteSuccessAndBlock() throws IOException {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(delegate.getWriter()).thenReturn(printWriter);

            HeadlessLayoutInterceptor.HeadlessSaveResponse wrapper =
                    new HeadlessLayoutInterceptor.HeadlessSaveResponse(delegate);

            wrapper.sendRedirect("/some/url");

            String output = stringWriter.toString();
            assertTrue(output.contains("entando.widgetConfigSaved"),
                    "Redirect should be intercepted and replaced with save success");
        }

        @Test
        void getWriterShouldReturnNoOpWriterWhenBlocked() throws IOException {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(delegate.getWriter()).thenReturn(printWriter);

            HeadlessLayoutInterceptor.HeadlessSaveResponse wrapper =
                    new HeadlessLayoutInterceptor.HeadlessSaveResponse(delegate);

            wrapper.writeSuccessAndBlock();

            PrintWriter blockedWriter = wrapper.getWriter();
            blockedWriter.write("this should be discarded");
            blockedWriter.flush();

            // The no-op writer should not write to the original writer
            assertFalse(stringWriter.toString().contains("this should be discarded"));
        }

        @Test
        void getWriterShouldReturnRealWriterWhenNotBlocked() throws IOException {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(delegate.getWriter()).thenReturn(printWriter);

            HeadlessLayoutInterceptor.HeadlessSaveResponse wrapper =
                    new HeadlessLayoutInterceptor.HeadlessSaveResponse(delegate);

            PrintWriter writer = wrapper.getWriter();
            writer.write("real content");
            writer.flush();

            assertTrue(stringWriter.toString().contains("real content"));
        }

        @Test
        void getOutputStreamShouldReturnNoOpStreamWhenBlocked() throws IOException {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(delegate.getWriter()).thenReturn(printWriter);

            HeadlessLayoutInterceptor.HeadlessSaveResponse wrapper =
                    new HeadlessLayoutInterceptor.HeadlessSaveResponse(delegate);

            wrapper.writeSuccessAndBlock();

            ServletOutputStream stream = wrapper.getOutputStream();
            assertNotNull(stream);
            assertTrue(stream.isReady());
            // Should not throw
            stream.write(42);
            stream.write(new byte[]{1, 2, 3});
            stream.write(new byte[]{1, 2, 3, 4, 5}, 1, 3);
        }

        @Test
        void resetBufferShouldBeNoOpWhenBlocked() throws IOException {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(delegate.getWriter()).thenReturn(printWriter);

            HeadlessLayoutInterceptor.HeadlessSaveResponse wrapper =
                    new HeadlessLayoutInterceptor.HeadlessSaveResponse(delegate);

            wrapper.writeSuccessAndBlock();
            // First call is from writeSuccessAndBlock itself
            verify(delegate, times(1)).resetBuffer();

            wrapper.resetBuffer();
            // Should still be 1 — the second call was blocked
            verify(delegate, times(1)).resetBuffer();
        }

        @Test
        void resetShouldBeNoOpWhenBlocked() throws IOException {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            when(delegate.getWriter()).thenReturn(printWriter);

            HeadlessLayoutInterceptor.HeadlessSaveResponse wrapper =
                    new HeadlessLayoutInterceptor.HeadlessSaveResponse(delegate);

            wrapper.writeSuccessAndBlock();

            wrapper.reset();
            verify(delegate, never()).reset();
        }

        @Test
        void resetShouldDelegateWhenNotBlocked() {
            HeadlessLayoutInterceptor.HeadlessSaveResponse wrapper =
                    new HeadlessLayoutInterceptor.HeadlessSaveResponse(delegate);

            wrapper.reset();
            verify(delegate).reset();
        }

        @Test
        void resetBufferShouldDelegateWhenNotBlocked() {
            HeadlessLayoutInterceptor.HeadlessSaveResponse wrapper =
                    new HeadlessLayoutInterceptor.HeadlessSaveResponse(delegate);

            wrapper.resetBuffer();
            verify(delegate).resetBuffer();
        }
    }

}