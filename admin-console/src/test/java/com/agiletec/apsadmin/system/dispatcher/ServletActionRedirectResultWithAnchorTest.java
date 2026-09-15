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
package com.agiletec.apsadmin.system.dispatcher;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import org.junit.jupiter.api.Test;

/**
 * Guards the Jetty 12 fix: a redirect location carrying a URL fragment must be emitted via the
 * {@code Location} header (not {@code HttpServletResponse.sendRedirect}, which Jetty 12 rejects with
 * {@code IllegalArgumentException: Fragment}), while fragment-free locations keep standard behaviour.
 */
class ServletActionRedirectResultWithAnchorTest {

    private static final String ANCHOR_URL =
            "/entando-de-app/do/Entity/CompositeAttribute/entryCompositeAttribute.action#fagiano_compositeTypesList";
    private static final String PLAIN_URL =
            "/entando-de-app/do/Entity/entryEntityType.action";

    @Test
    void locationWithAnchorIsSentViaHeaderNotSendRedirect() throws Exception {
        ServletActionRedirectResultWithAnchor result = new ServletActionRedirectResultWithAnchor();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        result.sendRedirect(response, ANCHOR_URL);

        // 302 + Location header directly — never sendRedirect (which Jetty 12 would reject on the fragment)
        verify(response).setStatus(HttpServletResponse.SC_FOUND);
        verify(response).setHeader("Location", ANCHOR_URL);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    void locationWithoutAnchorDelegatesToSendRedirect() throws Exception {
        ServletActionRedirectResultWithAnchor result = new ServletActionRedirectResultWithAnchor();
        HttpServletResponse response = mock(HttpServletResponse.class);

        result.sendRedirect(response, PLAIN_URL);

        verify(response).sendRedirect(PLAIN_URL);
        verify(response, never()).setHeader(anyString(), anyString());
    }
}
