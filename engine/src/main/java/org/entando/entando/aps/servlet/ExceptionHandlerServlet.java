package org.entando.entando.aps.servlet;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.url.IURLManager;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.entando.entando.aps.servlet.routing.VirtualContextHelper;
import org.entando.entando.aps.servlet.security.CustomWrappedRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

@Slf4j
public class ExceptionHandlerServlet extends HttpServlet {

    @Autowired
    private IPageManager pageManager;
    @Autowired
    private ILangManager langManager;
    @Autowired
    private IURLManager urlManager;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpServletRequest customizedRequest = setupVirtualContext(request);

        try {

            String pageCode = this.pageManager.getConfig(IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE);
            log.debug("Configured error page: '{}'", pageCode);
            if (pageCode != null) {
                IPage page = this.pageManager.getOnlinePage(pageCode);
                if (null != page) {
                    Lang lang = this.getLang(customizedRequest);
                    String url = this.urlManager.createURL(page, lang, Map.of(), false, customizedRequest);
                    String baseUrl = this.urlManager.getApplicationBaseURL(customizedRequest);
                    String path = url.substring(baseUrl.length() - 1);
                    log.debug("Forwarding to path '{}' (url='{}', baseUrl='{}')", path, url, baseUrl);
                    customizedRequest.getServletContext().getRequestDispatcher(path).forward(customizedRequest, response);
                    return;
                } else {
                    log.warn("Unable to find custom error page '{}'", pageCode);
                }
            }
        } catch (Throwable t) {
            log.warn("Error while displaying custom error page", t);
        }
        try {
            // Default error page
            log.debug("Displaying default error page");
            customizedRequest.getServletContext().getRequestDispatcher("/error.jsp").forward(customizedRequest, response);
        } catch (Throwable t) {
            log.warn("Error while displaying default error page", t);
        }
    }

    private static HttpServletRequest setupVirtualContext(HttpServletRequest request) {
        HttpServletRequest customizedRequest = VirtualContextHelper.customizeRequest(request);

        if (customizedRequest instanceof CustomWrappedRequest && ((CustomWrappedRequest) customizedRequest).hasVirtualContext()) {
            VirtualContextHelper.setThreadLocal_VirtualContextPath(customizedRequest.getContextPath());
        }
        return customizedRequest;
    }

    private Lang getLang(HttpServletRequest request) {
        RequestContext reqCtx = (RequestContext) request.getAttribute(RequestContext.REQCTX);
        if (reqCtx != null) {
            Lang currentLang = (Lang) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG);
            if (currentLang != null) {
                return currentLang;
            }
        }
        return this.langManager.getDefaultLang();
    }
}
