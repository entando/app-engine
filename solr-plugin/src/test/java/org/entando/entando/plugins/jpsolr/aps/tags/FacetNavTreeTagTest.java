package org.entando.entando.plugins.jpsolr.aps.tags;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.page.Widget;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class FacetNavTreeTagTest extends BaseFacetNavTest {

    @BeforeEach
    void setUp() throws Exception {
        Mockito.when(facetNavHelper.getResult(any(), any())).thenReturn(facetedContentsResult);
        Mockito.when(pageContext.getRequest()).thenReturn(servletRequest);
        Mockito.when(servletRequest.getAttribute(RequestContext.REQCTX)).thenReturn(requestContext);

        Widget widget = createFacetTreeWidget();
        Mockito.when(requestContext.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_WIDGET)).thenReturn(widget);
    }

    @Test
    void shouldSetRequiredFacetsWithoutParameters() throws Exception {
        mockParentCategory1();
        mockParentCategory2();
        executeDoStartTag();
        verifyRequiredFacets();
    }

    @Test
    void shouldSetRequiredFacetsWithOnlySelectedNode() throws Exception {
        Mockito.when(servletRequest.getParameter(SELECTED_NODE_PARAM)).thenReturn(CHILD_CATEGORY_1);

        mockParentCategory1();
        mockParentCategory2();
        mockChildCategory1();

        executeDoStartTag();

        verifyRequiredFacets(CHILD_CATEGORY_1);
    }

    @Test
    void shouldSetRequiredFacetsWithSelectedNodeAndFacetNode() throws Exception {
        Mockito.when(servletRequest.getParameter(SELECTED_NODE_PARAM)).thenReturn(CHILD_CATEGORY_1);
        Mockito.when(servletRequest.getParameter("facetNode_1")).thenReturn(CHILD_CATEGORY_2);

        mockParentCategory1();
        mockParentCategory2();
        mockChildCategory1();
        mockChildCategory2();

        executeDoStartTag();

        verifyRequiredFacets(CHILD_CATEGORY_2, CHILD_CATEGORY_1);
    }

    @Test
    void shouldSetRequiredFacetsWithFacetNodeAndRemovedNode() throws Exception {
        Mockito.when(servletRequest.getParameter("facetNode_1")).thenReturn(CHILD_CATEGORY_1);
        Mockito.when(servletRequest.getParameter("facetNode_2")).thenReturn(CHILD_CATEGORY_2);
        Mockito.when(servletRequest.getParameter("facetNodeToRemove_1")).thenReturn(PARENT_CATEGORY_1);
        Mockito.when(servletRequest.getParameter("facetNodeToRemove_2")).thenReturn(CHILD_CATEGORY_1);

        mockParentCategory1();
        mockParentCategory2();
        mockChildCategory2();

        executeDoStartTag();

        verifyRequiredFacets(CHILD_CATEGORY_2);
    }

    private void executeDoStartTag() throws Exception {
        FacetNavTreeTag tag = new FacetNavTreeTag();
        tag.setPageContext(pageContext);
        tag.setRequiredFacetsParamName(REQUIRED_FACETS_PARAM);
        tag.doStartTag();
    }

    private void verifyRequiredFacets(String... expectedFacets) {
        ArgumentCaptor<List<String>> facetsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(servletRequest).setAttribute(eq(REQUIRED_FACETS_PARAM), facetsCaptor.capture());
        List<String> facets = facetsCaptor.getValue();
        Assertions.assertEquals(expectedFacets.length, facets.size());
        for (int i = 0; i < expectedFacets.length; i++) {
            Assertions.assertEquals(expectedFacets[i], facets.get(i));
        }
    }
}
