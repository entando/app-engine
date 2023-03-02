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
package com.agiletec.aps.system.services.i18n;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.util.ApsProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class I18nManagerWrapperIntegrationTest extends BaseTestCase {

    private static final String KEY_1 = "TEST_LABEL";
    private static final String KEY_2 = "INCOMPLETE_LABEL";
    private static final String KEY_3 = "MISSING_LABEL";

    private II18nManager i18nManager;

    @BeforeEach
    void init() throws Exception {
        this.i18nManager = this.getApplicationContext().getBean(II18nManager.class);
        ApsProperties labels1 = new ApsProperties();
        labels1.put("it", "Testo1 Italiano (test 1)");
        labels1.put("en", "English text1 (test 1)");
        ApsProperties labels2 = new ApsProperties();
        labels2.put("it", "Testo2 Italiano (test 2)");
        Assertions.assertNull(this.i18nManager.getLabelGroups().get(KEY_1));
        Assertions.assertNull(this.i18nManager.getLabelGroups().get(KEY_2));
        Assertions.assertNull(this.i18nManager.getLabelGroups().get(KEY_3));
        this.i18nManager.addLabelGroup(KEY_1, labels1);
        Assertions.assertNotNull(this.i18nManager.getLabelGroups().get(KEY_1));
        this.i18nManager.addLabelGroup(KEY_2, labels2);
        Assertions.assertNotNull(this.i18nManager.getLabelGroups().get(KEY_2));
    }

    @AfterEach
    void dispose() throws Exception {
        this.i18nManager.deleteLabelGroup(KEY_1);
        Assertions.assertNull(this.i18nManager.getLabelGroups().get(KEY_1));
        this.i18nManager.deleteLabelGroup(KEY_2);
        Assertions.assertNull(this.i18nManager.getLabelGroups().get(KEY_2));
        Assertions.assertNull(this.i18nManager.getLabelGroups().get(KEY_3));
    }

    @Test
    void testLabelWithoutReqCtx() throws Throwable {
        I18nManagerWrapper wrapper = new I18nManagerWrapper("it", i18nManager);
        Assertions.assertEquals("Testo1 Italiano (test 1)", wrapper.getLabel(KEY_1));
        Assertions.assertEquals("Testo2 Italiano (test 2)", wrapper.getLabel(KEY_2));
        Assertions.assertEquals(KEY_3, wrapper.getLabel(KEY_3));

        wrapper = new I18nManagerWrapper("en", i18nManager);
        Assertions.assertEquals("English text1 (test 1)", wrapper.getLabel(KEY_1));
        Assertions.assertEquals(KEY_2, wrapper.getLabel(KEY_2));
        Assertions.assertEquals(KEY_3, wrapper.getLabel(KEY_3));
    }

    @Test
    void testLabelWithReqCtx() throws Throwable {
        I18nManagerWrapper wrapper = new I18nManagerWrapper("it", i18nManager, this.getRequestContext());
        Assertions.assertEquals("Testo1 Italiano (test 1)", wrapper.getLabel(KEY_1));
        Assertions.assertEquals("Testo2 Italiano (test 2)", wrapper.getLabel(KEY_2));
        Assertions.assertEquals(KEY_3, wrapper.getLabel(KEY_3));

        wrapper = new I18nManagerWrapper("en", i18nManager, this.getRequestContext());
        Assertions.assertEquals("English text1 (test 1)", wrapper.getLabel(KEY_1));
        Assertions.assertEquals("Testo2 Italiano (test 2)", wrapper.getLabel(KEY_2));
        Assertions.assertEquals(KEY_3, wrapper.getLabel(KEY_3));
    }

    @Test
    void testLabelWithParam() throws Throwable {
        I18nManagerWrapper wrapper = new I18nManagerWrapper("en", i18nManager);
        Assertions.assertEquals("Welcome ${surname} ${name} (${username} - ${name}.${surname})", wrapper.getLabel("LABEL_WITH_PARAMS"));
    }

}
