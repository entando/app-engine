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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.agiletec.aps.system.common.entity.AbstractEntitySearcherDAO;
import com.agiletec.aps.system.common.entity.IEntityManager;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.services.group.GroupDAO;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.entando.entando.aps.system.services.actionlog.ActionLogDAO;
import org.entando.entando.aps.system.services.actionlog.model.ActionLogRecordSearchBean;
import org.entando.entando.aps.system.services.guifragment.GuiFragmentDAO;
import org.entando.entando.aps.system.services.oauth2.OAuthConsumerDAO;
import org.entando.entando.aps.system.services.userprofile.UserProfileSearcherDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

/**
 * The shape of the SQL the searcher DAOs generate, asserted without a database.
 *
 * <p>The count and the list are one body: the count query is the list query's body wrapped, so the
 * two cannot report different row sets. Execution tests cannot see that - a wrong projection yields
 * a valid query returning a wrong number - which is what these assertions are for.</p>
 *
 * @see QueryCapture
 */
class SearcherDaoQueryShapeTest {

    private static final String DERBY = "org.apache.derby.jdbc.EmbeddedDriver";

    private QueryCapture capture;

    @BeforeEach
    void setUp() {
        this.capture = new QueryCapture();
    }

    // ---------------------------------------------------------------- the wrapper

    @Test
    void countQuery_opensAndClosesTheCountBlockExactlyOnce() {
        GroupDAO dao = this.capture.wire(new GroupDAO(), DERBY);

        dao.countGroups(new FieldSearchFilter[]{descriptionLike()});

        String query = this.capture.single();
        assertEquals(1, SqlShape.occurrences(query, SqlShape.COUNT_PREFIX));
        assertEquals(1, SqlShape.occurrences(query, SqlShape.COUNT_SUFFIX));
    }

    @Test
    void listQuery_carriesNoCountBlock() {
        GroupDAO dao = this.capture.wire(new GroupDAO(), DERBY);

        dao.searchGroups(new FieldSearchFilter[]{descriptionLike()});

        String query = this.capture.single();
        assertEquals(0, SqlShape.occurrences(query, SqlShape.COUNT_PREFIX));
        assertEquals(0, SqlShape.occurrences(query, SqlShape.COUNT_SUFFIX));
    }

    // ------------------------------------------------- join-free searchers: no DISTINCT

    /**
     * A searcher whose count queries the master table alone cannot multiply a row, so its count body
     * is a plain select: a derived table with no DISTINCT, aggregate or LIMIT is merged by the
     * planner, leaving a count that can be answered from an index.
     */
    @Test
    void joinFreeCount_selectsTheMasterIdWithoutDistinct() {
        GroupDAO dao = this.capture.wire(new GroupDAO(), DERBY);

        dao.countGroups(new FieldSearchFilter[]{descriptionLike()});

        assertEquals("SELECT COUNT(*) FROM ( SELECT authgroups.groupname FROM authgroups"
                        + " WHERE UPPER(authgroups.descr) LIKE ? ) counter",
                SqlShape.normalize(this.capture.single()));
    }

    @Test
    void actionLogCount_selectsTheMasterIdWithoutDistinct() {
        ActionLogDAO dao = this.capture.wire(new ActionLogDAO(), DERBY);
        ActionLogRecordSearchBean searchBean = new ActionLogRecordSearchBean();
        searchBean.setUsername("admin");

        dao.countActionLogRecords(searchBean);

        String query = this.capture.single();
        assertFalse(SqlShape.isDistinct(query), query);
        assertEquals(List.of("actionlogrecords.id"), SqlShape.selectedColumns(query));
        assertEquals(List.of(), SqlShape.joinedTables(query));
    }

    /**
     * The base list query is not distinct, so it is free to order on a column it does not project.
     * The rule that every ordered column has to be in the select list belongs to the distinct
     * searchers below.
     */
    @Test
    void joinFreeList_ordersOnAColumnItDoesNotProject() {
        GroupDAO dao = this.capture.wire(new GroupDAO(), DERBY);

        dao.searchGroups(new FieldSearchFilter[]{ordered(descriptionLike(), FieldSearchFilter.ASC_ORDER)});

        String query = this.capture.single();
        assertFalse(SqlShape.isDistinct(query), query);
        assertEquals(List.of("authgroups.groupname"), SqlShape.selectedColumns(query));
        assertEquals(List.of("authgroups.descr", "authgroups.groupname"), SqlShape.orderedColumns(query));
    }

    // ----------------------------------------------- entity searchers: one body, distinct

