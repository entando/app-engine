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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class WidgetTypeUtilsTest {

    private static final String STOCK = "formAction,login_form,messages_system,entando_apis";

    @ParameterizedTest
    @ValueSource(strings = {"formAction", "login_form", "messages_system", "entando_apis"})
    void listedCodeIsStock(String code) {
        assertTrue(WidgetTypeUtils.isStockWidget(STOCK, code));
    }

    @ParameterizedTest
    @ValueSource(strings = {"form", "Action", "login", "system", "apis", "entando", "_", ",", "n,log", "formAction,login_form"})
    void substringOfTheListIsNotStock(String code) {
        assertFalse(WidgetTypeUtils.isStockWidget(STOCK, code));
    }

    @Test
    void spacesAroundCodesAreIgnored() {
        assertTrue(WidgetTypeUtils.isStockWidget(" formAction , login_form ", "login_form"));
    }

    @Test
    void emptyEntriesMatchNothing() {
        assertFalse(WidgetTypeUtils.isStockWidget("formAction,,login_form", ""));
        assertTrue(WidgetTypeUtils.isStockWidget("formAction,,login_form", "login_form"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void blankListHasNoStockCodes(String stockWidgetCodes) {
        assertFalse(WidgetTypeUtils.isStockWidget(stockWidgetCodes, "formAction"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void blankCodeIsNotStock(String code) {
        assertFalse(WidgetTypeUtils.isStockWidget(STOCK, code));
    }

}
