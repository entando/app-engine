package org.entando.entando.web.configuration;

import com.agiletec.apsadmin.system.dispatcher.StrutsPrepareAndExecuteFilter;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.servlet.routing.VirtualContextHelper;
import org.entando.entando.ent.util.EntLogging;
import org.entando.entando.web.devmode.DevModeProxy;

import javax.servlet.*;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;

public class ServletContextConfigurator implements ServletContextListener {
    public static final EnumSet<DispatcherType> DISPATCHER_TYPES = EnumSet.allOf(DispatcherType.class);
    public static final String STRUTS_NEW_FILTER_NAME = "struts2-virtual-context";
    public static final String SPRING_DISPATCHER_NAME_ON_WEB_XML = "springDispatcher";
    public static final String CONTROLLER_SERVLET = "ControllerServlet";
    public static final String PREVIEW_CONTROLLER_SERVLET = "PreviewControllerServlet";
    public static final String EXCEPTION_HANDLER_SERVLET = "ExceptionHandlerServlet";

    @Override
    public void contextInitialized(ServletContextEvent ce) {
        ServletContext servletContext = ce.getServletContext();

        List<String> virtualContexts = VirtualContextHelper.getVirtualContexts();
        if (virtualContexts.isEmpty()) {
            return;
        }

        FilterRegistration.Dynamic strutsFilter = servletContext.addFilter(STRUTS_NEW_FILTER_NAME, StrutsPrepareAndExecuteFilter.class);
        ServletRegistration springDispatcherRegistration = servletContext.getServletRegistration(SPRING_DISPATCHER_NAME_ON_WEB_XML);
        ServletRegistration controllerServletRegistration = servletContext.getServletRegistration(CONTROLLER_SERVLET);
        ServletRegistration previewControllerServletRegistration = servletContext.getServletRegistration(PREVIEW_CONTROLLER_SERVLET);
        ServletRegistration exceptionHandlerServlet = servletContext.getServletRegistration(EXCEPTION_HANDLER_SERVLET);
        ServletRegistration proxyServlet = addProxyServletRegistration(servletContext);

        for (String vctx : virtualContexts) {
            LOGGER.info("Dynamically adding additional mappings for context: {}", vctx);
            addContextFiltersMapping(strutsFilter, vctx);
            addApiMapping(springDispatcherRegistration, vctx);
            addPagesMapping(controllerServletRegistration, previewControllerServletRegistration, vctx);
            addSpecialMapping(exceptionHandlerServlet, proxyServlet, vctx);
        }

    }

    private static ServletRegistration addProxyServletRegistration(ServletContext servletContext) {
        return (StringUtils.isBlank(DevModeProxy.CONFIG)) ? null :
                servletContext.addServlet("ProxyServlet", DevModeProxy.class);
    }

    private static void addContextFiltersMapping(FilterRegistration.Dynamic filter, String vctx) {
        filter.addMappingForUrlPatterns(DISPATCHER_TYPES, false, Paths.get("/", vctx, "do", "*").toString());
    }

    private static void addRootFiltersMapping(FilterRegistration.Dynamic filter, String... paths) {
        filter.addMappingForUrlPatterns(DISPATCHER_TYPES, false, paths);
    }

    private static void addApiMapping(ServletRegistration servletRegistration, String vctx) {
        servletRegistration.addMapping(Paths.get("/", vctx, "api", "*").toString());
    }

    private static void addPagesMapping(
            ServletRegistration pageServletRegistration, ServletRegistration previewServletRegistration, String vctx
    ) {
        pageServletRegistration.addMapping(Paths.get("/", vctx, "page", "*").toString());
        pageServletRegistration.addMapping(Paths.get("/", vctx, "pages", "*").toString());
        previewServletRegistration.addMapping(Paths.get("/", vctx, "preview", "*").toString());
    }

    private static void addSpecialMapping(ServletRegistration servletRegistration, ServletRegistration proxyServlet, String vctx) {
        servletRegistration.addMapping(Paths.get("/", vctx, "error").toString());
        if (proxyServlet != null) {
            proxyServlet.addMapping("/digital-exchange/*");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContextListener.super.contextDestroyed(sce);
    }

    private static final EntLogging.EntLogger LOGGER = EntLogging.EntLogFactory.getSanitizedLogger(ServletContextConfigurator.class);
}