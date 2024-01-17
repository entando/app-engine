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
package org.entando.entando.keycloak.filter;

import static org.entando.entando.KeycloakWiki.wiki;
import static org.entando.entando.aps.servlet.security.KeycloakSecurityConfig.API_PATH;

import com.agiletec.aps.system.EntThreadLocal;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.User;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.aps.util.ApsTenantApplicationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.KeycloakWiki;
import org.entando.entando.aps.servlet.security.GuestAuthentication;
import org.entando.entando.aps.system.exception.RestServerError;
import org.entando.entando.aps.util.UrlUtils;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.keycloak.services.KeycloakAuthorizationManager;
import org.entando.entando.keycloak.services.KeycloakConfiguration;
import org.entando.entando.keycloak.services.KeycloakJson;
import org.entando.entando.keycloak.services.oidc.OpenIDConnectService;
import org.entando.entando.keycloak.services.oidc.model.AccessToken;
import org.entando.entando.keycloak.services.oidc.model.AuthResponse;
import org.entando.entando.web.common.exceptions.EntandoTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;

public class KeycloakFilter implements Filter {

    private static final EntLogger log = EntLogFactory.getSanitizedLogger(KeycloakFilter.class);

    public static final String SESSION_PARAM_STATE = "keycloak-plugin-state";
    public static final String SESSION_PARAM_REDIRECT = "keycloak-plugin-redirectTo";
    public static final String SESSION_PARAM_ACCESS_TOKEN = "keycloak-plugin-access-token";
    public static final String SESSION_PARAM_ID_TOKEN = "keycloak-plugin-id-token";
    public static final String SESSION_PARAM_REFRESH_TOKEN = "keycloak-plugin-refresh-token";

    private final KeycloakConfiguration configuration;
    private final OpenIDConnectService oidcService;
    private final IAuthenticationProviderManager providerManager;
    private final KeycloakAuthorizationManager keycloakGroupManager;
    private final IUserManager userManager;
    private final ObjectMapper objectMapper;

