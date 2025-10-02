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

            http.authorizeHttpRequests(authorize -> {
                authorize
                        .requestMatchers(new AntPathRequestMatcher("/api/health")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/v3/api-docs")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/v3/api-docs/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v3/api-docs")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v3/api-docs/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/swagger-ui.html")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/swagger-ui/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/swagger-ui.html")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/webjars/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/webjars/**")).permitAll();
                if (StringUtils.isNotEmpty(configuration.getSecureUris())) {
                    final String[] urls = configuration.getSecureUris().split(",");
                    _logger.debug("Securing configured URIs: " + String.join(", ", urls));
                    for (String url : urls) {
                        if (StringUtils.isNotEmpty(url)) {
                            authorize.requestMatchers(new AntPathRequestMatcher(url)).authenticated();
                        }
                    }
                }

                authorize.requestMatchers(new AntPathRequestMatcher("/api/**")).authenticated()
                        .anyRequest().permitAll();
            });

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
