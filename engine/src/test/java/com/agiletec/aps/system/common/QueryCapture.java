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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;

/**
 * The SQL a searcher DAO hands to the driver, captured without a database.
 *
 * <p>Every searcher reaches the driver through {@link AbstractSearcherDAO#prepareStatement}, so
 * calling a DAO's own search or count method against this datasource yields the generated query and
 * nothing else: the statement records its parameters into a stub and the result set is always
 * empty, which leaves counts at zero and id lists empty.</p>
 */
public final class QueryCapture {

    private final List<String> queries = new ArrayList<>();
    private final DataSource dataSource;

    public QueryCapture() {
        try {
            ResultSet emptyResult = mock(ResultSet.class);
            when(emptyResult.next()).thenReturn(false);
            PreparedStatement statement = mock(PreparedStatement.class);
            when(statement.executeQuery()).thenReturn(emptyResult);
            Connection connection = mock(Connection.class);
            when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
                this.queries.add(invocation.getArgument(0));
                return statement;
            });
            this.dataSource = mock(DataSource.class);
            when(this.dataSource.getConnection()).thenReturn(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("the stubs above cannot throw", e);
        }
    }

    /**
     * Wire a DAO to this capture. The driver class name decides the paging syntax, and there is no
     * real datasource to read it from.
     *
     * @param dao The DAO under test.
     * @param driverClassName The JDBC driver the DAO should generate for.
     * @param <T> The DAO type.
     * @return The same DAO, wired.
     */
    public <T extends AbstractSearcherDAO> T wire(T dao, String driverClassName) {
        dao.setDataSource(this.dataSource);
        dao.setDataSourceClassName(driverClassName);
        return dao;
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }

    public List<String> getQueries() {
        return Collections.unmodifiableList(this.queries);
    }

    /**
     * @return The only query captured so far.
     */
    public String single() {
        if (this.queries.size() != 1) {
            throw new IllegalStateException("expected exactly one query, captured " + this.queries);
        }
        return this.queries.get(0);
    }

    public void clear() {
        this.queries.clear();
    }

}
