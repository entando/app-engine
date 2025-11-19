/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.aps.servlet;

import com.agiletec.aps.system.ApsSystemUtils;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import jakarta.servlet.*;
import org.entando.entando.aps.system.exception.CSRFProtectionException;
import org.entando.entando.aps.system.services.tenants.ITenantInitializerService;
import org.entando.entando.aps.system.services.tenants.ITenantInitializerService.InitializationTenantFilter;
import org.entando.entando.aps.util.UrlUtils;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * Init the system when the web application is started
 *
 * @author E.Santoboni
 */
public class StartupListener extends org.springframework.web.context.ContextLoaderListener {

    private static final String FILE_UPLOAD_MAX_SIZE_PROPERTY = "file.upload.maxSize";
    private static final String SPRING_DISPATCHER_SERVLET_NAME = "springDispatcher";
    // ENV flag jsessionid cookie secure
    private static final String ENTANDO_SECURE_SECRET_COOKIES = "ENTANDO_SECURE_SECRET_COOKIES";
    // Default values (50MB file, 100MB request)
    private static final long DEFAULT_MAX_FILE_SIZE = 52428800L; // 50MB
    private static final long DEFAULT_MAX_REQUEST_SIZE = 104857600L; // 100MB
    private static final int DEFAULT_FILE_SIZE_THRESHOLD = 0; // Write all to disk
    private static final boolean DEFAULT_SESSION_COOKIE_SECURE = false; // Default to false for development

    private static final EntLogger LOGGER = EntLogFactory.getSanitizedLogger(StartupListener.class);

    @Override
    public void contextInitialized(ServletContextEvent event) {
        long startMs = System.currentTimeMillis();
        ServletContext svCtx = event.getServletContext();
        String msg = this.getClass().getName() + ": INIT " + svCtx.getServletContextName();
        ApsSystemUtils.directStdoutTrace(msg, true);
        super.contextInitialized(event);
        msg = this.getClass().getName() + ": INIT DONE " + svCtx.getServletContextName();
        ApsSystemUtils.directStdoutTrace(msg, true);

        boolean isActive = Objects.nonNull(System.getenv(SystemConstants.ENTANDO_CSRF_PROTECTION));
        String whiteList = System.getenv(SystemConstants.ENTANDO_CSRF_ALLOWED_DOMAINS);

        if (!isActive || !SystemConstants.CSRF_BASIC_PROTECTION.equals(System.getenv(SystemConstants.ENTANDO_CSRF_PROTECTION))) {
            LOGGER.warn("CSRF protection is not enabled");
        } else if (SystemConstants.CSRF_BASIC_PROTECTION.equals(System.getenv(SystemConstants.ENTANDO_CSRF_PROTECTION))) {
            if (whiteList != null && !whiteList.equals("")) {
                String message = "CSRF protection is enabled Domains --> ".concat(whiteList);
                LOGGER.info(message);
            } else {
                LOGGER.error("CSRF protection is enabled but the domains are initialized. Please initialize the domains");
                throw new CSRFProtectionException("CSRF protection is enabled but the domains are not initialized. Please initialize the domains");
            }
        }
        
        String cspEnabled = System.getenv(SystemConstants.CSP_HEADER_ENABLED);
        if (StringUtils.isEmpty(cspEnabled) || Boolean.TRUE.toString().equalsIgnoreCase(cspEnabled)) {
            LOGGER.info("Content Security Policy (CSP) header is enabled");
            String cspExtraConfig = System.getenv(SystemConstants.CSP_HEADER_EXTRACONFIG);
            if (!StringUtils.isEmpty(cspExtraConfig)) {
                LOGGER.info("Content Security Policy (CSP) extra-config set to: " + cspExtraConfig);
            }
        } else {
            LOGGER.warn("Content Security Policy (CSP) header is not enabled");
        }

        ITenantInitializerService tenantAsynchInitService = ApsWebApplicationUtils.getBean(ITenantInitializerService.class, svCtx);

        tenantAsynchInitService.startTenantsInitialization(svCtx, InitializationTenantFilter.REQUIRED_INIT_AT_START).join();

        tenantAsynchInitService.startTenantsInitialization(svCtx, InitializationTenantFilter.NOT_REQUIRED_INIT_AT_START);

        this.setSessionCookieConfig(svCtx);
        this.configureMultipart(svCtx);

        long endMs = System.currentTimeMillis();
        String executionTimeMsg = String.format("%s: contextInitialized takes ms:'%s' of execution",
                this.getClass().getName(), endMs - startMs);
        ApsSystemUtils.directStdoutTrace(executionTimeMsg, true);

    }

