package org.entando.entando.aps.servlet.security;

import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import com.agiletec.aps.system.services.user.IUserManager;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.ent.util.EntLogging;
import org.entando.entando.keycloak.services.KeycloakConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
    private final IUserManager userManager;
    private final IAuthenticationProviderManager authenticationProviderManager;

    @Autowired
    public KeycloakSecurityConfig(final KeycloakAuthenticationFilter keycloakAuthenticationFilter,
                                  final KeycloakConfiguration configuration,
                                  final IUserManager userManager,
                                  final IAuthenticationProviderManager authenticationProviderManager) {
        this.keycloakAuthenticationFilter = keycloakAuthenticationFilter;
        this.configuration = configuration;
        this.userManager = userManager;
        this.authenticationProviderManager = authenticationProviderManager;
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
                        .requestMatchers(new AntPathRequestMatcher("/api/v3/api-docs")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v3/api-docs/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/swagger-ui/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/swagger-ui.html")).permitAll()
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
            // Keycloak disabled - use BasicAuthFilter for DB authentication
            _logger.warn("▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒");
            _logger.warn("▒▒▒ [SECURITY] Keycloak disabled; using Basic Auth. UNSAFE FOR PRODUCTION. ▒▒▒");
            _logger.warn("▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒▒");

            BasicAuthFilter basicAuthFilter = new BasicAuthFilter(userManager, authenticationProviderManager);

            http.authorizeHttpRequests(authorize -> {
                authorize
                        .requestMatchers(new AntPathRequestMatcher("/api/health")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v3/api-docs")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/v3/api-docs/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/swagger-ui/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/swagger-ui.html")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/webjars/**")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/**")).authenticated()
                        .anyRequest().permitAll();
            });

            http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                    .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                    .addFilterBefore(basicAuthFilter, BasicAuthenticationFilter.class)
                    .anonymous(AbstractHttpConfigurer::disable)
                    .csrf(AbstractHttpConfigurer::disable) //NOSONAR
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()));

            return http.build();
        }
    }

}
