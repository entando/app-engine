package org.entando.entando.plugins.jpredis.aps.system.redis.session;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.session.web.http.CookieSerializer.CookieValue;
import org.springframework.session.web.http.DefaultCookieSerializer;

class RedisSessionConfigTest {

    @Test
    void testIsSecureCookie() {
        Assertions.assertTrue(RedisSessionConfig.isSecureCookie("true", false));
        Assertions.assertTrue(RedisSessionConfig.isSecureCookie("TRUE", false));
        Assertions.assertFalse(RedisSessionConfig.isSecureCookie("false", true));
        Assertions.assertTrue(RedisSessionConfig.isSecureCookie(null, true));
        Assertions.assertTrue(RedisSessionConfig.isSecureCookie("", true));
        Assertions.assertFalse(RedisSessionConfig.isSecureCookie(null, false));
        Assertions.assertFalse(RedisSessionConfig.isSecureCookie("", false));
    }

    @Test
    void testSecureCookieOverHttp() {
        String header = writeCookie(new RedisSessionConfig().createCookieSerializer(true), false);
        Assertions.assertTrue(header.startsWith("SESSION="));
    }

    @Test
    void testNotSecureCookieFollowsRequest() {
        RedisSessionConfig config = new RedisSessionConfig();
        Assertions.assertFalse(writeCookie(config.createCookieSerializer(false), false).contains("Secure"));
        Assertions.assertTrue(writeCookie(config.createCookieSerializer(false), true).contains("; Secure"));
    }

    @Test
    void testSessionCookieConfigCopied() {
        MockServletContext servletContext = new MockServletContext();
        servletContext.getSessionCookieConfig().setName("CUSTOM");
        servletContext.getSessionCookieConfig().setPath("/ctx");
        servletContext.getSessionCookieConfig().setDomain("example.org");
        servletContext.getSessionCookieConfig().setMaxAge(60);
        RedisSessionConfig config = new RedisSessionConfig();
        config.setServletContext(servletContext);

        String header = writeCookie(config.createCookieSerializer(true), false);

        Assertions.assertTrue(header.startsWith("CUSTOM="));
        Assertions.assertTrue(header.contains("; Max-Age=60"));
        Assertions.assertTrue(header.contains("; Domain=example.org"));
        Assertions.assertTrue(header.contains("; Path=/ctx"));
        Assertions.assertTrue(header.contains("; Secure"));
    }

    @Test
    void testDefaultNameWithoutSessionCookieConfigName() {
        RedisSessionConfig config = new RedisSessionConfig();
        config.setServletContext(new MockServletContext());
        Assertions.assertTrue(writeCookie(config.createCookieSerializer(false), false).startsWith("SESSION="));
    }

    private String writeCookie(DefaultCookieSerializer serializer, boolean secureRequest) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSecure(secureRequest);
        MockHttpServletResponse response = new MockHttpServletResponse();
        serializer.writeCookieValue(new CookieValue(request, response, "id"));
        return response.getHeader("Set-Cookie");
    }
}
