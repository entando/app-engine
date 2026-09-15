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
package org.entando.entando.plugins.jpsolr.aps.system.solr.model;

import com.agiletec.aps.system.common.entity.IEntityManager;
import org.entando.entando.aps.system.common.entity.search.SearchFieldType;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;

/**
 * @author E.Santoboni
 */
public final class SolrFields {

    private SolrFields() {
        throw new IllegalStateException("Utility class");
    }

    public static final String SOLR_DATE_MIN = "1900-01-01T01:00:00Z";
    public static final String SOLR_DATE_MAX = "2200-01-01T01:00:00Z";
    public static final String SOLR_SEARCH_DATE_VALUE_FORMAT = "yyyy\\-MM\\-dd'T'HH\\:mm\\:ss'Z'";
    public static final String SOLR_SEARCH_DATE_RANGE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static final String SOLR_FIELD_PREFIX = "entity_";
    public static final String SOLR_CONTENT_ID_FIELD_NAME = "id";
    public static final String SOLR_CONTENT_GROUP_FIELD_NAME = SOLR_FIELD_PREFIX + IContentManager.CONTENT_GROUP_FILTER_KEY;
    public static final String SOLR_CONTENT_CATEGORY_FIELD_NAME = SOLR_FIELD_PREFIX + "category";
    public static final String SOLR_CONTENT_DESCRIPTION_FIELD_NAME = SOLR_FIELD_PREFIX + IContentManager.CONTENT_DESCR_FILTER_KEY;
    public static final String SOLR_CONTENT_LAST_MODIFY_FIELD_NAME = SOLR_FIELD_PREFIX + IContentManager.CONTENT_MODIFY_DATE_FILTER_KEY;
    public static final String SOLR_CONTENT_CREATION_FIELD_NAME = SOLR_FIELD_PREFIX + IContentManager.CONTENT_CREATION_DATE_FILTER_KEY;
    public static final String SOLR_CONTENT_TYPE_CODE_FIELD_NAME = SOLR_FIELD_PREFIX + IEntityManager.ENTITY_TYPE_CODE_FILTER_KEY;
    public static final String SOLR_CONTENT_MAIN_GROUP_FIELD_NAME = SOLR_FIELD_PREFIX + IContentManager.CONTENT_MAIN_GROUP_FILTER_KEY;

    public static final String ATTACHMENT_FIELD_SUFFIX = "_attachment";

    public static final String SOLR_FIELD_MULTIVALUED = "multiValued";
    public static final String SOLR_FIELD_TYPE = "type";
    public static final String SOLR_FIELD_NAME = "name";

    public static final String TYPE_STRING = "string";
    public static final String TYPE_TEXT_GENERAL = "text_general";
    public static final String TYPE_TEXT_GEN_SORT = "text_gen_sort";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_PLONG = "plong";
    public static final String TYPE_PLONGS = "plongs";
    public static final String TYPE_PDATE = "pdate";
    public static final String TYPE_PDATES = "pdates";

    /**
     * The Solr field type for one of the engine's back-end-neutral {@link SearchFieldType}s - the whole
     * of this plugin's knowledge about how an attribute is stored, in one place.
     *
     * <p>{@link SearchFieldType#TRISTATE} maps to a non-analyzed {@code string} rather than to
     * {@code boolean}: its third, unset state has no representation in Solr's two-valued
     * {@code BoolField}, so it is stored as the literal {@code true}/{@code false}/{@code none}. That is
     * the only reason the engine distinguishes it from {@link SearchFieldType#BOOLEAN}, and expressing it
     * as a mapping entry - rather than as an {@code instanceof} ordered subclass-first - is what makes
     * it impossible for a consumer to get wrong.</p>
     *
     * @param searchFieldType the attribute's declared field type; may be null.
     * @return the Solr field type name, or null when the attribute declares no search field.
     */
    public static String solrType(SearchFieldType searchFieldType) {
        if (null == searchFieldType) {
            return null;
        }
        switch (searchFieldType) {
            case DATE:
                return TYPE_PDATES;
            case NUMBER:
                return TYPE_PLONGS;
            case BOOLEAN:
                return TYPE_BOOLEAN;
            case TRISTATE:
                return TYPE_STRING;
            case TEXT:
            default:
                return TYPE_TEXT_GEN_SORT;
        }
    }
}
