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
     * <p><b>Supported hooks:</b></p>
     * <ul>
     *   <li>{@code "cms"} — items from all components are merged into a single entry
     *       (e.g. content-scheduler and content-workflow items appear together under the CMS menu)</li>
     *   <li>{@code "legacyPlugins"} — each component keeps its own separate entry with
     *       {@code pluginId} and {@code pluginLabel}, rendered under the "Legacy Plugins" sidebar menu</li>
     * </ul>
     *
     * <p><b>Menu item fields</b> (from {@code <menuItem>} in {@code component.xml}):</p>
     * <ul>
     *   <li>{@code id} — unique menu item identifier (required)</li>
     *   <li>{@code defaultLabel} — display label shown in the app-builder menu (required)</li>
     *   <li>{@code labelId} — i18n message key for the app-builder front-end (optional);
     *       when present, app-builder uses
     *       {@code intl.formatMessage({id: labelId, defaultMessage: defaultLabel})};
     *       when absent, falls back to {@code id} as the i18n key</li>
     *   <li>{@code href} — relative URL for the admin-console action (required)</li>
     *   <li>{@code requiredPermission} — permission expression checked by app-builder
     *       before rendering the item; supports {@code |} (OR) and {@code &} (AND)
     *       (required)</li>
     * </ul>
     *
     * <p><b>Plugin-level fields</b> (from {@code <property>} in {@code component.xml},
     * only for non-cms hooks):</p>
     * <ul>
     *   <li>{@code appBuilderMenu.pluginId} — unique plugin identifier (optional);
     *       when present, items are grouped under a plugin header;
     *       when absent, items render without a header</li>
     *   <li>{@code appBuilderMenu.pluginLabel} — display name for the plugin group header
     *       (optional, defaults to {@code pluginId})</li>
     * </ul>
     *
     * <p><b>Example response:</b></p>
     * <pre>{@code
     * {
     *   "payload": [
     *     {
     *       "appBuilderMenu.hook": "cms",
     *       "appBuilderMenu.items": [
     *         { "id": "menu-scheduler", "labelId": "cms.menu.scheduler",
     *           "defaultLabel": "Scheduler", "href": "do/...",
     *           "requiredPermission": "editContents|validateContents" }
     *       ]
     *     },
     *     {
     *       "appBuilderMenu.hook": "legacyPlugins",
     *       "appBuilderMenu.pluginId": "jpwebdynamicform",
     *       "appBuilderMenu.pluginLabel": "Web Dynamic Forms",
     *       "appBuilderMenu.items": [
     *         { "id": "wdf-messages", "defaultLabel": "Message List",
     *           "href": "do/...", "requiredPermission": "superuser" }
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