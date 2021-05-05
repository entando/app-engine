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

/**
 * @author E.Santoboni
 */
public class SolrFields {

    private SolrFields() {
        throw new IllegalStateException("Utility class");
    }

    public static final String SOLR_DATE_MIN = "1900-01-01T01:00:00Z";
    public static final String SOLR_DATE_MAX = "2200-01-01T01:00:00Z";
    public static final String SOLR_SEARCH_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static final String SOLR_FIELD_PREFIX = "entity_";
    public static final String SOLR_CONTENT_ID_FIELD_NAME = "id";
    public static final String SOLR_CONTENT_TYPE_FIELD_NAME = "entity_type";
    public static final String SOLR_CONTENT_GROUP_FIELD_NAME = "entity_group";
    public static final String SOLR_CONTENT_CATEGORY_FIELD_NAME = "entity_category";
    public static final String SOLR_CONTENT_CATEGORY_SEPARATOR = "_S_";
    public static final String SOLR_SORTERED_FIELD_SUFFIX = "_sort";
    public static final String SOLR_CONTENT_DESCRIPTION_FIELD_NAME = "entity_descr";
    public static final String SOLR_CONTENT_LAST_MODIFY_FIELD_NAME = "entity_modified";
    public static final String SOLR_CONTENT_CREATION_FIELD_NAME = "entity_created";
    public static final String SOLR_CONTENT_TYPE_CODE_FIELD_NAME = "entity_typeCode";
    public static final String SOLR_CONTENT_MAIN_GROUP_FIELD_NAME = "entity_maingroup";
    
    public static final String ATTACHMENT_FIELD_SUFFIX = "_attachment";

}
