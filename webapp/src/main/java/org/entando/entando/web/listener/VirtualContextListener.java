package org.entando.entando.web.listener;

import com.agiletec.apsadmin.system.dispatcher.StrutsPrepareAndExecuteFilter;
import org.entando.entando.aps.servlet.routing.VirtualContextHelper;
import org.entando.entando.ent.util.EntLogging;

import javax.servlet.*;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;

public class VirtualContextListener implements ServletContextListener {
    public static final EnumSet<DispatcherType> DISPATCHER_TYPES = EnumSet.allOf(DispatcherType.class);
    public static final String STRUTS_NEW_FILTER_NAME = "struts2-virtual-context";
    public static final String SPRING_DISPATCHER_NAME_ON_WEB_XML = "springDispatcher";

    @Override
    public void contextInitialized(ServletContextEvent ce) {
        ServletContext servletContext = ce.getServletContext();

        List<String> virtualContexts = VirtualContextHelper.getVirtualContexts();
        if (virtualContexts.isEmpty()) {
            return;
        }

        /*
         * $$$ TO BE TESTED WITH MULTIPLE VIRTUAL CONTEXTS
         */

        FilterRegistration.Dynamic strutsFilter = servletContext.addFilter(STRUTS_NEW_FILTER_NAME, StrutsPrepareAndExecuteFilter.class);
        ServletRegistration springDispatcherRegistration = servletContext.getServletRegistration(SPRING_DISPATCHER_NAME_ON_WEB_XML);

        for (String vctx : virtualContexts) {
            LOGGER.info("Dynamically adding additional mappings for context: {}", vctx);
            addStrutsMapping(strutsFilter, vctx);
            addApiMapping(springDispatcherRegistration, vctx);
        }
    }

    private static void addStrutsMapping(FilterRegistration.Dynamic filter, String vctx) {
        filter.addMappingForUrlPatterns(DISPATCHER_TYPES, false, Paths.get("/", vctx, "do", "*").toString());
    }

    private static void addApiMapping(ServletRegistration servletRegistration, String vctx) {
        servletRegistration.addMapping(Paths.get("/", vctx, "api", "*").toString());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContextListener.super.contextDestroyed(sce);
    }

    private static final EntLogging.EntLogger LOGGER = EntLogging.EntLogFactory.getSanitizedLogger(VirtualContextListener.class);
}