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
package com.agiletec.plugins.jacms.aps.system.services.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.common.QueryCapture;
import com.agiletec.aps.system.common.SqlShape;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.services.group.Group;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The shape of the SQL the content searchers generate, asserted without a database.
 *
 * <p>An attribute filter joins the search table, which holds one row per content per attribute per
 * language, so the joined query can return a content several times. The count and the list are one
 * body precisely so that the multiplication is seen by both.</p>
 *
 * @see QueryCapture
 */
class ContentSearcherDaoQueryShapeTest {

    private static final String DERBY = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final String ATTRIBUTE = "Titolo";
    /** An admin sees every group, so no group block is added and the shape stays readable. */
    private static final Collection<String> ADMIN = List.of(Group.ADMINS_GROUP_NAME);

    private QueryCapture capture;

    @BeforeEach
    void setUp() {
        this.capture = new QueryCapture();
    }

    @Test
    void contentCount_isTheListBodyWrapped() {
        WorkContentSearcherDAO dao = this.capture.wire(new WorkContentSearcherDAO(), DERBY);
        EntitySearchFilter[] filters = {attributeLike(), creationDateOrder()};
        String[] categories = {"cat1"};
        Collection<String> groups = List.of("customers");

        dao.countContents(categories, false, filters, groups);
        String countQuery = this.capture.single();
        this.capture.clear();
        dao.loadContentsId(categories, false, filters, groups);
        String listQuery = this.capture.single();

        assertEquals(SqlShape.listBody(listQuery), SqlShape.countBody(countQuery));
        assertEquals(1, SqlShape.occurrences(countQuery, SqlShape.COUNT_PREFIX));
        assertEquals(1, SqlShape.occurrences(countQuery, SqlShape.COUNT_SUFFIX));
        assertTrue(SqlShape.normalize(countQuery).contains("contents.maingroup = ?"), countQuery);
    }

    @Test
    void contentSelectBlock_isDistinctAndProjectsOnlyTheOrderedColumns() {
        WorkContentSearcherDAO dao = this.capture.wire(new WorkContentSearcherDAO(), DERBY);

        dao.loadContentsId(null, false, new EntitySearchFilter[]{attributeLike(), creationDateOrder()}, ADMIN);

        String query = this.capture.single();
        assertTrue(SqlShape.isDistinct(query), query);
        assertEquals(List.of("contents.contentid", "contents.created"), SqlShape.selectedColumns(query));
        assertEquals(List.of("contents.created", "contents.contentid"), SqlShape.orderedColumns(query));
        assertEquals(List.of("workcontentsearch"), SqlShape.joinedTables(query));
    }

    /**
     * The LIKE filter's value column used to be projected and aliased, and nothing ever read it back.
     * Under DISTINCT it would make the content distinct once per language, which is the defect.
     */
    @Test
    void contentListQuery_dropsTheColumnsProjectedOnlyForALikeFilter() {
        WorkContentSearcherDAO dao = this.capture.wire(new WorkContentSearcherDAO(), DERBY);

        dao.loadContentsId(null, false, new EntitySearchFilter[]{attributeLike()}, ADMIN);

        String query = this.capture.single();
        assertEquals(List.of("contents.contentid"), SqlShape.selectedColumns(query));
        assertFalse(SqlShape.normalize(query).contains("AS textvalue"), query);
    }

    /**
     * Ordering <em>by</em> an attribute is the case DISTINCT cannot collapse: the ORDER BY names a
     * column of the joined search table, and projecting it - which DISTINCT would require - makes the
     * content distinct once per language. The body groups on the content id instead and reaches the
     * attribute through an aggregate, which needs no projection.
     *
     * <p>The count wraps that same grouped body, so it counts contents and its total is exact.</p>
     */
    @Test
    void orderingByAMultiValuedAttribute_groupsInsteadOfProjectingTheAttribute() {
        WorkContentSearcherDAO dao = this.capture.wire(new WorkContentSearcherDAO(), DERBY);
        EntitySearchFilter[] filters = {ordered(attributeLike())};

        dao.countContents(null, false, filters, ADMIN);
        String countQuery = this.capture.single();
        this.capture.clear();
        dao.loadContentsId(null, false, filters, ADMIN);
        String listQuery = this.capture.single();

        assertEquals(List.of("contents.contentid"), SqlShape.selectedColumns(listQuery));
        assertEquals(List.of("contents.contentid"), SqlShape.groupedColumns(listQuery));
        assertFalse(SqlShape.isDistinct(listQuery), listQuery);
        assertEquals(List.of("MIN(workcontentsearch0.textvalue)", "contents.contentid"),
                SqlShape.orderedColumns(listQuery));
        assertEquals(SqlShape.listBody(listQuery), SqlShape.countBody(countQuery));
        assertTrue(SqlShape.isGrouped(countQuery), countQuery);
    }

