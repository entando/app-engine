package org.entando.entando.plugins.jpredis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.entando.entando.plugins.jpredis.aps.system.redis.CacheFrontendManager;
import org.entando.entando.plugins.jpredis.aps.system.redis.LettuceCache;
import org.entando.entando.plugins.jpredis.aps.system.redis.LettuceCacheManager;
import org.entando.entando.plugins.jpredis.aps.system.redis.SentinelTopologyRefreshManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
class SentinelTopologyRefreshManagerTest {

    private static final List<Map<String, String>> DEFAULT_MASTER = List.of(Map.of("ip", "redis1"));

    @Mock
    private RedisClient redisClient;
    @Mock
    private RedisURI redisURI;
    @Mock
    private LettuceCacheManager cacheManager;
    @Mock
    private CacheFrontendManager cacheFrontendManager;
    @Mock
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;
    @Mock
    private RedisPubSubCommands<String, String> pubSubCommands;

    @BeforeEach
    void setUp() {
        Mockito.when(redisURI.getSentinels()).thenReturn(List.of(Mockito.mock(RedisURI.class)));
        Mockito.when(redisClient.connectPubSub(Mockito.any(RedisURI.class))).thenReturn(pubSubConnection);
        Mockito.when(pubSubConnection.sync()).thenReturn(pubSubCommands);
    }

    @Test
    void shouldRebuildCacheFrontendIfMasterIsChanged() {
        mockMastersList(DEFAULT_MASTER);
        mockRebuildCacheFrontend();
        ArgumentCaptor<RedisPubSubAdapter> listenerCaptor = ArgumentCaptor.forClass(RedisPubSubAdapter.class);
        Mockito.doNothing().when(pubSubConnection).addListener(listenerCaptor.capture());
        createSentinelTopologyRefreshManager();
        listenerCaptor.getValue().message("+switch-master", "+switch-master", "redismaster redis1 6379 redis2 6379");
        Mockito.verify(cacheManager, Mockito.timeout(1000).times(1)).updateCacheFrontend(Mockito.any());
    }

    @Test
    void shouldRebuildCacheFrontendIfMasterIsChangedAndMasterWasNotInitiallyDetected() {
        mockMastersList(List.of());
        mockRebuildCacheFrontend();
        ArgumentCaptor<RedisPubSubAdapter> listenerCaptor = ArgumentCaptor.forClass(RedisPubSubAdapter.class);
        Mockito.doNothing().when(pubSubConnection).addListener(listenerCaptor.capture());
        createSentinelTopologyRefreshManager();
        listenerCaptor.getValue().message("+switch-master", "+switch-master", "redismaster redis1 6379 redis2 6379");
        Mockito.verify(cacheManager, Mockito.timeout(1000).times(1)).updateCacheFrontend(Mockito.any());
    }

    @Test
    void shouldNotRebuildCacheFrontendIfMasterIsNotChanged() {
        mockMastersList(DEFAULT_MASTER);
        ArgumentCaptor<RedisPubSubAdapter> listenerCaptor = ArgumentCaptor.forClass(RedisPubSubAdapter.class);
        Mockito.doNothing().when(pubSubConnection).addListener(listenerCaptor.capture());
        createSentinelTopologyRefreshManager();
        listenerCaptor.getValue().message("+switch-master", "+switch-master", "redismaster redis3 6379 redis1 6379");
        Mockito.verify(cacheManager, Mockito.never()).updateCacheFrontend(Mockito.any());
    }

    @Test
    void shouldIgnoreInvalidMessageFormats() {
        mockMastersList(DEFAULT_MASTER);
        ArgumentCaptor<RedisPubSubAdapter> listenerCaptor = ArgumentCaptor.forClass(RedisPubSubAdapter.class);
        Mockito.doNothing().when(pubSubConnection).addListener(listenerCaptor.capture());
        createSentinelTopologyRefreshManager();
        listenerCaptor.getValue().message("+switch-master", "+switch-master", "this-is-not-valid");
        Mockito.verify(cacheManager, Mockito.never()).updateCacheFrontend(Mockito.any());
    }