    @Test
    void entityCount_isTheListBodyWrapped() {
        ProbeProfileSearcherDAO dao = this.capture.wire(new ProbeProfileSearcherDAO(), DERBY);
        EntitySearchFilter[] filters = {attributeLike(), orderedMetadata()};

        dao.count(filters);
        String countQuery = this.capture.single();
        this.capture.clear();
        dao.searchId(filters);
        String listQuery = this.capture.single();

        assertEquals(SqlShape.listBody(listQuery), SqlShape.countBody(countQuery));
    }

    @Test
    void entitySelectBlock_isDistinct() {
        ProbeProfileSearcherDAO dao = this.capture.wire(new ProbeProfileSearcherDAO(), DERBY);

        dao.searchId(new EntitySearchFilter[]{attributeLike()});

        String query = this.capture.single();
        assertTrue(SqlShape.isDistinct(query), query);
        assertEquals(List.of("authuserprofilesearch"), SqlShape.joinedTables(query));
    }

    /**
     * A LIKE filter used to add the search table's value column to the select list, aliased and never
     * read back. Under DISTINCT that column makes the entity distinct row by row, which is the defect
     * this whole shape exists to prevent.
     */
    @Test
    void entityListQuery_dropsTheColumnsProjectedOnlyForALikeFilter() {
        ProbeProfileSearcherDAO dao = this.capture.wire(new ProbeProfileSearcherDAO(), DERBY);

        dao.searchId(new EntitySearchFilter[]{attributeLike()});

        String query = this.capture.single();
        assertEquals(List.of("authuserprofiles.username"), SqlShape.selectedColumns(query));
        assertFalse(SqlShape.normalize(query).contains("AS textvalue"), query);
    }

    /**
     * Every ordered column has to be reachable by the engine: a plain column reference must be in the
     * select list - Derby and PostgreSQL reject an ORDER BY outside it under DISTINCT - while an
     * aggregate must not be, because projecting it is exactly what would stop the entity collapsing.
     *
     * <p>Covers all five resolutions of the order block, including the two that resolve to three
     * columns at once (an allowed-values filter and a filter carrying no value).</p>
     */
    @ParameterizedTest(name = "ordered by {0}")
    @MethodSource("orderedFilters")
    void listQuery_makesEveryOrderedColumnReachable(String description, EntitySearchFilter orderFilter) {
        ProbeProfileSearcherDAO dao = this.capture.wire(new ProbeProfileSearcherDAO(), DERBY);

        dao.searchId(new EntitySearchFilter[]{orderFilter});

        String query = this.capture.single();
        List<String> projected = SqlShape.selectedColumns(query);
        List<String> grouped = SqlShape.groupedColumns(query);
        for (String term : SqlShape.orderedColumns(query)) {
            if (SqlShape.isAggregate(term)) {
                String column = SqlShape.aggregatedColumn(term);
                assertFalse(projected.contains(column),
                        () -> "aggregated " + column + " but also projected it - " + query);
                assertFalse(grouped.isEmpty(), () -> "aggregate without a GROUP BY - " + query);
            } else {
                assertTrue(projected.contains(term),
                        () -> "ordered by " + term + " but projected " + projected + " - " + query);
            }
        }
        // a grouped body collapses the entity itself; DISTINCT on top would be redundant
        assertEquals(SqlShape.isGrouped(query), !SqlShape.isDistinct(query), query);
    }

    /**
     * The grouping is scoped to the case that needs it. Ordering on metadata alone cannot multiply a
     * row, so those searches keep the plan, the totals and the row order they had.
     */
    @Test
    void orderingOnMetadataAlone_doesNotGroup() {
        ProbeProfileSearcherDAO dao = this.capture.wire(new ProbeProfileSearcherDAO(), DERBY);

        dao.searchId(new EntitySearchFilter[]{attributeLike(), orderedMetadata()});

        String query = this.capture.single();
        assertFalse(SqlShape.isGrouped(query), query);
        assertTrue(SqlShape.isDistinct(query), query);
    }

    /**
     * Ordering by an attribute groups instead, and the count wraps the grouped body - so it counts
     * entities rather than joined rows, and its total is exact.
     */
    @Test
    void orderingByAnAttribute_groupsOnTheMasterIdOnBothSides() {
        ProbeProfileSearcherDAO dao = this.capture.wire(new ProbeProfileSearcherDAO(), DERBY);
        EntitySearchFilter[] filters = {ordered(attributeLike(), FieldSearchFilter.ASC_ORDER)};

        dao.count(filters);
        String countQuery = this.capture.single();
        this.capture.clear();
        dao.searchId(filters);
        String listQuery = this.capture.single();

        assertEquals(List.of("authuserprofiles.username"), SqlShape.groupedColumns(countQuery));
        assertEquals(SqlShape.listBody(listQuery), SqlShape.countBody(countQuery));
        assertFalse(SqlShape.isDistinct(listQuery), listQuery);
        assertEquals(List.of("authuserprofiles.username"), SqlShape.selectedColumns(listQuery));
    }

