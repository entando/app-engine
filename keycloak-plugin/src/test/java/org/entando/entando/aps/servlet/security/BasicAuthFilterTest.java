/*
 * Copyright 2022-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.servlet.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.User;
import com.agiletec.aps.system.services.user.UserDetails;
import com.google.common.net.HttpHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.aps.util.UrlUtils;
import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ExtendWith(MockitoExtension.class)
class BasicAuthFilterTest {

    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "testpassword";

    @Mock
    private IUserManager userManager;
    @Mock
    private IAuthenticationProviderManager authenticationProviderManager;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private ServletContext servletContext;
    @Mock
    private WebApplicationContext webApplicationContext;
    @Mock
    private ITenantManager tenantManager;
    @Mock
    private FilterChain filterChain;
    @Mock
    private ServletOutputStream outputStream;

    private BasicAuthFilter basicAuthFilter;

    @BeforeEach
    void setUp() {
        basicAuthFilter = new BasicAuthFilter(userManager, authenticationProviderManager);

        Mockito.lenient().when(request.getSession()).thenReturn(session);
        Mockito.lenient().when(request.getHeader(UrlUtils.ENTANDO_TENANT_CODE_CUSTOM_HEADER)).thenReturn(null);
        Mockito.lenient().when(request.getHeader(HttpHeaders.X_FORWARDED_HOST)).thenReturn(null);
        Mockito.lenient().when(request.getHeader(HttpHeaders.HOST)).thenReturn("localhost");
        Mockito.lenient().when(request.getServerName()).thenReturn("localhost");
        Mockito.lenient().when(session.getServletContext()).thenReturn(servletContext);
        Mockito.lenient().when(webApplicationContext.getBean(ITenantManager.class)).thenReturn(tenantManager);
    }

    @Test
    void attemptAuthentication_noAuthorizationHeader_shouldReturnGuestAuthentication() {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getServletPath()).thenReturn("/api");

        User guestUser = createGuestUser();
        when(userManager.getGuestUser()).thenReturn(guestUser);

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webApplicationContext);

            Authentication auth = basicAuthFilter.attemptAuthentication(request, response);

            assertNotNull(auth);
            assertInstanceOf(GuestAuthentication.class, auth);
            assertFalse(auth.isAuthenticated());
            verify(request).setAttribute(eq("user"), any());
        }
    }

    @Test
    void attemptAuthentication_emptyAuthorizationHeader_shouldReturnGuestAuthentication() {
        when(request.getHeader("Authorization")).thenReturn("");
        when(request.getServletPath()).thenReturn("/api");

        User guestUser = createGuestUser();
        when(userManager.getGuestUser()).thenReturn(guestUser);

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webApplicationContext);

            Authentication auth = basicAuthFilter.attemptAuthentication(request, response);

            assertNotNull(auth);
            assertInstanceOf(GuestAuthentication.class, auth);
        }
    }

    @Test
    void attemptAuthentication_validBasicAuth_shouldReturnUserAuthentication() throws Exception {
        String credentials = USERNAME + ":" + PASSWORD;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Basic " + encodedCredentials);
        when(request.getServletPath()).thenReturn("/api");

        User user = createActiveUser();
        when(authenticationProviderManager.getUser(USERNAME, PASSWORD)).thenReturn(user);

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webApplicationContext);

            Authentication auth = basicAuthFilter.attemptAuthentication(request, response);

            assertNotNull(auth);
            assertInstanceOf(UserAuthentication.class, auth);
            assertTrue(auth.isAuthenticated());
            assertEquals(user, auth.getPrincipal());
            verify(request).setAttribute(eq("user"), eq(user));
        }
    }

    @Test
    void attemptAuthentication_validBasicAuthLowercase_shouldReturnUserAuthentication() throws Exception {
        String credentials = USERNAME + ":" + PASSWORD;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("basic " + encodedCredentials);
        when(request.getServletPath()).thenReturn("/api");

        User user = createActiveUser();
        when(authenticationProviderManager.getUser(USERNAME, PASSWORD)).thenReturn(user);

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webApplicationContext);

            Authentication auth = basicAuthFilter.attemptAuthentication(request, response);

            assertNotNull(auth);
            assertInstanceOf(UserAuthentication.class, auth);
            assertTrue(auth.isAuthenticated());
        }
    }

    @Test
    void attemptAuthentication_invalidCredentials_shouldThrowBadCredentialsException() throws Exception {
        String credentials = USERNAME + ":" + PASSWORD;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Basic " + encodedCredentials);

        when(authenticationProviderManager.getUser(USERNAME, PASSWORD)).thenReturn(null);

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webApplicationContext);

            BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                    () -> basicAuthFilter.attemptAuthentication(request, response));

            assertEquals("Invalid username or password", exception.getMessage());
        }
    }

    @Test
    void attemptAuthentication_disabledUser_shouldThrowBadCredentialsException() throws Exception {
        String credentials = USERNAME + ":" + PASSWORD;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Basic " + encodedCredentials);

        User user = createDisabledUser();
        when(authenticationProviderManager.getUser(USERNAME, PASSWORD)).thenReturn(user);

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webApplicationContext);

            BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                    () -> basicAuthFilter.attemptAuthentication(request, response));

            assertEquals("User account is disabled", exception.getMessage());
        }
    }

    @Test
    void attemptAuthentication_expiredAccount_shouldThrowBadCredentialsException() throws Exception {
        String credentials = USERNAME + ":" + PASSWORD;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Basic " + encodedCredentials);

        // Use a mock to control isAccountNotExpired() return value
        UserDetails user = mock(UserDetails.class);
        Mockito.lenient().when(user.getUsername()).thenReturn(USERNAME);
        when(user.isDisabled()).thenReturn(false);
        when(user.isAccountNotExpired()).thenReturn(false); // Account is expired

        when(authenticationProviderManager.getUser(USERNAME, PASSWORD)).thenReturn(user);

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webApplicationContext);

            BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                    () -> basicAuthFilter.attemptAuthentication(request, response));

            assertEquals("User account has expired", exception.getMessage());
        }
    }

    @Test
    void attemptAuthentication_invalidBase64Encoding_shouldThrowBadCredentialsException() {
        when(request.getHeader("Authorization")).thenReturn("Basic not-valid-base64!!!");

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webApplicationContext);

            BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                    () -> basicAuthFilter.attemptAuthentication(request, response));

            assertEquals("Invalid Basic Auth encoding", exception.getMessage());
        }
    }

    @Test
    void attemptAuthentication_noColonInCredentials_shouldThrowBadCredentialsException() {
        String invalidCredentials = "usernameWithoutPassword";
        String encodedCredentials = Base64.getEncoder().encodeToString(invalidCredentials.getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Basic " + encodedCredentials);

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webApplicationContext);

            BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                    () -> basicAuthFilter.attemptAuthentication(request, response));

            assertEquals("Invalid Basic Auth format", exception.getMessage());
        }
    }

    @Test
    void attemptAuthentication_emptyUsername_shouldThrowBadCredentialsException() {
        String credentials = ":" + PASSWORD;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Basic " + encodedCredentials);

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webApplicationContext);

            BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                    () -> basicAuthFilter.attemptAuthentication(request, response));

            assertEquals("Username and password are required", exception.getMessage());
        }
    }

    @Test
    void attemptAuthentication_emptyPassword_shouldThrowBadCredentialsException() {
        String credentials = USERNAME + ":";
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Basic " + encodedCredentials);

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webApplicationContext);

            BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                    () -> basicAuthFilter.attemptAuthentication(request, response));

            assertEquals("Username and password are required", exception.getMessage());
        }
    }

    @Test
    void attemptAuthentication_bearerToken_shouldThrowBadCredentialsException() {
        when(request.getHeader("Authorization")).thenReturn("Bearer some-jwt-token");

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webApplicationContext);

            BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                    () -> basicAuthFilter.attemptAuthentication(request, response));

            assertEquals("Invalid or expired token", exception.getMessage());
        }
    }

    @Test
    void attemptAuthentication_unknownAuthScheme_shouldReturnGuestAuthentication() {
        when(request.getHeader("Authorization")).thenReturn("Digest somevalue");
        when(request.getServletPath()).thenReturn("/api");

        User guestUser = createGuestUser();
        when(userManager.getGuestUser()).thenReturn(guestUser);

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webApplicationContext);

            Authentication auth = basicAuthFilter.attemptAuthentication(request, response);

            assertNotNull(auth);
            assertInstanceOf(GuestAuthentication.class, auth);
        }
    }

    @Test
    void attemptAuthentication_nonApiPath_shouldSetSessionAttributes() throws Exception {
        String credentials = USERNAME + ":" + PASSWORD;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Basic " + encodedCredentials);
        when(request.getServletPath()).thenReturn("/do/something");

        User user = createActiveUser();
        when(authenticationProviderManager.getUser(USERNAME, PASSWORD)).thenReturn(user);

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webApplicationContext);

            basicAuthFilter.attemptAuthentication(request, response);

            verify(session).setAttribute(eq("user"), eq(user));
            verify(session).setAttribute(eq(SystemConstants.SESSIONPARAM_CURRENT_USER), eq(user));
        }
    }

    @Test
    void attemptAuthentication_authenticationProviderThrowsException_shouldThrowBadCredentialsException() throws Exception {
        String credentials = USERNAME + ":" + PASSWORD;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Basic " + encodedCredentials);

        when(authenticationProviderManager.getUser(USERNAME, PASSWORD)).thenThrow(new EntException("DB error"));

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webApplicationContext);

            BadCredentialsException exception = assertThrows(BadCredentialsException.class,
                    () -> basicAuthFilter.attemptAuthentication(request, response));

            assertEquals("Authentication error", exception.getMessage());
        }
    }

    @Test
    void onAuthenticationFailure_shouldReturnUnauthorizedResponse() throws Exception {
        when(response.getOutputStream()).thenReturn(outputStream);

        BadCredentialsException exception = new BadCredentialsException("Test error message");

        basicAuthFilter.onAuthenticationFailure(request, response, exception);

        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response).addHeader("Content-Type", "application/json");
        verify(outputStream).println(anyString());
    }

    @Test
    void successfulAuthentication_shouldContinueFilterChain() throws Exception {
        Authentication auth = mock(Authentication.class);

        basicAuthFilter.successfulAuthentication(request, response, filterChain, auth);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void attemptAuthentication_passwordWithColons_shouldParseCorrectly() throws Exception {
        String passwordWithColons = "pass:word:with:colons";
        String credentials = USERNAME + ":" + passwordWithColons;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn("Basic " + encodedCredentials);
        when(request.getServletPath()).thenReturn("/api");

        User user = createActiveUser();
        when(authenticationProviderManager.getUser(USERNAME, passwordWithColons)).thenReturn(user);

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(servletContext)).thenReturn(webApplicationContext);

            Authentication auth = basicAuthFilter.attemptAuthentication(request, response);

            assertNotNull(auth);
            assertInstanceOf(UserAuthentication.class, auth);
            verify(authenticationProviderManager).getUser(USERNAME, passwordWithColons);
        }
    }

    // Helper methods to create test users

    private User createGuestUser() {
        User user = new User();
        user.setUsername("guest");
        return user;
    }

    private User createActiveUser() {
        User user = new User();
        user.setUsername(USERNAME);
        user.setDisabled(false);
        return user;
    }

    private User createDisabledUser() {
        User user = new User();
        user.setUsername(USERNAME);
        user.setDisabled(true);
        return user;
    }

}