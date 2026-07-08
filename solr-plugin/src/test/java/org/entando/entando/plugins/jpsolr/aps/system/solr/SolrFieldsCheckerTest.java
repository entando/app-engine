/*
 * Copyright 2021-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpsolr.aps.system.solr;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.services.lang.Lang;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SolrFieldsCheckerTest {

    private Lang lang(String code) {
        Lang lang = new Lang();
        lang.setCode(code);
        lang.setDescr(code);
        return lang;
    }

    @Test
    void shouldCreateMainAndAttachmentFieldPerLanguage() {
        // No pre-existing fields, no content-type attributes: only the base + per-language
        // fields are generated. Each language must yield BOTH the main "<lang>" field and the
        // "<lang>_attachment" field, so a full-text search with includeAttachments=true never
        // queries a field the schema doesn't know about.
        SolrFieldsChecker checker = new SolrFieldsChecker(
                Collections.emptyList(),
                Collections.<AttributeInterface>emptyList(),
                List.of(lang("en"), lang("it")));

        List<String> createdFieldNames = checker.checkFields().getFieldsToAdd().stream()
                .map(field -> (String) field.get(SolrFields.SOLR_FIELD_NAME))
                .collect(Collectors.toList());

        assertTrue(createdFieldNames.contains("en"), createdFieldNames.toString());
        assertTrue(createdFieldNames.contains("it"), createdFieldNames.toString());
        assertTrue(createdFieldNames.contains("en" + SolrFields.ATTACHMENT_FIELD_SUFFIX),
                createdFieldNames.toString());
        assertTrue(createdFieldNames.contains("it" + SolrFields.ATTACHMENT_FIELD_SUFFIX),
                createdFieldNames.toString());
    }

    @Test
    void attachmentFieldMatchesMainLanguageFieldTypeAndMultiplicity() {
        SolrFieldsChecker checker = new SolrFieldsChecker(
                Collections.emptyList(),
                Collections.<AttributeInterface>emptyList(),
                List.of(lang("en")));

        List<Map<String, ?>> added = checker.checkFields().getFieldsToAdd();
        Map<String, ?> main = added.stream()
                .filter(f -> "en".equals(f.get(SolrFields.SOLR_FIELD_NAME))).findFirst().orElseThrow();
        Map<String, ?> attachment = added.stream()
                .filter(f -> ("en" + SolrFields.ATTACHMENT_FIELD_SUFFIX).equals(f.get(SolrFields.SOLR_FIELD_NAME)))
                .findFirst().orElseThrow();

        Assertions.assertEquals(main.get(SolrFields.SOLR_FIELD_TYPE),
                attachment.get(SolrFields.SOLR_FIELD_TYPE));
        Assertions.assertEquals(main.get(SolrFields.SOLR_FIELD_MULTIVALUED),
                attachment.get(SolrFields.SOLR_FIELD_MULTIVALUED));
    }
}
