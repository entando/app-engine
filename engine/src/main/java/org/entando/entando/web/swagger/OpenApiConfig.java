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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "entando";

    @Autowired
    ServletContext servletContext; // package-private for testing

    @Autowired
    Environment environment; // package-private for testing

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

        // Add OAuth2 security scheme if auth server is configured
        String authServer = getAuthServer();
        if (authServer != null && !authServer.isEmpty()) {
            OAuthFlow authorizationCodeFlow = new OAuthFlow()
                    .authorizationUrl(authServer + "/auth")
                    .tokenUrl(authServer + "/token")
                    .refreshUrl(authServer + "/token");

            // Add logout URL as extension (for documentation)
            authorizationCodeFlow.addExtension("x-logout-url", authServer + "/logout");

            openAPI.components(new Components()
                    .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                            .type(SecurityScheme.Type.OAUTH2)
                            .flows(new OAuthFlows()
                                    .authorizationCode(authorizationCodeFlow))));

            // Apply security globally
            openAPI.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
        }

        return openAPI;
    }
}