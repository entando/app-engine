package org.entando.entando.web.legacyapi;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.role.Role;
import com.agiletec.aps.system.services.url.IURLManager;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.User;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.entando.entando.aps.system.services.api.DefaultJsonTypesProvider;
import org.entando.entando.aps.system.services.api.IApiErrorCodes;
import org.entando.entando.aps.system.services.api.LegacyApiUnmarshaller;
import org.entando.entando.aps.system.services.api.ObjectMapperConfiguration;
import org.entando.entando.aps.system.services.api.model.ApiException;
import org.entando.entando.aps.system.services.api.model.ApiMethod;
import org.entando.entando.aps.system.services.api.model.LegacyApiError;
import org.entando.entando.aps.system.services.api.model.StringApiResponse;
import org.entando.entando.aps.system.services.api.server.IResponseBuilder;
import org.entando.entando.aps.system.services.oauth2.IApiOAuth2TokenManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(MockitoExtension.class)
class ApiRestServerTest {

    @Mock
    private IAuthorizationManager authManager;
    @Mock
    private IUserManager userManager;
    @Mock
    private IApiOAuth2TokenManager tokenManager;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession session;
    @Mock
    private ServletContext servletContext;
    @Mock
    private WebApplicationContext webApplicationContext;
    @Mock
    private IResponseBuilder responseBuilder;
    @Mock
    private ILangManager langManager;
    @Mock
    private IURLManager urlManager;

    private ApiRestServer apiRestServer;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(request.getSession()).thenReturn(session);
        Mockito.lenient().when(session.getServletContext()).thenReturn(servletContext);
        Mockito.lenient().when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(webApplicationContext);

        Mockito.lenient().when(webApplicationContext.getBean(SystemConstants.AUTHORIZATION_SERVICE))
                .thenReturn(authManager);

