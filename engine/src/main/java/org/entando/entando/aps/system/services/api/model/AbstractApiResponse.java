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
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;

/**
 * @author E.Santoboni
 */
public abstract class AbstractApiResponse implements Serializable {

    public void setResult(Object result, String html) {
        AbstractApiResponseResult responseResult = this.createResponseResultInstance();
        responseResult.setMainResult(result);
        responseResult.setHtml(html);
        this.setResult(responseResult);
    }

    protected abstract AbstractApiResponseResult createResponseResultInstance();

    @XmlElement(name = "error", required = true)
    @XmlElementWrapper(name = "errors")
    @JsonProperty("errors")
    public List<LegacyApiError> getErrors() {
        return this.errors;
    }

    public void addError(LegacyApiError error) {
        if (null != error) {
            this.errors.add(error);
        }
    }

    public void addErrors(List<LegacyApiError> errors) {
        if (null == errors) {
            return;
        }
        this.getErrors().addAll(errors);
    }

    public Object getResult(){ return result; }

    protected void setResult(Object result) {
        this.result = result;
    }

    private List<LegacyApiError> errors = new ArrayList<>();
    private Object result;

}