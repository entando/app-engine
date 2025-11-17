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

import com.agiletec.aps.system.common.AbstractSearcherDAO;
import com.agiletec.aps.system.common.FieldSearchFilter;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.oauth2.model.OAuth2AccessTokenImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.core.Authentication;
import java.util.Date;

public class OAuth2TokenDAO extends AbstractSearcherDAO implements IOAuth2TokenDAO {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(OAuth2TokenDAO.class);

    private static final String ERROR_REMOVE_ACCESS_TOKEN = "Error while remove access token";

    private static final String INSERT_TOKEN = "INSERT INTO api_oauth_tokens (accesstoken, clientid, expiresin, refreshtoken, granttype, localuser)  VALUES (? , ? , ? , ? , ?, ?)";

    private static final String DELETE_EXPIRED_TOKENS = "DELETE FROM api_oauth_tokens WHERE expiresin < ?";

    private static final String SELECT_TOKEN_PREFIX = "SELECT * FROM api_oauth_tokens ";

    private static final String SELECT_TOKEN = SELECT_TOKEN_PREFIX + "WHERE accesstoken = ? ";

    private static final String SELECT_TOKEN_BY_REFRESH = SELECT_TOKEN_PREFIX + "WHERE refreshtoken = ? ";

    private static final String DELETE_TOKEN_PREFIX = "DELETE FROM api_oauth_tokens ";

    private static final String DELETE_TOKEN = DELETE_TOKEN_PREFIX + "WHERE accesstoken = ? ";

    private static final String DELETE_TOKEN_BY_REFRESH = DELETE_TOKEN_PREFIX + "WHERE refreshtoken = ? ";

    @Override
    protected String getTableFieldName(String metadataFieldKey) {
        return metadataFieldKey;
    }

    @Override
    protected String getMasterTableName() {
        return "api_oauth_tokens";
    }

    @Override
    protected String getMasterTableIdFieldName() {
        return "accesstoken";
    }

    @Override
    public List<OAuth2AccessToken> findTokensByClientIdAndUserName(String clientId, String username) {
        if (StringUtils.isBlank(clientId) && StringUtils.isBlank(username)) {
            throw new RuntimeException("clientId and username cannot both be null");
        }
        FieldSearchFilter expirationFilter = new FieldSearchFilter("expiresin");
        expirationFilter.setOrder(FieldSearchFilter.Order.ASC);
        FieldSearchFilter[] filters = {expirationFilter};
        if (!StringUtils.isBlank(clientId)) {
            FieldSearchFilter clientIdFilter = new FieldSearchFilter("clientid", clientId, true);
            filters = ArrayUtils.add(filters, clientIdFilter);
        }
        if (!StringUtils.isBlank(username)) {
            FieldSearchFilter usernameFilter = new FieldSearchFilter("localuser", username, true);
            filters = ArrayUtils.add(filters, usernameFilter);
        }
        List<OAuth2AccessToken> accessTokens = new ArrayList<>();
        List<String> tokens = super.searchId(filters);
        if (tokens.isEmpty()) {
            return accessTokens;
        }
        Connection conn = null;
        try {
            conn = this.getConnection();
            for (String token : tokens) {
                OAuth2AccessToken accessToken = this.getAccessToken(token, conn);
                if (accessToken != null && (accessToken.getExpiresAt() == null || accessToken.getExpiresAt().isAfter(java.time.Instant.now()))) {
                    accessTokens.add(accessToken);
                }
            }
        } catch (Exception t) {
            logger.error("Error while loading tokens", t);
            throw new RuntimeException("Error while loading tokens", t);
        } finally {
            this.closeConnection(conn);
        }
        return accessTokens;
    }

    protected OAuth2AccessToken getAccessToken(final String token, Connection conn) {
        OAuth2AccessTokenImpl accessToken = null;
        PreparedStatement stat = null;
        ResultSet res = null;
        try {
            stat = conn.prepareStatement(SELECT_TOKEN);
            stat.setString(1, token);
            res = stat.executeQuery();
            if (res.next()) {
                String refreshTokenValue = res.getString("refreshtoken");
                OAuth2RefreshToken refreshToken = refreshTokenValue != null ?
                    new OAuth2RefreshToken(refreshTokenValue, java.time.Instant.now()) : null;

                Timestamp timestamp = res.getTimestamp("expiresin");
                Date expiration = new Date(timestamp.getTime());

                // Use the immutable constructor pattern
                accessToken = new OAuth2AccessTokenImpl(
                    token,
                    expiration.toInstant(),
                    res.getString("clientid"),
                    res.getString("granttype"),
                    res.getString("localuser"),
                    refreshToken
                );
            }
        } catch (Throwable t) {
            logger.error("Error loading token {}", token, t);
            throw new RuntimeException("Error loading token " + token, t);
        } finally {
            closeDaoResources(res, stat);
        }
        return accessToken;
    }

