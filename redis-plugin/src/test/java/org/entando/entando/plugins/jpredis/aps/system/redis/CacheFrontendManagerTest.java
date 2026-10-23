package org.entando.entando.plugins.jpredis.aps.system.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.support.caching.CacheAccessor;
import io.lettuce.core.support.caching.CacheFrontend;
import io.lettuce.core.support.caching.ClientSideCaching;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockedStatic.Verification;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CacheFrontendManagerTest {

    private static final InetSocketAddress ADDRESS = InetSocketAddress.createUnresolved("redis", 6379);

    @Mock
    private RedisClient redisClient;

    @Mock
    private StatefulRedisConnection<String, Object> connection;

    @Mock
    private ScheduledExecutorService executor;

    private final Deque<Runnable> scheduledTasks = new ArrayDeque<>();
    private final List<Long> scheduledDelays = new ArrayList<>();

    private CacheFrontendManager cacheFrontendManager;

    @BeforeEach
    void setUp() {
        Mockito.when(redisClient.connect(Mockito.any(RedisCodec.class))).thenReturn(connection);
        Mockito.lenient().when(executor.schedule(Mockito.any(Runnable.class), Mockito.anyLong(), Mockito.any()))
                .thenAnswer(invocation -> {
                    scheduledTasks.add(invocation.getArgument(0));
                    scheduledDelays.add(invocation.getArgument(1));
                    return null;
                });
        cacheFrontendManager = new CacheFrontendManager(redisClient, executor);
    }

    @AfterEach
    void tearDown() {
        cacheFrontendManager.close();
    }

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

    @Test
    void failedEnableShouldCloseConnection() {
        try (MockedStatic<ClientSideCaching> clientSideCachingMockedStatic = Mockito.mockStatic(
                ClientSideCaching.class)) {
            Verification enableCall = () -> ClientSideCaching.enable(Mockito.any(), Mockito.any(), Mockito.any());
            clientSideCachingMockedStatic.when(enableCall).thenThrow(new RedisConnectionException("injected"));

            Assertions.assertThrows(RedisConnectionException.class, () -> cacheFrontendManager.getCacheFrontend());

            Mockito.verify(connection).close();
            Mockito.verify(connection, Mockito.never()).addListener(Mockito.any(RedisConnectionStateListener.class));
        }
    }

    @Test
    void reconnectShouldRebuildFrontend() {
        try (MockedStatic<ClientSideCaching> clientSideCachingMockedStatic = Mockito.mockStatic(
                ClientSideCaching.class)) {
            Verification enableCall = () -> ClientSideCaching.enable(Mockito.any(), Mockito.any(), Mockito.any());
            CacheFrontend<String, Object> previous = Mockito.mock(CacheFrontend.class);
            CacheFrontend<String, Object> next = Mockito.mock(CacheFrontend.class);
            clientSideCachingMockedStatic.when(enableCall).thenReturn(previous).thenReturn(next);

            cacheFrontendManager.getCacheFrontend();
            reconnect(lastListener());
            runScheduledTasks();

            clientSideCachingMockedStatic.verify(enableCall, Mockito.times(2));
            Mockito.verify(previous).close();
            Assertions.assertSame(next, cacheFrontendManager.getCacheFrontend());
        }
    }

    @Test
    void reconnectShouldRefreshThroughOverriddenRebuild() {
        List<CacheFrontend<String, Object>> rebuilt = new ArrayList<>();
        CacheFrontendManager overriding = new CacheFrontendManager(redisClient, executor) {
            @Override
            public CacheFrontend<String, Object> rebuildCacheFrontend() {
                CacheFrontend<String, Object> frontend = super.rebuildCacheFrontend();
                rebuilt.add(frontend);
                return frontend;
            }
        };
        try (MockedStatic<ClientSideCaching> clientSideCachingMockedStatic = Mockito.mockStatic(
                ClientSideCaching.class)) {
            Verification enableCall = () -> ClientSideCaching.enable(Mockito.any(), Mockito.any(), Mockito.any());
            CacheFrontend<String, Object> next = Mockito.mock(CacheFrontend.class);
            clientSideCachingMockedStatic.when(enableCall).thenReturn(Mockito.mock(CacheFrontend.class))
                    .thenReturn(next);

            overriding.getCacheFrontend();
            reconnect(lastListener());
            runScheduledTasks();

            Assertions.assertEquals(List.of(next), rebuilt);
            Assertions.assertSame(next, overriding.getCacheFrontend());
        } finally {
            overriding.close();
        }
    }

    @Test
    void connectWithoutDisconnectShouldNotRebuild() {
        try (MockedStatic<ClientSideCaching> clientSideCachingMockedStatic = Mockito.mockStatic(
                ClientSideCaching.class)) {
            Verification enableCall = () -> ClientSideCaching.enable(Mockito.any(), Mockito.any(), Mockito.any());
            clientSideCachingMockedStatic.when(enableCall).thenReturn(Mockito.mock(CacheFrontend.class));

            cacheFrontendManager.getCacheFrontend();
            lastListener().onRedisConnected(null, ADDRESS);
            runScheduledTasks();

            clientSideCachingMockedStatic.verify(enableCall, Mockito.times(1));
            Assertions.assertTrue(scheduledDelays.isEmpty());
        }
    }

    @Test
    void reconnectShouldDisableLocalMapBeforeRebuild() {
        try (MockedStatic<ClientSideCaching> clientSideCachingMockedStatic = Mockito.mockStatic(
                ClientSideCaching.class)) {
            Verification enableCall = () -> ClientSideCaching.enable(Mockito.any(), Mockito.any(), Mockito.any());
            clientSideCachingMockedStatic.when(enableCall).thenReturn(Mockito.mock(CacheFrontend.class));

            cacheFrontendManager.getCacheFrontend();
            CacheAccessor<String, Object> accessor = firstAccessor(clientSideCachingMockedStatic);
            accessor.put("key", "value");
            Assertions.assertEquals("value", accessor.get("key"));

            reconnect(lastListener());

            Assertions.assertNull(accessor.get("key"));
            accessor.put("key", "value");
            Assertions.assertNull(accessor.get("key"));
            clientSideCachingMockedStatic.verify(enableCall, Mockito.times(1));
        }
    }

    @Test
    void failedRefreshShouldBeRetried() {
        try (MockedStatic<ClientSideCaching> clientSideCachingMockedStatic = Mockito.mockStatic(
                ClientSideCaching.class)) {
            Verification enableCall = () -> ClientSideCaching.enable(Mockito.any(), Mockito.any(), Mockito.any());
            CacheFrontend<String, Object> previous = Mockito.mock(CacheFrontend.class);
            CacheFrontend<String, Object> next = Mockito.mock(CacheFrontend.class);
            clientSideCachingMockedStatic.when(enableCall).thenReturn(previous)
                    .thenThrow(new RedisConnectionException("injected")).thenReturn(next);

            cacheFrontendManager.getCacheFrontend();
            reconnect(lastListener());
            runScheduledTasks();

            clientSideCachingMockedStatic.verify(enableCall, Mockito.times(3));
            Assertions.assertEquals(List.of(0L, 1000L), scheduledDelays);
            Mockito.verify(previous).close();
            Assertions.assertSame(next, cacheFrontendManager.getCacheFrontend());
        }
    }

    @Test
    void repeatedReconnectsShouldRebuildOnce() {
        try (MockedStatic<ClientSideCaching> clientSideCachingMockedStatic = Mockito.mockStatic(
                ClientSideCaching.class)) {
            Verification enableCall = () -> ClientSideCaching.enable(Mockito.any(), Mockito.any(), Mockito.any());
            clientSideCachingMockedStatic.when(enableCall).thenReturn(Mockito.mock(CacheFrontend.class));

            cacheFrontendManager.getCacheFrontend();
            RedisConnectionStateListener listener = lastListener();
            reconnect(listener);
            reconnect(listener);
            reconnect(listener);
            runScheduledTasks();

            clientSideCachingMockedStatic.verify(enableCall, Mockito.times(2));
            Assertions.assertEquals(List.of(0L), scheduledDelays);
        }
    }

    @Test
    void reconnectOfReplacedGenerationShouldNotRebuild() {
        try (MockedStatic<ClientSideCaching> clientSideCachingMockedStatic = Mockito.mockStatic(
                ClientSideCaching.class)) {
            Verification enableCall = () -> ClientSideCaching.enable(Mockito.any(), Mockito.any(), Mockito.any());
            CacheFrontend<String, Object> next = Mockito.mock(CacheFrontend.class);
            clientSideCachingMockedStatic.when(enableCall).thenReturn(Mockito.mock(CacheFrontend.class))
                    .thenReturn(next);

            cacheFrontendManager.getCacheFrontend();
            RedisConnectionStateListener replaced = lastListener();
            cacheFrontendManager.rebuildCacheFrontend();
            reconnect(replaced);
            runScheduledTasks();

            clientSideCachingMockedStatic.verify(enableCall, Mockito.times(2));
            Assertions.assertSame(next, cacheFrontendManager.getCacheFrontend());
        }
    }

    @Test
    void closeShouldStopPendingRefresh() {
        try (MockedStatic<ClientSideCaching> clientSideCachingMockedStatic = Mockito.mockStatic(
                ClientSideCaching.class)) {
            Verification enableCall = () -> ClientSideCaching.enable(Mockito.any(), Mockito.any(), Mockito.any());
            clientSideCachingMockedStatic.when(enableCall).thenReturn(Mockito.mock(CacheFrontend.class));

            cacheFrontendManager.getCacheFrontend();
            RedisConnectionStateListener listener = lastListener();
            reconnect(listener);
            cacheFrontendManager.close();
            runScheduledTasks();
            reconnect(listener);
            runScheduledTasks();

            clientSideCachingMockedStatic.verify(enableCall, Mockito.times(1));
            Mockito.verify(executor, Mockito.atLeastOnce()).shutdownNow();
            Assertions.assertEquals(List.of(0L), scheduledDelays);
        }
    }

    private RedisConnectionStateListener lastListener() {
        ArgumentCaptor<RedisConnectionStateListener> captor = ArgumentCaptor.forClass(
                RedisConnectionStateListener.class);
        Mockito.verify(connection, Mockito.atLeastOnce()).addListener(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private CacheAccessor<String, Object> firstAccessor(MockedStatic<ClientSideCaching> clientSideCachingMockedStatic) {
        ArgumentCaptor<CacheAccessor<String, Object>> captor = ArgumentCaptor.forClass(CacheAccessor.class);
        clientSideCachingMockedStatic.verify(
                () -> ClientSideCaching.enable(captor.capture(), Mockito.any(), Mockito.any()), Mockito.atLeastOnce());
        return captor.getAllValues().get(0);
    }

    private void reconnect(RedisConnectionStateListener listener) {
        listener.onRedisDisconnected(null);
        listener.onRedisConnected(null, ADDRESS);
    }

    private void runScheduledTasks() {
        Runnable task;
        while ((task = scheduledTasks.poll()) != null) {
            task.run();
        }
    }
}
