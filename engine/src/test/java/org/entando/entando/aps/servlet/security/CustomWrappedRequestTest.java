package org.entando.entando.aps.servlet.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomWrappedRequestTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Test
    void testGetContextPath_withVirtualContext() {
        String virtualContextPath = "/virtual";
        CustomWrappedRequest wrappedRequest = new CustomWrappedRequest(mockRequest, virtualContextPath,
                Collections.emptyMap());
        assertEquals(virtualContextPath, wrappedRequest.getContextPath());
    }

    @Test
    void testGetServletPath_withVirtualContext() {
        String virtualContextPath = "/virtual";
        String originalServletPath = "/virtual/servlet/path";
        when(mockRequest.getServletPath()).thenReturn(originalServletPath);

        CustomWrappedRequest wrappedRequest = new CustomWrappedRequest(mockRequest, virtualContextPath,
                Collections.emptyMap());
        assertEquals("/servlet/path", wrappedRequest.getServletPath());
    }

    @Test
    void testGetServletPath_withEmptyVirtualContext() {
        String virtualContextPath = "";
        String originalServletPath = "/servlet/path";
        when(mockRequest.getServletPath()).thenReturn(originalServletPath);

        CustomWrappedRequest wrappedRequest = new CustomWrappedRequest(mockRequest, virtualContextPath,
                Collections.emptyMap());
        assertEquals("/servlet/path", wrappedRequest.getServletPath());
    }

    @Test
    void testHasVirtualContext_true() {
        CustomWrappedRequest wrappedRequest = new CustomWrappedRequest(mockRequest, "/virtual", Collections.emptyMap());
        assertTrue(wrappedRequest.hasVirtualContext());
    }

    @Test
    void testGetOriginalContextPath() {
        String originalContextPath = "/original";
        when(mockRequest.getContextPath()).thenReturn(originalContextPath);
        CustomWrappedRequest wrappedRequest = new CustomWrappedRequest(mockRequest, "/virtual", Collections.emptyMap());
        assertEquals(originalContextPath, wrappedRequest.getOriginalContextPath());
        verify(mockRequest).getContextPath(); // Verify the underlying request was called
    }

    @Test
    void testGetOriginalServletPath() {
        String originalServletPath = "/original/servlet/path";
        when(mockRequest.getServletPath()).thenReturn(originalServletPath);
        CustomWrappedRequest wrappedRequest = new CustomWrappedRequest(mockRequest, "/virtual", Collections.emptyMap());
        assertEquals(originalServletPath, wrappedRequest.getOriginalServletPath());
        verify(mockRequest).getServletPath();
    }

    @Test
    void testStripVirtualContextIfRequired_withVirtualContext_noLeadingSlashInVirtualContext() {
        String virtualContextPath = "virtual"; // No leading slash
        String path = "/virtual/some/path";
        when(mockRequest.getServletPath()).thenReturn(path);

        CustomWrappedRequest wrappedRequest = new CustomWrappedRequest(mockRequest, virtualContextPath,
                Collections.emptyMap());
        assertEquals("/some/path", wrappedRequest.getServletPath());
    }

    @Test
    void testStripVirtualContextIfRequired_withVirtualContext_leadingSlashInVirtualContext() {
        String virtualContextPath = "/virtual";
        String path = "/virtual/some/path";
        when(mockRequest.getServletPath()).thenReturn(path);

        CustomWrappedRequest wrappedRequest = new CustomWrappedRequest(mockRequest, virtualContextPath,
                Collections.emptyMap());
        assertEquals("/some/path", wrappedRequest.getServletPath());
    }

    @Test
    void testStripVirtualContextIfRequired_emptyVirtualContext() {
        String virtualContextPath = ""; // Empty string
        String path = "/some/path";
        when(mockRequest.getServletPath()).thenReturn(path);
        CustomWrappedRequest wrappedRequest = new CustomWrappedRequest(mockRequest, virtualContextPath,
                Collections.emptyMap());
        assertEquals(path, wrappedRequest.getServletPath());
    }

    @Test
    void testGetCustomizedRequest_isCustomWrappedRequest() {
        CustomWrappedRequest wrappedRequest = new CustomWrappedRequest(mockRequest, "/virtual", Collections.emptyMap());
        ServletRequest result = CustomWrappedRequest.getCustomizedRequest(wrappedRequest);
        assertSame(wrappedRequest, result); // Use assertSame for object identity
    }

    @Test
    void testGetCustomizedRequest_isHttpServletRequestWrapper() {
        HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(mockRequest);
        ServletRequest result = CustomWrappedRequest.getCustomizedRequest(wrapper);
        assertNull(result);
    }

    @Test
    void testGetCustomizedRequest_isNestedCustomWrappedRequest() {
        CustomWrappedRequest innerWrappedRequest = new CustomWrappedRequest(mockRequest, "/virtual",
                Collections.emptyMap());
        HttpServletRequestWrapper outerWrapper = new HttpServletRequestWrapper(innerWrappedRequest);
        ServletRequest result = CustomWrappedRequest.getCustomizedRequest(outerWrapper);
        assertSame(innerWrappedRequest, result);
    }

    @Test
    void testGetCustomizedRequest_isNestedHttpServletRequestWrapper() {
        HttpServletRequestWrapper innerWrapper = new HttpServletRequestWrapper(mockRequest);
        HttpServletRequestWrapper outerWrapper = new HttpServletRequestWrapper(innerWrapper);
        ServletRequest result = CustomWrappedRequest.getCustomizedRequest(outerWrapper);
        assertNull(result);
    }

    @Test
    void testGetCustomizedRequest_isBaseHttpServletRequest() {
        ServletRequest result = CustomWrappedRequest.getCustomizedRequest(mockRequest);
        assertNull(result);
    }

    @Test
    void testGetCustomizedRequest_nullInput() {
        ServletRequest result = CustomWrappedRequest.getCustomizedRequest(null);
        assertNull(result);
    }

    @Test
    void testConstructor_withLeadingSlash() {
        CustomWrappedRequest wrappedRequest = new CustomWrappedRequest(mockRequest, "virtual", Collections.emptyMap());
        assertEquals("/virtual", wrappedRequest.getContextPath());
    }

    @Test
    void testConstructor_withoutLeadingSlash() {
        CustomWrappedRequest wrappedRequest = new CustomWrappedRequest(mockRequest, "/virtual", Collections.emptyMap());
        assertEquals("/virtual", wrappedRequest.getContextPath());
    }

}