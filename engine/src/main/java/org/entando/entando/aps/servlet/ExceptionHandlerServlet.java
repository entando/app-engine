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
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
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
        try {
            String pageCode = this.pageManager.getConfig(IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE);
            log.debug("Configured error page: '{}'", pageCode);
            if (pageCode != null) {
                IPage page = this.pageManager.getOnlinePage(pageCode);
                if (null != page) {
                    Lang lang = this.getLang(request);
                    String url = this.urlManager.createURL(page, lang, Map.of(), false, request);
                    String baseUrl = this.urlManager.getApplicationBaseURL(request);
                    String path = url.substring(baseUrl.length() - 1);
                    log.debug("Forwarding to path '{}' (url='{}', baseUrl='{}')", path, url, baseUrl);
                    request.getServletContext().getRequestDispatcher(path).forward(request, response);
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
            request.getServletContext().getRequestDispatcher("/error.jsp").forward(request, response);
        } catch (Throwable t) {
            log.warn("Error while displaying default error page", t);
        }
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
