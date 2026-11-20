package org.entando.entando.plugins.jpredis.aps.system.redis;

import com.agiletec.aps.system.services.role.IPermissionDAO;
import com.agiletec.aps.system.services.role.IRoleManager;
import com.agiletec.aps.system.services.role.Permission;
import com.agiletec.aps.system.services.role.cache.IPermissionCacheWrapper;
import com.agiletec.aps.system.services.role.cache.PermissionCacheWrapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.support.caching.CacheFrontend;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.entando.entando.plugins.jpredis.utils.RedisTestExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.testcontainers.containers.GenericContainer;

@ExtendWith(MockitoExtension.class)
class LettuceCacheTest {

    @Mock
    private RedisCacheWriter cacheWriter;
    @Mock
    private RedisCacheConfiguration cacheConfig;
    @Mock
    private CacheFrontend<String, Object> cacheFrontend;

    private LettuceCache lettuceCache;

    @BeforeEach
    @SuppressWarnings("deprecation")
    void setUp() {
        lettuceCache = new LettuceCache("CacheName", cacheWriter, cacheConfig, cacheFrontend);
    }
    
    @Test
    void cacheShouldHandleNullValues() {
        Object value = lettuceCache.get("null");
        Assertions.assertNull(value);
    }

    @Test
    void cacheShouldHandleValuesThatAreNeitherListNorMaps() {
        Mockito.when(cacheFrontend.get("CacheName::string")).thenReturn("string");
        Object value = lettuceCache.get("string").get();
        Assertions.assertEquals("string", value);
    }

    @Test
    void cacheShouldReadThroughCurrentSupplierFrontend() {
        CacheFrontend<String, Object> nextFrontend = Mockito.mock(CacheFrontend.class);
        AtomicReference<CacheFrontend<String, Object>> current = new AtomicReference<>(cacheFrontend);
        LettuceCache cache = new LettuceCache("CacheName", cacheWriter, cacheConfig, current::get);
        Mockito.when(cacheFrontend.get("CacheName::key")).thenReturn("value1");
        Mockito.when(nextFrontend.get("CacheName::key")).thenReturn("value2");

        Assertions.assertEquals("value1", cache.get("key").get());
        current.set(nextFrontend);
        Assertions.assertEquals("value2", cache.get("key").get());
    }

    @Nested
    @ExtendWith(RedisTestExtension.class)
    class TwoNodesOnRealRedis {

        private static final String CODES_REDIS_KEY =
                IRoleManager.ROLE_MANAGER_CACHE_NAME + "::" + IPermissionCacheWrapper.PERMISSION_CODES_CACHE_NAME;

        private final List<Node> nodes = new ArrayList<>();
        private int port;

        @BeforeEach
        void setUpPort(GenericContainer redis) {
            port = redis.getMappedPort(6379);
        }

        @AfterEach
        void closeNodes() {
            nodes.forEach(Node::close);
        }

        @Test
        void twoNodesAddingDifferentCodesKeepBothCodes() throws Exception {
            Gate gate = new Gate(CODES_REDIS_KEY);
            Node node1 = node(gate);
            Node node2 = node(null);
            node1.wrapper.initCache(daoWith());

            race(gate,
                    () -> node1.wrapper.addPermission(permission("perm_a")),
                    () -> node2.wrapper.addPermission(permission("perm_b")));

            Assertions.assertEquals(Set.of("perm_a", "perm_b"), new HashSet<>(storedCodes(node1)),
                    "a permission added concurrently on another node was lost from the codes list in Redis");
            assertEventuallyVisible(node2, Set.of("perm_a", "perm_b"));
        }

        @Test
        void twoNodesAddAndDeleteDoNotResurrectTheDeletedCode() throws Exception {
            Gate gate = new Gate(CODES_REDIS_KEY);
            Node node1 = node(gate);
            Node node2 = node(null);
            node1.wrapper.initCache(daoWith(permission("perm_x")));

            race(gate,
                    () -> node1.wrapper.addPermission(permission("perm_a")),
                    () -> node2.wrapper.removePermission("perm_x"));

            Assertions.assertEquals(List.of("perm_a"), storedCodes(node1),
                    "a concurrent add on another node restored a permission deleted meanwhile");
        }

        private Node node(Gate gate) {
            Node node = new Node(port, gate);
            nodes.add(node);
            return node;
        }

        private List<String> storedCodes(Node node) {
            try (RedisConnection connection = node.connectionFactory.getConnection()) {
                byte[] raw = connection.stringCommands().get(CODES_REDIS_KEY.getBytes(StandardCharsets.UTF_8));
                return (List<String>) new JdkSerializationRedisSerializer().deserialize(raw);
            }
        }