    public KeycloakFilter(final KeycloakConfiguration configuration,
                          final OpenIDConnectService oidcService,
                          final IAuthenticationProviderManager providerManager,
                          final KeycloakAuthorizationManager keycloakGroupManager,
                          final IUserManager userManager) {
        this.configuration = configuration;
        this.oidcService = oidcService;
        this.providerManager = providerManager;
        this.keycloakGroupManager = keycloakGroupManager;
        this.userManager = userManager;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void doFilter(final ServletRequest servletRequest,
                         final ServletResponse servletResponse,
                         final FilterChain chain) throws IOException, ServletException {
        if (!configuration.isEnabled()) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        try {
            EntThreadLocal.clear();

            final HttpServletRequest request = (HttpServletRequest) servletRequest;
            ApsTenantApplicationUtils.extractCurrentTenantCode(request)
                    .filter(StringUtils::isNotBlank)
                    .ifPresent(ApsTenantApplicationUtils::setTenant);

            final HttpServletResponse response = (HttpServletResponse) servletResponse;

            if (!API_PATH.equals(request.getServletPath())) {
                final HttpSession session = request.getSession();
                final String accessToken = (String) session.getAttribute(SESSION_PARAM_ACCESS_TOKEN);

                if (accessToken != null && !isAccessTokenValid(accessToken) && !refreshToken(request)) {
                    invalidateSession(request);
                }
            }

            switch (request.getServletPath()) {
                case "/do/login":
                case "/do/doLogin":
                case "/do/login.action":
                case "/do/doLogin.action":
                    doLogin(request, response, chain);
                    break;
                case "/do/logout":
                case "/do/logout.action":
                    doLogout(request, response);
                    break;
                case "/keycloak.json":
                    returnKeycloakJson(response);
                    break;
                default:
                    handleLoginRedirect(request, response);
                    chain.doFilter(request, response);
            }

        } finally {
            // moved here to clean context everytime also if we have an exception
            EntThreadLocal.destroy();
        }
    }

    private void handleLoginRedirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!API_PATH.equals(request.getServletPath())) {
            HttpSession session = request.getSession();
            if (request.getServletPath().contains("login.page") && request.getParameter("returnUrl") != null) {
                String returnUrl = request.getParameter("returnUrl");
                response.sendRedirect(request.getContextPath() + "/do/login?redirectTo=" + returnUrl);
            } else if (!isRegisteredUser(session)) {
                // Setting the current path as redirect parameter to ensure that a user is redirected back to the
                // desired page after the authentication (in particular when using app-builder/admin-console integration)
                String redirect = request.getServletPath();
                if (request.getQueryString() != null) {
                    redirect += "?" + request.getQueryString();
                }
                if ("/".equals(redirect) || redirect.startsWith("/do/")) {
                    session.setAttribute(SESSION_PARAM_REDIRECT, redirect);
                }
            }
        }
    }

    private boolean isRegisteredUser(HttpSession session) {
        User userFromSession = (User) session.getAttribute("user");
        return userFromSession != null && !"guest".equals(userFromSession.getUsername());
    }

    private void returnKeycloakJson(final HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", "application/json");
        objectMapper.writeValue(response.getOutputStream(), new KeycloakJson(this.configuration));
    }

    private boolean isAccessTokenValid(final String accessToken) {
        final ResponseEntity<AccessToken> tokenResponse = oidcService.validateToken(accessToken);
        return HttpStatus.OK.equals(tokenResponse.getStatusCode())
                && tokenResponse.getBody() != null
                && tokenResponse.getBody().isActive();
    }

    private boolean refreshToken(final HttpServletRequest request) {
        final HttpSession session = request.getSession();
        final String refreshToken = (String) session.getAttribute(SESSION_PARAM_REFRESH_TOKEN);

        if (refreshToken != null) {
            try {
                final ResponseEntity<AuthResponse> refreshResponse = oidcService.refreshToken(refreshToken);
                if (refreshResponse!= null && HttpStatus.OK.equals(refreshResponse.getStatusCode()) && refreshResponse.getBody() != null) {
                    session.setAttribute(SESSION_PARAM_ACCESS_TOKEN, refreshResponse.getBody().getAccessToken());
                    session.setAttribute(SESSION_PARAM_ID_TOKEN, refreshResponse.getBody().getIdToken());
                    session.setAttribute(SESSION_PARAM_REFRESH_TOKEN, refreshResponse.getBody().getRefreshToken());
                    return true;
                }
            } catch (HttpClientErrorException e) {
                if (!HttpStatus.BAD_REQUEST.equals(e.getStatusCode())
                        || e.getResponseBodyAsString() == null
                        || !e.getResponseBodyAsString().contains("invalid_grant")) {
                    log.error("Something unexpected returned while trying to refresh token, the response was [{}] {}",
                            e.getStatusCode().toString(),
                            e.getResponseBodyAsString());
                }
            }
        }

        return false;
    }

    private void invalidateSession(final HttpServletRequest request) {
        final UserDetails guestUser = userManager.getGuestUser();
        final GuestAuthentication guestAuthentication = new GuestAuthentication(guestUser);
        SecurityContextHolder.getContext().setAuthentication(guestAuthentication);
        saveUserOnSession(request, guestUser);
        request.getSession().setAttribute(SESSION_PARAM_ACCESS_TOKEN, null);
        request.getSession().setAttribute(SESSION_PARAM_ID_TOKEN, null);
    }

    private void doLogout(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final HttpSession session = request.getSession();
        final String idToken = (String)session.getAttribute(SESSION_PARAM_ID_TOKEN);
        final String redirectUri = request.getRequestURL().toString().replace("/do/logout.action", "");
        session.invalidate();
        response.sendRedirect(oidcService.getLogoutUrl(redirectUri, idToken));
    }

    private void doLogin(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
        final HttpSession session = request.getSession();
        final String authorizationCode = request.getParameter("code");
        final String stateParameter = request.getParameter("state");
        final String redirectUri = UrlUtils.checkUrl(request.getRequestURL().toString());
        final String redirectTo = request.getParameter("redirectTo");
        final String error = request.getParameter("error");
        final String errorDescription = request.getParameter("error_description");

        if(log.isDebugEnabled()) {
            log.debug(
                    "doLogin with params code:'{}' state:'{}' redirectUri:'{}' redirectTo:'{}' error:'{}' error_description:'{}'",
                    authorizationCode, stateParameter, redirectUri, redirectTo, error, errorDescription);
        }

        checkNoErrorOrThrow(request, error, errorDescription);

        if (authorizationCode != null) {
            checkAndLogStateParameter(stateParameter, session);

            try {
                final ResponseEntity<AuthResponse> responseEntity = oidcService.requestToken(authorizationCode, redirectUri);
                if (!HttpStatus.OK.equals(responseEntity.getStatusCode()) || responseEntity.getBody() == null) {
                    throw new EntandoTokenException("invalid or expired token", request, "guest");
                }

                final ResponseEntity<AccessToken> tokenResponse = oidcService.validateToken(responseEntity.getBody().getAccessToken());
                if (!HttpStatus.OK.equals(tokenResponse.getStatusCode())
                        || tokenResponse.getBody() == null || !tokenResponse.getBody().isActive()) {
                    throw new EntandoTokenException("invalid or expired token", request, "guest");
                }
                final UserDetails user = providerManager.getUser(tokenResponse.getBody().getUsername());
                session.setAttribute(SESSION_PARAM_ACCESS_TOKEN, responseEntity.getBody().getAccessToken());
                session.setAttribute(SESSION_PARAM_ID_TOKEN, responseEntity.getBody().getIdToken());
                session.setAttribute(SESSION_PARAM_REFRESH_TOKEN, responseEntity.getBody().getRefreshToken());

                keycloakGroupManager.processNewUser(user);
                saveUserOnSession(request, user);
                log.info("Successfully authenticated user {}", user.getUsername());
            } catch (HttpClientErrorException e) {
                if (HttpStatus.FORBIDDEN.equals(e.getStatusCode())) {
                    throw new RestServerError("Unable to validate token because the Client in keycloak is configured as public. " +
                            "Please change the client on keycloak to confidential. " +
                            "For more details, refer to the wiki " + wiki(KeycloakWiki.EN_APP_CLIENT_PUBLIC), e);
                }
                if (HttpStatus.BAD_REQUEST.equals(e.getStatusCode())) {
                    if (isInvalidCredentials(e)) {
                        throw new RestServerError("Unable to validate token because the Client credentials are invalid. " +
                                "Please make sure the credentials from keycloak is correctly set in the params or environment variable." +
                                "For more details, refer to the wiki " + wiki(KeycloakWiki.EN_APP_CLIENT_CREDENTIALS), e);
                    } else if (isInvalidCode(e)) {
                        redirect(request, response, session);
                        return;
                    }
                }
                throw new RestServerError("Unable to validate token", e);
            } catch (EntException e) {
                throw new RestServerError("Unable to find user", e);
            }

            redirect(request, response, session);
            return;
        } else {
            buildAndSaveInSessionRedirectPath(redirectTo, request, session);
        }

        final Object user = session.getAttribute(SystemConstants.SESSIONPARAM_CURRENT_USER);

        if (user != null && !((UserDetails)user).getUsername().equals("guest")) {
            chain.doFilter(request, response);
        } else {
            final String state = UUID.randomUUID().toString();
            final String redirectUrl = oidcService.getRedirectUrl(redirectUri, state, fetchSuggestedIdp(request));

            session.setAttribute(SESSION_PARAM_STATE, state);
            log.debug("doLogin sendRedirect redirectUrl:'{}'", redirectUrl);
            response.sendRedirect(redirectUrl);
        }
    }

    private String fetchSuggestedIdp(HttpServletRequest request){
        String  clientSuggestedIdp = request.getParameter("idp_hint");
        log.debug("fetched clientSuggestedIdp:'{}' to compose redirect url with it", clientSuggestedIdp);
        return clientSuggestedIdp;
    }

    private void checkNoErrorOrThrow(HttpServletRequest request, String error, String errorDescription){
        if (StringUtils.isNotEmpty(error)) {
            if ("unsupported_response_type".equals(error)) {
                log.error("{}. For more details, refer to the wiki {}",
                        errorDescription,
                        wiki(KeycloakWiki.EN_APP_STANDARD_FLOW_DISABLED));
            }
            throw new EntandoTokenException(errorDescription, request, SystemConstants.GUEST_USER_NAME);
        }

    }

    private void checkAndLogStateParameter(String stateParameter, HttpSession session){
        if (stateParameter == null) {
            log.warn("State parameter not provided");
        } else if (!stateParameter.equals(session.getAttribute(SESSION_PARAM_STATE))) {
            log.warn("State parameter '{}' is different than generated '{}'", stateParameter, session.getAttribute(SESSION_PARAM_STATE));
        }

    }

    private void buildAndSaveInSessionRedirectPath(String redirectTo, HttpServletRequest request, HttpSession session){
        if (redirectTo != null) {
            if (log.isDebugEnabled()) {
                log.debug("doLogin evaluate redirect with redirectTo:'{}' requestURL:'{}' servletPath:'{}'",
                        redirectTo, request.getRequestURL(), request.getServletPath());
            }
            Optional<String> redirectToPath = UrlUtils.fetchPathFromUri(redirectTo);
            String redirectPathWithoutContextRoot = redirectToPath
                    .flatMap(p -> UrlUtils.removeContextRootFromPath(p, request))
                    .orElse("/");

            UrlUtils.fetchServerNameFromUri(redirectTo).ifPresent(redirectServerName -> {
                if (!redirectServerName.equals(request.getServerName())) {
                    // this was exception
                    log.warn("request server name:'{}' is NOT equals to redirectTo param server name:'{}'",
                            request.getServerName(),
                            redirectServerName);
                }
            });
            log.debug("doLogin set SESSION_PARAM_REDIRECT redirect:'{}'", redirectPathWithoutContextRoot);
            session.setAttribute(SESSION_PARAM_REDIRECT, redirectPathWithoutContextRoot);
        }
    }

    private void saveUserOnSession(final HttpServletRequest request, final UserDetails user) {
        request.getSession().setAttribute("user", user);
        request.getSession().setAttribute(SystemConstants.SESSIONPARAM_CURRENT_USER, user);
    }

    private void redirect(final HttpServletRequest request, final HttpServletResponse response, final HttpSession session) throws IOException {
        final String redirectPath = session.getAttribute(SESSION_PARAM_REDIRECT) != null
                ? session.getAttribute(SESSION_PARAM_REDIRECT).toString()
                : "/do/main";
        String baseUrl = UrlUtils.composeBaseUrl(request).toString();
        String redirectUrl = baseUrl + request.getContextPath() + redirectPath;
        log.info("Redirecting user to {}", redirectUrl);
        session.setAttribute(SESSION_PARAM_REDIRECT, null);
        response.sendRedirect(redirectUrl);
    }

    private boolean isInvalidCredentials(final HttpClientErrorException exception) {
        return StringUtils.contains(exception.getResponseBodyAsString(), "unauthorized_client");
    }

    private boolean isInvalidCode(final HttpClientErrorException exception) {
        return StringUtils.contains(exception.getResponseBodyAsString(), "invalid_grant");
    }

    @Override public void init(final FilterConfig filterConfig) {}
    @Override public void destroy() {}
}
