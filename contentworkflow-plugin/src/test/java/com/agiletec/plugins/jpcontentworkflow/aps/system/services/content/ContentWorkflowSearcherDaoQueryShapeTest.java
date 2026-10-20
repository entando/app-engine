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
package com.agiletec.plugins.jpcontentworkflow.aps.system.services.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.system.common.QueryCapture;
import com.agiletec.aps.system.common.SqlShape;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.plugins.jpcontentworkflow.aps.system.services.workflow.model.WorkflowSearchFilter;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The fifth and last <code>createQueryString</code> variant. It builds its own workflow-step block
 * and closes through the same composition point as the other four, so the count it produces is the
 * body its list pages over - workflow block included.
 *
 * @see QueryCapture
 */
class ContentWorkflowSearcherDaoQueryShapeTest {

    private static final String DERBY = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final Collection<String> ADMIN = List.of(Group.ADMINS_GROUP_NAME);

    private QueryCapture capture;

    @BeforeEach
    void setUp() {
        this.capture = new QueryCapture();
    }

    @Test
    void workflowCount_isTheListBodyWrapped() {
        ContentSearcherDAO dao = this.capture.wire(new ContentSearcherDAO(), DERBY);
        List<WorkflowSearchFilter> workflowFilters = List.of(workflowFilter());
        EntitySearchFilter[] filters = {attributeLike()};

        dao.countContents(workflowFilters, null, false, filters, ADMIN);
        String countQuery = this.capture.single();
        this.capture.clear();
        dao.loadContentsId(workflowFilters, null, false, filters, ADMIN);
        String listQuery = this.capture.single();

        assertEquals(SqlShape.listBody(listQuery), SqlShape.countBody(countQuery));
        assertEquals(1, SqlShape.occurrences(countQuery, SqlShape.COUNT_PREFIX));
        assertEquals(1, SqlShape.occurrences(countQuery, SqlShape.COUNT_SUFFIX));
        assertTrue(SqlShape.isDistinct(countQuery), countQuery);
        assertTrue(SqlShape.normalize(countQuery).contains("contents.status IN ("), countQuery);
        assertEquals(List.of("workcontentsearch"), SqlShape.joinedTables(countQuery));
        assertEquals(List.of("contents.contentid"), SqlShape.selectedColumns(listQuery));
    }

    private static EntitySearchFilter attributeLike() {
        return new EntitySearchFilter<>("Titolo", true, "abc", true);
    }

    private static WorkflowSearchFilter workflowFilter() {
        WorkflowSearchFilter filter = new WorkflowSearchFilter();
        filter.setTypeCode("EVN");
        filter.addAllowedStep("step1");
        return filter;
    }

}
