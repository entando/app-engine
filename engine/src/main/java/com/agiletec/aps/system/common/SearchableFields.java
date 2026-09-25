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
package com.agiletec.aps.system.common;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The search keys a searcher DAO accepts, and the column each one names.
 *
 * <p>A filter key becomes a column name by concatenation, so the set of keys a searcher accepts is
 * also the bound on what can reach the SQL. Declaring it as data - rather than as a chain of
 * comparisons inside each DAO - is what lets {@link AbstractSearcherDAO} apply the same check to
 * every searcher, and lets a searcher whose keys differ from its columns say so instead of relying
 * on the two happening to coincide.</p>
 *
 * <p>Keys are matched without regard to case, because the keys callers send are DTO field names
 * (<code>pluginCode</code>) while columns are spelled as the schema declares them
 * (<code>plugincode</code>), and every database the engine supports folds unquoted identifiers. What
 * a lookup returns is always the column as declared here, never as the caller spelled it.</p>
 *
 * @author E.Santoboni
 */
public final class SearchableFields {

    private final Map<String, String> columnsByKey;

    private SearchableFields(Map<String, String> columnsByKey) {
        this.columnsByKey = columnsByKey;
    }

    /**
     * Fields whose key is the column name.
     *
     * @param columns The columns, as literals - never caller input.
     * @return The fields those columns make up.
     */
    public static SearchableFields columns(String... columns) {
        Map<String, String> columnsByKey = new HashMap<>();
        for (String column : columns) {
            columnsByKey.put(normalize(column), column);
        }
        return new SearchableFields(columnsByKey);
    }

    /**
     * These fields, plus a key naming a column spelled differently - a logical key such as
     * <code>entityId</code>, or one the REST layer exposes under another name.
     *
     * @param searchKey The key callers use.
     * @param column The column it names, as a literal - never caller input.
     * @return The fields, with that key added.
     */
    public SearchableFields alias(String searchKey, String column) {
        Map<String, String> widened = new HashMap<>(this.columnsByKey);
        widened.put(normalize(searchKey), column);
        return new SearchableFields(widened);
    }

    /**
     * The column a key names.
     *
     * @param searchKey The key supplied by the caller.
     * @return The column, as declared here, or null when the key is not one of these fields.
     */
    String columnFor(String searchKey) {
        return (null == searchKey) ? null : this.columnsByKey.get(normalize(searchKey));
    }

    private static String normalize(String searchKey) {
        return searchKey.toLowerCase(Locale.ROOT);
    }

}
