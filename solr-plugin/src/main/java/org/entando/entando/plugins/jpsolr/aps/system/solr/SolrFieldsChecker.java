package org.entando.entando.plugins.jpsolr.aps.system.solr;

import static org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields.SOLR_FIELD_MULTIVALUED;
import static org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields.SOLR_FIELD_NAME;
import static org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields.SOLR_FIELD_TYPE;

import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.DateAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.NumberAttribute;
import com.agiletec.aps.system.common.searchengine.IndexableAttributeInterface;
import com.agiletec.aps.system.services.lang.Lang;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields;

@Slf4j
public class SolrFieldsChecker {

    private final List<Map<String, ?>> fields;
    private final List<AttributeInterface> attributes;
    private final List<Lang> languages;
    private final CheckFieldsResult schemaUpdateRequest = new CheckFieldsResult();

    public SolrFieldsChecker(List<Map<String, ?>> fields, List<AttributeInterface> attributes,
            List<Lang> languages) {
        this.fields = fields;
        this.attributes = attributes;
        this.languages = languages;
    }

    public static class CheckFieldsResult {

        private final Map<String, Map<String, ?>> fieldsToAdd = new HashMap<>();
        private final Map<String, Map<String, ?>> fieldsToReplace = new HashMap<>();

        void addField(String fieldName, Map<String, ?> fieldToAdd) {
            this.fieldsToAdd.put(fieldName, fieldToAdd);
        }

        void replaceField(String fieldName, Map<String, ?> fieldToAdd) {
            this.fieldsToReplace.put(fieldName, fieldToAdd);
        }

        public List<Map<String, ?>> getFieldsToAdd() {
            return new ArrayList<>(fieldsToAdd.values());
        }

        public List<Map<String, ?>> getFieldsToReplace() {
            return new ArrayList<>(fieldsToReplace.values());
        }

        public boolean needsUpdate() {
            return !fieldsToAdd.isEmpty() || !fieldsToReplace.isEmpty();
        }
    }

    public CheckFieldsResult checkFields() {
        this.checkLangFields();
        this.refreshBaseFields();
        for (AttributeInterface attribute : this.attributes) {
            for (Lang language : this.languages) {
                this.checkAttribute(attribute, language);
            }
        }
        return schemaUpdateRequest;
    }

    private void refreshBaseFields() {
        this.checkField(SolrFields.SOLR_CONTENT_ID_FIELD_NAME, SolrFields.TYPE_STRING);
        this.checkField(SolrFields.SOLR_CONTENT_TYPE_CODE_FIELD_NAME, SolrFields.TYPE_TEXT_GENERAL);
        this.checkField(SolrFields.SOLR_CONTENT_GROUP_FIELD_NAME, SolrFields.TYPE_TEXT_GENERAL, true);
        this.checkField(SolrFields.SOLR_CONTENT_DESCRIPTION_FIELD_NAME, SolrFields.TYPE_TEXT_GEN_SORT);
        this.checkField(SolrFields.SOLR_CONTENT_MAIN_GROUP_FIELD_NAME, SolrFields.TYPE_TEXT_GENERAL);
        this.checkField(SolrFields.SOLR_CONTENT_CATEGORY_FIELD_NAME, SolrFields.TYPE_TEXT_GENERAL, true);
        this.checkField(SolrFields.SOLR_CONTENT_CREATION_FIELD_NAME, SolrFields.TYPE_PDATES, false);
        this.checkField(SolrFields.SOLR_CONTENT_LAST_MODIFY_FIELD_NAME, SolrFields.TYPE_PDATES, false);
    }

    private void checkLangFields() {
        for (Lang lang : this.languages) {
            this.checkField(lang.getCode(), SolrFields.TYPE_TEXT_GENERAL, true);
            // The per-language attachment field ("<lang>_attachment") is written by IndexerDAO
            // and queried by SearcherDAO when includeAttachments=true. It must exist in the
            // schema up front; otherwise a full-text search with includeAttachments on an
            // environment where no attachment content has been indexed queries an unknown field
            // and fails. Same type/multiplicity as the main language field.
            this.checkField(lang.getCode() + SolrFields.ATTACHMENT_FIELD_SUFFIX,
                    SolrFields.TYPE_TEXT_GENERAL, true);
        }
    }

    private void checkAttribute(AttributeInterface attribute, Lang lang) {
        attribute.setRenderingLang(lang.getCode());
        if (attribute instanceof IndexableAttributeInterface
                || ((attribute instanceof DateAttribute || attribute instanceof NumberAttribute)
                && attribute.isSearchable())) {
            String type;
            if (attribute instanceof DateAttribute) {
                type = SolrFields.TYPE_PDATES;
            } else if (attribute instanceof NumberAttribute) {
                type = SolrFields.TYPE_PLONGS;
            } else if (attribute instanceof BooleanAttribute) {
                type = SolrFields.TYPE_BOOLEAN;
            } else {
                type = SolrFields.TYPE_TEXT_GEN_SORT;
            }
            String fieldName = lang.getCode().toLowerCase() + "_" + attribute.getName();
            fieldName = fieldName.replace(":", "_");
            this.checkField(fieldName, type);
            if (null == attribute.getRoles()) {
                return;
            }
            for (String role : attribute.getRoles()) {
                String roleFieldName = lang.getCode().toLowerCase() + "_" + role;
                roleFieldName = roleFieldName.replace(":", "_");
                this.checkField(roleFieldName, type);
            }
        }
    }

    private void checkField(String fieldName, String type) {
        this.checkField(fieldName, type, false);
    }

    private void checkField(String fieldName, String type, boolean multiValue) {
        Map<String, Object> newField = new HashMap<>();
        newField.put(SOLR_FIELD_NAME, fieldName);
        newField.put(SOLR_FIELD_TYPE, type);
        newField.put(SOLR_FIELD_MULTIVALUED, multiValue);

        fields.stream()
                .filter(f -> f.get(SOLR_FIELD_NAME).equals(fieldName))
                .findFirst().ifPresentOrElse(
                        currentField -> replaceField(currentField, newField, fieldName, type, multiValue),
                        () -> schemaUpdateRequest.addField(fieldName, newField));
    }

    private void replaceField(Map<String, ?> currentField, Map<String, ?> newField,
            String fieldName, String type, boolean multiValue) {
        if (currentField.get(SOLR_FIELD_TYPE).equals(type)
                && ((null == currentField.get(SOLR_FIELD_MULTIVALUED) && multiValue) || (
                null != currentField.get(SOLR_FIELD_MULTIVALUED) && currentField.get(SOLR_FIELD_MULTIVALUED)
                        .equals(multiValue)))) {
            return;
        } else {
            log.warn(
                    "Field '{}' already exists but with different configuration! - type '{}' to '{}' - multiValued '{}' to '{}'",
                    fieldName, currentField.get(SOLR_FIELD_TYPE), type, currentField.get(SOLR_FIELD_MULTIVALUED),
                    multiValue);
        }
        if (!type.equals(currentField.get(SOLR_FIELD_TYPE)) || !Boolean.valueOf(multiValue)
                .equals(currentField.get(SOLR_FIELD_MULTIVALUED))) {
            schemaUpdateRequest.replaceField(fieldName, newField);
        }
    }
}
