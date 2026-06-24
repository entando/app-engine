package org.entando.entando.apsadmin.system;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.entando.entando.apsadmin.system.RequestLoggingInterceptorConfig.Exclusion;
import org.junit.jupiter.api.Test;

class TestRequestLoggingInterceptorConfig {

    @Test
    void globalExclusionMatchesAnyUri() {
        Exclusion e = new Exclusion();
        e.setParameters(List.of("content"));
        assertTrue(e.matches("/do/Category/save.action", "content"));
        assertTrue(e.matches("/do/PageModel/save.action", "content"));
        assertFalse(e.matches("/do/Category/save.action", "title"));
    }

    @Test
    void actionPathExclusionMatchesWithContextPath() {
        Exclusion e = new Exclusion();
        e.setActionPath("/do/PageModel/save.action");
        e.setParameters(List.of("template", "extraConfig"));
        assertTrue(e.matches("/Entando/do/PageModel/save.action", "template"));
        assertTrue(e.matches("/Entando/do/PageModel/save.action", "extraConfig"));
        assertFalse(e.matches("/Entando/do/PageModel/save.action", "other"));
        assertFalse(e.matches("/Entando/do/Category/save.action", "template"));
    }

    @Test
    void configIsExcludedDelegatesToExclusions() {
        RequestLoggingInterceptorConfig config = new RequestLoggingInterceptorConfig();
        Exclusion e = new Exclusion();
        e.setActionPath("/do/PageModel/save.action");
        e.setParameters(List.of("template"));
        config.setExclusions(List.of(e));
        assertTrue(config.isExcluded("/Entando/do/PageModel/save.action", "template"));
        assertFalse(config.isExcluded("/Entando/do/PageModel/save.action", "other"));
        assertFalse(config.isExcluded("/Entando/do/Category/save.action", "template"));
    }
}