        private void assertEventuallyVisible(Node node, Set<String> expected) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 5000;
            Set<String> seen = new HashSet<>(node.wrapper.getPermissionsCodes());
            while (!seen.equals(expected) && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
                seen = new HashSet<>(node.wrapper.getPermissionsCodes());
            }
            Assertions.assertEquals(expected, seen, "the other node does not see the codes list written in Redis");
        }
    }

    private static IPermissionDAO daoWith(Permission... permissions) {
        Map<String, Permission> map = new HashMap<>();
        for (Permission permission : permissions) {
            map.put(permission.getName(), permission);
        }
        IPermissionDAO dao = Mockito.mock(IPermissionDAO.class);
        Mockito.when(dao.loadPermissions()).thenReturn(map);
        return dao;
    }

    private static Permission permission(String name) {
        Permission permission = new Permission();
        permission.setName(name);
        permission.setDescription(name);
        return permission;
    }

    /**
     * Starts writer 1, pauses it right after it reads the gated key, runs writer 2 meanwhile and waits for both.
     */
    private static void race(Gate gate, Runnable first, Runnable second) throws InterruptedException {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread writer1 = new Thread(guarded(first, failure, null), "race-writer-1");
        gate.arm(writer1);
        writer1.start();
        Assertions.assertTrue(gate.firstRead.await(5, TimeUnit.SECONDS), "writer 1 never read the gated key");
        Thread writer2 = new Thread(guarded(second, failure, gate.secondDone), "race-writer-2");
        writer2.start();
        writer1.join(10_000);
        writer2.join(10_000);
        Assertions.assertFalse(writer1.isAlive() || writer2.isAlive(), "a writer did not finish");
        if (failure.get() != null) {
            Assertions.fail("a writer failed", failure.get());
        }
    }

    private static Runnable guarded(Runnable work, AtomicReference<Throwable> failure, CountDownLatch done) {
        return () -> {
            try {
                work.run();
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            } finally {
                if (done != null) {
                    done.countDown();
                }
            }
        };
    }

    /**
     * Pauses the armed thread once, right after it reads the gated key, until writer 2 is done or the timeout expires.
     */
    private static class Gate {

        private static final long GATE_TIMEOUT_MS = 1000;

        private final String gatedKey;
        private final AtomicBoolean fired = new AtomicBoolean();
        private final CountDownLatch firstRead = new CountDownLatch(1);
        private final CountDownLatch secondDone = new CountDownLatch(1);
        private volatile Thread armedThread;

        Gate(String gatedKey) {
            this.gatedKey = gatedKey;
        }

        void arm(Thread thread) {
            this.armedThread = thread;
        }

        void pauseIfGated(String key) {
            if (Thread.currentThread() != this.armedThread || !this.gatedKey.equals(key)
                    || !this.fired.compareAndSet(false, true)) {
                return;
            }
            this.firstRead.countDown();
            try {
                this.secondDone.await(GATE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class GatedCacheFrontend implements CacheFrontend<String, Object> {

        private final CacheFrontend<String, Object> delegate;
        private final Gate gate;

        GatedCacheFrontend(CacheFrontend<String, Object> delegate, Gate gate) {
            this.delegate = delegate;
            this.gate = gate;
        }

        @Override
        public Object get(String key) {
            Object value = this.delegate.get(key);
            this.gate.pauseIfGated(key);
            return value;
        }

        @Override
        public Object get(String key, Callable<Object> valueLoader) {
            return this.delegate.get(key, valueLoader);
        }

        @Override
        public void close() {
            this.delegate.close();
        }
    }

    private static class GatedCacheWriter extends DefaultLettuceCacheWriter {

        GatedCacheWriter(RedisConnectionFactory connectionFactory) {
            super(connectionFactory);
        }
    }

    /**
     * One application node: its own Redis client, connection factory, client-side cache and cache manager.
     */
    private static class Node {

        private final RedisClient redisClient;
        private final LettuceConnectionFactory connectionFactory;
        private final CacheFrontendManager frontendManager;
        private final PermissionCacheWrapper wrapper;

        Node(int port, Gate gate) {
            this.redisClient = RedisClient.create("redis://localhost:" + port);
            this.connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", port));
            this.connectionFactory.afterPropertiesSet();
            this.frontendManager = new CacheFrontendManager(this.redisClient);
            Supplier<CacheFrontend<String, Object>> frontend = (gate == null)
                    ? this.frontendManager::getCacheFrontend
                    : () -> new GatedCacheFrontend(this.frontendManager.getCacheFrontend(), gate);
            RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ZERO)
                    .serializeValuesWith(SerializationPair.fromSerializer(new JdkSerializationRedisSerializer()));
            LettuceCacheManager cacheManager = LettuceCacheManager.builder(new GatedCacheWriter(this.connectionFactory))
                    .cacheDefaults(config).cacheFrontendSupplier(frontend).build();
            cacheManager.afterPropertiesSet();
            this.wrapper = new PermissionCacheWrapper();
            this.wrapper.setSpringCacheManager(cacheManager);
        }

        void close() {
            this.frontendManager.getCacheFrontend().close();
            this.connectionFactory.destroy();
            this.redisClient.shutdown();
        }
    }
}
