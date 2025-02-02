/*
 * Copyright 2022-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.aps.util;

import static org.entando.entando.aps.util.UrlUtils.HTTPS_SCHEME;
import static org.entando.entando.aps.util.UrlUtils.HTTP_SCHEME;
import static org.entando.entando.aps.util.UrlUtils.fetchServerNameFromUri;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.google.common.net.HttpHeaders;
import java.net.URI;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.entando.entando.aps.util.UrlUtils.EntUrlBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith({MockitoExtension.class, SystemStubsExtension.class})
class UrlUtilsTest {

    @Mock private HttpServletRequest requestMock;
    
    @SystemStub
    private EnvironmentVariables environmentVariables;

    @BeforeEach
    private void init() throws Exception {
        Mockito.reset(requestMock);
    }

    @AfterEach
    public void afterAll() throws Exception {
        Mockito.reset(requestMock);
    }
    
    @Test
    void shouldFetchSchemeWorksFineWithDifferentInputs() throws Exception {
        // case0
        Mockito.reset(requestMock);
        when(requestMock.getHeader(HttpHeaders.X_FORWARDED_PROTO)).thenReturn(HTTPS_SCHEME);
        when(requestMock.getScheme()).thenReturn(HTTP_SCHEME);
        Assertions.assertEquals(HTTPS_SCHEME, UrlUtils.fetchScheme(requestMock));

        // case1
        Mockito.reset(requestMock);
        when(requestMock.getHeader(HttpHeaders.X_FORWARDED_PROTO)).thenReturn(HTTP_SCHEME);
        when(requestMock.getScheme()).thenReturn(HTTP_SCHEME);
        Assertions.assertEquals(HTTP_SCHEME, UrlUtils.fetchScheme(requestMock));
        
        // case3
        environmentVariables.set("ENTANDO_APP_USE_TLS", "true");
        when(requestMock.getHeader(HttpHeaders.X_FORWARDED_PROTO)).thenReturn(HTTP_SCHEME);
        when(requestMock.getScheme()).thenReturn(HTTP_SCHEME);
        Assertions.assertEquals(HTTPS_SCHEME, UrlUtils.fetchScheme(requestMock));

    }

    @Test
    void shouldFetchHostWorksFineWithDifferentInputs() throws Exception {
        // case1
        when(requestMock.getHeader(HttpHeaders.X_FORWARDED_HOST)).thenReturn("www.test1.com");
        when(requestMock.getHeader(HttpHeaders.HOST)).thenReturn("www.test2.com");
        when(requestMock.getServerName()).thenReturn("www.test3.com");
        Assertions.assertEquals("www.test1.com", UrlUtils.fetchServer(requestMock));

        // case2-a
        Mockito.reset(requestMock);
        when(requestMock.getHeader(HttpHeaders.X_FORWARDED_HOST)).thenReturn(null);
        when(requestMock.getHeader(HttpHeaders.HOST)).thenReturn("www.test2.com:443");
        when(requestMock.getServerName()).thenReturn("www.test2.com");
        Assertions.assertEquals("www.test2.com", UrlUtils.fetchServer(requestMock));

        // case2-b
        Mockito.reset(requestMock);
        when(requestMock.getHeader(HttpHeaders.X_FORWARDED_HOST)).thenReturn(null);
        when(requestMock.getHeader(HttpHeaders.HOST)).thenReturn("www.test2.com:443");
        when(requestMock.getServerName()).thenReturn("www.test3.com");
        Assertions.assertEquals("www.test3.com", UrlUtils.fetchServer(requestMock));

        // case3
        Mockito.reset(requestMock);
        when(requestMock.getHeader(HttpHeaders.X_FORWARDED_HOST)).thenReturn(null);
        when(requestMock.getHeader(HttpHeaders.HOST)).thenReturn(null);
        when(requestMock.getServerName()).thenReturn("www.test3.com");
        Assertions.assertEquals("www.test3.com", UrlUtils.fetchServer(requestMock));

    }

    @Test
    void shouldFetchPortWorksFineWithDifferentInputs() throws Exception {
        // case0
        when(requestMock.getHeader(HttpHeaders.X_FORWARDED_PORT)).thenReturn("443");
        when(requestMock.getHeader(HttpHeaders.HOST)).thenReturn("test.com:4443");
        when(requestMock.getServerPort()).thenReturn(8443);
        Optional<Integer> port = port = UrlUtils.fetchPort(requestMock);
        Assertions.assertTrue(port.isPresent());
        Assertions.assertEquals(443, port.get());

        // case1-a
        Mockito.reset(requestMock);
        when(requestMock.getHeader(HttpHeaders.X_FORWARDED_PORT)).thenReturn(null);
        when(requestMock.getHeader(HttpHeaders.HOST)).thenReturn("test.com:4443");
        when(requestMock.getServerName()).thenReturn("test.com");
        when(requestMock.getServerPort()).thenReturn(8443);
        port = UrlUtils.fetchPort(requestMock);
        Assertions.assertTrue(port.isPresent());
        Assertions.assertEquals(4443, port.get());

        // case1-b
        Mockito.reset(requestMock);
        when(requestMock.getHeader(HttpHeaders.X_FORWARDED_PORT)).thenReturn(null);
        when(requestMock.getHeader(HttpHeaders.HOST)).thenReturn("test.com:4443");
        when(requestMock.getServerName()).thenReturn("test2.com");
        when(requestMock.getServerPort()).thenReturn(8443);
        port = UrlUtils.fetchPort(requestMock);
        Assertions.assertTrue(port.isPresent());
        Assertions.assertEquals(8443, port.get());

        // case2
        Mockito.reset(requestMock);
        when(requestMock.getHeader(HttpHeaders.X_FORWARDED_PORT)).thenReturn(null);
        when(requestMock.getHeader(HttpHeaders.HOST)).thenReturn(null);
        when(requestMock.getServerPort()).thenReturn(8443);
        port = UrlUtils.fetchPort(requestMock);
        Assertions.assertTrue(port.isPresent());
        Assertions.assertEquals(8443, port.get());

        // case3
        Mockito.reset(requestMock);
        when(requestMock.getHeader(HttpHeaders.X_FORWARDED_PORT)).thenReturn(null);
        when(requestMock.getHeader(HttpHeaders.HOST)).thenReturn(null);
        when(requestMock.getServerPort()).thenReturn(0);
        port = UrlUtils.fetchPort(requestMock);
        Assertions.assertTrue(port.isEmpty());

        // case 4
        environmentVariables.set("ENTANDO_APP_ENGINE_EXTERNAL_PORT", "8888");
        when(requestMock.getHeader(HttpHeaders.X_FORWARDED_PORT)).thenReturn("443");
        when(requestMock.getHeader(HttpHeaders.HOST)).thenReturn("test.com:4443");
        when(requestMock.getServerPort()).thenReturn(8443);
        port = UrlUtils.fetchPort(requestMock);
        Assertions.assertTrue(port.isPresent());
        Assertions.assertEquals(8888, port.get());
    }

    @Test
    void shouldFetchServerNameFromUriWorkFine(){
        Assertions.assertTrue(fetchServerNameFromUri(null).isEmpty());
        Assertions.assertTrue(fetchServerNameFromUri(null).isEmpty());
        Assertions.assertTrue(fetchServerNameFromUri(":/test").isEmpty());
        Assertions.assertTrue(fetchServerNameFromUri("/test").isEmpty());
        Optional<String> host = fetchServerNameFromUri("http://www.test.com:8080/test");
        Assertions.assertTrue(host.isPresent());
        Assertions.assertEquals("www.test.com",host.get());

    }

    @Test
    void fetchPathFromUri(){
        Assertions.assertTrue(UrlUtils.fetchPathFromUri(null).isEmpty());
        Assertions.assertTrue(UrlUtils.fetchPathFromUri(null).isEmpty());
        Assertions.assertTrue(UrlUtils.fetchPathFromUri("http://www.test.com:8080").isEmpty());

        Optional<String> path = UrlUtils.fetchPathFromUri("http://www.test.com:8080/");
        Assertions.assertTrue(path.isPresent());
        Assertions.assertEquals("/", path.get());

        path = UrlUtils.fetchPathFromUri("/test");
        Assertions.assertTrue(path.isPresent());
        Assertions.assertEquals("/test", path.get());

        path = UrlUtils.fetchPathFromUri("http://www.test.com:8080/test");
        Assertions.assertTrue(path.isPresent());
        Assertions.assertEquals("/test",path.get());
    }

    @Test
    void removeContextRootFromPath() {
        Assertions.assertTrue(UrlUtils.removeContextRootFromPath(null, requestMock).isEmpty());
        Assertions.assertTrue(UrlUtils.removeContextRootFromPath("", requestMock).isEmpty());

        Mockito.reset(requestMock);
        when(requestMock.getContextPath()).thenReturn("/context-path");
        Assertions.assertTrue(UrlUtils.removeContextRootFromPath("/context-path", requestMock).isEmpty());

        when(requestMock.getContextPath()).thenReturn("/context-path");
        Optional<String> path = UrlUtils.removeContextRootFromPath("/context-path/test", requestMock);
        Assertions.assertTrue(path.isPresent());
        Assertions.assertEquals("/test", path.get());

        Mockito.reset(requestMock);
        when(requestMock.getContextPath()).thenReturn("/context-path");
        path = UrlUtils.removeContextRootFromPath("/test", requestMock);
        Assertions.assertTrue(path.isPresent());
        Assertions.assertEquals("/test", path.get());

        when(requestMock.getContextPath()).thenReturn("/context-path");
        path = UrlUtils.removeContextRootFromPath("/context-path/", requestMock);
        Assertions.assertTrue(path.isPresent());
        Assertions.assertEquals("/", path.get());

    }

    @Test
    void shouldComposeBaseUrlWorkFine(){
        lenient().when(requestMock.getHeader(HttpHeaders.X_FORWARDED_HOST)).thenReturn("www.test1.com");
        lenient().when(requestMock.getHeader(HttpHeaders.X_FORWARDED_PROTO)).thenReturn(HTTPS_SCHEME);
        lenient().when(requestMock.getHeader(HttpHeaders.X_FORWARDED_PORT)).thenReturn("443");
        lenient().when(requestMock.getHeader(HttpHeaders.HOST)).thenReturn("localhost:4443");
        lenient().when(requestMock.getScheme()).thenReturn(HTTP_SCHEME);
        lenient().when(requestMock.getServerName()).thenReturn("localhost");
        lenient().when(requestMock.getServerPort()).thenReturn(4443);
        lenient().when(requestMock.getContextPath()).thenReturn("/context-path");
        lenient().when(requestMock.getServletPath()).thenReturn("/my-path");
        Assertions.assertEquals("https://www.test1.com",UrlUtils.composeBaseUrl(requestMock).toString());

        lenient().when(requestMock.getHeader(HttpHeaders.X_FORWARDED_HOST)).thenReturn("www.test1.com");
        lenient().when(requestMock.getHeader(HttpHeaders.X_FORWARDED_PROTO)).thenReturn(HTTPS_SCHEME);
        lenient().when(requestMock.getHeader(HttpHeaders.X_FORWARDED_PORT)).thenReturn("883");
        lenient().when(requestMock.getHeader(HttpHeaders.HOST)).thenReturn("localhost:4443");
        lenient().when(requestMock.getScheme()).thenReturn(HTTP_SCHEME);
        lenient().when(requestMock.getServerName()).thenReturn("localhost");
        lenient().when(requestMock.getServerPort()).thenReturn(4443);
        lenient().when(requestMock.getContextPath()).thenReturn("/context-path");
        lenient().when(requestMock.getServletPath()).thenReturn("/my-path");
        Assertions.assertEquals("https://www.test1.com:883",UrlUtils.composeBaseUrl(requestMock).toString());


        Mockito.reset(requestMock);
        lenient().when(requestMock.getHeader(HttpHeaders.HOST)).thenReturn("www.test2.com:4443");
        lenient().when(requestMock.getScheme()).thenReturn(HTTPS_SCHEME);
        lenient().when(requestMock.getServerName()).thenReturn("www.test2.com");
        lenient().when(requestMock.getServerPort()).thenReturn(4443);
        lenient().when(requestMock.getContextPath()).thenReturn("/context-path");
        lenient().when(requestMock.getServletPath()).thenReturn("/my-path");
        Assertions.assertEquals("https://www.test2.com:4443",UrlUtils.composeBaseUrl(requestMock).toString());


        Mockito.reset(requestMock);
        lenient().when(requestMock.getScheme()).thenReturn(HTTPS_SCHEME);
        lenient().when(requestMock.getServerName()).thenReturn("www.test3.com");
        lenient().when(requestMock.getServerPort()).thenReturn(443);
        lenient().when(requestMock.getContextPath()).thenReturn("/context-path");
        lenient().when(requestMock.getServletPath()).thenReturn("/my-path");
        Assertions.assertEquals("https://www.test3.com",UrlUtils.composeBaseUrl(requestMock).toString());


        Mockito.reset(requestMock);
        lenient().when(requestMock.getHeader(HttpHeaders.X_FORWARDED_HOST)).thenReturn("www.test1.com");
        lenient().when(requestMock.getHeader(HttpHeaders.X_FORWARDED_PROTO)).thenReturn(HTTPS_SCHEME);
        lenient().when(requestMock.getHeader(HttpHeaders.HOST)).thenReturn("localhost:80");
        lenient().when(requestMock.getScheme()).thenReturn(HTTP_SCHEME);
        lenient().when(requestMock.getServerName()).thenReturn("localhost");
        lenient().when(requestMock.getServerPort()).thenReturn(80);
        lenient().when(requestMock.getContextPath()).thenReturn("/context-path");
        lenient().when(requestMock.getServletPath()).thenReturn("/my-path");
        Assertions.assertEquals("https://www.test1.com",UrlUtils.composeBaseUrl(requestMock).toString());

        // FIXME problem!!!
/*
        Mockito.reset(requestMock);
        lenient().when(requestMock.getHeader(HttpHeaders.X_FORWARDED_HOST)).thenReturn("www.test1.com");
        lenient().when(requestMock.getHeader(HttpHeaders.X_FORWARDED_PROTO)).thenReturn(HTTPS_SCHEME);
        lenient().when(requestMock.getHeader(HttpHeaders.HOST)).thenReturn("localhost:8080");
        lenient().when(requestMock.getScheme()).thenReturn(HTTP_SCHEME);
        lenient().when(requestMock.getServerName()).thenReturn("localhost");
        lenient().when(requestMock.getServerPort()).thenReturn(8080);
        lenient().when(requestMock.getContextPath()).thenReturn("/context-path");
        lenient().when(requestMock.getServletPath()).thenReturn("/my-path");
        Assertions.assertEquals("https://www.test1.com",UrlUtils.composeBaseUrl(requestMock).toString());
*/

    }


    @Test
    void shouldComposeBaseUrlThrowException() {
        Mockito.reset(requestMock);
        lenient().when(requestMock.getHeader(HttpHeaders.X_FORWARDED_HOST)).thenReturn("www.test1.com");
        lenient().when(requestMock.getHeader(HttpHeaders.X_FORWARDED_PROTO)).thenReturn(HTTP_SCHEME);
        lenient().when(requestMock.getHeader(HttpHeaders.X_FORWARDED_PORT)).thenReturn("80");
        lenient().when(requestMock.getHeader(HttpHeaders.HOST)).thenReturn("localhost:8080");
        lenient().when(requestMock.getScheme()).thenReturn("://");
        lenient().when(requestMock.getServerName()).thenReturn("localhost");
        lenient().when(requestMock.getServerPort()).thenReturn(8080);
        lenient().when(requestMock.getContextPath()).thenReturn("/context-path");
        lenient().when(requestMock.getServletPath()).thenReturn("/my-path");
        Assertions.assertThrows(RuntimeException.class, () -> UrlUtils.composeBaseUrl(requestMock));
    }

    @Test
    void shouldEntUrlBuilderWorksFineWithDifferentInputs() {
        URI url = EntUrlBuilder.builder().url(URI.create("http://server.com/")).path("/context-root").path("path/").build();
        Assertions.assertEquals("http://server.com/context-root/path/", url.toString());

        url = EntUrlBuilder.builder().url("http://server.com/")
                .path("/context-root")
                .path("").path("path/").path(null).path("   ").path(" /path1/ ")
                .paths("path2","path3").build();
        Assertions.assertEquals("http://server.com/context-root/path/path1/path2/path3", url.toString());

    }


    @Test
    void shouldFetchSTenantCodeFromCustomHeader() throws Exception {
        // case1
        when(requestMock.getHeader("X-ENTANDO-TENANTCODE")).thenReturn("tenant2");
        Assertions.assertEquals("tenant2", UrlUtils.fetchTenantCode(requestMock));

        // case2
        Mockito.reset(requestMock);
        when(requestMock.getHeader("X-ENTANDO-TENANTCODE")).thenReturn("primary");
        Assertions.assertEquals("primary", UrlUtils.fetchTenantCode(requestMock));

        // case3
        Mockito.reset(requestMock);
        Assertions.assertNull(UrlUtils.fetchTenantCode(requestMock));

    }
}