    /**
     * A metadata column ordered alongside the attribute is grouped on as well. Derby and Oracle both
     * reject an un-aggregated column outside the GROUP BY, even one functionally dependent on the
     * grouping key that MySQL and PostgreSQL accept - so the portable form is the explicit one.
     */
    @Test
    void aMetadataOrderAlongsideAnAttribute_joinsTheGroupingKey() {
        ProbeProfileSearcherDAO dao = this.capture.wire(new ProbeProfileSearcherDAO(), DERBY);

        dao.searchId(new EntitySearchFilter[]{ordered(attributeLike(), FieldSearchFilter.ASC_ORDER),
                orderedMetadata()});

        String query = this.capture.single();
        assertEquals(List.of("authuserprofiles.username", "authuserprofiles.profiletype"),
                SqlShape.groupedColumns(query));
        assertEquals(List.of("authuserprofiles.username", "authuserprofiles.profiletype"),
                SqlShape.selectedColumns(query));
    }

    /**
     * The select-all path loads whole records, has no count paired with it and projects the master
     * table's CLOB columns. It is never grouped - an aggregate there would have to be matched by a
     * grouping key for every projected column.
     */
    @Test
    void selectAllPath_isNeverGrouped() {
        ProbeProfileSearcherDAO dao = this.capture.wire(new ProbeProfileSearcherDAO(), DERBY);

        dao.searchRecords(new EntitySearchFilter[]{ordered(attributeLike(), FieldSearchFilter.ASC_ORDER)});

        String query = this.capture.single();
        assertFalse(SqlShape.isGrouped(query), query);
        assertFalse(SqlShape.normalize(query).contains("MIN("), query);
    }

    static Stream<Arguments> orderedFilters() {
        return Stream.of(
                Arguments.of("a metadata field", orderedMetadata()),
                Arguments.of("an attribute carrying a value",
                        ordered(new EntitySearchFilter<>("Nome", true, "abc", false), FieldSearchFilter.ASC_ORDER)),
                Arguments.of("an attribute carrying a range",
                        ordered(new EntitySearchFilter<>("Data", true, new Date(0), new Date()), FieldSearchFilter.ASC_ORDER)),
                Arguments.of("an attribute carrying allowed values",
                        ordered(new EntitySearchFilter<>("Numero", true,
                                List.of(BigDecimal.ONE, BigDecimal.TEN), false), FieldSearchFilter.DESC_ORDER)),
                Arguments.of("an attribute carrying no value at all",
                        ordered(new EntitySearchFilter("Nome", true), FieldSearchFilter.ASC_ORDER)));
    }

    /**
     * The select-all path loads whole records and has no count paired with it. It must keep its old
     * shape: contents.workxml and authuserprofiles.profilexml are CLOB, and Derby rejects DISTINCT
     * over a CLOB.
     */
    @Test
    void selectAllPath_isNotDistinct() {
        ProbeProfileSearcherDAO dao = this.capture.wire(new ProbeProfileSearcherDAO(), DERBY);

        dao.searchRecords(new EntitySearchFilter[]{attributeLike()});

        String query = this.capture.single();
        assertFalse(SqlShape.isDistinct(query), query);
        assertTrue(SqlShape.normalize(query).startsWith("SELECT authuserprofiles.*"), query);
    }

    // ---------------------------------------------------------------- the balance guard

    /**
     * The only remaining route to an unbalanced count block is a subclass writing the markers by
     * hand. The guard names the DAO that built the query; it does not repair it, and it does not
     * throw - the database rejects such a query on its own, and hiding that would be worse.
     */
    @Test
    void unbalancedCountBlock_isReportedByNameAndNotRepaired() {
        UnbalancedGroupDAO dao = this.capture.wire(new UnbalancedGroupDAO(), DERBY);
        Logger logger = (Logger) LoggerFactory.getLogger(AbstractSearcherDAO.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            dao.countGroups(new FieldSearchFilter[]{descriptionLike()});
        } finally {
            logger.detachAppender(appender);
        }

        String query = this.capture.single();
        assertEquals(2, SqlShape.occurrences(query, SqlShape.COUNT_PREFIX));
        assertEquals(1, SqlShape.occurrences(query, SqlShape.COUNT_SUFFIX));
        List<String> errors = appender.list.stream()
                .filter(event -> Level.ERROR.equals(event.getLevel()))
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
        assertEquals(1, errors.size(), () -> "expected one report, got " + errors);
        assertTrue(errors.get(0).contains(UnbalancedGroupDAO.class.getName()), errors.get(0));
    }

