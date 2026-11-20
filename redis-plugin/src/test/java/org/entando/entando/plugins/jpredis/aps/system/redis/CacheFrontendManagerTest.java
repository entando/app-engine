package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.support.caching.CacheFrontend;
import io.lettuce.core.support.caching.ClientSideCaching;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockedStatic.Verification;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CacheFrontendManagerTest {

    @Mock
    private RedisClient redisClient;

    @InjectMocks
    private CacheFrontendManager cacheFrontendManager;

    @Test
    void testRebuildCacheFrontend() {
        try (MockedStatic<ClientSideCaching> clientSideCachingMockedStatic = Mockito.mockStatic(
                ClientSideCaching.class)) {
            Verification enableCall = () -> ClientSideCaching.enable(Mockito.any(), Mockito.any(), Mockito.any());

            CacheFrontend<String, Object> cacheFrontend = Mockito.mock(CacheFrontend.class);
            clientSideCachingMockedStatic.when(enableCall).thenReturn(cacheFrontend);

            cacheFrontendManager.getCacheFrontend();
            cacheFrontendManager.rebuildCacheFrontend();

            clientSideCachingMockedStatic.verify(enableCall, Mockito.times(2));
            Mockito.verify(cacheFrontend, Mockito.times(1)).close();
        }
    }

    @Test
    void rebuildShouldEnableNewFrontendBeforeClosingPrevious() {
        try (MockedStatic<ClientSideCaching> clientSideCachingMockedStatic = Mockito.mockStatic(
                ClientSideCaching.class)) {
            Verification enableCall = () -> ClientSideCaching.enable(Mockito.any(), Mockito.any(), Mockito.any());
            List<String> calls = new ArrayList<>();
            CacheFrontend<String, Object> previous = Mockito.mock(CacheFrontend.class);
            CacheFrontend<String, Object> next = Mockito.mock(CacheFrontend.class);
            clientSideCachingMockedStatic.when(enableCall).thenAnswer(invocation -> {
                calls.add("enable");
                return calls.size() == 1 ? previous : next;
            });
            Mockito.doAnswer(invocation -> calls.add("close previous")).when(previous).close();

            cacheFrontendManager.getCacheFrontend();
            cacheFrontendManager.rebuildCacheFrontend();

            Assertions.assertEquals(List.of("enable", "enable", "close previous"), calls);
        }
    }

    @Test
    void failedRebuildShouldKeepPreviousFrontendPublishedAndOpen() {
        try (MockedStatic<ClientSideCaching> clientSideCachingMockedStatic = Mockito.mockStatic(
                ClientSideCaching.class)) {
            Verification enableCall = () -> ClientSideCaching.enable(Mockito.any(), Mockito.any(), Mockito.any());
            CacheFrontend<String, Object> previous = Mockito.mock(CacheFrontend.class);
            clientSideCachingMockedStatic.when(enableCall).thenReturn(previous)
                    .thenThrow(new RedisConnectionException("injected"));

            cacheFrontendManager.getCacheFrontend();
            Assertions.assertThrows(RedisConnectionException.class, () -> cacheFrontendManager.rebuildCacheFrontend());

            Assertions.assertSame(previous, cacheFrontendManager.getCacheFrontend());
            Mockito.verify(previous, Mockito.never()).close();
        }
    }

    @Test
    void failedCloseShouldKeepNewFrontendPublished() {
        try (MockedStatic<ClientSideCaching> clientSideCachingMockedStatic = Mockito.mockStatic(
                ClientSideCaching.class)) {
            Verification enableCall = () -> ClientSideCaching.enable(Mockito.any(), Mockito.any(), Mockito.any());
            CacheFrontend<String, Object> previous = Mockito.mock(CacheFrontend.class);
            CacheFrontend<String, Object> next = Mockito.mock(CacheFrontend.class);
            clientSideCachingMockedStatic.when(enableCall).thenReturn(previous).thenReturn(next);
            Mockito.doThrow(new RedisConnectionException("injected")).when(previous).close();

            cacheFrontendManager.getCacheFrontend();

            Assertions.assertSame(next, cacheFrontendManager.rebuildCacheFrontend());
            Assertions.assertSame(next, cacheFrontendManager.getCacheFrontend());
        }
    }
}
