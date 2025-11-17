/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.web.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;

/**
 * @author E.Santoboni
 *
 * Entando Bearer Token Extractor for Spring Security 6.x
 * Migrated from deprecated BearerTokenExtractor to use BearerTokenResolver
 */
public class EntandoBearerTokenExtractor {

    private final BearerTokenResolver bearerTokenResolver;

    public EntandoBearerTokenExtractor() {
        this.bearerTokenResolver = new DefaultBearerTokenResolver();
    }

    public EntandoBearerTokenExtractor(BearerTokenResolver bearerTokenResolver) {
        this.bearerTokenResolver = bearerTokenResolver;
    }

    /**
     * Extract bearer token from HTTP request
     *
     * @param request the HTTP servlet request
     * @return the bearer token string, or null if not found
     */
    public String extractToken(HttpServletRequest request) {
        return this.bearerTokenResolver.resolve(request);
    }

    /**
     * Extract Authentication from HTTP request (compatibility method)
     * Creates a BearerTokenAuthenticationToken from the extracted token
     *
     * @param request the HTTP servlet request
     * @return Authentication object, or null if no token found
     */
    public Authentication extract(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            return new BearerTokenAuthenticationToken(token);
        }
        return null;
    }

}
