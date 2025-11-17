/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jacms.aps.system.services.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.entando.entando.aps.system.services.api.model.AbstractApiResponse;
import org.entando.entando.aps.system.services.api.model.AbstractApiResponseResult;

/**
 *
 * @author super
 */
@XmlRootElement(name = "response")
@JsonRootName(value = "response")
public class CmsApiResponse extends AbstractApiResponse {

    @Override
    @XmlElement(name = "result", required = false)
    @JsonProperty("result")
    public JAXBCmsResult getResult() {
        return (JAXBCmsResult) super.getResult();
    }

    @Override
    public void setResult(Object result, String html) {
        super.setResult(result);
    }

    public void setResult(String result, String html) {
        JAXBCmsResult res = new JAXBCmsResult();
        res.setStatus(html);
        super.setResult(res);
    }

    @Override
    protected AbstractApiResponseResult createResponseResultInstance() {
        throw new UnsupportedOperationException("Unsupported method");
    }

}
