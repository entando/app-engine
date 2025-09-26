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

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.codec.digest.DigestUtils;
import org.entando.entando.aps.system.services.oauth2.model.OAuth2AccessTokenImpl;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

public class ApiOAuth2TokenManager extends AbstractOAuthManager implements IApiOAuth2TokenManager {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(ApiOAuth2TokenManager.class);
    public static final int ACCESS_TOKEN_LEN = 32;
    private transient ScheduledExecutorService scheduler = null;

    private IOAuth2TokenDAO oAuth2TokenDAO;

    @Override
    public void init() throws Exception {
        ScheduledDeleteExpiredTokenThread sdett
                = new ScheduledDeleteExpiredTokenThread(this.getOAuth2TokenDAO(), this.getRefreshTokenValiditySeconds());
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.scheduler.scheduleAtFixedRate(sdett, 0, 1, TimeUnit.HOURS);
        logger.debug("{}  initialized ", this.getClass().getName());
    }

    @Override
    protected void release() {
        this.scheduler.shutdown();
    }

    @Override
    public void destroy() {
        this.scheduler.shutdown();
    }

//    @Override
    public OAuth2Authorization readAuthentication(OAuth2AccessToken token) {
        logger.warn("readAuthentication Not supported yet.");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

//    @Override
    public OAuth2Authorization readAuthentication(String tokenValue) {
        logger.warn("readAuthentication Not supported yet.");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void storeAccessToken(OAuth2AccessToken accessToken, OAuth2Authorization authentication) {
        this.getOAuth2TokenDAO().storeAccessToken(accessToken, authentication);
    }

    @Override
    public OAuth2AccessToken readAccessToken(String tokenValue) {
        return this.getOAuth2TokenDAO().readAccessToken(tokenValue);
    }

    @Override
    public void removeAccessToken(OAuth2AccessToken token) {
        this.getOAuth2TokenDAO().removeAccessToken(token.getTokenValue());
    }

    @Override
    public void removeAccessToken(String tokenValue) {
        this.getOAuth2TokenDAO().removeAccessToken(tokenValue);
    }

    @Override
    public void storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authorization authentication) {
        logger.info("storeRefreshToken - nothing to do");
    }

    @Override
    public OAuth2RefreshToken readRefreshToken(String tokenValue) {
        return this.getOAuth2TokenDAO().readRefreshToken(tokenValue);
    }

//    @Override
    public OAuth2Authorization readAuthenticationForRefreshToken(OAuth2RefreshToken refreshToken) {
        return this.getOAuth2TokenDAO().readAuthenticationForRefreshToken(refreshToken);
    }

    @Override
    public void removeRefreshToken(OAuth2RefreshToken refreshToken) {
        this.getOAuth2TokenDAO().removeAccessTokenUsingRefreshToken(refreshToken.getTokenValue());
    }

//    @Override
    public void removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
        this.getOAuth2TokenDAO().removeAccessTokenUsingRefreshToken(refreshToken.getTokenValue());
    }

    @Override
    public OAuth2AccessToken createAccessTokenForLocalUser(String username) {
        OAuth2AccessToken token = this.getAccessToken(username, "LOCAL_USER", "implicit");
        this.getOAuth2TokenDAO().storeAccessToken(token, null);
        return token;
    }

//    @Override
    public OAuth2AccessToken getAccessToken(OAuth2Authorization authorization) {
        String principal = authorization.getPrincipalName();
        String clientId = authorization.getRegisteredClientId();
        String grantType = authorization.getAuthorizationGrantType().getValue();
        return this.getAccessToken(principal, clientId, grantType);
    }

