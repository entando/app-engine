package org.entando.entando.plugins.jpredis.aps.system.redis.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.entando.entando.aps.system.services.sync.IGlobalLockManager;
import org.entando.entando.aps.system.services.sync.exception.GlobalLockEntException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GlobalLockManagerTest {

    @Mock
    private RedisClient redisClient;
    @Mock
    private StatefulRedisConnection<String, String> connection;
    @Mock
    private RedisCommands<String, String> commands;

    private GlobalLockManager lockManager;

    @BeforeEach
    void setUp() {
        when(redisClient.connect()).thenReturn(connection);
        lockManager = new GlobalLockManager(redisClient);
    }

    private void stubRedisConnection() {
        when(connection.sync()).thenReturn(commands);
    }

    @Test
    void tryLock_shouldEvalLuaScriptAndReturnToken() throws GlobalLockEntException {
        stubRedisConnection();
        when(commands.eval(any(String.class), eq(ScriptOutputType.VALUE), any(String[].class),
                any())).thenReturn("generated-token");

        String result = lockManager.tryLock("myKey", "myProp", Duration.ofMinutes(5));

        assertEquals("generated-token", result);
        verify(commands).eval(any(String.class), eq(ScriptOutputType.VALUE),
                any(String[].class), any());
    }

    @Test
    void tryLock_withBlankKey_shouldReturnNull() throws GlobalLockEntException {
        assertNull(lockManager.tryLock("", "prop", Duration.ofMinutes(1)));
        assertNull(lockManager.tryLock(null, "prop", Duration.ofMinutes(1)));
    }

    @Test
    void tryLock_shouldPrefixKeyWithGlobalLockNamespace() throws GlobalLockEntException {
        stubRedisConnection();
        when(commands.eval(any(String.class), eq(ScriptOutputType.VALUE), any(String[].class),
                any())).thenReturn("token");

        lockManager.tryLock("myKey", "prop", Duration.ofMinutes(1));

        verify(commands).eval(any(String.class), eq(ScriptOutputType.VALUE),
                eq(new String[]{GlobalLockManager.ENTANDO_GLOBAL_LOCK + "myKey"}),
                any());
    }

    @Test
    void tryLock_onRedisException_shouldThrowGlobalLockEntException() {
        stubRedisConnection();
        when(commands.eval(any(String.class), eq(ScriptOutputType.VALUE), any(String[].class),
                any())).thenThrow(new RedisException("connection lost"));

        assertThrows(GlobalLockEntException.class,
                () -> lockManager.tryLock("key", "prop", Duration.ofMinutes(1)));
    }

    @Test
    void lock_shouldReturnTokenOnFirstSuccessfulTryLock() throws GlobalLockEntException {
        stubRedisConnection();
        when(commands.eval(any(String.class), eq(ScriptOutputType.VALUE), any(String[].class),
                any())).thenReturn("token-abc");

        String result = lockManager.lock("key", "prop", Duration.ofMinutes(5), Duration.ofSeconds(1));

        assertEquals("token-abc", result);
    }

    @Test
    void lock_shouldReturnNullWhenTimeoutExceeded() throws GlobalLockEntException {
        stubRedisConnection();
        when(commands.eval(any(String.class), eq(ScriptOutputType.VALUE), any(String[].class),
                any())).thenReturn(null);

        String result = lockManager.lock("key", "prop", Duration.ofMinutes(5), Duration.ofMillis(50));

        assertNull(result);
    }

    @Test
    void unlock_shouldEvalUnlockLuaScript() throws GlobalLockEntException {
        stubRedisConnection();
        when(commands.eval(any(String.class), eq(ScriptOutputType.INTEGER), any(String[].class),
                any())).thenReturn(1L);

        boolean result = lockManager.unlock("myKey", "myToken");

        assertTrue(result);
        verify(commands).eval(any(String.class), eq(ScriptOutputType.INTEGER),
                eq(new String[]{GlobalLockManager.ENTANDO_GLOBAL_LOCK + "myKey"}),
                eq("myToken"));
    }

    @Test
    void unlock_shouldReturnFalseWhenRedisReturnsZero() throws GlobalLockEntException {
        stubRedisConnection();
        when(commands.eval(any(String.class), eq(ScriptOutputType.INTEGER), any(String[].class),
                any())).thenReturn(0L);

        assertFalse(lockManager.unlock("key", "wrong-token"));
    }

    @Test
    void unlock_shouldReturnFalseWhenRedisReturnsNull() throws GlobalLockEntException {
        stubRedisConnection();
        when(commands.eval(any(String.class), eq(ScriptOutputType.INTEGER), any(String[].class),
                any())).thenReturn(null);

        assertFalse(lockManager.unlock("key", "token"));
    }

    @Test
    void unlock_withBlankKey_shouldReturnFalse() throws GlobalLockEntException {
        assertFalse(lockManager.unlock("", "token"));
        assertFalse(lockManager.unlock(null, "token"));
    }

    @Test
    void unlock_withBlankToken_shouldReturnFalse() throws GlobalLockEntException {
        assertFalse(lockManager.unlock("key", ""));
        assertFalse(lockManager.unlock("key", null));
    }

    @Test
    void unlock_onRedisException_shouldThrowGlobalLockEntException() {
        stubRedisConnection();
        when(commands.eval(any(String.class), eq(ScriptOutputType.INTEGER), any(String[].class),
                any())).thenThrow(new RedisException("connection lost"));

        assertThrows(GlobalLockEntException.class,
                () -> lockManager.unlock("key", "token"));
    }

    @Test
    void verifyLock_shouldReturnFieldsWithoutToken() throws GlobalLockEntException {
        stubRedisConnection();
        Map<String, String> fields = new HashMap<>();
        fields.put(IGlobalLockManager.LOCK_PROP_TOKEN, "secret-token");
        fields.put(IGlobalLockManager.LOCk_PROP_CREATED_AT, "2026-01-01T00:00:00Z");
        fields.put(IGlobalLockManager.LOCK_PROP_DATA, "system");

        when(commands.hgetall(GlobalLockManager.ENTANDO_GLOBAL_LOCK + "myKey")).thenReturn(fields);

        Optional<Map<String, String>> result = lockManager.verifyLock("myKey");

        assertTrue(result.isPresent());
        assertFalse(result.get().containsKey(IGlobalLockManager.LOCK_PROP_TOKEN));
        assertEquals("2026-01-01T00:00:00Z", result.get().get(IGlobalLockManager.LOCk_PROP_CREATED_AT));
        assertEquals("system", result.get().get(IGlobalLockManager.LOCK_PROP_DATA));
    }

    @Test
    void verifyLock_shouldReturnEmptyWhenNoLockExists() throws GlobalLockEntException {
        stubRedisConnection();
        when(commands.hgetall(any())).thenReturn(new HashMap<>());

        Optional<Map<String, String>> result = lockManager.verifyLock("noSuchKey");

        assertTrue(result.isEmpty());
    }

    @Test
    void verifyLock_shouldReturnEmptyWhenNullFields() throws GlobalLockEntException {
        stubRedisConnection();
        when(commands.hgetall(any())).thenReturn(null);

        Optional<Map<String, String>> result = lockManager.verifyLock("key");

        assertTrue(result.isEmpty());
    }

    @Test
    void verifyLock_withBlankKey_shouldReturnEmpty() throws GlobalLockEntException {
        assertTrue(lockManager.verifyLock("").isEmpty());
        assertTrue(lockManager.verifyLock(null).isEmpty());
    }

    @Test
    void verifyLock_onRedisException_shouldThrowGlobalLockEntException() {
        stubRedisConnection();
        when(commands.hgetall(any())).thenThrow(new RedisException("timeout"));

        assertThrows(GlobalLockEntException.class,
                () -> lockManager.verifyLock("key"));
    }
}