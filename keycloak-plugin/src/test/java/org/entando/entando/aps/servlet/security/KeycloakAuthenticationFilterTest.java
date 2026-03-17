package org.entando.entando.aps.servlet.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.services.role.Permission;
import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.User;
import com.google.common.net.HttpHeaders;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.aps.util.UrlUtils;
import org.entando.entando.assertionHelper.KeycloakAuthenticationFilterAssertionHelper;
import org.entando.entando.keycloak.services.KeycloakAuthorizationManager;
import org.entando.entando.keycloak.services.KeycloakConfiguration;
import org.entando.entando.keycloak.services.oidc.OpenIDConnectService;
import org.entando.entando.keycloak.services.oidc.model.AccessToken;
import org.entando.entando.keycloak.services.oidc.model.TokenRoles;
import org.entando.entando.mockhelper.UserMockHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

@ExtendWith(MockitoExtension.class)
class KeycloakAuthenticationFilterTest {

    @Mock
    private KeycloakConfiguration configuration;
    @Mock
    private IUserManager userManager;
    @Mock
    private ITenantManager tenantManager;
    @Mock
    private OpenIDConnectService oidcService;
    @Mock
    private IAuthenticationProviderManager authenticationProviderManager;
    @Mock
    private KeycloakAuthorizationManager keycloakGroupManager;
    @Mock
    private AccessToken accessToken;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private ServletContext svCtx;
    @Mock
    private WebApplicationContext wac;
    @Mock
    private Map<String, TokenRoles> resourceAccess;
    @Mock
    private TokenRoles tokenRoles;

    @InjectMocks
    private KeycloakAuthenticationFilter keycloakAuthenticationFilter;

    @BeforeEach
    public void setUp() {
        Mockito.lenient().when(request.getSession()).thenReturn(session);
        Mockito.lenient().when(request.getHeader(UrlUtils.ENTANDO_TENANT_CODE_CUSTOM_HEADER)).thenReturn(null);
        Mockito.lenient().when(request.getHeader(HttpHeaders.X_FORWARDED_HOST)).thenReturn(null);
        Mockito.lenient().when(request.getHeader(HttpHeaders.HOST)).thenReturn("dev.entando.org");
        Mockito.lenient().when(request.getServerName()).thenReturn("dev.entando.org");
        Mockito.lenient().when(session.getServletContext()).thenReturn(svCtx);
        Mockito.lenient().when(wac.getBean(ITenantManager.class)).thenReturn(tenantManager);
    }

    @Test
    void attemptAuthenticationWithSuperuserPermissionShouldAddKeycloakAuthentication() throws Exception {

        List<String> expected = Collections.singletonList(Permission.SUPERUSER);

        // multiple permissions
        this.mockForAttemptAuthenticationTest();
        List<String> permissionList = Arrays.asList(Permission.ENTER_BACKEND, Permission.SUPERUSER);
        when(tokenRoles.getRoles()).thenReturn(permissionList);
        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            User actual = (User) keycloakAuthenticationFilter.attemptAuthentication(request, response).getPrincipal();
            KeycloakAuthenticationFilterAssertionHelper.assertKeycloakAuthorization(actual.getAuthorizations().get(0), expected);

            // single permission
            this.mockForAttemptAuthenticationTest();
            permissionList = Collections.singletonList(Permission.SUPERUSER);
            when(tokenRoles.getRoles()).thenReturn(permissionList);
            actual = (User) keycloakAuthenticationFilter.attemptAuthentication(request, response).getPrincipal();
            KeycloakAuthenticationFilterAssertionHelper.assertKeycloakAuthorization(actual.getAuthorizations().get(0), expected);
        }
    }

    @Test
    void attemptAuthenticationWithoutKeycloakPermissionShouldReturnEmptyAuthorizationList() throws Exception {

        this.mockForAttemptAuthenticationTest();

        List<String> permissionList = Collections.singletonList(Permission.ENTER_BACKEND);
        when(tokenRoles.getRoles()).thenReturn(permissionList);
        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            User actual = (User) keycloakAuthenticationFilter.attemptAuthentication(request, response).getPrincipal();
            assertEquals(0, actual.getAuthorizations().size());
        }
    }

    @Test
    void attemptAuthenticationWithEmptyOrNullPermissionListShouldReturnEmptyAuthorizationList() throws Exception {

        this.mockForAttemptAuthenticationTest();

        when(tokenRoles.getRoles()).thenReturn(null);
        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            User actual = (User) keycloakAuthenticationFilter.attemptAuthentication(request, response).getPrincipal();
            assertEquals(0, actual.getAuthorizations().size());
            when(tokenRoles.getRoles()).thenReturn(new ArrayList<>());
            actual = (User) keycloakAuthenticationFilter.attemptAuthentication(request, response).getPrincipal();
            assertEquals(0, actual.getAuthorizations().size());
        }
    }

    @Test
    void apiAuthenticationShouldSetAttributeRequest() {
        when(request.getServletPath()).thenReturn("/api");
        try ( MockedStatic<WebApplicationContextUtils> wacUtil = Mockito.mockStatic(WebApplicationContextUtils.class)) {
            wacUtil.when(() -> WebApplicationContextUtils.getWebApplicationContext(svCtx)).thenReturn(wac);
            keycloakAuthenticationFilter.attemptAuthentication(request, response);
            verify(request).setAttribute(eq("user"), any());
            verify(request, times(1)).getSession();
        }
    }

    private void mockForAttemptAuthenticationTest() throws Exception {

        when(request.getHeader("Authorization")).thenReturn("Bearer jwt");
        when(request.getSession()).thenReturn(session);
        doNothing().when(session).setAttribute(anyString(), any());
        when(oidcService.validateToken(anyString())).thenReturn(new ResponseEntity<>(accessToken, HttpStatus.OK));
        when(accessToken.isActive()).thenReturn(true);
        when(accessToken.getUsername()).thenReturn(UserMockHelper.USERNAME);
        when(authenticationProviderManager.getUser(anyString())).thenReturn(UserMockHelper.mockUser());
        when(accessToken.getResourceAccess()).thenReturn(resourceAccess);
        when(configuration.getClientId()).thenReturn("clientId");
        when(resourceAccess.get(anyString())).thenReturn(tokenRoles);
    }
}
