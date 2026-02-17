package com.agiletec.aps.system.services.keygenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import com.agiletec.aps.system.services.keygenerator.cache.IKeyGeneratorManagerCacheWrapper;
import org.entando.entando.aps.system.services.sync.IGlobalLockManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeyGeneratorManagerTest {

    @Mock
    private IKeyGeneratorDAO keyGeneratorDAO;
    @Mock
    private IKeyGeneratorManagerCacheWrapper cacheWrapper;
    @Mock
    private IGlobalLockManager globalLockManager;

    private KeyGeneratorManager manager;
    private boolean originalLockEnabled;

    @BeforeEach
    void setUp() throws Exception {
        originalLockEnabled = getStaticField();
        manager = new KeyGeneratorManager();
        manager.setKeyGeneratorDAO(keyGeneratorDAO);
        manager.setCacheWrapper(cacheWrapper);
        manager.setGlobalLockManager(globalLockManager);
    }

    @AfterEach
    void tearDown() throws Exception {
        setStaticField(originalLockEnabled);
    }

    @Test
    void getUniqueKeyCurrentValue_lockDisabled_shouldUseDaoDirectly() throws Exception {
        setStaticField(false);
        when(keyGeneratorDAO.getNextUniqueKey()).thenReturn(42);

        int result = manager.getUniqueKeyCurrentValue();

        assertEquals(42, result);
        verify(keyGeneratorDAO).getNextUniqueKey();
        verify(cacheWrapper, never()).getAndIncrementUniqueKeyCurrentValue(any());
    }

    @Test
    void getUniqueKeyCurrentValue_lockEnabled_shouldUseCacheWithLock() throws Exception {
        setStaticField(true);
        when(globalLockManager.lock(anyString(), anyString(), any(), any())).thenReturn("token123");
        when(cacheWrapper.getAndIncrementUniqueKeyCurrentValue(keyGeneratorDAO)).thenReturn(99);

        int result = manager.getUniqueKeyCurrentValue();

        assertEquals(99, result);
        verify(globalLockManager).lock(anyString(), anyString(), any(), any());
        verify(cacheWrapper).getAndIncrementUniqueKeyCurrentValue(keyGeneratorDAO);
        verify(globalLockManager).unlock(anyString(), anyString());
    }

    @Test
    void initTenantAware_lockDisabled_shouldNotInitCache() throws Exception {
        setStaticField(false);

        manager.initTenantAware();

        verify(cacheWrapper, never()).initCache(any());
    }

    @Test
    void initTenantAware_lockEnabled_shouldInitCache() throws Exception {
        setStaticField(true);

        manager.initTenantAware();

        verify(cacheWrapper).initCache(keyGeneratorDAO);
    }

    @Test
    void releaseTenantAware_lockDisabled_shouldNotReleaseCache() throws Exception {
        setStaticField(false);

        manager.releaseTenantAware();

        verify(cacheWrapper, never()).release();
    }

    @Test
    void releaseTenantAware_lockEnabled_shouldReleaseCache() throws Exception {
        setStaticField(true);

        manager.releaseTenantAware();

        verify(cacheWrapper).release();
    }

    private static boolean getStaticField() throws Exception {
        Field field = KeyGeneratorManager.class.getDeclaredField("KEYGEN_LOCK_ENABLED");
        field.setAccessible(true);
        return (boolean) field.get(null);
    }

    private static void setStaticField(boolean value) throws Exception {
        Field field = KeyGeneratorManager.class.getDeclaredField("KEYGEN_LOCK_ENABLED");
        field.setAccessible(true);
        field.set(null, value);
    }
}