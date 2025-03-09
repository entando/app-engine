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

import com.google.common.net.HttpHeaders;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.owasp.encoder.Encode;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
public final class UrlUtils {

    public static final String ENTANDO_TENANT_CODE_CUSTOM_HEADER = "X-ENTANDO-TENANTCODE";
    public static final String ENTANDO_APP_USE_TLS = "ENTANDO_APP_USE_TLS";
    public static final String ENTANDO_APP_ENGINE_EXTERNAL_PORT = "ENTANDO_APP_ENGINE_EXTERNAL_PORT";
    public static final String HTTP_SCHEME = "http";
    public static final String HTTPS_SCHEME = "https";

    private UrlUtils(){}

    public static String determineFullFrontendURL(HttpServletRequest req) {
        String base = determineFrontendURL(req);
        String query = req.getQueryString();
        return base + ((query != null) ? "?" + query : "");
    }

    /**
     * Returns the actual URL fetched by the browser, without the query part.
     * <pre>
     * The term "FrontendURL" refers to the URL fetched by the browser,
     * which may or may not be aligned with the data (e.g. HOST header)
     * received the server when reverse proxies are in the way.
     * This function in fact assists in the proper generation of links
     * and redirect locations.
     * </pre>
     */

    public static String determineFrontendURL(HttpServletRequest req) {
        URL url = determineFrontendUrlObject(req);
        String path = req.getRequestURI();
        return url.getProtocol() + "://" +
                url.getHost()
                + ((url.getPort() != -1) ? ":" + url.getPort() : "")
                + ((path != null) ? path : "");
    }

    public static String determineFrontendServerName(HttpServletRequest request) {
        return determineFrontendUrlObject(request).getHost();
    }

    /**
     * Returns a URL object that stores information about the frontendURL
     * (see determineFrontendURL)
     */
    public static URL determineFrontendUrlObject(HttpServletRequest request) {
        String host, scheme = request.getScheme();
        Optional<Integer> port;

        host = getHostFromXHeader(request).orElse(request.getServerName());
        scheme = getProtoFromXHeader(request).orElse(scheme);
        port = Optional.of(getPortFromXHeader(request).orElse(request.getServerPort()));

        if (BooleanUtils.toBoolean(System.getenv(ENTANDO_APP_USE_TLS))) {
            scheme = HTTPS_SCHEME;
        }

        return generateUrl(scheme, host, port, request);
    }

    public static String fetchScheme(HttpServletRequest request){
        return getProtoFromEnv().filter(UrlUtils::isHttps)
                .orElseGet(() -> getProtoFromXHeader(request).filter(UrlUtils::isHttps).orElse(request.getScheme()));
    }

    private static Optional<String> getProtoFromEnv(){
        if( BooleanUtils.toBoolean(System.getenv(ENTANDO_APP_USE_TLS)) ) {
            return Optional.ofNullable(HTTPS_SCHEME);
        } else {
            return Optional.ofNullable(HTTP_SCHEME);
        }
    }

    private static Optional<String> getProtoFromXHeader(HttpServletRequest request){
        return Optional.ofNullable(request.getHeader(HttpHeaders.X_FORWARDED_PROTO)).filter(StringUtils::isNotBlank);
    }

    private static boolean isHttps(String proto){
        return HTTPS_SCHEME.equals(proto);
    }

    public static String fetchServer(HttpServletRequest request){
        return getHostFromXHeader(request)
                .orElseGet(() -> getHostFromHeader(request).orElse(request.getServerName()));
    }

    public static String fetchTenantCode(HttpServletRequest request){
        return request.getHeader(ENTANDO_TENANT_CODE_CUSTOM_HEADER);
    }

    private static Optional<String> getHostFromXHeader(HttpServletRequest request){
        return Optional.ofNullable(request.getHeader(HttpHeaders.X_FORWARDED_HOST)).filter(StringUtils::isNotBlank);
    }

    private static Optional<String> getHostFromHeader(HttpServletRequest request){
        return Optional.ofNullable(request.getHeader(HttpHeaders.HOST))
                .filter(StringUtils::isNotBlank)
                .filter(s -> s.startsWith(request.getServerName()))
                .map(s -> s.split(":")[0]);
    }

    public static Optional<Integer> fetchPort(HttpServletRequest request) {
        Integer port = getPortFromEnv().orElseGet(() -> getPortFromXHeader(request)
                .orElseGet(() -> getPortFromHeader(request)
                        .orElseGet(() -> getPortServlet(request))));
        return Optional.ofNullable(port);
    }

    private static Optional<Integer> getPortFromEnv(){
        String port = System.getenv(ENTANDO_APP_ENGINE_EXTERNAL_PORT);
        if( NumberUtils.isParsable(port)) {
            return Optional.of(NumberUtils.toInt(port));
        } else {
            return Optional.empty();
        }
    }

