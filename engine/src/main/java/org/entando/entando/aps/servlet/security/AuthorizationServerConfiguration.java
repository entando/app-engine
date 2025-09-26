/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.aps.servlet.security;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Spring Authorization Server 1.x Configuration for Entando
 *
 * Migrated from deprecated Spring Security OAuth @EnableAuthorizationServer
 * to modern Spring Authorization Server SecurityFilterChain approach
 */
@Configuration
@EnableWebSecurity
public class AuthorizationServerConfiguration {

    @Autowired
    @Qualifier(SystemConstants.OAUTH_TOKEN_MANAGER)
    private OAuth2AuthorizationService authorizationService;

    @Autowired
    @Qualifier(SystemConstants.BASE_CONFIG_MANAGER)
    private ConfigInterface configManager;

    @Autowired
    @Qualifier(SystemConstants.OAUTH_CONSUMER_MANAGER)
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    @Qualifier(SystemConstants.AUTHENTICATION_PROVIDER_MANAGER)
    private IAuthenticationProviderManager authenticationManager;

    @Autowired
    private CorsFilter corsFilter;

    /**
     * Configure the OAuth2 Authorization Server SecurityFilterChain
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        // Apply the default authorization server configuration
        http.apply(authorizationServerConfigurer);

        // Add CORS support for token endpoints
        http.addFilterBefore(corsFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        // Configure OAuth2 endpoints path prefix if not in test mode
        if (!this.configManager.getParam(SystemConstants.INIT_PROP_CONFIG_VERSION).equals("test")) {
            // Note: In Spring Authorization Server 1.x, endpoint paths are configured via AuthorizationServerSettings
            // The prefix configuration is handled by the authorizationServerSettings bean
        }

        return http.build();
    }

    /**
     * Configure Authorization Server settings including endpoint paths
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        AuthorizationServerSettings.Builder builder = AuthorizationServerSettings.builder();

        // Configure API prefix if not in test mode
        if (!this.configManager.getParam(SystemConstants.INIT_PROP_CONFIG_VERSION).equals("test")) {
            builder.issuer("http://localhost:8080/api")
                   .authorizationEndpoint("/api/oauth2/authorize")
                   .tokenEndpoint("/api/oauth2/token")
                   .tokenIntrospectionEndpoint("/api/oauth2/introspect")
                   .tokenRevocationEndpoint("/api/oauth2/revoke");
        }

        return builder.build();
    }


}
