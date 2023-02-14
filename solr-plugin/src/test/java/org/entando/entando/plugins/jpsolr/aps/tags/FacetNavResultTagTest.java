package org.entando.entando.plugins.jpsolr.aps.tags;

import static org.entando.entando.plugins.jpsolr.aps.tags.AbstractFacetNavTag.SOLR_RESULT_REQUEST_PARAM;
import static org.mockito.ArgumentMatchers.eq;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.Widget;
import java.util.List;
import org.entando.entando.plugins.jpsolr.aps.tags.util.FacetBreadCrumbs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class FacetNavResultTagTest extends BaseFacetNavTest {

    private static final String BREADCRUMBS_PARAM = "breadCrumbs";
    private static final String RESULT_PARAM = "contentList";

    @BeforeEach
    void setUp() {
        Widget widget = super.createFacetTreeWidget();
        IPage page = Mockito.mock(IPage.class);
        Mockito.when(page.getWidgets()).thenReturn(new Widget[]{null, widget, null});
        Mockito.when(requestContext.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE)).thenReturn(page);
        Mockito.when(requestContext.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_FRAME)).thenReturn(2);
        Mockito.when(pageContext.getRequest()).thenReturn(servletRequest);
        Mockito.when(servletRequest.getAttribute(RequestContext.REQCTX)).thenReturn(requestContext);
    }

    @Test
    void shouldSetRequiredFacetsAndBreadcrumbsWithoutParameters() throws Exception {
        mockParentCategory1();
        mockParentCategory2();
        Mockito.when(requestContext.getExtraParam(SOLR_RESULT_REQUEST_PARAM)).thenReturn(facetedContentsResult);

        executeDoStartTag();

        verifyRequiredFacets();

        List<FacetBreadCrumbs> breadCrumbs = getBreadcrumbsToVerify();
        Assertions.assertTrue(breadCrumbs.isEmpty());
    }

    @Test
    void shouldSetRequiredFacetsAndBreadcrumbsWithOnlySelectedNode() throws Exception {
        mockParentCategory1();
        mockParentCategory2();
        mockChildCategory1();

        Mockito.when(servletRequest.getParameter(SELECTED_NODE_PARAM)).thenReturn(CHILD_CATEGORY_1);
        Mockito.when(requestContext.getExtraParam(SOLR_RESULT_REQUEST_PARAM)).thenReturn(facetedContentsResult);

        executeDoStartTag();

        verifyRequiredFacets(CHILD_CATEGORY_1);

        List<FacetBreadCrumbs> breadCrumbs = getBreadcrumbsToVerify();
        Assertions.assertEquals(1, breadCrumbs.size());
        verifyChild1Breadcrumb(breadCrumbs.get(0));
    }

    @Test
    void shouldSetRequiredFacetsAndBreadcrumbsWithSelectedNodeAndFacetNode() throws Exception {
        Mockito.when(servletRequest.getParameter(SELECTED_NODE_PARAM)).thenReturn(CHILD_CATEGORY_1);
        Mockito.when(servletRequest.getParameter("facetNode_1")).thenReturn(CHILD_CATEGORY_2);
        Mockito.when(requestContext.getExtraParam(SOLR_RESULT_REQUEST_PARAM)).thenReturn(facetedContentsResult);

        mockParentCategory1();
        mockParentCategory2();
        mockChildCategory1();
        mockChildCategory2();

        executeDoStartTag();

        verifyRequiredFacets(CHILD_CATEGORY_2, CHILD_CATEGORY_1);

        List<FacetBreadCrumbs> breadCrumbs = getBreadcrumbsToVerify();
        Assertions.assertEquals(2, breadCrumbs.size());
        verifyChild2Breadcrumb(breadCrumbs.get(0));
        verifyChild1Breadcrumb(breadCrumbs.get(1));
    }

    @Test
    void shouldSetRequiredFacetsAndBreadcrumbsWithFacetNodeAndRemovedNode() throws Exception {
        Mockito.when(servletRequest.getParameter("facetNode_1")).thenReturn(CHILD_CATEGORY_1);
        Mockito.when(servletRequest.getParameter("facetNode_2")).thenReturn(CHILD_CATEGORY_2);
        Mockito.when(servletRequest.getParameter("facetNodeToRemove_1")).thenReturn(PARENT_CATEGORY_1);
        Mockito.when(servletRequest.getParameter("facetNodeToRemove_2")).thenReturn(CHILD_CATEGORY_1);
        Mockito.when(requestContext.getExtraParam(SOLR_RESULT_REQUEST_PARAM)).thenReturn(facetedContentsResult);

        mockParentCategory1();
        mockParentCategory2();
        mockChildCategory2();

        executeDoStartTag();

        verifyRequiredFacets(CHILD_CATEGORY_2);

        List<FacetBreadCrumbs> breadCrumbs = getBreadcrumbsToVerify();
        Assertions.assertEquals(1, breadCrumbs.size());
        verifyChild2Breadcrumb(breadCrumbs.get(0));
    }

    private void executeDoStartTag() throws Exception {
        FacetNavResultTag tag = new FacetNavResultTag();
        tag.setPageContext(pageContext);
        tag.setBreadCrumbsParamName(BREADCRUMBS_PARAM);
        tag.setResultParamName(RESULT_PARAM);
        tag.setRequiredFacetsParamName(REQUIRED_FACETS_PARAM);
        tag.doStartTag();
    }

    private void verifyRequiredFacets(String... expectedFacets) {
        ArgumentCaptor<List<String>> facetsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(pageContext).setAttribute(eq(REQUIRED_FACETS_PARAM), facetsCaptor.capture());
        List<String> facets = facetsCaptor.getValue();
        Assertions.assertEquals(expectedFacets.length, facets.size());
        for (int i = 0; i < expectedFacets.length; i++) {
            Assertions.assertEquals(expectedFacets[i], facets.get(i));
        }
    }

    private void verifyChild1Breadcrumb(FacetBreadCrumbs breadCrumb) {
        Assertions.assertEquals(PARENT_CATEGORY_1, breadCrumb.getBreadCrumbs().get(0));
        Assertions.assertEquals(CHILD_CATEGORY_1, breadCrumb.getBreadCrumbs().get(1));
        Assertions.assertEquals(PARENT_CATEGORY_1, breadCrumb.getFacetRoot());
        Assertions.assertEquals(CHILD_CATEGORY_1, breadCrumb.getRequiredFacet());
    }

    private void verifyChild2Breadcrumb(FacetBreadCrumbs breadCrumb) {
        Assertions.assertEquals(PARENT_CATEGORY_2, breadCrumb.getBreadCrumbs().get(0));
        Assertions.assertEquals(CHILD_CATEGORY_2, breadCrumb.getBreadCrumbs().get(1));
        Assertions.assertEquals(PARENT_CATEGORY_2, breadCrumb.getFacetRoot());
        Assertions.assertEquals(CHILD_CATEGORY_2, breadCrumb.getRequiredFacet());
    }

    private List<FacetBreadCrumbs> getBreadcrumbsToVerify() {
        ArgumentCaptor<List<FacetBreadCrumbs>> breadCrumbsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(pageContext).setAttribute(eq(BREADCRUMBS_PARAM), breadCrumbsCaptor.capture());
        return breadCrumbsCaptor.getValue();
    }
}
