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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Reads the parts of a generated query that carry the contract, so that a shape assertion does not
 * also assert whitespace or formatting. The count markers are the DAO's own constants: a test that
 * pins the composition has to move when they move.
 */
public final class SqlShape {

    public static final String COUNT_PREFIX = AbstractSearcherDAO.COUNT_QUERY_PREFIX;
    public static final String COUNT_SUFFIX = AbstractSearcherDAO.COUNT_QUERY_SUFFIX;

    private static final String ORDER_BY = " ORDER BY ";
    private static final String GROUP_BY = "GROUP BY ";
    /** The paging block, in either of the two syntaxes {@link QueryLimitResolver} emits. */
    private static final String[] PAGING = {" OFFSET ", " LIMIT "};
    /** A trailing sort direction. Possessive so a run of spaces cannot backtrack. */
    private static final Pattern SORT_DIRECTION = Pattern.compile("(?i)\\s++(ASC|DESC)$");

    private SqlShape() {
        // utility
    }

    /**
     * @param sql Any generated query.
     * @return The same query with every run of whitespace reduced to one space.
     */
    public static String normalize(String sql) {
        return (null == sql) ? null : sql.replaceAll("\\s+", " ").trim();
    }

    public static int occurrences(String sql, String marker) {
        return StringUtils.countMatches(normalize(sql), normalize(marker));
    }

    public static boolean isCountQuery(String sql) {
        return normalize(sql).startsWith(normalize(COUNT_PREFIX));
    }

    /**
     * @param countQuery A count query.
     * @return The block the count counts: everything the wrapper encloses.
     */
    public static String countBody(String countQuery) {
        String query = normalize(countQuery);
        String prefix = normalize(COUNT_PREFIX);
        String suffix = normalize(COUNT_SUFFIX);
        if (!query.startsWith(prefix) || !query.endsWith(suffix)) {
            throw new IllegalArgumentException("not a count query: " + query);
        }
        return query.substring(prefix.length(), query.length() - suffix.length()).trim();
    }

    /**
     * @param listQuery A list query.
     * @return The block the list query pages over: everything before ORDER BY and the paging block.
     */
    public static String listBody(String listQuery) {
        String query = normalize(listQuery);
        int cut = query.length();
        for (String marker : new String[]{ORDER_BY, PAGING[0], PAGING[1]}) {
            int index = query.indexOf(marker);
            if (index >= 0 && index < cut) {
                cut = index;
            }
        }
        return query.substring(0, cut).trim();
    }

    /**
     * @param query A count or a list query.
     * @return The projected columns, in order, without the DISTINCT keyword.
     */
    public static List<String> selectedColumns(String query) {
        String body = isCountQuery(query) ? countBody(query) : normalize(query);
        String selectList = StringUtils.substringBefore(StringUtils.substringAfter(body, "SELECT "), " FROM ");
        selectList = StringUtils.removeStart(selectList.trim(), "DISTINCT ").trim();
        List<String> columns = new ArrayList<>();
        for (String column : selectList.split(",")) {
            columns.add(column.trim());
        }
        return Collections.unmodifiableList(columns);
    }

    public static boolean isDistinct(String query) {
        String body = isCountQuery(query) ? countBody(query) : normalize(query);
        return body.startsWith("SELECT DISTINCT ");
    }

    /**
     * @param query A list query.
     * @return The columns the ORDER BY references, without their direction.
     */
    public static List<String> orderedColumns(String query) {
        String normalized = normalize(query);
        int index = normalized.indexOf(ORDER_BY);
        if (index < 0) {
            return Collections.emptyList();
        }
        String block = normalized.substring(index + ORDER_BY.length());
        for (String marker : PAGING) {
            block = StringUtils.substringBefore(block, marker);
        }
        List<String> columns = new ArrayList<>();
        for (String term : block.split(",")) {
            columns.add(SORT_DIRECTION.matcher(term.trim()).replaceAll(""));
        }
        return Collections.unmodifiableList(columns);
    }

    /**
     * @param query A count or a list query.
     * @return The columns the body groups on, in order, or empty when the body does not group.
     */
    public static List<String> groupedColumns(String query) {
        String body = isCountQuery(query) ? countBody(query) : normalize(query);
        int index = body.indexOf(GROUP_BY);
        if (index < 0) {
            return Collections.emptyList();
        }
        String block = body.substring(index + GROUP_BY.length());
        block = StringUtils.substringBefore(block, ORDER_BY.trim());
        List<String> columns = new ArrayList<>();
        for (String term : block.split(",")) {
            columns.add(term.trim());
        }
        return Collections.unmodifiableList(columns);
    }

    public static boolean isGrouped(String query) {
        return !groupedColumns(query).isEmpty();
    }

    /**
     * @param term One term of an ORDER BY block.
     * @return True when the term is an aggregate rather than a plain column reference.
     */
    public static boolean isAggregate(String term) {
        return term.startsWith("MIN(") || term.startsWith("MAX(");
    }

    /**
     * @param term An aggregate term.
     * @return The column the aggregate is taken over.
     */
    public static String aggregatedColumn(String term) {
        return StringUtils.substringBefore(StringUtils.substringAfter(term, "("), ")").trim();
    }

    /**
     * @param query A list query.
     * @return The paging block, or an empty string when the query is not paged.
     */
    public static String pagingBlock(String query) {
        String normalized = normalize(query);
        for (String marker : PAGING) {
            int index = normalized.indexOf(marker);
            if (index >= 0) {
                return normalized.substring(index).trim();
            }
        }
        return "";
    }

    /**
     * @param query Any generated query.
     * @return The joined tables, in the order they are joined.
     */
    public static List<String> joinedTables(String query) {
        List<String> tables = new ArrayList<>();
        String[] parts = normalize(query).split("(?i)INNER JOIN ");
        for (int i = 1; i < parts.length; i++) {
            tables.add(Arrays.stream(parts[i].split(" ")).findFirst().orElse(""));
        }
        return Collections.unmodifiableList(tables);
    }

}
