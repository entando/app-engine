package org.entando.entando.plugins.jpsolr.aps.tags;

import java.util.List;
import javax.servlet.jsp.tagext.Tag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HasToViewFacetNodeTagTest extends BaseFacetNavTest {

    @BeforeEach
    void setUp() {
        Mockito.when(pageContext.getRequest()).thenReturn(servletRequest);
    }

    @Test
    void shouldNotDisplayFacetWhenOnlyFirstChildIsSelected() throws Exception {
        mockChildCategory1();
        HasToViewFacetNodeTag tag = getTag();
        Assertions.assertEquals(0, tag.doStartTag());
    }

    @Test
    void shouldDisplayFacetWhenSubChildIsSelected() throws Exception {
        mockParentCategory1();
        mockChildCategory1();
        mockSubChildCategory1();
        Mockito.when(servletRequest.getParameter("facetNode_1")).thenReturn(CHILD_CATEGORY_1);
        Mockito.when(servletRequest.getParameter(SELECTED_NODE_PARAM)).thenReturn(SUB_CHILD_CATEGORY_1);
        HasToViewFacetNodeTag tag = getTag();
        tag.setRequiredFacets(List.of(CHILD_CATEGORY_1, SUB_CHILD_CATEGORY_1));
        Assertions.assertEquals(Tag.EVAL_BODY_INCLUDE, tag.doStartTag());
    }

    private HasToViewFacetNodeTag getTag() throws Exception {
        HasToViewFacetNodeTag tag = new HasToViewFacetNodeTag();
        tag.setPageContext(pageContext);
        tag.setFacetNodeCode(CHILD_CATEGORY_1);
        tag.setRequiredFacetsParamName(REQUIRED_FACETS_PARAM);
        tag.setOccurrencesParamName(OCCURRENCES_PARAM);
        return tag;
    }

}
