package org.entando.entando.plugins.jpsolr.aps.system.content;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import com.agiletec.aps.system.services.category.Category;
import com.agiletec.aps.system.services.category.CategoryManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.plugins.jacms.aps.system.services.content.widget.UserFilterOptionBean;
import java.util.List;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter;
import org.entando.entando.plugins.jpsolr.aps.system.solr.SolrSearchEngineManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdvContentFacetManagerTest {

    private static final String CATEGORY_1 = "category1";
    private static final String CATEGORY_2 = "category2";

    @Mock
    private CategoryManager categoryManager;
    @Mock
    private SolrSearchEngineManager searchEngineManager;

    @InjectMocks
    private AdvContentFacetManager facetManager;

    @Test
    void shouldGetFacetResultWithBeansFilterAndNodeCodesAsList() throws Exception {
        UserFilterOptionBean filterOptionBean = Mockito.mock(UserFilterOptionBean.class);
        Mockito.when(filterOptionBean.extractFilter()).thenReturn(Mockito.mock(SearchEngineFilter.class));
        Mockito.when(categoryManager.getCategory(CATEGORY_1)).thenReturn(new Category());

        SearchEngineFilter[] baseFilters = new SearchEngineFilter[]{};
        List<String> facetNodeCodes = List.of(CATEGORY_1, CATEGORY_2);
        List<UserFilterOptionBean> filterOptionBeans = List.of(filterOptionBean);
        List<String> groups = List.of(Group.FREE_GROUP_NAME);
        facetManager.getFacetResult(baseFilters, facetNodeCodes, filterOptionBeans, groups);

        ArgumentCaptor<SearchEngineFilter[]> filtersCaptor = ArgumentCaptor.forClass(SearchEngineFilter[].class);
        ArgumentCaptor<SearchEngineFilter[]> categoryFiltersCaptor = ArgumentCaptor.forClass(
                SearchEngineFilter[].class);

        Mockito.verify(searchEngineManager).searchFacetedEntities(
                filtersCaptor.capture(), categoryFiltersCaptor.capture(), eq(groups));

        SearchEngineFilter[] filters = filtersCaptor.getValue();
        SearchEngineFilter[] categoryFilters = categoryFiltersCaptor.getValue();

        Assertions.assertEquals(1, filters.length);
        Assertions.assertEquals(1, categoryFilters.length);
        Assertions.assertEquals(CATEGORY_1, categoryFilters[0].getValue());
    }

    @Test
    void shouldGetFacetResultWithNullFilters() throws Exception {
        facetManager.getFacetResult(null, (List<String>) null, null, null);

        ArgumentCaptor<SearchEngineFilter[]> filtersCaptor = ArgumentCaptor.forClass(SearchEngineFilter[].class);

        Mockito.verify(searchEngineManager).searchFacetedEntities(
                filtersCaptor.capture(), (SearchEngineFilter[]) isNull(), isNull());

        SearchEngineFilter[] filters = filtersCaptor.getValue();
        Assertions.assertEquals(0, filters.length);
    }

    @Test
    void shouldGetFacetResultWithNodeCodesAsFilter() throws Exception {
        SearchEngineFilter[] nodeCodesFilter = new SearchEngineFilter[]{};
        facetManager.getFacetResult(null, nodeCodesFilter, null, null);
        Mockito.verify(searchEngineManager).searchFacetedEntities(
                any(SearchEngineFilter[].class), eq(nodeCodesFilter), isNull());
    }
}
