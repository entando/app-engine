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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller that exposes menu configuration for app-builder integration.
 * <p>
 * Provides a {@code /legacy-components-menu} endpoint that returns menu entries
 * declared in each installed component's {@code component.xml} via
 * {@code <properties>} and {@code <menuItem>} elements.
 * This allows app-builder to dynamically discover plugin menu entries,
 * labels, and required permissions without hardcoded configuration.
 * </p>
 */
@RestController
@RequestMapping(value = "/system")
public class SystemController {

    @Autowired
    private IComponentManager componentManager;

    private static final String HOOK_CMS = "cms";

    /**
     * Returns menu entries from installed components that declare an
     * {@code appBuilderMenu.hook} property in their {@code component.xml}.
     *
     * <p>Aggregation rules:</p>
     * <ul>
     *   <li>{@code hook = "cms"} - items from all components are merged into a single entry
     *       (e.g. content-scheduler and content-workflow items appear together)</li>
     *   <li>Any other hook (e.g. {@code "legacyPlugins"}) - each component keeps its own
     *       separate entry with its {@code pluginId}, {@code pluginLabel}, and {@code items}</li>
     * </ul>
     *
     * <p>Each {@code <menuItem>} in {@code component.xml} maps to an item object with:</p>
     * <ul>
     *   <li>{@code id} - unique menu item identifier</li>
     *   <li>{@code defaultLabel} - fallback label when i18n key is not available</li>
     *   <li>{@code labelId} - (optional) i18n message key</li>
     *   <li>{@code href} - relative URL for the admin console action</li>
     *   <li>{@code requiredPermission} - (optional) permission expression;
     *       supports {@code |} (OR) and {@code &} (AND)</li>
     * </ul>
     *
     * <p>Example response:</p>
     * <pre>{@code
     * {
     *   "payload": [
     *     {
     *       "appBuilderMenu.hook": "cms",
     *       "appBuilderMenu.items": [
     *         { "id": "menu-scheduler", "defaultLabel": "Scheduler", "href": "..." },
     *         { "id": "menu-workflow", "defaultLabel": "Workflow", "href": "..." }
     *       ]
     *     },
     *     {
     *       "appBuilderMenu.hook": "legacyPlugins",
     *       "appBuilderMenu.pluginId": "jpwebdynamicform",
     *       "appBuilderMenu.pluginLabel": "Web Dynamic Forms",
     *       "appBuilderMenu.items": [
     *         { "id": "wdf-messages", "defaultLabel": "Message List", "href": "..." },
     *         { "id": "wdf-config", "defaultLabel": "Configuration", "href": "..." }
     *       ]
     *     },
     *     {
     *       "appBuilderMenu.hook": "legacyPlugins",
     *       "appBuilderMenu.pluginId": "jpotherplugin",
     *       "appBuilderMenu.pluginLabel": "Other Plugin",
     *       "appBuilderMenu.items": [
     *         { "id": "other-config", "defaultLabel": "Config", "href": "..." }
     *       ]
     *     }
     *   ]
     * }
     * }</pre>
     *
     * @return a list of menu entry maps grouped according to the aggregation rules
     */

    @RestAccessControl(permission = Permission.ENTER_BACKEND)
    @GetMapping(value = "/legacy-components-menu", produces = MediaType.APPLICATION_JSON_VALUE)
    @SuppressWarnings("unchecked")
    public ResponseEntity<SimpleRestResponse<List<Map<String, Object>>>> getLegacyComponentsMenu() {

        List<Map<String, Object>> allProperties = componentManager.getCurrentComponents().stream()
                .filter(c -> c.getProperties() != null && !c.getProperties().isEmpty())
                .filter(c -> c.getProperties().containsKey("appBuilderMenu.hook"))
                .map(Component::getProperties)
                .collect(Collectors.toList());

        List<Map<String, Object>> report = new ArrayList<>();
        Map<String, Object> cmsEntry = null;

        for (Map<String, Object> props : allProperties) {
            String hook = String.valueOf(props.get("appBuilderMenu.hook"));
            if (HOOK_CMS.equals(hook)) {
                // Aggregate cms items into a single entry
                if (cmsEntry == null) {
                    cmsEntry = new HashMap<>();
                    cmsEntry.put("appBuilderMenu.hook", HOOK_CMS);
                    cmsEntry.put("appBuilderMenu.items", new ArrayList<Map<String, String>>());
                    report.add(cmsEntry);
                }
                Object items = props.get("appBuilderMenu.items");
                if (items instanceof List) {
                    ((List<Map<String, String>>) cmsEntry.get("appBuilderMenu.items"))
                            .addAll((List<Map<String, String>>) items);
                }
            } else {
                // Each plugin keeps its own entry
                report.add(props);
            }
        }

        return new ResponseEntity<>(new SimpleRestResponse<>(report), HttpStatus.OK);
    }
}