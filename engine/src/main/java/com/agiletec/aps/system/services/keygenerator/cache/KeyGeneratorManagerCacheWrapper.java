/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package com.agiletec.aps.system.services.keygenerator.cache;

import com.agiletec.aps.system.ApsSystemUtils;
import com.agiletec.aps.system.common.AbstractCacheWrapper;
import com.agiletec.aps.system.services.keygenerator.IKeyGeneratorDAO;
import com.agiletec.aps.util.ApsTenantApplicationUtils;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.springframework.cache.Cache;

public class KeyGeneratorManagerCacheWrapper extends AbstractCacheWrapper implements IKeyGeneratorManagerCacheWrapper {

    private final EntLogger logger = EntLogFactory.getSanitizedLogger(getClass());

    @Override
    public void release() {
        Cache cache = this.getCache();
        ApsSystemUtils.ApsDeepDebug.print("CACHE:TENANT", String.format("%s  - release - tenant %s",
                this.getClass().getSimpleName(), ApsTenantApplicationUtils.getTenant().orElse("primary")));
        cache.evict(IKeyGeneratorManagerCacheWrapper.CURRENT_KEY);
    }

    @Override
    protected String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    public void initCache(IKeyGeneratorDAO keyGeneratorDAO) {
        Integer value = keyGeneratorDAO.getUniqueKey();
        Cache cache = this.getCache();
        ApsSystemUtils.ApsDeepDebug.print("CACHE:TENANT", String.format("%s  - initCache - tenant %s",
                this.getClass().getSimpleName(), ApsTenantApplicationUtils.getTenant().orElse("primary")));
        this.insertObjectsOnCache(cache, value);
    }

    @Override
    public synchronized int getAndIncrementUniqueKeyCurrentValue(IKeyGeneratorDAO keyGeneratorDAO) {
        Cache cache = this.getCache();
        // apro il lock
        ApsSystemUtils.ApsDeepDebug.print("CACHE:TENANT", String.format("%s  - start getAndIncrement - tenant %s",
                this.getClass().getSimpleName(), ApsTenantApplicationUtils.getTenant().orElse("primary")));
        Integer currentValue = this.get(cache, CURRENT_KEY, Integer.class);
        Integer nextValue = currentValue + 1;
        this.insertObjectsOnCache(cache, nextValue);
        keyGeneratorDAO.updateKey(nextValue);
        ApsSystemUtils.ApsDeepDebug.print("CACHE:TENANT", String.format("%s  - end getAndIncrement - tenant %s",
                this.getClass().getSimpleName(), ApsTenantApplicationUtils.getTenant().orElse("primary")));
        // chiudo il lock
        return nextValue;
    }

    @Override
    public int getUniqueKeyCurrentValue() {
        ApsSystemUtils.ApsDeepDebug.print("CACHE:TENANT", String.format("%s  - getUniqueKey - tenant %s",
                this.getClass().getSimpleName(), ApsTenantApplicationUtils.getTenant().orElse("primary")));
        return this.get(this.getCache(), CURRENT_KEY, Integer.class);
    }

    @Override
    public void updateCurrentKey(int value) {
        ApsSystemUtils.ApsDeepDebug.print("CACHE:TENANT", String.format("%s  - updateCurrentKey - tenant %s",
                this.getClass().getSimpleName(), ApsTenantApplicationUtils.getTenant().orElse("primary")));
        this.insertObjectsOnCache(this.getCache(), value);
    }

    private void insertObjectsOnCache(Cache cache, Integer value) {
        ApsSystemUtils.ApsDeepDebug.print("CACHE:TENANT", String.format("%s  - insertObjectsOnCache - value %s - tenant %s",
                this.getClass().getSimpleName(), value, ApsTenantApplicationUtils.getTenant().orElse("primary")));
        cache.put(IKeyGeneratorManagerCacheWrapper.CURRENT_KEY, value);
        logger.trace("current key is now {}", value);
    }

}