    @Test
    void shouldRetryRebuildWhenFirstAttemptFails() {
        mockMastersList(DEFAULT_MASTER);
        Mockito.doThrow(new RedisConnectionException("injected")).doReturn(null)
                .when(cacheFrontendManager).rebuildCacheFrontend();
        ArgumentCaptor<RedisPubSubAdapter> listenerCaptor = ArgumentCaptor.forClass(RedisPubSubAdapter.class);
        Mockito.doNothing().when(pubSubConnection).addListener(listenerCaptor.capture());
        createSentinelTopologyRefreshManager();
        listenerCaptor.getValue().message("+switch-master", "+switch-master", "redismaster redis1 6379 redis2 6379");
        Mockito.verify(cacheFrontendManager, Mockito.timeout(5000).times(2)).rebuildCacheFrontend();
    }

    @Test
    void shouldIgnoreSwitchMasterOfAnotherMaster() {
        mockMastersList(DEFAULT_MASTER);
        Mockito.lenient().when(redisURI.getSentinelMasterId()).thenReturn("redismaster");
        ArgumentCaptor<RedisPubSubAdapter> listenerCaptor = ArgumentCaptor.forClass(RedisPubSubAdapter.class);
        Mockito.doNothing().when(pubSubConnection).addListener(listenerCaptor.capture());
        createSentinelTopologyRefreshManager();
        listenerCaptor.getValue().message("+switch-master", "+switch-master", "othermaster redis1 6379 redis2 6379");
        Mockito.verify(cacheFrontendManager, Mockito.after(1000).never()).rebuildCacheFrontend();
    }

    @Test
    void shouldStartWhenOneSentinelIsUnreachable() {
        mockMastersList(DEFAULT_MASTER);
        RedisURI unreachableSentinel = Mockito.mock(RedisURI.class);
        RedisURI reachableSentinel = Mockito.mock(RedisURI.class);
        Mockito.when(redisURI.getSentinels()).thenReturn(List.of(unreachableSentinel, reachableSentinel));
        Mockito.doThrow(new RedisConnectionException("injected")).when(redisClient).connectPubSub(unreachableSentinel);
        Assertions.assertDoesNotThrow(
                () -> new SentinelTopologyRefreshManager(redisClient, redisURI, cacheManager, cacheFrontendManager));
        Mockito.verify(pubSubCommands, Mockito.times(1)).psubscribe("+switch-master");
    }

    @Test
    void shouldNotRunRebuildsConcurrently() throws Exception {
        mockMastersList(DEFAULT_MASTER);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            firstEntered.countDown();
            entered.countDown();
            release.await(5, TimeUnit.SECONDS);
            return null;
        }).when(cacheFrontendManager).rebuildCacheFrontend();
        ArgumentCaptor<RedisPubSubAdapter> listenerCaptor = ArgumentCaptor.forClass(RedisPubSubAdapter.class);
        Mockito.doNothing().when(pubSubConnection).addListener(listenerCaptor.capture());
        createSentinelTopologyRefreshManager();
        listenerCaptor.getValue().message("+switch-master", "+switch-master", "redismaster redis1 6379 redis2 6379");
        Assertions.assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
        listenerCaptor.getValue().message("+switch-master", "+switch-master", "redismaster redis2 6379 redis1 6379");
        boolean overlapped = entered.await(2, TimeUnit.SECONDS);
        release.countDown();
        Assertions.assertFalse(overlapped, "two rebuilds ran at the same time");
    }

    private void createSentinelTopologyRefreshManager() {
        new SentinelTopologyRefreshManager(redisClient, redisURI, cacheManager, cacheFrontendManager);
        Mockito.verify(pubSubCommands, Mockito.times(1)).psubscribe("+switch-master");
    }

    private void mockMastersList(List<Map<String, String>> masters) {
        StatefulRedisSentinelConnection<String, String> connection = Mockito.mock(
                StatefulRedisSentinelConnection.class);
        RedisSentinelCommands<String, String> commands = Mockito.mock(RedisSentinelCommands.class);
        Mockito.when(connection.sync()).thenReturn(commands);
        Mockito.when(commands.masters()).thenReturn(masters);
        Mockito.when(redisClient.connectSentinel()).thenReturn(connection);
    }

    private void mockRebuildCacheFrontend() {
        Mockito.when(cacheManager.getCacheNames()).thenReturn(List.of("cache1"));
        Mockito.when(cacheManager.getCache("cache1")).thenReturn(Mockito.mock(LettuceCache.class));
    }
}
