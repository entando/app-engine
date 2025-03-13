package org.entando.entando.aps.tags;

import org.entando.entando.aps.servlet.routing.VirtualContextHelper;
import org.entando.entando.aps.util.UrlUtils.EntUrlBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class EntandoModuleUrlTagTest {

    @Mock
    private PageContext pageContext;
    @Mock
    private JspWriter jspWriter;
    @Mock
    private EntUrlBuilder entUrlBuilder;
    @Mock
    private VirtualContextHelper virtualContextHelper;


    @InjectMocks
    private EntandoModuleUrlTag tag;

    @Test
    void testDoEndTag_Success() throws Exception {
        // Arrange
        String baseUrl = "http://example.com";
        String path = "/path/to/resource";
        String virtualContextPath = "/vctx";
        String expectedUrl = "http://example.com/vctx/path/to/resource";

        when(pageContext.getOut()).thenReturn(jspWriter);
        VirtualContextHelper.setThreadLocal_VirtualContextPath(virtualContextPath);
        tag.setBaseUrl(baseUrl);
        tag.setPath(path);
        tag.setVirtualContext(virtualContextPath);

        // Act
        int result = tag.doEndTag();

        // Assert
        assertEquals(TagSupport.EVAL_PAGE, result);
        verify(jspWriter).print(expectedUrl);
    }


    @Test
    void testDoEndTag_NullVirtualContext() throws Exception {
        // Arrange
        String baseUrl = "http://example.com";
        String path = "/path/to/resource";
        String expectedUrl = "http://example.com/path/to/resource";
        String virtualContextPath = "";

        when(pageContext.getOut()).thenReturn(jspWriter);
        VirtualContextHelper.setThreadLocal_VirtualContextPath(virtualContextPath);
        tag.setBaseUrl(baseUrl);
        tag.setPath(path);

        // Act
        int result = tag.doEndTag();

        // Assert
        assertEquals(TagSupport.EVAL_PAGE, result);
        verify(jspWriter).print(expectedUrl);
    }


    @ParameterizedTest
    @CsvSource({
            "http://example.com, /vctx, /path, http://example.com/vctx/path",
            "https://another.site, , /another/path, https://another.site/another/path",
            "http://test.com, /context/path, , http://test.com/context/path",
            "http://localhost:8080, /my/vctx/context, /a/long/path, http://localhost:8080/my/vctx/context/a/long/path"
    })
    void testCompose(String baseUrl, String virtualContext, String path, String expectedUrl) {
        String actualUrl = EntandoModuleUrlTag.compose(baseUrl, virtualContext, path);
        assertEquals(expectedUrl, actualUrl);
    }


    @Test
    void testGetCurrentVirtualContextPath() {
        VirtualContextHelper.setThreadLocal_VirtualContextPath(null);
        assertEquals("", tag.getCurrentVirtualContextPath());

        VirtualContextHelper.setThreadLocal_VirtualContextPath("");
        assertEquals("", tag.getCurrentVirtualContextPath());

        VirtualContextHelper.setThreadLocal_VirtualContextPath("/mycontext");
        assertEquals("/mycontext", tag.getCurrentVirtualContextPath());
    }
}