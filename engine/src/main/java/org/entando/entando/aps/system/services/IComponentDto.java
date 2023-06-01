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
package org.entando.entando.aps.system.services;

import java.util.Map;
import org.entando.entando.web.component.ComponentUsageEntity;

public interface IComponentDto {
    
    public default ComponentUsageEntity buildUsageEntity(String type) {
        return new ComponentUsageEntity(type, this);
    }
    
    public String getCode();
    
    public default String getStatus() {
        return null;
    }
    
    public default Map<String, Object> getExtraProperties() {
        return Map.of();
    }
    
}