    private static Optional<Integer> getPortFromXHeader(HttpServletRequest request){
        return Optional.ofNullable(request.getHeader(HttpHeaders.X_FORWARDED_PORT)).filter(StringUtils::isNotBlank).map(Integer::valueOf);
    }

    private static Optional<Integer> getPortFromHeader(HttpServletRequest request){
        return Optional.ofNullable(request.getHeader(HttpHeaders.HOST))
                .filter(StringUtils::isNotBlank)
                .filter(s -> s.startsWith(request.getServerName()))
                .filter(UrlUtils::hostHeaderContainsPort)
                .map(host -> host.split(":")[1])
                .map(Encode::forHtmlContent)
                .map(Integer::valueOf);
    }

    private static boolean hostHeaderContainsPort(String header) {
        return header.contains(":") && header.split(":").length > 1;
    }


    private static Integer getPortServlet(HttpServletRequest request) {
        return request.getServerPort() == 0 ? null : request.getServerPort();
    }

    public static Optional<String> fetchServerNameFromUri(String uri){
        return Optional.ofNullable(uri)
                .map(u -> getUri(u))
                .map(URI::getHost)
                .filter(StringUtils::isNotBlank);
    }

    public static Optional<String> fetchPathFromUri(String uri){
        return Optional.ofNullable(uri)
                .map(u -> getUri(u))
                .map(URI::getPath)
                .filter(StringUtils::isNotBlank);
    }

    public static Optional<String> removeContextRootFromPath(String path, HttpServletRequest request){
        return Optional.ofNullable(path).filter(StringUtils::isNotBlank).map(s -> {
            if(s.startsWith(request.getContextPath())){
                return s.replaceFirst(request.getContextPath(), "");
            }
            return s;
        }).filter(StringUtils::isNotBlank);
    }

    /**
     * Returns the parsed version of the provided URI string.
     * If the string is not a real URI null is returned instead.
     */
    private static URI getUri(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException ex) {
            log.debug("error with url:'{}'", url, ex);
            return null;
        }
    }

    public static URL composeBaseUrl(HttpServletRequest request){
        String reqScheme = UrlUtils.fetchScheme(request);
        String serverName = UrlUtils.fetchServer(request);
        Optional<Integer> port = UrlUtils.fetchPort(request);

        return generateUrl(reqScheme, serverName, port, request);
    }

    private static URL generateUrl(String reqScheme, String serverName, Optional<Integer> port, HttpServletRequest request) {
        try {
            URL baseUrl = null;
            if ( port.isEmpty() || isStandardHttp(reqScheme, port.get()) || isStandardHttps(reqScheme, port.get()) || forcedHttps(request, port.get()) ) {
                baseUrl = new URL(reqScheme, serverName, "");
            } else {
                baseUrl = new URL(reqScheme, serverName, port.get(), "");
            }
            return baseUrl;

        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static boolean isStandardHttp(String reqScheme, int port) {
        return HTTP_SCHEME.equals(reqScheme) && 80 == port;
    }

    private static boolean isStandardHttps(String reqScheme, int port) {
        return HTTPS_SCHEME.equals(reqScheme) && 443 == port;
    }

    private static boolean forcedHttps(HttpServletRequest request, int port) {
        return (HTTPS_SCHEME.equals(getProtoFromXHeader(request).orElse("")) && isStandardPort(port) )||
                (HTTPS_SCHEME.equals(getProtoFromEnv().orElse("")) && isStandardPort(port) ) ;
    }

    private static boolean isStandardPort(int port) {
        return 443 == port || 80 == port;
    }

    public static class EntUrlBuilder {
        private String url;
        private List<String> paths = new ArrayList<>();

        private EntUrlBuilder(){}

        public static EntUrlBuilder builder() {
            return new EntUrlBuilder();
        }

        public EntUrlBuilder url(String u){
            this.url = u;
            return this;
        }

        public EntUrlBuilder url(URI u){
            this.url = u.toString();
            return this;
        }

        public EntUrlBuilder path(String p){
            paths.add(p);
            return this;
        }

        public EntUrlBuilder paths(String ... u){
            paths.addAll(Arrays.asList(u));
            return this;
        }

        public URI build() {
            UriBuilder builder = UriComponentsBuilder.fromUriString(url);
            for (String path : paths) {
                if (StringUtils.isNotBlank(path)) {
                    path = path.trim();
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }
                    builder.path(path);
                }
            }
            return builder.build();
        }
    }

    private static final Pattern URI_SCHEME_PATTERN = Pattern.compile("^[a-z][a-z0-9+.-]*:.*$");

    /**
     * Tells if a string is not an absolute URI by checking that the string doesn't start with the "{scheme}:" pattern.
     * Note that this is not a safe way to identify a relative URI, so if you need it please use {@link #getUri(String)}
     */
    public static boolean isNotAbsoluteURI(String mayBeURI) {
        return !URI_SCHEME_PATTERN.matcher(mayBeURI).matches();
    }
}
