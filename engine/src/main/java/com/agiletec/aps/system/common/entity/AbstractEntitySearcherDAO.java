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
package com.agiletec.aps.system.common.entity;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.agiletec.aps.system.common.AbstractSearcherDAO;
import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.common.entity.model.ApsEntityRecord;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

/**
 * Abstract class extended by those DAO that perform searches on entities.
 * @author E.Santoboni
 */
@SuppressWarnings(value = {"serial", "rawtypes"})
public abstract class AbstractEntitySearcherDAO extends AbstractSearcherDAO implements IEntitySearcherDAO {

    private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(AbstractEntitySearcherDAO.class);
    private static final String TEXTVALUE = "textvalue";


    @Override
    public List<ApsEntityRecord> searchRecords(EntitySearchFilter[] filters) {
        Connection conn = null;
        List<ApsEntityRecord> records = new ArrayList<>();
        PreparedStatement stat = null;
        ResultSet result = null;
        try {
            conn = this.getConnection();
            stat = this.buildStatement(filters, false, true, conn);
            result = stat.executeQuery();
            while (result.next()) {
                ApsEntityRecord record = this.createRecord(result);
                if (!records.contains(record)) {

                    records.add(record);
                }
            }
        } catch (Throwable t) {
            _logger.error("Error while loading records list", t);
            throw new RuntimeException("Error while loading records list", t);
        } finally {
            closeDaoResources(result, stat, conn);
        }
        return records;
    }

    protected abstract ApsEntityRecord createRecord(ResultSet result) throws Throwable;

    protected Integer countId(EntitySearchFilter[] filters) {
        Connection conn = null;
        int count = 0;
        PreparedStatement stat = null;
        ResultSet result = null;
        try {
            conn = this.getConnection();
            stat = this.buildStatement(filters, true, false, conn);
            result = stat.executeQuery();
            if (result.next()) {
                count = result.getInt(1);
            }
        } catch (Throwable t) {
            _logger.error("Error while loading the count of IDs", t);
            throw new RuntimeException("Error while loading the count of IDs", t);
        } finally {
            closeDaoResources(result, stat, conn);
        }
        return count;
    }

    @Override
    public List<String> searchId(String typeCode, EntitySearchFilter[] filters) {
        if (typeCode != null && typeCode.trim().length() > 0) {
            EntitySearchFilter filter = new EntitySearchFilter(IEntityManager.ENTITY_TYPE_CODE_FILTER_KEY, false, typeCode, false);
            EntitySearchFilter[] newFilters = this.addFilter(filters, filter);
            return this.searchId(newFilters);
        }
        return this.searchId(filters);
    }

    @Override
    public List<String> searchId(EntitySearchFilter[] filters) {
        Connection conn = null;
        List<String> idList = new ArrayList<>();
        PreparedStatement stat = null;
        ResultSet result = null;
        try {
            conn = this.getConnection();
            stat = this.buildStatement(filters, false, false, conn);
            result = stat.executeQuery();
            while (result.next()) {
                String id = result.getString(this.getMasterTableIdFieldName());
                if (!idList.contains(id)) {
                    idList.add(id);
                }
            }
        } catch (Throwable t) {
            _logger.error("Error while loading the list of IDs", t);
            throw new RuntimeException("Error while loading the list of IDs", t);
        } finally {
            closeDaoResources(result, stat, conn);
        }
        return idList;
    }

    protected EntitySearchFilter[] addFilter(EntitySearchFilter[] filters, EntitySearchFilter filterToAdd) {
        int len = 0;
        if (filters != null) {
            len = filters.length;
        }
        EntitySearchFilter[] newFilters = new EntitySearchFilter[len + 1];
        for (int i = 0; i < len; i++) {
            newFilters[i] = filters[i];
        }
        newFilters[len] = filterToAdd;
        return newFilters;
    }

