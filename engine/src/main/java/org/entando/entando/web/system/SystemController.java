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
package org.entando.entando.web.system;

import com.agiletec.aps.system.services.role.Permission;
import org.entando.entando.aps.system.init.IComponentManager;
import org.entando.entando.aps.system.init.model.Component;
import org.entando.entando.web.common.annotation.RestAccessControl;
import org.entando.entando.web.common.model.SimpleRestResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller that exposes system-level information for app-builder integration.
 * <p>
 * Provides a report endpoint that returns the properties declared in each installed
 * component's {@code component.xml} (via the {@code <properties>} element).
 * This allows app-builder to dynamically discover plugin capabilities such as
 * menu entries, labels, and required permissions, instead of relying on
 * hardcoded configuration.
 * </p>
 */
@RestController
@RequestMapping(value = "/system")
public class SystemController {

    @Autowired
    private IComponentManager componentManager;

    /**
     * Returns the properties of all installed components that declare a
     * {@code <properties>} section in their {@code component.xml}.
     *
     * <p>Example response payload:</p>
     * <pre>{@code
     * {
     *   "payload": [
     *     {
     *       "appBuilderMenu.id": "menu-scheduler",
     *       "appBuilderMenu.labelId": "cms.menu.scheduler",
     *       "appBuilderMenu.defaultLabel": "Scheduler",
     *       "appBuilderMenu.href": "do/jpcontentscheduler/config/viewItem.action",
     *       "appBuilderMenu.requiredPermission": "superuser"
     *     }
     *   ]
     * }
     * }</pre>
     *
     * <p>Supported property keys:</p>
     * <ul>
     *   <li>{@code appBuilderMenu.id} - unique menu item identifier</li>
     *   <li>{@code appBuilderMenu.labelId} - i18n message key for the menu label</li>
     *   <li>{@code appBuilderMenu.defaultLabel} - fallback label when i18n key is missing</li>
     *   <li>{@code appBuilderMenu.href} - relative URL for the admin console action</li>
     *   <li>{@code appBuilderMenu.requiredPermission} - boolean permission expression evaluated by
     *       app-builder's {@code checkPermission} helper. Supports {@code &} (AND), {@code |} (OR),
     *       and parentheses for grouping.
     *       <br>Examples:
     *       <ul>
     *         <li>{@code "superuser"} - single permission</li>
     *         <li>{@code "editContents|validateContents"} - either permission</li>
     *         <li>{@code "(editContents|validateContents)&superuser"} - either editor or supervisor, AND superuser</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * @return a list of property maps, one per component with properties defined
     */
    @RestAccessControl(permission = Permission.ENTER_BACKEND)
    @GetMapping(value = "/report", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleRestResponse<List<Map<String, String>>>> getReport() {

        List<Map<String, String>> report = componentManager.getCurrentComponents().stream()
                .filter(c -> c.getProperties() != null && !c.getProperties().isEmpty())
                .map(Component::getProperties)
                .collect(Collectors.toList());

        return new ResponseEntity<>(new SimpleRestResponse<>(report), HttpStatus.OK);
    }
}