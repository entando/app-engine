/*
 * Copyright 2022-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.servlet.routing;

import com.agiletec.aps.system.EntThreadLocal;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class EngineVirtualContextFilter extends OncePerRequestFilter {

    public EngineVirtualContextFilter() {
    }

    @Override
    public void doFilterInternal(@NonNull final HttpServletRequest servletRequest,
                                 @NonNull final HttpServletResponse servletResponse,
                                 final FilterChain chain
    ) throws IOException, ServletException {
        try {
            EntThreadLocal.clear();
            chain.doFilter(
                    EngineVirtualContext.applyVirtualContext(servletRequest, servletResponse),
                    servletResponse
            );
        } finally {
            EntThreadLocal.destroy();
        }
    }
}