    protected OAuth2AccessToken getAccessToken(String principal, String clientId, String grantType) {
        byte[] array1 = new byte[8];
        new SecureRandom().nextBytes(array1);
        String tokenPrefix1 = principal + new String(Hex.encode(array1));
        final String accessToken = DigestUtils.sha256Hex(tokenPrefix1 + "_accessToken").substring(0, ACCESS_TOKEN_LEN);

        byte[] array2 = new byte[8];
        new SecureRandom().nextBytes(array2);
        String tokenPrefix2 = principal + Arrays.toString(Hex.encode(array2));
        final String refreshTokenValue = DigestUtils.sha256Hex(tokenPrefix2 + "_refreshToken").substring(0, ACCESS_TOKEN_LEN);

        // Calculate expiration times using Instant
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, this.getAccessTokenValiditySeconds());
        final Instant accessTokenExpiresAt = calendar.toInstant();

        // Create refresh token with expiration (typically longer than access token)
        Calendar refreshCalendar = Calendar.getInstance();
        refreshCalendar.add(Calendar.SECOND, this.getRefreshTokenValiditySeconds());
        final Instant refreshTokenExpiresAt = refreshCalendar.toInstant();

        // Create refresh token using the helper method
        final OAuth2RefreshToken refreshToken = OAuth2AccessTokenImpl.createRefreshToken(
                refreshTokenValue, refreshTokenExpiresAt);

        // Create OAuth2AccessToken with all required parameters in constructor
        return new OAuth2AccessTokenImpl(
                accessToken,
                accessTokenExpiresAt,
                clientId,
                grantType,
                principal,
                refreshToken
        );
    }

    @Override
    public Collection<OAuth2AccessToken> findTokensByClientIdAndUserName(String clientId, String username) {
        return this.getOAuth2TokenDAO().findTokensByClientIdAndUserName(clientId, username);
    }

    @Override
    public Collection<OAuth2AccessToken> findTokensByUserName(String username) {
        return this.getOAuth2TokenDAO().findTokensByUserName(username);
    }

    @Override
    public Collection<OAuth2AccessToken> findTokensByClientId(String clientId) {
        return this.getOAuth2TokenDAO().findTokensByClientId(clientId);
    }

    protected IOAuth2TokenDAO getOAuth2TokenDAO() {
        return oAuth2TokenDAO;
    }

    public void setOAuth2TokenDAO(IOAuth2TokenDAO oAuth2TokenDAO) {
        this.oAuth2TokenDAO = oAuth2TokenDAO;
    }

    //TODO da rimuovere quasi sicuramente
    // OAuth2AuthorizationService implementation
    // These methods bridge between Entando's token management and Spring Security 6.x OAuth2AuthorizationService

    @Override
    public void save(OAuth2Authorization authorization) {
        // Extract access token from authorization and store using DAO
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken != null) {
            this.getOAuth2TokenDAO().storeAccessToken(accessToken.getToken(), authorization);
        }

        // Store refresh token if present
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        if (refreshToken != null) {
            this.storeRefreshToken(refreshToken.getToken(), authorization);
        }
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        // Remove access token
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken != null) {
            this.removeAccessToken(accessToken.getToken());
        }

        // Remove refresh token
        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        if (refreshToken != null) {
            this.removeRefreshToken(refreshToken.getToken());
        }
    }

    @Override
    public OAuth2Authorization findById(String id) {
        // This method is required by OAuth2AuthorizationService but not directly used by Entando
        // Could be implemented if needed for full authorization server functionality
        logger.warn("findById not implemented - authorization server functionality not fully supported");
        return null;
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        // This method is required by OAuth2AuthorizationService
        // Implementation would need to reverse-lookup from token to authorization
        if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
            OAuth2AccessToken accessToken = this.readAccessToken(token);
            if (accessToken != null) {
                // Would need to reconstruct OAuth2Authorization from token data
                // For now, return null as this is complex to implement without more context
                logger.warn("findByToken not fully implemented - would need authorization reconstruction logic");
            }
        } else if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
            OAuth2RefreshToken refreshToken = this.readRefreshToken(token);
            if (refreshToken != null) {
                // Similar issue - would need to reconstruct authorization
                logger.warn("findByToken for refresh token not fully implemented");
            }
        }
        return null;
    }
}
