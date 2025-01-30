package org.entando.entando.web.listener;

import com.agiletec.apsadmin.system.dispatcher.StrutsPrepareAndExecuteFilter;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.EnumSet;

public class VirtualContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent ce) {
        ServletContext servletContext = ce.getServletContext();

        servletContext.addFilter("StrutsPrepareAndExecuteFilter", StrutsPrepareAndExecuteFilter.class)
                .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/alessandro/do/*");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContextListener.super.contextDestroyed(sce);
    }
}