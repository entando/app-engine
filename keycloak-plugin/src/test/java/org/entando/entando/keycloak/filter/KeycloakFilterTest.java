package org.entando.entando.keycloak.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.User;
import com.agiletec.aps.system.services.user.UserDetails;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.StringWriter;
import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.keycloak.services.KeycloakAuthorizationManager;
import org.entando.entando.keycloak.services.KeycloakConfiguration;
import org.entando.entando.keycloak.services.oidc.OpenIDConnectService;
import org.entando.entando.keycloak.services.oidc.model.AccessToken;
import org.entando.entando.keycloak.services.oidc.model.AuthResponse;
import org.entando.entando.web.common.exceptions.EntandoTokenException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

@ExtendWith(MockitoExtension.class)
class KeycloakFilterTest {

    @Mock private KeycloakConfiguration configuration;
    @Mock private OpenIDConnectService oidcService;
    @Mock private IAuthenticationProviderManager providerManager;
    @Mock private KeycloakAuthorizationManager keycloakGroupManager;
    @Mock private IUserManager userManager;
    @Mock private ITenantManager tenantManager;

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private ServletContext svCtx;
    @Mock private WebApplicationContext wac;
    @Mock private FilterChain filterChain;

    @Mock private ResponseEntity<AccessToken> accessTokenResponse;
    @Mock private ResponseEntity<AuthResponse> authResponse;
    @Mock private ResponseEntity<AuthResponse> refreshResponse;
    @Mock private UserDetails userDetails;

    @Mock private AccessToken accessToken;
    @Mock private AuthResponse auth;
    @Mock private AuthResponse refreshAuth;

    private KeycloakFilter keycloakFilter;

    @BeforeEach
    public void setUp() {

        Mockito.lenient().when(configuration.getAuthUrl()).thenReturn("https://dev.entando.org/auth");
        Mockito.lenient().when(configuration.getRealm()).thenReturn("entando");
        Mockito.lenient().when(configuration.getClientId()).thenReturn("entando-app");
        Mockito.lenient().when(configuration.getPublicClientId()).thenReturn("entando-web");
        Mockito.lenient().when(configuration.getClientSecret()).thenReturn("a76d5398-fc2f-4859-bf57-f043a89eea70");

        keycloakFilter = new KeycloakFilter(configuration, oidcService, providerManager, keycloakGroupManager, userManager);
        Mockito.lenient().when(request.getSession()).thenReturn(session);
        Mockito.lenient().when(session.getServletContext()).thenReturn(svCtx);
        Mockito.lenient().when(wac.getBean(ITenantManager.class)).thenReturn(tenantManager);
        Mockito.lenient().when(request.getServerName()).thenReturn("dev.entando.org");
        Mockito.lenient().when(request.getScheme()).thenReturn("https");
        Mockito.lenient().when(request.getServerPort()).thenReturn(443);
        Mockito.lenient().when(request.getHeader("X-Forwarded-Proto")).thenReturn("https");
        Mockito.lenient().when(request.getHeader("Host")).thenReturn("dev.entando.org");
    }

    @Test
    void testConfigurationDisabled() throws IOException, ServletException {
        when(configuration.isEnabled()).thenReturn(false);
        keycloakFilter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(same(request), same(response));
    }

