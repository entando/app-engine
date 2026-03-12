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
package org.entando.entando.keycloak.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.net.URI;
import java.util.List;
import org.entando.entando.aps.system.exception.RestServerError;
import org.entando.entando.keycloak.services.oidc.OpenIDConnectService;
import org.entando.entando.keycloak.services.oidc.exception.OidcException;
import org.entando.entando.keycloak.services.oidc.model.AuthResponse;
import org.entando.entando.keycloak.services.oidc.model.UserRepresentation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class KeycloakServiceTest {

    @Mock
    private KeycloakConfiguration keycloakConfiguration;
    @Mock
    private OpenIDConnectService openIDConnectService;
    @Mock
    private RestTemplate restTemplate;

    private KeycloakService keycloakService;

    @BeforeEach
    public void setUp() {
        keycloakService = new KeycloakService(keycloakConfiguration, openIDConnectService, restTemplate);
        Mockito.when(keycloakConfiguration.getAuthUrl()).thenReturn("http://localhost/auth");
        Mockito.when(keycloakConfiguration.getRealm()).thenReturn("my-realm");
    }

    @Test
    void shouldReturnEmptyList() throws Exception {
        mockAuthenticateAPI();
        URI uri = UriComponentsBuilder
                .fromUriString("http://localhost/auth/admin/realms/my-realm/users").build().toUri();
        Mockito.when(restTemplate.exchange(eq(uri), eq(HttpMethod.GET), any(), eq(UserRepresentation[].class)))
                .thenReturn(
                        ResponseEntity.ok(null)
                );

        List<UserRepresentation> users = keycloakService.listUsers();
        Assertions.assertTrue(users.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWithBlankText() throws Exception {
        mockAuthenticateAPI();
        URI uri = UriComponentsBuilder
                .fromUriString("http://localhost/auth/admin/realms/my-realm/users").build().toUri();
        Mockito.when(restTemplate.exchange(eq(uri), eq(HttpMethod.GET), any(), eq(UserRepresentation[].class)))
                .thenReturn(
                        ResponseEntity.ok(null)
                );

        List<UserRepresentation> users = keycloakService.listUsers("");
        Assertions.assertTrue(users.isEmpty());
    }

    @Test
    void shouldListUsersExactSearch() throws Exception {
        mockAuthenticateAPI();
        URI uri = UriComponentsBuilder
                .fromUriString("http://localhost/auth/admin/realms/my-realm/users?username=testuser").build().toUri();
        
        UserRepresentation user = new UserRepresentation();
        user.setUsername("testuser");
        
        Mockito.when(restTemplate.exchange(eq(uri), eq(HttpMethod.GET), any(), eq(UserRepresentation[].class)))
                .thenReturn(ResponseEntity.ok(new UserRepresentation[]{user}));

        List<UserRepresentation> users = keycloakService.listUsers("testuser");
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("testuser", users.get(0).getUsername());
    }

    @Test
    void shouldListUsersExactSearchFilteringMultipleResults() throws Exception {
        mockAuthenticateAPI();
        URI uri = UriComponentsBuilder
                .fromUriString("http://localhost/auth/admin/realms/my-realm/users?username=testuser").build().toUri();

        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername("testuser");
        user1.setEmail("testuser@example.com");

        UserRepresentation user2 = new UserRepresentation();
        user2.setUsername("testuser-other");
        user2.setEmail("other@example.com");

        Mockito.when(restTemplate.exchange(eq(uri), eq(HttpMethod.GET), any(), eq(UserRepresentation[].class)))
                .thenReturn(ResponseEntity.ok(new UserRepresentation[]{user1, user2}));

        List<UserRepresentation> users = keycloakService.listUsers("testuser");
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("testuser", users.get(0).getUsername());
    }

    @Test
    void shouldListUsersWithSpecialCharacters() throws Exception {
        mockAuthenticateAPI();
        // test+user -> test%2Buser (as per encodeForKeycloakSearchAPI)
        // isExact will be false because searchString ("test%2Buser") != text ("test+user")
        
        UserRepresentation user = new UserRepresentation();
        user.setUsername("test+user");

        Mockito.when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(UserRepresentation[].class)))
                .thenAnswer(invocation -> {
                    URI uri = invocation.getArgument(0);
                    String urlString = uri.toString();
                    Assertions.assertTrue(urlString.contains("search=test%2Buser"));
                    Assertions.assertTrue(urlString.contains("briefRepresentation=true"));
                    return ResponseEntity.ok(new UserRepresentation[]{user});
                });

        List<UserRepresentation> users = keycloakService.listUsers("test+user");
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("test+user", users.get(0).getUsername());
    }

    @Test
    void shouldListUsersExactSearchFilteringMultipleResultsByEmail() throws Exception {
        mockAuthenticateAPI();
        URI uri = UriComponentsBuilder
                .fromUriString("http://localhost/auth/admin/realms/my-realm/users?username=testuser@example.com").build().toUri();

        UserRepresentation user1 = new UserRepresentation();
        user1.setUsername("testuser-id");
        user1.setEmail("testuser@example.com");

        UserRepresentation user2 = new UserRepresentation();
        user2.setUsername("other");
        user2.setEmail("other@example.com");

        Mockito.when(restTemplate.exchange(eq(uri), eq(HttpMethod.GET), any(), eq(UserRepresentation[].class)))
                .thenReturn(ResponseEntity.ok(new UserRepresentation[]{user1, user2}));

        List<UserRepresentation> users = keycloakService.listUsers("testuser@example.com");
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("testuser@example.com", users.get(0).getEmail());
    }

    @Test
    void shouldThrowException() throws Exception {
        Mockito.when(openIDConnectService.authenticateAPI())
                .thenThrow(new OidcException(new Exception("test-message")));
        Assertions.assertThrows(RuntimeException.class, () -> keycloakService.listUsers());

        AuthResponse resp = new AuthResponse();
        resp.setAccessToken("access-token-fake");
        Mockito.reset(openIDConnectService);
        Mockito.when(openIDConnectService.authenticateAPI()).thenReturn(resp);
        URI uri = UriComponentsBuilder
                .fromUriString("http://localhost/auth/admin/realms/my-realm/users").build().toUri();
        Mockito.when(restTemplate.exchange(eq(uri), eq(HttpMethod.GET), any(), eq(UserRepresentation[].class)))
                .thenThrow(
                        new HttpClientErrorException(HttpStatus.FORBIDDEN)
                );
        Assertions.assertThrows(RestServerError.class, () -> keycloakService.listUsers());

        Mockito.reset(restTemplate);
        Mockito.when(restTemplate.exchange(eq(uri), eq(HttpMethod.GET), any(), eq(UserRepresentation[].class)))
                .thenThrow(
                        new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR)
                );
        Assertions.assertThrows(HttpClientErrorException.class, () -> keycloakService.listUsers());
    }

    @Test
    void shouldResetPassword() throws Exception {
        mockAuthenticateAPI();
        keycloakService.resetPassword("uuid", "password", false);
        URI uri = UriComponentsBuilder
                .fromUriString("http://localhost/auth/admin/realms/my-realm/users/uuid/reset-password").build().toUri();
        Mockito.verify(restTemplate, Mockito.times(1))
                .exchange(eq(uri), eq(HttpMethod.PUT), any(), eq(Void.class));
    }

    @Test
    void shouldUpdateUser() throws Exception {
        mockAuthenticateAPI();
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId("uuid");
        keycloakService.updateUser(userRepresentation);
        URI uri = UriComponentsBuilder
                .fromUriString("http://localhost/auth/admin/realms/my-realm/users/uuid").build().toUri();
        Mockito.verify(restTemplate, Mockito.times(1))
                .exchange(eq(uri), eq(HttpMethod.PUT), any(), eq(Void.class));
    }

    @Test
    void shouldRemoveUser() throws Exception {
        mockAuthenticateAPI();
        keycloakService.removeUser("uuid");
        URI uri = UriComponentsBuilder
                .fromUriString("http://localhost/auth/admin/realms/my-realm/users/uuid").build().toUri();
        Mockito.verify(restTemplate, Mockito.times(1))
                .exchange(eq(uri), eq(HttpMethod.DELETE), any(), eq(Void.class));
    }

    @Test
    void shouldCreateUser() throws Exception {
        mockAuthenticateAPI();
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername("username");
        ResponseEntity<Void> response = Mockito.mock(ResponseEntity.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(
                UriComponentsBuilder.fromUriString("http://localhost/auth/realms/my-realm/users/uuid").build().toUri());
        Mockito.when(response.getHeaders()).thenReturn(headers);
        Mockito.when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(), eq(Void.class)))
                .thenReturn(response);
        String redirect = keycloakService.createUser(userRepresentation);
        Assertions.assertEquals("uuid", redirect);
        URI uri = UriComponentsBuilder
                .fromUriString("http://localhost/auth/admin/realms/my-realm/users").build().toUri();
        Mockito.verify(restTemplate, Mockito.times(1))
                .exchange(eq(uri), eq(HttpMethod.POST), any(), eq(Void.class));
    }

    private void mockAuthenticateAPI() throws Exception {
        AuthResponse resp = new AuthResponse();
        resp.setAccessToken("access-token-fake");
        Mockito.when(openIDConnectService.authenticateAPI()).thenReturn(resp);
    }
}
