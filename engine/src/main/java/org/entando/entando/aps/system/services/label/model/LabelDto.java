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
package org.entando.entando.aps.system.services.label.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import org.entando.entando.aps.system.services.component.ComponentUsageEntity;
import org.entando.entando.aps.system.services.component.IComponentDto;

public class LabelDto implements IComponentDto {

    private String key;
    private Map<String, String> titles;

    public LabelDto() {
        super();
    }

    public LabelDto(final String key, final Map<String, String> titles) {
        this.key = key;
        this.titles = titles;
    }

    @JsonIgnore
    @Override
    public String getType() {
        return ComponentUsageEntity.TYPE_LABEL;
    }
    
    @Override
    @JsonIgnore
    public String getCode() {
        return this.getKey();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Map<String, String> getTitles() {
        return titles;
    }

    public void setTitles(Map<String, String> titles) {
        this.titles = titles;
    }

}
