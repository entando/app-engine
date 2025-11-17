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
package org.entando.entando.aps.tags;

import static jakarta.servlet.jsp.tagext.Tag.EVAL_PAGE;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;
import org.entando.entando.aps.system.services.guifragment.IGuiFragmentManager;
import org.entando.entando.aps.system.services.storage.CdsEnvironmentVariables;
import org.entando.entando.aps.system.services.storage.IStorageManager;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceURLTagTest {


    @Mock
    private ConfigInterface configSerivce;
    @Mock
    private IStorageManager storageManager;
    @Mock
    private PageContext pageContext;
    @Mock
    private ServletRequest servletRequest;
    @Mock
    private RequestContext reqCtx;
    @Mock
    private JspWriter writer;
    @InjectMocks
    private ResourceURLTag resourceURLTag;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Mockito.lenient().when(pageContext.getRequest()).thenReturn(this.servletRequest);
        Mockito.lenient().when(servletRequest.getAttribute(RequestContext.REQCTX)).thenReturn(this.reqCtx);
        Mockito.lenient().when(pageContext.getOut()).thenReturn(this.writer);
    }

    @Test
    void testTag() throws Exception {
        try(MockedStatic<ApsWebApplicationUtils> apsWebApplicationUtils = Mockito.mockStatic(ApsWebApplicationUtils.class)){

            apsWebApplicationUtils.when(() -> ApsWebApplicationUtils.getBean(SystemConstants.BASE_CONFIG_MANAGER,
                    ConfigInterface.class,
                    this.pageContext)).thenReturn(configSerivce);
            Mockito.when(configSerivce.getParam(SystemConstants.PAR_RESOURCES_ROOT_URL)).thenReturn("/test1/");
            resourceURLTag.setIgnoreTenant(true);
            Assertions.assertEquals(EVAL_PAGE, resourceURLTag.doEndTag());


            apsWebApplicationUtils.when(() -> ApsWebApplicationUtils.getBean(SystemConstants.STORAGE_MANAGER,
                            IStorageManager.class, this.pageContext))
                    .thenReturn(storageManager);
            Mockito.lenient().when(storageManager.getResourceUrl("", false)).thenReturn("/test2");
            resourceURLTag.setRoot(null);
            resourceURLTag.setIgnoreTenant(false);
            resourceURLTag.setFolder(null);
            Assertions.assertEquals(EVAL_PAGE, resourceURLTag.doEndTag());

            apsWebApplicationUtils.reset();
            resourceURLTag.setRoot(null);
            resourceURLTag.setIgnoreTenant(false);
            resourceURLTag.setFolder(null);
            Assertions.assertThrows(JspException.class, () -> resourceURLTag.doEndTag());

            apsWebApplicationUtils.reset();
            resourceURLTag.setRoot("/context-root");
            resourceURLTag.setIgnoreTenant(true);
            resourceURLTag.setFolder("/test-folder");
            Assertions.assertEquals(EVAL_PAGE, resourceURLTag.doEndTag());

        }
    }
}
