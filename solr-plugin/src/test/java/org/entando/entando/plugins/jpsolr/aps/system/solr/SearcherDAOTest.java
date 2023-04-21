package org.entando.entando.plugins.jpsolr.aps.system.solr;

import com.agiletec.aps.system.common.tree.ITreeNode;
import com.agiletec.aps.system.common.tree.ITreeNodeManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.NumericSearchEngineFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
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

@ExtendWith(MockitoExtension.class)
class SearcherDAOTest {

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

    @Test
    void shouldFilterUsingTextSearchOptionAtLeastOneIfTextSearchOptionIsNull_SingleValue() throws Exception {
        mockDefaultLang();
        SearchEngineFilter filter = new SearchEngineFilter("key", true, "value", null);

        SearchEngineFilter[] filters = new SearchEngineFilter[]{filter};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+(en_key:value) +(entity_group:free)");
    }

    @Test
    void shouldFilterUsingTextSearchOptionAtLeastOneIfTextSearchOptionIsNull_MultipleValues() throws Exception {
        mockDefaultLang();
        SearchEngineFilter filter = new SearchEngineFilter("key", true, List.of("value1", "value2"), null);

        SearchEngineFilter[] filters = new SearchEngineFilter[]{filter};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+((en_key:value1) (en_key:value2)) +(entity_group:free)");
    }