    @Disabled("Disabled due to junit5 integration")
    @Test
    void testAuthenticationFlow() throws IOException, ServletException, EntException {
        final String requestRedirect = "https://dev.entando.org/entando-app/main.html";
        final String loginEndpoint = "https://dev.entando.org/entando-app/do/login";

        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn("/do/login");
        when(request.getRequestURL()).thenReturn(new StringBuffer(loginEndpoint));
        Mockito.lenient().when(request.getParameter(eq("redirectTo"))).thenReturn(requestRedirect);

        final String redirect = "https://dev.entando.org/auth/realms/entando/protocol/openid-connect/auth";
        when(oidcService.getRedirectUrl(any(), any(), any())).thenReturn(redirect);

        keycloakFilter.doFilter(request, response, filterChain);
        verify(filterChain, times(0)).doFilter(any(), any());
        verify(response, times(1)).sendRedirect(redirect);
        verify(oidcService, times(1)).getRedirectUrl(eq(loginEndpoint), anyString(), anyString());
        verify(session, times(1)).setAttribute(eq(KeycloakFilter.SESSION_PARAM_REDIRECT), eq("/main.html"));
        verify(session, times(1)).setAttribute(eq(KeycloakFilter.SESSION_PARAM_STATE), anyString());

        reset(session, oidcService, filterChain, response, request);

        final String state = "0ca97afd-f0b0-4860-820a-b7cd1414f69c";
        final String authorizationCode = "the-authorization-code-from-keycloak";

        when(request.getSession()).thenReturn(session);
        when(session.getAttribute(eq(KeycloakFilter.SESSION_PARAM_STATE))).thenReturn(state);
        when(session.getAttribute(eq(KeycloakFilter.SESSION_PARAM_REDIRECT))).thenReturn("/main.html");

        when(request.getServletPath()).thenReturn("/do/login");
        when(request.getParameter(eq("code"))).thenReturn(authorizationCode);
        when(request.getParameter(eq("state"))).thenReturn(state);
        when(request.getRequestURL()).thenReturn(new StringBuffer(loginEndpoint));
        when(request.getContextPath()).thenReturn("/entando-app");

        when(oidcService.requestToken(anyString(), anyString())).thenReturn(authResponse);
        when(authResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(authResponse.getBody()).thenReturn(auth);
        when(auth.getAccessToken()).thenReturn("access-token-over-here");
        when(auth.getRefreshToken()).thenReturn("refresh-token-over-here");

        when(oidcService.validateToken(anyString())).thenReturn(accessTokenResponse);
        when(accessTokenResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(accessTokenResponse.getBody()).thenReturn(accessToken);
        when(accessToken.isActive()).thenReturn(true);
        when(accessToken.getUsername()).thenReturn("admin");
        when(providerManager.getUser(anyString())).thenReturn(userDetails);

        keycloakFilter.doFilter(request, response, filterChain);

        verify(oidcService, times(1)).requestToken(eq(authorizationCode), eq(loginEndpoint));
        verify(oidcService, times(1)).validateToken(eq("access-token-over-here"));
        verify(keycloakGroupManager, times(1)).processNewUser(same(userDetails), null, false);

        verify(session, times(1)).setAttribute(eq("user"), same(userDetails));
        verify(session, times(1)).setAttribute(eq(SystemConstants.SESSIONPARAM_CURRENT_USER), same(userDetails));
        verify(response, times(1)).sendRedirect(eq("https://dev.entando.org/entando-app/main.html"));
        verify(session, times(1)).setAttribute(eq(KeycloakFilter.SESSION_PARAM_REDIRECT), isNull());
    }

    @Test
    void testAuthenticationFlowWithError() {
        final String state = "0ca97afd-f0b0-4860-820a-b7cd1414f69c";
        final String authorizationCode = "the-authorization-code-from-keycloak";

        when(configuration.isEnabled()).thenReturn(true);
        when(request.getSession()).thenReturn(session);
        Mockito.lenient().when(session.getAttribute(eq(KeycloakFilter.SESSION_PARAM_STATE))).thenReturn(state);
        Mockito.lenient().when(session.getAttribute(eq(KeycloakFilter.SESSION_PARAM_REDIRECT))).thenReturn("/main.html");

        // error provided by keycloak
        Mockito.lenient().when(request.getParameter(eq("error"))).thenReturn("invalid_code");
        Mockito.lenient().when(request.getParameter(eq("error_description"))).thenReturn("Any description provided by keycloak");

        when(request.getServletPath()).thenReturn("/do/login");
        when(request.getParameter(eq("code"))).thenReturn(authorizationCode);
        when(request.getParameter(eq("state"))).thenReturn(state);
        Mockito.lenient().when(request.getContextPath()).thenReturn("/entando-app");


        Mockito.lenient().when(oidcService.requestToken(anyString(), anyString())).thenReturn(authResponse);
        Mockito.lenient().when(authResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.lenient().when(authResponse.getBody()).thenReturn(auth);
        Mockito.lenient().when(auth.getAccessToken()).thenReturn("access-token-over-here");
        Mockito.lenient().when(auth.getRefreshToken()).thenReturn("refresh-token-over-here");
        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            Assertions.assertThrows(EntandoTokenException.class, () -> keycloakFilter.doFilter(request, response, filterChain));
        }
    }

    @Test
    void testAuthenticationWithInvalidAuthCode() throws IOException, ServletException {
        final String state = "0ca97afd-f0b0-4860-820a-b7cd1414f69c";
        final String authorizationCode = "the-authorization-code-from-keycloak";

        when(configuration.isEnabled()).thenReturn(true);
        when(request.getSession()).thenReturn(session);
        Mockito.lenient().when(session.getAttribute(eq(KeycloakFilter.SESSION_PARAM_STATE))).thenReturn(state);
        Mockito.lenient().when(session.getAttribute(eq(KeycloakFilter.SESSION_PARAM_REDIRECT))).thenReturn("/main.html");

        when(request.getServletPath()).thenReturn("/do/login");
        when(request.getParameter(eq("code"))).thenReturn(authorizationCode);
        when(request.getParameter(eq("state"))).thenReturn(state);
        when(request.getContextPath()).thenReturn("/entando-app");

        final HttpClientErrorException exception = Mockito.mock(HttpClientErrorException.class);
        when(exception.getResponseBodyAsString()).thenReturn("{ \"error\": \"invalid_grant\", \"error_description\": \"Refresh token expired\" }");
        when(exception.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);

        when(oidcService.requestToken(anyString(), anyString())).thenThrow(exception);

        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
            verify(response, times(1)).sendRedirect(eq("https://dev.entando.org/entando-app/main.html"));
        }
    }

    @Test
    void shouldLoginWithInvalidRedirectURLHostnameNotThrowError() throws IOException, ServletException {
        final String requestRedirect = "https://not.authorized.url";

        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn("/do/login");
        Mockito.lenient().when(request.getParameter(eq("redirectTo"))).thenReturn(requestRedirect);

        final String redirect = "http://dev.entando.org/auth/realms/entando/protocol/openid-connect/auth";
        Mockito.lenient().when(oidcService.getRedirectUrl(any(), any(), any())).thenReturn(redirect);
        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }
        verify(filterChain, times(0)).doFilter(any(), any());
        verify(session,times(1)).setAttribute(KeycloakFilter.SESSION_PARAM_REDIRECT, "/");
    }

