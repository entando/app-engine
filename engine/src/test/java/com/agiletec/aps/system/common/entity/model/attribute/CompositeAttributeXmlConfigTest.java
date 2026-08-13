/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package com.agiletec.aps.system.common.entity.model.attribute;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.common.entity.parse.attribute.BooleanAttributeHandler;
import com.agiletec.aps.system.common.entity.parse.attribute.TextAttributeHandler;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import java.util.Collections;
import java.util.Map;
import org.jdom2.Element;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link CompositeAttribute#setComplexAttributeConfig} - in particular the new rule that a
 * boolean-like composite child (Boolean, CheckBox, ThreeState) may keep its configured
 * {@code searchable} flag, while every other type is forced non-searchable regardless of what the XML
 * config says.
 */
class CompositeAttributeXmlConfigTest {

    @Test
    void booleanChildKeepsConfiguredSearchableFlag() throws Exception {
        CompositeAttribute composite = parseComposite(
                childElement("Boolean", "certified", true), Map.of("Boolean", booleanPrototype()));

        AttributeInterface child = composite.getAttributeMap().get("certified");
        assertTrue(child.isSearchable());
    }

    @Test
    void checkBoxChildKeepsConfiguredSearchableFlag() throws Exception {
        CompositeAttribute composite = parseComposite(
                childElement("CheckBox", "archived", true),
                Map.of("CheckBox", booleanLikePrototype(new CheckBoxAttribute(), "CheckBox")));

        AttributeInterface child = composite.getAttributeMap().get("archived");
        assertTrue(child.isSearchable());
    }

    @Test
    void threeStateChildKeepsConfiguredSearchableFlag() throws Exception {
        CompositeAttribute composite = parseComposite(
                childElement("ThreeState", "reviewed", true),
                Map.of("ThreeState", booleanLikePrototype(new ThreeStateAttribute(), "ThreeState")));

        AttributeInterface child = composite.getAttributeMap().get("reviewed");
        assertTrue(child.isSearchable());
    }

    @Test
    void booleanChildWithoutTheFlagStaysNonSearchable() throws Exception {
        CompositeAttribute composite = parseComposite(
                childElement("Boolean", "certified", false), Map.of("Boolean", booleanPrototype()));

        AttributeInterface child = composite.getAttributeMap().get("certified");
        assertFalse(child.isSearchable());
    }

    @Test
    void nonBooleanChildIsForcedNonSearchable() throws Exception {
        CompositeAttribute composite = parseComposite(
                childElement("Text", "note", true), Map.of("Text", textPrototype()));

        AttributeInterface child = composite.getAttributeMap().get("note");
        assertFalse(child.isSearchable());
    }

    // --- fixtures ------------------------------------------------------

    private CompositeAttribute parseComposite(Element childElement, Map<String, AttributeInterface> attrTypes)
            throws Exception {
        Element attributesWrapper = new Element("attributes");
        attributesWrapper.addContent(childElement);
        Element compositeElement = new Element("composite");
        compositeElement.addContent(attributesWrapper);

        CompositeAttribute composite = new CompositeAttribute();
        composite.setName("address");
        composite.setComplexAttributeConfig(compositeElement, attrTypes);
        return composite;
    }

    private Element childElement(String attributeType, String name, boolean searchable) {
        Element element = new Element(attributeType.toLowerCase());
        element.setAttribute("attributetype", attributeType);
        element.setAttribute("name", name);
        element.setAttribute("searchable", String.valueOf(searchable));
        return element;
    }

    private BooleanAttribute booleanPrototype() {
        return booleanLikePrototype(new BooleanAttribute(), "Boolean");
    }

    private BooleanAttribute booleanLikePrototype(BooleanAttribute attribute, String type) {
        attribute.setType(type);
        attribute.setHandler(new BooleanAttributeHandler());
        attribute.setLangManager(langManager());
        return attribute;
    }

    private TextAttribute textPrototype() {
        TextAttribute attribute = new TextAttribute();
        attribute.setType("Text");
        attribute.setHandler(new TextAttributeHandler());
        attribute.setLangManager(langManager());
        return attribute;
    }

    private ILangManager langManager() {
        ILangManager langManager = mock(ILangManager.class);
        Lang en = new Lang();
        en.setCode("en");
        en.setDescr("English");
        when(langManager.getDefaultLang()).thenReturn(en);
        when(langManager.getLangs()).thenReturn(Collections.singletonList(en));
        return langManager;
    }

}
