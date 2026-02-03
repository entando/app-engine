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

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.aps.util.ApsTenantApplicationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging;
import org.entando.entando.web.common.model.RestError;
import org.entando.entando.web.common.model.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.entando.entando.aps.servlet.security.KeycloakSecurityConfig.API_PATH;

/**
 * Authentication filter that validates Basic Auth credentials against the database.
 * Used when Keycloak is disabled (keycloak.enabled=false).
 */
public class BasicAuthFilter extends AbstractAuthenticationProcessingFilter implements AuthenticationFailureHandler {

    private static final EntLogging.EntLogger log = EntLogging.EntLogFactory.getSanitizedLogger(BasicAuthFilter.class);

    private final ObjectMapper objectMapper;
    private final IUserManager userManager;
    private final IAuthenticationProviderManager authenticationProviderManager;

    public BasicAuthFilter(final IUserManager userManager,
                           final IAuthenticationProviderManager authenticationProviderManager) {
        super("/api/**");
        this.objectMapper = new ObjectMapper();
        this.userManager = userManager;
        this.authenticationProviderManager = authenticationProviderManager;
        this.setAuthenticationManager(authenticationProviderManager);
    }

    @Override
    public Authentication attemptAuthentication(final HttpServletRequest request,
                                                 final HttpServletResponse response) throws AuthenticationException {
        ApsTenantApplicationUtils.extractCurrentTenantCode(request)
                .filter(StringUtils::isNotBlank)
                .ifPresentOrElse(ApsTenantApplicationUtils::setTenant, ApsTenantApplicationUtils::removeTenant);
        log.warn("Keycloak disabled, handling Basic Auth");
        final String authorization = request.getHeader("Authorization");

        // No Authorization header - return guest
        if (authorization == null || authorization.isBlank()) {
            return createGuestAuthentication(request);
        }

        // Handle Basic Auth
        if (authorization.toLowerCase().startsWith("basic ")) {
            return handleBasicAuth(request, authorization);
        }

        // Handle Bearer token (validate against DB-stored tokens)
        if (authorization.toLowerCase().startsWith("bearer ")) {
            return handleBearerToken(request, authorization);
        }

        // Unknown auth scheme - return guest
        return createGuestAuthentication(request);
    }

    private Authentication handleBasicAuth(HttpServletRequest request, String authorization) {
        try {
            String base64Credentials = authorization.substring("Basic ".length()).trim();
            byte[] decodedBytes = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(decodedBytes, StandardCharsets.UTF_8);

            int colonIndex = credentials.indexOf(':');
            if (colonIndex == -1) {
                throw new BadCredentialsException("Invalid Basic Auth format");
            }

            String username = credentials.substring(0, colonIndex);
            String password = credentials.substring(colonIndex + 1);

            if (username.isBlank() || password.isBlank()) {
                throw new BadCredentialsException("Username and password are required");
            }

            // Validate credentials against the database
            UserDetails user = authenticationProviderManager.getUser(username, password);

            if (user == null) {
                throw new BadCredentialsException("Invalid username or password");
            }

            if (user.isDisabled()) {
                throw new BadCredentialsException("User account is disabled");
            }

            if (!user.isAccountNotExpired()) {
                throw new BadCredentialsException("User account has expired");
            }

            log.warn("Successfully authenticated user '{}' via Basic Auth", username);

            UserAuthentication userAuthentication = new UserAuthentication(user);
            setUserOnContext(request, user, userAuthentication);
            return userAuthentication;

        } catch (EntException e) {
            log.error("Error during Basic Auth authentication", e);
            throw new BadCredentialsException("Authentication error", e);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Base64 in Basic Auth header");
            throw new BadCredentialsException("Invalid Basic Auth encoding");
        }
    }

    private Authentication handleBearerToken(HttpServletRequest request, String authorization) {
        try {
            String token = authorization.substring("Bearer ".length()).trim();

            // Try to find user by access token in the database
            UserDetails user = findUserByAccessToken(token);

            if (user == null) {
                log.debug("No valid user found for Bearer token");
                throw new BadCredentialsException("Invalid or expired token");
            }

            log.debug("Successfully authenticated user '{}' via Bearer token", user.getUsername());

            UserAuthentication userAuthentication = new UserAuthentication(user);
            setUserOnContext(request, user, userAuthentication);
            return userAuthentication;

        } catch (EntException e) {
            log.error("Error during Bearer token authentication", e);
            throw new BadCredentialsException("Authentication error", e);
        }
    }

    private UserDetails findUserByAccessToken(String token) throws EntException {
        // The token was generated by AuthenticationProviderManager.extractUser()
        // which stores it in the OAuth2 token manager
        // We need to look up the user by their stored access token
        // For now, we'll try to get the user from the token manager
        // This requires the token to be valid and stored in the database

        // Note: This is a simplified implementation. In a production scenario,
        // you might want to validate the token through the OAuth2 token manager
        // and extract the username from there.
        return null; // Bearer tokens require Keycloak validation - return null to reject
    }

    private Authentication createGuestAuthentication(HttpServletRequest request) {
        UserDetails guestUser = userManager.getGuestUser();
        GuestAuthentication guestAuthentication = new GuestAuthentication(guestUser);
        setUserOnContext(request, guestUser, guestAuthentication);
        return guestAuthentication;
    }

    private void setUserOnContext(HttpServletRequest request, UserDetails user, Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        if (API_PATH.equals(request.getServletPath())) {
            request.setAttribute("user", user);
        } else {
            request.getSession().setAttribute("user", user);
            request.getSession().setAttribute(SystemConstants.SESSIONPARAM_CURRENT_USER, user);
        }
    }

    @Override
    protected void successfulAuthentication(final HttpServletRequest request,
                                            final HttpServletResponse response,
                                            final FilterChain chain,
                                            final Authentication authResult) throws IOException, ServletException {
        chain.doFilter(request, response);
    }

    @Override
    protected void unsuccessfulAuthentication(final HttpServletRequest request,
                                              final HttpServletResponse response,
                                              final AuthenticationException failed) throws IOException {
        this.onAuthenticationFailure(request, response, failed);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        final RestResponse<Void, Void> restResponse = new RestResponse<>(null, null);
        restResponse.addError(new RestError(HttpStatus.UNAUTHORIZED.toString(), exception.getMessage()));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.addHeader("Content-Type", "application/json");
        response.getOutputStream().println(objectMapper.writeValueAsString(restResponse));
    }
}