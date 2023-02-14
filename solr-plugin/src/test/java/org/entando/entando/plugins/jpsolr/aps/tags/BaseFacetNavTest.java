package org.entando.entando.plugins.jpsolr.aps.tags;

import static org.mockito.ArgumentMatchers.any;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.common.tree.ITreeNodeManager;
import com.agiletec.aps.system.services.category.Category;
import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.aps.util.ApsProperties;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import org.entando.entando.aps.system.services.searchengine.FacetedContentsResult;
import org.entando.entando.plugins.jpsolr.aps.system.JpSolrSystemConstants;
import org.entando.entando.plugins.jpsolr.aps.system.content.widget.IFacetNavHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(MockitoExtension.class)
abstract class BaseFacetNavTest {

    protected static final String PARENT_CATEGORY_1 = "parentCategory1";
    protected static final String PARENT_CATEGORY_2 = "parentCategory2";
    protected static final String CHILD_CATEGORY_1 = "childCategory1";
    protected static final String CHILD_CATEGORY_2 = "childCategory2";
    protected static final String SUB_CHILD_CATEGORY_1 = "subChildCategory1";

    protected static final String REQUIRED_FACETS_PARAM = "requiredFacets";
    protected static final String SELECTED_NODE_PARAM = "selectedNode";
    protected static final String OCCURRENCES_PARAM = "occurrences";

    @Mock
    protected RequestContext requestContext;
    @Mock
    protected PageContext pageContext;
    @Mock
    protected ServletContext servletContext;
    @Mock
    protected HttpServletRequest servletRequest;
    @Mock
    protected WebApplicationContext webApplicationContext;
    @Mock
    protected IFacetNavHelper facetNavHelper;
    @Mock
    protected ITreeNodeManager facetManager;
    @Mock
    protected FacetedContentsResult facetedContentsResult;

    @BeforeEach
    void baseSetUp() {
        Mockito.when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(webApplicationContext);
        Mockito.when(pageContext.getServletContext()).thenReturn(servletContext);
        Mockito.when(webApplicationContext.getBean(JpSolrSystemConstants.CONTENT_FACET_NAV_HELPER))
                .thenReturn(facetNavHelper);
        Mockito.when(facetNavHelper.getTreeNodeManager()).thenReturn(facetManager);
        Mockito.lenient().when(facetManager.getNode(null)).thenReturn(null);
        Mockito.lenient().when(servletRequest.getParameter(any())).thenReturn(null);
    }

    protected Widget createFacetTreeWidget() {
        Widget widget = new Widget();
        widget.setTypeCode("jpsolr_facetTree");
        ApsProperties config = new ApsProperties();
        config.setProperty(JpSolrSystemConstants.FACET_ROOTS_WIDGET_PARAM_NAME,
                String.join(",", PARENT_CATEGORY_1, PARENT_CATEGORY_2));
        widget.setConfig(config);
        return widget;
    }

    protected void mockParentCategory1() {
        Category category = new Category();
        category.setCode(PARENT_CATEGORY_1);
        category.setChildrenCodes(new String[]{CHILD_CATEGORY_1});
        Mockito.when(facetManager.getNode(PARENT_CATEGORY_1)).thenReturn(category);
    }

    protected void mockParentCategory2() {
        Category category = new Category();
        category.setCode(PARENT_CATEGORY_2);
        category.setChildrenCodes(new String[]{CHILD_CATEGORY_2});
        Mockito.when(facetManager.getNode(PARENT_CATEGORY_2)).thenReturn(category);
    }

    protected void mockChildCategory1() {
        Category category = new Category();
        category.setCode(CHILD_CATEGORY_1);
        category.setParentCode(PARENT_CATEGORY_1);
        category.setChildrenCodes(new String[]{SUB_CHILD_CATEGORY_1});
        Mockito.when(facetManager.getNode(CHILD_CATEGORY_1)).thenReturn(category);
    }

    protected void mockChildCategory2() {
        Category category = new Category();
        category.setCode(CHILD_CATEGORY_2);
        category.setParentCode(PARENT_CATEGORY_2);
        Mockito.when(facetManager.getNode(CHILD_CATEGORY_2)).thenReturn(category);
    }

    protected void mockSubChildCategory1() {
        Category category = new Category();
        category.setCode(SUB_CHILD_CATEGORY_1);
        category.setParentCode(CHILD_CATEGORY_1);
        Mockito.when(facetManager.getNode(SUB_CHILD_CATEGORY_1)).thenReturn(category);
    }
}
