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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.common.model.dao.SearcherDaoPaginatedResult;
import com.agiletec.aps.system.services.group.IGroupManager;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.resource.IResourceManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage for commit c28afd0ba ("Fixed common search returning multiple values in case of
 * join"), which made the count query distinct while leaving the list query - and therefore SQL
 * pagination - working on the multiplied row set.
 *
 * <p>The fixture attribute "Titolo" is stored in workcontentsearch once per language (it, en) for 11
 * contents, so an attribute filter on it joins 22 rows for 11 distinct contents.</p>
 */
class ContentSearchJoinCountRegressionTest extends BaseTestCase {

    /** Contents holding the multi-language "Titolo" attribute, in creation-date order. */
    private static final String[] EXPECTED_CONTENTS = {"EVN191", "EVN192", "EVN193", "EVN194", "EVN103",
            "EVN20", "EVN23", "EVN24", "EVN25", "EVN41", "EVN21"};

    private static final String JOINING_ATTRIBUTE = "Titolo";

    private IContentManager contentManager;
    private IResourceManager resourceManager;
    private IGroupManager groupManager;

    @BeforeEach
    void init() throws Exception {
        this.contentManager = (IContentManager) this.getService(JacmsSystemConstants.CONTENT_MANAGER);
        this.resourceManager = (IResourceManager) this.getService(JacmsSystemConstants.RESOURCE_MANAGER);
        this.groupManager = (IGroupManager) this.getService(SystemConstants.GROUP_MANAGER);
    }

    /**
     * The behaviour c28afd0ba intends to deliver: the count must collapse the rows multiplied by the
     * join on workcontentsearch. Green after the commit, red before it.
     */
    @Test
    void countContents_withJoiningAttributeFilter_countsDistinctContents() throws Throwable {
        EntitySearchFilter[] filters = {creationDateOrder(), joiningAttributeFilter()};
        List<String> allMatching = this.contentManager.loadWorkContentsId(null, false, filters, allGroups());
        assertEquals(EXPECTED_CONTENTS.length, allMatching.size());

        Integer count = this.contentManager.countWorkContents(null, false, filters, allGroups());
        assertEquals(EXPECTED_CONTENTS.length, count.intValue());
    }

    /**
     * The regression. The count is distinct but the list query is not, and LIMIT/OFFSET is applied to
     * the multiplied row set, so a caller that derives the number of pages from the count - as
     * PagedMetadata and the admin content finder both do - stops paging before it has seen every
     * content.
     */
    @Test
    void paginatedSearch_withJoiningAttributeFilter_returnsEveryCountedContent() throws Throwable {
        int pageSize = EXPECTED_CONTENTS.length;
        int declaredCount = paginatedWorkContents(pageSize, 0).getCount();
        int lastPage = lastPage(declaredCount, pageSize);

        // page exactly the way PagedMetadata and the admin content finder do: the declared count is
        // the only thing that tells the caller when to stop asking for pages.
        Set<String> reachable = new HashSet<>();
        for (int page = 0; page < lastPage; page++) {
            reachable.addAll(paginatedWorkContents(pageSize, page * pageSize).getList());
        }

        List<String> unreachable = Arrays.stream(EXPECTED_CONTENTS)
                .filter(id -> !reachable.contains(id))
                .toList();
        assertTrue(unreachable.isEmpty(), "contents matching the filter but not reachable within the "
                + lastPage + " page(s) implied by the declared count of " + declaredCount + ": " + unreachable);
    }

    /**
     * Same root cause, pre-existing rather than introduced by c28afd0ba: LIMIT slices duplicated rows
     * and de-duplication happens per page in Java, so a content whose rows straddle the page boundary
     * is served twice.
     */
    @Test
    void paginatedSearch_withJoiningAttributeFilter_neverRepeatsAnIdAcrossPages() throws Throwable {
        int pageSize = EXPECTED_CONTENTS.length;
        List<String> seen = new ArrayList<>();
        List<String> repeated = new ArrayList<>();
        // the join yields two rows per content, so walk the whole multiplied row set
        for (int offset = 0; offset < EXPECTED_CONTENTS.length * 2; offset += pageSize) {
            for (String id : paginatedWorkContents(pageSize, offset).getList()) {
                if (seen.contains(id)) {
                    repeated.add(id);
                } else {
                    seen.add(id);
                }
            }
        }
        assertTrue(repeated.isEmpty(), "ids served on more than one page: " + repeated);
    }

    /**
     * Ordering <em>by</em> the multi-valued attribute - the case DISTINCT cannot collapse, because the
     * ORDER BY forces the attribute column into the projection. The body groups on the content id and
     * reaches the attribute through an aggregate instead, so the total is exact rather than generous.
     *
     * <p>Before the GROUP BY change this reported 22 for 11 contents: consistent and lossless, but it
     * drew a second page for data that fits on one.</p>
     */
    @Test
    void countContents_orderedByTheJoiningAttribute_isExact() throws Throwable {
        EntitySearchFilter[] filters = {orderedJoiningAttributeFilter()};

        List<String> ids = this.contentManager.loadWorkContentsId(null, false, filters, allGroups());
        Integer count = this.contentManager.countWorkContents(null, false, filters, allGroups());

        assertEquals(EXPECTED_CONTENTS.length, count.intValue());
        assertEquals(ids.size(), count.intValue());
    }

    /**
     * The invariant of the whole fix, on the ordered-attribute path: every counted content is
     * reachable within the pages the count implies, and none is served twice.
     */
    @Test
    void paginatedSearch_orderedByTheJoiningAttribute_pagesOverContentsNotJoinedRows() throws Throwable {
        int pageSize = 4;
        int declaredCount = paginatedByAttribute(pageSize, 0).getCount();
        assertEquals(EXPECTED_CONTENTS.length, declaredCount);

        List<String> seen = new ArrayList<>();
        List<String> repeated = new ArrayList<>();
        for (int page = 0; page < lastPage(declaredCount, pageSize); page++) {
            List<String> ids = paginatedByAttribute(pageSize, page * pageSize).getList();
            assertTrue(ids.size() <= pageSize, "page " + page + " returned " + ids.size() + " ids");
            for (String id : ids) {
                if (seen.contains(id)) {
                    repeated.add(id);
                } else {
                    seen.add(id);
                }
            }
        }

        assertTrue(repeated.isEmpty(), "ids served on more than one page: " + repeated);
        List<String> unreachable = Arrays.stream(EXPECTED_CONTENTS)
                .filter(id -> !seen.contains(id))
                .toList();
        assertTrue(unreachable.isEmpty(), "contents counted but not reachable by paging: " + unreachable);
    }

    /**
     * Both directions return every content exactly once and do so deterministically, which is what
     * paging needs from an ORDER BY.
     *
     * <p>Note what is deliberately <em>not</em> asserted: that DESC is the reverse of ASC. A content
     * holding several values sorts on the one the direction asks for - the lowest ascending, the
     * highest descending - so on a multi-valued attribute the two orders are not reverses of each
     * other. That is the point of the aggregate, not a defect: the alternative is sorting on whichever
     * row the database happened to pick. The ASC-to-MIN and DESC-to-MAX mapping is pinned on the
     * generated SQL by ContentSearcherDaoQueryShapeTest.</p>
     */
    @Test
    void orderingByTheJoiningAttribute_isCompleteAndStableInBothDirections() throws Throwable {
        EntitySearchFilter<String> descending = orderedJoiningAttributeFilter();
        descending.setOrder(EntitySearchFilter.DESC_ORDER);

        List<String> ascending = this.contentManager.loadWorkContentsId(null, false,
                new EntitySearchFilter[]{orderedJoiningAttributeFilter()}, allGroups());
        List<String> descendingIds = this.contentManager.loadWorkContentsId(null, false,
                new EntitySearchFilter[]{descending}, allGroups());

        for (List<String> ids : List.of(ascending, descendingIds)) {
            assertEquals(EXPECTED_CONTENTS.length, ids.size());
            assertEquals(new HashSet<>(Arrays.asList(EXPECTED_CONTENTS)), new HashSet<>(ids));
        }
        assertNotEquals(ascending, descendingIds, "the direction made no difference to the order");
        // a repeated request must slice the same order, or paging can serve a row twice
        assertEquals(ascending, this.contentManager.loadWorkContentsId(null, false,
                new EntitySearchFilter[]{orderedJoiningAttributeFilter()}, allGroups()));
    }

    /**
     * Guard: a metadata-only filter builds no join, so the count keeps the value it had before
     * c28afd0ba. This is the "unrelated searchers are unaffected" case, expressed on the searcher the
     * commit was aimed at.
     */
    @Test
    void countContents_withoutJoiningFilter_isUnchanged() throws Throwable {
        EntitySearchFilter<String> descr = new EntitySearchFilter<>(IContentManager.CONTENT_DESCR_FILTER_KEY,
                false, "Cont", true);
        EntitySearchFilter[] filters = {creationDateOrder(), descr};

        List<String> ids = this.contentManager.loadWorkContentsId(null, false, filters, allGroups());
        Integer count = this.contentManager.countWorkContents(null, false, filters, allGroups());
        assertEquals(9, count.intValue());
        assertEquals(ids.size(), count.intValue());
    }

    /**
     * The published-content search restricts itself to online contents, and its count must apply the
     * same restriction as its list - otherwise the total includes drafts the list will never return and
     * the caller is offered pages that come back empty.
     *
     * <p>The fixture is a mixed one on purpose: the draft corpus is larger than the published one, so a
     * count that ignored the online filter would be visibly larger than the list it describes.</p>
     */
    @Test
    void countPublicContents_appliesTheOnlineFilterItsListApplies() throws Throwable {
        EntitySearchFilter[] filters = {creationDateOrder()};

        List<String> published = this.contentManager.loadPublicContentsId(null, false, filters, allGroups());
        SearcherDaoPaginatedResult<String> paged =
                this.contentManager.getPaginatedPublicContentsId(null, false, filters, allGroups());

        assertFalse(published.isEmpty(), "empty published fixture");
        assertEquals(published.size(), paged.getCount().intValue(),
                "the reported total must describe the rows the list can return");
        // and it must genuinely be a subset of the drafts, or the fixture proves nothing
        List<String> drafts = this.contentManager.loadWorkContentsId(null, false, filters, allGroups());
        assertTrue(drafts.size() > published.size(),
                "fixture no longer has more draft than published contents: " + drafts.size()
                        + " vs " + published.size());
    }

    /**
     * Guard: resourcerelations rows are written from a Set, so the category joins are 1:1 and the
     * distinct count cannot change the value resources report - with one category and with two.
     */
    @Test
    void countResources_matchesResourceListSize() throws Throwable {
        assertResourceCountMatchesList(List.of("resCat1"));
        assertResourceCountMatchesList(List.of("resCat1", "resCat3"));
    }

    private void assertResourceCountMatchesList(List<String> categories) throws Throwable {
        SearcherDaoPaginatedResult<String> result = this.resourceManager
                .getPaginatedResourcesId(new FieldSearchFilter[0], categories, allGroups());
        assertFalse(result.getList().isEmpty(), "empty fixture for categories " + categories);
        assertEquals(result.getList().size(), result.getCount().intValue(),
                "count and list disagree for categories " + categories);
    }

    private SearcherDaoPaginatedResult<String> paginatedWorkContents(int pageSize, int offset) throws Throwable {
        EntitySearchFilter[] filters = {creationDateOrder(), joiningAttributeFilter(),
                new EntitySearchFilter<>(pageSize, offset)};
        return this.contentManager.getPaginatedWorkContentsId(null, false, filters, allGroups());
    }

    /**
     * Attribute filter with no value and no language code: it matches every workcontentsearch row for
     * the attribute, in every language, which is what multiplies the joined rows.
     */
    private EntitySearchFilter<String> joiningAttributeFilter() {
        return new EntitySearchFilter<>(JOINING_ATTRIBUTE, true);
    }

    private SearcherDaoPaginatedResult<String> paginatedByAttribute(int pageSize, int offset) throws Throwable {
        EntitySearchFilter[] filters = {orderedJoiningAttributeFilter(),
                new EntitySearchFilter<>(pageSize, offset)};
        return this.contentManager.getPaginatedWorkContentsId(null, false, filters, allGroups());
    }

    /** The same multiplying filter, now also carrying the order - which is what forces the grouping. */
    private EntitySearchFilter<String> orderedJoiningAttributeFilter() {
        EntitySearchFilter<String> filter = joiningAttributeFilter();
        filter.setOrder(EntitySearchFilter.ASC_ORDER);
        return filter;
    }

    private EntitySearchFilter<String> creationDateOrder() {
        EntitySearchFilter<String> order = new EntitySearchFilter<>(
                IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false);
        order.setOrder(EntitySearchFilter.ASC_ORDER);
        return order;
    }

    private static int lastPage(int count, int pageSize) {
        return (int) Math.ceil((double) count / (double) pageSize);
    }

    private List<String> allGroups() {
        return this.groupManager.getGroups().stream().map(group -> group.getName())
                .toList();
    }

}
