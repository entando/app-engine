package org.entando.entando.aps.system.services.entity.model;

import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.TextAttribute;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EntityAttributeDtoTest {

    @Test
    void testFillEntityAttributeBoolean() {
        EntityAttributeDto entityAttributeDto;

        BooleanAttribute attribute = new BooleanAttribute();
        entityAttributeDto = new EntityAttributeDto(attribute);

        entityAttributeDto.setValue("true");
        entityAttributeDto.fillEntityAttribute(attribute, null);
        Assertions.assertTrue(attribute.getBooleanValue());

        entityAttributeDto.setValue("false");
        entityAttributeDto.fillEntityAttribute(attribute, null);
        Assertions.assertFalse(attribute.getBooleanValue());

        entityAttributeDto.setValue(true);
        entityAttributeDto.fillEntityAttribute(attribute, null);
        Assertions.assertTrue(attribute.getBooleanValue());

        entityAttributeDto.setValue(false);
        entityAttributeDto.fillEntityAttribute(attribute, null);
        Assertions.assertFalse(attribute.getBooleanValue());
    }

    @Test
    void testFillEntityAttributeTextWithNullValueRemovesTheLanguage() {
        TextAttribute attribute = new TextAttribute();
        attribute.setText("Text EN", "en");
        attribute.setText("Text IT", "it");
        EntityAttributeDto entityAttributeDto = new EntityAttributeDto();
        Map<String, Object> values = new HashMap<>();
        values.put("en", "New EN");
        values.put("it", null);
        entityAttributeDto.setValues(values);

        Assertions.assertDoesNotThrow(() -> entityAttributeDto.fillEntityAttribute(attribute, null));

        Assertions.assertEquals("New EN", attribute.getTextForLang("en"));
        Assertions.assertFalse(attribute.getTextMap().containsKey("it"));
    }

    @Test
    void testFillEntityAttributeTextIgnoresMapValue() {
        TextAttribute attribute = new TextAttribute();
        EntityAttributeDto entityAttributeDto = new EntityAttributeDto();
        Map<String, Object> values = new HashMap<>();
        values.put("en", Map.of("id", "44"));
        values.put("it", "Text IT");
        entityAttributeDto.setValues(values);

        entityAttributeDto.fillEntityAttribute(attribute, null);

        Assertions.assertFalse(attribute.getTextMap().containsKey("en"));
        Assertions.assertEquals("Text IT", attribute.getTextForLang("it"));
    }
}
