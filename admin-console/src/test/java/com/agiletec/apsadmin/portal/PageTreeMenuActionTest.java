/*
 * Copyright 2026-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package com.agiletec.apsadmin.portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agiletec.aps.util.SelectItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.entando.entando.aps.system.services.widgettype.IWidgetTypeManager;
import org.entando.entando.aps.system.services.widgettype.WidgetType;
import org.junit.jupiter.api.Test;

class PageTreeMenuActionTest {

    @Test
    void shouldGroupOnlyListedCoreWidgetsAsStock() {
        IWidgetTypeManager widgetTypeManager = mock(IWidgetTypeManager.class);
        when(widgetTypeManager.getWidgetTypes()).thenReturn(List.of(
                coreWidget("formAction"), coreWidget("login_form"), coreWidget("form"), coreWidget("login")));
        PageTreeMenuAction action = new PageTreeMenuAction();
        action.setWidgetTypeManager(widgetTypeManager);
        action.setStockWidgetCodes("formAction, login_form");

        Map<String, List<SelectItem>> mapping = action.getWidgetFlavoursMapping(new ArrayList<>());

        assertEquals(List.of("formAction", "login_form"), codes(mapping.get(PageTreeMenuAction.STOCK_WIDGETS_CODE)));
        assertEquals(List.of("form", "login"), codes(mapping.get(PageTreeMenuAction.CUSTOM_WIDGETS_CODE)));
    }

    private static WidgetType coreWidget(String code) {
        WidgetType type = new WidgetType();
        type.setCode(code);
        type.setLocked(true);
        return type;
    }

    private static List<String> codes(List<SelectItem> items) {
        return items.stream().map(SelectItem::getKey).toList();
    }

}
