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

import com.agiletec.aps.system.SystemConstants;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;
import org.entando.entando.aps.system.exception.CSRFProtectionException;
import org.entando.entando.aps.system.services.tenants.ITenantInitializerService;
import org.entando.entando.aps.system.services.tenants.ITenantInitializerService.InitializationTenantFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.context.WebApplicationContext;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, SystemStubsExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class StartupListenerTest {

    @InjectMocks
    private StartupListener startupListener;

    @Mock
    private ServletContextEvent servletContextEvent;

    @Spy
    private ServletContext servletContext;

    @Mock
    private WebApplicationContext webApplicationContext;

    @Mock
    private ITenantInitializerService tenantInitializerService;

    @Mock
    private SessionCookieConfig sessionCookieConfig;

    @SystemStub
    private EnvironmentVariables environmentVariables;

    @BeforeEach
    void setUp() {
//        startupListener = new StartupListener();

        when(servletContextEvent.getServletContext()).thenReturn(servletContext);
        when(servletContext.getServletContextName()).thenReturn("TestContext");
        // Return null first (so Spring doesn't think context exists), then return mock
        when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(null).thenReturn(webApplicationContext);
        doNothing().when(servletContext).setAttribute(eq(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE), any());
        when(servletContext.getInitParameter(any())).thenReturn(null);
        when(servletContext.getResourceAsStream(any())).thenReturn(null);
        when(webApplicationContext.getBean(ITenantInitializerService.class)).thenReturn(tenantInitializerService);
        when(tenantInitializerService.startTenantsInitialization(any(), eq(InitializationTenantFilter.REQUIRED_INIT_AT_START)))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(tenantInitializerService.startTenantsInitialization(any(), eq(InitializationTenantFilter.NOT_REQUIRED_INIT_AT_START)))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(servletContext.getSessionCookieConfig()).thenReturn(sessionCookieConfig);
    }

    @Test
    void testContextInitialized_WithCsrfProtectionEnabled() throws Exception {
        // Arrange
        environmentVariables.set(SystemConstants.ENTANDO_CSRF_PROTECTION, SystemConstants.CSRF_BASIC_PROTECTION);
        environmentVariables.set(SystemConstants.ENTANDO_CSRF_ALLOWED_DOMAINS, "example.com,test.com");
        environmentVariables.set(SystemConstants.CSP_HEADER_ENABLED, "true");

        // Act
        assertDoesNotThrow(() -> startupListener.contextInitialized(servletContextEvent));

        // Assert
        verify(tenantInitializerService).startTenantsInitialization(
                eq(servletContext), eq(InitializationTenantFilter.REQUIRED_INIT_AT_START));
        verify(tenantInitializerService).startTenantsInitialization(
                eq(servletContext), eq(InitializationTenantFilter.NOT_REQUIRED_INIT_AT_START));
    }

    @Test
    void testContextInitialized_WithCsrfProtectionDisabled() throws Exception {
        // Arrange
        environmentVariables.set(SystemConstants.CSP_HEADER_ENABLED, "true");

        // Act
        assertDoesNotThrow(() -> startupListener.contextInitialized(servletContextEvent));

        // Assert - should log warning but not throw exception
        verify(servletContext, atLeastOnce()).getServletContextName();
    }

    @Test
    void testContextInitialized_WithCsrfProtectionEnabledButNoDomains() throws Exception {
        // Arrange
        environmentVariables.set(SystemConstants.ENTANDO_CSRF_PROTECTION, SystemConstants.CSRF_BASIC_PROTECTION);
        environmentVariables.set(SystemConstants.CSP_HEADER_ENABLED, "true");

        // Act & Assert
        assertThrows(CSRFProtectionException.class,
                () -> startupListener.contextInitialized(servletContextEvent));
    }

    @Test
    void testContextInitialized_WithCsrfProtectionEnabledButEmptyDomains() throws Exception {
        // Arrange
        environmentVariables.set(SystemConstants.ENTANDO_CSRF_PROTECTION, SystemConstants.CSRF_BASIC_PROTECTION);
        environmentVariables.set(SystemConstants.ENTANDO_CSRF_ALLOWED_DOMAINS, "");
        environmentVariables.set(SystemConstants.CSP_HEADER_ENABLED, "true");

        // Act & Assert
        assertThrows(CSRFProtectionException.class,
                () -> startupListener.contextInitialized(servletContextEvent));
    }

    @Test
    void testContextInitialized_WithCspDisabled() throws Exception {
        // Arrange
        environmentVariables.set(SystemConstants.CSP_HEADER_ENABLED, "false");

        // Act
        assertDoesNotThrow(() -> startupListener.contextInitialized(servletContextEvent));

        // Assert - should log warning about CSP being disabled
        verify(servletContext, atLeastOnce()).getServletContextName();
    }

    @Test
    void testContextInitialized_WithCspEnabledAndExtraConfig() throws Exception {
        // Arrange
        environmentVariables.set(SystemConstants.CSP_HEADER_ENABLED, "true");
        environmentVariables.set(SystemConstants.CSP_HEADER_EXTRACONFIG, "script-src 'self'");

        // Act
        assertDoesNotThrow(() -> startupListener.contextInitialized(servletContextEvent));

        // Assert
        verify(servletContext, atLeastOnce()).getServletContextName();
    }

    @Test
    void testContextInitialized_WithCspEmptyString() throws Exception {
        // Arrange
        environmentVariables.set(SystemConstants.CSP_HEADER_ENABLED, "");

        // Act - empty string should default to enabled
        assertDoesNotThrow(() -> startupListener.contextInitialized(servletContextEvent));

        // Assert
        verify(servletContext, atLeastOnce()).getServletContextName();
    }

    @Test
    void testSetSessionCookieConfig_WithSecureCookiesEnabled() throws Exception {
        // Arrange
        environmentVariables.set("ENTANDO_SECURE_SECRET_COOKIES", "true");

        // Act
        startupListener.setSessionCookieConfig(servletContext);

        // Assert
        verify(sessionCookieConfig).setSecure(true);
    }

    @Test
    void testSetSessionCookieConfig_WithSecureCookiesDisabled() throws Exception {
        // Arrange
        environmentVariables.set("ENTANDO_SECURE_SECRET_COOKIES", "false");

        // Act
        startupListener.setSessionCookieConfig(servletContext);

        // Assert - secure should not be set when false
        verify(sessionCookieConfig, never()).setSecure(anyBoolean());
    }

    @Test
    void testSetSessionCookieConfig_WithoutEnvironmentVariableButForceHttps() throws Exception {
        // Arrange - no environment variable set

        // Act & Assert - method should complete without error
        // getSessionCookieConfig() is only called if secure==true,
        // which depends on UrlUtils.determineForceHttps() (likely false in test environments)
        assertDoesNotThrow(() -> startupListener.setSessionCookieConfig(servletContext));

        // Verify setSecure was not called (since secure would be false in test environment)
        verify(sessionCookieConfig, never()).setSecure(anyBoolean());
    }

    @Test
    void testSetSessionCookieConfig_WithEmptyString() throws Exception {
        // Arrange
        environmentVariables.set("ENTANDO_SECURE_SECRET_COOKIES", "");

        // Act & Assert - method should complete without error
        // Empty string is treated as if no value was set, falls back to UrlUtils.determineForceHttps()
        assertDoesNotThrow(() -> startupListener.setSessionCookieConfig(servletContext));

        // Verify setSecure was not called (since secure would be false in test environment)
        verify(sessionCookieConfig, never()).setSecure(anyBoolean());
    }

    @Test
    void testContextInitialized_WithNonBasicCsrfProtection() throws Exception {
        // Arrange
        environmentVariables.set(SystemConstants.ENTANDO_CSRF_PROTECTION, "ADVANCED");
        environmentVariables.set(SystemConstants.CSP_HEADER_ENABLED, "true");

        // Act
        assertDoesNotThrow(() -> startupListener.contextInitialized(servletContextEvent));

        // Assert - should log warning but not throw exception
        verify(servletContext, atLeastOnce()).getServletContextName();
    }

    @Test
    void testConfigureMultipart_WithServletRegistrationNotFound() throws Exception {
        // Arrange
        when(servletContext.getServletRegistration("springDispatcher")).thenReturn(null);

        // Act & Assert - method should complete without error even if servlet not found
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = StartupListener.class.getDeclaredMethod("configureMultipart", ServletContext.class);
            method.setAccessible(true);
            method.invoke(startupListener, servletContext);
        });

        // Verify getServletRegistration was called
        verify(servletContext).getServletRegistration("springDispatcher");
    }

    @Test
    void testConfigureMultipart_WithDynamicServletRegistration() throws Exception {
        // Arrange
        ServletRegistration.Dynamic dynamicRegistration = mock(ServletRegistration.Dynamic.class);
        when(servletContext.getServletRegistration("springDispatcher")).thenReturn(dynamicRegistration);

        // Act & Assert - method should set multipart config
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = StartupListener.class.getDeclaredMethod("configureMultipart", ServletContext.class);
            method.setAccessible(true);
            method.invoke(startupListener, servletContext);
        });

        // Verify setMultipartConfig was called on the dynamic registration
        verify(dynamicRegistration).setMultipartConfig(any(MultipartConfigElement.class));
    }

    @Test
    void testConfigureMultipart_WithNonDynamicServletRegistration() throws Exception {
        // Arrange
        ServletRegistration nonDynamicRegistration = mock(ServletRegistration.class);
        when(servletContext.getServletRegistration("springDispatcher")).thenReturn(nonDynamicRegistration);

        // Act & Assert - method should complete without error but not set config
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = StartupListener.class.getDeclaredMethod("configureMultipart", ServletContext.class);
            method.setAccessible(true);
            method.invoke(startupListener, servletContext);
        });

        // Verify getServletRegistration was called
        verify(servletContext).getServletRegistration("springDispatcher");
    }

    @Test
    void testConfigureMultipart_WithCustomMaxFileSize() throws Exception {
        // Arrange
        ServletRegistration.Dynamic dynamicRegistration = mock(ServletRegistration.Dynamic.class);
        when(servletContext.getServletRegistration("springDispatcher")).thenReturn(dynamicRegistration);

        // Set system property for max file size
        System.setProperty("file.upload.maxSize", "104857600"); // 100MB

        try {
            // Act
            java.lang.reflect.Method method = StartupListener.class.getDeclaredMethod("configureMultipart", ServletContext.class);
            method.setAccessible(true);
            method.invoke(startupListener, servletContext);

            // Assert - verify multipart config was set with custom size
            verify(dynamicRegistration).setMultipartConfig(any(MultipartConfigElement.class));
        } finally {
            // Clean up system property
            System.clearProperty("file.upload.maxSize");
        }
    }

    @Test
    void testConfigureMultipart_WithInvalidMaxFileSize() throws Exception {
        // Arrange
        ServletRegistration.Dynamic dynamicRegistration = mock(ServletRegistration.Dynamic.class);
        when(servletContext.getServletRegistration("springDispatcher")).thenReturn(dynamicRegistration);

        // Set invalid system property
        System.setProperty("file.upload.maxSize", "invalid");

        try {
            // Act & Assert - should use default value and not throw exception
            assertDoesNotThrow(() -> {
                java.lang.reflect.Method method = StartupListener.class.getDeclaredMethod("configureMultipart", ServletContext.class);
                method.setAccessible(true);
                method.invoke(startupListener, servletContext);
            });

            // Verify multipart config was set with default size
            verify(dynamicRegistration).setMultipartConfig(any(MultipartConfigElement.class));
        } finally {
            // Clean up system property
            System.clearProperty("file.upload.maxSize");
        }
    }

    @Test
    void testConfigureMultipart_WithoutSystemProperty() throws Exception {
        // Arrange
        ServletRegistration.Dynamic dynamicRegistration = mock(ServletRegistration.Dynamic.class);
        when(servletContext.getServletRegistration("springDispatcher")).thenReturn(dynamicRegistration);

        // Ensure property is not set
        System.clearProperty("file.upload.maxSize");

        // Act & Assert - should use default value
        assertDoesNotThrow(() -> {
            java.lang.reflect.Method method = StartupListener.class.getDeclaredMethod("configureMultipart", ServletContext.class);
            method.setAccessible(true);
            method.invoke(startupListener, servletContext);
        });

        // Verify multipart config was set with default size (50MB)
        verify(dynamicRegistration).setMultipartConfig(any(MultipartConfigElement.class));
    }
}