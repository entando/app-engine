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
package org.entando.entando.aps.internalservlet.system.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import com.agiletec.apsadmin.system.dispatcher.ServletActionRedirectResultXF;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.struts2.dispatcher.mapper.ActionMapper;
import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.apache.struts2.views.util.UrlHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

class ServletActionRedirectResultXFIntegrationTest extends ApsAdminBaseTestCase {

    private ConfigInterface configManager;

    @BeforeEach
    protected void init() throws Exception {
        this.configManager = (ConfigInterface) this.getService(SystemConstants.BASE_CONFIG_MANAGER);
        IPageManager pageManager = (IPageManager) this.getService(SystemConstants.PAGE_MANAGER);
        ILangManager langManager = (ILangManager) this.getService(SystemConstants.LANGUAGE_MANAGER);
        RequestContext reqCtx = (RequestContext) super.getRequest().getAttribute(RequestContext.REQCTX);
        reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE, pageManager.getOnlineRoot());
        reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG, langManager.getDefaultLang());
        reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_FRAME, 0);
        ((MockHttpServletRequest) super.getRequest()).setScheme("https");
        ((MockHttpServletRequest) super.getRequest()).setServerPort(443);
        ((MockHttpServletRequest) super.getRequest()).setServerName("www.myproduct.com");
        ((MockHttpServletRequest) super.getRequest()).removeHeader("Host");
        ((MockHttpServletRequest) super.getRequest()).addHeader("Host", "www.myproduct.com");
        ((MockHttpServletRequest) super.getRequest()).setContextPath("/entando");
    }

    @Test
    void testExecuteServlet_1() throws Throwable {
        this.executeServlet("https", "www.myproduct.com", "/entando/do/main");
    }

    private void executeServlet(String expectedProtocol, String expectedAuthority, String expectedPath)
            throws Exception {
        UrlHelper urlHelper = this.getContainerObject(UrlHelper.class);
        this.initAction("/do", "main", true);
        ServletActionRedirectResultXF servlet = new ServletActionRedirectResultXF();
        servlet.setActionName("main");
        ActionMapper mapper = Mockito.mock(ActionMapper.class);
        Mockito.doReturn("/do/main").when(mapper).getUriFromActionMapping(Mockito.any(ActionMapping.class));
        servlet.setActionMapper(mapper);
        servlet.setUrlHelper(urlHelper);
        servlet.execute(super.getActionContext().getActionInvocation());
        URL aURL = new URL(servlet.getLocation());
        assertEquals(expectedProtocol, aURL.getProtocol());
        assertEquals(expectedAuthority, aURL.getAuthority());
        assertEquals(expectedPath, aURL.getPath());
        List<NameValuePair> params = URLEncodedUtils.parse(aURL.getQuery(), StandardCharsets.UTF_8);
        for (NameValuePair param : params) {
            if (param.getName().equals("internalServletActionPath")) {
                assertEquals("/ExtStr2/do/main", param.getValue());
            } else if (param.getName().equals("internalServletFrameDest")) {
                assertEquals("0", param.getValue());
            } else {
                fail();
            }
        }
    }

}