    private PreparedStatement buildStatement(EntitySearchFilter[] filters, boolean isCount, boolean selectAll, Connection conn) {
        String query = this.createQueryString(filters, isCount, selectAll);
        PreparedStatement stat = null;
        try {
            stat = this.prepareStatement(conn, query);
            int index = 0;
            index = this.addAttributeFilterStatementBlock(filters, index, stat);
            index = this.addMetadataFieldFilterStatementBlock(filters, index, stat);
        } catch (Throwable t) {
            _logger.error("Error while creating the statement", t);
            throw new RuntimeException("Error while creating the statement", t);
        }
        return stat;
    }

    /**
     * Add to the statement the filters on the entity metadata.
     * @param filters the filters to add to the statement.
     * @param index The current index of the statement.
     * @param stat The statement.
     * @return The current statement index, eventually incremented by filters.
     * @throws Throwable In case of error.
     */
    protected int addMetadataFieldFilterStatementBlock(EntitySearchFilter[] filters, int index, PreparedStatement stat) throws Throwable {
        if (filters == null) {
            return index;
        }
        for (int i = 0; i < filters.length; i++) {
            EntitySearchFilter filter = filters[i];
            if (filter.getKey() != null && !filter.isAttributeFilter()) {
                index = this.addObjectSearchStatementBlock(filter, index, stat);
            }
        }
        return index;
    }

    /**
     * Add the attribute filters to the statement.
     * @param filters The filters on the entity filters to insert in the statement.
     * @param index The last index used to associate the elements to the statement.
     * @param stat The statement where the filters are applied.
     * @return The last used index.
     * @throws SQLException In case of error.
     */
    protected int addAttributeFilterStatementBlock(EntitySearchFilter[] filters, 
            int index, PreparedStatement stat) throws SQLException {
        if (filters == null) {
            return index;
        }
        for (int i = 0; i < filters.length; i++) {
            EntitySearchFilter filter = filters[i];
            if ((null != filter.getKey() || null != filter.getRoleName()) && filter.isAttributeFilter()) {
                if (null != filter.getKey()) {
                    stat.setString(++index, filter.getKey());
                } else {
                    stat.setString(++index, filter.getRoleName().toUpperCase());
                }
                index = this.addObjectSearchStatementBlock(filter, index, stat);
            }
        }
        return index;
    }

    /**
     * Add to the statement a filter on a attribute.
     * @param filter The filter on the attribute to apply in the statement.
     * @param index The last index used to associate the elements to the statement.
     * @param stat The statement where the filters are applied.
     * @return The last used index.
     * @throws SQLException In case of error.
     */
    protected int addObjectSearchStatementBlock(EntitySearchFilter filter, int index, PreparedStatement stat) throws SQLException {
        if (filter.isAttributeFilter() && null != filter.getLangCode()) {
            stat.setString(++index, filter.getLangCode());
        }
        return super.addObjectSearchStatementBlock(filter, index, stat);
    }
    
    protected String createQueryString(EntitySearchFilter[] filters, boolean isCount, boolean selectAll) {
        StringBuffer query = this.createBaseQueryBlock(filters, isCount, selectAll);
        boolean hasAppendWhereClause = this.appendFullAttributeFilterQueryBlocks(filters, query, false);
        this.appendMetadataFieldFilterQueryBlocks(filters, query, hasAppendWhereClause);
        boolean grouped = this.appendGroupByQueryBlock(filters, query, selectAll);
        if (!isCount) {
            this.appendOrderQueryBlocks(filters, query, false, grouped);
            this.appendLimitQueryBlock(filters, query);
        }
        return this.toQueryString(query, isCount);
    }

