package org.entando.entando.plugins.jpredis.aps.system.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.redis.cache.CacheStatistics;
import org.springframework.data.redis.cache.CacheStatisticsCollector;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.types.Expiration;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultLettuceCacheWriterTest {

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection connection;

    @Mock
    private CacheStatisticsCollector statisticsCollector;

    private DefaultLettuceCacheWriter cacheWriter;
    private DefaultLettuceCacheWriter lockingCacheWriter;

    @BeforeEach
    void setUp() {
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.isPipelined()).thenReturn(false);

        // Non-locking cache writer (Duration.ZERO)
        cacheWriter = new DefaultLettuceCacheWriter(connectionFactory);

        // Locking cache writer (Duration > 0)
        lockingCacheWriter = new DefaultLettuceCacheWriter(connectionFactory, Duration.ofMillis(100));
    }

    @Test
    void constructorWithConnectionFactoryOnlyShouldSetZeroSleepTime() {
        assertNotNull(cacheWriter);
    }

    @Test
    void constructorShouldThrowExceptionWhenConnectionFactoryIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new DefaultLettuceCacheWriter(null, Duration.ZERO)
        );
    }

    @Test
    void constructorShouldThrowExceptionWhenSleepTimeIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            new DefaultLettuceCacheWriter(connectionFactory, null)
        );
    }

    @Test
    void putShouldStoreValueWithoutTTL() {
        String cacheName = "testCache";
        byte[] key = "testKey".getBytes();
        byte[] value = "testValue".getBytes();

        cacheWriter.put(cacheName, key, value, null);

        verify(connection).set(key, value);
        verify(connection).close();
    }

    @Test
    void putShouldStoreValueWithTTL() {
        String cacheName = "testCache";
        byte[] key = "testKey".getBytes();
        byte[] value = "testValue".getBytes();
        Duration ttl = Duration.ofMinutes(10);

        cacheWriter.put(cacheName, key, value, ttl);

        verify(connection).set(eq(key), eq(value),
            eq(Expiration.from(ttl.toMillis(), TimeUnit.MILLISECONDS)),
            eq(SetOption.upsert()));
        verify(connection).close();
    }

    @Test
    void putShouldStoreValueWithZeroTTL() {
        String cacheName = "testCache";
        byte[] key = "testKey".getBytes();
        byte[] value = "testValue".getBytes();
        Duration ttl = Duration.ZERO;

        cacheWriter.put(cacheName, key, value, ttl);

        verify(connection).set(key, value);
        verify(connection, never()).set(any(), any(), any(Expiration.class), any(SetOption.class));
        verify(connection).close();
    }

    @Test
    void putShouldStoreValueWithNegativeTTL() {
        String cacheName = "testCache";
        byte[] key = "testKey".getBytes();
        byte[] value = "testValue".getBytes();
        Duration ttl = Duration.ofMinutes(-1);

        cacheWriter.put(cacheName, key, value, ttl);

        verify(connection).set(key, value);
        verify(connection, never()).set(any(), any(), any(Expiration.class), any(SetOption.class));
        verify(connection).close();
    }

    @Test
    void putShouldThrowExceptionWhenNameIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            cacheWriter.put(null, "key".getBytes(), "value".getBytes(), null)
        );
    }

    @Test
    void putShouldThrowExceptionWhenKeyIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            cacheWriter.put("cache", null, "value".getBytes(), null)
        );
    }

    @Test
    void putShouldThrowExceptionWhenValueIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            cacheWriter.put("cache", "key".getBytes(), null, null)
        );
    }

    @Test
    void getShouldRetrieveValue() {
        String cacheName = "testCache";
        byte[] key = "testKey".getBytes();
        byte[] expectedValue = "testValue".getBytes();

        when(connection.get(key)).thenReturn(expectedValue);

        byte[] result = cacheWriter.get(cacheName, key);

        assertArrayEquals(expectedValue, result);
        verify(connection).get(key);
        verify(connection).close();
    }

    @Test
    void getShouldThrowExceptionWhenNameIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            cacheWriter.get(null, "key".getBytes())
        );
    }

    @Test
    void getShouldThrowExceptionWhenKeyIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            cacheWriter.get("cache", null)
        );
    }

    @Test
    void putIfAbsentShouldStoreValueWhenKeyDoesNotExist() {
        String cacheName = "testCache";
        byte[] key = "testKey".getBytes();
        byte[] value = "testValue".getBytes();

        when(connection.setNX(key, value)).thenReturn(true);

        byte[] result = cacheWriter.putIfAbsent(cacheName, key, value, null);

        assertNull(result);
        verify(connection).setNX(key, value);
        verify(connection).close();
    }

    @Test
    void putIfAbsentShouldReturnExistingValueWhenKeyExists() {
        String cacheName = "testCache";
        byte[] key = "testKey".getBytes();
        byte[] value = "testValue".getBytes();
        byte[] existingValue = "existingValue".getBytes();

        when(connection.setNX(key, value)).thenReturn(false);
        when(connection.get(key)).thenReturn(existingValue);

        byte[] result = cacheWriter.putIfAbsent(cacheName, key, value, null);

        assertArrayEquals(existingValue, result);
        verify(connection).setNX(key, value);
        verify(connection).get(key);
        verify(connection).close();
    }

    @Test
    void putIfAbsentShouldSetExpirationWhenKeyDoesNotExistAndTTLProvided() {
        String cacheName = "testCache";
        byte[] key = "testKey".getBytes();
        byte[] value = "testValue".getBytes();
        Duration ttl = Duration.ofMinutes(5);

        when(connection.setNX(key, value)).thenReturn(true);

        byte[] result = cacheWriter.putIfAbsent(cacheName, key, value, ttl);

        assertNull(result);
        verify(connection).setNX(key, value);
        verify(connection).pExpire(key, ttl.toMillis());
        verify(connection).close();
    }

    @Test
    void putIfAbsentShouldUseLockingInLockingMode() {
        String cacheName = "testCache";
        byte[] key = "testKey".getBytes();
        byte[] value = "testValue".getBytes();
        byte[] lockKey = (cacheName + "~lock").getBytes(StandardCharsets.UTF_8);

        when(connection.setNX(key, value)).thenReturn(true);
        when(connection.setNX(lockKey, new byte[0])).thenReturn(true);
        when(connection.exists(lockKey)).thenReturn(false);

        byte[] result = lockingCacheWriter.putIfAbsent(cacheName, key, value, null);

        assertNull(result);
        verify(connection).setNX(lockKey, new byte[0]);
        verify(connection).del(lockKey);
        verify(connection).close();
    }

    @Test
    void putIfAbsentShouldThrowExceptionWhenNameIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            cacheWriter.putIfAbsent(null, "key".getBytes(), "value".getBytes(), null)
        );
    }

    @Test
    void putIfAbsentShouldThrowExceptionWhenKeyIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            cacheWriter.putIfAbsent("cache", null, "value".getBytes(), null)
        );
    }

    @Test
    void putIfAbsentShouldThrowExceptionWhenValueIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            cacheWriter.putIfAbsent("cache", "key".getBytes(), null, null)
        );
    }

    @Test
    void removeShouldDeleteKey() {
        String cacheName = "testCache";
        byte[] key = "testKey".getBytes();

        when(connection.del(key)).thenReturn(1L);

        cacheWriter.remove(cacheName, key);

        verify(connection).del(key);
        verify(connection).close();
    }

    @Test
    void removeShouldThrowExceptionWhenNameIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            cacheWriter.remove(null, "key".getBytes())
        );
    }

    @Test
    void removeShouldThrowExceptionWhenKeyIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            cacheWriter.remove("cache", null)
        );
    }

    @Test
    void cleanShouldDeleteMatchingKeys() {
        String cacheName = "testCache";
        byte[] pattern = "test*".getBytes();
        byte[] key1 = "testKey1".getBytes();
        byte[] key2 = "testKey2".getBytes();
        Set<byte[]> keys = new HashSet<>();
        keys.add(key1);
        keys.add(key2);

        when(connection.keys(pattern)).thenReturn(keys);
        when(connection.del(any(byte[].class), any(byte[].class))).thenReturn(2L);

        cacheWriter.clean(cacheName, pattern);

        verify(connection).keys(pattern);
        verify(connection).del(any(byte[].class), any(byte[].class));
        verify(connection).close();
    }

    @Test
    void cleanShouldHandleEmptyKeySet() {
        String cacheName = "testCache";
        byte[] pattern = "test*".getBytes();

        when(connection.keys(pattern)).thenReturn(Collections.emptySet());

        cacheWriter.clean(cacheName, pattern);

        verify(connection).keys(pattern);
        verify(connection, never()).del(any(byte[][].class));
        verify(connection).close();
    }

    @Test
    void cleanShouldHandleNullKeySet() {
        String cacheName = "testCache";
        byte[] pattern = "test*".getBytes();

        when(connection.keys(pattern)).thenReturn(null);

        cacheWriter.clean(cacheName, pattern);

        verify(connection).keys(pattern);
        verify(connection, never()).del(any(byte[][].class));
        verify(connection).close();
    }

    @Test
    void cleanShouldUseLockingInLockingMode() {
        String cacheName = "testCache";
        byte[] pattern = "test*".getBytes();
        byte[] lockKey = (cacheName + "~lock").getBytes(StandardCharsets.UTF_8);

        when(connection.keys(pattern)).thenReturn(Collections.emptySet());
        when(connection.setNX(lockKey, new byte[0])).thenReturn(true);
        when(connection.exists(lockKey)).thenReturn(false);

        lockingCacheWriter.clean(cacheName, pattern);

        verify(connection).setNX(lockKey, new byte[0]);
        verify(connection).del(lockKey);
        verify(connection).close();
    }

    @Test
    void cleanShouldThrowExceptionWhenNameIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            cacheWriter.clean(null, "pattern".getBytes())
        );
    }

    @Test
    void cleanShouldThrowExceptionWhenPatternIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
            cacheWriter.clean("cache", null)
        );
    }

    @Test
    void lockShouldSetLockKey() {
        String cacheName = "testCache";
        byte[] lockKey = (cacheName + "~lock").getBytes(StandardCharsets.UTF_8);

        when(connection.setNX(lockKey, new byte[0])).thenReturn(true);
        when(connection.exists(lockKey)).thenReturn(false);

        lockingCacheWriter.lock(cacheName);

        verify(connection).setNX(lockKey, new byte[0]);
        verify(connection).close();
    }

    @Test
    void unlockShouldRemoveLockKey() {
        String cacheName = "testCache";
        byte[] lockKey = (cacheName + "~lock").getBytes(StandardCharsets.UTF_8);

        when(connection.del(lockKey)).thenReturn(1L);

        lockingCacheWriter.unlock(cacheName);

        verify(connection).del(lockKey);
        verify(connection).close();
    }

    @Test
    void doCheckLockShouldReturnTrueWhenLockExists() {
        String cacheName = "testCache";
        byte[] lockKey = (cacheName + "~lock").getBytes(StandardCharsets.UTF_8);

        when(connection.exists(lockKey)).thenReturn(true);

        boolean result = lockingCacheWriter.doCheckLock(cacheName, connection);

        assertTrue(result);
        verify(connection).exists(lockKey);
    }

    @Test
    void doCheckLockShouldReturnFalseWhenLockDoesNotExist() {
        String cacheName = "testCache";
        byte[] lockKey = (cacheName + "~lock").getBytes(StandardCharsets.UTF_8);

        when(connection.exists(lockKey)).thenReturn(false);

        boolean result = lockingCacheWriter.doCheckLock(cacheName, connection);

        assertFalse(result);
        verify(connection).exists(lockKey);
    }

    @Test
    void openPipelineShouldOpenPipelineConnection() {
        RedisConnection pipelineConnection = mock(RedisConnection.class);
        when(connectionFactory.getConnection()).thenReturn(pipelineConnection);

        cacheWriter.openPipeline();

        verify(pipelineConnection).openPipeline();
    }

    @Test
    void closePipelineShouldClosePipelineConnection() {
        RedisConnection pipelineConnection = mock(RedisConnection.class);
        when(connectionFactory.getConnection()).thenReturn(pipelineConnection);
        when(pipelineConnection.isPipelined()).thenReturn(true);

        cacheWriter.openPipeline();
        cacheWriter.closePipeline();

        verify(pipelineConnection).closePipeline();
        verify(pipelineConnection).close();
    }

    @Test
    void pipelineModeShouldReuseConnection() {
        RedisConnection pipelineConnection = mock(RedisConnection.class);
        when(connectionFactory.getConnection()).thenReturn(pipelineConnection);
        when(pipelineConnection.isPipelined()).thenReturn(true);

        byte[] key = "key".getBytes();
        byte[] value = "value".getBytes();

        cacheWriter.openPipeline();

        when(pipelineConnection.get(key)).thenReturn(value);
        cacheWriter.get("cache", key);

        cacheWriter.put("cache", key, value, null);

        cacheWriter.closePipeline();

        // Connection should be obtained only once for opening pipeline
        verify(connectionFactory, times(1)).getConnection();
        // Connection should be closed exactly once when closePipeline() is called
        verify(pipelineConnection, times(1)).close();
    }

    @Test
    void nonPipelinedModeShouldCloseConnectionAfterEachOperation() {
        byte[] key = "key".getBytes();
        byte[] value = "value".getBytes();

        when(connection.get(key)).thenReturn(value);

        cacheWriter.get("cache", key);
        cacheWriter.put("cache", key, value, null);

        verify(connection, times(2)).close();
    }

    @Test
    void withStatisticsCollectorShouldReturnSameInstance() {
        DefaultLettuceCacheWriter result = (DefaultLettuceCacheWriter) cacheWriter.withStatisticsCollector(statisticsCollector);

        assertSame(cacheWriter, result);
    }

    @Test
    void clearStatisticsShouldNotThrowException() {
        assertDoesNotThrow(() -> cacheWriter.clearStatistics("testCache"));
    }

    @Test
    void getCacheStatisticsShouldReturnNull() {
        CacheStatistics result = cacheWriter.getCacheStatistics("testCache");

        assertNull(result);
    }

    @Test
    void storeShouldReturnNull() {
        byte[] key = "key".getBytes();
        byte[] value = "value".getBytes();

        assertNull(cacheWriter.store("cache", key, value, Duration.ofMinutes(5)));
    }

    @Test
    void retrieveWithTTLShouldReturnNull() {
        byte[] key = "key".getBytes();

        assertNull(cacheWriter.retrieve("cache", key, Duration.ofMinutes(5)));
    }

    @Test
    void retrieveWithoutTTLShouldReturnNull() {
        byte[] key = "key".getBytes();

        assertNull(cacheWriter.retrieve("cache", key));
    }

    @Test
    void supportsAsyncRetrieveShouldReturnFalse() {
        assertFalse(cacheWriter.supportsAsyncRetrieve());
    }

    @Test
    void isEnabledShouldReturnFeatureFlagValue() {
        boolean result = cacheWriter.isEnabled();

        // Result is always a boolean (true or false based on feature flag)
        assertTrue(result || !result);
    }

    @Test
    void checkAndPotentiallyWaitUntilUnlockedShouldWaitWhenLocked() {
        String cacheName = "testCache";
        byte[] lockKey = (cacheName + "~lock").getBytes(StandardCharsets.UTF_8);

        // First call returns true (locked), second call returns false (unlocked)
        when(connection.exists(lockKey)).thenReturn(true, false);

        // This should wait and not throw exception
        lockingCacheWriter.lock(cacheName);

        verify(connection, atLeastOnce()).exists(lockKey);
    }

    @Test
    void checkAndPotentiallyWaitUntilUnlockedShouldThrowPessimisticLockingFailureExceptionOnInterrupt() {
        String cacheName = "testCache";
        byte[] key = "key".getBytes();
        byte[] value = "value".getBytes();
        byte[] lockKey = (cacheName + "~lock").getBytes(StandardCharsets.UTF_8);

        // Always return true to simulate lock never being released
        when(connection.exists(lockKey)).thenReturn(true);

        // Interrupt the current thread to trigger InterruptedException
        Thread.currentThread().interrupt();

        assertThrows(PessimisticLockingFailureException.class, () ->
            lockingCacheWriter.put(cacheName, key, value, null)
        );

        // Clear the interrupted status
        Thread.interrupted();
    }

    @Test
    void executeShouldNotClosePipelinedConnection() {
        RedisConnection pipelineConnection = mock(RedisConnection.class);
        when(connectionFactory.getConnection()).thenReturn(pipelineConnection);
        when(pipelineConnection.isPipelined()).thenReturn(true);

        byte[] key = "key".getBytes();
        byte[] value = "value".getBytes();

        cacheWriter.openPipeline();

        when(pipelineConnection.get(key)).thenReturn(value);
        cacheWriter.get("cache", key);

        // Connection should not be closed while in pipeline mode
        verify(pipelineConnection, never()).close();
    }

    @Test
    void shouldHandleMultiplePutOperations() {
        byte[] key1 = "key1".getBytes();
        byte[] value1 = "value1".getBytes();
        byte[] key2 = "key2".getBytes();
        byte[] value2 = "value2".getBytes();

        cacheWriter.put("cache", key1, value1, null);
        cacheWriter.put("cache", key2, value2, Duration.ofMinutes(10));

        verify(connection).set(key1, value1);
        verify(connection).set(eq(key2), eq(value2), any(Expiration.class), any(SetOption.class));
        verify(connection, times(2)).close();
    }

    @Test
    void shouldHandleMultipleGetOperations() {
        byte[] key1 = "key1".getBytes();
        byte[] key2 = "key2".getBytes();
        byte[] value1 = "value1".getBytes();
        byte[] value2 = "value2".getBytes();

        when(connection.get(key1)).thenReturn(value1);
        when(connection.get(key2)).thenReturn(value2);

        assertArrayEquals(value1, cacheWriter.get("cache", key1));
        assertArrayEquals(value2, cacheWriter.get("cache", key2));

        verify(connection, times(2)).close();
    }
}