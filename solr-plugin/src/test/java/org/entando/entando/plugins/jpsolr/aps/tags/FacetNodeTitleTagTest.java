package org.entando.entando.plugins.jpsolr.aps.tags;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.tree.ITreeNodeManager;
import com.agiletec.aps.system.services.category.Category;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import org.entando.entando.plugins.jpsolr.aps.system.content.widget.IFacetNavHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(MockitoExtension.class)
class FacetNodeTitleTagTest {

    private static final String LANG_CODE_EN = "en";
    private static final String LANG_CODE_IT = "it";
    private static final String PARENT_CATEGORY_CODE = "parentCategory";
    private static final String CATEGORY_1_CODE = "category1";

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
    private JspWriter jspWriter;

    @BeforeEach
    void setUp() {
        Mockito.when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(webApplicationContext);
        Mockito.when(pageContext.getServletContext()).thenReturn(servletContext);
        Mockito.when(pageContext.getRequest()).thenReturn(servletRequest);
        Mockito.when(servletRequest.getAttribute(RequestContext.REQCTX)).thenReturn(requestContext);
        Mockito.when(webApplicationContext.getBean(IFacetNavHelper.class)).thenReturn(facetNavHelper);
        Mockito.when(facetNavHelper.getTreeNodeManager()).thenReturn(facetManager);
        Mockito.when(pageContext.getOut()).thenReturn(jspWriter);
    }

    @Test
    void shouldWriteFacetTitle() throws Exception {
        mockCategory("Category 1");
        mockCurrentLangToEn();

        FacetNodeTitleTag tag = new FacetNodeTitleTag();
        tag.setFacetNodeCode(CATEGORY_1_CODE);
        tag.setPageContext(pageContext);
        tag.doStartTag();

        Mockito.verify(jspWriter).write("Category 1");
    }

    @Test
    void shouldWriteFacetFullTitleWithDefaultSeparator() throws Exception {
        mockParentCategory("Parent Category");
        mockCategory("Category 1");
        mockCurrentLangToEn();

        FacetNodeTitleTag tag = new FacetNodeTitleTag();
        tag.setFacetNodeCode(CATEGORY_1_CODE);
        tag.setPageContext(pageContext);
        tag.setFullTitle(true);
        tag.doStartTag();

        Mockito.verify(jspWriter).write("Parent Category / Category 1");
    }

    @Test
    void shouldWriteFacetFullTitleWithCustomSeparator() throws Exception {
        mockParentCategory("Parent Category");
        mockCategory("Category 1");
        mockCurrentLangToEn();

        FacetNodeTitleTag tag = new FacetNodeTitleTag();
        tag.setFacetNodeCode(CATEGORY_1_CODE);
        tag.setPageContext(pageContext);
        tag.setFullTitle(true);
        tag.setSeparator(" | ");
        tag.doStartTag();

        Mockito.verify(jspWriter).write("Parent Category | Category 1");

        Assertions.assertEquals(" | ", tag.getSeparator());
        tag.release();
        Assertions.assertEquals(" / ", tag.getSeparator());
    }

    @Test
    void shouldWriteFacetTitleEscapingXml() throws Exception {
        mockCategory("<p>Category 1</p>");
        mockCurrentLangToEn();

        FacetNodeTitleTag tag = new FacetNodeTitleTag();
        tag.setFacetNodeCode(CATEGORY_1_CODE);
        tag.setPageContext(pageContext);
        tag.doStartTag();

        Mockito.verify(jspWriter).write("&lt;p&gt;Category 1&lt;/p&gt;");

        Assertions.assertEquals(CATEGORY_1_CODE, tag.getFacetNodeCode());
        tag.release();
        Assertions.assertNull(tag.getFacetNodeCode());
    }

    @Test
    void shouldWriteFacetTitleWithoutEscapingXml() throws Exception {
        mockCategory("<p>Category 1</p>");
        mockCurrentLangToEn();

        FacetNodeTitleTag tag = new FacetNodeTitleTag();
        tag.setFacetNodeCode(CATEGORY_1_CODE);
        tag.setPageContext(pageContext);
        tag.setFullTitle(true);
        tag.setEscapeXml(false);
        tag.doStartTag();

        Mockito.verify(jspWriter).print("<p>Category 1</p>");

        Assertions.assertTrue(tag.isFullTitle());
        tag.release();
        Assertions.assertFalse(tag.isFullTitle());
    }

    @Test
    void shouldHandleUnknownNode() throws Exception {
        FacetNodeTitleTag tag = new FacetNodeTitleTag();
        tag.setFacetNodeCode(CATEGORY_1_CODE);
        tag.setPageContext(pageContext);
        tag.doStartTag();

        Mockito.verify(jspWriter).print("UNKNOWN FACET");
    }

    @Test
    void shouldFallbackToDefaultLangIfTitleInCurrentLangIsNotSet() throws Exception {
        mockCategory(LANG_CODE_IT, "Categoria 1");
        mockCurrentLangToEn();

        Lang defaultLang = new Lang();
        defaultLang.setCode("it");

        ILangManager langManager = Mockito.mock(ILangManager.class);
        Mockito.when(webApplicationContext.getBean(SystemConstants.LANGUAGE_MANAGER)).thenReturn(langManager);
        Mockito.when(langManager.getDefaultLang()).thenReturn(defaultLang);

        FacetNodeTitleTag tag = new FacetNodeTitleTag();
        tag.setFacetNodeCode(CATEGORY_1_CODE);
        tag.setPageContext(pageContext);
        tag.doStartTag();

        Mockito.verify(jspWriter).write("Categoria 1");
    }

    private void mockCategory(String title) {
        mockCategory(LANG_CODE_EN, title);
    }

    private void mockCategory(String lang, String title) {
        Category category = new Category();
        category.setCode(CATEGORY_1_CODE);
        category.setTitle(lang, title);
        category.setParentCode(PARENT_CATEGORY_CODE);
        Mockito.when(facetManager.getNode(CATEGORY_1_CODE)).thenReturn(category);
    }

    private void mockParentCategory(String title) {
        Category parentCategory = new Category();
        parentCategory.setCode(PARENT_CATEGORY_CODE);
        parentCategory.setTitle(LANG_CODE_EN, title);
        parentCategory.setParentCode("root");
        Mockito.when(facetManager.getNode(PARENT_CATEGORY_CODE)).thenReturn(parentCategory);
    }

    private void mockCurrentLangToEn() {
        Lang lang = new Lang();
        lang.setCode(LANG_CODE_EN);
        Mockito.when(requestContext.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG)).thenReturn(lang);
    }
}