    /**
     * Create the 'base block' of the query with the eventual references to the support table.
     * @param filters The filters defined.
     * @param isCount
     * @param selectAll When true, this will insert all the fields in the master table in the select 
     * of the master query.
     * When true we select all the available fields; when false only the field addressed by the filter
     * is selected.
     * @return The base block of the query.
     */
    protected StringBuffer createBaseQueryBlock(EntitySearchFilter[] filters, boolean isCount, boolean selectAll) {
        StringBuffer query = null;
        if (isCount) {
            // count the rows of the very select block the list query pages over, so that the two can
            // never disagree: an attribute filter joins the search table and can match several rows
            // per entity, and LIMIT/OFFSET is applied to whatever that block returns
            query = this.createMasterSelectQueryBlock(filters, false);
        } else {
            query = this.createMasterSelectQueryBlock(filters, selectAll);
        }
        this.appendJoinSearchTableQueryBlock(filters, query);
        return query;
    }

    protected StringBuffer createMasterSelectQueryBlock(EntitySearchFilter[] filters, boolean selectAll) {
        String masterTableName = this.getEntityMasterTableName();
        boolean grouped = this.isGroupedByMasterId(filters, selectAll);
        StringBuffer query = new StringBuffer("SELECT ");
        if (!selectAll && !grouped) {
            // GROUP BY on the master id already returns one row per entity
            query.append("DISTINCT ");
        }
        query.append(masterTableName).append(".");
        if (selectAll) {
            query.append("* ");
        } else {
            query.append(this.getEntityMasterTableIdFieldName());
        }
        if (filters != null) {
            if (selectAll) {
                this.appendLikeFieldsSelectBlock(filters, query);
            } else {
                this.appendOrderFieldsSelectBlock(filters, query, grouped);
            }
        }
        query.append(" FROM ").append(masterTableName).append(" ");
        return query;
    }

    /**
     * Whether the body collapses the entity in SQL rather than with DISTINCT.
     *
     * <p>DISTINCT cannot collapse an entity that is ordered <em>by</em> an attribute: the ORDER BY
     * names a column of the joined search table, that column has to be projected, and an entity
     * holding one value per language then produces rows that are genuinely distinct. Grouping on the
     * master id collapses it, and the attribute is reached through an aggregate instead.</p>
     *
     * <p>Deliberately scoped to that case. Ordering on metadata alone cannot multiply a row, so those
     * searches - the large majority - keep the plan, the totals and the row order they have.</p>
     *
     * @param filters The filters of the query.
     * @param selectAll True when the query loads whole records; that path has no count paired with it
     * and projects the master table's CLOB columns, so it is never grouped.
     * @return True when the body must group by the master id.
     */
    protected boolean isGroupedByMasterId(EntitySearchFilter[] filters, boolean selectAll) {
        if (selectAll || null == filters) {
            return false;
        }
        for (EntitySearchFilter filter : filters) {
            if (this.isOrderFilter(filter) && filter.isAttributeFilter()) {
                return true;
            }
        }
        return false;
    }

    /**
     * The filters the ORDER BY block will emit a term for. Every method that has to stay aligned with
     * that block - the projection, the GROUP BY - asks this rather than repeating the condition.
     *
     * @param filter The filter to test.
     * @return True when the filter carries an order the query builder honours.
     */
    private boolean isOrderFilter(EntitySearchFilter filter) {
        return (null != filter.getKey() || null != filter.getRoleName())
                && null != filter.getOrder() && !filter.isNullOption();
    }

    /**
     * The master-table columns the ORDER BY block references, de-duplicated and in the order the
     * filters declare them. They are projected, and when the body groups they are also grouped on:
     * Derby and Oracle both reject an un-aggregated column that is not in the GROUP BY, even one
     * functionally dependent on the grouping key that MySQL and PostgreSQL accept.
     *
     * @param filters The filters of the query.
     * @return The column names, without the table prefix.
     */
    private List<String> metadataOrderColumns(EntitySearchFilter[] filters) {
        List<String> columns = new ArrayList<>();
        if (null == filters) {
            return columns;
        }
        for (EntitySearchFilter filter : filters) {
            if (!this.isOrderFilter(filter) || filter.isAttributeFilter()) {
                continue;
            }
            String fieldName = this.resolveTableFieldName(filter.getKey());
            // two filters can order on the same column; a count wraps this block in a derived table,
            // and a derived table may not repeat a column name
            if (!columns.contains(fieldName) && !fieldName.equals(this.getEntityMasterTableIdFieldName())) {
                columns.add(fieldName);
            }
        }
        return columns;
    }

