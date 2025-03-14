package com.agiletec.aps.tags;

import org.entando.entando.aps.tags.ExtendedTagSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import java.util.Arrays;
import java.util.List;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.url.IURLManager;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import org.entando.entando.aps.servlet.routing.VirtualContextHelper;
import org.entando.entando.ent.util.EntLogging.EntLogger;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class InfoTagTest {

    private static final short EVAL_PAGE = 6;
    @Mock
    private PageContext pageContext;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private RequestContext requestContext;
    @Mock
    private IPageManager pageManager;
    @Mock
    private ILangManager langManager;
    @Mock
    private ConfigInterface configManager;
    @Mock
    private IURLManager urlManager;
    @Mock
    private EntLogger logger;


    @InjectMocks
    private InfoTag tag;

    @Test
    void testDoStartTag_startLang_browserLang() throws Exception {
        try (MockedStatic<ApsWebApplicationUtils> mockedStatic = Mockito.mockStatic(ApsWebApplicationUtils.class)) {
            tag.setKey("startLang");
            when(pageManager.getConfig(IPageManager.CONFIG_PARAM_START_LANG_FROM_BROWSER)).thenReturn("true");
            when(pageContext.getRequest()).thenReturn(httpServletRequest);
            when(httpServletRequest.getHeader("Accept-Language")).thenReturn("en-GB");
            mockedStatic.when(() -> ApsWebApplicationUtils.getBean(SystemConstants.PAGE_MANAGER, pageContext))
                    .thenReturn(pageManager);
            mockedStatic.when(() -> ApsWebApplicationUtils.getBean(SystemConstants.LANGUAGE_MANAGER, pageContext))
                    .thenReturn(langManager);

            Lang enLang = mkLang("en", "English");
            when(langManager.getLang("en")).thenReturn(enLang);
            int result = tag.doStartTag();
            assertEquals(InfoTag.EVAL_BODY_INCLUDE, result);
            assertEquals("en", tag.getInfo());
        }
    }


    @Test
    void testDoStartTag_startLang_defaultLang() throws Exception {
        try (MockedStatic<ApsWebApplicationUtils> mockedStatic = Mockito.mockStatic(ApsWebApplicationUtils.class)) {
            tag.setKey("startLang");
            when(pageManager.getConfig(IPageManager.CONFIG_PARAM_START_LANG_FROM_BROWSER)).thenReturn("true");
            when(pageContext.getRequest()).thenReturn(httpServletRequest);
            when(httpServletRequest.getHeader("Accept-Language")).thenReturn("invalid");
            mockedStatic.when(() -> ApsWebApplicationUtils.getBean(SystemConstants.PAGE_MANAGER, pageContext))
                    .thenReturn(pageManager);
            mockedStatic.when(() -> ApsWebApplicationUtils.getBean(SystemConstants.LANGUAGE_MANAGER, pageContext))
                    .thenReturn(langManager);
            Lang defaultLang = mkLang("it", "Italian");
            when(langManager.getDefaultLang()).thenReturn(defaultLang);
            int result = tag.doStartTag();
            assertEquals(InfoTag.EVAL_BODY_INCLUDE, result);
            assertEquals("it", tag.getInfo());
        }
    }

    @Test
    void testDoStartTag_defaultLang() throws Exception {
        try (MockedStatic<ApsWebApplicationUtils> mockedStatic = Mockito.mockStatic(ApsWebApplicationUtils.class)) {
            tag.setKey("defaultLang");
            Lang defaultLang = mkLang("it", "Italian");
            mockedStatic.when(() -> ApsWebApplicationUtils.getBean(SystemConstants.LANGUAGE_MANAGER, pageContext))
                    .thenReturn(langManager);
            when(langManager.getDefaultLang()).thenReturn(defaultLang);
            int result = tag.doStartTag();
            assertEquals(InfoTag.EVAL_BODY_INCLUDE, result);
            assertEquals("it", tag.getInfo());
        }
    }

    @Test
    void testDoStartTag_currentLang() throws Exception {
        try (MockedStatic<ApsWebApplicationUtils> mockedStatic = Mockito.mockStatic(ApsWebApplicationUtils.class)) {
            tag.setKey("currentLang");
            Lang currentLang = mkLang("es", "Spanish");
            when(pageContext.getRequest()).thenReturn(httpServletRequest);
            when(httpServletRequest.getAttribute(RequestContext.REQCTX)).thenReturn(requestContext);
            mockedStatic.when(() -> ApsWebApplicationUtils.getBean(SystemConstants.LANGUAGE_MANAGER, pageContext))
                    .thenReturn(langManager);
            when(requestContext.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG)).thenReturn(currentLang);
            int result = tag.doStartTag();
            assertEquals(InfoTag.EVAL_BODY_INCLUDE, result);
            assertEquals("es", tag.getInfo());
        }
    }

    @Test
    void testDoStartTag_langs() throws Exception {
        try (MockedStatic<ApsWebApplicationUtils> mockedStatic = Mockito.mockStatic(ApsWebApplicationUtils.class)) {
            tag.setKey("langs");
            List<Lang> langs = Arrays.asList(mkLang("en", "English"), mkLang("it", "Italian"));
            mockedStatic.when(() -> ApsWebApplicationUtils.getBean(SystemConstants.LANGUAGE_MANAGER, pageContext))
                    .thenReturn(langManager);
            when(langManager.getLangs()).thenReturn(langs);
            int result = tag.doStartTag();
            assertEquals(InfoTag.EVAL_BODY_INCLUDE, result);
            assertEquals(langs, tag.getInfo());
        }
    }

    @Test
    void testDoStartTag_request_currentVirtualContext() throws Exception {
        try (MockedStatic<ApsWebApplicationUtils> mockedStatic = Mockito.mockStatic(ApsWebApplicationUtils.class)) {
            tag.setKey("request");
            tag.setParamName(SystemConstants.PAR_APPL_CURRENT_VIRTUAL_CONTEXT);
            String virtualContext = "/myVirtualContext";
            mockedStatic.when(() -> ApsWebApplicationUtils.getBean(SystemConstants.LANGUAGE_MANAGER, pageContext))
                    .thenReturn(langManager); // Added this line

            VirtualContextHelper.setThreadLocal_VirtualContextPath(virtualContext);
            int result = tag.doStartTag();
            assertEquals(InfoTag.EVAL_BODY_INCLUDE, result);
            assertEquals("myVirtualContext", tag.getInfo());
        }
    }


    @Test
    void testDoStartTag_request_virtualContexts() throws Exception {
        try (MockedStatic<ApsWebApplicationUtils> mockedStatic = Mockito.mockStatic(ApsWebApplicationUtils.class)) {
            try (MockedStatic<VirtualContextHelper> mockedVCH = Mockito.mockStatic(VirtualContextHelper.class)) {
                tag.setKey("request");
                tag.setParamName(SystemConstants.PAR_APPL_VIRTUAL_CONTEXTS);
                List<String> contexts = Arrays.asList("context1", "context2");
                mockedStatic.when(() -> ApsWebApplicationUtils.getBean(SystemConstants.LANGUAGE_MANAGER, pageContext))
                        .thenReturn(langManager); // Added this line
                mockedVCH.when(VirtualContextHelper::getVirtualContexts).thenReturn(contexts);
                int result = tag.doStartTag();
                assertEquals(InfoTag.EVAL_BODY_INCLUDE, result);
                assertEquals(contexts, tag.getInfo());
            }
        }
    }

    @Test
    void testDoStartTag_systemParam_baseUrl() throws Exception {
        try (MockedStatic<ApsWebApplicationUtils> mockedStatic = Mockito.mockStatic(ApsWebApplicationUtils.class)) {
            tag.setKey("systemParam");
            tag.setParamName(SystemConstants.PAR_APPL_BASE_URL);
            String baseUrl = "http://example.com";
            mockedStatic.when(() -> ApsWebApplicationUtils.getBean(SystemConstants.URL_MANAGER, pageContext))
                    .thenReturn(urlManager);
            when(urlManager.getApplicationBaseURL(httpServletRequest)).thenReturn(baseUrl);
            when(pageContext.getRequest()).thenReturn(httpServletRequest);
            int result = tag.doStartTag();
            assertEquals(InfoTag.EVAL_BODY_INCLUDE, result);
            assertEquals(baseUrl, tag.getInfo());
        }
    }

    @Test
    void testDoStartTag_systemParam_configParam() throws Exception {
        try (MockedStatic<ApsWebApplicationUtils> mockedStatic = Mockito.mockStatic(ApsWebApplicationUtils.class)) {
            tag.setKey("systemParam");
            tag.setParamName("testParam");
            String paramValue = "testValue";
            mockedStatic.when(() -> ApsWebApplicationUtils.getBean(SystemConstants.BASE_CONFIG_MANAGER, pageContext))
                    .thenReturn(configManager);
            when(configManager.getParam("testParam")).thenReturn(paramValue);
            int result = tag.doStartTag();
            assertEquals(InfoTag.EVAL_BODY_INCLUDE, result);
            assertEquals(paramValue, tag.getInfo());
        }
    }


    @Test
    void testDoEndTag_withVar() throws Exception {
        tag.setInfo("testValue");
        tag.setVar("myVar");
        String[] res = new String[]{""};
        Mockito.doAnswer((invocation) -> {
            res[0] = invocation.getArgument(1).toString();
            return null;
        }).when(pageContext).setAttribute(anyString(), anyString());
        int result = tag.doEndTag();
        assertEquals(EVAL_PAGE, result);
        assertEquals("testValue", res[0]);
    }

    @Test
    void testDoEndTag_withoutVar() throws Exception {
        String[] res = new String[]{""};
        try (MockedStatic<ExtendedTagSupport> mockedStatic = Mockito.mockStatic(ExtendedTagSupport.class)) {
            mockedStatic.when(() -> ExtendedTagSupport.out(pageContext, true, "testValue"))
                    .thenAnswer(invocation -> {
                        res[0] = invocation.getArgument(2).toString();
                        return res[0];
                    });

            tag.setInfo("testValue");
            int result = tag.doEndTag();
            assertEquals(EVAL_PAGE, result);
            assertEquals("testValue", res[0]);
        }
    }

    @Test
    void testDoEndTag_nullInfo() throws Exception {
        int result = tag.doEndTag();
        assertEquals(EVAL_PAGE, result);
    }

    @Test
    void testRelease() {
        tag.setKey("testKey");
        tag.setVar("testVar");
        tag.setParamName("testParam");
        tag.setInfo("testInfo");
        tag.release();
        assertNull(tag.getKey());
        assertNull(tag.getVar());
        assertNull(tag.getParamName());
        assertNull(tag.getInfo());
    }

    private Lang mkLang(String code, String name) {
        Lang l = new Lang();
        l.setCode(code);
        l.setDescr(name);
        return l;
    }
}