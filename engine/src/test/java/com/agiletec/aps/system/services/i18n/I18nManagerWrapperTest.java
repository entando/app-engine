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
package com.agiletec.aps.system.services.i18n;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.services.i18n.wrapper.I18nLabelBuilder;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(MockitoExtension.class)
class I18nManagerWrapperTest {

    private static final String KEY_1 = "TEST_LABEL";
    private static final String KEY_2 = "INCOMPLETE_LABEL";
    private static final String KEY_3 = "MISSING_LABEL";

    @Mock
    private II18nManager i18nManager;
    
    @Mock
    private RequestContext reqCtx;

    @BeforeEach
    void init() throws Exception {
        ILangManager langManager = Mockito.mock(ILangManager.class);
        Lang defaultLang = new Lang();
        defaultLang.setCode("it");
        defaultLang.setDescr("Italiano");
        Mockito.lenient().when(langManager.getDefaultLang()).thenReturn(defaultLang);
        WebApplicationContext wac = Mockito.mock(WebApplicationContext.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        Mockito.lenient().when(reqCtx.getRequest()).thenReturn(request);
        request.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
        Mockito.lenient().when(wac.getBean(ILangManager.class)).thenReturn(langManager);
        Mockito.lenient().when(i18nManager.getLabel(KEY_1, "it")).thenReturn("Testo1 Italiano (test 1)");
        Mockito.lenient().when(i18nManager.getLabel(KEY_1, "en")).thenReturn("English text1 (test 1)");
        Mockito.lenient().when(i18nManager.getLabel(KEY_2, "it")).thenReturn("Testo2 Italiano (test 2)");
    }

    @Test
    void testLabelWithoutReqCtx() throws Throwable {
        I18nManagerWrapper wrapper = new I18nManagerWrapper("it", i18nManager);
        Assertions.assertEquals("Testo1 Italiano (test 1)", wrapper.getLabel(KEY_1));
        Assertions.assertEquals("Testo2 Italiano (test 2)", wrapper.getLabel(KEY_2));
        Assertions.assertEquals(KEY_3, wrapper.getLabel(KEY_3));

        wrapper = new I18nManagerWrapper("en", i18nManager);
        Assertions.assertEquals("English text1 (test 1)", wrapper.getLabel(KEY_1));
        Assertions.assertEquals(KEY_2, wrapper.getLabel(KEY_2));
        Assertions.assertEquals(KEY_3, wrapper.getLabel(KEY_3));
    }

    @Test
    void testLabelWithReqCtx() throws Throwable {
        I18nManagerWrapper wrapper = new I18nManagerWrapper("it", i18nManager, this.reqCtx);
        Assertions.assertEquals("Testo1 Italiano (test 1)", wrapper.getLabel(KEY_1));
        Assertions.assertEquals("Testo2 Italiano (test 2)", wrapper.getLabel(KEY_2));
        Assertions.assertEquals(KEY_3, wrapper.getLabel(KEY_3));
        Assertions.assertNull(wrapper.getLabel(null));

        wrapper = new I18nManagerWrapper("en", i18nManager, this.reqCtx);
        Assertions.assertEquals("English text1 (test 1)", wrapper.getLabel(KEY_1));
        Assertions.assertEquals("Testo2 Italiano (test 2)", wrapper.getLabel(KEY_2));
        Assertions.assertEquals(KEY_3, wrapper.getLabel(KEY_3));
    }

    @Test
    void testLabelWithError() throws Throwable {
        String labelKey = "test_label";
        Mockito.lenient().when(i18nManager.getLabel(labelKey, "it")).thenThrow(new EntException("Error"));
        I18nManagerWrapper wrapper = new I18nManagerWrapper("it", i18nManager);
        Assertions.assertEquals(labelKey, wrapper.getLabel(labelKey));
    }

    @Test
    void testLabelWithParam() throws Throwable {
        Mockito.lenient().when(i18nManager.getLabel("LABEL_WITH_PARAMS", "en"))
                .thenReturn("Welcome ${surname} ${name} (${username} - ${name}.${surname})");
        I18nManagerWrapper wrapper = new I18nManagerWrapper("en", i18nManager);
        I18nLabelBuilder builder = wrapper.getLabelWithParams("LABEL_WITH_PARAMS");
        builder.addParam("surname", "Black");
        builder.addParam("name", "Joe");
        builder.addParam("username", "joeblack");
        Assertions.assertEquals("Welcome Black Joe (joeblack - Joe.Black)", builder.toString());
    }

}
