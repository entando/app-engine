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
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeRole;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.PageMetadata;
import com.agiletec.aps.util.ApsProperties;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.tagext.TagSupport;
import org.entando.entando.plugins.jpseo.aps.system.services.metatag.Metatag;
import org.entando.entando.plugins.jpseo.aps.system.services.page.PageMetatag;
import org.entando.entando.plugins.jpseo.aps.system.services.page.SeoPageMetadata;
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
class SeoMetatagTagTest {
    
    private MockServletContext mockServletContext;
    
    @Mock
    private IPage currentPage;

    @Mock
    private MockPageContext mockPageContext;

    @Mock
    private WebApplicationContext webApplicationContext;
    
    @Mock
    private RequestContext requestContext;
    
    @Mock
    private ILangManager langManager;
    
    @Mock
    private IContentManager contentManager;
    
    private SeoMetatagTag seoMetatagTag;
    
    @BeforeEach
    void setUp() {
        this.seoMetatagTag = new SeoMetatagTag();
        this.seoMetatagTag.setPageContext(mockPageContext);
        mockServletContext = new MockServletContext();
        mockServletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, webApplicationContext);
        MockHttpServletRequest request = new MockHttpServletRequest();
        Mockito.when(mockPageContext.getRequest()).thenReturn(request);
        Mockito.lenient().when(requestContext.getRequest()).thenReturn(request);
        Lang currentLang = new Lang();
        currentLang.setCode("it");
        currentLang.setDescr("italiano");
        Mockito.when(requestContext.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG)).thenReturn(currentLang);
        Mockito.when(requestContext.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE)).thenReturn(this.currentPage);
        Lang defaultLang = new Lang();
        defaultLang.setCode("en");
        defaultLang.setDescr("Enlish");
        Mockito.lenient().when(webApplicationContext.getBean(SystemConstants.LANGUAGE_MANAGER)).thenReturn(langManager);
        Mockito.lenient().when(langManager.getDefaultLang()).thenReturn(defaultLang);
        Mockito.lenient().when(webApplicationContext.getBean(JacmsSystemConstants.CONTENT_MANAGER)).thenReturn(contentManager);
        Mockito.lenient().when(this.currentPage.getMetadata()).thenReturn(this.createPageMetadata());
        Mockito.lenient().when(mockPageContext.getOut()).thenReturn(Mockito.mock(JspWriter.class));
        Mockito.lenient().when(mockPageContext.getServletContext()).thenReturn(mockServletContext);
        mockPageContext.getRequest().setAttribute(RequestContext.REQCTX, requestContext);
    }
    
    @Test
    void testGetMetadata() throws Exception {
        Mockito.when(this.currentPage.getMetadata()).thenReturn(new PageMetadata());
        int result = this.seoMetatagTag.doEndTag();
        Mockito.verifyNoInteractions(webApplicationContext);
        Assertions.assertEquals(TagSupport.EVAL_PAGE, result);
    }
    
    @Test
    void testDescriptionSeoValue_1() throws Exception {
        this.seoMetatagTag.setKey(CurrentPageTag.DESCRIPTION_INFO);
        int result = this.seoMetatagTag.doEndTag();
        Mockito.verifyNoInteractions(webApplicationContext);
        Assertions.assertEquals(TagSupport.EVAL_PAGE, result);
        Mockito.verify(mockPageContext, Mockito.times(1)).getOut();
    }
    
    @Test
    void testDescriptionSeoValue_2() throws Exception {
        Mockito.when(requestContext.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG)).thenReturn(null);
        this.seoMetatagTag.setEscapeXml(false);
        this.seoMetatagTag.setKey(CurrentPageTag.DESCRIPTION_INFO);
        int result = this.seoMetatagTag.doEndTag();
        Mockito.verify(webApplicationContext, Mockito.times(1)).getBean(SystemConstants.LANGUAGE_MANAGER);
        Mockito.verify(langManager, Mockito.times(1)).getDefaultLang();
        Assertions.assertEquals(TagSupport.EVAL_PAGE, result);
        Mockito.verify(mockPageContext, Mockito.times(1)).getOut();
    }
    
    @Test
    void testKeywordsSeoValue() throws Exception {
        this.seoMetatagTag.setKey(CurrentPageTag.KEYWORDS_INFO);
        this.seoMetatagTag.setVar("var");
        int result = this.seoMetatagTag.doEndTag();
        Mockito.verifyNoInteractions(webApplicationContext);
        Assertions.assertEquals(TagSupport.EVAL_PAGE, result);
        Mockito.verify(mockPageContext, Mockito.times(0)).getOut();
        Mockito.verify(this.mockPageContext, Mockito.times(1)).setAttribute("var", "Ita keywords");
        Mockito.verify(this.mockPageContext, Mockito.times(0)).setAttribute(Mockito.eq("var_attribute"), Mockito.anyString());
    }
    
    @Test
    void testFriendlyCodeSeoValue() throws Exception {
        Mockito.when(requestContext.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG)).thenReturn(null);
        this.seoMetatagTag.setKey(CurrentPageTag.FRIENDLY_CODE_INFO);
        this.seoMetatagTag.setVar("var");
        int result = this.seoMetatagTag.doEndTag();
        Mockito.verify(webApplicationContext, Mockito.times(1)).getBean(SystemConstants.LANGUAGE_MANAGER);
        Mockito.verify(langManager, Mockito.times(1)).getDefaultLang();
        Assertions.assertEquals(TagSupport.EVAL_PAGE, result);
        Mockito.verify(mockPageContext, Mockito.times(0)).getOut();
        Mockito.verify(this.mockPageContext, Mockito.times(1)).setAttribute("var", "en_friendly_code");
        Mockito.verify(this.mockPageContext, Mockito.times(0)).setAttribute(Mockito.eq("var_attribute"), Mockito.anyString());
    }
    
    @Test
    void testExtraParamSeoValue_1() throws Exception {
        Mockito.when(requestContext.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG)).thenReturn(null);
        this.seoMetatagTag.setKey("param2");
        this.seoMetatagTag.setVar("var");
        Mockito.when(contentManager.getAttributeRole("param2")).thenReturn(null);
        int result = this.seoMetatagTag.doEndTag();
        Mockito.verify(webApplicationContext, Mockito.times(1)).getBean(SystemConstants.LANGUAGE_MANAGER);
        Mockito.verify(langManager, Mockito.times(1)).getDefaultLang();
        Assertions.assertEquals(TagSupport.EVAL_PAGE, result);
        Mockito.verify(mockPageContext, Mockito.times(0)).getOut();
        Mockito.verify(this.mockPageContext, Mockito.times(1)).setAttribute("var", "En value for param param2");
        Mockito.verify(this.mockPageContext, Mockito.times(1)).setAttribute("param2_attribute", Metatag.ATTRIBUTE_NAME_PROPERTY);
    }
    
    @Test
    void testExtraParamSeoValue_2() throws Exception {
        this.seoMetatagTag.setKey("param3");
        this.seoMetatagTag.setVar("var");
        Mockito.when(contentManager.getAttributeRole("param3")).thenReturn(null);
        int result = this.seoMetatagTag.doEndTag();
        Mockito.verify(webApplicationContext, Mockito.times(1)).getBean(SystemConstants.LANGUAGE_MANAGER);
        Mockito.verifyNoInteractions(this.langManager);
        Assertions.assertEquals(TagSupport.EVAL_PAGE, result);
        Mockito.verify(mockPageContext, Mockito.times(0)).getOut();
        Mockito.verify(this.mockPageContext, Mockito.times(1)).setAttribute("var", "Valore It per parametro param3");
        Mockito.verify(this.mockPageContext, Mockito.times(1)).setAttribute("param3_attribute", Metatag.ATTRIBUTE_NAME_NAME);
    }
    
    @Test
    void testExtraParamSeoValue_3() throws Exception {
        Mockito.when(this.currentPage.getMetadata()).thenReturn(new SeoPageMetadata());
        this.seoMetatagTag.setKey("param4");
        this.seoMetatagTag.setVar("var");
        Mockito.when(contentManager.getAttributeRole("param4")).thenReturn(null);
        int result = this.seoMetatagTag.doEndTag();
        Mockito.verify(webApplicationContext, Mockito.times(2)).getBean(Mockito.anyString());
        Assertions.assertEquals(TagSupport.EVAL_PAGE, result);
        Mockito.verify(mockPageContext, Mockito.times(0)).getOut();
        Mockito.verify(this.mockPageContext, Mockito.times(0)).setAttribute(Mockito.any(), Mockito.any());
    }
    
    @Test
    void testExtraParamSeoValue_4() throws Exception {
        this.seoMetatagTag.setKey("param4");
        this.seoMetatagTag.setVar("var");
        Mockito.when(contentManager.getAttributeRole("param4")).thenReturn(null);
        int result = this.seoMetatagTag.doEndTag();
        Mockito.verify(webApplicationContext, Mockito.times(2)).getBean(Mockito.anyString());
        Assertions.assertEquals(TagSupport.EVAL_PAGE, result);
        Mockito.verify(mockPageContext, Mockito.times(0)).getOut();
        Mockito.verify(this.mockPageContext, Mockito.times(1)).setAttribute("var", "default value for param4");
        Mockito.verify(this.mockPageContext, Mockito.times(1)).setAttribute("param4_attribute", Metatag.ATTRIBUTE_NAME_PROPERTY);
    }
    
    @Test
    void testRoleSeoValue_1() throws Exception {
        this.seoMetatagTag.setKey("role1");
        this.seoMetatagTag.setVar("var");
        Mockito.when(contentManager.getAttributeRole("role1")).thenReturn(Mockito.mock(AttributeRole.class));
        Mockito.when(requestContext.getExtraParam(JacmsSystemConstants.ATTRIBUTE_WITH_ROLE_CTX_PREFIX  + "role1")).thenReturn("Single role value");
        int result = this.seoMetatagTag.doEndTag();
        Mockito.verify(webApplicationContext, Mockito.times(1)).getBean(SystemConstants.LANGUAGE_MANAGER);
        Mockito.verifyNoInteractions(langManager);
        Assertions.assertEquals(TagSupport.EVAL_PAGE, result);
        Mockito.verify(mockPageContext, Mockito.times(0)).getOut();
        Mockito.verify(this.mockPageContext, Mockito.times(1)).setAttribute("var", "Single role value");
        Mockito.verify(this.mockPageContext, Mockito.times(1)).setAttribute("role1_attribute", Metatag.ATTRIBUTE_NAME_NAME);
    }
    
    @Test
    void testRoleSeoValue_2() throws Exception {
        Mockito.when(requestContext.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG)).thenReturn(null);
        this.seoMetatagTag.setKey("role2");
        this.seoMetatagTag.setVar("var");
        Mockito.when(contentManager.getAttributeRole("role2")).thenReturn(Mockito.mock(AttributeRole.class));
        Map<String,String> map = Map.ofEntries(Map.entry("it","It Value"), Map.entry("en","En Value"));
        Mockito.when(requestContext.getExtraParam(JacmsSystemConstants.ATTRIBUTE_WITH_ROLE_CTX_PREFIX  + "role2")).thenReturn(map);
        int result = this.seoMetatagTag.doEndTag();
        Mockito.verify(webApplicationContext, Mockito.times(1)).getBean(SystemConstants.LANGUAGE_MANAGER);
        Mockito.verify(langManager, Mockito.times(1)).getDefaultLang();
        Assertions.assertEquals(TagSupport.EVAL_PAGE, result);
        Mockito.verify(mockPageContext, Mockito.times(0)).getOut();
        Mockito.verify(this.mockPageContext, Mockito.times(1)).setAttribute("var", "En Value");
        Mockito.verify(this.mockPageContext, Mockito.times(1)).setAttribute("role2_attribute", Metatag.ATTRIBUTE_NAME_NAME);
    }
    
    @Test
    void testRoleSeoValue_3() throws Exception {
        this.seoMetatagTag.setKey("role3");
        this.seoMetatagTag.setVar("var");
        Mockito.when(contentManager.getAttributeRole("role3")).thenReturn(Mockito.mock(AttributeRole.class));
        Map<String,String> map = Map.ofEntries(Map.entry("en","En Value Alternative"));
        Mockito.when(requestContext.getExtraParam(JacmsSystemConstants.ATTRIBUTE_WITH_ROLE_CTX_PREFIX  + "role3")).thenReturn(map);
        int result = this.seoMetatagTag.doEndTag();
        Mockito.verify(webApplicationContext, Mockito.times(1)).getBean(SystemConstants.LANGUAGE_MANAGER);
        Mockito.verify(langManager, Mockito.times(1)).getDefaultLang();
        Assertions.assertEquals(TagSupport.EVAL_PAGE, result);
        Mockito.verify(mockPageContext, Mockito.times(0)).getOut();
        Mockito.verify(this.mockPageContext, Mockito.times(1)).setAttribute("var", "En Value Alternative");
        Mockito.verify(this.mockPageContext, Mockito.times(1)).setAttribute("role3_attribute", Metatag.ATTRIBUTE_NAME_NAME);
    }
    
    @Test
    void testRoleSeoValue_4() throws Exception {
        this.seoMetatagTag.setKey("role4");
        this.seoMetatagTag.setVar("var");
        Mockito.when(contentManager.getAttributeRole("role4")).thenReturn(Mockito.mock(AttributeRole.class));
        Mockito.when(requestContext.getExtraParam(JacmsSystemConstants.ATTRIBUTE_WITH_ROLE_CTX_PREFIX  + "role4")).thenReturn(null);
        int result = this.seoMetatagTag.doEndTag();
        Mockito.verify(webApplicationContext, Mockito.times(1)).getBean(SystemConstants.LANGUAGE_MANAGER);
        Mockito.verifyNoInteractions(langManager);
        Assertions.assertEquals(TagSupport.EVAL_PAGE, result);
        Mockito.verify(mockPageContext, Mockito.times(0)).getOut();
        Mockito.verify(this.mockPageContext, Mockito.times(0)).setAttribute(Mockito.any(), Mockito.any());
    }
    
    @Test
    void testDoEndTagWithError_1() {
        Mockito.lenient().when(this.currentPage.getMetadata()).thenThrow(new RuntimeException("Error"));
        Assertions.assertThrows(JspException.class, () -> {
            this.seoMetatagTag.doEndTag();
        });
    }
    
    @Test
    void testDoEndTagWithError_2() throws Exception {
        this.seoMetatagTag.setKey(CurrentPageTag.DESCRIPTION_INFO);
        this.seoMetatagTag.setEscapeXml(false);
        Mockito.lenient().when(mockPageContext.getOut()).thenThrow(new RuntimeException());
        Assertions.assertThrows(JspException.class, () -> {
            this.seoMetatagTag.doEndTag();
        });
    }
    
    private SeoPageMetadata createPageMetadata() {
        SeoPageMetadata meta = new SeoPageMetadata();
        meta.setDescriptions(this.createProperty("description", "Descrizione", "Description"));
        meta.setKeywords(this.createProperty("keywords", "Ita keywords", "En keywords"));
        meta.setFriendlyCodes(this.createProperty("friendlycode", "ita_friendly_code", "en_friendly_code"));
        Map<String, Map<String, PageMetatag>> complexParameters = new HashMap<>();
        complexParameters.put("it", new HashMap<>());
        complexParameters.put("en", new HashMap<>());
        complexParameters.put("default", new HashMap<>());
        Arrays.asList("param1", "param2", "param3", "param4").stream().forEach(p -> {
            if (p.equals("param4")) {
                complexParameters.get("default").put("param4", new PageMetatag(p, p, "default value for param4", Metatag.ATTRIBUTE_NAME_PROPERTY, false));
            } else {
                complexParameters.get("it").put(p, new PageMetatag("it", p, "Valore It per parametro " + p));
                complexParameters.get("en").put(p, new PageMetatag("en", p, "En value for param " + p, Metatag.ATTRIBUTE_NAME_PROPERTY, false));
            }
        });
        meta.setComplexParameters(complexParameters);
        return meta;
    }
    
    private ApsProperties createProperty(String key, String itValue, String enValue) {
        ApsProperties prop = new ApsProperties();
        prop.put("en", new PageMetatag("en", key, enValue));
        prop.put("it", new PageMetatag("it", key, itValue));
        return prop;
    }
    
}
