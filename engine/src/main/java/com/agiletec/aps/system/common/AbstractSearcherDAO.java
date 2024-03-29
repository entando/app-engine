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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

/**
 * Utility Class for searching operation on db. This class presents utility
 * method for searching on db table throw Field search filter.
 *
 * @author E.Santoboni
 */
@SuppressWarnings(value = {"serial", "rawtypes"})
public abstract class AbstractSearcherDAO extends AbstractDAO {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(AbstractSearcherDAO.class);
    private static final String DEFAULT_LIKE_CLAUSE = "LIKE ? ";

    private String likeClause;
    private String dataSourceClassName;

    protected List<String> searchId(FieldSearchFilter[] filters) {
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
            logger.error("Error while loading the list of IDs", t);
            throw new RuntimeException("Error while loading the list of IDs", t);
        } finally {
            closeDaoResources(result, stat, conn);
        }
        return idList;
    }

    protected Integer countId(FieldSearchFilter[] filters) {
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
            logger.error("Error while loading the count of IDs", t);
            throw new RuntimeException("Error while loading the count of IDs", t);
        } finally {
            closeDaoResources(result, stat, conn);
        }
        return count;
    }

    protected FieldSearchFilter[] addFilter(FieldSearchFilter[] filters, FieldSearchFilter filterToAdd) {
        return ArrayUtils.add(filters, filterToAdd);
    }

    protected PreparedStatement buildStatement(FieldSearchFilter[] filters, boolean isCount, boolean selectAll, Connection conn) {
        String query = this.createQueryString(filters, isCount, selectAll);
        logger.trace("{}", query);
        PreparedStatement stat = null;
        try {
            stat = conn.prepareStatement(query);
            int index = 0;
            index = this.addMetadataFieldFilterStatementBlock(filters, index, stat);
        } catch (Throwable t) {
            logger.error("Error while creating the statement", t);
            throw new RuntimeException("Error while creating the statement", t);
        }
        return stat;
    }

    /**
     * Add to the statement the filters on the entity metadata.
     *
     * @param filters the filters to add to the statement.
     * @param index The current index of the statement.
     * @param stat The statement.
     * @return The current statement index, eventually incremented by filters.
     * @throws Throwable In case of error.
     */
    protected int addMetadataFieldFilterStatementBlock(FieldSearchFilter[] filters, int index, PreparedStatement stat) throws Throwable {
        if (filters == null) {
            return index;
        }
        for (FieldSearchFilter filter : filters) {
            if (filter.getKey() != null) {
                index = this.addObjectSearchStatementBlock(filter, index, stat);
            }
        }
        return index;
    }

    /**
     * Add to the statement a filter on a attribute.
     *
     * @param filter The filter on the attribute to apply in the statement.
     * @param index The last index used to associate the elements to the
     * statement.
     * @param stat The statement where the filters are applied.
     * @return The last used index.
     * @throws SQLException In case of error.
     *
     */
    protected int addObjectSearchStatementBlock(FieldSearchFilter filter, int index, PreparedStatement stat) throws SQLException {
        if (filter.isNullOption()) {
            return index;
        }
        if (filter.getAllowedValues() != null && filter.getAllowedValues().size() > 0) {
            List<Object> allowedValues = filter.getAllowedValues();
            for (int i = 0; i < allowedValues.size(); i++) {
                Object allowedValue = allowedValues.get(i);
                this.addObjectSearchStatementBlock(stat, ++index, allowedValue, filter.isLikeOption());
            }
        } else if (filter.getValue() != null) {
            this.addObjectSearchStatementBlock(stat, ++index, filter.getValue(), filter.getValueDateDelay(), filter.isLikeOption(), filter.getLikeOptionType());
        } else {
            if (null != filter.getStart()) {
                this.addObjectSearchStatementBlock(stat, ++index, filter.getStart(), filter.getStartDateDelay(), false);
            }
            if (null != filter.getEnd()) {
                this.addObjectSearchStatementBlock(stat, ++index, filter.getEnd(), filter.getEndDateDelay(), false);
            }
        }
        return index;
    }

    protected void addObjectSearchStatementBlock(PreparedStatement stat,
            int index,
            Object object,
            boolean isLikeOption) throws SQLException {
        this.addObjectSearchStatementBlock(stat, index, object, null, isLikeOption, null);
    }

    protected void addObjectSearchStatementBlock(PreparedStatement stat,
            int index,
            Object object,
            Integer dateDelay,
            boolean isLikeOption) throws SQLException {
        this.addObjectSearchStatementBlock(stat, index, object, dateDelay, isLikeOption, null);
    }

    protected void addObjectSearchStatementBlock(PreparedStatement stat,
            int index,
            Object object,
            Integer dateDelay,
            boolean isLikeOption,
            FieldSearchFilter.LikeOptionType likeOptionType) throws SQLException {
        if (object instanceof String) {
            if (isLikeOption) {
                object = ((String) object).toUpperCase();
                String parameter = null;
                if (null == likeOptionType || likeOptionType.equals(FieldSearchFilter.LikeOptionType.COMPLETE)) {
                    parameter = "%" + ((String) object) + "%";
                } else if (likeOptionType.equals(FieldSearchFilter.LikeOptionType.LEFT)) {
                    parameter = "%" + ((String) object);
                } else if (likeOptionType.equals(FieldSearchFilter.LikeOptionType.RIGHT)) {
                    parameter = ((String) object) + "%";
                }
                stat.setString(index, parameter);
            } else {
                stat.setString(index, (String) object);
            }
        } else if (object instanceof Date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime((Date) object);
            if (dateDelay != null) {
                calendar.add(Calendar.DATE, dateDelay);
            }
            if (object instanceof Timestamp) {
                Timestamp timestamp = new Timestamp(calendar.getTime().getTime());
                stat.setTimestamp(index, timestamp);
            } else {
                Date data = calendar.getTime();
                stat.setDate(index, new java.sql.Date(data.getTime()));
            }
        } else if (object instanceof BigDecimal) {
            stat.setBigDecimal(index, (BigDecimal) object);
        } else if (object instanceof Boolean) {
            stat.setString(index, ((Boolean) object).toString());
        } else if (object instanceof LocalDateTime) {
            stat.setObject(index, Timestamp.valueOf((LocalDateTime)object));
        } else {
            stat.setObject(index, object);
        }
    }

    protected String createQueryString(FieldSearchFilter[] filters, boolean isCount, boolean selectAll) {
        StringBuffer query = this.createBaseQueryBlock(filters, isCount, selectAll);
        boolean hasAppendWhereClause = this.appendMetadataFieldFilterQueryBlocks(filters, query, false);
        if (!isCount) {
            boolean ordered = appendOrderQueryBlocks(filters, query, false);
            this.appendLimitQueryBlock(filters, query);
        }
        return query.toString();
    }

    protected StringBuffer createBaseQueryBlock(FieldSearchFilter[] filters, boolean isCount, boolean selectAll) {
        StringBuffer query = null;
        if (isCount) {
            query = this.createMasterCountQueryBlock();
        } else {
            query = this.createMasterSelectQueryBlock(filters, selectAll);
        }
        return query;
    }

    protected StringBuffer createMasterCountQueryBlock() {
        String masterTableName = this.getMasterTableName();
        StringBuffer query = new StringBuffer("SELECT COUNT(*)");
        query.append(" FROM ").append(masterTableName).append(" ");
        return query;
    }

    private StringBuffer createMasterSelectQueryBlock(FieldSearchFilter[] filters, boolean selectAll) {
        String masterTableName = this.getMasterTableName();
        StringBuffer query = new StringBuffer("SELECT ").append(masterTableName).append(".");
        if (selectAll) {
            query.append("* ");
        } else {
            query.append(this.getMasterTableIdFieldName());
        }
        query.append(" FROM ").append(masterTableName).append(" ");
        return query;
    }
    
    protected void appendLimitQueryBlock(FieldSearchFilter[] filters, StringBuffer query) {
        try {
            if (null == filters || filters.length == 0) {
                logger.debug("no filters");
                return;
            }
            for (FieldSearchFilter filter : filters) {
                if (filter.getOffset() != null && filter.getLimit() != null) {
                    query.append(QueryLimitResolver.createLimitBlock(filter, this.getDataSource(), this.getDataSourceClassName()));
                    break;
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException("error building limit query", t);
        }
    }

    protected boolean appendMetadataFieldFilterQueryBlocks(FieldSearchFilter[] filters, StringBuffer query, boolean hasAppendWhereClause) {
        if (filters == null) {
            return hasAppendWhereClause;
        }
        for (FieldSearchFilter filter : filters) {
            if (filter.getKey() != null) {
                hasAppendWhereClause = this.addMetadataFieldFilterQueryBlock(filter, query, hasAppendWhereClause);
            }
        }
        return hasAppendWhereClause;
    }

    protected boolean addMetadataFieldFilterQueryBlock(FieldSearchFilter filter, StringBuffer query, boolean hasAppendWhereClause) {
        return addFilters(filter, query, hasAppendWhereClause);
    }

    protected boolean addFilters(FieldSearchFilter filter, StringBuffer query, boolean hasAppendWhereClause) {
        if (filter.isSortOnly()) {
            return hasAppendWhereClause;
        }
        hasAppendWhereClause = this.verifyWhereClauseAppend(query, hasAppendWhereClause);
        String tableFieldName = this.getTableFieldName(filter.getKey());
        if (filter.getAllowedValues() != null && filter.getAllowedValues().size() > 0) {
            List<Object> allowedValues = filter.getAllowedValues();
            for (int j = 0; j < allowedValues.size(); j++) {
                if (j == 0) {
                    query.append(" ( ");
                } else {
                    query.append(" OR ");
                }
                StringBuffer x = new StringBuffer(this.getMasterTableName()).append(".").append(tableFieldName).append(" ");
                if (filter.isLikeOption()) {
                    query.append("UPPER(").append(x.toString()).append(") ");
                } else {
                    query.append(x.toString());
                }
                if (filter.isLikeOption()) {
                    query.append(this.getLikeClause());
                } else {
                    query.append("= ? ");
                }
                if (j == (allowedValues.size() - 1)) {
                    query.append(" ) ");
                }
            }
        } else {
            if (filter.isLikeOption()) {
                query.append("UPPER(").append(this.getMasterTableName()).append(".").append(tableFieldName).append(") ");
            } else {
                query.append(this.getMasterTableName()).append(".").append(tableFieldName).append(" ");
            }
            if (filter.getValue() != null) {
                if (filter.isLikeOption()) {
                    query.append(this.getLikeClause());
                } else if (filter.isNotOption()) {
                    query.append("<> ? ");
                } else {
                    query.append("= ? ");
                }
            } else if (null != filter.getStart()) {
                query.append(">= ? ");
                if (null != filter.getEnd()) {
                    query.append("AND ").append(this.getMasterTableName()).append(".").append(tableFieldName).append(" ");
                    query.append("<= ? ");
                }
            } else if (null != filter.getEnd()) {
                query.append("<= ? ");
            } else if (filter.isNullOption()) {
                query.append(" IS NULL ");
            } else {
                query.append(" IS NOT NULL ");
            }
        }
        return hasAppendWhereClause;
    }

    protected boolean appendOrderQueryBlocks(FieldSearchFilter[] filters, StringBuffer query, boolean ordered) {
        if (filters == null) {
            return ordered;
        }
        for (FieldSearchFilter filter : filters) {
            if (null != filter.getKey() && null != filter.getOrder() && !filter.isNullOption()) {
                if (!ordered) {
                    query.append("ORDER BY ");
                    ordered = true;
                } else {
                    query.append(", ");
                }
                String fieldName = this.getTableFieldName(filter.getKey());
                query.append(this.getMasterTableName()).append(".").append(fieldName).append(" ").append(filter.getOrder());
            }
        }
        return ordered;
    }

    protected boolean verifyWhereClauseAppend(StringBuffer query, boolean hasAppendWhereClause) {
        if (hasAppendWhereClause) {
            query.append("AND ");
        } else {
            query.append("WHERE ");
            hasAppendWhereClause = true;
        }
        return hasAppendWhereClause;
    }

    protected abstract String getTableFieldName(String metadataFieldKey);

    /**
     * Return the name of the entities master table.
     *
     * @return The name of the master table.
     */
    protected abstract String getMasterTableName();

    /**
     * Return the name of the ID field in the master table.
     *
     * @return The name of the ID field.
     */
    protected abstract String getMasterTableIdFieldName();

    protected boolean hasLikeFilters(FieldSearchFilter[] filters) {
        if (null == filters || filters.length == 0) {
            return false;
        }
        for (FieldSearchFilter filter : filters) {
            if (filter.isLikeOption()) {
                return true;
            }
        }
        return false;
    }

    protected String getLikeClause() {
        if (null == this.likeClause || this.likeClause.trim().length() == 0) {
            return DEFAULT_LIKE_CLAUSE;
        }
        return likeClause;
    }

    public void setLikeClause(String likeClause) {
        this.likeClause = likeClause;
    }

    protected String getDataSourceClassName() {
        return dataSourceClassName;
    }

    public void setDataSourceClassName(String dataSourceClassName) {
        this.dataSourceClassName = dataSourceClassName;
    }
    
}
