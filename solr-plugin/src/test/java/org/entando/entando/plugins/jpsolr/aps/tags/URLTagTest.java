package org.entando.entando.plugins.jpsolr.aps.tags;

import static org.springframework.web.context.WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.url.IURLManager;
import com.agiletec.aps.system.services.url.PageURL;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(MockitoExtension.class)
class URLTagTest {

    private static final String URL = "http://localhost:8080/entando-de-app/en/solr.page?selectedNode=mycategory";

    @Mock
    private PageContext pageContext;
    @Mock
    private HttpServletRequest request;
    @Mock
    private RequestContext reqCtx;
    @Mock
    private ServletContext servletContext;
    @Mock
    private WebApplicationContext webApplicationContext;
    @Mock
    private IURLManager urlManager;
    @Mock
    private JspWriter jspWriter;
    @Mock
    private PageURL pageURL;

    @BeforeEach
    void setUp() {
        Mockito.when(pageContext.getRequest()).thenReturn(request);
        Mockito.when(pageContext.getServletContext()).thenReturn(servletContext);
        Mockito.when(request.getAttribute(RequestContext.REQCTX)).thenReturn(reqCtx);
        Mockito.when(servletContext.getAttribute(ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(webApplicationContext);
        Mockito.when(webApplicationContext.getBean(SystemConstants.URL_MANAGER)).thenReturn(urlManager);
        Mockito.when(pageURL.getURL()).thenReturn(URL);
        Mockito.when(urlManager.createURL(reqCtx)).thenReturn(pageURL);
    }

    @Test
    void shouldPrintUrl() throws Exception {
        Mockito.when(pageContext.getOut()).thenReturn(jspWriter);
        URLTag tag = new URLTag();
        tag.setPageContext(pageContext);
        Assertions.assertEquals(Tag.EVAL_BODY_INCLUDE, tag.doStartTag());
        Assertions.assertEquals(Tag.EVAL_PAGE, tag.doEndTag());
        Mockito.verify(jspWriter).print(URL);
    }

    @Test
    void shouldPrintUrlUsingTagAttributes() throws Exception {
        Mockito.when(pageContext.getOut()).thenReturn(jspWriter);
        URLTag tag = new URLTag();
        tag.setPageContext(pageContext);
        tag.setLang("it");
        tag.setEscapeAmp(true);
        tag.setPage("pageCode");
        Assertions.assertEquals(Tag.EVAL_BODY_INCLUDE, tag.doStartTag());
        Assertions.assertEquals(Tag.EVAL_PAGE, tag.doEndTag());
        Mockito.verify(jspWriter).print(URL);
        Mockito.verify(pageURL).setLangCode("it");
        Mockito.verify(pageURL).setEscapeAmp(true);
        Mockito.verify(pageURL).setPageCode("pageCode");
        Assertions.assertEquals("it", tag.getLang());
        Assertions.assertTrue(tag.isEscapeAmp());
        Assertions.assertEquals("pageCode", tag.getPage());
        tag.release();
        Assertions.assertNull(tag.getPage());
        Assertions.assertNull(tag.getLang());
    }

    @Test
    void shouldPrintUrlRemovingExcludedParameters() throws Exception {
        Mockito.when(pageContext.getOut()).thenReturn(jspWriter);
        URLTag tag = new URLTag();
        tag.setPageContext(pageContext);
        tag.setParamRepeat(true);
        tag.setExcludeParameters("param1,param2");
        Assertions.assertEquals(Tag.EVAL_BODY_INCLUDE, tag.doStartTag());
        Assertions.assertEquals(Tag.EVAL_PAGE, tag.doEndTag());
        Mockito.verify(jspWriter).print(URL);
        ArgumentCaptor<List<String>> paramsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(pageURL).setParamRepeat(paramsCaptor.capture());
        List<String> params = paramsCaptor.getValue();
        Assertions.assertEquals(3, params.size());
        Assertions.assertEquals("param1", params.get(0));
        Assertions.assertEquals("param2", params.get(1));
        Assertions.assertEquals(SystemConstants.LOGIN_PASSWORD_PARAM_NAME, params.get(2));
        Assertions.assertEquals("param1,param2", tag.getExcludeParameters());
        tag.release();
        Assertions.assertNull(tag.getExcludeParameters());
    }

    @Test
    void shouldUseVar() throws Exception {
        URLTag tag = new URLTag();
        tag.setPageContext(pageContext);
        tag.setVar("var");
        Assertions.assertEquals(Tag.EVAL_BODY_INCLUDE, tag.doStartTag());
        Assertions.assertEquals(Tag.EVAL_PAGE, tag.doEndTag());
        Mockito.verify(pageContext).setAttribute("var", URL);
        Assertions.assertEquals("var", tag.getVar());
        tag.release();
        Assertions.assertNull(tag.getVar());
    }
}
