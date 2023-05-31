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
package org.entando.entando.web.component.validator;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.entando.entando.aps.system.services.IComponentUsageService;

@Component
public class ComponentValidator implements Validator {

    public static final String ERRCODE_MISSING_FIELD = "1";
    public static final String ERRCODE_INVALID_TYPE = "2";
    
    public static final String TYPE_FIELD = "type";
    public static final String CODE_FIELD = "code";

    @Autowired
    private List<IComponentUsageService> services;

    @Override
    public boolean supports(Class<?> paramClass) {
        return (List.class.equals(paramClass));
    }

    @Override
    public void validate(Object target, Errors errors) {
        List<Map<String, String>> components = (List<Map<String, String>>) target;
        for (int i = 0; i < components.size(); i++) {
            Map<String, String> component = components.get(i);
            String index = String.valueOf(i);
            List.of(TYPE_FIELD, CODE_FIELD).stream().forEach(f -> {
                if (!component.containsKey(f)) {
                    errors.reject( ERRCODE_MISSING_FIELD, new String[]{index, f}, "components.usage.field.missing");
                }
            });
            Optional.ofNullable(component.get(TYPE_FIELD)).ifPresent(type -> {
                Optional<IComponentUsageService> object = services.stream()
                    .filter(s -> s.getObjectType().equalsIgnoreCase(type)).findFirst();
                if (!object.isPresent()) {
                    errors.reject(ERRCODE_INVALID_TYPE, new String[]{index, type}, "components.usage.type.invalid");
                }
            });
        }
    }

}
