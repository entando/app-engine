package org.entando.entando.aps.system.services.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.entando.entando.aps.system.services.sync.exception.GlobalLockEntException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GlobalLockManagerTest {

    private GlobalLockManager lockManager;

    @BeforeEach
    void setUp() {
        lockManager = new GlobalLockManager();
    }

    @Test
    void tryLock_shouldReturnEmptyString() throws GlobalLockEntException {
        String result = lockManager.tryLock("testKey", "testProp", Duration.ofMinutes(1));
        assertEquals("", result);
    }

    @Test
    void lock_shouldReturnEmptyString() throws GlobalLockEntException {
        String result = lockManager.lock("testKey", "testProp", Duration.ofMinutes(1), Duration.ofSeconds(5));
        assertEquals("", result);
    }

    @Test
    void unlock_shouldReturnFalse() throws GlobalLockEntException {
        boolean result = lockManager.unlock("testKey", "testToken");
        assertFalse(result);
    }

    @Test
    void verifyLock_shouldReturnEmpty() throws GlobalLockEntException {
        Optional<Map<String, String>> result = lockManager.verifyLock("testKey");
        assertTrue(result.isEmpty());
    }

    @Test
    void tryLock_withNullKey_shouldReturnEmptyString() throws GlobalLockEntException {
        String result = lockManager.tryLock(null, "prop", Duration.ofMinutes(1));
        assertEquals("", result);
    }

    @Test
    void lock_withNullKey_shouldReturnEmptyString() throws GlobalLockEntException {
        String result = lockManager.lock(null, null, Duration.ofMinutes(1), Duration.ofSeconds(1));
        assertEquals("", result);
    }

    @Test
    void unlock_withNullArgs_shouldReturnFalse() throws GlobalLockEntException {
        assertFalse(lockManager.unlock(null, null));
    }

    @Test
    void verifyLock_withNullKey_shouldReturnEmpty() throws GlobalLockEntException {
        assertTrue(lockManager.verifyLock(null).isEmpty());
    }
}