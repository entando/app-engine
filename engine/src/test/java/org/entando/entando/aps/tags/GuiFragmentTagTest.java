/*
 * Copyright 2020-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.tags;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;
import org.entando.entando.aps.system.services.guifragment.IGuiFragmentManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuiFragmentTagTest {

    @Mock
    private PageContext pageContext;
    @Mock
    private ServletRequest servletRequest;
    @Mock
    private RequestContext reqCtx;
    @Mock
    private IGuiFragmentManager guiFragmentManager;
    @Mock
    private JspWriter writer;
    @InjectMocks
    private GuiFragmentTag guiFragmentTag;

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.lenient().when(pageContext.getRequest()).thenReturn(this.servletRequest);
        Mockito.lenient().when(servletRequest.getAttribute(RequestContext.REQCTX)).thenReturn(this.reqCtx);
        Mockito.lenient().when(pageContext.getOut()).thenReturn(this.writer);
        this.guiFragmentTag.release();
    }

    @Test
    void shouldOk() throws Throwable {
        try (MockedStatic<ApsWebApplicationUtils> apsWebApplication = Mockito.mockStatic(
                ApsWebApplicationUtils.class)) {
            apsWebApplication.when(
                            () -> ApsWebApplicationUtils.getBean(SystemConstants.GUI_FRAGMENT_MANAGER, this.pageContext))
                    .thenReturn(guiFragmentManager);
            int result = this.guiFragmentTag.doStartTag();
            Assertions.assertEquals(0, result);
        }
    }
}
