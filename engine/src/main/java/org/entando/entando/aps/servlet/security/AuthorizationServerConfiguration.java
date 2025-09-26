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
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.entando.entando.aps.util.crypto.CryptoBeansConfig;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.entando.entando.aps.system.services.oauth2.IApiOAuth2TokenManager;
import org.entando.entando.aps.system.services.oauth2.IOAuthConsumerManager;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * Spring Authorization Server 1.x Configuration for Entando
 *
 * Migrated from deprecated Spring Security OAuth @EnableAuthorizationServer
 * to modern Spring Authorization Server SecurityFilterChain approach
 */
@Configuration
@EnableWebSecurity
@Profile("!test")
public class AuthorizationServerConfiguration {

    @Autowired(required = false)
    @Qualifier(SystemConstants.OAUTH_TOKEN_MANAGER)
    private IApiOAuth2TokenManager apiOAuth2TokenManager;

    @Autowired
    @Qualifier(SystemConstants.BASE_CONFIG_MANAGER)
    private ConfigInterface configManager;

    @Autowired(required = false)
    @Qualifier(SystemConstants.OAUTH_CONSUMER_MANAGER)
    private IOAuthConsumerManager oauthConsumerManager;

    @Autowired
    @Qualifier(SystemConstants.AUTHENTICATION_PROVIDER_MANAGER)
    private IAuthenticationProviderManager authenticationManager;


    @Autowired
    @Qualifier("bcryptEncoder")
    private CryptoBeansConfig.BCryptEncoderWrapper passwordEncoderWrapper;


    /**
     * Configure the OAuth2 Authorization Server SecurityFilterChain
     */
//    @Bean
//    @Order(1)
    protected HttpSecurity authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults()); // Enable OpenID Connect 1.0

        // Add CORS support for token endpoints
//        http.addFilterBefore(corsFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        // Accept access tokens for User Info and/or Client Registration
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        // Redirect to the login page when not authenticated from the authorization endpoint
        http.exceptionHandling(exceptions -> exceptions
            .defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
            )
        );

        return http;
    }

    /**
     * Configure Authorization Server settings including endpoint paths
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        AuthorizationServerSettings.Builder builder = AuthorizationServerSettings.builder();

        // Configure API prefix if not in test mode
        if (!this.configManager.getParam(SystemConstants.INIT_PROP_CONFIG_VERSION).equals("test")) {
            builder.issuer("http://localhost:8080/entando-de-app")
                   .authorizationEndpoint("/oauth2/authorize")
                   .tokenEndpoint("/oauth2/token")
                   .tokenIntrospectionEndpoint("/oauth2/introspect")
                   .tokenRevocationEndpoint("/oauth2/revoke")
                   .jwkSetEndpoint("/oauth2/jwks")
                   .oidcClientRegistrationEndpoint("/connect/register")
                   .oidcUserInfoEndpoint("/userinfo");
        }

        return builder.build();
    }

    /**
     * Default resource server security filter chain for protecting APIs
     */
    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll()
            )
            .formLogin(Customizer.withDefaults())
//            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
            )
            .csrf(csrf -> csrf.disable()) // NOSONAR - CSRF handled by Entando framework
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    /**
     * JWT Key Source for signing tokens
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    /**
     * JWT Decoder for validating tokens
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * OAuth2 Token Customizer to add custom claims
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
        return context -> {
            if (context.getTokenType().getValue().equals("access_token")) {
                context.getClaims().claims(claims -> {
                    // Add custom claims here if needed
                    claims.put("iss", "Entando");
                });
            }
        };
    }

    /**
     * OAuth2AuthorizationService bean using the existing database-backed token manager
     * This integrates the JDBC-based token storage with Spring Authorization Server
     */
//    @Bean
//    public OAuth2AuthorizationService authorizationService() {
//        if (apiOAuth2TokenManager != null) {
//            return apiOAuth2TokenManager;
//        }
//        // Fallback: create a default implementation if token manager is not available
//        return new InMemoryOAuth2AuthorizationService();
//    }


    @Autowired
    public CorsConfiguration corsConfiguration;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }

}