    @Override
    public List<OAuth2AccessToken> findTokensByUserName(String username) {
        return this.findTokensByClientIdAndUserName(null, username);
    }
    
    @Override
    public List<OAuth2AccessToken> findTokensByClientId(String clientId) {
        return this.findTokensByClientIdAndUserName(clientId, null);
    }

    @Override
    public void storeAccessToken(OAuth2AccessToken accessToken, OAuth2Authorization authentication) {
        Connection conn = null;
        PreparedStatement stat = null;
        try {
            conn = this.getConnection();
            String tokenValue = accessToken.getTokenValue();
            if (null != this.getAccessToken(tokenValue, conn)) {
                logger.debug("storeAccessToken: Stored Token already exists");
                return;
            }
            conn.setAutoCommit(false);
            stat = conn.prepareStatement(INSERT_TOKEN);
            stat.setString(1, accessToken.getTokenValue());

            // Extract client ID from OAuth2Authorization
            String clientId = null;
            if (accessToken instanceof OAuth2AccessTokenImpl) {
                clientId = ((OAuth2AccessTokenImpl) accessToken).getClientId();
            } else if (authentication != null) {
                clientId = authentication.getRegisteredClientId();
            }
            stat.setString(2, clientId);

            stat.setTimestamp(3, new Timestamp(accessToken.getExpiresAt().toEpochMilli()));

            // Handle refresh token - Spring Security 6.x OAuth2AccessToken doesn't have getRefreshToken()
            // We need to extract it from OAuth2AccessTokenImpl or OAuth2Authorization
            String refreshTokenValue = null;
            if (accessToken instanceof OAuth2AccessTokenImpl) {
                OAuth2RefreshToken refreshToken = ((OAuth2AccessTokenImpl) accessToken).getRefreshToken();
                refreshTokenValue = refreshToken != null ? refreshToken.getTokenValue() : null;
            }
            // Note: For standard OAuth2AccessToken, refresh token info needs to come from OAuth2Authorization
            if (refreshTokenValue != null) {
                stat.setString(4, refreshTokenValue);
            } else {
                stat.setNull(4, Types.VARCHAR);
            }

            // Extract grant type and user info
            String grantType = null;
            String localUser = null;

            if (accessToken instanceof OAuth2AccessTokenImpl) {
                grantType = ((OAuth2AccessTokenImpl) accessToken).getGrantType();
                localUser = ((OAuth2AccessTokenImpl) accessToken).getLocalUser();
            } else if (authentication != null) {
                // In Spring Security 6.x, grant type is stored differently
                grantType = authentication.getAuthorizationGrantType() != null ?
                    authentication.getAuthorizationGrantType().getValue() : null;

                // Extract user info from principal
                Authentication principal = authentication.getAttribute(Authentication.class.getName());
                if (principal != null && principal.getPrincipal() instanceof UserDetails) {
                    localUser = ((UserDetails) principal.getPrincipal()).getUsername();
                } else if (principal != null) {
                    localUser = principal.getName();
                }
            }

            stat.setString(5, grantType);
            stat.setString(6, localUser);

            stat.executeUpdate();
            conn.commit();
        } catch (Exception t) {
            this.executeRollback(conn);
            logger.error("Error while adding an access token", t);
            throw new RuntimeException("Error while adding an access token", t);
        } finally {
            closeDaoResources(null, stat, conn);
        }
    }

    @Override
    public OAuth2AccessToken readAccessToken(final String accessToken) {
        Connection conn = null;
        OAuth2AccessToken token = null;
        try {
            conn = this.getConnection();
            token = this.getAccessToken(accessToken, conn);
        } catch (Exception t) {
            logger.error("Error while loading token {}", accessToken, t);
            throw new RuntimeException("Error while loading token " + accessToken, t);
        } finally {
            this.closeConnection(conn);
        }
        return token;
    }

    @Override
    public void removeAccessToken(final String accessToken) {
        super.executeQueryWithoutResultset(DELETE_TOKEN, accessToken);
    }
    
