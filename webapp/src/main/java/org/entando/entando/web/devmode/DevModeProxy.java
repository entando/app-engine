package org.entando.entando.web.devmode;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.eclipse.jetty.http.HttpStatus;
import org.entando.entando.aps.util.UrlUtils;
import springfox.documentation.spring.web.paths.Paths;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

public class DevModeProxy extends HttpServlet {

    public static final String CONFIG = System.getenv("ENTANDO_PROXY_CONFIG");
    public static final String ADDR_CM = "http://localhost:8083";

    public DevModeProxy() {
        super();
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpURLConnection target = connectToTarget(request, CONFIG);
        forwardRequestToTarget(request, target);
        sendResponseBackToClient(response, target);
    }

    private static void forwardRequestToTarget(HttpServletRequest request, HttpURLConnection target) throws IOException {
        // METHOD
        target.setRequestMethod(request.getMethod());

        // HEADERS
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> target.setRequestProperty(headerName, request.getHeader(headerName)));

        // BODY
        if (request.getMethod().equals("POST") || request.getMethod().equals("PUT")) {
            target.setDoOutput(true);
            target.getOutputStream().write(request.getInputStream().readAllBytes());
        }
    }

    private static void sendResponseBackToClient(HttpServletResponse response, HttpURLConnection target) throws IOException {
        // STATUS
        int status = target.getResponseCode();
        response.setStatus(status);

        // HEADERS
        target.getHeaderFields().forEach((headerName, headerValues) -> {
            if (headerName != null) headerValues.forEach(headerValue -> response.addHeader(headerName, headerValue));
        });

        // BODY
        response.getOutputStream().write(readContent(target, status));
    }

    private static byte[] readContent(HttpURLConnection target, int status) throws IOException {
        InputStream contentStream = (status < HttpStatus.BAD_REQUEST_400) ? target.getInputStream() : target.getErrorStream();
        return (contentStream != null) ? contentStream.readAllBytes() : new byte[0];
    }

    private static HttpURLConnection connectToTarget(HttpServletRequest request, String targetUrl) throws IOException {
        String context = determineContext(request);
        String address = determineAddress(request.getRequestURI(), context);
        return (HttpURLConnection) mkTargetUrl(request, address).openConnection();
    }

    private static String determineAddress(String targetURL, String context) {
        String targetAddr;
        if (targetURL.startsWith("/digital-exchange/")) {
            targetAddr = ADDR_CM + "/digital-exchange/" + context;
        } else {
            throw new RuntimeException(String.format("Unhandled URL \"%s\" (unknown http authority)", targetURL));
        }
        return targetAddr;
    }

    private static String determineContext(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String tenantCode = request.getHeader(UrlUtils.ENTANDO_TENANT_CODE_CUSTOM_HEADER);
        String context;

        String[] parts = request.getHeader("Authorization").replaceFirst("^Bearer ", "").split("\\.");
        if (parts.length != 3) {
            return "";
        }

        String realm = null;
        try {
            String part1 = new String(Base64.getUrlDecoder().decode(parts[1]));
            String[] arr = ((String) new JSONObject(part1).get("iss")).split("/");
            realm = arr[arr.length - 1];
        } catch (Exception e) {
            return "";
        }

        if (Objects.equals(realm, "entando")) {
            context = "";
        } else if (Objects.equals(realm, "tenant1")) {
            context = "tenant1/";
        } else {
            throw new RuntimeException(String.format("Unhandled ORIGIN \"%s\"", origin));
        }
        return context;
    }

    private static URL mkTargetUrl(HttpServletRequest request, String targetAuthority) throws MalformedURLException {
        String rest = request.getRequestURI().replaceFirst("^" + request.getServletPath(), "");
        String base = targetAuthority;
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String sep = (rest.startsWith("/")) ? "" : "/";
        base = base + sep + rest;
        String queryString = request.getQueryString();
        return new URL((queryString != null) ? base + "?" + queryString : base);
    }
}