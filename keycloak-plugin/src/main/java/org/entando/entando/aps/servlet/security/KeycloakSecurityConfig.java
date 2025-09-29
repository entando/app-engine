package org.entando.entando.aps.servlet.security;

import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.userprofile.api.ApiMyUserProfileInterface;
import org.entando.entando.ent.util.EntLogging;
import org.entando.entando.keycloak.services.KeycloakConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Order(70)
@Configuration
@EnableWebSecurity
public class KeycloakSecurityConfig extends AuthorizationServerConfiguration {

    private static final EntLogging.EntLogger _logger =  EntLogging.EntLogFactory.getSanitizedLogger(KeycloakSecurityConfig.class);

    public static final String API_PATH = "/api";

    private final KeycloakAuthenticationFilter keycloakAuthenticationFilter;
    private final KeycloakConfiguration configuration;

    @Autowired
    public KeycloakSecurityConfig(final KeycloakAuthenticationFilter keycloakAuthenticationFilter,
                                  final KeycloakConfiguration configuration) {
        this.keycloakAuthenticationFilter = keycloakAuthenticationFilter;
        this.configuration = configuration;
    }

    @Bean
    @Order(70)
    public SecurityFilterChain keycloakSecurityFilterChain(HttpSecurity http) throws Exception {
        _logger.debug("KeycloakSecurityConfig.keycloakSecurityFilterChain() called, Keycloak enabled: " + configuration.isEnabled());
        if (configuration.isEnabled()) {
            _logger.debug("Keycloak enabled, configuring security filter chain");

            if (StringUtils.isNotEmpty(configuration.getSecureUris())) {
                final String[] urls = configuration.getSecureUris().split(",");
                _logger.debug("Securing configured URIs: " + String.join(", ", urls));
                http.authorizeHttpRequests(authorize -> {
                    var requests = authorize;
                    for (String url : urls) {
                        if (StringUtils.isNotEmpty(url)) {
                            requests = requests.requestMatchers(new AntPathRequestMatcher(url)).authenticated();
                        }
                    }
                    requests.anyRequest().permitAll();
                });
            } else {
                // Default: secure all API endpoints
                _logger.debug("No secure URIs configured, securing all /api/** endpoints");
                http.authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(new AntPathRequestMatcher("/api/**")).authenticated()
                    .anyRequest().permitAll()
                );
            }

            http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                    .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                    .addFilterBefore(keycloakAuthenticationFilter, BasicAuthenticationFilter.class)
                    .anonymous(AbstractHttpConfigurer::disable)
                    .csrf(AbstractHttpConfigurer::disable) //NOSONAR
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()));
            return http.build();
        } else {
            return super.filterChain(http);
        }
    }

}
