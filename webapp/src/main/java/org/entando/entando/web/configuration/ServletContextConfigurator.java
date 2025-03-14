package org.entando.entando.web.configuration;

import com.google.common.collect.ImmutableList;
import org.entando.entando.aps.servlet.routing.VirtualContextHelper;
import org.entando.entando.ent.util.EntLogging;

import javax.servlet.*;
import java.nio.file.Paths;
import java.util.*;

import javax.servlet.annotation.WebListener;

import static org.entando.entando.ent.util.EntLogging.EntLogFactory.*;

/**
 * Dynamically set most of the configurations usually found in web.xml in order to support the
 * dynamic mappings of the virtual-contexts feature.
 */
@WebListener
public class ServletContextConfigurator implements ServletContextListener {
    private static final EntLogging.EntLogger logger = getSanitizedLogger(ServletContextConfigurator.class);

    public static final int SESSION_TIMEOUT = 30;
    public static final boolean SESSION_HTTP_ONLY = true;

    private static List<String> virtualContexts;
    private ServletContext servletContext;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContextListener.super.contextInitialized(sce);

        logger.info("** DYNAMIC CONTEXT CONFIGURATION STARTED **");

        servletContext = sce.getServletContext();
        virtualContexts = VirtualContextHelper.getVirtualContexts();

        //initParameters();
        registerFilters();
        registerBaseServlets();
        registerSpecialServlets();
        configureSessionManagement();
        configureSpecialFiles();