    /**
     * Group the body on the master id so that an entity holding several values for the ordered
     * attribute collapses to one row. Part of the body, not of the order block: the count wraps the
     * same block, so it counts groups and its total becomes exact.
     *
     * @param filters The filters of the query.
     * @param query The query under construction.
     * @param selectAll True when the query loads whole records.
     * @return True when the clause was appended, to be handed to
     * {@link #appendOrderQueryBlocks(EntitySearchFilter[], StringBuffer, boolean, boolean)} - the two
     * cannot disagree because one produces what the other consumes.
     */
    protected boolean appendGroupByQueryBlock(EntitySearchFilter[] filters, StringBuffer query, boolean selectAll) {
        if (!this.isGroupedByMasterId(filters, selectAll)) {
            return false;
        }
        String masterTableName = this.getEntityMasterTableName();
        query.append("GROUP BY ").append(masterTableName).append(".")
                .append(this.getEntityMasterTableIdFieldName());
        for (String column : this.metadataOrderColumns(filters)) {
            query.append(", ").append(masterTableName).append(".").append(column);
        }
        query.append(" ");
        return true;
    }

    private void appendLikeFieldsSelectBlock(EntitySearchFilter[] filters, StringBuffer query) {
        String masterTableName = this.getEntityMasterTableName();
        String searchTableName = this.getEntitySearchTableName();
        for (int i = 0; i < filters.length; i++) {
            EntitySearchFilter filter = filters[i];
            if (!filter.isAttributeFilter() && filter.isLikeOption()) {
                String tableFieldName = this.resolveTableFieldName(filter.getKey());
                //check for id column already present
                if (!tableFieldName.equals(this.getMasterTableIdFieldName())) {
                    query.append(", ").append(masterTableName).append(".").append(tableFieldName);
                }
            } else if (filter.isAttributeFilter() && filter.isLikeOption()) {
                String columnName = this.getAttributeFieldColunm(filter);
                query.append(", ").append(searchTableName).append(i).append(".").append(columnName);
                query.append(" AS ").append(columnName).append(i).append(" ");
            }
        }
    }

    /**
     * Project the columns the ORDER BY block will reference. Under DISTINCT they have to appear in the
     * select list, and they are the only extra columns allowed to: any other column of the joined
     * search table would make the entity distinct again, row by row.
     *
     * <p>When the body groups, the attribute column is deliberately <em>not</em> projected. Projecting
     * it is what stops DISTINCT collapsing the entity, and the ORDER BY reaches it through an
     * aggregate instead, which needs no projection.</p>
     *
     * @param filters The filters of the query.
     * @param query The query under construction.
     * @param grouped True when the body groups by the master id.
     */
    private void appendOrderFieldsSelectBlock(EntitySearchFilter[] filters, StringBuffer query, boolean grouped) {
        for (String column : this.metadataOrderColumns(filters)) {
            query.append(", ").append(this.getEntityMasterTableName()).append(".").append(column);
        }
        if (grouped) {
            return;
        }
        for (int i = 0; i < filters.length; i++) {
            EntitySearchFilter filter = filters[i];
            if (!this.isOrderFilter(filter) || !filter.isAttributeFilter()) {
                continue;
            }
            String searchTableNameAlias = this.getEntitySearchTableName() + i;
            String columnName = this.getAttributeFieldColunm(this.getOrderReferenceValue(filter));
            if (null == columnName) {
                query.append(", ").append(searchTableNameAlias).append(".textvalue");
                query.append(", ").append(searchTableNameAlias).append(".datevalue");
                query.append(", ").append(searchTableNameAlias).append(".numvalue");
            } else {
                query.append(", ").append(searchTableNameAlias).append(".").append(columnName);
            }
        }
    }

