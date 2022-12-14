/*
 * Copyright 2018-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.web.page.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.entando.entando.aps.system.services.page.serializer.WidgetConfigPropertiesDeserializer;

public class WidgetConfigurationRequest {

    @JsonIgnore
    private Map<String, Object> processInfo = new HashMap<>();

    @NotNull(message = "widgetConfigurationRequest.code.notBlank")
    private String code;

    public WidgetConfigurationRequest() {}

    public WidgetConfigurationRequest(
            @NotNull(message = "widgetConfigurationRequest.code.notBlank") String code) {
        this.code = code;
    }

    /*
     * Related to EN6-183, Frontend needs all config objects to be completely valid JSON objects.
     * This Deserializer converts known widget config formats, but may need to be improved case by case.
     * Also, possible conflicts may arise if different widgets use same property names and different value formats.
     * See also, WidgetConfigPropertiesSerializer.java.
     */
    @JsonDeserialize(converter = WidgetConfigPropertiesDeserializer.class)
    private Map<String, Object> config;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public Map<String, Object> getProcessInfo() {
        return processInfo;
    }

    public void setProcessInfo(Map<String, Object> processInfo) {
        this.processInfo = processInfo;
    }

}
