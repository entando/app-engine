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
package org.entando.entando.aps.system.common.entity.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CheckBoxAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.DateAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoTextAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.NumberAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.ThreeStateAttribute;
import org.junit.jupiter.api.Test;

/**
 * The contract the finder JSPs bind to. A form control is chosen from the attribute's declared
 * {@link SearchFieldType}, not from a hardcoded list of type codes - so the same predicate answers for a
 * type the platform does not ship, and there is no code list to keep in sync across three JSPs.
 */
class SearchableAttributeRefTest {

    @Test
    void shouldOfferTheBooleanFamilyOneControlAndSingleOutTheThreeStateOne() {
        assertTrue(ref(new BooleanAttribute()).isBooleanLike());
        assertFalse(ref(new BooleanAttribute()).isTristate());
        assertTrue(ref(new CheckBoxAttribute()).isBooleanLike());
        assertFalse(ref(new CheckBoxAttribute()).isTristate());
        // ThreeState shares the control but needs its fourth "not set" option
        assertTrue(ref(new ThreeStateAttribute()).isBooleanLike());
        assertTrue(ref(new ThreeStateAttribute()).isTristate());
    }

    @Test
    void shouldSeparateDateNumberAndText() {
        SearchableAttributeRef date = ref(new DateAttribute());
        assertTrue(date.isDate());
        assertFalse(date.isNumber());
        assertFalse(date.isBooleanLike());

        SearchableAttributeRef number = ref(new NumberAttribute());
        assertTrue(number.isNumber());
        assertFalse(number.isDate());

        SearchableAttributeRef text = ref(new MonoTextAttribute());
        assertTrue(text.isTextAttribute());
        assertFalse(text.isDate());
        assertFalse(text.isNumber());
        assertFalse(text.isBooleanLike());
        // TEXT is wider than the text filter's audience, so the text control keeps asking
        // isTextAttribute() instead of the field type
        assertEquals(SearchFieldType.TEXT, text.getSearchFieldType());
    }

    @Test
    void aCustomBooleanFamilyTypeShouldGetAControlWithoutAnyJspEdit() {
        // the end-to-end extension point: an attribute type that opts into nested search and declares
        // BOOLEAN is indexed, validated, given a Solr field - and now also rendered, because no JSP asks
        // for its type code
        SearchableAttributeRef custom = ref(new CustomFlagAttribute());
        assertTrue(custom.isBooleanLike());
        assertFalse(custom.isTristate());
        assertEquals("CustomFlag", custom.getType());
    }

    @Test
    void shouldExposeTheMachineKeyAsTheFormFieldName() {
        BooleanAttribute nested = new BooleanAttribute();
        nested.setName("highlighted");
        SearchableAttributeRef ref = new SearchableAttributeRef("configuration_highlighted",
                "configuration > highlighted", nested);
        // the form field is named after the key, never after the attribute's own name
        assertEquals("configuration_highlighted", ref.getName());
        assertEquals("configuration > highlighted", ref.label());
        // getLabel(), not label(): OGNL cannot see a record accessor, which is what the parallel
        // key -> label map used to work around
        assertEquals("configuration > highlighted", ref.getLabel());
    }

    @Test
    void shouldFallBackToTheKeyWhenThereIsNoLabel() {
        // absorbs the "label != null ? label : name" ternary the finder JSPs used to carry
        assertEquals("title", new SearchableAttributeRef("title", null, new MonoTextAttribute()).getLabel());
    }

    /** A custom type in the boolean family without extending any shipped boolean class. */
    private static class CustomFlagAttribute extends MonoTextAttribute {

        @Override
        public String getType() {
            return "CustomFlag";
        }

        @Override
        public boolean isNestedSearchSupported() {
            return true;
        }

        @Override
        public SearchFieldType getSearchFieldType() {
            return SearchFieldType.BOOLEAN;
        }
    }

    private SearchableAttributeRef ref(AttributeInterface attribute) {
        attribute.setName("attr");
        return new SearchableAttributeRef("attr", "attr", attribute);
    }

}
