package org.entando.entando.plugins.jpsolr.aps.system.solr.model;

import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CheckBoxAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.TextAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.ThreeStateAttribute;
import com.agiletec.aps.system.services.lang.Lang;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentTypeSettingsTest {

    @Test
    void shouldDetectMissingLangField() {

        ContentTypeSettings contentTypeSettings = new ContentTypeSettings("NWS", "News");

        TextAttribute titleAttribute = new TextAttribute();
        titleAttribute.setName("title");
        titleAttribute.setType("Text");

        Map<String, Map<String, Serializable>> currentField = Map.of("en_title", Map.of(
                "name", "en_title",
                "type", "text_gen_sort",
                "multiValued", false
        ));

        contentTypeSettings.addAttribute(titleAttribute, currentField, getLanguages("en", "it"));

        Assertions.assertFalse(contentTypeSettings.isValid());
    }

    @Test
    void shouldDetectLangMismatchInField() {

        ContentTypeSettings contentTypeSettings = new ContentTypeSettings("NWS", "News");

        TextAttribute titleAttribute = new TextAttribute();
        titleAttribute.setName("title");
        titleAttribute.setType("Text");

        Map<String, Map<String, Serializable>> currentField = Map.of("en_title", Map.of(
                "name", "en_title",
                "type", "text_gen_sort",
                "multiValued", false
        ));

        contentTypeSettings.addAttribute(titleAttribute, currentField, getLanguages("es"));

        Assertions.assertFalse(contentTypeSettings.isValid());
    }

    @Test
    void shouldDetectValidLangField() {

        ContentTypeSettings contentTypeSettings = new ContentTypeSettings("NWS", "News");

        TextAttribute titleAttribute = new TextAttribute();
        titleAttribute.setName("title");
        titleAttribute.setType("Text");

        Map<String, Map<String, Serializable>> currentField = Map.of("en_title", Map.of(
                "name", "en_title",
                "type", "text_gen_sort",
                "multiValued", false
        ));

        contentTypeSettings.addAttribute(titleAttribute, currentField, getLanguages("en"));

        Assertions.assertTrue(contentTypeSettings.isValid());
    }

    @Test
    void shouldExpectBooleanTypeForSearchableBooleanAttribute() {

        ContentTypeSettings contentTypeSettings = new ContentTypeSettings("NWS", "News");

        BooleanAttribute flagAttribute = new BooleanAttribute();
        flagAttribute.setName("flag");
        flagAttribute.setType("Boolean");
        flagAttribute.setSearchable(true);

        Map<String, Map<String, Serializable>> currentField = Map.of("en_flag", Map.of(
                "name", "en_flag",
                "type", "boolean",
                "multiValued", false
        ));

        contentTypeSettings.addAttribute(flagAttribute, currentField, getLanguages("en"));

        Assertions.assertTrue(contentTypeSettings.isValid());
    }

    @Test
    void shouldDetectFieldTypeMismatchForBooleanAttribute() {

        ContentTypeSettings contentTypeSettings = new ContentTypeSettings("NWS", "News");

        BooleanAttribute flagAttribute = new BooleanAttribute();
        flagAttribute.setName("flag");
        flagAttribute.setType("Boolean");
        flagAttribute.setSearchable(true);

        Map<String, Map<String, Serializable>> currentField = Map.of("en_flag", Map.of(
                "name", "en_flag",
                "type", "text_gen_sort",
                "multiValued", false
        ));

        contentTypeSettings.addAttribute(flagAttribute, currentField, getLanguages("en"));

        Assertions.assertFalse(contentTypeSettings.isValid());
    }

    @Test
    void shouldNotExpectAnyFieldForNonSearchableBooleanAttribute() {

        ContentTypeSettings contentTypeSettings = new ContentTypeSettings("NWS", "News");

        BooleanAttribute flagAttribute = new BooleanAttribute();
        flagAttribute.setName("flag");
        flagAttribute.setType("Boolean");
        flagAttribute.setSearchable(false);

        contentTypeSettings.addAttribute(flagAttribute, Map.of(), getLanguages("en"));

        Assertions.assertTrue(contentTypeSettings.isValid());
    }

    @Test
    void shouldExpectBooleanTypeForSearchableCheckBoxAttribute() {

        ContentTypeSettings contentTypeSettings = new ContentTypeSettings("NWS", "News");

        CheckBoxAttribute checkAttribute = new CheckBoxAttribute();
        checkAttribute.setName("check");
        checkAttribute.setType("CheckBox");
        checkAttribute.setSearchable(true);

        Map<String, Map<String, Serializable>> currentField = Map.of("en_check", Map.of(
                "name", "en_check",
                "type", "boolean",
                "multiValued", false
        ));

        contentTypeSettings.addAttribute(checkAttribute, currentField, getLanguages("en"));

        Assertions.assertTrue(contentTypeSettings.isValid());
    }

    @Test
    void shouldExpectStringTypeForSearchableThreeStateAttribute() {
        // ThreeState's third, uninitialized state cannot be represented in a two-valued Solr
        // BoolField, so it is expected as "string" (true|false|none), not "boolean".

        ContentTypeSettings contentTypeSettings = new ContentTypeSettings("NWS", "News");

        ThreeStateAttribute flag3Attribute = new ThreeStateAttribute();
        flag3Attribute.setName("flag3");
        flag3Attribute.setType("ThreeState");
        flag3Attribute.setSearchable(true);

        Map<String, Map<String, Serializable>> currentField = Map.of("en_flag3", Map.of(
                "name", "en_flag3",
                "type", "string",
                "multiValued", false
        ));

        contentTypeSettings.addAttribute(flag3Attribute, currentField, getLanguages("en"));

        Assertions.assertTrue(contentTypeSettings.isValid());
    }

    @Test
    void shouldNotExpectFieldForUnsupportedAttributeType() {
        // Closes the final "instanceof BooleanAttribute" false outcome of addAttribute's dispatch
        // condition: an attribute that is neither IndexableAttributeInterface, Date, Number nor
        // Boolean must not get an expected field configuration (isValid() trivially true).

        ContentTypeSettings contentTypeSettings = new ContentTypeSettings("NWS", "News");

        AttributeInterface unsupported = mock(AttributeInterface.class);
        when(unsupported.getName()).thenReturn("unsupported");
        when(unsupported.getType()).thenReturn("Unsupported");

        contentTypeSettings.addAttribute(unsupported, Map.of(), getLanguages("en"));

        Assertions.assertTrue(contentTypeSettings.isValid());
    }

    @Test
    void shouldExpectSingleValuedBooleanTypeForNestedCheckBoxAttribute() {
        // Single-valued: a Composite occurs at most once per document per lang (the caller
        // excludes List/Monolist ancestry), so the nested field can never repeat.

        ContentTypeSettings contentTypeSettings = new ContentTypeSettings("NWS", "News");

        CheckBoxAttribute nestedCheck = new CheckBoxAttribute();
        nestedCheck.setName("featuredCheck");
        nestedCheck.setType("CheckBox");
        nestedCheck.setSearchable(true);

        Map<String, Map<String, Serializable>> currentField = Map.of("en_compo_featuredCheck", Map.of(
                "name", "en_compo_featuredCheck",
                "type", "boolean",
                "multiValued", false
        ));

        contentTypeSettings.addNestedSearchAttribute(nestedCheck, "compo_featuredCheck", currentField,
                getLanguages("en"));

        Assertions.assertTrue(contentTypeSettings.isValid());
        // reported under the path, which is what the schema field and the index use
        Assertions.assertEquals("compo_featuredCheck",
                contentTypeSettings.getAttributeSettings().get(0).getCode());
    }

    @Test
    void shouldExpectSingleValuedStringTypeForNestedThreeStateAttribute() {

        ContentTypeSettings contentTypeSettings = new ContentTypeSettings("NWS", "News");

        ThreeStateAttribute nestedFlag3 = new ThreeStateAttribute();
        nestedFlag3.setName("featured3");
        nestedFlag3.setType("ThreeState");
        nestedFlag3.setSearchable(true);

        Map<String, Map<String, Serializable>> currentField = Map.of("en_compo_featured3", Map.of(
                "name", "en_compo_featured3",
                "type", "string",
                "multiValued", false
        ));

        contentTypeSettings.addNestedSearchAttribute(nestedFlag3, "compo_featured3", currentField,
                getLanguages("en"));

        Assertions.assertTrue(contentTypeSettings.isValid());
    }

    @Test
    void shouldExpectSingleValuedBooleanTypeForNestedBooleanAttribute() {

        ContentTypeSettings contentTypeSettings = new ContentTypeSettings("NWS", "News");

        BooleanAttribute nestedFlag = new BooleanAttribute();
        nestedFlag.setName("featured");
        nestedFlag.setType("Boolean");
        nestedFlag.setSearchable(true);

        Map<String, Map<String, Serializable>> currentField = Map.of("en_compo_featured", Map.of(
                "name", "en_compo_featured",
                "type", "boolean",
                "multiValued", false
        ));

        contentTypeSettings.addNestedSearchAttribute(nestedFlag, "compo_featured", currentField,
                getLanguages("en"));

        Assertions.assertTrue(contentTypeSettings.isValid());
    }

    @Test
    void shouldDetectMissingNestedBooleanField() {

        ContentTypeSettings contentTypeSettings = new ContentTypeSettings("NWS", "News");

        BooleanAttribute nestedFlag = new BooleanAttribute();
        nestedFlag.setName("featured");
        nestedFlag.setType("Boolean");
        nestedFlag.setSearchable(true);

        contentTypeSettings.addNestedSearchAttribute(nestedFlag, "compo_featured", Map.of(),
                getLanguages("en"));

        Assertions.assertFalse(contentTypeSettings.isValid());
    }

    @Test
    void shouldDetectMultiValuedNestedBooleanFieldAsInvalid() {

        ContentTypeSettings contentTypeSettings = new ContentTypeSettings("NWS", "News");

        BooleanAttribute nestedFlag = new BooleanAttribute();
        nestedFlag.setName("featured");
        nestedFlag.setType("Boolean");
        nestedFlag.setSearchable(true);

        // multiValued=true on a nested boolean field is stale (nested fields are always
        // single-valued); the schema must be refreshed to a single-valued field.
        Map<String, Map<String, Serializable>> currentField = Map.of("en_compo_featured", Map.of(
                "name", "en_compo_featured",
                "type", "boolean",
                "multiValued", true
        ));

        contentTypeSettings.addNestedSearchAttribute(nestedFlag, "compo_featured", currentField,
                getLanguages("en"));

        Assertions.assertFalse(contentTypeSettings.isValid());
    }

    private List<Lang> getLanguages(String... codes) {
        return Arrays.stream(codes).map(c -> {
            Lang lang = new Lang();
            lang.setCode(c);
            return lang;
        }).collect(Collectors.toList());
    }
}
