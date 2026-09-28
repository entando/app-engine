/*
 * Copyright 2026-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package com.agiletec.apsadmin.system.entity.attribute.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.agiletec.aps.system.common.entity.model.AttributeTracer;
import com.agiletec.aps.system.common.entity.model.attribute.TimestampAttribute;
import com.agiletec.apsadmin.system.entity.attribute.manager.AbstractAttributeManagerTest.FixedParameterWrapper;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

class TimestampAttributeManagerTest {

    private final TimestampAttributeManager manager = new TimestampAttributeManager();
    private final AttributeTracer tracer = new AttributeTracer();
    private TimestampAttribute attribute;

    @BeforeEach
    void setUp() {
        attribute = new TimestampAttribute();
        attribute.setName("it_start");
        attribute.setType("Timestamp");
    }

    @ParameterizedTest
    @CsvSource(nullValues = "missing", value = {
            "_hour,   10,      11,      10",
            "_minute, missing, 30,      30",
            "_second, missing, missing, missing",
            "_hour,   '',      11,      ''"
    })
    void getValueFromFormShouldFallBackToRequestAttribute(String suffix, String parameter, String attr, String result) {
        String field = tracer.getFormFieldName(attribute) + suffix;
        assertEquals("Timestamp:it_start" + suffix, field);
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (parameter != null) {
            request.addParameter(field, parameter);
        }
        request.setAttribute(field, attr);
        assertEquals(result, manager.getValueFromForm(attribute, tracer, suffix, new FixedParameterWrapper(request, parameter)));
    }

    @Test
    void updateEntityAttributeShouldSetHourPostedOnlyOnInnermostRequest() {
        attribute.setDate(new GregorianCalendar(2026, Calendar.JANUARY, 15, 0, 0, 0).getTime());
        MockHttpServletRequest original = new MockHttpServletRequest();
        original.addParameter("Timestamp:it_start_hour", "10");
        manager.updateEntityAttribute(attribute, new FixedParameterWrapper(original, null));
        Calendar cal = Calendar.getInstance();
        cal.setTime(attribute.getDate());
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
        assertNull(attribute.getFailedHourString());
    }

    @Test
    void updateEntityAttributeShouldFlagInvalidHourPostedOnlyOnInnermostRequest() {
        MockHttpServletRequest original = new MockHttpServletRequest();
        original.addParameter("Timestamp:it_start_hour", "99");
        manager.updateEntityAttribute(attribute, new FixedParameterWrapper(original, null));
        assertEquals("99", attribute.getFailedHourString());
    }

}
