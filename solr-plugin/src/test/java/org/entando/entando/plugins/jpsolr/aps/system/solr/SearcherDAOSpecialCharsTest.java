package org.entando.entando.plugins.jpsolr.aps.system.solr;

import com.agiletec.aps.system.common.tree.ITreeNodeManager;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import java.util.ArrayList;
import java.util.List;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter.TextSearchOption;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrSearchEngineFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Special-character handling tests for SearcherDAO query building.
 *
 * <p>Verifies that user-supplied search terms containing Lucene/Solr reserved
 * characters (e.g. {@code : ~ + - ( ) [ ] " \ *} and whitespace tokens such as
 * {@code " TO "}) are properly escaped across the different query-building paths:
 * <ul>
 *   <li>Attribute-filter term queries — plain single-term, LIKE and wildcard
 *       (leading/trailing {@code *}) inputs handled by
 *       {@link SearcherDAO#getTermQueryForTextSearch}.</li>
 *   <li>String range bounds built in {@code createRangeQuery}.</li>
 *   <li>Full-text EXACT / phrase searches, where Lucene's
 *       {@code PhraseQuery.toString()} emits a quoted value.</li>
 *   <li>Full-text attachment-inclusive ({@code ANY_WORD}) searches.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class SearcherDAOSpecialCharsTest {

    @Mock
    private SolrClient solrClient;
    @Mock
    private ILangManager langManager;
    @Mock
    private ITreeNodeManager treeNodeManager;

    @InjectMocks
    private SearcherDAO searcherDAO;

    @BeforeEach
    void setUp() {
        searcherDAO.setLangManager(langManager);
        searcherDAO.setTreeNodeManager(treeNodeManager);
    }

    // ------------------------------------------------------------------
    // Plain / LIKE / wildcard term escaping
    // ------------------------------------------------------------------

    @Test
    void shouldEscapeColonInPlainSingleValue() throws Exception {
        mockDefaultLang();
        SearchEngineFilter filter = new SearchEngineFilter("key", true, "foo:bar", null);

        runAndAssert(filter, "+(en_key:foo\\:bar) +(entity_group:free)");
    }

    @Test
    void shouldEscapePlusAndParenthesesInPlainValue() throws Exception {
        mockDefaultLang();
        SearchEngineFilter filter = new SearchEngineFilter("key", true, "(a+b)", null);

        runAndAssert(filter, "+(en_key:\\(a\\+b\\)) +(entity_group:free)");
    }

    @Test
    void shouldEscapeColonInLikeSearch() throws Exception {
        mockDefaultLang();
        SolrSearchEngineFilter<String> filter =
                new SolrSearchEngineFilter<>("key", true, "foo:bar");
        filter.setLikeOption(true);

        runAndAssert(filter, "+(en_key:*foo\\:bar*) +(entity_group:free)");
    }

    @Test
    void shouldEscapeCoreOfLeadingAndTrailingWildcardValue() throws Exception {
        mockDefaultLang();
        SearchEngineFilter filter = new SearchEngineFilter("key", true, "*foo:bar*", null);

        runAndAssert(filter, "+(en_key:*foo\\:bar*) +(entity_group:free)");
    }

    @Test
    void shouldEscapeCoreOfLeadingWildcardValue() throws Exception {
        mockDefaultLang();
        SearchEngineFilter filter = new SearchEngineFilter("key", true, "*foo:bar", null);

        runAndAssert(filter, "+(en_key:*foo\\:bar) +(entity_group:free)");
    }

    @Test
    void shouldEscapeCoreOfTrailingWildcardValue() throws Exception {
        mockDefaultLang();
        SearchEngineFilter filter = new SearchEngineFilter("key", true, "foo:bar*", null);

        runAndAssert(filter, "+(en_key:foo\\:bar*) +(entity_group:free)");
    }

    @Test
    void shouldNotThrowOnSingleAsteriskValue() throws Exception {
        mockDefaultLang();
        SearchEngineFilter filter = new SearchEngineFilter("key", true, "*", null);

        ArgumentCaptor<SolrQuery> queryCaptor = ArgumentCaptor.forClass(SolrQuery.class);
        QueryResponse queryResponse = mockQueryResponse();
        Mockito.when(solrClient.query(Mockito.any(), queryCaptor.capture()))
                .thenReturn(queryResponse);

        searcherDAO.searchFacetedContents(new SearchEngineFilter[]{filter},
                new SearchEngineFilter[]{}, new ArrayList<>());

        // current implementation produces a wildcard query whose toString contains "**"
        // — semantically equivalent to "*" and accepted by Solr.
        Assertions.assertTrue(queryCaptor.getValue().getQuery().contains("en_key:*"),
                "expected wildcard query, got: " + queryCaptor.getValue().getQuery());
    }

    // ------------------------------------------------------------------
    // Full-text search term escaping (TextSearchOption-based)
    // ------------------------------------------------------------------

    @Test
    void shouldEscapeColonInFullTextAnyWordSearch() throws Exception {
        SolrSearchEngineFilter<String> filter =
                new SolrSearchEngineFilter<>("key", "foo:bar", TextSearchOption.ANY_WORD);

        runAndAssert(filter, "+(entity_key:foo\\:bar) +(entity_group:free)");
    }

    @Test
    void shouldEscapeTildeInFullTextAtLeastOneWordSearch() throws Exception {
        SolrSearchEngineFilter<String> filter =
                new SolrSearchEngineFilter<>("key", "foo~bar", TextSearchOption.AT_LEAST_ONE_WORD);

        runAndAssert(filter, "+(entity_key:foo\\~bar) +(entity_group:free)");
    }

    @Test
    void shouldEscapeMultipleSpecialCharsInFullTextAllWordsSearch() throws Exception {
        SolrSearchEngineFilter<String> filter =
                new SolrSearchEngineFilter<>("key", "foo:bar baz+qux", TextSearchOption.ALL_WORDS);

        runAndAssert(filter,
                "+(+entity_key:foo\\:bar +entity_key:baz\\+qux) +(entity_group:free)");
    }

    @Test
    void shouldEscapeParenthesesInFullTextAtLeastOneWordSearch() throws Exception {
        SolrSearchEngineFilter<String> filter =
                new SolrSearchEngineFilter<>("key", "(a)", TextSearchOption.AT_LEAST_ONE_WORD);

        runAndAssert(filter, "+(entity_key:\\(a\\)) +(entity_group:free)");
    }

    @Test
    void shouldEscapeSpecialCharsOnAttachmentNotExactBranch() throws Exception {
        SolrSearchEngineFilter<String> filter =
                new SolrSearchEngineFilter<>("key", "foo:bar", TextSearchOption.ANY_WORD);
        filter.setIncludeAttachments(true);

        runAndAssert(filter,
                "+((entity_key:foo\\:bar entity_key_attachment:foo\\:bar)) +(entity_group:free)");
    }

    // ------------------------------------------------------------------
    // String range bound escaping
    // ------------------------------------------------------------------

    /**
     * A closing bracket in the lower bound would otherwise break the
     * generated Solr range syntax {@code [lower TO upper]}.
     */
    @Test
    void shouldEscapeClosingBracketInStringRangeStart() throws Exception {
        SolrSearchEngineFilter<String> filter = new SolrSearchEngineFilter<>("key", "a]b", "x");

        runAndAssert(filter, "+(+entity_key:[a\\]b TO xz]) +(entity_group:free)");
    }

    @Test
    void shouldEscapeOpeningBracketInStringRangeEnd() throws Exception {
        SolrSearchEngineFilter<String> filter = new SolrSearchEngineFilter<>("key", "a", "x[y");

        runAndAssert(filter, "+(+entity_key:[a TO x\\[yz]) +(entity_group:free)");
    }

    @Test
    void shouldEscapeWhitespaceTokenInStringRangeBound() throws Exception {
        // " TO " inside a bound currently breaks the Solr range parser.
        SolrSearchEngineFilter<String> filter = new SolrSearchEngineFilter<>("key", "a TO z", "m");

        runAndAssert(filter, "+(+entity_key:[a\\ to\\ z TO mz]) +(entity_group:free)");
    }

    @Test
    void shouldEscapeQuoteInStringRangeBound() throws Exception {
        SolrSearchEngineFilter<String> filter = new SolrSearchEngineFilter<>("key", "a\"b", "m");

        runAndAssert(filter, "+(+entity_key:[a\\\"b TO mz]) +(entity_group:free)");
    }

    @Test
    void shouldEscapeBackslashInStringRangeBound() throws Exception {
        SolrSearchEngineFilter<String> filter = new SolrSearchEngineFilter<>("key", "a\\b", "m");

        runAndAssert(filter, "+(+entity_key:[a\\\\b TO mz]) +(entity_group:free)");
    }

    // ------------------------------------------------------------------
    // EXACT / phrase paths — safe in Solr because PhraseQuery.toString()
    // emits double-quoted output. Kept here to detect any future change.
    // ------------------------------------------------------------------

    @Test
    void exactPhraseEmitsQuotedOutput_documentingCurrentBehavior() throws Exception {
        SolrSearchEngineFilter<String> filter =
                new SolrSearchEngineFilter<>("key", "foo:bar", TextSearchOption.EXACT);

        runAndAssert(filter, "+entity_key:\"foo:bar\" +(entity_group:free)");
    }

    @Test
    void exactPhraseMultipleValuesEmitsQuotedOutput_documentingCurrentBehavior() throws Exception {
        SolrSearchEngineFilter<String> filter = new SolrSearchEngineFilter<>(
                "key", List.of("foo:bar", "baz+qux"), TextSearchOption.EXACT);

        runAndAssert(filter,
                "+(entity_key:\"foo:bar\" entity_key:\"baz+qux\") +(entity_group:free)");
    }

    // ------------------------------------------------------------------
    // helpers (mirror SearcherDAOTest)
    // ------------------------------------------------------------------

    private void runAndAssert(SearchEngineFilter filter, String expectedQuery) throws Exception {
        ArgumentCaptor<SolrQuery> queryCaptor = ArgumentCaptor.forClass(SolrQuery.class);
        QueryResponse queryResponse = mockQueryResponse();
        Mockito.when(solrClient.query(Mockito.any(), queryCaptor.capture()))
                .thenReturn(queryResponse);

        searcherDAO.searchFacetedContents(new SearchEngineFilter[]{filter},
                new SearchEngineFilter[]{}, new ArrayList<>());

        Assertions.assertEquals(expectedQuery, queryCaptor.getValue().getQuery());
    }

    private void mockDefaultLang() {
        Lang lang = new Lang();
        lang.setCode("en");
        Mockito.when(langManager.getDefaultLang()).thenReturn(lang);
    }

    private QueryResponse mockQueryResponse() {
        QueryResponse queryResponse = Mockito.mock(QueryResponse.class);
        SolrDocumentList documents = new SolrDocumentList();
        Mockito.when(queryResponse.getResults()).thenReturn(documents);
        return queryResponse;
    }
}