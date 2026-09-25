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
package org.entando.entando.aps.system.services.widgettype;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

/**
 * Helper class for widget types.
 */
public final class WidgetTypeUtils {

    private WidgetTypeUtils() {
    }

    /**
     * @return true when widgetCode equals one entry of the comma-separated stockWidgetCodes ({@code widgets.stock})
     */
    public static boolean isStockWidget(String stockWidgetCodes, String widgetCode) {
        if (StringUtils.isBlank(stockWidgetCodes) || StringUtils.isBlank(widgetCode)) {
            return false;
        }
        return Arrays.stream(stockWidgetCodes.split(","))
                .map(String::trim)
                .anyMatch(widgetCode::equals);
    }

}
