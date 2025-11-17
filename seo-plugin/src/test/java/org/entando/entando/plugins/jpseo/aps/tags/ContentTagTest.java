/*
 * Copyright 2023-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpseo.aps.tags;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.widget.IContentViewerHelper;
import com.agiletec.plugins.jacms.aps.system.services.dispenser.ContentRenderizationInfo;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.plugins.jpseo.aps.system.JpseoSystemConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockPageContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(MockitoExtension.class)
class ContentTagTest {
    
    private MockServletContext mockServletContext;
    
    @Mock
    private MockPageContext mockPageContext;

    @Mock
    private WebApplicationContext webApplicationContext;
    
    @Mock
    private RequestContext requestContext;
    
    @Mock
    private IContentViewerHelper helper;
    
    private ContentTag contentTag;
    
    @BeforeEach
    void setUp() {
        mockServletContext = new MockServletContext();
        mockServletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, webApplicationContext);
        MockHttpServletRequest request = new MockHttpServletRequest();
        Mockito.lenient().when(requestContext.getRequest()).thenReturn(request);
        Mockito.lenient().when(mockPageContext.getRequest()).thenReturn(request);
        Mockito.lenient().when(mockPageContext.getOut()).thenReturn(Mockito.mock(JspWriter.class));
        Mockito.lenient().when(mockPageContext.getServletContext()).thenReturn(mockServletContext);
        Mockito.lenient().when(webApplicationContext.getBean(JacmsSystemConstants.CONTENT_VIEWER_HELPER)).thenReturn(helper);
        mockPageContext.getRequest().setAttribute(RequestContext.REQCTX, requestContext);
        this.contentTag = new ContentTag();
        this.contentTag.setPageContext(mockPageContext);
    }
    
    @Test
    void testOutOnPageContext() throws Exception {
        contentTag.doStartTag();
        Mockito.verify(mockPageContext, Mockito.times(1)).getOut();
    }
    
    @Test
    void testOutOnVar() throws Exception {
        contentTag.setVar("param");
        contentTag.setContentId("ART123");
        contentTag.setModelId("default");
        contentTag.setPublishExtraTitle(true);
        contentTag.setAttributeValuesByRoleVar("valuesByRole");
        contentTag.setPublishExtraDescription(true);
        
        ContentRenderizationInfo mockRenderInfo = Mockito.mock(ContentRenderizationInfo.class);
        Mockito.when(helper.getRenderizationInfo("ART123", "default", true, requestContext)).thenReturn(mockRenderInfo);
        Mockito.when(mockRenderInfo.getRenderedContent()).thenReturn("rendered");
        Map<String, Object> attributeValues = new HashMap<>();
        attributeValues.put(JpseoSystemConstants.ATTRIBUTE_ROLE_DESCRIPTION, "Description");
        Mockito.when(mockRenderInfo.getAttributeValues()).thenReturn(attributeValues);
        contentTag.doStartTag();
        Mockito.verify(mockPageContext, Mockito.times(1)).setAttribute(Mockito.eq("valuesByRole"), Mockito.any());
        Mockito.verify(mockPageContext, Mockito.times(1)).setAttribute("param", "rendered");
        Mockito.verify(mockPageContext, Mockito.times(0)).getOut();
        Mockito.verify(requestContext, Mockito.times(1)).addExtraParam(Mockito.eq(JpseoSystemConstants.EXTRAPAR_EXTRA_PAGE_DESCRIPTIONS), Mockito.any());
    }
    
    @Test
    void testDoStartTagWithError() throws Exception {
        Mockito.when(helper.getRenderizationInfo(null, null, false, requestContext)).thenThrow(new EntException("Error"));
        Assertions.assertThrows(JspException.class, () -> {
            contentTag.doStartTag();
        });
    }
    
}
