package org.entando.entando.aps.system.services.sync;

import org.entando.entando.aps.system.services.sync.exception.GlobalLockEntException;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public interface IGlobalLockManager {

    String BEAN_ID = "GlobalLockManager";

    /**
     * Lock property: creation data
     */
    String LOCk_PROP_CREATED_AT = "createdAt";
    /**
     * Lock property:: general purpose data
     */
    String LOCK_PROP_DATA = "gpData";
    /**
     * UUID of the lock, returned when the lock is created successfully
     */
    String LOCK_PROP_TOKEN = "token";

    String tryLock(String key, String property, Duration duration) throws GlobalLockEntException;

    String lock(String key, String property, Duration duration, Duration maxWait) throws GlobalLockEntException;

    boolean unlock(String key, String token) throws GlobalLockEntException;

    Optional<Map<String, String>> verifyLock(String key) throws GlobalLockEntException;

}