    // ---------------------------------------------------------------- order and paging

    /**
     * ORDER BY and the paging block are appended on the list side only. They are what the count body
     * must not contain: a count over a paged body would count one page, and a count that sorted would
     * pay for a sort nobody reads. The per-vendor syntax of the block itself is covered by
     * {@link QueryLimitResolverTest}.
     */
    @Test
    void orderAndPagingBelongToTheListQueryOnly() {
        GroupDAO dao = this.capture.wire(new GroupDAO(), DERBY);
        FieldSearchFilter[] filters = {ordered(descriptionLike(), FieldSearchFilter.ASC_ORDER),
                new FieldSearchFilter(10, 5)};

        dao.searchGroups(filters);
        String listQuery = this.capture.single();
        this.capture.clear();
        dao.countGroups(filters);
        String countQuery = this.capture.single();

        assertEquals("OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY", SqlShape.pagingBlock(listQuery));
        assertEquals("", SqlShape.pagingBlock(countQuery));
        assertFalse(SqlShape.normalize(countQuery).contains("ORDER BY"), countQuery);
        assertEquals(SqlShape.listBody(listQuery), SqlShape.countBody(countQuery));
    }

    // ---------------------------------------------------------------- the field whitelist

    /**
     * A filter key becomes a column name by concatenation, so it is checked against the columns the
     * searcher accepts. Values are bound as parameters and were never the exposure; keys are.
     *
     * <p>The REST layer validates keys against a DTO's fields, but that is a guarantee made far from
     * here and absent for every non-REST caller - so the searcher does not rely on it.</p>
     */
    @Test
    void aFilterKeyThatIsNotAColumn_neverReachesTheDriver() {
        GroupDAO dao = this.capture.wire(new GroupDAO(), DERBY);
        FieldSearchFilter[] filters = {new FieldSearchFilter<>("descr) OR 1=1 --", "x", true)};

        assertThrows(RuntimeException.class, () -> dao.searchGroups(filters));
        assertTrue(this.capture.getQueries().isEmpty(),
                () -> "a query was still handed to the driver: " + this.capture.getQueries());
    }

    /** The mirror image: a key that does name a column is untouched. */
    @Test
    void aFilterKeyThatIsAColumn_isAccepted() {
        GroupDAO dao = this.capture.wire(new GroupDAO(), DERBY);

        dao.searchGroups(new FieldSearchFilter[]{descriptionLike()});

        assertTrue(SqlShape.normalize(this.capture.single()).contains("UPPER(authgroups.descr)"),
                this.capture.single());
    }

    /**
     * The allowlist is matched without regard to case, because SQL identifiers are folded by every
     * database the engine supports - <code>ORDER BY guifragment.pluginCode</code> and
     * <code>...plugincode</code> are the same column on Derby, PostgreSQL, MySQL and Oracle.
     *
     * <p>The keys reaching the DAO are DTO field names, which are camelCase: <code>pluginCode</code> is
     * an inherited <code>GuiFragmentDtoSmall</code> field, it passes the REST validator, and
     * <code>GuiFragmentService</code> forwards it without remapping. Matching it exactly would refuse a
     * query that works.</p>
     */
    @Test
    void aFilterKeyDifferingOnlyByCase_isAccepted() {
        GuiFragmentDAO dao = this.capture.wire(new GuiFragmentDAO(), DERBY);

        dao.searchGuiFragments(new FieldSearchFilter[]{sortOnly("pluginCode")});

        assertTrue(SqlShape.normalize(this.capture.single()).contains("ORDER BY guifragment.plugincode"),
                this.capture.single());
    }

    /**
     * And what is concatenated is the column as the searcher declares it, not as the caller spelled it,
     * so caller-supplied text does not reach the query even on the accepting path.
     */
    @Test
    void anAcceptedKey_isEmittedInTheSearchersOwnSpelling() {
        OAuthConsumerDAO dao = this.capture.wire(new OAuthConsumerDAO(), DERBY);

        dao.getConsumerKeys(new FieldSearchFilter[]{sortOnly("issuedDate")});

        String query = SqlShape.normalize(this.capture.single());
        assertTrue(query.contains("issueddate"), query);
        assertFalse(query.contains("issuedDate"), query);
    }

