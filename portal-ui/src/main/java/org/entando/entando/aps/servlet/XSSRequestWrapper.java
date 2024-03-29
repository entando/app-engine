/*
 * Copyright 2017-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.servlet;

import org.apache.commons.lang3.StringUtils;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.errors.ValidationException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XSSRequestWrapper extends HttpServletRequestWrapper {

    private static final Logger log = LoggerFactory.getLogger(XSSRequestWrapper.class.getName());

    public XSSRequestWrapper(HttpServletRequest servletRequest) {
        super(servletRequest);
    }

    @Override
    public String[] getParameterValues(String parameter) {
        String[] values = super.getParameterValues(parameter);
        if (values == null) {
            return null;
        }
        int count = values.length;
        String[] encodedValues = new String[count];
        for (int i = 0; i < count; i++) {
            encodedValues[i] = validatedParameter(values[i]);
        }
        return encodedValues;
    }

    @Override
    public String getParameter(String parameter) {
        return validatedParameter(parameter);
    }

    @Override
    public String getHeader(String name) {
        return validatedHeader(name);
    }

    private String validatedParameter(String name) {
        String value = super.getParameter(name);
        if (StringUtils.isBlank(value)) {
            return value;
        }
        try {
            value = ESAPI.validator().getValidInput("HTTP parameter value: " + value, value, "HTTPParameterValue", 2000, true);
        } catch (ValidationException e) {
            log.error("Invalid parameter ('{}' - '{}'), encoding as HTML attribute", name, value, e);
            value = ESAPI.encoder().encodeForHTMLAttribute(value);
        } catch (Throwable e) {
            log.debug("Invalid parameter ('{}' - '{}') - error message {}", name, value, e.getMessage());
        }
        return value;
    }

    private String validatedHeader(String name) {
        String value = super.getHeader(name);
        if (StringUtils.isBlank(value)) {
            return value;
        }
        try {
            value = ESAPI.validator().getValidInput("HTTP header value: " + value, value, "HTTPHeaderValue", 150, false);
        } catch (ValidationException e) {
            log.error("Invalid header ('{}' - '{}'), encoding as HTML attribute", name, value, e);
            value = ESAPI.encoder().encodeForHTMLAttribute(value);
        } catch (Throwable e) {
            log.debug("Invalid header ('{}' - '{}') - error message {}", name, value, e.getMessage());
        }
        return value;
    }

}