    protected void appendJoinSearchTableQueryBlock(EntitySearchFilter[] filters, StringBuffer query) {
        if (filters == null) {
            return;
        }
        String masterTableName = this.getEntityMasterTableName();
        String masterTableIdFieldName = this.getEntityMasterTableIdFieldName();
        String searchTableName = this.getEntitySearchTableName();
        String searchTableIdFieldName = this.getEntitySearchTableIdFieldName();
        String attributeRoleTableName = this.getEntityAttributeRoleTableName();
        String attributeRoleTableIdFieldName = this.getEntityAttributeRoleTableIdFieldName();
        for (int i = 0; i < filters.length; i++) {
            EntitySearchFilter filter = filters[i];
            if ((null != filter.getKey() || null != filter.getRoleName()) && filter.isAttributeFilter() && !filter.isNullOption()) {
                query.append("INNER JOIN ");
                query.append(searchTableName).append(" ").append(searchTableName).append(i).append(" ON ")
                     .append(masterTableName).append(".").append(masterTableIdFieldName).append(" = ")
                     .append(searchTableName).append(i).append(".").append(searchTableIdFieldName).append(" ");
                if (null != filter.getRoleName()) {
                    query.append("INNER JOIN ");
                    query.append(attributeRoleTableName).append(" ").append(attributeRoleTableName).append(i).append(" ON ")
                         .append(masterTableName).append(".").append(masterTableIdFieldName).append(" = ")
                         .append(attributeRoleTableName).append(i).append(".").append(attributeRoleTableIdFieldName).append(" ");
                }
            }
        }
    }

    protected boolean appendFullAttributeFilterQueryBlocks(EntitySearchFilter[] filters, StringBuffer query, boolean hasAppendWhereClause) {
        if (filters != null) {
            for (int i = 0; i < filters.length; i++) {
                EntitySearchFilter filter = filters[i];
                if (!filter.isAttributeFilter()) {
                    continue;
                }
                if (filter.isNullOption() && filter.getKey() != null) {
                    hasAppendWhereClause = this.appendNullAttributeFilterQueryBlocks(filter, query, hasAppendWhereClause);
                } else if (!filter.isNullOption() && (filter.getKey() != null || filter.getRoleName() != null)) {
                    hasAppendWhereClause = this.appendValuedAttributeFilterQueryBlocks(filter, i, query, hasAppendWhereClause);
                }
            }
        }
        return hasAppendWhereClause;
    }

    private boolean appendNullAttributeFilterQueryBlocks(EntitySearchFilter filter, StringBuffer query, boolean hasAppendWhereClause) {
        hasAppendWhereClause = this.verifyWhereClauseAppend(query, hasAppendWhereClause);
        query.append(this.getEntityMasterTableName()).append(".").append(this.getEntityMasterTableIdFieldName());
        query.append(" NOT IN (");
        String searchTableName = this.getEntitySearchTableName();
        query.append("SELECT ").append(searchTableName).append(".").append(this.getEntitySearchTableIdFieldName());
        query.append(" FROM ").append(searchTableName).append(" WHERE ").append(searchTableName).append(".attrname = ? ");
        this.addAttributeLangQueryBlock(searchTableName, query, filter, true);
        query.append(" AND (").append(searchTableName).append(".datevalue IS NOT NULL OR ").append(searchTableName).append(".textvalue IS NOT NULL OR ").append(searchTableName).append(".numvalue IS NOT NULL) ");
        query.append(" ) ");
        return hasAppendWhereClause;
    }