    /** Case folding is not a way past the allowlist: an unknown key is still refused. */
    @Test
    void aFilterKeyThatIsNoColumnInAnyCase_isStillRefused() {
        GuiFragmentDAO dao = this.capture.wire(new GuiFragmentDAO(), DERBY);
        FieldSearchFilter[] filters = {sortOnly("PLUGINCODE) OR 1=1 --")};

        assertThrows(RuntimeException.class, () -> dao.searchGuiFragments(filters));
        assertTrue(this.capture.getQueries().isEmpty(),
                () -> "a query was still handed to the driver: " + this.capture.getQueries());
    }

    /**
     * A searcher whose keys are not its column names declares that as an alias, and the alias resolves
     * to the column. <code>typeCode</code> is the key every entity manager uses; on profiles the column
     * behind it is <code>profiletype</code>.
     *
     * <p>This is the path the three entity searchers used to walk through an if/else chain of their own,
     * each responsible for rejecting the unknown key. Declaring the mapping as data is what let that
     * check move into {@link AbstractSearcherDAO} for every searcher at once.</p>
     */
    @Test
    void anAliasedKey_resolvesToItsColumn() {
        ProbeProfileSearcherDAO dao = this.capture.wire(new ProbeProfileSearcherDAO(), DERBY);

        dao.searchId(new EntitySearchFilter[]{
                ordered(new EntitySearchFilter<>(IEntityManager.ENTITY_TYPE_CODE_FILTER_KEY, false),
                        FieldSearchFilter.ASC_ORDER)});

        String query = SqlShape.normalize(this.capture.single());
        assertTrue(query.contains("authuserprofiles.profiletype"), query);
        assertFalse(query.contains("typeCode"), query);
    }

    /** An alias is the only way in: the column it hides is not itself a key. */
    @Test
    void theColumnBehindAnAlias_isNotAKeyOfItsOwn() {
        ProbeProfileSearcherDAO dao = this.capture.wire(new ProbeProfileSearcherDAO(), DERBY);
        EntitySearchFilter[] filters = {ordered(new EntitySearchFilter<>("profiletype", false),
                FieldSearchFilter.ASC_ORDER)};

        assertThrows(RuntimeException.class, () -> dao.searchId(filters));
        assertTrue(this.capture.getQueries().isEmpty(),
                () -> "a query was still handed to the driver: " + this.capture.getQueries());
    }

    // ---------------------------------------------------------------- the deprecated key hook

    @Test
    void getTableFieldName_isStillAnOverridableDeprecatedHook() throws NoSuchMethodException {
        Method hook = AbstractSearcherDAO.class.getDeclaredMethod("getTableFieldName", String.class);
        Method declaration = AbstractSearcherDAO.class.getDeclaredMethod("getSearchableFields");

        assertTrue(Modifier.isProtected(hook.getModifiers()));
        assertFalse(Modifier.isFinal(hook.getModifiers()));
        assertFalse(Modifier.isAbstract(hook.getModifiers()));
        assertEquals(String.class, hook.getReturnType());
        assertTrue(hook.isAnnotationPresent(Deprecated.class));
        assertFalse(Modifier.isAbstract(declaration.getModifiers()));
    }

    @Test
    void aSearcherOverridingOnlyTheDeprecatedHook_isResolvedThroughIt() {
        LegacySearcherDAO dao = this.capture.wire(new LegacySearcherDAO(), DERBY);
        FieldSearchFilter[] filters = {ordered(new FieldSearchFilter<>("title", "x", true), FieldSearchFilter.ASC_ORDER)};

        List<String> warnings = warningsWhile(() -> dao.search(filters));

        String query = SqlShape.normalize(this.capture.single());
        assertTrue(query.contains("UPPER(legacytable.titlecol)"), query);
        assertTrue(query.contains("ORDER BY legacytable.titlecol ASC"), query);
        assertFalse(warnings.isEmpty());
        warnings.forEach(warning -> {
            assertTrue(warning.contains(LegacySearcherDAO.class.getName()), warning);
            assertTrue(warning.contains("getTableFieldName(String)"), warning);
            assertTrue(warning.contains("getSearchableFields()"), warning);
            assertFalse(warning.contains("\n"), warning);
        });
    }

    @Test
    void aKeyTheDeprecatedHookDoesNotMap_neverReachesTheDriver() {
        LegacySearcherDAO dao = this.capture.wire(new LegacySearcherDAO(), DERBY);
        FieldSearchFilter[] filters = {new FieldSearchFilter<>("unknown", "x", true)};

        assertThrows(RuntimeException.class, () -> dao.search(filters));
        assertTrue(this.capture.getQueries().isEmpty(),
                () -> "a query was still handed to the driver: " + this.capture.getQueries());
    }

