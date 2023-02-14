package org.entando.entando.plugins.jpsolr.aps.tags;

import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class URLParTagTest {

    @Test
    void shouldAddParameterToParentURLTag() throws Exception {
        URLTag parentUrlTag = Mockito.mock(URLTag.class);
        TagSupport anotherParentTag = new TagSupport();
        anotherParentTag.setParent(parentUrlTag);

        BodyContent bodyContent = Mockito.mock(BodyContent.class);
        Mockito.when(bodyContent.getString()).thenReturn("body");

        URLParTag urlParTag = new URLParTag();
        urlParTag.setParent(anotherParentTag);
        urlParTag.setName("name");
        urlParTag.setBodyContent(bodyContent);

        Assertions.assertEquals(Tag.EVAL_PAGE, urlParTag.doEndTag());
        Mockito.verify(parentUrlTag).addParameter("name", "body");
    }
}
