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
package org.entando.entando.aps.system.services.controller.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.springframework.mock.web.MockHttpServletRequest;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.IParameterizableManager;
import com.agiletec.aps.system.services.controller.ControllerManager;
import com.agiletec.aps.system.services.controller.control.ControlServiceInterface;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.page.Page;
import com.agiletec.aps.system.services.page.PageMetadata;
import com.agiletec.aps.system.services.page.PageTestUtil;
import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.aps.system.services.pagemodel.IPageModelManager;
import com.agiletec.aps.system.services.pagemodel.PageModel;
import java.util.HashMap;
import java.util.Map;
import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author M.Casari
 */
class TestRequestValidator extends BaseTestCase {

    @Test
    void testService() throws EntException {
        RequestContext reqCtx = this.getRequestContext();
        ((MockHttpServletRequest) reqCtx.getRequest()).setServletPath("/it/homepage.wp");
        int status = this.requestValidator.service(reqCtx, ControllerManager.CONTINUE);
        assertEquals(ControllerManager.CONTINUE, status);
        Lang lang = (Lang) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG);
        IPage page = (IPage) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE);
        assertNotNull(page);
        assertNotNull(lang);
        assertEquals("it", lang.getCode());
        assertEquals("homepage", page.getCode());
    }
    
    @Test
    void testServiceWithNewPage() throws EntException {
        this.testServiceWithNewPage("it", "req_validator_page_code", false);
        this.testServiceWithNewPage("en", "req-validator-page-code", false);
        this.testServiceWithNewPage("it", "req_validator_page_code", true);
        this.testServiceWithNewPage("en", "req-validator-page-code", true);
    }

    void testServiceWithNewPage(String langCode, String pageCode, boolean breadcrumbs) throws EntException {
        String pathInfo = (breadcrumbs) ? "/"+langCode+"/homepage/"+pageCode : "/"+langCode+"/"+pageCode+".page";
        IPage parentPage = pageManager.getDraftPage("service");
        String parentForNewPage = parentPage.getParentCode();
        PageModel pageModel = this.pageModelManager.getPageModel(parentPage.getMetadata().getModelCode());
        PageMetadata metadata = PageTestUtil.createPageMetadata(pageModel,
                true, "temp page", null, null, false, null, null);
        Page pageToAdd = PageTestUtil.createPage(pageCode, parentForNewPage, 
                "free", pageModel, metadata, new Widget[pageModel.getFrames().length]);
        try {
            this.pageManager.addPage(pageToAdd);
            this.pageManager.setPageOnline(pageCode);
            RequestContext reqCtx = this.getRequestContext();
            if (breadcrumbs) {
                ((MockHttpServletRequest) reqCtx.getRequest()).setServletPath("/pages");
                ((MockHttpServletRequest) reqCtx.getRequest()).setPathInfo(pathInfo);
            } else {
                ((MockHttpServletRequest) reqCtx.getRequest()).setServletPath(pathInfo);
            }
            int status = this.requestValidator.service(reqCtx, ControllerManager.CONTINUE);
            assertEquals(ControllerManager.CONTINUE, status);
            Lang lang = (Lang) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG);
            IPage page = (IPage) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE);
            assertNotNull(page);
            assertNotNull(lang);
            assertEquals(langCode, lang.getCode());
            assertEquals(pageCode, page.getCode());
        } catch (Exception e) {
            throw e;
        } finally {
            this.pageManager.setPageOffline(pageCode);
            this.pageManager.deletePage(pageCode);
        }
    }

    @Test
    void testServiceFailureWhenRequestPageThatDoesNotExist() throws EntException {
        String notFoundPageCode = this.pageManager.getConfig(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE);
        Map<String, String> paramsToUpgrade = new HashMap<>();
        try {
            RequestContext reqCtx = this.getRequestContext();
            ((MockHttpServletRequest) reqCtx.getRequest()).setServletPath("/it/notexists.wp");//Page does not exist
            int status = this.requestValidator.service(reqCtx, ControllerManager.CONTINUE);
            assertEquals(ControllerManager.CONTINUE, status);
            IPage page = (IPage) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE);
            assertEquals(notFoundPageCode, page.getCode());
            
            paramsToUpgrade.put(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE, "invalid");
            ((IParameterizableManager)this.pageManager).updateParams(paramsToUpgrade);
            
            ((MockHttpServletRequest) reqCtx.getRequest()).setServletPath("/it/notexists.wp");//Page does not exist
            status = this.requestValidator.service(reqCtx, ControllerManager.CONTINUE);
            assertEquals(ControllerManager.REDIRECT, status);
            String redirectUrl = (String) reqCtx.getExtraParam(RequestContext.EXTRAPAR_REDIRECT_URL);
            assertEquals("http://www.entando.com/Entando/it/errorpage.page?redirectflag=1", redirectUrl);
        } catch (Exception e) {
            throw e;
        } finally {
            paramsToUpgrade.put(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE, notFoundPageCode);
            ((IParameterizableManager)this.pageManager).updateParams(paramsToUpgrade);
        }
    }

    @Test
    void testServiceFailureWhenRequestAWrongPath() throws EntException {
        String notFoundPageCode = this.pageManager.getConfig(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE);
        RequestContext reqCtx = this.getRequestContext();
        ((MockHttpServletRequest) reqCtx.getRequest()).setServletPath("/wrongpath.wp");//wrong path
        int status = this.requestValidator.service(reqCtx, ControllerManager.CONTINUE);
        IPage page = (IPage) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE);
        assertEquals(notFoundPageCode, page.getCode());
        assertEquals(ControllerManager.CONTINUE, status);
    }

    @Test
    void testServiceFailureWhenRequestLangThatDoesNotExist() throws EntException {
        String notFoundPageCode = this.pageManager.getConfig(IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE);
        RequestContext reqCtx = this.getRequestContext();
        ((MockHttpServletRequest) reqCtx.getRequest()).setServletPath("/cc/homepage.wp");//lang does not exist
        int status = this.requestValidator.service(reqCtx, ControllerManager.CONTINUE);
        IPage page = (IPage) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE);
        assertEquals(notFoundPageCode, page.getCode());
        assertEquals(ControllerManager.CONTINUE, status);
    }
    
    @BeforeEach
    void init() throws Exception {
        try {
            this.requestValidator = (ControlServiceInterface) this.getApplicationContext().getBean("RequestValidatorControlService");
            this.pageManager = this.getApplicationContext().getBean(SystemConstants.PAGE_MANAGER, IPageManager.class);
            this.pageModelManager = this.getApplicationContext().getBean(IPageModelManager.class);
            RequestContext reqCtx = this.getRequestContext();
            reqCtx.removeExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE);
            reqCtx.removeExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG);
        } catch (Throwable e) {
            throw new Exception(e);
        }
    }
    
    private ControlServiceInterface requestValidator;
    private IPageManager pageManager;
    private IPageModelManager pageModelManager;

}