    private boolean appendValuedAttributeFilterQueryBlocks(EntitySearchFilter filter, int index, StringBuffer query, boolean hasAppendWhereClause) {
        String searchTableNameAlias = this.getEntitySearchTableName() + index;
        String attributeRoleTableNameAlias = this.getEntityAttributeRoleTableName() + index;
        hasAppendWhereClause = this.verifyWhereClauseAppend(query, hasAppendWhereClause);
        if (null != filter.getKey()) {
            query.append(searchTableNameAlias).append(".attrname = ? ");
        } else {
            query.append("UPPER(").append(attributeRoleTableNameAlias).append(".rolename) = ? ");
            query.append(" AND ").append(searchTableNameAlias).append(".attrname = ").append(attributeRoleTableNameAlias).append(".attrname ");
        }
        hasAppendWhereClause = this.addAttributeLangQueryBlock(searchTableNameAlias, query, filter, hasAppendWhereClause);

        if (filter.getAllowedValues() != null && filter.getAllowedValues().size() > 0) {
            hasAppendWhereClause = this.verifyWhereClauseAppend(query, hasAppendWhereClause);
            List<Object> allowedValues = filter.getAllowedValues();
            for (int j = 0; j < allowedValues.size(); j++) {
                Object allowedValue = allowedValues.get(j);
                if (j == 0) {
                    query.append(" ( ");
                } else {
                    query.append(" OR ");
                }
                String operator = filter.isLikeOption() ? this.getLikeClause() : "= ? ";
                if (filter.isLikeOption()) {
                    query.append("UPPER(" + searchTableNameAlias).append(".").append(this.getAttributeFieldColunm(allowedValue)).append(") ");
                } else {

                    query.append(searchTableNameAlias).append(".").append(this.getAttributeFieldColunm(allowedValue)).append(" ");
                }

                query.append(operator);
                if (j == (allowedValues.size() - 1)) {
                    query.append(" ) ");
                }
            }
        } else if (filter.getValue() != null) {
            Object object = filter.getValue();
            String operator = filter.isLikeOption() ? this.getLikeClause() : "= ? ";
            hasAppendWhereClause = this.addAttributeObjectSearchQueryBlock(searchTableNameAlias, query,
                                                                           object, operator, hasAppendWhereClause, filter.getLangCode(), filter);
        } else {
            //creazione blocco selezione su tabella ricerca
            if (null != filter.getStart()) {
                hasAppendWhereClause = this.addAttributeObjectSearchQueryBlock(searchTableNameAlias, query,
                                                                               filter.getStart(), ">= ? ", hasAppendWhereClause, filter.getLangCode(), filter);
            }
            if (null != filter.getEnd()) {
                hasAppendWhereClause = this.addAttributeObjectSearchQueryBlock(searchTableNameAlias, query,
                                                                               filter.getEnd(), "<= ? ", hasAppendWhereClause, filter.getLangCode(), filter);
            }
            if (null == filter.getStart() && null == filter.getEnd()) {
                hasAppendWhereClause = this.verifyWhereClauseAppend(query, hasAppendWhereClause);
                query.append(" (").append(searchTableNameAlias).append(".datevalue IS NOT NULL OR ").append(searchTableNameAlias).append(".textvalue IS NOT NULL OR ").append(searchTableNameAlias).append(
                                                                                                                                                                                                           ".numvalue IS NOT NULL) ");
            }
        }
        return hasAppendWhereClause;
    }

    protected boolean addAttributeLangQueryBlock(String searchTableName,
                                                 StringBuffer query,
                                                 EntitySearchFilter filter,
                                                 boolean hasAppendWhereClause) {
        if (filter.isAttributeFilter() && null != filter.getLangCode()) {
            hasAppendWhereClause = this.verifyWhereClauseAppend(query, hasAppendWhereClause);
            query.append(searchTableName).append(".langcode = ? ");
        }
        return hasAppendWhereClause;
    }

    protected boolean appendMetadataFieldFilterQueryBlocks(EntitySearchFilter[] filters, StringBuffer query, boolean hasAppendWhereClause) {
        if (filters == null) {
            return hasAppendWhereClause;
        }
        for (int i = 0; i < filters.length; i++) {
            EntitySearchFilter filter = filters[i];
            if (filter.getKey() != null && !filter.isAttributeFilter()) {
                hasAppendWhereClause = this.addMetadataFieldFilterQueryBlock(filter, query, hasAppendWhereClause);
            }
        }
        return hasAppendWhereClause;
    }

