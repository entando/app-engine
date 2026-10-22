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

import static org.mockito.Mockito.when;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import io.lettuce.core.RedisClient;
import io.lettuce.core.StatefulRedisConnectionImpl;
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
import org.entando.entando.ent.exception.EntRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
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
        StatefulRedisSentinelConnection connection = Mockito.mock(StatefulRedisSentinelConnection.class);
        when(this.lettuceClient.connectSentinel()).thenReturn(connection);
        when(connection.sync()).thenReturn(this.commands);
        List<Map<String, String>> masters = Arrays.asList(master);
        when(commands.masters()).thenReturn(masters);
        Mockito.lenient().when(master.get("ip")).thenReturn("myMaster").thenReturn("mySecondMaster");
    }

    @Test
    void getInitScheduler() throws Exception {
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, cacheManager, cacheFrontendManager);
        Mockito.verify(master, Mockito.times(1)).get("ip");
    }

    @Test
    void runScheduler_1() throws Exception {
        when(master.get("ip")).thenReturn("myMaster");
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, cacheManager, cacheFrontendManager);
        scheduler.run();
        Mockito.verify(master, Mockito.times(2)).get("ip");
        Mockito.verify(cacheFrontendManager, Mockito.never()).rebuildCacheFrontend();
        scheduler.cancel();
    }

    @Test
    void runScheduler_2() throws Exception {
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, cacheManager, cacheFrontendManager);
        scheduler.run();
        Mockito.verify(master, Mockito.times(2)).get("ip");
        Mockito.verify(cacheFrontendManager, Mockito.times(1)).rebuildCacheFrontend();
        Mockito.verify(cacheManager, Mockito.times(1)).updateCacheFrontend(Mockito.any());
        scheduler.cancel();
    }

    @Test
    void runScheduler_3() throws Exception {
        when(commands.masters()).thenReturn(new ArrayList<>());
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, cacheManager, cacheFrontendManager);
        scheduler.run();
        Mockito.verify(master, Mockito.never()).get("ip");
        Mockito.verify(cacheFrontendManager, Mockito.never()).rebuildCacheFrontend();
        scheduler.cancel();
    }

    @Test
    void runScheduler_4() throws Exception {
        when(cacheManager.getCacheNames()).thenReturn(Arrays.asList("cache1", "cache2"));
        LettuceCache cache = Mockito.mock(LettuceCache.class);
        when(cacheManager.getCache(Mockito.anyString())).thenReturn(cache);
        StatefulRedisConnectionImpl connection = Mockito.mock(StatefulRedisConnectionImpl.class);
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, cacheManager, cacheFrontendManager);
        scheduler.run();
        Mockito.verify(master, Mockito.times(2)).get("ip");
        Mockito.verify(cache, Mockito.times(2)).setFrontendCache(Mockito.any());
        scheduler.cancel();
    }

    @Test
    void failRunScheduler() throws Exception {
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, cacheManager, cacheFrontendManager);
        Mockito.doThrow(RuntimeException.class).when(this.cacheFrontendManager).rebuildCacheFrontend();
        Assertions.assertThrows(EntRuntimeException.class, () -> scheduler.run());
        Mockito.verify(master, Mockito.times(2)).get("ip");
        scheduler.cancel();
    }

    @Test
    void failedRebuildShouldNotStopPolling() throws Exception {
        SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, cacheManager, cacheFrontendManager);
        Mockito.doThrow(new RuntimeException("injected")).doReturn(null)
                .when(this.cacheFrontendManager).rebuildCacheFrontend();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            executor.scheduleWithFixedDelay(scheduler, 0, 10, TimeUnit.MILLISECONDS);
            Mockito.verify(cacheFrontendManager, Mockito.timeout(2000).times(2)).rebuildCacheFrontend();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rebuildWithTenantOnThreadShouldRepointEveryExistingCache() throws Exception {
        try (MockedStatic<ClientSideCaching> clientSideCachingMockedStatic = Mockito.mockStatic(
                ClientSideCaching.class)) {
            CacheFrontend<String, Object> previous = Mockito.mock(CacheFrontend.class);
            CacheFrontend<String, Object> next = Mockito.mock(CacheFrontend.class);
            clientSideCachingMockedStatic.when(
                    () -> ClientSideCaching.enable(Mockito.any(), Mockito.any(), Mockito.any()))
                    .thenReturn(previous).thenReturn(next);
            CacheFrontendManager realCacheFrontendManager = new CacheFrontendManager(lettuceClient);
            ITenantManager tenantManager = Mockito.mock(ITenantManager.class);
            when(tenantManager.getCodes()).thenReturn(List.of("tenant1"));
            LettuceCacheManager realCacheManager = (LettuceCacheManager) new BaseRedisCacheConfig().cacheManager(
                    lettuceClient, Mockito.mock(RedisConnectionFactory.class), realCacheFrontendManager,
                    tenantManager);
            realCacheManager.afterPropertiesSet();
            Cache primaryCache = realCacheManager.getCache("Entando_Cache");
            Cache tenantCache = realCacheManager.getCache("tenant1_Entando_Cache");
            SentinelScheduler scheduler = new SentinelScheduler(lettuceClient, realCacheManager,
                    realCacheFrontendManager);

            ApsTenantApplicationUtils.setTenant("tenant1");
            try {
                scheduler.run();
            } finally {
                ApsTenantApplicationUtils.removeTenant();
            }

            primaryCache.get("key");
            tenantCache.get("key");
            Mockito.verify(next).get("Entando_Cache::key");
            Mockito.verify(next).get("tenant1_Entando_Cache::key");
            Mockito.verify(previous, Mockito.never()).get(Mockito.anyString());
        }
    }

}
