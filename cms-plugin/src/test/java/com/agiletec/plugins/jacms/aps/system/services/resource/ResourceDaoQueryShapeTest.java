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
package com.agiletec.plugins.jacms.aps.system.services.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.common.QueryCapture;
import com.agiletec.aps.system.common.SqlShape;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The shape of the SQL {@link ResourceDAO} generates, asserted without a database.
 *
 * <p>A resource holds one <code>resourcerelations</code> row per category and the schema carries no
 * uniqueness on the pair, so the joined query can return a resource more than once. The DAO is the
 * one searcher outside the entity family whose body joins, and it has to de-duplicate on both
 * sides: a distinct count beside a plain list is the original defect in miniature.</p>
 *
 * @see QueryCapture
 */
class ResourceDaoQueryShapeTest {

    private static final String DERBY = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final List<String> TWO_CATEGORIES = List.of("cat1", "cat2");

    private QueryCapture capture;

    @BeforeEach
    void setUp() {
        this.capture = new QueryCapture();
    }

    @Test
    void resourceCount_isTheListBodyWrapped() {
        ResourceDAO dao = this.capture.wire(new ResourceDAO(), DERBY);
        FieldSearchFilter[] filters = {descriptionOrder()};

        dao.countResources(filters, TWO_CATEGORIES, null);
        String countQuery = this.capture.single();
        this.capture.clear();
        dao.searchResourcesId(filters, TWO_CATEGORIES);
        String listQuery = this.capture.single();

        assertEquals(SqlShape.listBody(listQuery), SqlShape.countBody(countQuery));
        assertEquals(1, SqlShape.occurrences(countQuery, SqlShape.COUNT_PREFIX));
        assertEquals(1, SqlShape.occurrences(countQuery, SqlShape.COUNT_SUFFIX));
    }

    @Test
    void bothSidesAreDistinctAndJoinOncePerCategory() {
        ResourceDAO dao = this.capture.wire(new ResourceDAO(), DERBY);
        FieldSearchFilter[] filters = {descriptionOrder()};

        dao.countResources(filters, TWO_CATEGORIES, null);
        String countQuery = this.capture.single();
        this.capture.clear();
        dao.searchResourcesId(filters, TWO_CATEGORIES);
        String listQuery = this.capture.single();

        assertTrue(SqlShape.isDistinct(countQuery), countQuery);
        assertTrue(SqlShape.isDistinct(listQuery), listQuery);
        assertEquals(List.of("resourcerelations", "resourcerelations"), SqlShape.joinedTables(countQuery));
        assertEquals(SqlShape.joinedTables(countQuery), SqlShape.joinedTables(listQuery));
    }

    /**
     * Derby and PostgreSQL reject an ORDER BY on a column outside the select list under DISTINCT, so
     * the ordered column is projected - and only that one, or the extra column would make the
     * resource distinct again, row by row.
     */
    @Test
    void distinctListQuery_projectsTheOrderedColumnAndNothingElse() {
        ResourceDAO dao = this.capture.wire(new ResourceDAO(), DERBY);

        dao.searchResourcesId(new FieldSearchFilter[]{descriptionOrder()}, TWO_CATEGORIES);

        String query = this.capture.single();
        assertEquals(List.of("resources.resid", "resources.descr"), SqlShape.selectedColumns(query));
        assertEquals(List.of("resources.descr", "resources.resid"), SqlShape.orderedColumns(query));
    }

    @Test
    void withoutCategories_theBodyDoesNotJoin() {
        ResourceDAO dao = this.capture.wire(new ResourceDAO(), DERBY);

        dao.countResources(new FieldSearchFilter[]{descriptionOrder()}, null, null);

        String query = this.capture.single();
        assertEquals(List.of(), SqlShape.joinedTables(query));
        assertEquals(List.of("resources.resid", "resources.descr"), SqlShape.selectedColumns(query));
    }

    // ---------------------------------------------------------------- fixtures

    private static FieldSearchFilter descriptionOrder() {
        FieldSearchFilter filter = new FieldSearchFilter("descr");
        filter.setOrder(FieldSearchFilter.ASC_ORDER);
        return filter;
    }

}