    @Test
    void shouldFilterUsingTextSearchOptionExact_SingleValue() throws Exception {
        mockDefaultLang();
        SearchEngineFilter filter = new SearchEngineFilter("key", true, "value", TextSearchOption.EXACT);

        SearchEngineFilter[] filters = new SearchEngineFilter[]{filter};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+en_key:\"value\" +(entity_group:free)");
    }

    @Test
    void shouldFilterUsingTextSearchOptionExact_MultipleValues() throws Exception {
        mockDefaultLang();
        SearchEngineFilter filter = new SearchEngineFilter("key", true, List.of("value1", "value2"),
                TextSearchOption.EXACT);

        SearchEngineFilter[] filters = new SearchEngineFilter[]{filter};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+(en_key:\"value1\" en_key:\"value2\") +(entity_group:free)");
    }

    @Test
    void shouldFilterByDate() throws Exception {
        mockDefaultLang();
        Date date = new GregorianCalendar(2023, 4, 21).getTime();

        SearchEngineFilter filter = new SearchEngineFilter("key", true, date, TextSearchOption.EXACT);

        SearchEngineFilter[] filters = new SearchEngineFilter[]{filter};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+(+en_key:2023-05-20T22:00:00Z) +(entity_group:free)");
    }

    @Test
    void shouldAddEntityPrefixOnlyIfNeeded() throws Exception {
        SolrSearchEngineFilter filter1 = new SolrSearchEngineFilter("entity_key1", false, "value1");
        SolrSearchEngineFilter filter2 = new SolrSearchEngineFilter("key2", false, "value2");

        SearchEngineFilter[] filters = new SearchEngineFilter[]{filter1, filter2};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+(entity_key1:value1) +(entity_key2:value2) +(entity_group:free)");
    }

    @Test
    void shouldAddSingleCategoryFilterWithMissingCategory() throws Exception {
        testAddSingleCategoryFilter();
    }

    @Test
    void shouldAddSingleCategoryFilterWithExistingCategory() throws Exception {
        mockCategory("cat1");
        testAddSingleCategoryFilter();
    }

    private void testAddSingleCategoryFilter() throws Exception {
        SolrSearchEngineFilter categoryFilter = new SolrSearchEngineFilter("category", "cat1");

        SearchEngineFilter[] filters = new SearchEngineFilter[]{};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{categoryFilter};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+(entity_group:free) +(+entity_category:cat1)");
    }

    @Test
    void shouldAddMultipleCategoryFilter() throws Exception {
        mockCategory("cat1");
        SolrSearchEngineFilter categoriesFilter = new SolrSearchEngineFilter("category", List.of("cat1", "cat2"));

        SearchEngineFilter[] filters = new SearchEngineFilter[]{};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{categoriesFilter};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+(entity_group:free) +(+(entity_category:cat1 entity_category:cat2))");
    }

    @Test
    void shouldHandleNumericFilterSingleValue() throws Exception {
        SolrSearchEngineFilter numericFilter = new SolrSearchEngineFilter("key", 5);

        SearchEngineFilter[] filters = new SearchEngineFilter[]{numericFilter};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+(+entity_key:5) +(entity_group:free)");
    }

    @Test
    void shouldHandleNumericFilterMultipleValues() throws Exception {
        mockDefaultLang();
        NumericSearchEngineFilter numericFilter = NumericSearchEngineFilter
                .createAllowedValuesFilter("key", true, List.of(5, 6));

        SearchEngineFilter[] filters = new SearchEngineFilter[]{numericFilter};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+(en_key:5 en_key:6) +(entity_group:free)");
    }

    @Test
    void shouldFilterOnSingleValueAllWords() throws Exception {
        SolrSearchEngineFilter filter = new SolrSearchEngineFilter("key", "value1 value2", TextSearchOption.ALL_WORDS);

        SearchEngineFilter[] filters = new SearchEngineFilter[]{filter};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+(+entity_key:value1 +entity_key:value2) +(entity_group:free)");
    }

    @Test
    void shouldUseInsertedLangCode() throws Exception {
        SolrSearchEngineFilter filter = new SolrSearchEngineFilter("key", true);
        filter.setLangCode("sl");

        SearchEngineFilter[] filters = new SearchEngineFilter[]{filter};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups, "+(+sl_key:*) +(entity_group:free)");
    }

    @Test
    void shouldCreateAttachmentFilterExact() throws Exception {
        SolrSearchEngineFilter filter = new SolrSearchEngineFilter("key", "value", TextSearchOption.EXACT);
        filter.setIncludeAttachments(true);

        SearchEngineFilter[] filters = new SearchEngineFilter[]{filter};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+(entity_key:\"value\" entity_key_attachment:\"value\") +(entity_group:free)");
    }

    @Test
    void shouldCreateAttachmentFilterNotExact() throws Exception {
        SolrSearchEngineFilter filter = new SolrSearchEngineFilter("key", "value", TextSearchOption.ANY_WORD);
        filter.setIncludeAttachments(true);

        SearchEngineFilter[] filters = new SearchEngineFilter[]{filter};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+((entity_key:value entity_key_attachment:value)) +(entity_group:free)");
    }

    @Test
    void shouldAddFreeGroupIfAdminGroupIsNotPresent() throws Exception {
        SearchEngineFilter[] filters = new SearchEngineFilter[]{};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>(List.of("group1"));

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+(entity_group:group1 entity_group:free)");
    }

    @Test
    void shouldNotAddFreeGroupIfAdminGroupIsPresent() throws Exception {
        SearchEngineFilter[] filters = new SearchEngineFilter[]{};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>(List.of(Group.ADMINS_GROUP_NAME));

        testSearchFacetedContents(filters, categories, allowedGroups, "*:*");
    }

    @Test
    void shouldFilterOnMultipleValuesAllWords() throws Exception {
        SolrSearchEngineFilter filter = new SolrSearchEngineFilter("key", List.of("value1", "value2"),
                TextSearchOption.ALL_WORDS);

        SearchEngineFilter[] filters = new SearchEngineFilter[]{filter};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+((+entity_key:value1) (+entity_key:value2)) +(entity_group:free)");
    }

    @Test
    void shouldFilterOnMultipleValuesAnyWord() throws Exception {
        SolrSearchEngineFilter filter = new SolrSearchEngineFilter("key", List.of("value1", "value2"),
                TextSearchOption.ANY_WORD);

        SearchEngineFilter[] filters = new SearchEngineFilter[]{filter};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+((entity_key:value1) (entity_key:value2)) +(entity_group:free)");
    }

    @Test
    void shouldFilterOnRangeOfDate() throws Exception {
        Date date1 = new GregorianCalendar(2023, 4, 21).getTime();
        Date date2 = new GregorianCalendar(2023, 4, 30).getTime();
        SolrSearchEngineFilter filter = new SolrSearchEngineFilter("key", date1, date2);

        SearchEngineFilter[] filters = new SearchEngineFilter[]{filter};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+(+entity_key:[2023-05-20T22:00:00Z TO 2023-05-29T22:00:00Z]) +(entity_group:free)");
    }

    @Test
    void shouldFilterOnRangeOfNumbers() throws Exception {
        SolrSearchEngineFilter filter = new SolrSearchEngineFilter("key", 10, 20);

        SearchEngineFilter[] filters = new SearchEngineFilter[]{filter};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+(+entity_key:[10 TO 20]) +(entity_group:free)");
    }

    @Test
    void shouldFilterOnRangeOfStrings() throws Exception {
        SolrSearchEngineFilter filter = new SolrSearchEngineFilter("key", "A", "E");

        SearchEngineFilter[] filters = new SearchEngineFilter[]{filter};
        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        testSearchFacetedContents(filters, categories, allowedGroups,
                "+(+entity_key:[a TO ez]) +(entity_group:free)");
    }

    @Test
    void shouldHandleArrayOfArraysFilters() throws Exception {
        mockDefaultLang();

        ArgumentCaptor<SolrQuery> queryCaptor = ArgumentCaptor.forClass(SolrQuery.class);
        QueryResponse queryResponse = mockQueryResponse();
        Mockito.when(solrClient.query(Mockito.any(), queryCaptor.capture())).thenReturn(queryResponse);

        SolrSearchEngineFilter filterSort = new SolrSearchEngineFilter("created", false);
        SolrSearchEngineFilter filterValue = new SolrSearchEngineFilter("key", true, "value");
        SolrSearchEngineFilter filterValue1 = new SolrSearchEngineFilter("key1", true, "value1");
        SolrSearchEngineFilter filterValue2 = new SolrSearchEngineFilter("key2", true, "value2");
        SolrSearchEngineFilter filterPaginator = new SolrSearchEngineFilter(1, 100);

        SearchEngineFilter[][] filters = new SearchEngineFilter[][]{
                new SearchEngineFilter[]{filterSort},
                new SearchEngineFilter[]{filterValue},
                new SearchEngineFilter[]{filterValue1, filterValue2},
                new SearchEngineFilter[]{filterPaginator},
        };

        SearchEngineFilter[] categories = new SearchEngineFilter[]{};
        List<String> allowedGroups = new ArrayList<>();

        searcherDAO.searchFacetedContents(filters, categories, allowedGroups);

        SolrQuery query = queryCaptor.getValue();
        Assertions.assertEquals(
                "+(+entity_created:*) +(en_key:value) +((en_key1:value1) (en_key2:value2)) +(entity_group:free)",
                query.getQuery());
    }

    private void testSearchFacetedContents(SearchEngineFilter[] filters, SearchEngineFilter[] categories,
            List<String> allowedGroups, String expectedQuery) throws Exception {
        ArgumentCaptor<SolrQuery> queryCaptor = ArgumentCaptor.forClass(SolrQuery.class);
        QueryResponse queryResponse = mockQueryResponse();
        Mockito.when(solrClient.query(Mockito.any(), queryCaptor.capture())).thenReturn(queryResponse);

        searcherDAO.searchFacetedContents(filters, categories, allowedGroups);

        SolrQuery query = queryCaptor.getValue();
        Assertions.assertEquals(expectedQuery, query.getQuery());
    }

    private void mockDefaultLang() {
        Lang lang = new Lang();
        lang.setCode("en");
        Mockito.when(langManager.getDefaultLang()).thenReturn(lang);
    }

    private void mockCategory(String categoryCode) {
        Mockito.when(treeNodeManager.getNode(categoryCode)).thenReturn(Mockito.mock(ITreeNode.class));
    }

    private QueryResponse mockQueryResponse() {
        QueryResponse queryResponse = Mockito.mock(QueryResponse.class);
        SolrDocumentList documents = new SolrDocumentList();
        Mockito.when(queryResponse.getResults()).thenReturn(documents);
        return queryResponse;
    }
}
