package org.entando.entando.apsadmin.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import com.agiletec.apsadmin.category.CategoryAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class TestRequestLoggingInterceptor extends ApsAdminBaseTestCase {

    @BeforeEach
    void init() throws Exception {
        this.setUserOnSession("admin");
        this.initAction("/do/Category", "save");
        // MockHttpServletRequest URI defaults to ""; set it so the interceptor's /do/ guard activates.
        ((MockHttpServletRequest) this.getRequest()).setRequestURI("/do/Category/save");
    }

    @Test
    void testCleanParameterPassesThrough() throws Throwable {
        this.addParameter("categoryCode", "myCategory");
        this.executeAction();
        CategoryAction action = (CategoryAction) this.getAction();
        assertEquals("myCategory", action.getCategoryCode());
    }

    @Test
    void testScriptTagStripped() throws Throwable {
        this.addParameter("categoryCode", "<script>alert('xss')</script>safe");
        this.executeAction();
        CategoryAction action = (CategoryAction) this.getAction();
        String code = action.getCategoryCode();
        assertFalse(code.contains("<script>"), "opening script tag must be stripped");
        assertFalse(code.contains("</script>"), "closing script tag must be stripped");
        assertFalse(code.contains("alert"), "script content must be stripped");
        assertEquals("safe", code, "non-tag content after the script block must survive");
    }

    @Test
    void testJavascriptSchemeStripped() throws Throwable {
        this.addParameter("categoryCode", "javascript:alert(1)");
        this.executeAction();
        CategoryAction action = (CategoryAction) this.getAction();
        assertFalse(action.getCategoryCode().contains("javascript:"), "javascript: scheme must be stripped");
    }

    @Test
    void testEventHandlerStripped() throws Throwable {
        this.addParameter("categoryCode", "x onerror=alert(1) y");
        this.executeAction();
        CategoryAction action = (CategoryAction) this.getAction();
        assertFalse(action.getCategoryCode().contains("onerror="), "event handler attribute must be stripped");
    }

    @Test
    void testAttributeBreakoutQuotesStripped() throws Throwable {
        this.addParameter("categoryCode", "\" onmouseover=\"alert(1)");
        this.executeAction();
        CategoryAction action = (CategoryAction) this.getAction();
        String code = action.getCategoryCode();
        assertFalse(code.contains("onmouseover="), "event handler must be stripped");
        assertFalse(code.contains("\""), "double quotes must be stripped from contaminated value");
    }

    @Test
    void testColonKeyedParameterNotSanitized() throws Throwable {
        // Parameters whose name contains ':' bypass the interceptor's sanitization.
        // Execution must complete without exceptions regardless.
        this.addParameter("Image:metadata", "<script>test</script>");
        this.addParameter("categoryCode", "safe");
        this.executeAction();
        CategoryAction action = (CategoryAction) this.getAction();
        assertEquals("safe", action.getCategoryCode());
    }
}
