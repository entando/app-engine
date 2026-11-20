package org.entando.entando.plugins.jpredis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.entando.entando.aps.system.services.cache.RedisEnvironmentVariables;
import org.entando.entando.plugins.jpredis.aps.system.redis.CacheFrontendManager;
import org.entando.entando.plugins.jpredis.aps.system.redis.LettuceCacheManager;
import org.entando.entando.plugins.jpredis.aps.system.redis.SentinelSchedulerConfig;
import org.entando.entando.plugins.jpredis.aps.system.redis.SentinelTopologyRefreshManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@ExtendWith({MockitoExtension.class})
class SentinelTopologyRefreshManagerTest {

    private static final String MASTER_ID = "redismaster";
    private static final InetSocketAddress REDIS_1 = InetSocketAddress.createUnresolved("redis1", 6379);
    private static final InetSocketAddress REDIS_2 = InetSocketAddress.createUnresolved("redis2", 6379);

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
    @Mock
    private RedisSentinelCommands<String, String> sentinelCommands;

    private SentinelTopologyRefreshManager refreshManager;

    @BeforeEach
    void setUp() {
        Mockito.when(redisURI.getSentinelMasterId()).thenReturn(MASTER_ID);
        Mockito.when(redisURI.getSentinels()).thenReturn(List.of(Mockito.mock(RedisURI.class)));
        Mockito.when(redisClient.connectPubSub(Mockito.any(RedisURI.class))).thenReturn(pubSubConnection);
        Mockito.when(pubSubConnection.sync()).thenReturn(pubSubCommands);
        StatefulRedisSentinelConnection<String, String> sentinelConnection = Mockito.mock(
                StatefulRedisSentinelConnection.class);
        Mockito.when(sentinelConnection.sync()).thenReturn(sentinelCommands);
        Mockito.when(redisClient.connectSentinel()).thenReturn(sentinelConnection);
    }

    @AfterEach
    void tearDown() {
        if (refreshManager != null) {
            refreshManager.close();
        }
    }

    @Test
    void shouldRebuildCacheFrontendIfMasterIsChanged() {
        Mockito.when(sentinelCommands.getMasterAddrByName(MASTER_ID)).thenReturn(REDIS_1).thenReturn(REDIS_2);
        RedisPubSubAdapter<String, String> listener = createSentinelTopologyRefreshManager();
        listener.message("+switch-master", "+switch-master", "redismaster redis1 6379 redis2 6379");
        Mockito.verify(cacheFrontendManager, Mockito.timeout(1000).times(1)).rebuildCacheFrontend();
    }

    @Test
    void shouldRebuildCacheFrontendIfMasterIsChangedAndMasterWasNotInitiallyDetected() {
        Mockito.when(sentinelCommands.getMasterAddrByName(MASTER_ID)).thenReturn(null).thenReturn(REDIS_2);
        RedisPubSubAdapter<String, String> listener = createSentinelTopologyRefreshManager();
        listener.message("+switch-master", "+switch-master", "redismaster redis1 6379 redis2 6379");
        Mockito.verify(cacheFrontendManager, Mockito.timeout(1000).times(1)).rebuildCacheFrontend();
    }

    @Test
    void shouldRebuildCacheFrontendIfInitialMasterLookupFails() {
        Mockito.when(sentinelCommands.getMasterAddrByName(MASTER_ID))
                .thenThrow(new RedisConnectionException("injected")).thenReturn(REDIS_2);
        RedisPubSubAdapter<String, String> listener = createSentinelTopologyRefreshManager();
        listener.message("+switch-master", "+switch-master", "redismaster redis1 6379 redis2 6379");
        Mockito.verify(cacheFrontendManager, Mockito.timeout(1000).times(1)).rebuildCacheFrontend();
    }

    @Test
    void shouldNotRebuildCacheFrontendIfMasterIsNotChanged() {
        Mockito.when(sentinelCommands.getMasterAddrByName(MASTER_ID)).thenReturn(REDIS_1);
        RedisPubSubAdapter<String, String> listener = createSentinelTopologyRefreshManager();
        listener.message("+switch-master", "+switch-master", "redismaster redis3 6379 redis1 6379");
        Mockito.verify(cacheFrontendManager, Mockito.after(500).never()).rebuildCacheFrontend();
    }

    @Test
    void shouldIgnoreInvalidMessageFormats() {
        Mockito.when(sentinelCommands.getMasterAddrByName(MASTER_ID)).thenReturn(REDIS_1);
        RedisPubSubAdapter<String, String> listener = createSentinelTopologyRefreshManager();
        listener.message("+switch-master", "+switch-master", "this-is-not-valid");
        Mockito.verify(cacheFrontendManager, Mockito.after(500).never()).rebuildCacheFrontend();
    }

