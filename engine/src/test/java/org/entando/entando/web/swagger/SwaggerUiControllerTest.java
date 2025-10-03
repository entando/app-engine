package org.entando.entando.web.swagger;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for SwaggerUiController
 */
class SwaggerUiControllerTest {

    private SwaggerUiController controller;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new SwaggerUiController();
    }

    @Test
    void testSwaggerUiReturnsHtml() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/app");

        String html = controller.swaggerUi(request);

        assertNotNull(html);
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("</html>"));
    }

    @Test
    void testSwaggerUiContainsTitle() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/app");

        String html = controller.swaggerUi(request);

        assertTrue(html.contains("<title>Entando API Documentation</title>"));
    }

    @Test
    void testSwaggerUiIncludesSwaggerResources() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/app");

        String html = controller.swaggerUi(request);

        assertTrue(html.contains("swagger-ui.css"));
        assertTrue(html.contains("swagger-ui-bundle.js"));
        assertTrue(html.contains("swagger-ui-standalone-preset.js"));
    }

    @Test
    void testContextPathIsUsedCorrectly() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/myapp");

        String html = controller.swaggerUi(request);

        assertTrue(html.contains("/myapp/api/webjars/swagger-ui"));
        assertTrue(html.contains("/myapp/api/v3/api-docs"));
    }

    @Test
    void testOAuth2Configuration() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/app");

        String html = controller.swaggerUi(request);

        assertTrue(html.contains("initOAuth"));
        assertTrue(html.contains("clientId: 'swagger'"));
        assertTrue(html.contains("clientSecret: 'swaggerswagger'"));
        assertTrue(html.contains("scopes: 'openid'"));
    }

    @Test
    void testOAuth2RedirectUrl() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/app");

        String html = controller.swaggerUi(request);

        assertTrue(html.contains("oauth2RedirectUrl:"));
        assertTrue(html.contains("oauth2-redirect.html"));
    }

    @Test
    void testTokenRefreshPluginIncluded() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/app");

        String html = controller.swaggerUi(request);

        assertTrue(html.contains("OAuthLogoutPlugin"));
        assertTrue(html.contains("refreshAccessToken"));
        assertTrue(html.contains("scheduleTokenRefresh"));
    }

    @Test
    void testTokenRefreshUsesClientCredentials() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/app");

        String html = controller.swaggerUi(request);

        assertTrue(html.contains("var clientId = entandoAuth.get('clientId')"));
        assertTrue(html.contains("var clientSecret = entandoAuth.get('clientSecret')"));
        assertTrue(html.contains("'client_id': clientId"));
    }

    @Test
    void testLogoutPluginIncluded() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/app");

        String html = controller.swaggerUi(request);

        assertTrue(html.contains("logout:"));
        assertTrue(html.contains("lastAuthUrl"));
        assertTrue(html.contains("logoutUrl"));
    }

    @Test
    void testSwaggerUiConfigOptions() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/app");

        String html = controller.swaggerUi(request);

        assertTrue(html.contains("deepLinking: true"));
        assertTrue(html.contains("tagsSorter: 'alpha'"));
        assertTrue(html.contains("operationsSorter: 'alpha'"));
        assertTrue(html.contains("docExpansion: 'none'"));
    }

    @Test
    void testHttpsScheme() {
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("example.com");
        when(request.getServerPort()).thenReturn(443);
        when(request.getContextPath()).thenReturn("/app");

        String html = controller.swaggerUi(request);

        assertTrue(html.contains("https://example.com/app"));
        assertFalse(html.contains(":443")); // Standard HTTPS port should be omitted
    }

    @Test
    void testNonStandardPort() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(9090);
        when(request.getContextPath()).thenReturn("/app");

        String html = controller.swaggerUi(request);

        assertTrue(html.contains(":9090"));
    }

    @Test
    void testStandardHttpPort() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(80);
        when(request.getContextPath()).thenReturn("/app");

        String html = controller.swaggerUi(request);

        assertFalse(html.contains(":80")); // Standard HTTP port should be omitted
    }

    @Test
    void testApiDocsUrl() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/app");

        String html = controller.swaggerUi(request);

        assertTrue(html.contains("url: specUrl"));
        assertTrue(html.contains("/app/api/v3/api-docs"));
    }

    @Test
    void testSwaggerUiBundleConfiguration() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/app");

        String html = controller.swaggerUi(request);

        assertTrue(html.contains("SwaggerUIBundle({"));
        assertTrue(html.contains("dom_id: '#swagger-ui'"));
        assertTrue(html.contains("layout: \"StandaloneLayout\""));
    }

    @Test
    void testPluginsRegistration() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/app");

        String html = controller.swaggerUi(request);

        assertTrue(html.contains("plugins: ["));
        assertTrue(html.contains("SwaggerUIBundle.plugins.DownloadUrl"));
        assertTrue(html.contains("OAuthLogoutPlugin"));
    }

    @Test
    void testEmptyContextPath() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("");

        String html = controller.swaggerUi(request);

        assertNotNull(html);
        assertTrue(html.contains("/api/v3/api-docs"));
        assertTrue(html.contains("/api/swagger-ui.html"));
    }

    @Test
    void testTokenRefreshLogging() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(request.getContextPath()).thenReturn("/app");

        String html = controller.swaggerUi(request);

        assertTrue(html.contains("console.log('[Token Refresh]"));
        assertTrue(html.contains("Using client_id:"));
        assertTrue(html.contains("Attempting to refresh token"));
    }
}