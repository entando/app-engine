package com.agiletec.aps.tags;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.tags.util.HeadInfoContainer;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.jsp.PageContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HeadInfoTagTest {

    private static final String TYPE = "myType";

    @Mock
    private PageContext pageContext;
    @Mock
    private ServletRequest servletRequest;
    @Mock
    private RequestContext reqCtx;

    @BeforeEach
    void setUp() {
        Mockito.when(pageContext.getRequest()).thenReturn(servletRequest);
        Mockito.when(servletRequest.getAttribute(RequestContext.REQCTX)).thenReturn(reqCtx);
    }

    @Test
    void shouldIncludeExistingInfo() throws Exception {
        HeadInfoContainer headInfoContainer = new HeadInfoContainer();
        Mockito.when(reqCtx.getExtraParam(SystemConstants.EXTRAPAR_HEAD_INFO_CONTAINER)).thenReturn(headInfoContainer);
        HeadInfoTag tag = new HeadInfoTag();
        tag.setPageContext(pageContext);
        tag.setType(TYPE);
        tag.setInfo("myInfo");
        tag.doEndTag();
        Assertions.assertEquals("myInfo", headInfoContainer.getInfos(TYPE).get(0));

        Assertions.assertEquals(TYPE, tag.getType());
        Assertions.assertEquals("myInfo", tag.getInfo());
        tag.release();
        Assertions.assertNull(tag.getType());
        Assertions.assertNull(tag.getInfo());
    }

    @Test
    void shouldExtractExistingInfoFromAttribute() throws Exception {
        HeadInfoContainer headInfoContainer = new HeadInfoContainer();
        Mockito.when(reqCtx.getExtraParam(SystemConstants.EXTRAPAR_HEAD_INFO_CONTAINER)).thenReturn(headInfoContainer);
        Mockito.when(pageContext.getAttribute("myVar")).thenReturn("attributeValue");
        HeadInfoTag tag = new HeadInfoTag();
        tag.setPageContext(pageContext);
        tag.setType(TYPE);
        tag.setVar("myVar");
        tag.doEndTag();
        Assertions.assertEquals("attributeValue", headInfoContainer.getInfos(TYPE).get(0));

        Assertions.assertEquals(TYPE, tag.getType());
        Assertions.assertEquals("myVar", tag.getVar());
        tag.release();
        Assertions.assertNull(tag.getType());
        Assertions.assertNull(tag.getVar());
    }

    @Test
    void shouldIgnoreMissingInfo() throws Exception {
        HeadInfoContainer headInfoContainer = new HeadInfoContainer();
        Mockito.when(reqCtx.getExtraParam(SystemConstants.EXTRAPAR_HEAD_INFO_CONTAINER)).thenReturn(headInfoContainer);
        HeadInfoTag tag = new HeadInfoTag();
        tag.setPageContext(pageContext);
        tag.setType(TYPE);
        tag.doEndTag();
        Assertions.assertNull(headInfoContainer.getInfos(TYPE));
        Mockito.verify(reqCtx).getExtraParam(SystemConstants.EXTRAPAR_HEAD_INFO_CONTAINER);
    }
}
