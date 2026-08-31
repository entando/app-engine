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
import java.util.stream.Collectors;
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
    private void init() throws Exception {
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
                .collect(Collectors.toList());
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
                .collect(Collectors.toList());
    }

}
