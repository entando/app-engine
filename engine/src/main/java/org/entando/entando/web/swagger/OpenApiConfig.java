package org.entando.entando.web.swagger;

import com.agiletec.aps.system.SystemConstants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class OpenApiConfig {

    private static final String OAUTH2_SECURITY_SCHEME_NAME = "oauth2";
    private static final String BASIC_AUTH_SECURITY_SCHEME_NAME = "basicAuth";

    @Autowired
    ServletContext servletContext; // package-private for testing

    @Autowired
    Environment environment; // package-private for testing

    @Value("${keycloak.enabled:false}")
    boolean keycloakEnabled; // package-private for testing

    private String getAuthServer() {
        String baseAuthServer = environment.getProperty(SystemConstants.SYSTEM_PROP_KEYCLOAK_AUTH_URL);
        String kcRealm = environment.getProperty(SystemConstants.SYSTEM_PROP_KEYCLOAK_REALM);

        if (baseAuthServer != null && kcRealm != null) {
            return String.format("%s/realms/%s/protocol/openid-connect", baseAuthServer, kcRealm);
        }
        return baseAuthServer;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("Entando API")
                        .version("7.5.0")
                        .description("Entando App Engine API Documentation"));

        // Add server with context path
        String contextPath = servletContext.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()) {
            openAPI.addServersItem(new Server().url(contextPath));
        }

        if (keycloakEnabled) {
            // Add OAuth2 security scheme when Keycloak is enabled
            configureOAuth2Security(openAPI);
        } else {
            // Add Basic Auth security scheme when Keycloak is disabled
            configureBasicAuthSecurity(openAPI);
        }

        return openAPI;
    }

    private void configureOAuth2Security(OpenAPI openAPI) {
        String authServer = getAuthServer();
        if (authServer != null && !authServer.isEmpty()) {
            OAuthFlow authorizationCodeFlow = new OAuthFlow()
                    .authorizationUrl(authServer + "/auth")
                    .tokenUrl(authServer + "/token")
                    .refreshUrl(authServer + "/token");

            // Add logout URL as extension (for documentation)
            authorizationCodeFlow.addExtension("x-logout-url", authServer + "/logout");

            openAPI.components(new Components()
                    .addSecuritySchemes(OAUTH2_SECURITY_SCHEME_NAME, new SecurityScheme()
                            .type(SecurityScheme.Type.OAUTH2)
                            .flows(new OAuthFlows()
                                    .authorizationCode(authorizationCodeFlow))));

            // Apply security globally
            openAPI.addSecurityItem(new SecurityRequirement().addList(OAUTH2_SECURITY_SCHEME_NAME));
        }
    }

    private void configureBasicAuthSecurity(OpenAPI openAPI) {
        openAPI.components(new Components()
                .addSecuritySchemes(BASIC_AUTH_SECURITY_SCHEME_NAME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("basic")
                        .description("Basic Authentication - Use your Entando username and password")));

        // Apply security globally
        openAPI.addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH_SECURITY_SCHEME_NAME));
    }
}