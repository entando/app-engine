/*
 * Copyright 2022-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpredis.aps.system.redis;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;
import io.lettuce.core.support.caching.CacheFrontend;
import io.lettuce.core.support.caching.ClientSideCaching;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * @author E.Santoboni
 */
@ExtendWith(MockitoExtension.class)
class SentinelSchedulerTest {

    @Mock
    private Map<String, String> master;

    @Mock
    private RedisSentinelCommands commands;

    @Mock
    private RedisClient lettuceClient;

    @Mock
    private LettuceCacheManager cacheManager;

    @Mock
    private CacheFrontendManager cacheFrontendManager;

    @BeforeEach
    void setUp() throws Exception {
        StatefulRedisSentinelConnection connection = mock(StatefulRedisSentinelConnection.class);
        when(this.lettuceClient.connectSentinel()).thenReturn(connection);
        when(connection.sync()).thenReturn(this.commands);
        List<Map<String, String>> masters = Arrays.asList(master);
        when(commands.masters()).thenReturn(masters);
        lenient().when(master.get("ip")).thenReturn("myMaster").thenReturn("mySecondMaster");
    }

    @Test
    void getInitScheduler() throws Exception {
        Assertions.assertNotNull(new SentinelScheduler(lettuceClient, cacheFrontendManager));
        verify(master, times(1)).get("ip");
    }

    @Test
    void runScheduler_1() throws Exception {
        when(master.get("ip")).thenReturn("myMaster");
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, cacheFrontendManager);
        scheduler.run();
        verify(master, times(2)).get("ip");
        verify(cacheFrontendManager, never()).rebuildCacheFrontend();
        scheduler.cancel();
    }

    @Test
    void runScheduler_2() throws Exception {
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, cacheFrontendManager);
        scheduler.run();
        verify(master, times(2)).get("ip");
        verify(cacheFrontendManager, times(1)).rebuildCacheFrontend();
        scheduler.cancel();
    }

    @Test
    void runScheduler_3() throws Exception {
        when(commands.masters()).thenReturn(new ArrayList<>());
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, cacheFrontendManager);
        scheduler.run();
        verify(master, never()).get("ip");
        verify(cacheFrontendManager, never()).rebuildCacheFrontend();
        scheduler.cancel();
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedConstructorShouldRebuildThroughCacheFrontendManager() {
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, cacheManager, cacheFrontendManager);
        scheduler.run();
        verify(cacheFrontendManager, times(1)).rebuildCacheFrontend();
        verifyNoInteractions(cacheManager);
        scheduler.cancel();
    }

    @Test
    void failRunScheduler() {
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, cacheFrontendManager);
        doThrow(RuntimeException.class).doReturn(null).when(this.cacheFrontendManager).rebuildCacheFrontend();
        Assertions.assertDoesNotThrow(scheduler::run);
        scheduler.run();
        verify(master, times(3)).get("ip");
        verify(cacheFrontendManager, times(2)).rebuildCacheFrontend();
        scheduler.cancel();
    }

    @Test
    void failedRebuildShouldNotStopPolling() throws Exception {
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, cacheFrontendManager);
        doThrow(new RuntimeException("injected")).doReturn(null)
                .when(this.cacheFrontendManager).rebuildCacheFrontend();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            executor.scheduleWithFixedDelay(scheduler, 0, 10, TimeUnit.MILLISECONDS);
            verify(cacheFrontendManager, timeout(2000).times(2)).rebuildCacheFrontend();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rebuildWithTenantOnThreadShouldRepointEveryExistingCache() throws Exception {
        try (MockedStatic<ClientSideCaching> clientSideCachingMockedStatic = mockStatic(
                ClientSideCaching.class)) {
            CacheFrontend<String, Object> previous = mock(CacheFrontend.class);
            CacheFrontend<String, Object> next = mock(CacheFrontend.class);
            clientSideCachingMockedStatic.when(
                    () -> ClientSideCaching.enable(any(), any(), any()))
                    .thenReturn(previous).thenReturn(next);
            StatefulRedisConnection<String, Object> connection = mock(StatefulRedisConnection.class);
            when(lettuceClient.connect(any(RedisCodec.class))).thenReturn(connection);
            CacheFrontendManager realCacheFrontendManager = new CacheFrontendManager(lettuceClient);
            ITenantManager tenantManager = mock(ITenantManager.class);
            when(tenantManager.getCodes()).thenReturn(List.of("tenant1"));
            LettuceCacheManager realCacheManager = (LettuceCacheManager) new BaseRedisCacheConfig().cacheManager(
                    lettuceClient, mock(RedisConnectionFactory.class), realCacheFrontendManager,
                    tenantManager);
            realCacheManager.afterPropertiesSet();
            Cache primaryCache = realCacheManager.getCache("Entando_Cache");
            Cache tenantCache = realCacheManager.getCache("tenant1_Entando_Cache");
            SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, realCacheFrontendManager);

            ApsTenantApplicationUtils.setTenant("tenant1");
            try {
                scheduler.run();
            } finally {
                ApsTenantApplicationUtils.removeTenant();
            }

            primaryCache.get("key");
            tenantCache.get("key");
            verify(next).get("Entando_Cache::key");
            verify(next).get("tenant1_Entando_Cache::key");
            verify(previous, never()).get(anyString());
        }
    }

}