    @Test
    void shouldRetryRebuildWhenFirstAttemptFails() {
        Mockito.when(sentinelCommands.getMasterAddrByName(MASTER_ID)).thenReturn(REDIS_1).thenReturn(REDIS_2);
        Mockito.doThrow(new RedisConnectionException("injected")).doReturn(null)
                .when(cacheFrontendManager).rebuildCacheFrontend();
        RedisPubSubAdapter<String, String> listener = createSentinelTopologyRefreshManager();
        listener.message("+switch-master", "+switch-master", "redismaster redis1 6379 redis2 6379");
        Mockito.verify(cacheFrontendManager, Mockito.timeout(5000).times(2)).rebuildCacheFrontend();
        Mockito.verify(cacheFrontendManager, Mockito.after(1500).times(2)).rebuildCacheFrontend();
    }

    @Test
    void shouldRebuildOnlyAfterSentinelAgreesOnNewMaster() {
        Mockito.when(sentinelCommands.getMasterAddrByName(MASTER_ID))
                .thenReturn(REDIS_1).thenReturn(REDIS_1).thenReturn(REDIS_2);
        RedisPubSubAdapter<String, String> listener = createSentinelTopologyRefreshManager();
        listener.message("+switch-master", "+switch-master", "redismaster redis1 6379 redis2 6379");
        Mockito.verify(sentinelCommands, Mockito.timeout(500).times(2)).getMasterAddrByName(MASTER_ID);
        Mockito.verify(cacheFrontendManager, Mockito.never()).rebuildCacheFrontend();
        Mockito.verify(cacheFrontendManager, Mockito.timeout(5000).times(1)).rebuildCacheFrontend();
    }

    @Test
    void shouldRebuildOnceForSameEventFromSeveralSentinels() {
        Mockito.when(sentinelCommands.getMasterAddrByName(MASTER_ID)).thenReturn(REDIS_1).thenReturn(REDIS_2);
        RedisPubSubAdapter<String, String> listener = createSentinelTopologyRefreshManager();
        for (int i = 0; i < 3; i++) {
            listener.message("+switch-master", "+switch-master", "redismaster redis1 6379 redis2 6379");
        }
        Mockito.verify(cacheFrontendManager, Mockito.timeout(1000).times(1)).rebuildCacheFrontend();
        Mockito.verify(cacheFrontendManager, Mockito.after(500).times(1)).rebuildCacheFrontend();
    }

