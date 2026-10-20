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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

class AbstractAttributeManagerTest {

    private static final String NAME = "Text:it_title";

    private final AbstractAttributeManager manager = new MonoTextAttributeManager();

    @ParameterizedTest
    @CsvSource(nullValues = "missing", value = {
            "A,       B,       C,       A",
            "missing, B,       C,       B",
            "'',      B,       C,       B",
            "missing, missing, C,       C",
            "'',      '',      '',      ''",
            "'',      missing, missing, missing",
            "missing, missing, missing, missing"
    })
    void unwrapParameterShouldReturnOutermostNonEmptyValue(String outer, String middle, String inner, String result) {
        MockHttpServletRequest innerRequest = new MockHttpServletRequest();
        if (inner != null) {
            innerRequest.addParameter(NAME, inner);
        }
        HttpServletRequest chain = new FixedParameterWrapper(new FixedParameterWrapper(innerRequest, middle), outer);
        assertEquals(result, manager.unwrapParameter(chain, NAME));
    }

    @Test
    void unwrapParameterShouldReadPlainRequestOnce() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter(NAME, "A");
        assertEquals("A", manager.unwrapParameter(request, NAME));
    }

    @Test
    void unwrapParameterShouldNotStopAtClearedField() {
        MockHttpServletRequest inner = new MockHttpServletRequest();
        inner.addParameter(NAME, "old value");
        assertEquals("old value", manager.unwrapParameter(new FixedParameterWrapper(inner, ""), NAME));
    }

    /**
     * Wrapper that answers every parameter with a fixed value, as an include wrapper that does not expose the original POST.
     */
    static class FixedParameterWrapper extends HttpServletRequestWrapper {

        private final String value;

        FixedParameterWrapper(HttpServletRequest request, String value) {
            super(request);
            this.value = value;
        }

        @Override
        public String getParameter(String name) {
            return this.value;
        }
    }

}
