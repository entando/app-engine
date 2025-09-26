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

import static jakarta.servlet.jsp.tagext.Tag.EVAL_BODY_INCLUDE;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.RequestContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;
import org.entando.entando.aps.system.services.controller.executor.ExecutorBeanContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FreemarkerTemplateParameterTagTest {
    
    @Mock
    private PageContext pageContext;
    @Mock
    private ServletRequest servletRequest;
    @Mock
    private RequestContext reqCtx;
    @Mock
    private ExecutorBeanContainer executorBeanContainer;
    @Mock
    private JspWriter writer;

    @InjectMocks
    private FreemarkerTemplateParameterTag freemarkerTemplate;
    
    @BeforeEach
    public void setUp() throws Exception {
        when(pageContext.getRequest()).thenReturn(this.servletRequest);
        when(servletRequest.getAttribute(RequestContext.REQCTX)).thenReturn(this.reqCtx);
        when(reqCtx.getExecutorBeanContainer()).thenReturn(executorBeanContainer);
        Mockito.lenient().when(pageContext.getOut()).thenReturn(this.writer);
        this.freemarkerTemplate.release();
    }
    
    @Test
    void shouldOk() throws Throwable {
        int result = this.freemarkerTemplate.doStartTag();
        Assertions.assertEquals(EVAL_BODY_INCLUDE, result);
        Mockito.verify(pageContext, Mockito.times(0)).getOut();
        Mockito.verify(pageContext, Mockito.times(0)).setAttribute(Mockito.anyString(), Mockito.anyString());
    }

}
