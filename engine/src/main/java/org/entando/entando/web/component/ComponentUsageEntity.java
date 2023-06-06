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
package org.entando.entando.web.component;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import org.entando.entando.aps.system.services.IComponentDto;

public class ComponentUsageEntity implements Serializable {

    public static final String TYPE_PAGE = "page";
    public static final String TYPE_WIDGET = "widget";
    public static final String TYPE_FRAGMENT = "fragment";
    public static final String TYPE_PAGE_TEMPLATE = "pageTemplate";
    public static final String TYPE_CONTENT = "content";
    
    public static final String ONLINE_PROPERTY = "online";
    
    private String type;
    private String code;
    private String status;
    
    private Map<String, Object> extraProperties = new TreeMap<>();

    public ComponentUsageEntity() {
    }

    public ComponentUsageEntity(String type, String code) {
        this.type = type;
        this.code = code;
    }

    public ComponentUsageEntity(String type, String code, String status) {
        this(type, code);
        this.status = status;
    }

    public ComponentUsageEntity(String type, IComponentDto dto) {
        this(type, dto.getCode(), dto.getStatus());
        this.extraProperties.putAll(dto.getExtraProperties());
    }

    public String getType() {
        return type;
    }

    public ComponentUsageEntity setType(String type) {
        this.type = type;
        return this;
    }

    public String getCode() {
        return code;
    }

    public ComponentUsageEntity setCode(String code) {
        this.code = code;
        return this;
    }

    @JsonInclude(Include.NON_NULL)
    public String getStatus() {
        return status;
    }

    public ComponentUsageEntity setStatus(String status) {
        this.status = status;
        return this;
    }
    
    @JsonAnyGetter
    public Map<String, Object> getExtraProperties() {
        return this.extraProperties;
    }
    
}