    /**
     * Configures secure flag for cookie-config
     *
     * @param svCtx the servlet context
     */
    protected void setSessionCookieConfig(ServletContext svCtx) {
        String secureFlag = System.getenv(ENTANDO_SECURE_SECRET_COOKIES);
        boolean secure = StringUtils.isNotEmpty(secureFlag) ?
                Boolean.parseBoolean(secureFlag) :
                UrlUtils.determineForceHttps();

        if (secure){
            SessionCookieConfig sessionCookieConfig = svCtx.getSessionCookieConfig();
            sessionCookieConfig.setSecure(secure);
            LOGGER.info("Jsessionid cookie Secure flag: TRUE");
        }
        // Note: same-site-mode is set in web.xml or for tomcat in JSessionIdSameSiteCookieProcessor as it's not available in SessionCookieConfig API

    }

    /**
     * Configures multipart file upload settings for the Spring DispatcherServlet
     *
     * @param svCtx the servlet context
     */
    private void configureMultipart(ServletContext svCtx) {
        LOGGER.info("ConfigWebApplicationInitializer: Configuring multipart file upload settings");

        long maxFileSize = readMaxFileSizeFromSystemProperty(svCtx);
        long maxRequestSize = maxFileSize * 2; // Double the file size for request size

        ServletRegistration servletRegistration = svCtx.getServletRegistration(SPRING_DISPATCHER_SERVLET_NAME);

        if (servletRegistration instanceof ServletRegistration.Dynamic) {
            MultipartConfigElement multipartConfig = new MultipartConfigElement(
                    null,                       // location (temp directory)
                    maxFileSize,                // maxFileSize
                    maxRequestSize,             // maxRequestSize
                    DEFAULT_FILE_SIZE_THRESHOLD // fileSizeThreshold
            );

            ((ServletRegistration.Dynamic) servletRegistration).setMultipartConfig(multipartConfig);

            LOGGER.info(String.format(
                    "ConfigWebApplicationInitializer: Multipart config applied to '%s' - maxFileSize: %d bytes (%.2f MB), maxRequestSize: %d bytes (%.2f MB)",
                    SPRING_DISPATCHER_SERVLET_NAME,
                    maxFileSize,
                    maxFileSize / 1024.0 / 1024.0,
                    maxRequestSize,
                    maxRequestSize / 1024.0 / 1024.0
            ));
        } else {
            LOGGER.info("ConfigWebApplicationInitializer: ServletRegistration '" + SPRING_DISPATCHER_SERVLET_NAME +
                    "' not found or not dynamic. Multipart config not applied.");
        }
    }

    /**
     * Reads the file.upload.maxSize system property (can be set via pom.xml)
     *
     * @param servletContext the servlet context for logging
     * @return the max file size in bytes, or default if property not found
     */
    private long readMaxFileSizeFromSystemProperty(ServletContext servletContext) {
        String maxSizeStr = System.getProperty(FILE_UPLOAD_MAX_SIZE_PROPERTY);

        if (maxSizeStr != null && !maxSizeStr.trim().isEmpty()) {
            try {
                long maxSize = Long.parseLong(maxSizeStr.trim());
                LOGGER.info("ConfigWebApplicationInitializer: Loaded " + FILE_UPLOAD_MAX_SIZE_PROPERTY +
                        " = " + maxSize + " bytes from system properties");
                return maxSize;
            } catch (NumberFormatException e) {
                LOGGER.info("ConfigWebApplicationInitializer: Invalid value for " + FILE_UPLOAD_MAX_SIZE_PROPERTY +
                        ": '" + maxSizeStr + "'. Using default: " + DEFAULT_MAX_FILE_SIZE + " bytes");
                return DEFAULT_MAX_FILE_SIZE;
            }
        } else {
            LOGGER.info("ConfigWebApplicationInitializer: System property " + FILE_UPLOAD_MAX_SIZE_PROPERTY +
                    " not found. Using default: " + DEFAULT_MAX_FILE_SIZE + " bytes");
            return DEFAULT_MAX_FILE_SIZE;
        }
    }

}