    @Test
    void testLogout() throws IOException, ServletException {
        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn("/do/logout.action");

        final String redirect = "http://dev.entando.org/auth/realms/entando/protocol/openid-connect/logout";
        when(oidcService.getLogoutUrl(any(),any())).thenReturn(redirect);

        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }
        verify(filterChain, times(0)).doFilter(any(), any());
        verify(response, times(1)).sendRedirect(redirect);
        verify(session, times(1)).invalidate();
    }

    @Test
    void testNormalFlow() throws IOException, ServletException {
        final String endpoint = "https://dev.entando.org/entando-app/do/main";

        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn("/do/main");
        Mockito.lenient().when(request.getRequestURL()).thenReturn(new StringBuffer(endpoint));

        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }
        verify(filterChain, times(1)).doFilter(any(), any());
    }

    @Test
    void testTokenValidation() throws IOException, ServletException {
        final String path = "/do/main";
        final String endpoint = "https://dev.entando.org/entando-app" + path;

        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn(path);
        Mockito.lenient().when(request.getRequestURL()).thenReturn(new StringBuffer(endpoint));

        when(session.getAttribute(eq(KeycloakFilter.SESSION_PARAM_ACCESS_TOKEN))).thenReturn("access-token-over-here");
        when(oidcService.validateToken(anyString())).thenReturn(accessTokenResponse);
        when(accessTokenResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(accessTokenResponse.getBody()).thenReturn(accessToken);
        when(accessToken.isActive()).thenReturn(false);

        when(userManager.getGuestUser()).thenReturn(userDetails);

        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }
        verify(filterChain, times(1)).doFilter(any(), any());
        verify(userManager, times(1)).getGuestUser();
        verify(session, times(1)).setAttribute(eq("user"), same(userDetails));
        verify(session, times(1)).setAttribute(eq(SystemConstants.SESSIONPARAM_CURRENT_USER), same(userDetails));
        verify(session, times(1)).setAttribute(eq(KeycloakFilter.SESSION_PARAM_ACCESS_TOKEN), isNull());
    }

    @Test
    void testTokenValidationAndRefreshToken() throws IOException, ServletException {
        final String path = "/do/main";
        final String endpoint = "https://dev.entando.org/entando-app" + path;

        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn(path);
        Mockito.lenient().when(request.getRequestURL()).thenReturn(new StringBuffer(endpoint));

        when(session.getAttribute(eq(KeycloakFilter.SESSION_PARAM_ACCESS_TOKEN))).thenReturn("access-token-over-here");
        when(session.getAttribute(eq(KeycloakFilter.SESSION_PARAM_REFRESH_TOKEN))).thenReturn("refresh-token-over-here");

        when(oidcService.validateToken(anyString())).thenReturn(accessTokenResponse);
        when(accessTokenResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(accessTokenResponse.getBody()).thenReturn(accessToken);
        when(accessToken.isActive()).thenReturn(false);

        final String newAccessToken = "a-new-access-token-over-here";
        final String newRefreshToken = "a-new-refresh-token-over-here";

        when(oidcService.refreshToken(anyString())).thenReturn(refreshResponse);
        when(refreshResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(refreshResponse.getBody()).thenReturn(refreshAuth);
        when(refreshAuth.getAccessToken()).thenReturn(newAccessToken);
        when(refreshAuth.getRefreshToken()).thenReturn(newRefreshToken);
        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }

        verify(filterChain, times(1)).doFilter(any(), any());

        verify(session, times(1)).setAttribute(eq(KeycloakFilter.SESSION_PARAM_ACCESS_TOKEN), eq(newAccessToken));
        verify(session, times(1)).setAttribute(eq(KeycloakFilter.SESSION_PARAM_REFRESH_TOKEN), eq(newRefreshToken));
    }

    @Test
    void testTokenValidationWithTokenAndRefreshExpired() throws IOException, ServletException {
        final String path = "/do/main";
        final String endpoint = "https://dev.entando.org/entando-app" + path;

        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn(path);
        Mockito.lenient().when(request.getRequestURL()).thenReturn(new StringBuffer(endpoint));

        when(session.getAttribute(eq(KeycloakFilter.SESSION_PARAM_ACCESS_TOKEN))).thenReturn("access-token-over-here");
        when(session.getAttribute(eq(KeycloakFilter.SESSION_PARAM_REFRESH_TOKEN))).thenReturn("refresh-token-over-here");
        when(oidcService.validateToken(anyString())).thenReturn(accessTokenResponse);
        when(accessTokenResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(accessTokenResponse.getBody()).thenReturn(accessToken);
        when(accessToken.isActive()).thenReturn(false);

        final HttpClientErrorException exception = Mockito.mock(HttpClientErrorException.class);
        when(exception.getResponseBodyAsString()).thenReturn("{ \"error\": \"invalid_grant\", \"error_description\": \"Refresh token expired\" }");
        when(exception.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(oidcService.refreshToken(anyString())).thenThrow(exception);

        when(userManager.getGuestUser()).thenReturn(userDetails);

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }
        verify(filterChain, times(1)).doFilter(any(), any());
        verify(userManager, times(1)).getGuestUser();
        verify(session, times(1)).setAttribute(eq("user"), same(userDetails));
        verify(session, times(1)).setAttribute(eq(SystemConstants.SESSIONPARAM_CURRENT_USER), same(userDetails));
        verify(session, times(1)).setAttribute(eq(KeycloakFilter.SESSION_PARAM_ACCESS_TOKEN), isNull());
    }

    @Test
    void testTokenValidationWithTokenAndRefreshReturnResponseNull() throws IOException, ServletException {
        final String path = "/do/main";
        final String endpoint = "https://dev.entando.org/entando-app" + path;

        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn(path);
        Mockito.lenient().when(request.getRequestURL()).thenReturn(new StringBuffer(endpoint));

        when(session.getAttribute(eq(KeycloakFilter.SESSION_PARAM_ACCESS_TOKEN))).thenReturn("access-token-over-here");
        when(session.getAttribute(eq(KeycloakFilter.SESSION_PARAM_REFRESH_TOKEN))).thenReturn("refresh-token-over-here");
        when(oidcService.validateToken(anyString())).thenReturn(accessTokenResponse);
        when(accessTokenResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(accessTokenResponse.getBody()).thenReturn(accessToken);
        when(accessToken.isActive()).thenReturn(false);

        when(oidcService.refreshToken(anyString())).thenReturn(refreshResponse);
        when(refreshResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(refreshResponse.getBody()).thenReturn(null);

        when(userManager.getGuestUser()).thenReturn(userDetails);

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }
        verify(filterChain, times(1)).doFilter(any(), any());
        verify(userManager, times(1)).getGuestUser();
        verify(session, times(1)).setAttribute(eq("user"), same(userDetails));
        verify(session, times(1)).setAttribute(eq(SystemConstants.SESSIONPARAM_CURRENT_USER), same(userDetails));
        verify(session, times(1)).setAttribute(eq(KeycloakFilter.SESSION_PARAM_ACCESS_TOKEN), isNull());
        verify(session, times(1)).setAttribute(eq(KeycloakFilter.SESSION_PARAM_ID_TOKEN), isNull());
    }

    @Test
    void apiCallShouldNotSaveUserOnSession() throws Exception {
        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn("/api");

        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }

        verify(filterChain, times(1)).doFilter(any(), any());
        verify(request, times(1)).getSession();
    }

    @Test
    void testLoginWithAuthorizationCode() throws Exception {

        final String path = "/do/login";

        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn(path);
        when(request.getParameter("state")).thenReturn("<state>");
        when(request.getParameter("code")).thenReturn("<code>");

        AuthResponse authResponse = new AuthResponse();
        authResponse.setAccessToken("<token>");
        when(oidcService.requestToken(eq("<code>"), any())).thenReturn(ResponseEntity.ok().body(authResponse));

        AccessToken token = new AccessToken();
        token.setActive(true);
        token.setUsername("<username>");
        when(oidcService.validateToken("<token>")).thenReturn(ResponseEntity.ok().body(token));
        when(providerManager.getUser("<username>")).thenReturn(userDetails);

        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }

        verify(session, times(1)).setAttribute(eq("user"), same(userDetails));
    }

    @Test
    void testKeycloakJsonEndpoint() throws IOException, ServletException {
        final String path = "/keycloak.json";
        final String endpoint = "https://dev.entando.org/entando-app" + path;

        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn(path);
        Mockito.lenient().when(request.getRequestURL()).thenReturn(new StringBuffer(endpoint));

        final StringWriter writer = new StringWriter();
        when(response.getOutputStream()).thenReturn(new ServletOutputStreamWrapper(writer));

        try (MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }

        verify(response, times(1)).setHeader(eq("Content-Type"), eq("application/json"));

        final String json = writer.toString();

        assertJsonString(json, "$.realm").isEqualTo(configuration.getRealm());
        assertJsonString(json, "$.auth-server-url").isEqualTo(configuration.getAuthUrl());
        assertJsonString(json, "$.resource").isEqualTo(configuration.getPublicClientId());
        assertJsonString(json, "$.ssl-required").isEqualTo("external");
        assertJsonBoolean(json, "$.public-client").isTrue();
    }

    @Test
    void testRedirectParameterIsSetForNullUser() throws Exception {

        String path = "/do/jacms/Content/list.action";

        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn(path);
        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }

        verify(session).setAttribute(KeycloakFilter.SESSION_PARAM_REDIRECT, "/do/jacms/Content/list.action");
    }

    @Test
    void testRedirectParameterIsSetForGuestUser() throws Exception {
        String path = "/do/jacms/Content/list.action";

        User user = new User();
        user.setUsername("guest");
        Mockito.lenient().when(session.getAttribute(eq("user"))).thenReturn(user);
        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn(path);
        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }
        verify(session).setAttribute(KeycloakFilter.SESSION_PARAM_REDIRECT, "/do/jacms/Content/list.action");
    }

    @Test
    void testRedirectParameterWithQueryStringIsSet() throws Exception {

        String path = "/do/jacms/Resource/list.action";
        String queryString = "resourceTypeCode=Image";

        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn(path);
        when(request.getQueryString()).thenReturn(queryString);
        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }
        verify(session).setAttribute(KeycloakFilter.SESSION_PARAM_REDIRECT, "/do/jacms/Resource/list.action?resourceTypeCode=Image");
    }

    @Test
    void testLoginPageRedirect() throws Exception {

        String path = "/en/login.page";
        String returnUrlParam = "https%3A%2F%2Fdev.entando.org%2Fentando-app";
        String contextPath = "https://dev.entando.org/entando-app";

        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn(path);
        when(request.getParameter("returnUrl")).thenReturn(returnUrlParam);
        when(request.getContextPath()).thenReturn(contextPath);
        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }
        verify(response).sendRedirect("https://dev.entando.org/entando-app/do/login?redirectTo=" + returnUrlParam);
    }

    @Test
    void testRedirectParameterNotStartingWithDoPrefix() throws Exception {

        String path = "/resources";

        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn(path);
        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }

        verify(session, never()).setAttribute(eq(KeycloakFilter.SESSION_PARAM_REDIRECT), any());
    }

    @Test
    void testRedirectParameterRoot() throws Exception {

        String path = "/";

        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn(path);
        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }

        verify(session).setAttribute(KeycloakFilter.SESSION_PARAM_REDIRECT, "/");
    }


    @Test
    void shouldLoginWithRedirectToDifferentDomainNotThrowException() throws Exception {

        String path = "/do/login.action";
        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn(path);
        when(request.getParameter("code")).thenReturn(null);
        when(request.getParameter("state")).thenReturn(null);
        when(request.getParameter("redirectTo")).thenReturn("http://fakedomain.entando.org/entando-de-app/pages/en/homepage/");
        when(request.getContextPath()).thenReturn("/entando-de-app");

        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }

        verify(session,times(1)).setAttribute(KeycloakFilter.SESSION_PARAM_REDIRECT, "/pages/en/homepage/");

    }

    @Test
    void shouldLoginWithRedirectWithUrlExecuteFine() throws Exception {

        final String path = "/do/login.action";
        final String redirectPath = "/pages/en/homepage/";
        final String contextRoot = "/entando-de-app";
        final String protoAndServerName = "http://dev.entando.org";

        testLoginExecuteFine(contextRoot, path, protoAndServerName+contextRoot+redirectPath, redirectPath);

    }

    private void testLoginExecuteFine(final String contextRoot, final String path,
            final String redirectToUri, final String redirectToPath) throws Exception {
        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn(path);
        when(request.getParameter("code")).thenReturn(null);
        when(request.getParameter("state")).thenReturn(null);
        when(request.getParameter("redirectTo")).thenReturn(redirectToUri);
        when(request.getContextPath()).thenReturn(contextRoot);

        final String redirect = "http://dev.entando.org/auth/realms/entando/protocol/openid-connect/auth";
        when(oidcService.getRedirectUrl(any(), any(), any())).thenReturn(redirect);

        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }
        verify(session,times(1)).setAttribute(KeycloakFilter.SESSION_PARAM_REDIRECT, redirectToPath);
        verify(response).sendRedirect(redirect);

    }


    @Test
    void shouldLoginWithRedirectToWithPathExecuteFine() throws Exception {

        final String path = "/do/login.action";
        final String redirectPath = "/pages/en/homepage/";
        final String contextRoot = "/entando-de-app";

        testLoginExecuteFine(contextRoot, path, contextRoot+redirectPath, redirectPath);
    }

    @Test
    void shouldLoginWithRedirectToSameDomainWithDifferentSchemaExecuteFine() throws Exception {

        String path = "/do/login.action";
        when(configuration.isEnabled()).thenReturn(true);
        when(request.getServletPath()).thenReturn(path);
        when(request.getParameter("code")).thenReturn(null);
        when(request.getParameter("state")).thenReturn(null);
        when(request.getParameter("redirectTo")).thenReturn("https://dev.entando.org/entando-de-app/pages/en/mypage");
        when(request.getContextPath()).thenReturn("/entando-de-app");

        final String redirect = "http://dev.entando.org/auth/realms/entando/protocol/openid-connect/auth";
        when(oidcService.getRedirectUrl(any(), any(), any())).thenReturn(redirect);

        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakFilter.doFilter(request, response, filterChain);
        }
        verify(session,times(1)).setAttribute(KeycloakFilter.SESSION_PARAM_REDIRECT, "/pages/en/mypage");
        verify(response).sendRedirect(redirect);

    }

    static class ServletOutputStreamWrapper extends ServletOutputStream {
        private final StringWriter writer;
        private ServletOutputStreamWrapper(final StringWriter aWriter) {
            this.writer = aWriter;
        }

        @Override
        public void write(int aByte) {
            this.writer.write(aByte);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(final WriteListener writeListener) {}
    }

    private AbstractCharSequenceAssert assertJsonString(final String json, final String path) {
        return assertThat((String) JsonPath.read(json, path));
    }

    private AbstractBooleanAssert assertJsonBoolean(final String json, final String path) {
        return assertThat((Boolean) JsonPath.read(json, path));
    }

}
