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
package com.agiletec.plugins.jacms.aps.system.services.content.widget;

import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.DateAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.NumberAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.TextAttribute;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.util.DateConverter;
import com.agiletec.plugins.jacms.aps.system.services.content.widget.UserFilterOptionBean.AttributeFormFieldError;
import java.math.BigDecimal;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter;
import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserFilterOptionBeanTest {
    
    @Mock
    private IApsEntity prototype;
    
    @Test
    void shouldReturnErrorOnInvalidAttribute() {
        Properties prop = new Properties();
        prop.setProperty(UserFilterOptionBean.PARAM_KEY, "textAttribute");
        prop.setProperty(UserFilterOptionBean.TYPE_ATTRIBUTE, "text");
        prop.setProperty(UserFilterOptionBean.PARAM_IS_ATTRIBUTE_FILTER, "true");
        Mockito.when(prototype.getAttribute("textAttribute")).thenReturn(null);
        Assertions.assertThrows(EntException.class, () -> {
            new UserFilterOptionBean(prop, prototype);
        });
    }
    
    @Test
    void shouldExtractTextFilter() throws Throwable {
        Properties prop = new Properties();
        prop.setProperty(UserFilterOptionBean.PARAM_KEY, "textAttribute");
        prop.setProperty(UserFilterOptionBean.TYPE_ATTRIBUTE, "text");
        prop.setProperty(UserFilterOptionBean.PARAM_IS_ATTRIBUTE_FILTER, "true");
        TextAttribute textAttribute = Mockito.mock(TextAttribute.class);
        Mockito.when(textAttribute.getType()).thenReturn("Text");
        Mockito.when(textAttribute.getName()).thenReturn("textAttribute");
        Mockito.when(prototype.getAttribute("textAttribute")).thenReturn(textAttribute);
        Lang lang = new Lang();
        lang.setCode("en");
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        // String[] fieldsSuffix = {"_textFieldName"};
        // String fieldName = paramName + fieldSuffix + frameIdSuffix;
        Mockito.when(request.getParameter("textAttribute_textFieldName_frame8")).thenReturn("text");
        UserFilterOptionBean ubof = new UserFilterOptionBean(prop, prototype, 8, lang, "yyyy-MM-dd", request);
        EntitySearchFilter ef = ubof.getEntityFilter();
        Assertions.assertEquals("textAttribute", ef.getKey());
        Assertions.assertEquals("text", ef.getValue());
        Assertions.assertEquals(true, ef.isAttributeFilter());
        SearchEngineFilter filter = ubof.extractFilter(); 
        Assertions.assertEquals("Text:textAttribute", filter.getKey());
        Assertions.assertEquals("text", filter.getValue());
        Assertions.assertEquals(SearchEngineFilter.TextSearchOption.EXACT, filter.getTextSearchOption());
    }
    
    @Test
    void shouldExtractDateFilter() throws Throwable {
        Properties prop = new Properties();
        prop.setProperty(UserFilterOptionBean.PARAM_KEY, "dateAttribute");
        prop.setProperty(UserFilterOptionBean.TYPE_ATTRIBUTE, "date");
        prop.setProperty(UserFilterOptionBean.PARAM_IS_ATTRIBUTE_FILTER, "true");
        DateAttribute dateAttribute = Mockito.mock(DateAttribute.class);
        Mockito.when(dateAttribute.getType()).thenReturn("Date");
        Mockito.when(dateAttribute.getName()).thenReturn("dateAttribute");
        Mockito.when(prototype.getAttribute("dateAttribute")).thenReturn(dateAttribute);
        Lang lang = new Lang();
        lang.setCode("it");
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        // String[] fieldsSuffix = {"_dateStartFieldName", "_dateEndFieldName"};
        Mockito.when(request.getParameter("dateAttribute_dateStartFieldName_frame7")).thenReturn("2023-10-23");
        Mockito.when(request.getParameter("dateAttribute_dateEndFieldName_frame7")).thenReturn("2023-11-17");
        UserFilterOptionBean ubof = new UserFilterOptionBean(prop, prototype, 7, lang, "yyyy-MM-dd", request);
        Assertions.assertNull(ubof.getFormFieldErrors());
        EntitySearchFilter ef = ubof.getEntityFilter();
        Assertions.assertEquals("dateAttribute", ef.getKey());
        Assertions.assertNull(ef.getValue());
        Assertions.assertEquals(true, ef.isAttributeFilter());
        Assertions.assertEquals(DateConverter.parseDate("2023-10-23", "yyyy-MM-dd"), ef.getStart());
        Assertions.assertEquals(DateConverter.parseDate("2023-11-17", "yyyy-MM-dd"), ef.getEnd());
        SearchEngineFilter filter = ubof.extractFilter(); 
        Assertions.assertEquals("Date:dateAttribute", filter.getKey());
        Assertions.assertNull(filter.getValue());
        Assertions.assertEquals(DateConverter.parseDate("2023-10-23", "yyyy-MM-dd"), filter.getStart());
        Assertions.assertEquals(DateConverter.parseDate("2023-11-17", "yyyy-MM-dd"), filter.getEnd());
        Assertions.assertNull(filter.getTextSearchOption());
    }
    
    @Test
    void shouldReturnErrorWithWrongDateRange() throws Throwable {
        Properties prop = new Properties();
        prop.setProperty(UserFilterOptionBean.PARAM_KEY, "dateAttr");
        prop.setProperty(UserFilterOptionBean.TYPE_ATTRIBUTE, "date");
        prop.setProperty(UserFilterOptionBean.PARAM_IS_ATTRIBUTE_FILTER, "true");
        DateAttribute dateAttribute = Mockito.mock(DateAttribute.class);
        Mockito.when(dateAttribute.getType()).thenReturn("Date");
        Mockito.when(dateAttribute.getName()).thenReturn("dateAttr");
        Mockito.when(prototype.getAttribute("dateAttr")).thenReturn(dateAttribute);
        Lang lang = new Lang();
        lang.setCode("it");
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        // String[] fieldsSuffix = {"_dateStartFieldName", "_dateEndFieldName"};
        Mockito.when(request.getParameter("dateAttr_dateStartFieldName_frame7")).thenReturn("2023-10-23");
        Mockito.when(request.getParameter("dateAttr_dateEndFieldName_frame7")).thenReturn("2023-07-17");
        UserFilterOptionBean ubof = new UserFilterOptionBean(prop, prototype, 7, lang, "yyyy-MM-dd", request);
        Assertions.assertEquals(1, ubof.getFormFieldErrors().size());
        AttributeFormFieldError error = ubof.getFormFieldErrors().get("dateAttr_dateEndFieldName_frame7");
        Assertions.assertEquals("dateAttr", error.getAttributeName());
        Assertions.assertEquals("dateAttr_dateEndFieldName_frame7", error.getFieldName());
        Assertions.assertEquals(AttributeFormFieldError.INVALID_RANGE_KEY, error.getErrorKey());
    }
    
    @Test
    void shouldExtractNumberFilter() throws Throwable {
        Properties prop = new Properties();
        prop.setProperty(UserFilterOptionBean.PARAM_KEY, "numberAttribute");
        prop.setProperty(UserFilterOptionBean.TYPE_ATTRIBUTE, "number");
        prop.setProperty(UserFilterOptionBean.PARAM_IS_ATTRIBUTE_FILTER, "true");
        NumberAttribute numberAttribute = Mockito.mock(NumberAttribute.class);
        Mockito.when(numberAttribute.getType()).thenReturn("Number");
        Mockito.when(numberAttribute.getName()).thenReturn("numberAttribute");
        Mockito.when(prototype.getAttribute("numberAttribute")).thenReturn(numberAttribute);
        Lang lang = new Lang();
        lang.setCode("it");
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        // String[] fieldsSuffix = {"_numberStartFieldName", "_numberEndFieldName"};
        Mockito.when(request.getParameter("numberAttribute_numberStartFieldName_frame3")).thenReturn("19");
        Mockito.when(request.getParameter("numberAttribute_numberEndFieldName_frame3")).thenReturn("128");
        UserFilterOptionBean ubof = new UserFilterOptionBean(prop, prototype, 3, lang, "yyyy-MM-dd", request);
        Assertions.assertNull(ubof.getFormFieldErrors());
        EntitySearchFilter ef = ubof.getEntityFilter();
        Assertions.assertEquals("numberAttribute", ef.getKey());
        Assertions.assertNull(ef.getValue());
        Assertions.assertEquals(true, ef.isAttributeFilter());
        Assertions.assertEquals(19, ((BigDecimal) ef.getStart()).intValue());
        Assertions.assertEquals(128, ((BigDecimal) ef.getEnd()).intValue());
        SearchEngineFilter filter = ubof.extractFilter(); 
        Assertions.assertEquals("Number:numberAttribute", filter.getKey());
        Assertions.assertNull(filter.getValue());
        Assertions.assertEquals(19, ((BigDecimal) filter.getStart()).intValue());
        Assertions.assertEquals(128, ((BigDecimal) filter.getEnd()).intValue());
        Assertions.assertNull(filter.getTextSearchOption());
    }
    
    @Test
    void shouldExtractBooleanFilter() throws Throwable {
        Properties prop = new Properties();
        prop.setProperty(UserFilterOptionBean.PARAM_KEY, "booleanAttribute");
        prop.setProperty(UserFilterOptionBean.TYPE_ATTRIBUTE, "boolean");
        prop.setProperty(UserFilterOptionBean.PARAM_IS_ATTRIBUTE_FILTER, "true");
        BooleanAttribute booleanAttribute = Mockito.mock(BooleanAttribute.class);
        Mockito.when(booleanAttribute.getType()).thenReturn("Boolean");
        Mockito.when(booleanAttribute.getName()).thenReturn("booleanAttribute");
        Mockito.when(prototype.getAttribute("booleanAttribute")).thenReturn(booleanAttribute);
        Lang lang = new Lang();
        lang.setCode("en");
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        // String[] fieldsSuffix = {"_booleanFieldName", "_booleanFieldName_ignore", "_booleanFieldName_control"};
        Mockito.when(request.getParameter("booleanAttribute_booleanFieldName_frame9")).thenReturn("true");
        Mockito.when(request.getParameter("booleanAttribute_booleanFieldName_ignore_frame9")).thenReturn(null);
        Mockito.when(request.getParameter("booleanAttribute_booleanFieldName_control_frame9")).thenReturn("true");
        UserFilterOptionBean ubof = new UserFilterOptionBean(prop, prototype, 9, lang, "yyyy-MM-dd", request);
        Assertions.assertNull(ubof.getFormFieldErrors());
        EntitySearchFilter ef = ubof.getEntityFilter();
        Assertions.assertEquals("booleanAttribute", ef.getKey());
        Assertions.assertEquals("true", ef.getValue());
        Assertions.assertEquals(true, ef.isAttributeFilter());
        Assertions.assertNull(ef.getStart());
        Assertions.assertNull(ef.getEnd());
    }
    
}