    @Test
    void shouldApplyNewestMasterOnBackToBackFailovers() throws Exception {
        Mockito.when(sentinelCommands.getMasterAddrByName(MASTER_ID))
                .thenReturn(REDIS_1).thenReturn(REDIS_2).thenReturn(REDIS_1);
        CountDownLatch firstRebuildStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            firstRebuildStarted.countDown();
            release.await(5, TimeUnit.SECONDS);
            return null;
        }).doReturn(null).when(cacheFrontendManager).rebuildCacheFrontend();
        RedisPubSubAdapter<String, String> listener = createSentinelTopologyRefreshManager();
        listener.message("+switch-master", "+switch-master", "redismaster redis1 6379 redis2 6379");
        Assertions.assertTrue(firstRebuildStarted.await(2, TimeUnit.SECONDS));
        listener.message("+switch-master", "+switch-master", "redismaster redis2 6379 redis1 6379");
        release.countDown();
        Mockito.verify(cacheFrontendManager, Mockito.timeout(2000).times(2)).rebuildCacheFrontend();
        Mockito.verify(sentinelCommands, Mockito.times(3)).getMasterAddrByName(MASTER_ID);
    }

    @Test
    void shouldIgnoreSwitchMasterOfAnotherMaster() {
        Mockito.when(sentinelCommands.getMasterAddrByName(MASTER_ID)).thenReturn(REDIS_1);
        RedisPubSubAdapter<String, String> listener = createSentinelTopologyRefreshManager();
        listener.message("+switch-master", "+switch-master", "othermaster redis1 6379 redis2 6379");
        Mockito.verify(cacheFrontendManager, Mockito.after(1000).never()).rebuildCacheFrontend();
    }

    @Test
    void shouldStartWhenOneSentinelIsUnreachable() {
        Mockito.when(sentinelCommands.getMasterAddrByName(MASTER_ID)).thenReturn(REDIS_1);
        RedisURI unreachableSentinel = Mockito.mock(RedisURI.class);
        RedisURI reachableSentinel = Mockito.mock(RedisURI.class);
        Mockito.when(redisURI.getSentinels()).thenReturn(List.of(unreachableSentinel, reachableSentinel));
        Mockito.doThrow(new RedisConnectionException("injected")).when(redisClient).connectPubSub(unreachableSentinel);
        Assertions.assertDoesNotThrow(() -> refreshManager =
                new SentinelTopologyRefreshManager(redisClient, redisURI, cacheFrontendManager));
        Mockito.verify(pubSubCommands, Mockito.times(1)).psubscribe("+switch-master");
    }

    @Test
    void shouldNotRunRebuildsConcurrently() throws Exception {
        Mockito.when(sentinelCommands.getMasterAddrByName(MASTER_ID))
                .thenReturn(REDIS_1).thenReturn(REDIS_2).thenReturn(REDIS_1);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            firstEntered.countDown();
            entered.countDown();
            release.await(5, TimeUnit.SECONDS);
            return null;
        }).when(cacheFrontendManager).rebuildCacheFrontend();
        RedisPubSubAdapter<String, String> listener = createSentinelTopologyRefreshManager();
        listener.message("+switch-master", "+switch-master", "redismaster redis1 6379 redis2 6379");
        Assertions.assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
        listener.message("+switch-master", "+switch-master", "redismaster redis2 6379 redis1 6379");
        boolean overlapped = entered.await(2, TimeUnit.SECONDS);
        release.countDown();
        Assertions.assertFalse(overlapped, "two rebuilds ran at the same time");
        Mockito.verify(cacheFrontendManager, Mockito.timeout(2000).times(2)).rebuildCacheFrontend();
    }

    @Test
    void shouldNotRebuildAfterClose() {
        Mockito.when(sentinelCommands.getMasterAddrByName(MASTER_ID)).thenReturn(REDIS_1).thenReturn(REDIS_2);
        Mockito.doThrow(new RedisConnectionException("injected")).when(cacheFrontendManager).rebuildCacheFrontend();
        RedisPubSubAdapter<String, String> listener = createSentinelTopologyRefreshManager();
        listener.message("+switch-master", "+switch-master", "redismaster redis1 6379 redis2 6379");
        Mockito.verify(cacheFrontendManager, Mockito.timeout(1000).times(1)).rebuildCacheFrontend();
        refreshManager.close();
        Mockito.verify(pubSubConnection).close();
        Mockito.verify(cacheFrontendManager, Mockito.after(2000).times(1)).rebuildCacheFrontend();
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedConstructorShouldRebuildThroughCacheFrontendManager() {
        Mockito.when(sentinelCommands.getMasterAddrByName(MASTER_ID)).thenReturn(REDIS_1).thenReturn(REDIS_2);
        ArgumentCaptor<RedisPubSubAdapter> listenerCaptor = ArgumentCaptor.forClass(RedisPubSubAdapter.class);
        Mockito.doNothing().when(pubSubConnection).addListener(listenerCaptor.capture());
        refreshManager = new SentinelTopologyRefreshManager(redisClient, redisURI, cacheManager, cacheFrontendManager);
        listenerCaptor.getValue().message("+switch-master", "+switch-master", "redismaster redis1 6379 redis2 6379");
        Mockito.verify(cacheFrontendManager, Mockito.timeout(1000).times(1)).rebuildCacheFrontend();
        Mockito.verifyNoInteractions(cacheManager);
    }

    @Test
    void springShouldCreateManagerInEventModeWithoutCacheManager() {
        Mockito.when(sentinelCommands.getMasterAddrByName(MASTER_ID)).thenReturn(REDIS_1).thenReturn(REDIS_2);
        ArgumentCaptor<RedisPubSubAdapter> listenerCaptor = ArgumentCaptor.forClass(RedisPubSubAdapter.class);
        Mockito.doNothing().when(pubSubConnection).addListener(listenerCaptor.capture());
        try (MockedStatic<RedisEnvironmentVariables> environment = Mockito.mockStatic(RedisEnvironmentVariables.class);
                AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            environment.when(RedisEnvironmentVariables::active).thenReturn(true);
            environment.when(RedisEnvironmentVariables::sentinelActive).thenReturn(true);
            environment.when(RedisEnvironmentVariables::useSentinelEvents).thenReturn(true);
            environment.when(RedisEnvironmentVariables::frontEndCacheCheckDelay).thenReturn(30);
            context.registerBean(RedisClient.class, () -> redisClient);
            context.registerBean(RedisURI.class, () -> redisURI);
            context.registerBean(CacheFrontendManager.class, () -> cacheFrontendManager);
            context.register(SentinelTopologyRefreshManager.class, SentinelSchedulerConfig.class);
            context.refresh();

            Assertions.assertNotNull(context.getBean(SentinelTopologyRefreshManager.class));
            Assertions.assertTrue(context.getBeansOfType(Executor.class).isEmpty());
            listenerCaptor.getValue().message("+switch-master", "+switch-master", "redismaster redis1 6379 redis2 6379");
            Mockito.verify(cacheFrontendManager, Mockito.timeout(1000).times(1)).rebuildCacheFrontend();
        }
        Mockito.verify(pubSubConnection).close();
    }

    private RedisPubSubAdapter<String, String> createSentinelTopologyRefreshManager() {
        ArgumentCaptor<RedisPubSubAdapter> listenerCaptor = ArgumentCaptor.forClass(RedisPubSubAdapter.class);
        Mockito.doNothing().when(pubSubConnection).addListener(listenerCaptor.capture());
        refreshManager = new SentinelTopologyRefreshManager(redisClient, redisURI, cacheFrontendManager);
        Mockito.verify(pubSubCommands, Mockito.times(1)).psubscribe("+switch-master");
        return listenerCaptor.getValue();
    }
}
