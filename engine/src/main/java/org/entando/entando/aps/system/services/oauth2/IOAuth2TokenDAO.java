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
package org.entando.entando.aps.system.services.oauth2;

import java.util.List;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

public interface IOAuth2TokenDAO {

    public void storeAccessToken(OAuth2AccessToken accessToken, OAuth2Authorization authentication);

    public List<OAuth2AccessToken> findTokensByClientIdAndUserName(String clientId, String username);

    public List<OAuth2AccessToken> findTokensByClientId(String clientId);
    
    public List<OAuth2AccessToken> findTokensByUserName(String username);

    public OAuth2AccessToken readAccessToken(final String accessToken);

    public void removeAccessToken(final String accessToken);
    
    public void deleteExpiredToken(int expirationTime);

    public OAuth2RefreshToken readRefreshToken(String tokenValue);
    
    public OAuth2Authorization readAuthenticationForRefreshToken(OAuth2RefreshToken refreshToken);

    public void removeAccessTokenUsingRefreshToken(final String refreshToken);

    // New methods for Spring Authorization Server support
    public void storeAuthorization(OAuth2Authorization authorization);

    public OAuth2Authorization findAuthorizationById(String id);

    public OAuth2Authorization findAuthorizationByToken(String token, String tokenType);

    public void removeAuthorization(String id);

}
