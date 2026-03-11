package org.entando.entando.apsadmin.system;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Set;
import org.apache.struts2.ActionContext;
import org.apache.struts2.ActionInvocation;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.interceptor.AbstractInterceptor;
import org.entando.entando.aps.system.services.IFHeadlessWidgetConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Struts interceptor that activates headless layout mode for widget configuration pages.
 * <p>
 * When the request parameter {@code entandoHeadless=true} is present,
 * the {@link IFHeadlessWidgetConfig#HEADLESS_WIDGET_CONFIG} feature flag is enabled,
 * and {@code appBuilderIntegrationEnabled} system param is {@code "true"},
 * this interceptor sets a request attribute that tells the layout template
 * to render without header and navigation menu (headless mode).
 * <p>
 * The {@code entandoHeadless} parameter is propagated across form submissions
 * via a hidden field injected by {@code headless.jsp}.
 * <p>
 * When a save action succeeds in headless mode, the Struts result would normally
 * navigate away from the widget config form via the {@code "configure"} or
 * {@code "pageTree"} global results defined in all specialWidget packages.
 * This interceptor intercepts those results and instead sends a
 * {@code postMessage} to the parent frame to signal save success.
 * A response wrapper blocks any further output from chain/redirect results.
 */
public class HeadlessLayoutInterceptor extends AbstractInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(HeadlessLayoutInterceptor.class);

    public static final String HEADLESS_MODE_ATTR = "entandoHeadlessMode";
    public static final String HEADLESS_PARAM = "entandoHeadless";

    /**
     * Result codes from specialWidget global-results that indicate a successful
     * save navigating away from the widget config form. These are the only result
     * codes explicitly returned by save() methods in widget config actions
     * (e.g. SimpleWidgetConfigAction). Internal multi-step navigation actions
     * (e.g. joinContent, saveFilter) use action-level results with different names.
     */
    private static final Set<String> SAVE_SUCCESS_RESULTS = Set.of("configure", "pageTree");

    private static final String SAVE_SUCCESS_HTML =
            "<!DOCTYPE html><html><body><script>"
            + "if(window.parent!==window){"
            + "window.parent.postMessage({type:'entando.widgetConfigSaved'},'*');"
            + "}"
            + "</script></body></html>";

    @Override
    public String intercept(ActionInvocation invocation) throws Exception {
        HttpServletRequest request = ServletActionContext.getRequest();
        boolean headless = isHeadlessFeatureFlagEnabled()
                && isAppBuilderIntegrationEnabled(request)
                && "true".equals(request.getParameter(HEADLESS_PARAM));
        if (headless) {
            request.setAttribute(HEADLESS_MODE_ATTR, Boolean.TRUE);
            HttpServletResponse originalResponse = ServletActionContext.getResponse();
            HeadlessSaveResponse wrapper = new HeadlessSaveResponse(originalResponse);
            ActionContext.getContext().withServletResponse(wrapper);
            invocation.addPreResultListener((inv, resultCode) -> {
                if (SAVE_SUCCESS_RESULTS.contains(resultCode)) {
                    try {
                        wrapper.writeSuccessAndBlock();
                    } catch (IOException e) {
                        logger.error("Failed to send headless save success response", e);
                    }
                }
            });
        }
        return invocation.invoke();
    }

    boolean isHeadlessFeatureFlagEnabled() {
        return IFHeadlessWidgetConfig.HEADLESS_WIDGET_CONFIG;
    }

    private boolean isAppBuilderIntegrationEnabled(HttpServletRequest request) {
        ConfigInterface configManager = (ConfigInterface) ApsWebApplicationUtils
                .getBean(SystemConstants.BASE_CONFIG_MANAGER, request);
        return "true".equals(configManager.getParam("appBuilderIntegrationEnabled"));
    }

    /**
     * Response wrapper that, once activated via {@link #writeSuccessAndBlock()},
     * writes the postMessage HTML and blocks all further output
     * (redirects, chain rendering, etc.).
     */
    static class HeadlessSaveResponse extends HttpServletResponseWrapper {

        private boolean blocked;
        private PrintWriter noOpWriter;
        private ServletOutputStream noOpStream;

        HeadlessSaveResponse(HttpServletResponse response) {
            super(response);
        }

        void writeSuccessAndBlock() throws IOException {
            if (blocked) {
                return;
            }
            blocked = true;
            super.resetBuffer();
            super.setContentType("text/html;charset=UTF-8");
            super.setStatus(SC_OK);
            super.getWriter().write(SAVE_SUCCESS_HTML);
            super.getWriter().flush();
            super.flushBuffer();
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            writeSuccessAndBlock();
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (blocked) {
                if (noOpWriter == null) {
                    noOpWriter = new PrintWriter(Writer.nullWriter());
                }
                return noOpWriter;
            }
            return super.getWriter();
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (blocked) {
                if (noOpStream == null) {
                    noOpStream = new NoOpOutputStream();
                }
                return noOpStream;
            }
            return super.getOutputStream();
        }

        @Override
        public void resetBuffer() {
            if (!blocked) {
                super.resetBuffer();
            }
        }

        @Override
        public void reset() {
            if (!blocked) {
                super.reset();
            }
        }
    }

    private static class NoOpOutputStream extends ServletOutputStream {

        @Override
        public void write(int b) { }

        @Override
        public void write(byte[] b) { }

        @Override
        public void write(byte[] b, int off, int len) { }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener listener) { }
    }
}