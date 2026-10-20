package org.entando.entando.plugins.jpredis.aps.system.redis.session;

import jakarta.servlet.ServletContext;
import jakarta.servlet.SessionCookieConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.util.UrlUtils;
import org.entando.entando.plugins.jpredis.aps.system.redis.conditions.RedisSessionActive;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.context.ServletContextAware;

@Slf4j
@Configuration
@EnableRedisHttpSession
@RedisSessionActive(true)
public class RedisSessionConfig implements ServletContextAware {

    static final String ENTANDO_SECURE_SECRET_COOKIES = "ENTANDO_SECURE_SECRET_COOKIES";

    private ServletContext servletContext;

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        return createCookieSerializer(isSecureCookie(System.getenv(ENTANDO_SECURE_SECRET_COOKIES),
                UrlUtils.determineForceHttps()));
    }

    /**
     * Same rule StartupListener applies to the container session cookie.
     */
    static boolean isSecureCookie(String secureFlag, boolean forceHttps) {
        return StringUtils.isNotEmpty(secureFlag) ? Boolean.parseBoolean(secureFlag) : forceHttps;
    }

    /**
     * Copies the servlet session cookie config as Spring Session's default serializer does, adding Secure when
     * requested; otherwise Secure follows request.isSecure().
     */
    DefaultCookieSerializer createCookieSerializer(boolean secure) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        SessionCookieConfig config = null;
        if (this.servletContext != null) {
            try {
                config = this.servletContext.getSessionCookieConfig();
            } catch (UnsupportedOperationException ex) {
                log.warn("Unable to obtain SessionCookieConfig: {}", ex.getMessage());
            }
        }
        if (config != null) {
            if (config.getName() != null) {
                serializer.setCookieName(config.getName());
            }
            if (config.getDomain() != null) {
                serializer.setDomainName(config.getDomain());
            }
            if (config.getPath() != null) {
                serializer.setCookiePath(config.getPath());
            }
            if (config.getMaxAge() != -1) {
                serializer.setCookieMaxAge(config.getMaxAge());
            }
        }
        if (secure) {
            serializer.setUseSecureCookie(true);
            log.info("Spring Session cookie Secure flag: TRUE");
        }
        return serializer;
    }
}
