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

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;
import org.springframework.web.WebApplicationInitializer;

/**
 * Configures multipart file upload settings and session cookie security for the application.
 *
 * This initializer runs VERY EARLY before web.xml processing and before logging is initialized,
 * allowing dynamic configuration of servlet multipart settings and session cookie configuration.
 *
 * System properties read (can be set via pom.xml):
 * - file.upload.maxSize: Maximum file upload size in bytes
 * - jsessionid.secure: Whether to enable the secure flag on session cookies (true for HTTPS-only)
 *
 * @author ffalqui
 */
public class ConfigWebApplicationInitializer implements WebApplicationInitializer {

    private static final String FILE_UPLOAD_MAX_SIZE_PROPERTY = "file.upload.maxSize";
    private static final String SESSION_COOKIE_SECURE_SYSTEM_PROPERTY = "jsessionid.secure";
    private static final String SPRING_DISPATCHER_SERVLET_NAME = "springDispatcher";

    // Default values (50MB file, 100MB request)
    private static final long DEFAULT_MAX_FILE_SIZE = 52428800L; // 50MB
    private static final long DEFAULT_MAX_REQUEST_SIZE = 104857600L; // 100MB
    private static final int DEFAULT_FILE_SIZE_THRESHOLD = 0; // Write all to disk
    private static final boolean DEFAULT_SESSION_COOKIE_SECURE = false; // Default to false for development

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        // Configure multipart file upload settings
        configureMultipart(servletContext);

        // Configure session cookie security
        configureSessionCookie(servletContext);
    }

    /**
     * Configures multipart file upload settings for the Spring DispatcherServlet
     *
     * @param servletContext the servlet context
     */
    private void configureMultipart(ServletContext servletContext) {
        //TODO fixup logging format
        System.out.println("ConfigWebApplicationInitializer: Configuring multipart file upload settings");

        long maxFileSize = readMaxFileSizeFromSystemProperty(servletContext);
        long maxRequestSize = maxFileSize * 2; // Double the file size for request size

        ServletRegistration servletRegistration = servletContext.getServletRegistration(SPRING_DISPATCHER_SERVLET_NAME);

        if (servletRegistration != null && servletRegistration instanceof ServletRegistration.Dynamic) {
            MultipartConfigElement multipartConfig = new MultipartConfigElement(
                null,                       // location (temp directory)
                maxFileSize,                // maxFileSize
                maxRequestSize,             // maxRequestSize
                DEFAULT_FILE_SIZE_THRESHOLD // fileSizeThreshold
            );

            ((ServletRegistration.Dynamic) servletRegistration).setMultipartConfig(multipartConfig);

            System.out.println(String.format(
                "ConfigWebApplicationInitializer: Multipart config applied to '%s' - maxFileSize: %d bytes (%.2f MB), maxRequestSize: %d bytes (%.2f MB)",
                SPRING_DISPATCHER_SERVLET_NAME,
                maxFileSize,
                maxFileSize / 1024.0 / 1024.0,
                maxRequestSize,
                maxRequestSize / 1024.0 / 1024.0
            ));
        } else {
            System.err.println("ConfigWebApplicationInitializer: ServletRegistration '" + SPRING_DISPATCHER_SERVLET_NAME +
                "' not found or not dynamic. Multipart config not applied.");
        }
    }

    /**
     * Configures session cookie security settings programmatically
     *
     * @param servletContext the servlet context
     */
    private void configureSessionCookie(ServletContext servletContext) {
        System.out.println("ConfigWebApplicationInitializer: Configuring session cookie security");

        boolean secureEnabled = readSessionCookieSecureFromSystemProperty(servletContext);

        SessionCookieConfig cookieConfig = servletContext.getSessionCookieConfig();
        cookieConfig.setSecure(secureEnabled);
        cookieConfig.setHttpOnly(true);
        // Note: same-site-mode is set in web.xml as it's not available in SessionCookieConfig API

        System.out.println("ConfigWebApplicationInitializer: Session cookie 'secure' flag set to: " + secureEnabled + " (httpOnly: true)");
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
                System.out.println("ConfigWebApplicationInitializer: Loaded " + FILE_UPLOAD_MAX_SIZE_PROPERTY +
                    " = " + maxSize + " bytes from system properties");
                return maxSize;
            } catch (NumberFormatException e) {
                System.err.println("ConfigWebApplicationInitializer: Invalid value for " + FILE_UPLOAD_MAX_SIZE_PROPERTY +
                    ": '" + maxSizeStr + "'. Using default: " + DEFAULT_MAX_FILE_SIZE + " bytes");
                return DEFAULT_MAX_FILE_SIZE;
            }
        } else {
            System.out.println("ConfigWebApplicationInitializer: System property " + FILE_UPLOAD_MAX_SIZE_PROPERTY +
                " not found. Using default: " + DEFAULT_MAX_FILE_SIZE + " bytes");
            return DEFAULT_MAX_FILE_SIZE;
        }
    }

    /**
     * Reads the jsessionid.secure system property (can be set via pom.xml)
     *
     * @param servletContext the servlet context for logging
     * @return true if secure cookies should be enabled, false otherwise
     */
    private boolean readSessionCookieSecureFromSystemProperty(ServletContext servletContext) {
        String secureStr = System.getProperty(SESSION_COOKIE_SECURE_SYSTEM_PROPERTY);

        if (secureStr != null && !secureStr.trim().isEmpty()) {
            boolean secure = Boolean.parseBoolean(secureStr.trim());
            System.out.println("ConfigWebApplicationInitializer: Loaded " + SESSION_COOKIE_SECURE_SYSTEM_PROPERTY +
                " = " + secure + " from system properties");
            return secure;
        } else {
            System.out.println("ConfigWebApplicationInitializer: System property " + SESSION_COOKIE_SECURE_SYSTEM_PROPERTY +
                " not found. Using default: " + DEFAULT_SESSION_COOKIE_SECURE);
            return DEFAULT_SESSION_COOKIE_SECURE;
        }
    }
}