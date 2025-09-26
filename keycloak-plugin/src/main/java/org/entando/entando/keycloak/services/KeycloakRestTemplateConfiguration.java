/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.keycloak.services;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.LaxRedirectStrategy;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class KeycloakRestTemplateConfiguration {

    @Value("${MAX_CONN_PER_ROUTE:32}")
    private int maxConnPerRoute;
    @Value("${MAX_CONN_TOTAL:64}")
    private int maxConnTotal;
    @Value("${CONN_REQ_TIMEOUT_MS:5000}")
    private int connReqTimeout;
    @Value("${CONN_TIMEOUT_MS:5000}")
    private int connTimeout;
    @Value("${CONN_SOCKET_TIMEOUT_MS:25000}")
    private int connSocketTimeout;

    @Bean(name="keycloakRestTemplate")
    public RestTemplate keycloakRestTemplate() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(Duration.ofMillis(connReqTimeout)))
                .setConnectTimeout(Timeout.of(Duration.ofMillis(connTimeout)))
                .setResponseTimeout(Timeout.of(Duration.ofMillis(connSocketTimeout)))
                .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxConnTotal);
        connectionManager.setDefaultMaxPerRoute(maxConnPerRoute);

        HttpClientBuilder builder = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig);

        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setHttpClient(builder.build());

        return new RestTemplate(httpRequestFactory);
    }

    @Bean(name="keycloakRestTemplateWithRedirect")
    public RestTemplate keycloakRestTemplateWithRedirect() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(Duration.ofMillis(connReqTimeout)))
                .setConnectTimeout(Timeout.of(Duration.ofMillis(connTimeout)))
                .setResponseTimeout(Timeout.of(Duration.ofMillis(connSocketTimeout)))
                .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxConnTotal);
        connectionManager.setDefaultMaxPerRoute(maxConnPerRoute);

        HttpClientBuilder builder = HttpClients.custom()
                .setRedirectStrategy(new LaxRedirectStrategy())
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig);

        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setHttpClient(builder.build());

        return new RestTemplate(httpRequestFactory);
    }

}
