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
package org.entando.entando.aps.system.services.oauth2.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.Collections;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

/**
 * @author E.Santoboni
 */
public class OAuth2AccessTokenImpl extends OAuth2AccessToken implements Serializable {

    private String clientId;
    private String grantType;
    private String localUser;
    private OAuth2RefreshToken refreshToken;

    public OAuth2AccessTokenImpl(String tokenValue) {
        super(OAuth2AccessToken.TokenType.BEARER, tokenValue,
              Instant.now(), null, Collections.emptySet());
    }

    public OAuth2AccessTokenImpl(OAuth2AccessToken.TokenType tokenType, String tokenValue,
                                Instant issuedAt, Instant expiresAt, Set<String> scopes) {
        super(tokenType != null ? tokenType : OAuth2AccessToken.TokenType.BEARER,
              tokenValue, issuedAt != null ? issuedAt : Instant.now(),
              expiresAt, scopes != null ? scopes : Collections.emptySet());
    }

    // Constructor with all Entando-specific parameters
    public OAuth2AccessTokenImpl(String tokenValue, Instant expiresAt,
                                String clientId, String grantType, String localUser,
                                OAuth2RefreshToken refreshToken) {
        super(OAuth2AccessToken.TokenType.BEARER, tokenValue,
              Instant.now(), expiresAt, Collections.emptySet());
        this.clientId = clientId;
        this.grantType = grantType;
        this.localUser = localUser;
        this.refreshToken = refreshToken;
    }

    public String getClientId() {
        return clientId;
    }

    public String getGrantType() {
        return grantType;
    }

    public String getLocalUser() {
        return localUser;
    }

    public OAuth2RefreshToken getRefreshToken() {
        return refreshToken;
    }

    // Deprecated: Use constructor with all parameters instead
    @Deprecated
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    // Deprecated: Use constructor with all parameters instead
    @Deprecated
    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    // Deprecated: Use constructor with all parameters instead
    @Deprecated
    public void setLocalUser(String localUser) {
        this.localUser = localUser;
    }

    // Deprecated: Use constructor with all parameters instead
    @Deprecated
    public void setRefreshToken(OAuth2RefreshToken refreshToken) {
        this.refreshToken = refreshToken;
    }

    // Deprecated: In Spring Security 6.x, expiration is set in constructor
    @Deprecated
    public void setExpiration(Date expiration) {
        // No-op: expiration is now immutable and set in constructor
    }

    // Helper method to create OAuth2RefreshToken with proper parameters
    public static OAuth2RefreshToken createRefreshToken(String tokenValue, Instant expiresAt) {
        return new OAuth2RefreshToken(tokenValue, Instant.now(), expiresAt);
    }
}
