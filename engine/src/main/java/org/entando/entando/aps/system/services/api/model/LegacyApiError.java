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
package org.entando.entando.aps.system.services.api.model;

import java.io.Serializable;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.springframework.http.HttpStatus;

/**
 * @author E.Santoboni
 */
@XmlRootElement(name = "error")
@XmlType(propOrder = {"code", "message"})
public class LegacyApiError implements Serializable {

    private String code;
    private String message;
    private HttpStatus status;

    public LegacyApiError() {
    }

    public LegacyApiError(String code, String message) {
        this.setCode(code);
        this.setMessage(message);
    }

    public LegacyApiError(String code, String message, HttpStatus status) {
        this.setCode(code);
        this.setStatus(status);
        this.setMessage(message);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    protected void setStatus(HttpStatus status) {
        this.status = status;
    }
}