    @Test
    void aPassThroughOverride_stillAcceptsAPlainColumnName() {
        PassThroughSearcherDAO dao = this.capture.wire(new PassThroughSearcherDAO(), DERBY);

        warningsWhile(() -> dao.search(new FieldSearchFilter[]{new FieldSearchFilter<>("descr", "x", true)}));

        assertTrue(SqlShape.normalize(this.capture.single()).contains("UPPER(passthrough.descr)"),
                this.capture.single());
    }

    @ParameterizedTest
    @MethodSource("injectedKeys")
    void aPassThroughOverride_neverHandsAnInjectedKeyToTheDriver(String key) {
        PassThroughSearcherDAO dao = this.capture.wire(new PassThroughSearcherDAO(), DERBY);
        FieldSearchFilter[] filters = {ordered(new FieldSearchFilter<>(key, "x", true), FieldSearchFilter.ASC_ORDER)};

        assertThrows(RuntimeException.class, () -> dao.search(filters));
        assertTrue(this.capture.getQueries().isEmpty(),
                () -> "a query was still handed to the driver: " + this.capture.getQueries());
    }

    private static Stream<String> injectedKeys() {
        return Stream.of("descr) OR 1=1 --", "descr; DROP TABLE passthrough", "descr, (SELECT 1)",
                "passthrough.descr", "\"descr\"", "1descr");
    }

    @Test
    void aSearcherDeclaringNoKeys_refusesEveryKeyBeforeTheDriver() {
        NoKeysSearcherDAO dao = this.capture.wire(new NoKeysSearcherDAO(), DERBY);
        FieldSearchFilter[] filters = {new FieldSearchFilter<>("id", "x", false)};

        assertThrows(RuntimeException.class, () -> dao.search(filters));
        assertTrue(this.capture.getQueries().isEmpty(),
                () -> "a query was still handed to the driver: " + this.capture.getQueries());
    }

    @Test
    void anOverrideOnADeclaringSearcher_isStillHonoured() {
        LegacyGroupDAO dao = this.capture.wire(new LegacyGroupDAO(), DERBY);

        warningsWhile(() -> dao.searchGroups(new FieldSearchFilter[]{
                new FieldSearchFilter<>("title", "x", true), descriptionLike()}));

        String query = SqlShape.normalize(this.capture.single());
        assertEquals(2, SqlShape.occurrences(query, "UPPER(authgroups.descr)"), query);
    }

    @Test
    void callingTheDeprecatedHook_resolvesThroughTheDeclaredKeysAndWarnsOnce() {
        ProbeGroupDAO dao = new ProbeGroupDAO();
        String[] column = new String[1];

        List<String> warnings = warningsWhile(() -> column[0] = dao.legacyColumnFor("DESCR"));

        assertEquals("descr", column[0]);
        assertEquals(1, warnings.size(), () -> "expected one warning, got " + warnings);
        assertTrue(warnings.get(0).contains(ProbeGroupDAO.class.getName()), warnings.get(0));
        assertTrue(warnings.get(0).contains("resolveTableFieldName(String)"), warnings.get(0));
        assertFalse(warnings.get(0).contains("\n"), warnings.get(0));
        assertThrows(RuntimeException.class, () -> dao.legacyColumnFor("descr) OR 1=1 --"));
    }

    @Test
    void aSearcherUsingOnlyTheDeclaredKeys_logsNoDeprecationWarning() {
        GroupDAO dao = this.capture.wire(new GroupDAO(), DERBY);

        List<String> warnings = warningsWhile(() -> dao.searchGroups(
                new FieldSearchFilter[]{ordered(descriptionLike(), FieldSearchFilter.ASC_ORDER)}));

        assertTrue(warnings.isEmpty(), () -> "unexpected warnings: " + warnings);
    }

    // ---------------------------------------------------------------- count queries in the previous API's shapes

    @Test
    void aCountBlockThatAlreadyCounts_isReportedByName() {
        CountingBlockGroupDAO dao = this.capture.wire(new CountingBlockGroupDAO(), DERBY);

        List<String> warnings = warningsWhile(() -> dao.countGroups(new FieldSearchFilter[]{descriptionLike()}));

        assertEquals("SELECT COUNT(*) FROM ( SELECT COUNT(*) FROM authgroups WHERE UPPER(authgroups.descr) LIKE ? ) counter",
                SqlShape.normalize(this.capture.single()));
        assertEquals(1, warnings.size(), () -> "expected one warning, got " + warnings);
        assertTrue(warnings.get(0).contains(CountingBlockGroupDAO.class.getName()), warnings.get(0));
        assertTrue(warnings.get(0).contains("createMasterCountQueryBlock()"), warnings.get(0));
        assertFalse(warnings.get(0).contains("\n"), warnings.get(0));
    }