    /**
     * ASC takes the lowest value a content holds, DESC the highest - so a content sorts on the value
     * the requested direction actually asks for, rather than on whichever joined row the database
     * happened to pick.
     */
    @Test
    void theAggregateFollowsTheRequestedDirection() {
        WorkContentSearcherDAO dao = this.capture.wire(new WorkContentSearcherDAO(), DERBY);
        EntitySearchFilter descending = attributeLike();
        descending.setOrder(FieldSearchFilter.DESC_ORDER);

        dao.loadContentsId(null, false, new EntitySearchFilter[]{ordered(attributeLike())}, ADMIN);
        String ascending = this.capture.single();
        this.capture.clear();
        dao.loadContentsId(null, false, new EntitySearchFilter[]{descending}, ADMIN);
        String descendingQuery = this.capture.single();

        assertEquals(List.of("MIN(workcontentsearch0.textvalue)", "contents.contentid"),
                SqlShape.orderedColumns(ascending));
        assertEquals(List.of("MAX(workcontentsearch0.textvalue)", "contents.contentid"),
                SqlShape.orderedColumns(descendingQuery));
        // the tie-breaker keeps following the direction it breaks
        assertTrue(SqlShape.normalize(descendingQuery).endsWith("contents.contentid DESC"), descendingQuery);
    }

    /**
     * The paging block sits after an aggregate ORDER BY over a grouped body - a shape none of the
     * engines had ever been handed before this change.
     */
    @Test
    void paginationOfAGroupedQueryKeepsTheVendorPagingBlock() {
        WorkContentSearcherDAO dao = this.capture.wire(new WorkContentSearcherDAO(), DERBY);

        dao.loadContentsId(null, false,
                new EntitySearchFilter[]{ordered(attributeLike()), new EntitySearchFilter(10, 5)}, ADMIN);

        String query = this.capture.single();
        assertTrue(SqlShape.isGrouped(query), query);
        assertEquals("OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY", SqlShape.pagingBlock(query));
    }

    /**
     * Scoping: a metadata order alongside the attribute filter cannot multiply a row, so the query
     * keeps the DISTINCT shape it had - same plan, same total, same row order as before.
     */
    @Test
    void filteringOnAnAttributeButOrderingOnMetadata_doesNotGroup() {
        WorkContentSearcherDAO dao = this.capture.wire(new WorkContentSearcherDAO(), DERBY);

        dao.loadContentsId(null, false, new EntitySearchFilter[]{attributeLike(), creationDateOrder()}, ADMIN);

        String query = this.capture.single();
        assertFalse(SqlShape.isGrouped(query), query);
        assertTrue(SqlShape.isDistinct(query), query);
    }

    /**
     * The public searcher restricts its search to published contents, and both of its queries are built
     * from that same filter set - the filter is applied in buildStatement, which the count and the list
     * both pass through.
     *
     * <p>It used to be applied in loadContentsId alone, so the count included drafts the list would
     * never return and the API reported pages that came back empty. Same class of defect as the join
     * one, a level up: there the two queries disagreed on the body, here on the filters.</p>
     */
    @Test
    void publicContentSearcher_appliesTheOnlineFilterToTheCountAsWell() {
        PublicContentSearcherDAO dao = this.capture.wire(new PublicContentSearcherDAO(), DERBY);
        EntitySearchFilter[] filters = {attributeLike()};

        dao.countContents(null, false, filters, ADMIN);
        String countQuery = this.capture.single();
        this.capture.clear();
        dao.loadContentsId(null, false, filters, ADMIN);
        String listQuery = this.capture.single();

        assertTrue(SqlShape.normalize(listQuery).contains("contents.onlinexml IS NOT NULL"), listQuery);
        assertTrue(SqlShape.normalize(countQuery).contains("contents.onlinexml IS NOT NULL"), countQuery);
        assertEquals(SqlShape.listBody(listQuery), SqlShape.countBody(countQuery));
        assertEquals(List.of("contentsearch"), SqlShape.joinedTables(listQuery));
        // the filter carries no value, so it must not have introduced a placeholder
        assertEquals(SqlShape.occurrences(listQuery, "?"), SqlShape.occurrences(countQuery, "?"));
    }

    // ---------------------------------------------------------------- fixtures

    private static EntitySearchFilter attributeLike() {
        return new EntitySearchFilter<>(ATTRIBUTE, true, "abc", true);
    }

    private static EntitySearchFilter creationDateOrder() {
        EntitySearchFilter filter = new EntitySearchFilter(IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false);
        filter.setOrder(FieldSearchFilter.ASC_ORDER);
        return filter;
    }

    private static EntitySearchFilter ordered(EntitySearchFilter filter) {
        filter.setOrder(FieldSearchFilter.ASC_ORDER);
        return filter;
    }

}
