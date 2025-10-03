package org.entando.entando.web.swagger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.SystemConstants;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class OpenApiConfigTest {

    private final String authUrl = "http://localhost:9999/auth";
    private final String realm = "entandoRealm";

    @Mock
    private Environment environment;

    @Mock
    private ServletContext servletContext;

    @BeforeEach
    public void setup() {
        when(environment.getProperty(SystemConstants.SYSTEM_PROP_KEYCLOAK_AUTH_URL)).thenReturn(authUrl);
        when(environment.getProperty(SystemConstants.SYSTEM_PROP_KEYCLOAK_REALM)).thenReturn(realm);
        Mockito.lenient().when(servletContext.getContextPath()).thenReturn("/entando-de-app");
    }

    @Test
    void createSpringdocConfig() {
        OpenApiConfig config = new OpenApiConfig();
        config.environment = environment;
        config.servletContext = servletContext;

        OpenAPI openAPI = config.customOpenAPI();
        assertNotNull(openAPI);
    }

    @Test
    void customOpenAPITest() {
        OpenApiConfig config = new OpenApiConfig();
        config.environment = environment;
        config.servletContext = servletContext;

        OpenAPI openAPI = config.customOpenAPI();

        assertNotNull(openAPI);
        Info info = openAPI.getInfo();
        assertNotNull(info);
        assertEquals("Entando API", info.getTitle());
        assertEquals("7.5.0", info.getVersion());
        assertEquals("Entando App Engine API Documentation", info.getDescription());

        // Check server configuration
        assertNotNull(openAPI.getServers());
        assertEquals(1, openAPI.getServers().size());
        assertEquals("/entando-de-app", openAPI.getServers().get(0).getUrl());

        // Check security configuration with Keycloak enabled
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());
        SecurityScheme securityScheme = openAPI.getComponents().getSecuritySchemes().get("entando");
        assertNotNull(securityScheme);
        assertEquals(SecurityScheme.Type.OAUTH2, securityScheme.getType());
        assertNotNull(securityScheme.getFlows());
        assertNotNull(securityScheme.getFlows().getAuthorizationCode());
        assertTrue(securityScheme.getFlows().getAuthorizationCode().getAuthorizationUrl()
                .contains("/realms/" + realm + "/protocol/openid-connect/auth"));
        assertTrue(securityScheme.getFlows().getAuthorizationCode().getTokenUrl()
                .contains("/realms/" + realm + "/protocol/openid-connect/token"));

        // Check security requirement
        assertNotNull(openAPI.getSecurity());
        assertEquals(1, openAPI.getSecurity().size());
        SecurityRequirement securityRequirement = openAPI.getSecurity().get(0);
        assertTrue(securityRequirement.containsKey("entando"));
    }

    @Test
    void customOpenAPIWithoutAuthServer() {
        when(environment.getProperty(SystemConstants.SYSTEM_PROP_KEYCLOAK_AUTH_URL)).thenReturn(null);
        when(servletContext.getContextPath()).thenReturn("/entando-de-app");

        OpenApiConfig config = new OpenApiConfig();
        config.environment = environment;
        config.servletContext = servletContext;

        OpenAPI openAPI = config.customOpenAPI();

        assertNotNull(openAPI);

        // Security should not be configured when auth server is null
        if (openAPI.getComponents() != null) {
            assertNull(openAPI.getComponents().getSecuritySchemes());
        }
        assertNull(openAPI.getSecurity());
    }

    @Test
    void customOpenAPIWithEmptyContextPath() {
        when(servletContext.getContextPath()).thenReturn("");

        OpenApiConfig config = new OpenApiConfig();
        config.environment = environment;
        config.servletContext = servletContext;

        OpenAPI openAPI = config.customOpenAPI();

        assertNotNull(openAPI);
        // Servers should be empty when context path is empty
        assertTrue(openAPI.getServers() == null || openAPI.getServers().isEmpty());
    }
}