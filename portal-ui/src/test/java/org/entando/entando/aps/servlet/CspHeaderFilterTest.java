/*
 * Copyright 2026-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.servlet;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CspHeaderFilterTest {

    private static final String POLICY = "default-src 'self'; object-src 'none'";

    @Mock
    private ConfigInterface configManager;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private CspHeaderFilter filter;

    @BeforeEach
    void setUp() {
        this.filter = new CspHeaderFilter() {
            @Override
            protected ConfigInterface getConfigManager(HttpServletRequest request) {
                return configManager;
            }
        };
    }

    @Test
    void shouldSetEnforcedHeaderWhenCspIsEnabled() throws Exception {
        Mockito.when(configManager.getParam(SystemConstants.PAR_CSP_ENABLED)).thenReturn("true");
        Mockito.when(configManager.getParam(SystemConstants.PAR_CSP_BACKOFFICE_CONFIG)).thenReturn(POLICY);
        Mockito.when(configManager.getParam(SystemConstants.PAR_CSP_BACKOFFICE_REPORT_ONLY)).thenReturn("false");
        filter.doFilter(request, response, filterChain);
        Mockito.verify(response).setHeader(CspHeaderFilter.CSP_HEADER_NAME, POLICY);
        Mockito.verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSetReportOnlyHeaderWhenConfigured() throws Exception {
        Mockito.when(configManager.getParam(SystemConstants.PAR_CSP_ENABLED)).thenReturn("true");
        Mockito.when(configManager.getParam(SystemConstants.PAR_CSP_BACKOFFICE_CONFIG)).thenReturn(POLICY);
        Mockito.when(configManager.getParam(SystemConstants.PAR_CSP_BACKOFFICE_REPORT_ONLY)).thenReturn("true");
        filter.doFilter(request, response, filterChain);
        Mockito.verify(response).setHeader(CspHeaderFilter.CSP_REPORT_ONLY_HEADER_NAME, POLICY);
        Mockito.verify(response, never()).setHeader(eq(CspHeaderFilter.CSP_HEADER_NAME), anyString());
        Mockito.verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetHeaderWhenCspIsDisabled() throws Exception {
        Mockito.when(configManager.getParam(SystemConstants.PAR_CSP_ENABLED)).thenReturn("false");
        filter.doFilter(request, response, filterChain);
        Mockito.verify(response, never()).setHeader(anyString(), anyString());
        Mockito.verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotSetHeaderWhenPolicyIsBlank() throws Exception {
        Mockito.when(configManager.getParam(SystemConstants.PAR_CSP_ENABLED)).thenReturn("true");
        Mockito.when(configManager.getParam(SystemConstants.PAR_CSP_BACKOFFICE_CONFIG)).thenReturn(" ");
        filter.doFilter(request, response, filterChain);
        Mockito.verify(response, never()).setHeader(anyString(), anyString());
        Mockito.verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldReplaceNoncePlaceholderAndExposeRequestAttribute() throws Exception {
        Mockito.when(configManager.getParam(SystemConstants.PAR_CSP_ENABLED)).thenReturn("true");
        Mockito.when(configManager.getParam(SystemConstants.PAR_CSP_BACKOFFICE_CONFIG))
                .thenReturn("script-src 'nonce-{nonce}'; object-src 'none'");
        Mockito.when(configManager.getParam(SystemConstants.PAR_CSP_BACKOFFICE_REPORT_ONLY)).thenReturn("false");
        filter.doFilter(request, response, filterChain);
        ArgumentCaptor<String> nonceCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(request).setAttribute(eq(CspHeaderFilter.REQUEST_ATTR_CSP_NONCE), nonceCaptor.capture());
        String nonce = nonceCaptor.getValue();
        Assertions.assertEquals(64, nonce.length());
        Mockito.verify(response).setHeader(CspHeaderFilter.CSP_HEADER_NAME,
                "script-src 'nonce-" + nonce + "'; object-src 'none'");
    }

    @Test
    void shouldGenerateDifferentNoncesPerRequest() {
        String first = CspNonceGenerator.createNonce();
        String second = CspNonceGenerator.createNonce();
        Assertions.assertNotEquals(first, second);
        Assertions.assertTrue(first.matches("[0-9A-Za-z]{64}"));
    }

}
