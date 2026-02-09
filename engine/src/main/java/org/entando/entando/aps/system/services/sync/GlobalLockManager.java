package org.entando.entando.aps.system.services.sync;

import org.entando.entando.aps.system.services.sync.exception.GlobalLockEntException;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Default no-op implementation. Overridden by the redis-plugin's
 * GlobalLockManagerFactoryBean via XML bean definition when Redis is active.
 */
public class GlobalLockManager implements IGlobalLockManager {

    @Override
    public String tryLock(String key, String property, Duration duration) throws GlobalLockEntException {
        return "";
    }

    @Override
    public String lock(String key, String property, Duration duration, Duration maxWait) throws GlobalLockEntException {
        return "";
    }

    @Override
    public boolean unlock(String key, String token) throws GlobalLockEntException {
        return false;
    }

    @Override
    public Optional<Map<String, String>> verifyLock(String key) throws GlobalLockEntException {
        return Optional.empty();
    }
}
