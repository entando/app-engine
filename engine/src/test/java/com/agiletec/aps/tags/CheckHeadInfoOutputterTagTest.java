package com.agiletec.aps.tags;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.tags.util.HeadInfoContainer;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.Tag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckHeadInfoOutputterTagTest {

    private static final String TYPE = "myType";

    @Mock
    private PageContext pageContext;
    @Mock
    private ServletRequest servletRequest;
    @Mock
    private RequestContext reqCtx;

    @BeforeEach
    void setUp() {
        HeadInfoContainer headInfoContainer = new HeadInfoContainer();
        headInfoContainer.addInfo(TYPE, "info1");
        headInfoContainer.addInfo(TYPE, "info2");
        Mockito.when(reqCtx.getExtraParam(SystemConstants.EXTRAPAR_HEAD_INFO_CONTAINER)).thenReturn(headInfoContainer);
        Mockito.when(pageContext.getRequest()).thenReturn(servletRequest);
        Mockito.when(servletRequest.getAttribute(RequestContext.REQCTX)).thenReturn(reqCtx);
    }

    @Test
    void shouldIncludeExistingInfo() throws Exception {
        CheckHeadInfoOutputterTag tag = new CheckHeadInfoOutputterTag();
        tag.setPageContext(pageContext);
        tag.setType(TYPE);
        Assertions.assertEquals(Tag.EVAL_BODY_INCLUDE, tag.doStartTag());
        tag.release();
        Assertions.assertNull(tag.getType());
    }

    @Test
    void shouldSkipBodyWhenInfoIsMissing() throws Exception {
        CheckHeadInfoOutputterTag tag = new CheckHeadInfoOutputterTag();
        tag.setPageContext(pageContext);
        tag.setType("anotherType");
        Assertions.assertEquals(Tag.SKIP_BODY, tag.doStartTag());
    }
}