    /**
     * Order a body that does not group. Kept for callers that build an un-grouped query; a caller that
     * appended a GROUP BY must use the four-argument form, or the attribute term would name a column
     * that is neither grouped nor aggregated.
     */
    protected boolean appendOrderQueryBlocks(EntitySearchFilter[] filters, StringBuffer query, boolean ordered) {
        return this.appendOrderQueryBlocks(filters, query, ordered, false);
    }

    /**
     * @param filters The filters of the query.
     * @param query The query under construction.
     * @param ordered Whether an ORDER BY block was already opened.
     * @param grouped True when the body groups by the master id, as reported by
     * {@link #appendGroupByQueryBlock}.
     * @return Whether an ORDER BY block is open.
     */
    protected boolean appendOrderQueryBlocks(EntitySearchFilter[] filters, StringBuffer query, boolean ordered,
            boolean grouped) {
        if (filters == null) {
            return ordered;
        }
        Set<String> orderedFields = new HashSet<>();
        Object lastOrder = null;
        for (int i = 0; i < filters.length; i++) {
            EntitySearchFilter filter = filters[i];
            if (this.isOrderFilter(filter)) {
                if (!ordered) {
                    query.append("ORDER BY ");
                    ordered = true;
                } else {
                    query.append(", ");
                }
                if (filter.isAttributeFilter()) {
                    String tableName = this.getEntitySearchTableName() + i;
                    this.addAttributeOrderQueryBlock(tableName, query, filter, filter.getOrder().toString(), grouped);
                } else {
                    String fieldName = this.resolveTableFieldName(filter.getKey());
                    query.append(this.getEntityMasterTableName()).append(".").append(fieldName).append(" ").append(filter.getOrder());
                    orderedFields.add(fieldName);
                }
                lastOrder = filter.getOrder();
            }
        }
        this.appendOrderTieBreaker(query, ordered, orderedFields, this.getEntityMasterTableIdFieldName(), lastOrder);
        return ordered;
    }

    protected boolean addAttributeObjectSearchQueryBlock(String searchTableName,
                                                         StringBuffer query,
                                                         Object object,
                                                         String operator,
                                                         boolean hasAppendWhereClause,
                                                         String langCode,
                                                         EntitySearchFilter filter) {
        hasAppendWhereClause = this.verifyWhereClauseAppend(query, hasAppendWhereClause);
        if (filter.getValue() != null) {
            if (filter.isLikeOption()) {
                query.append("UPPER(").append(searchTableName).append(".").append(this.getAttributeFieldColunm(object)).append(") ");
            } else {
                query.append(searchTableName).append(".").append(this.getAttributeFieldColunm(object)).append(" ");
            }
        } else {
            query.append("(").append(searchTableName).append(".").append(this.getAttributeFieldColunm(object)).append(") ");
        }
        query.append(operator);
        return hasAppendWhereClause;
    }

    @Override
    protected boolean verifyWhereClauseAppend(StringBuffer query, boolean hasAppendWhereClause) {
        if (hasAppendWhereClause) {
            query.append("AND ");
        } else {
            query.append("WHERE ");
            hasAppendWhereClause = true;
        }
        return hasAppendWhereClause;
    }

    private void addAttributeOrderQueryBlock(String searchTableNameAlias, StringBuffer query,
            EntitySearchFilter filter, String order, boolean grouped) {
        if (order == null) {
            order = "";
        }
        Object object = this.getOrderReferenceValue(filter);
        if (null == object) {
            query.append(this.orderTerm(searchTableNameAlias, TEXTVALUE, order, grouped)).append(", ")
                 .append(this.orderTerm(searchTableNameAlias, "datevalue", order, grouped)).append(", ")
                 .append(this.orderTerm(searchTableNameAlias, "numvalue", order, grouped));
            return;
        }
        query.append(this.orderTerm(searchTableNameAlias, this.getAttributeFieldColunm(object), order, grouped));
    }

