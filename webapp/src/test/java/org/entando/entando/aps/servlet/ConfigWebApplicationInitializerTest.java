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

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigWebApplicationInitializerTest {

    private ConfigWebApplicationInitializer initializer;

    @Mock
    private ServletContext servletContext;

    @Mock
    private ServletRegistration.Dynamic dynamicRegistration;

    @Mock
    private SessionCookieConfig sessionCookieConfig;

    @BeforeEach
    void setUp() {
        initializer = new ConfigWebApplicationInitializer();

        // Setup default mocks
        when(servletContext.getServletRegistration("springDispatcher")).thenReturn(dynamicRegistration);
        when(servletContext.getSessionCookieConfig()).thenReturn(sessionCookieConfig);
    }

    @AfterEach
    void tearDown() {
        // Clean up system properties
        System.clearProperty("jsessionid.secure");
        System.clearProperty("file.upload.maxSize");
    }

    @Test
    @DisplayName("onStartup should configure both multipart and session cookies")
    void testOnStartupConfiguresBothMultipartAndSessionCookies() throws ServletException {
        // Given
        System.setProperty("file.upload.maxSize", "10485760"); // 10MB
        System.setProperty("jsessionid.secure", "true");

        // When
        initializer.onStartup(servletContext);

        // Then
        verify(dynamicRegistration).setMultipartConfig(any());
        verify(sessionCookieConfig).setSecure(true);
        verify(sessionCookieConfig).setHttpOnly(true);
    }

    @Test
    @DisplayName("Should read file upload max size from system properties")
    void testReadMaxFileSizeFromSystemProperties() throws ServletException {
        // Given - 20MB in system properties
        System.setProperty("file.upload.maxSize", "20971520");

        // When
        initializer.onStartup(servletContext);

        // Then
        ArgumentCaptor<jakarta.servlet.MultipartConfigElement> captor =
            ArgumentCaptor.forClass(jakarta.servlet.MultipartConfigElement.class);
        verify(dynamicRegistration).setMultipartConfig(captor.capture());

        jakarta.servlet.MultipartConfigElement config = captor.getValue();
        assertEquals(20971520L, config.getMaxFileSize());
        assertEquals(41943040L, config.getMaxRequestSize()); // Double the file size

        // Cleanup
        System.clearProperty("file.upload.maxSize");
    }

    @Test
    @DisplayName("Should use default max file size when property not found")
    void testUseDefaultMaxFileSizeWhenPropertyNotFound() throws ServletException {
        // Given - no system property set

        // When
        initializer.onStartup(servletContext);

        // Then
        ArgumentCaptor<jakarta.servlet.MultipartConfigElement> captor =
            ArgumentCaptor.forClass(jakarta.servlet.MultipartConfigElement.class);
        verify(dynamicRegistration).setMultipartConfig(captor.capture());

        jakarta.servlet.MultipartConfigElement config = captor.getValue();
        assertEquals(52428800L, config.getMaxFileSize()); // 50MB default
        assertEquals(104857600L, config.getMaxRequestSize()); // 100MB default
    }

    @Test
    @DisplayName("Should use default max file size when property is invalid")
    void testUseDefaultMaxFileSizeWhenPropertyIsInvalid() throws ServletException {
        // Given - invalid number in system property
        System.setProperty("file.upload.maxSize", "invalid_number");

        // When
        initializer.onStartup(servletContext);

        // Then
        ArgumentCaptor<jakarta.servlet.MultipartConfigElement> captor =
            ArgumentCaptor.forClass(jakarta.servlet.MultipartConfigElement.class);
        verify(dynamicRegistration).setMultipartConfig(captor.capture());

        jakarta.servlet.MultipartConfigElement config = captor.getValue();
        assertEquals(52428800L, config.getMaxFileSize()); // 50MB default
    }

    @Test
    @DisplayName("Should not configure multipart when servlet registration not found")
    void testNotConfigureMultipartWhenServletRegistrationNotFound() throws ServletException {
        // Given
        when(servletContext.getServletRegistration("springDispatcher")).thenReturn(null);

        // When
        initializer.onStartup(servletContext);

        // Then
        verify(dynamicRegistration, never()).setMultipartConfig(any());
    }

    @Test
    @DisplayName("Should read jsessionid.secure=true from system properties")
    void testReadSessionCookieSecureTrueFromSystemProperties() throws ServletException {
        // Given
        System.setProperty("jsessionid.secure", "true");

        // When
        initializer.onStartup(servletContext);

        // Then
        verify(sessionCookieConfig).setSecure(true);
        verify(sessionCookieConfig).setHttpOnly(true);
    }

    @Test
    @DisplayName("Should read jsessionid.secure=false from system properties")
    void testReadSessionCookieSecureFalseFromSystemProperties() throws ServletException {
        // Given
        System.setProperty("jsessionid.secure", "false");

        // When
        initializer.onStartup(servletContext);

        // Then
        verify(sessionCookieConfig).setSecure(false);
        verify(sessionCookieConfig).setHttpOnly(true);
    }

    @Test
    @DisplayName("Should use default secure=false when system property not set")
    void testUseDefaultSecureFalseWhenSystemPropertyNotSet() throws ServletException {
        // Given - no system property set

        // When
        initializer.onStartup(servletContext);

        // Then
        verify(sessionCookieConfig).setSecure(false); // Default is false
        verify(sessionCookieConfig).setHttpOnly(true);
    }

    @Test
    @DisplayName("Should parse jsessionid.secure system property case-insensitively")
    void testParseSessionCookieSecureCaseInsensitively() throws ServletException {
        // Given
        System.setProperty("jsessionid.secure", "TRUE");

        // When
        initializer.onStartup(servletContext);

        // Then
        verify(sessionCookieConfig).setSecure(true);
    }

    @Test
    @DisplayName("Should always set httpOnly to true")
    void testAlwaysSetHttpOnlyToTrue() throws ServletException {
        // Given - no special setup needed

        // When
        initializer.onStartup(servletContext);

        // Then
        verify(sessionCookieConfig).setHttpOnly(true);
    }

    @Test
    @DisplayName("Should handle whitespace in system property values")
    void testHandleWhitespaceInSystemPropertyValues() throws ServletException {
        // Given
        System.setProperty("jsessionid.secure", "  true  ");

        // When
        initializer.onStartup(servletContext);

        // Then
        verify(sessionCookieConfig).setSecure(true);
    }

    @Test
    @DisplayName("Should handle empty string in system property")
    void testHandleEmptyStringInSystemProperty() throws ServletException {
        // Given
        System.setProperty("jsessionid.secure", "");

        // When
        initializer.onStartup(servletContext);

        // Then
        verify(sessionCookieConfig).setSecure(false); // Default
    }

    @Test
    @DisplayName("Should configure multipart with correct file size threshold")
    void testConfigureMultipartWithCorrectFileSizeThreshold() throws ServletException {
        // Given
        System.setProperty("file.upload.maxSize", "10485760");

        // When
        initializer.onStartup(servletContext);

        // Then
        ArgumentCaptor<jakarta.servlet.MultipartConfigElement> captor =
            ArgumentCaptor.forClass(jakarta.servlet.MultipartConfigElement.class);
        verify(dynamicRegistration).setMultipartConfig(captor.capture());

        jakarta.servlet.MultipartConfigElement config = captor.getValue();
        assertEquals(0, config.getFileSizeThreshold()); // Should write all to disk
    }
}