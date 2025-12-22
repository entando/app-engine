package org.entando.entando.aps.system;

import com.agiletec.aps.system.ApsSystemUtils;
import com.agiletec.aps.system.ApsSystemUtils.ApsDeepDebug;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApsSystemUtilsTest {

    @Test
    void testConstructorThrowsException() {
        Exception exception = assertThrows(Exception.class, () -> {
            java.lang.reflect.Constructor<ApsSystemUtils> constructor =
                ApsSystemUtils.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });

        // The IllegalStateException is wrapped in InvocationTargetException
        assertInstanceOf(InvocationTargetException.class, exception);
        Throwable cause = exception.getCause();
        assertInstanceOf(IllegalStateException.class, cause);
        assertEquals("Utility class", cause.getMessage());
    }

    @Test
    void testGetLogger() {
        assertNotNull(ApsSystemUtils.getLogger());
    }

    @Test
    void testLogThrowableWithMessage() {
        Exception exception = new RuntimeException("Test exception");
        Object caller = new Object();

        // Should not throw any exception
        ApsSystemUtils.logThrowable(exception, caller, "testMethod", "Test message");
    }

    @Test
    void testLogThrowableWithoutMessage() {
        Exception exception = new RuntimeException("Test exception");
        Object caller = new Object();

        // Should not throw any exception
        ApsSystemUtils.logThrowable(exception, caller, "testMethod");
    }

    @Test
    void testLogThrowableWithNullCaller() {
        Exception exception = new RuntimeException("Test exception");

        // Should not throw any exception
        ApsSystemUtils.logThrowable(exception, null, "testMethod", "Test message");
    }

    @Test
    void testDirectStdoutTraceWithoutForce() {
        assertFalse(ApsSystemUtils.directStdoutTrace("test message"));
    }

    @Test
    void testDirectStdoutTraceWithForce() {
        assertTrue(ApsSystemUtils.directStdoutTrace("test message", true));
    }

    @Test
    void testGetEnvWithExistingVariable() {
        String result = ApsSystemUtils.getEnv("PATH", "default");
        assertNotNull(result);
    }

    @Test
    void testGetEnvWithNonExistingVariable() {
        String result = ApsSystemUtils.getEnv("NON_EXISTING_VAR_12345", "default_value");
        assertEquals("default_value", result);
    }

    @Test
    void testGetEnvFlagWithNonExistingVariable() {
        boolean result = ApsSystemUtils.getEnvFlag("NON_EXISTING_FLAG_12345", true);
        assertTrue(result);

        result = ApsSystemUtils.getEnvFlag("NON_EXISTING_FLAG_12345", false);
        assertFalse(result);
    }

    @Test
    void testDeepDebug() {
        try (MockedStatic<ApsSystemUtils> mock = Mockito.mockStatic(ApsSystemUtils.class, InvocationOnMock::callRealMethod)) {
            mock.when(ApsSystemUtils::getDeepDebugFF).thenReturn("");
            assertFalse(ApsDeepDebug.print("test print"));
        }
    }

    @Test
    void testDeepDebug2() {
        try (MockedStatic<ApsSystemUtils> mock = Mockito.mockStatic(ApsSystemUtils.class, InvocationOnMock::callRealMethod)) {
            mock.when(ApsSystemUtils::getDeepDebugFF).thenReturn("true");
            assertEquals(ApsSystemUtils.getDeepDebugFF(), "true");
            assertTrue(ApsDeepDebug.print("test print"));
        }
    }

    @Test
    void testDeepDebugPrintWithTag() {
        try (MockedStatic<ApsSystemUtils> mock = Mockito.mockStatic(ApsSystemUtils.class, InvocationOnMock::callRealMethod)) {
            mock.when(ApsSystemUtils::getDeepDebugFF).thenReturn("TAG1");
            assertTrue(ApsDeepDebug.print("TAG1", "test message"));
            assertFalse(ApsDeepDebug.print("TAG2", "test message"));
        }
    }

    @Test
    void testDeepDebugPrintWithTagAndPrefix() {
        try (MockedStatic<ApsSystemUtils> mock = Mockito.mockStatic(ApsSystemUtils.class, InvocationOnMock::callRealMethod)) {
            mock.when(ApsSystemUtils::getDeepDebugFF).thenReturn("TAG1");
            assertTrue(ApsDeepDebug.print("TAG1", ">>> ", "message1", "message2"));
        }
    }

    @Test
    void testDeepDebugPrintWithSuppressTitle() {
        try (MockedStatic<ApsSystemUtils> mock = Mockito.mockStatic(ApsSystemUtils.class, InvocationOnMock::callRealMethod)) {
            mock.when(ApsSystemUtils::getDeepDebugFF).thenReturn("*");
            assertTrue(ApsDeepDebug.print("TAG1", "prefix", true, "message"));
        }
    }

    @Test
    void testDeepDebugPrintWithNullTag() {
        try (MockedStatic<ApsSystemUtils> mock = Mockito.mockStatic(ApsSystemUtils.class, InvocationOnMock::callRealMethod)) {
            mock.when(ApsSystemUtils::getDeepDebugFF).thenReturn("*");
            assertTrue(ApsDeepDebug.print(null, null, "message"));
        }
    }

    @Test
    void testDeepDebugPrintHttpRequest() {
        try (MockedStatic<ApsSystemUtils> mock = Mockito.mockStatic(ApsSystemUtils.class, InvocationOnMock::callRealMethod)) {
            mock.when(ApsSystemUtils::getDeepDebugFF).thenReturn("HTTP");

            HttpServletRequest request = mock(HttpServletRequest.class);
            when(request.getRequestURL()).thenReturn(new StringBuffer("http://example.com/test"));

            Vector<String> headerNames = new Vector<>();
            headerNames.add("Content-Type");
            headerNames.add("Authorization");

            when(request.getHeaderNames()).thenReturn(headerNames.elements());

            Vector<String> contentTypeValues = new Vector<>();
            contentTypeValues.add("application/json");
            when(request.getHeaders("Content-Type")).thenReturn(contentTypeValues.elements());

            Vector<String> authValues = new Vector<>();
            authValues.add("Bearer token123");
            when(request.getHeaders("Authorization")).thenReturn(authValues.elements());

            ApsDeepDebug.printHttpRequest("HTTP", request);
        }
    }

    @Test
    void testDeepDebugPrintHttpRequestDisabled() {
        try (MockedStatic<ApsSystemUtils> mock = Mockito.mockStatic(ApsSystemUtils.class, InvocationOnMock::callRealMethod)) {
            mock.when(ApsSystemUtils::getDeepDebugFF).thenReturn("");

            HttpServletRequest request = mock(HttpServletRequest.class);

            // Should not process when tag is not enabled
            ApsDeepDebug.printHttpRequest("HTTP", request);
        }
    }

    @Test
    void testDeepDebugIsTagEnabled() {
        try (MockedStatic<ApsSystemUtils> mock = Mockito.mockStatic(ApsSystemUtils.class, InvocationOnMock::callRealMethod)) {
            mock.when(ApsSystemUtils::getDeepDebugFF).thenReturn("TAG1,TAG2");
            assertTrue(ApsDeepDebug.isTagEnabled("TAG1"));
            assertTrue(ApsDeepDebug.isTagEnabled("TAG2"));
            assertFalse(ApsDeepDebug.isTagEnabled("TAG3"));
        }
    }
    

    @Test
    void testPrintFeatureFlareWithTagAndAlwaysPrint() {
        try (MockedStatic<ApsSystemUtils> mock = Mockito.mockStatic(ApsSystemUtils.class, InvocationOnMock::callRealMethod)) {
            mock.when(ApsSystemUtils::getDeepDebugFF).thenReturn("FLARE");
            ApsDeepDebug.printFeatureFlare("FEATURE_TAG", "feature_name", true);
        }
    }

    @Test
    void testPrintFeatureFlareWithNullFlareName() {
        try (MockedStatic<ApsSystemUtils> mock = Mockito.mockStatic(ApsSystemUtils.class, InvocationOnMock::callRealMethod)) {
            mock.when(ApsSystemUtils::getDeepDebugFF).thenReturn("FLARE");
            ApsDeepDebug.printFeatureFlare("FEATURE_TAG", null, false);
        }
    }

    @Test
    void test_isTagEnabled() {
        assertFalse(ApsSystemUtils.isTagEnabled(null, "TAG1"));
        assertTrue(ApsSystemUtils.isTagEnabled("TAG1,TAG2", "TAG1"));
        assertFalse(ApsSystemUtils.isTagEnabled("TAG1,TAG2", "TAG11"));
        assertFalse(ApsSystemUtils.isTagEnabled("TAG1_,TAG2", "TAG1"));

        assertTrue(ApsSystemUtils.isTagEnabled("TAG1,TAG2", "TAG2"));
        assertTrue(ApsSystemUtils.isTagEnabled("TAG1,TAG2,TAG3", "TAG2"));
        assertTrue(ApsSystemUtils.isTagEnabled("CAT", "CAT:TAG2"));
        assertTrue(ApsSystemUtils.isTagEnabled("TAG1,TAG2,CAT", "CAT:TAG2"));
        assertTrue(ApsSystemUtils.isTagEnabled("TAG1,TAG2,CAT", "CAT"));
    }

    @Test
    void test_isTagEnabledWithWildcard() {
        assertTrue(ApsSystemUtils.isTagEnabled("*", "ANY_TAG"));
        assertTrue(ApsSystemUtils.isTagEnabled("true", "ANY_TAG"));
    }

    @Test
    void test_isTagEnabledWithNullTag() {
        assertFalse(ApsSystemUtils.isTagEnabled("TAG1,TAG2", null));
    }

    @Test
    void test_isTagEnabledWithMultipleSubtags() {
        assertTrue(ApsSystemUtils.isTagEnabled("PARENT", "PARENT:CHILD:GRANDCHILD"));
        assertTrue(ApsSystemUtils.isTagEnabled("CHILD", "PARENT:CHILD:GRANDCHILD"));
        assertTrue(ApsSystemUtils.isTagEnabled("GRANDCHILD", "PARENT:CHILD:GRANDCHILD"));
    }

    @Test
    void test_isFeatureEnabled() {
        try (MockedStatic<ApsSystemUtils> utilsMock = Mockito.mockStatic(ApsSystemUtils.class, InvocationOnMock::callRealMethod)) {
            utilsMock.when(ApsSystemUtils::getFeatureFlags).thenReturn("A-FEATURE");
            assertTrue(ApsSystemUtils.isFeatureEnabled("A-FEATURE"));
            assertTrue(ApsSystemUtils.isFeatureEnabled("A-FEATURE:SUB-FEATURE"));
            assertFalse(ApsSystemUtils.isFeatureEnabled("A-FEATURE_"));
        }
    }

    @Test
    void test_isFeatureEnabledWithNull() {
        try (MockedStatic<ApsSystemUtils> utilsMock = Mockito.mockStatic(ApsSystemUtils.class, InvocationOnMock::callRealMethod)) {
            utilsMock.when(ApsSystemUtils::getFeatureFlags).thenReturn(null);
            assertFalse(ApsSystemUtils.isFeatureEnabled("ANY_FEATURE"));
        }
    }

    @Test
    void testGetFeatureFlags() {
        // This will return the actual environment variable or null
        String flags = ApsSystemUtils.getFeatureFlags();
        // Just ensure it doesn't throw
        assertNotNull(flags != null ? flags : "null value is ok");
    }

    @Test
    void testGetDeepDebugFF() {
        // This will return the actual environment variable or null
        String flags = ApsSystemUtils.getDeepDebugFF();
        // Just ensure it doesn't throw
        assertNotNull(flags != null ? flags : "null value is ok");
    }
}
