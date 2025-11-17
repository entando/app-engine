/*
 * Copyright 2018-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.system.services.oauth2;

import java.util.Collection;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;

/**
 * Entando-specific OAuth2 Token Manager interface.
 *
 * Extends OAuth2AuthorizationService to provide Spring Security 6.x compatibility
 * while maintaining Entando's custom token management requirements.
 *
 * Migration Note: This replaces the previous TokenStore-based approach
 * with Spring Security 6.x OAuth2AuthorizationService pattern.
 */
public interface IApiOAuth2TokenManager extends OAuth2AuthorizationService {

    // Entando-specific token management methods
    Collection<OAuth2AccessToken> findTokensByUserName(String username);

    OAuth2AccessToken createAccessTokenForLocalUser(String username);

    // Additional methods to bridge TokenStore functionality
    OAuth2AccessToken readAccessToken(String tokenValue);

    void storeAccessToken(OAuth2AccessToken token, OAuth2Authorization authorization);

    void removeAccessToken(OAuth2AccessToken token);

    void removeAccessToken(String tokenValue);

    OAuth2RefreshToken readRefreshToken(String tokenValue);

    void storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authorization authorization);

    void removeRefreshToken(OAuth2RefreshToken token);

    Collection<OAuth2AccessToken> findTokensByClientIdAndUserName(String clientId, String userName);

    Collection<OAuth2AccessToken> findTokensByClientId(String clientId);

}
