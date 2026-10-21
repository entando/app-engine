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
package com.agiletec.plugins.jacms.aps.system.services.content.model;

import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.ImageAttribute;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.AttachResource;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ResourceInterface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.entity.model.EntityAttributeDto;
import org.entando.entando.web.entity.validator.EntityValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

class ContentDtoTest {

    private static final String ATTRIBUTE_NAME = "image";

    private Content prototype;
    private ImageAttribute imageAttribute;
    private BindingResult bindingResult;

    @BeforeEach
    void setUp() {
        this.imageAttribute = new ImageAttribute();
        this.imageAttribute.setName(ATTRIBUTE_NAME);
        this.imageAttribute.setType("Image");
        this.imageAttribute.setDefaultLangCode("en");
        this.prototype = new Content();
        this.prototype.setTypeCode("BNR");
        this.prototype.addAttribute(this.imageAttribute);
        this.bindingResult = new BeanPropertyBindingResult(this.prototype, "content");
    }

    @Test
    void fillEntityShouldStoreResourceMetadataByKeyThenLanguage() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("alt", "Alt EN");
        metadata.put("title", "Title EN");
        ContentDto dto = this.createDto(Map.of("en", this.createResourceValue("44", "Image EN", metadata)));

        dto.fillEntity(this.prototype, null, this.bindingResult);

        Assertions.assertFalse(this.bindingResult.hasErrors());
        Assertions.assertEquals("Alt EN", this.imageAttribute.getMetadataForLang("alt", "en"));
        Assertions.assertEquals("Title EN", this.imageAttribute.getMetadataForLang("title", "en"));
        Assertions.assertEquals("Alt EN", this.imageAttribute.getResourceAltForLang("en"));
        Assertions.assertFalse(this.imageAttribute.getMetadatas().containsKey("en"));
    }

    @Test
    void fillEntityShouldKeepStoredMetadataNotCarriedByTheRequest() {
        this.imageAttribute.setResource(this.createResource("44"), "en");
        this.imageAttribute.setText("Image EN", "en");
        this.imageAttribute.setMetadata("alt", "en", "Alt EN");
        this.imageAttribute.setMetadata("title", "en", "Title EN");
        ContentDto dto = this.createDto(Map.of("en", this.createResourceValue("44", "Image EN", null)));

        dto.fillEntity(this.prototype, null, this.bindingResult);

        Assertions.assertFalse(this.bindingResult.hasErrors());
        Assertions.assertEquals("Alt EN", this.imageAttribute.getMetadataForLang("alt", "en"));
        Assertions.assertEquals("Title EN", this.imageAttribute.getMetadataForLang("title", "en"));
    }

    @Test
    void fillEntityShouldIgnoreNullMetadataValue() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("alt", null);
        metadata.put("title", "Title EN");
        ContentDto dto = this.createDto(Map.of("en", this.createResourceValue("44", "Image EN", metadata)));

        Assertions.assertDoesNotThrow(() -> dto.fillEntity(this.prototype, null, this.bindingResult));

        Assertions.assertFalse(this.bindingResult.hasErrors());
        Assertions.assertNull(this.imageAttribute.getMetadataForLang("alt", "en"));
        Assertions.assertEquals("Title EN", this.imageAttribute.getMetadataForLang("title", "en"));
    }

    @Test
    void fillEntityShouldRejectNonStringMetadataValue() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("alt", 5);
        ContentDto dto = this.createDto(Map.of("en", this.createResourceValue("44", "Image EN", metadata)));

        Assertions.assertDoesNotThrow(() -> dto.fillEntity(this.prototype, null, this.bindingResult));

        Assertions.assertTrue(this.bindingResult.hasErrors());
        Assertions.assertEquals(EntityValidator.ERRCODE_ATTRIBUTE_INVALID,
                this.bindingResult.getAllErrors().get(0).getCode());
    }

    @Test
    void fillEntityShouldNotStoreTheResourceMapAsTextWhenNameIsMissing() {
        ContentDto dto = this.createDto(Map.of("en", this.createResourceValue("44", null, null)));

        dto.fillEntity(this.prototype, null, this.bindingResult);

        String text = this.imageAttribute.getTextForLang("en");
        Assertions.assertTrue(StringUtils.isBlank(text), "unexpected attribute text: " + text);
        Assertions.assertEquals("44", this.imageAttribute.getResource("en").getId());
    }

    @Test
    void fillEntityShouldSkipNullLanguageValue() {
        Map<String, Object> values = new HashMap<>();
        values.put("en", this.createResourceValue("44", "Image EN", null));
        values.put("it", null);
        ContentDto dto = this.createDto(values);

        Assertions.assertDoesNotThrow(() -> dto.fillEntity(this.prototype, null, this.bindingResult));

        Assertions.assertEquals("44", this.imageAttribute.getResource("en").getId());
        Assertions.assertNull(this.imageAttribute.getResource("it"));
    }

    private ContentDto createDto(Map<String, Object> values) {
        EntityAttributeDto attributeDto = new EntityAttributeDto();
        attributeDto.setCode(ATTRIBUTE_NAME);
        attributeDto.setValues(values);
        List<EntityAttributeDto> attributes = new ArrayList<>();
        attributes.add(attributeDto);
        ContentDto dto = new ContentDto();
        dto.setTypeCode("BNR");
        dto.setDescription("description");
        dto.setMainGroup("free");
        dto.setAttributes(attributes);
        return dto;
    }

    private Map<String, Object> createResourceValue(String id, String name, Map<String, Object> metadata) {
        Map<String, Object> value = new HashMap<>();
        value.put("id", id);
        if (null != name) {
            value.put("name", name);
        }
        if (null != metadata) {
            value.put("metadata", metadata);
        }
        return value;
    }

    private ResourceInterface createResource(String id) {
        ResourceInterface resource = new AttachResource();
        resource.setId(id);
        return resource;
    }

}
