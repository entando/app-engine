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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import io.swagger.models.auth.In;
import org.entando.entando.aps.system.services.oauth2.model.OAuth2AccessTokenImpl;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * @author E.Santoboni
 */
@ExtendWith(MockitoExtension.class)
class ApiOAuth2TokenManagerTest {

    @Mock
    private OAuth2TokenDAO tokenDAO;

    @InjectMocks
    private ApiOAuth2TokenManager tokenManager;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void findTokensByUserName() {
        Mockito.lenient().when(tokenDAO.findTokensByClientIdAndUserName(Mockito.anyString(), Mockito.anyString())).thenReturn(new ArrayList<>());
        Collection<OAuth2AccessToken> tokens = tokenManager.findTokensByUserName("username");
        Assertions.assertNotNull(tokens);
    }

    @Test
    void findTokensByClientIdAndUserName() {
        when(tokenDAO.findTokensByClientIdAndUserName(Mockito.anyString(), Mockito.anyString())).thenReturn(new ArrayList<>());
        Collection<OAuth2AccessToken> tokens = tokenManager.findTokensByClientIdAndUserName("clientId", "username");
        Assertions.assertNotNull(tokens);
    }

    @Test
    void findTokensByClientId() {
        Mockito.lenient().when(tokenDAO.findTokensByClientIdAndUserName(Mockito.anyString(), Mockito.anyString())).thenReturn(new ArrayList<>());
        Collection<OAuth2AccessToken> tokens = tokenManager.findTokensByClientId("clientId");
        Assertions.assertNotNull(tokens);
    }

    @Test
    void createAccessTokenForLocalUser() {
        OAuth2AccessToken token = this.tokenManager.createAccessTokenForLocalUser("username");
        Assertions.assertNotNull(token);
        Mockito.verify(tokenDAO, Mockito.times(1)).storeAccessToken(Mockito.any(OAuth2AccessToken.class), Mockito.eq(null));
        Assertions.assertTrue(token instanceof OAuth2AccessTokenImpl);
        Assertions.assertEquals("LOCAL_USER", ((OAuth2AccessTokenImpl) token).getClientId());
    }

    @Test
    void readUnsupportedAuthenticationByAccessTokenObject() throws Exception {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            this.tokenManager.readAuthentication(this.createMockAccessToken());
        });
    }

    @Test
    void readUnsupportedAuthenticationByAccessToken() throws Exception {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            this.tokenManager.readAuthentication("token");
        });
    }

    @Test
    void storeAccessToken() {
        this.tokenManager.storeAccessToken(this.createMockAccessToken(), this.createMockAuthentication());
        Mockito.verify(tokenDAO, Mockito.times(1)).storeAccessToken(Mockito.any(OAuth2AccessToken.class), Mockito.any(OAuth2Authorization.class));
    }

    @Test
    void readAccessToken() throws Exception {
        when(tokenDAO.readAccessToken(Mockito.anyString())).thenReturn(new OAuth2AccessTokenImpl("token"));
        OAuth2AccessToken token = tokenManager.readAccessToken("token");
        Assertions.assertNotNull(token);
        Assertions.assertTrue(token instanceof OAuth2AccessTokenImpl);
        Assertions.assertEquals("token", token.getTokenValue());
    }

    @Test
    void removeAccessToken() throws Exception {
        this.tokenManager.removeAccessToken(this.createMockAccessToken());
        Mockito.verify(tokenDAO, Mockito.times(1)).removeAccessToken(Mockito.anyString());
    }

    @Test
    void storeRefreshToken() throws Exception {
        this.tokenManager.storeRefreshToken(new OAuth2RefreshToken("value", Instant.now()), this.createMockAuthentication());
        Mockito.verifyZeroInteractions(tokenDAO);
    }

    @Test
    void readRefreshToken() throws Exception {
        when(tokenDAO.readRefreshToken(Mockito.anyString())).thenReturn(Mockito.any(OAuth2RefreshToken.class));
        OAuth2RefreshToken refreshToken = this.tokenManager.readRefreshToken("refresh_token");
        Assertions.assertNull(refreshToken);
        Mockito.verify(tokenDAO, Mockito.times(1)).readRefreshToken("refresh_token");
    }

    @Test
    void readAuthenticationForRefreshToken() throws Exception {
        when(tokenDAO.readAuthenticationForRefreshToken(Mockito.any(OAuth2RefreshToken.class))).thenReturn(Mockito.any(OAuth2Authorization.class));
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("value", Instant.now());
        OAuth2Authorization auth = this.tokenManager.readAuthenticationForRefreshToken(refreshToken);
        Assertions.assertNull(auth);
        Mockito.verify(tokenDAO, Mockito.times(1)).readAuthenticationForRefreshToken(refreshToken);
    }

    @Test
    void removeRefreshToken() throws Exception {
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("value_1", Instant.now());
        this.tokenManager.removeRefreshToken(refreshToken);
        Mockito.verify(tokenDAO, Mockito.times(1)).removeAccessTokenUsingRefreshToken("value_1");
    }

    @Test
    void removeAccessTokenUsingRefreshToken() throws Exception {
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("value_2", Instant.now());
        this.tokenManager.removeAccessTokenUsingRefreshToken(refreshToken);
        Mockito.verify(tokenDAO, Mockito.times(1)).removeAccessTokenUsingRefreshToken("value_2");
    }

    @Test
    void getAccessToken() throws Exception {
        OAuth2AccessToken token = tokenManager.getAccessToken(this.createMockAuthentication());
        Assertions.assertNotNull(token);
        Assertions.assertTrue(token instanceof OAuth2AccessTokenImpl);
        Assertions.assertEquals("clientId", ((OAuth2AccessTokenImpl) token).getClientId());
        Assertions.assertEquals("username", ((OAuth2AccessTokenImpl) token).getLocalUser());
    }

    private OAuth2AccessToken createMockAccessToken() {
        OAuth2AccessTokenImpl token = new OAuth2AccessTokenImpl("token");
//        token.setValue("token");
        token.setClientId("client_id");
        token.setExpiration(new Date());
        token.setGrantType("password");
        token.setLocalUser("username");
        token.setRefreshToken(new OAuth2RefreshToken("refresh", Instant.now()));
//        token.setTokenType("bearer");
        return token;
    }

    private OAuth2Authorization createMockAuthentication() {
        // Create a registered client for testing
        RegisteredClient registeredClient = RegisteredClient.withId("client-1")
                .clientId("clientId")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:8080/callback")
                .build();

        // Create authentication token
        TestingAuthenticationToken authenticationToken = new TestingAuthenticationToken("username", "password");

        // Build OAuth2Authorization using the builder pattern
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName("username")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .attribute("java.security.Principal", authenticationToken)
                .build();

        return authorization;
    }

}