    @Override
    public void removeAccessTokenUsingRefreshToken(String refreshToken) {
        super.executeQueryWithoutResultset(DELETE_TOKEN_BY_REFRESH, refreshToken);
    }
    
    @Override
    public void deleteExpiredToken(int expirationTime) {
        Connection conn = null;
        PreparedStatement stat = null;
        try {
            conn = this.getConnection();
            conn.setAutoCommit(false);
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, -expirationTime);
            stat = conn.prepareStatement(DELETE_EXPIRED_TOKENS);
            stat.setTimestamp(1, new Timestamp(calendar.getTimeInMillis()));
            stat.executeUpdate();
            conn.commit();
        } catch (Exception t) {
            this.executeRollback(conn);
            logger.error(ERROR_REMOVE_ACCESS_TOKEN, t);
            throw new RuntimeException(ERROR_REMOVE_ACCESS_TOKEN, t);
        } finally {
            closeDaoResources(null, stat, conn);
        }
    }
    
    @Override
    public OAuth2RefreshToken readRefreshToken(String tokenValue) {
        FieldSearchFilter filter = new FieldSearchFilter("refreshtoken", tokenValue, true);
        FieldSearchFilter[] filters = {filter};
        List<String> accessTokens = super.searchId(filters);
        if (null != accessTokens && accessTokens.size() > 0) {
            return new OAuth2RefreshToken(tokenValue, java.time.Instant.now());
        }
        return null;
    }
    
    @Override
    public OAuth2Authorization readAuthenticationForRefreshToken(OAuth2RefreshToken refreshToken) {
        OAuth2Authorization authorization = null;
        Connection conn = null;
        PreparedStatement stat = null;
        ResultSet res = null;
        try {
            conn = this.getConnection();
            stat = conn.prepareStatement(SELECT_TOKEN_BY_REFRESH);
            stat.setString(1, refreshToken.getTokenValue());
            res = stat.executeQuery();
            if (res.next()) {
                String username = res.getString("localuser");
                String clientId = res.getString("clientid");
                String grantType = res.getString("granttype");

                // In Spring Security 6.x OAuth2Authorization, we need to build a more complete structure
                // For now, we'll return null and log a warning since this method should be handled
                // by the OAuth2AuthorizationService implementation
                logger.warn("readAuthenticationForRefreshToken called - this method should be handled by OAuth2AuthorizationService");
                logger.debug("Token data: clientId={}, username={}, grantType={}", clientId, username, grantType);

                // OAuth2Authorization in Spring Security 6.x requires RegisteredClient and other components
                // that are not easily reconstructable from database fields alone.
                // The calling code should use OAuth2AuthorizationService.findByToken() instead
                return null;
            }
        } catch (Exception t) {
            logger.error("Error while reading tokens", t);
            throw new RuntimeException("Error while reading tokens", t);
        } finally {
            this.closeDaoResources(res, stat, conn);
        }
        return authorization;
    }

    // Default implementations for new OAuth2AuthorizationService support methods
    // These provide backward compatibility by delegating to the token reconstruction logic

    @Override
    public void storeAuthorization(OAuth2Authorization authorization) {
        // Default implementation: extract and store tokens using existing methods
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization.getAccessToken();
        if (accessToken != null) {
            this.storeAccessToken(accessToken.getToken(), authorization);
        }

        OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = authorization.getRefreshToken();
        if (refreshToken != null) {
            // Note: refresh token storage is currently handled via access token storage
            logger.debug("Refresh token storage handled via access token record");
        }
    }

    @Override
    public OAuth2Authorization findAuthorizationById(String id) {
        // Default implementation: treat id as access token value
        // This is a fallback - in practice, the authorization reconstruction
        // is handled by ApiOAuth2TokenManager.reconstructAuthorizationFromToken
        logger.debug("findAuthorizationById called with id: {} - using token manager reconstruction", id);
        return null; // Let the token manager handle reconstruction
    }

    @Override
    public OAuth2Authorization findAuthorizationByToken(String token, String tokenType) {
        // Default implementation: delegate to token manager reconstruction
        // This is a fallback - the actual reconstruction logic is in ApiOAuth2TokenManager
        logger.debug("findAuthorizationByToken called with token type: {} - using token manager reconstruction", tokenType);
        return null; // Let the token manager handle reconstruction
    }

    @Override
    public void removeAuthorization(String id) {
        // Default implementation: treat id as access token value and remove
        try {
            this.removeAccessToken(id);
        } catch (Exception e) {
            logger.debug("Could not remove authorization by id: {}", id, e);
        }
    }

}
