package com.agiletec.aps.system.services.lang.cache;

import java.util.List;
import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

class LangManagerCacheWrapperTest {
    @Test
    void shouldInitCacheRunWithoutException() throws EntException {
        LangManagerCacheWrapper langManagerCacheWrapper = new LangManagerCacheWrapper();
        LangManagerCacheWrapper spy = Mockito.spy(langManagerCacheWrapper);

        String xmlConfig = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Langs>\n"
                + "\t<Lang>\n"
                + "\t\t<code>it</code>\n"
                + "\t\t<descr>Italiano</descr>\n"
                + "\t</Lang>\n"
                + "\t<Lang>\n"
                + "\t\t<code>en</code>\n"
                + "\t\t<descr>English</descr>\n"
                + "\t\t<default>true</default>\n"
                + "\t</Lang>\n"
                + "</Langs>";

        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        spy.setSpringCacheManager(cacheManager);
        Mockito.when(cacheManager.getCache(ArgumentMatchers.anyString())).thenReturn(Mockito.mock(Cache.class));

        Assertions.assertDoesNotThrow(() -> spy.initCache(xmlConfig, List.of()));
    }

    @Test
    void shouldInitCacheThrowEntExceptionThrowsEntException() {
        LangManagerCacheWrapper langManagerCacheWrapper = new LangManagerCacheWrapper();
        LangManagerCacheWrapper spy = Mockito.spy(langManagerCacheWrapper);

        CacheManager cacheManager = Mockito.mock(CacheManager.class);
        spy.setSpringCacheManager(cacheManager);
        Mockito.when(cacheManager.getCache(ArgumentMatchers.anyString())).thenReturn(Mockito.mock(Cache.class));

        Assertions.assertThrows(EntException.class, () -> spy.initCache("", List.of()));
    }
}