    @Test
    void aCountQueryNotWrappedByToQueryString_isReportedByName() {
        UnwrappedCountGroupDAO dao = this.capture.wire(new UnwrappedCountGroupDAO(), DERBY);

        List<String> warnings = warningsWhile(() -> dao.countGroups(new FieldSearchFilter[]{descriptionLike()}));

        assertEquals("SELECT authgroups.groupname FROM authgroups WHERE UPPER(authgroups.descr) LIKE ?",
                SqlShape.normalize(this.capture.single()));
        assertEquals(1, warnings.size(), () -> "expected one warning, got " + warnings);
        assertTrue(warnings.get(0).contains(UnwrappedCountGroupDAO.class.getName()), warnings.get(0));
        assertTrue(warnings.get(0).contains("toQueryString(StringBuffer, boolean)"), warnings.get(0));
        assertFalse(warnings.get(0).contains("\n"), warnings.get(0));
    }

    @Test
    void anInTreeCountAndList_logNoCountShapeWarning() {
        GroupDAO groups = this.capture.wire(new GroupDAO(), DERBY);
        ProbeProfileSearcherDAO profiles = this.capture.wire(new ProbeProfileSearcherDAO(), DERBY);
        EntitySearchFilter[] entityFilters = {attributeLike(), orderedMetadata()};

        List<String> warnings = warningsWhile(() -> {
            groups.countGroups(new FieldSearchFilter[]{descriptionLike()});
            groups.searchGroups(new FieldSearchFilter[]{descriptionLike()});
            profiles.count(entityFilters);
            profiles.searchId(entityFilters);
        });

        assertTrue(warnings.isEmpty(), () -> "unexpected warnings: " + warnings);
    }

    @Test
    void anUnbalancedCountBlock_isReportedOnlyAsUnbalanced() {
        UnbalancedGroupDAO dao = this.capture.wire(new UnbalancedGroupDAO(), DERBY);

        List<String> warnings = warningsWhile(() -> dao.countGroups(new FieldSearchFilter[]{descriptionLike()}));

        assertTrue(warnings.isEmpty(), () -> "unexpected warnings: " + warnings);
    }

    // ---------------------------------------------------------------- the deprecated order overload

    @Test
    void theDeprecatedOrderOverload_ordersAsTheUngroupedFormAndWarnsOnce() {
        OrderProbeProfileSearcherDAO dao = new OrderProbeProfileSearcherDAO();
        EntitySearchFilter[] filters = {orderedMetadata(),
                ordered(new EntitySearchFilter<>("Nome", true, "abc", true), FieldSearchFilter.ASC_ORDER)};
        String[] deprecatedBlock = new String[1];

        List<String> warnings = warningsWhile(() -> deprecatedBlock[0] = dao.deprecatedOrderBlock(filters));

        assertEquals(dao.ungroupedOrderBlock(filters), deprecatedBlock[0]);
        assertEquals(1, warnings.size(), () -> "expected one warning, got " + warnings);
        assertTrue(warnings.get(0).contains(OrderProbeProfileSearcherDAO.class.getName()), warnings.get(0));
        assertTrue(warnings.get(0).contains("appendOrderQueryBlocks(EntitySearchFilter[], StringBuffer, boolean, boolean)"),
                warnings.get(0));
        assertFalse(warnings.get(0).contains("\n"), warnings.get(0));
    }

