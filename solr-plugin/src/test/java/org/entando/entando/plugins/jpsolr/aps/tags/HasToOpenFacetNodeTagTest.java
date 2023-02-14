package org.entando.entando.plugins.jpsolr.aps.tags;

import java.util.List;
import java.util.Map;
import javax.servlet.jsp.tagext.Tag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HasToOpenFacetNodeTagTest extends BaseFacetNavTest {

    @BeforeEach
    void setUp() {
        Mockito.when(pageContext.getRequest()).thenReturn(servletRequest);
    }

    @Test
    void shouldNotOpenFacetWhenOnlyFirstChildIsSelected() throws Exception {
        mockChildCategory1();
        mockSubChildCategory1();
        Mockito.when(servletRequest.getAttribute(REQUIRED_FACETS_PARAM)).thenReturn(List.of(CHILD_CATEGORY_1));
        Assertions.assertEquals(0, executeDoStartTag());
    }

    @Test
    void shouldOpenFacetWhenSubChildIsSelected() throws Exception {
        mockChildCategory1();
        mockSubChildCategory1();
        Mockito.when(servletRequest.getAttribute(REQUIRED_FACETS_PARAM)).thenReturn(
                List.of(CHILD_CATEGORY_1, SUB_CHILD_CATEGORY_1));
        Mockito.when(servletRequest.getAttribute(OCCURRENCES_PARAM)).thenReturn(
                Map.of(CHILD_CATEGORY_1, 0, SUB_CHILD_CATEGORY_1, 1));
        Assertions.assertEquals(Tag.EVAL_BODY_INCLUDE, executeDoStartTag());
    }

    private int executeDoStartTag() throws Exception {
        HasToOpenFacetNodeTag tag = new HasToOpenFacetNodeTag();
        tag.setPageContext(pageContext);
        tag.setFacetNodeCode(CHILD_CATEGORY_1);
        tag.setRequiredFacetsParamName(REQUIRED_FACETS_PARAM);
        tag.setOccurrencesParamName(OCCURRENCES_PARAM);
        return tag.doStartTag();
    }
}
