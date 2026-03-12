package org.entando.entando.keycloak.services;

import static org.entando.entando.KeycloakWiki.wiki;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.KeycloakWiki;
import org.entando.entando.aps.system.exception.RestServerError;
import org.entando.entando.keycloak.services.oidc.OpenIDConnectService;
import org.entando.entando.keycloak.services.oidc.exception.OidcException;
import org.entando.entando.keycloak.services.oidc.model.AuthResponse;
import org.entando.entando.keycloak.services.oidc.model.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class KeycloakService {

    private final OpenIDConnectService oidcService;
    private final KeycloakConfiguration configuration;
    private final RestTemplate restTemplate;

    @Autowired
    public KeycloakService(final KeycloakConfiguration configuration, final OpenIDConnectService oidcService, @Qualifier("keycloakRestTemplate")final RestTemplate rest) {
        this.configuration = configuration;
        this.oidcService = oidcService;
        this.restTemplate = rest;
    }

    public List<UserRepresentation> listUsers() {
        return listUsers(null);
    }

    // Handle invocations such as http://localhost:8081/auth/admin/realms/entando-development/users?briefRepresentation=true&first=0&max=20&search=testutentemariorossi%2B4375@gmail.com
    public List<UserRepresentation> listUsers(final String text) {
        final String url = String.format("%s/admin/realms/%s/users", configuration.getAuthUrl(), configuration.getRealm());
        final String searchString = StringUtils.isNotBlank(text) ? encodeForKeycloakSearchAPI(text) : text;
        final boolean isExact = StringUtils.isBlank(text) || (StringUtils.isNotBlank(text) && searchString.equals(text));
        final Map<String, String> params = this.buildParams(searchString, isExact);

        final String token = this.extractToken();
        final ResponseEntity<UserRepresentation[]> response = this.executeEscapedRequest(token, url,
                HttpMethod.GET, createEntity(token, null), UserRepresentation[].class, params, 0);
        List<UserRepresentation> retval = Optional.ofNullable(response.getBody())
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
        if (StringUtils.isNotBlank(text) && isExact && retval.size() > 1) {
            // must match the exact element by USERNAME
            Optional<UserRepresentation> userOpt = retval.stream()
                    .filter(e ->  (e.getUsername() != null && e.getUsername().equals(text))
                            || (e.getEmail() != null && e.getEmail().equals(text)))
                    .findFirst();
            return userOpt.stream().collect(Collectors.toList());
        }
        return retval;
    }

    private Map<String, String> buildParams(final String text, boolean isExact) {
        final Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(text)) {
            if (isExact) {
                params.put("username", text);
            } else {
                params.put("briefRepresentation", "true");
                params.put("search", text);
                // reference for paged request:
                // params.put("first", "0");
                // params.put("max", "20");
            }
        }
        return params;
    }

    private String encodeForKeycloakSearchAPI(String text) {
        final List<Character> specialCharacters = List.of(
                '#', '+', '&', '%', '\'', '/', '=', '?', '^', '{', '|', '}', '`', '"'
        );
        final StringBuilder encoded = new StringBuilder();

        for (char c : text.toCharArray()) {
            if (specialCharacters.contains(c)) {
                encoded.append(URLEncoder.encode(String.valueOf(c), StandardCharsets.UTF_8));
            } else {
                encoded.append(c);
            }
        }
        return encoded.toString();
    }

    public void removeUser(final String uuid) {
        final String url = String.format("%s/admin/realms/%s/users/%s", configuration.getAuthUrl(), configuration.getRealm(), uuid);
        String token = this.extractToken();
        this.executeRequest(token, url, HttpMethod.DELETE, createEntity(token));
    }

    public void resetPassword(final String uuid, final String password, final Boolean temporary) {
        final String url = String.format("%s/admin/realms/%s/users/%s/reset-password", configuration.getAuthUrl(), configuration.getRealm(), uuid);
        final Map<String, Object> body = new HashMap<>();
        body.put("value", password);
        body.put("temporary", temporary);
        body.put("type", "password");
        String token = this.extractToken();
        this.executeRequest(token, url, HttpMethod.PUT, createEntity(token, body));
    }

    public String createUser(final UserRepresentation user) {
        final String url = String.format("%s/admin/realms/%s/users", configuration.getAuthUrl(), configuration.getRealm());
        String token = this.extractToken();
        final ResponseEntity<Void> response = this.executeRequest(token, url, HttpMethod.POST, createEntity(token, user));
        return Optional.ofNullable(response.getHeaders().getLocation())
                .map(location -> location.getPath().replaceAll(".*/([^/]+)$", "$1"))
                .orElseThrow(() -> new RuntimeException("User id response shouldn't return null from Keycloak"));
    }

    public void updateUser(final UserRepresentation user) {
        final String url = String.format("%s/admin/realms/%s/users/%s", configuration.getAuthUrl(), configuration.getRealm(), user.getId());
        String token = this.extractToken();
        this.executeRequest(token, url, HttpMethod.PUT, createEntity(token, user));
    }

    private <T> HttpEntity<T> createEntity(String token) {
        return createEntity(token, null);
    }

    private <T> HttpEntity<T> createEntity(String token, final T body) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        if (body != null) {
            headers.add("Content-Type", "application/json");
        }
        return new HttpEntity<>(body, headers);
    }

    private <T> ResponseEntity<Void> executeRequest(String token, final String url, final HttpMethod method, final HttpEntity<T> entity) {
        return this.executeRequest(token, url, method, entity, Void.class, Collections.emptyMap());
    }

    private <T, Y> ResponseEntity<Y> executeRequest(String token, final String url, final HttpMethod method, final HttpEntity<T> entity,
                                                    final Class<Y> result, final Map<String, String> params) {
        return executeRequest(token, url, method, entity, result, params, 0);
    }

    private <T, Y> ResponseEntity<Y> executeRequest(String token, final String url, final HttpMethod method, final HttpEntity<T> entity,
                                                    final Class<Y> result, final Map<String, String> params, int retryCount) {
        try {
            final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
            params.forEach(builder::queryParam);

            return restTemplate.exchange(builder.build().toUri(), method, createEntity(token, entity.getBody()), result);
        } catch (HttpClientErrorException e) {
            if (HttpStatus.FORBIDDEN.equals(e.getStatusCode()) || (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode()) && retryCount > 10)) {
                throw new RestServerError("There was an error while trying to load user because the " +
                        "client on Keycloak doesn't have permission to do that. " +
                        "The client needs to have Service Accounts enabled and the permission 'realm-admin' on client 'realm-management'. " +
                        "For more details, refer to the wiki " + wiki(KeycloakWiki.EN_APP_CLIENT_FORBIDDEN), e);
            }
            if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
                return this.executeRequest(null, url, method, entity, result, params, retryCount + 1);
            }
            throw e;
        }
    }

    private <T, Y> ResponseEntity<Y> executeEscapedRequest(String token, final String url, final HttpMethod method, final HttpEntity<T> entity,
            final Class<Y> result, final Map<String, String> params, int retryCount) {
        try {
            final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
            final Optional<String> isAlreadyEscaped = params.values()
                    .stream().filter(v -> StringUtils.isNotBlank(v) && v.contains("%"))
                    .findFirst();
            if (isAlreadyEscaped.isEmpty()) {
                // default behaviour, query string is escaped automatically
                params.forEach(builder::queryParam);
                return restTemplate.exchange(builder.build().toUri(), method,
                        createEntity(token, entity.getBody()), result);
            } else {
                /*
                 * We don't want to automatically escape character since they come
                 * already escaped, so we construct URI directly from the string
                 */
                StringBuilder queryBuilder = new StringBuilder();
                params.forEach((key, value) -> {
                    if (queryBuilder.length() > 0) {
                        queryBuilder.append("&");
                    }
                    queryBuilder.append(key).append("=").append(value);
                });
                String escapedUrl = builder.build().toUri() + "?" + queryBuilder;
                return  restTemplate.exchange(
                        URI.create(escapedUrl), method, createEntity(token, entity.getBody()),
                        result);
            }
        } catch (HttpClientErrorException e) {
            if (HttpStatus.FORBIDDEN.equals(e.getStatusCode()) || (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode()) && retryCount > 10)) {
                throw new RestServerError("There was an error while trying to load user because the " +
                        "client on Keycloak doesn't have permission to do that. " +
                        "The client needs to have Service Accounts enabled and the permission 'realm-admin' on client 'realm-management'. ", e);
            }
            if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode())) {
                return this.executeEscapedRequest(null, url, method, entity, result, params, retryCount + 1);
            }
            throw e;
        }
    }

    private String extractToken() {
        try {
            final AuthResponse authResponse = oidcService.authenticateAPI();
            return authResponse.getAccessToken();
        } catch (OidcException e) {
            throw new RuntimeException(e);
        }
    }

}