    private static List<String> warningsWhile(Runnable action) {
        List<Logger> loggers = List.of((Logger) LoggerFactory.getLogger(AbstractSearcherDAO.class),
                (Logger) LoggerFactory.getLogger(AbstractEntitySearcherDAO.class));
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        loggers.forEach(logger -> logger.addAppender(appender));
        try {
            action.run();
        } finally {
            loggers.forEach(logger -> logger.detachAppender(appender));
        }
        appender.list.forEach(event -> assertEquals(null, event.getThrowableProxy()));
        return appender.list.stream()
                .filter(event -> Level.WARN.equals(event.getLevel()))
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    // ---------------------------------------------------------------- fixtures

    private static FieldSearchFilter sortOnly(String key) {
        FieldSearchFilter filter = new FieldSearchFilter<>(key);
        filter.setSortOnly(true);
        filter.setOrder(FieldSearchFilter.ASC_ORDER);
        return filter;
    }

    private static FieldSearchFilter descriptionLike() {
        return new FieldSearchFilter<>("descr", "test", true);
    }

    private static FieldSearchFilter ordered(FieldSearchFilter filter, String order) {
        filter.setOrder(order);
        return filter;
    }

    private static EntitySearchFilter ordered(EntitySearchFilter filter, String order) {
        filter.setOrder(order);
        return filter;
    }

    private static EntitySearchFilter attributeLike() {
        return new EntitySearchFilter<>("Nome", true, "abc", true);
    }

    private static EntitySearchFilter orderedMetadata() {
        return ordered(new EntitySearchFilter<>(IEntityManager.ENTITY_TYPE_CODE_FILTER_KEY, false, "PFL", false),
                FieldSearchFilter.ASC_ORDER);
    }

    /** Exposes the count entry point: no manager in the engine reaches it for profiles. */
    private static class ProbeProfileSearcherDAO extends UserProfileSearcherDAO {

        Integer count(EntitySearchFilter[] filters) {
            return this.countId(filters);
        }
    }

    /** Written against the previous API: maps its keys in the deprecated hook and declares none. */
    @SuppressWarnings("deprecation")
    private static class LegacySearcherDAO extends AbstractSearcherDAO {

        List<String> search(FieldSearchFilter[] filters) {
            return this.searchId(filters);
        }

        @Override
        protected String getTableFieldName(String metadataFieldKey) {
            return "title".equals(metadataFieldKey) ? "titlecol" : null;
        }

        @Override
        protected String getMasterTableName() {
            return "legacytable";
        }

        @Override
        protected String getMasterTableIdFieldName() {
            return "id";
        }
    }

    /** The override most client searchers carry: the caller's key, returned as the column. */
    @SuppressWarnings("deprecation")
    private static class PassThroughSearcherDAO extends AbstractSearcherDAO {

        List<String> search(FieldSearchFilter[] filters) {
            return this.searchId(filters);
        }

        @Override
        protected String getTableFieldName(String metadataFieldKey) {
            return metadataFieldKey;
        }

        @Override
        protected String getMasterTableName() {
            return "passthrough";
        }

        @Override
        protected String getMasterTableIdFieldName() {
            return "id";
        }
    }

    /** Declares no keys and does not override the deprecated hook. */
    private static class NoKeysSearcherDAO extends AbstractSearcherDAO {

        List<String> search(FieldSearchFilter[] filters) {
            return this.searchId(filters);
        }

        @Override
        protected String getMasterTableName() {
            return "nokeystable";
        }

        @Override
        protected String getMasterTableIdFieldName() {
            return "id";
        }
    }

    /** Extends an in-tree searcher, adding a key through the deprecated hook and falling back on it. */
    @SuppressWarnings("deprecation")
    private static class LegacyGroupDAO extends GroupDAO {

        @Override
        protected String getTableFieldName(String metadataFieldKey) {
            return "title".equals(metadataFieldKey) ? "descr" : super.getTableFieldName(metadataFieldKey);
        }
    }

    @SuppressWarnings("deprecation")
    private static class ProbeGroupDAO extends GroupDAO {

        String legacyColumnFor(String key) {
            return this.getTableFieldName(key);
        }
    }

    private static class OrderProbeProfileSearcherDAO extends UserProfileSearcherDAO {

        @SuppressWarnings("deprecation")
        String deprecatedOrderBlock(EntitySearchFilter[] filters) {
            StringBuffer query = new StringBuffer();
            this.appendOrderQueryBlocks(filters, query, false);
            return query.toString();
        }

        String ungroupedOrderBlock(EntitySearchFilter[] filters) {
            StringBuffer query = new StringBuffer();
            this.appendOrderQueryBlocks(filters, query, false, false);
            return query.toString();
        }
    }

    /** Returns the count block the previous API expected: a counting query, not the body to count. */
    private static class CountingBlockGroupDAO extends GroupDAO {

        @Override
        protected StringBuffer createMasterCountQueryBlock() {
            return new StringBuffer("SELECT COUNT(*) FROM ").append(this.getMasterTableName()).append(" ");
        }
    }

    /** Assembles its query as the previous API did, without returning through toQueryString. */
    private static class UnwrappedCountGroupDAO extends GroupDAO {

        @Override
        protected String createQueryString(FieldSearchFilter[] filters, boolean isCount, boolean selectAll) {
            StringBuffer query = this.createBaseQueryBlock(filters, isCount, selectAll);
            this.appendMetadataFieldFilterQueryBlocks(filters, query, false);
            if (!isCount) {
                this.appendOrderQueryBlocks(filters, query, false);
                this.appendLimitQueryBlock(filters, query);
            }
            return query.toString();
        }
    }

    /** A subclass that writes an opening marker of its own: the wrapper then opens twice, closes once. */
    private static class UnbalancedGroupDAO extends GroupDAO {

        @Override
        protected StringBuffer createMasterCountQueryBlock() {
            return new StringBuffer(COUNT_QUERY_PREFIX).append(super.createMasterCountQueryBlock());
        }
    }

}