        apiRestServer = new ApiRestServer(getUnmarshaller());
    }

    private LegacyApiUnmarshaller getUnmarshaller() {
        ObjectMapperConfiguration mapperConfiguration = new ObjectMapperConfiguration();
        mapperConfiguration.setJsonTypesProviders(List.of(new DefaultJsonTypesProvider()));
        LegacyApiUnmarshaller unmarshaller = new LegacyApiUnmarshaller(
                mapperConfiguration.defaultObjectMapper(), mapperConfiguration.xmlMapper());
        return unmarshaller;
    }

    @Test
    void extractOAuthParameters_adminOnRestrictedEndpoint_shouldAllowAccess() throws Exception {

        ApiMethod apiMethod = Mockito.mock(ApiMethod.class);
        Mockito.when(apiMethod.getRequiredPermission()).thenReturn("viewUsers");

        User user = new User();
        user.setUsername("admin");

        Group group = new Group();
        group.setName("administrators");
        Role role = new Role();
        role.addPermission("superuser");

        Mockito.when(authManager.isAuthOnPermission(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(true);
        Mockito.when(request.getAttribute("user")).thenReturn(user);

        Assertions.assertDoesNotThrow(() -> {
            apiRestServer.extractOAuthParameters(request, apiMethod, new Properties());
        });
    }

    @Test
    void extractOAuthParameters_nullUserOnRestrictedEndpoint_shouldDenyAccess() throws Exception {

        ApiMethod apiMethod = Mockito.mock(ApiMethod.class);
        Mockito.when(apiMethod.getRequiredPermission()).thenReturn("viewUsers");
        Mockito.when(apiMethod.getRequiredAuth()).thenReturn(true);

        ApiException exception = Assertions.assertThrows(ApiException.class, () -> {
            apiRestServer.extractOAuthParameters(request, apiMethod, new Properties());
        });
        Assertions.assertEquals(IApiErrorCodes.API_AUTHENTICATION_REQUIRED, exception.getErrors().get(0).getCode());
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getErrors().get(0).getStatus());
    }

    @Test
    void extractOAuthParameters_guestOnRestrictedEndpoint_shouldDenyAccess() throws Exception {

        ApiMethod apiMethod = Mockito.mock(ApiMethod.class);
        Mockito.when(apiMethod.getRequiredPermission()).thenReturn("viewUsers");
        Mockito.when(apiMethod.getRequiredAuth()).thenReturn(true);

        User user = new User();
        user.setUsername(SystemConstants.GUEST_USER_NAME);
        Mockito.when(request.getAttribute("user")).thenReturn(user);

        ApiException exception = Assertions.assertThrows(ApiException.class, () -> {
            apiRestServer.extractOAuthParameters(request, apiMethod, new Properties());
        });
        Assertions.assertEquals(IApiErrorCodes.API_AUTHENTICATION_REQUIRED, exception.getErrors().get(0).getCode());
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getErrors().get(0).getStatus());
    }

    @Test
    void extractOAuthParameters_guestOnFreeEndpoint_shouldAllowAccess() throws Exception {

        ApiMethod apiMethod = Mockito.mock(ApiMethod.class);

        Assertions.assertDoesNotThrow(() -> {
            apiRestServer.extractOAuthParameters(request, apiMethod, new Properties());
        });
    }

    @Test
    void extractOAuthParameters_expiredTokenOnRestrictedEndpoint_shouldDenyAccess() throws Exception {

        ApiMethod apiMethod = Mockito.mock(ApiMethod.class);
        Mockito.when(apiMethod.getRequiredPermission()).thenReturn("viewUsers");
        Mockito.when(apiMethod.getRequiredAuth()).thenReturn(true);

        ApiException exception = Assertions.assertThrows(ApiException.class, () -> {
            apiRestServer.extractOAuthParameters(request, apiMethod, new Properties());
        });
        Assertions.assertEquals(IApiErrorCodes.API_AUTHENTICATION_REQUIRED, exception.getErrors().get(0).getCode());
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED, exception.getErrors().get(0).getStatus());
    }

    @Test
    void extractOAuthParameters_unauthorizedUserOnRestrictedEndpoint_shouldDenyAccess() throws Exception {

        ApiMethod apiMethod = Mockito.mock(ApiMethod.class);
        Mockito.when(apiMethod.getRequiredPermission()).thenReturn("simple-user");

        User user = new User();
        user.setUsername("simple-user");

        Group group = new Group();
        group.setName("free");
        Role role = new Role();
        role.addPermission("viewUsers");

        Mockito.when(authManager.getUserRoles(user)).thenReturn(Collections.singletonList(role));

        Mockito.when(request.getAttribute("user")).thenReturn(user);

        ApiException exception = Assertions.assertThrows(ApiException.class, () -> {
            apiRestServer.extractOAuthParameters(request, apiMethod, new Properties());
        });
        Assertions.assertEquals(IApiErrorCodes.API_AUTHORIZATION_REQUIRED, exception.getErrors().get(0).getCode());
        Assertions.assertEquals(HttpStatus.FORBIDDEN, exception.getErrors().get(0).getStatus());
    }

    @Test
    void shouldHandleInvalidApiRequest() throws Exception {
        mockLanguageManager();
        mockUrlManager();
        Mockito.when(webApplicationContext.getBean(SystemConstants.API_RESPONSE_BUILDER)).thenReturn(responseBuilder);
        Mockito.when(request.getParameterNames()).thenReturn(Collections.emptyEnumeration());
        Mockito.when(request.getPathInfo()).thenReturn(".json");

        LegacyApiError error = new LegacyApiError(IApiErrorCodes.API_INVALID_RESPONSE, "Invalid", HttpStatus.SERVICE_UNAVAILABLE);
        Mockito.doThrow(new ApiException(error)).when(responseBuilder)
                .extractApiMethod(Mockito.any(), Mockito.any(), Mockito.any());

        Object response = apiRestServer.doDeleteJson("en", "invalidResource", request);

        Assertions.assertEquals(ResponseEntity.class, response.getClass());
        ResponseEntity<StringApiResponse> responseEntity = (ResponseEntity<StringApiResponse>) response;
        Assertions.assertEquals(HttpStatus.SERVICE_UNAVAILABLE, responseEntity.getStatusCode());
        Assertions.assertEquals("FAILURE", responseEntity.getBody().getResult());
    }

    @Test
    void shouldHandleUnexpectedError() throws Exception {
        mockLanguageManager();
        mockUrlManager();
        Mockito.when(webApplicationContext.getBean(SystemConstants.API_RESPONSE_BUILDER)).thenReturn(responseBuilder);
        Mockito.when(request.getParameterNames()).thenReturn(Collections.emptyEnumeration());
        Mockito.when(request.getPathInfo()).thenReturn(".json");

        Mockito.doThrow(RuntimeException.class).when(responseBuilder)
                .extractApiMethod(Mockito.any(), Mockito.any(), Mockito.any());

        Object response = apiRestServer.doDeleteJson("en", "core", "errorTest", request);

        Assertions.assertEquals(ResponseEntity.class, response.getClass());
        ResponseEntity<StringApiResponse> responseEntity = (ResponseEntity<StringApiResponse>) response;
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        Assertions.assertEquals("FAILURE", responseEntity.getBody().getResult());
    }

    @Test
    void shouldDetectXmlMediaTypeFromAcceptHeader() {
        Mockito.when(request.getPathInfo()).thenReturn("/legacy/en/core/guiFragments");
        Mockito.when(request.getHeader("Accept")).thenReturn("application/xml");
        Assertions.assertEquals(MediaType.APPLICATION_XML, apiRestServer.extractProducesMediaType(request));
    }

    @Test
    void shouldDetectJsonMediaTypeFromAcceptHeader() {
        Mockito.when(request.getPathInfo()).thenReturn("/legacy/en/core/guiFragments");
        Mockito.when(request.getHeader("Accept")).thenReturn("application/json");
        Assertions.assertEquals(MediaType.APPLICATION_JSON, apiRestServer.extractProducesMediaType(request));
    }

    @Test
    void shouldUseJsonMediaTypeIfAcceptHeaderIsNotDefined() {
        Mockito.when(request.getPathInfo()).thenReturn("/legacy/en/core/guiFragments");
        Assertions.assertEquals(MediaType.APPLICATION_JSON, apiRestServer.extractProducesMediaType(request));
    }

    @Test
    void shouldDetectJsonMediaTypeFromPath() {
        Mockito.when(request.getPathInfo()).thenReturn("/legacy/en/core/guiFragments.json");
        Assertions.assertEquals(MediaType.APPLICATION_JSON, apiRestServer.extractProducesMediaType(request));
    }

    @Test
    void shouldDetectXmlMediaTypeFromPath() {
        Mockito.when(request.getPathInfo()).thenReturn("/legacy/en/core/guiFragments.xml");
        Assertions.assertEquals(MediaType.APPLICATION_XML, apiRestServer.extractProducesMediaType(request));
    }

    private void mockLanguageManager() {
        Mockito.when(webApplicationContext.getBean(SystemConstants.LANGUAGE_MANAGER)).thenReturn(langManager);
        Lang defaultLang = new Lang();
        defaultLang.setCode("en");
        Mockito.when(langManager.getDefaultLang()).thenReturn(defaultLang);
    }

    private void mockUrlManager() throws Exception {
        Mockito.when(webApplicationContext.getBean(SystemConstants.URL_MANAGER)).thenReturn(urlManager);
        Mockito.when(urlManager.getApplicationBaseURL(Mockito.any())).thenReturn("http://localhost:8080");
    }
}