    /**
     * One ORDER BY term over an attribute column. When the body groups, the column belongs to the
     * joined search table and is not part of the grouping key, so it is reached through an aggregate:
     * an entity holding several values sorts on the one the requested direction asks for - the lowest
     * ascending, the highest descending.
     *
     * @param searchTableNameAlias The alias of the joined search table.
     * @param columnName The column holding the attribute value.
     * @param order The requested direction.
     * @param grouped True when the body groups by the master id.
     * @return The term, direction included.
     */
    private String orderTerm(String searchTableNameAlias, String columnName, String order, boolean grouped) {
        StringBuilder term = new StringBuilder();
        if (grouped) {
            String aggregate = FieldSearchFilter.DESC_ORDER.equalsIgnoreCase(order) ? "MAX" : "MIN";
            term.append(aggregate).append("(").append(searchTableNameAlias).append(".").append(columnName).append(")");
        } else {
            term.append(searchTableNameAlias).append(".").append(columnName);
        }
        return term.append(" ").append(order).toString();
    }

    /**
     * The value an ORDER BY on an attribute filter is resolved against. Shared with the select block so
     * that the projected column and the ordered column are always the same one.
     */
    private Object getOrderReferenceValue(EntitySearchFilter filter) {
        Object object = filter.getValue();
        if (object == null) {
            object = filter.getStart();
        }
        if (object == null) {
            object = filter.getEnd();
        }
        return object;
    }

    private String getAttributeFieldColunm(EntitySearchFilter filter) {
        Object object = null;
        if (null != filter.getAllowedValues() && filter.getAllowedValues().size() > 0) {
            object = filter.getAllowedValues().get(0);
        } else if (null != filter.getValue()) {
            object = filter.getValue();
        } else if (null != filter.getStart()) {
            object = filter.getStart();
        } else if (null != filter.getEnd()) {
            object = filter.getEnd();
        } else {
            return null;
        }
        return this.getAttributeFieldColunm(object);
    }

    private String getAttributeFieldColunm(Object attributeValue) {
        String columnName = null;
        if (null == attributeValue) {
            columnName = null;
        } else if (attributeValue instanceof String) {
            columnName = TEXTVALUE;
        } else if (attributeValue instanceof Date) {
            columnName = "datevalue";
        } else if (attributeValue instanceof BigDecimal) {
            columnName = "numvalue";
        } else if (attributeValue instanceof Boolean) {
            columnName = TEXTVALUE;
        }
        return columnName;
    }

    /**
     * Return the name of the entities master table.
     * @return The name of the master table.
     */
    protected abstract String getEntityMasterTableName();

    /**
     * Return the name of the "entity ID" field in the master entity table.
     * @return The name of the "entity ID" field.
     */
    protected abstract String getEntityMasterTableIdFieldName();

    /**
     * Return the name of the "Entity Type code" in the master entity table.
     * @return The name of the "Entity Type code".
     */
    protected abstract String getEntityMasterTableIdTypeFieldName();

    /**
     * Return the name of the support table used to perform search on entities.
     * @return The name of the support table.
     */
    protected abstract String getEntitySearchTableName();

    /**
     * Return the name of the "Entity ID" in the support table used to perform search on entities.
     * @return The name of "Entity ID" field.
     */
    protected abstract String getEntitySearchTableIdFieldName();

    protected abstract String getEntityAttributeRoleTableName();

    protected abstract String getEntityAttributeRoleTableIdFieldName();

    @Override
    protected String getMasterTableIdFieldName() {
        return this.getEntityMasterTableIdFieldName();
    }

    @Override
    protected String getMasterTableName() {
        return this.getEntityMasterTableName();
    }

}