        logger.info("** DYNAMIC CONTEXT CONFIGURATION COMPLETD **");
    }

    private void configureSpecialFiles() {
        servletContext.setInitParameter("welcomeFileList", "index.jsp,adminindex.jsp");
        servletContext.setInitParameter("welcomeFileList", "index.jsp,adminindex.jsp");
    }

    private void configureSessionManagement() {
        servletContext.getSessionCookieConfig().setHttpOnly(SESSION_HTTP_ONLY);
        servletContext.setSessionTimeout(SESSION_TIMEOUT);
    }

    private void registerSpecialServlets() {
        ServletRegistration.Dynamic springDispatcher = servletContext.addServlet("springDispatcher", "org.springframework.web.servlet.DispatcherServlet");
        springDispatcher.setInitParameter("contextConfigLocation", "classpath:spring/web/servlet-context-keycloak.xml");
        springDispatcher.setLoadOnStartup(1);
        springDispatcher.addMapping();
        withAlsoVirtualContexts("/api/*").forEach(springDispatcher::addMapping);
    }

    private void registerBaseServlets() {
        registerServlet("ExceptionHandlerServlet", ENTANDO_EXCEPTION_HANDLER_SERVLET, 1, withAlsoVirtualContexts("/error"));
        registerServlet("ControllerServlet", ENTANDO_CONTROLLER_SERVLET, 1, ImmutableList.<String>builder()
                .add("*.wp", "*.page")
                .addAll(withAlsoVirtualContexts("/pages/*", "/page/*")).build()
        );
        registerServlet("PreviewControllerServlet", ENTANDO_PREVIEW_SERVLET, 1, withAlsoVirtualContexts("/preview/*"));
        registerServlet("ResourceControllerServlet", ENTANDO_PROTECTED_RESOURCE_SERVLET, 1, withAlsoVirtualContexts("/protected/*"));
        registerServlet("Struts2ExtServlet", ENTANDO_STRUTS_2_SERVLET_DISPATCHER, withAlsoVirtualContexts("/ExtStr2/do/*"));
    }

    private void registerFilters() {
        registerFilter("mdcTenantFilter", MDCTENANT_FILTER, List.of("/*"));
        registerFilter("keycloakFilter", SPRING_FILTER_DELEGATOR, List.of("/*"));
        registerFilter("mdcUserFilter", MDCUSER_FILTER, List.of("/*"));
        registerFilter("springSessionRepositoryFilter", SPRING_FILTER_DELEGATOR, "/*", DISPATCHER_TYPES);
        registerFilter("struts2", ENTANDO_STRUTS_FILTER, withAlsoVirtualContexts("/do/*", "/struts/*"));
        registerFilter("XSSFilter", ENTANDO_XSSFILTER, List.of("*.wp", "*.page", "/pages/*"));

        FilterRegistration.Dynamic filter = servletContext.addFilter("CharacterEncodingFilter", "org.springframework.web.filter.CharacterEncodingFilter");
        if (filter == null)
            throw new IllegalStateException("Filter \"CharacterEncodingFilter\" registration failed (already present?)");
        filter.setInitParameter("encoding", "UTF-8");
        filter.setInitParameter("forceEncoding", "true");
        filter.addMappingForUrlPatterns(null, false, "*");
    }

    private void initParameters() {
//        servletContext.setInitParameter("display-name", "Entando Minimal App - powered by Entando");
//        servletContext.setInitParameter("description", "Entando Minimal App - powered by Entando");

        servletContext.setInitParameter("Struts2Config",
                "struts-default.xml,struts-plugin.xml,struts.xml,entando-struts-plugin.xml,japs-struts-plugin.xml");
        servletContext.setInitParameter("org.apache.tiles.definition.DefinitionsFactory.DEFINITIONS_CONFIG",
                "/WEB-INF/apsadmin/tiles.xml,/WEB-INF/plugins/**/apsadmin/**tiles.xml");
        servletContext.setInitParameter("contextClass",
                "org.springframework.web.context.support.XmlWebApplicationContext");

//        servletContext.setInitParameter("contextConfigLocation",
//                "classpath:spring/propertyPlaceholder.xml" + "\n" +
//                        "classpath:spring/baseSystemConfig.xml" + "\n" +
//                        "classpath*:spring/aps/**/**.xml" + "\n" +
//                        "classpath*:spring/apsadmin/**/**.xml" + "\n" +
//                        "classpath*:spring/plugins/**/aps/**/**.xml" + "\n" +
//                        "classpath*:spring/plugins/**/apsadmin/**/**.xml" + "\n" +
//                        "classpath*:spring/aps/managers/keycloak.xml" + "\n" +
//                        "classpath*:spring/aps/managers/cors.xml");
    }

    private List<String> withAlsoVirtualContexts(String... rootMappings) {
        ArrayList<String> res = new ArrayList<>();

        for (String m : rootMappings) {
            res.add(m);
            addContextMapping(res, m);
        }

        return List.copyOf(res);
    }

    private void addContextMapping(ArrayList<String> res, String rootMapping) {
        for (String vctx : virtualContexts) {
            res.add(Paths.get("/", vctx, rootMapping).toString());
        }
    }


    private void registerFilter(String name, String className, List<String> urlPatterns) {
        FilterRegistration.Dynamic filter = servletContext.addFilter(name, className);
        if (filter == null)
            throw new IllegalStateException(String.format("Filter \"%s\" registration failed (already present?)", name));
        for (String urlPattern : urlPatterns) {
            filter.addMappingForUrlPatterns(null, false, urlPattern);
        }
    }


    @SuppressWarnings("SameParameterValue")
    private void registerFilter(
            String name,
            String className,
            String urlPattern,
            EnumSet<DispatcherType> dispatcherTypes
    ) {
        FilterRegistration.Dynamic filter = servletContext.addFilter(name, className);
        if (filter == null)
            throw new IllegalStateException(String.format("Filter \"%s\" registration failed (already present?)", name));
        filter.addMappingForUrlPatterns(dispatcherTypes, false, urlPattern);
    }

    @SuppressWarnings("SameParameterValue")
    private void registerServlet(String name, String className, List<String> urlPatterns) {
        registerServlet(name, className, -1, urlPatterns); // Default load-on-startup if not provided
    }

    private void registerServlet(
            String name, String className, int startPriority, List<String> urlPatterns
    ) {
        ServletRegistration.Dynamic servlet = servletContext.addServlet(name, className);
        if (servlet == null)
            throw new IllegalStateException(String.format("Servlet \"%s\" registration failed (already present?)", name));
        if (startPriority > 0) {
            servlet.setLoadOnStartup(startPriority);
        }
        urlPatterns.forEach(servlet::addMapping);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContextListener.super.contextDestroyed(sce);
    }

    public static final String MDCTENANT_FILTER = "org.entando.entando.web.filter.MDCTenantFilter";
    public static final String SPRING_FILTER_DELEGATOR = "org.springframework.web.filter.DelegatingFilterProxy";
    public static final String MDCUSER_FILTER = "org.entando.entando.web.filter.MDCUserFilter";
    public static final EnumSet<DispatcherType> DISPATCHER_TYPES = EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR);
    public static final String ENTANDO_STRUTS_FILTER = "com.agiletec.apsadmin.system.dispatcher.StrutsPrepareAndExecuteFilter";
    public static final String ENTANDO_XSSFILTER = "org.entando.entando.aps.servlet.XSSFilter";
    public static final String ENTANDO_EXCEPTION_HANDLER_SERVLET = "org.entando.entando.aps.servlet.ExceptionHandlerServlet";
    public static final String ENTANDO_CONTROLLER_SERVLET = "org.entando.entando.aps.servlet.ControllerServlet";
    public static final String ENTANDO_PREVIEW_SERVLET = "org.entando.entando.aps.servlet.PreviewControllerServlet";
    public static final String ENTANDO_PROTECTED_RESOURCE_SERVLET = "org.entando.entando.aps.servlet.ProtectedResourceWardenServlet";
    public static final String ENTANDO_STRUTS_2_SERVLET_DISPATCHER = "org.entando.entando.aps.internalservlet.system.dispatcher.Struts2ServletDispatcher";
}