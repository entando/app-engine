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

import com.agiletec.aps.system.common.entity.model.attribute.MonoListAttribute;
import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.ImageAttribute;
import com.agiletec.plugins.jacms.aps.system.services.content.parse.attribute.ResourceAttributeHandler;
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

    @Test
    void fillEntityShouldClearStoredMetadataWithEmptyMetadata() {
        this.storeImage(this.imageAttribute, "44", "Alt EN", "Title EN");
        ContentDto dto = this.createDto(Map.of("en", this.createResourceValue("44", "Image EN", new HashMap<>())));

        dto.fillEntity(this.prototype, null, this.bindingResult);

        Assertions.assertFalse(this.bindingResult.hasErrors());
        Assertions.assertNull(this.imageAttribute.getMetadataForLang("alt", "en"));
        Assertions.assertNull(this.imageAttribute.getMetadataForLang("title", "en"));
    }

    @Test
    void fillEntityShouldReplaceStoredMetadataOfTheLanguage() {
        this.storeImage(this.imageAttribute, "44", "Alt EN", "Title EN");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("alt", "New alt");
        ContentDto dto = this.createDto(Map.of("en", this.createResourceValue("44", "Image EN", metadata)));

        dto.fillEntity(this.prototype, null, this.bindingResult);

        Assertions.assertEquals("New alt", this.imageAttribute.getMetadataForLang("alt", "en"));
        Assertions.assertNull(this.imageAttribute.getMetadataForLang("title", "en"));
    }

    @Test
    void fillEntityShouldNotCarryStoredMetadataToAnotherResource() {
        this.storeImage(this.imageAttribute, "44", "Alt EN", "Title EN");
        ContentDto dto = this.createDto(Map.of("en", this.createResourceValue("55", "Other image", null)));

        dto.fillEntity(this.prototype, null, this.bindingResult);

        Assertions.assertEquals("55", this.imageAttribute.getResource("en").getId());
        Assertions.assertNull(this.imageAttribute.getMetadataForLang("alt", "en"));
    }

    @Test
    void fillEntityShouldKeepStoredMetadataOfReorderedMonolistElements() {
        ImageAttribute nestedType = new ImageAttribute();
        nestedType.setName("gallery");
        nestedType.setType("Image");
        nestedType.setHandler(new ResourceAttributeHandler());
        MonoListAttribute gallery = new MonoListAttribute();
        gallery.setName("gallery");
        gallery.setType("Monolist");
        gallery.setDefaultLangCode("en");
        gallery.setNestedAttributeType(nestedType);
        this.storeImage((ImageAttribute) gallery.addAttribute(), "44", "Alt 44", null);
        this.storeImage((ImageAttribute) gallery.addAttribute(), "55", "Alt 55", null);
        this.prototype.addAttribute(gallery);
        EntityAttributeDto galleryDto = new EntityAttributeDto();
        galleryDto.setCode("gallery");
        galleryDto.getElements().add(this.createElementDto("gallery", "55"));
        galleryDto.getElements().add(this.createElementDto("gallery", "44"));
        ContentDto dto = this.createDto(Map.of("en", this.createResourceValue("44", "Image EN", null)));
        dto.getAttributes().add(galleryDto);

        dto.fillEntity(this.prototype, null, this.bindingResult);

        Assertions.assertFalse(this.bindingResult.hasErrors());
        ImageAttribute first = (ImageAttribute) gallery.getAttribute(0);
        ImageAttribute second = (ImageAttribute) gallery.getAttribute(1);
        Assertions.assertEquals("55", first.getResource("en").getId());
        Assertions.assertEquals("Alt 55", first.getMetadataForLang("alt", "en"));
        Assertions.assertEquals("44", second.getResource("en").getId());
        Assertions.assertEquals("Alt 44", second.getMetadataForLang("alt", "en"));
    }

    private void storeImage(ImageAttribute attribute, String resourceId, String alt, String title) {
        attribute.setDefaultLangCode("en");
        attribute.setResource(this.createResource(resourceId), "en");
        attribute.setText("Image " + resourceId, "en");
        attribute.setMetadata("alt", "en", alt);
        if (null != title) {
            attribute.setMetadata("title", "en", title);
        }
    }

    private EntityAttributeDto createElementDto(String code, String resourceId) {
        EntityAttributeDto elementDto = new EntityAttributeDto();
        elementDto.setCode(code);
        Map<String, Object> values = new HashMap<>();
        values.put("en", this.createResourceValue(resourceId, "Image " + resourceId, null));
        elementDto.setValues(values);
        return elementDto;
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
