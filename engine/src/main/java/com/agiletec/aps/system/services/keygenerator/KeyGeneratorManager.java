/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package com.agiletec.aps.system.services.keygenerator;

import com.agiletec.aps.system.ApsSystemUtils;
import com.agiletec.aps.system.common.AbstractService;
import com.agiletec.aps.util.ApsTenantApplicationUtils;
import org.entando.entando.aps.system.services.IFeatureFlag;
import org.entando.entando.aps.system.services.sync.IGlobalLockManager;
import org.entando.entando.aps.system.services.sync.exception.GlobalLockEntException;
import org.entando.entando.aps.system.services.tenants.RefreshableBeanTenantAware;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.keygenerator.cache.IKeyGeneratorManagerCacheWrapper;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import java.time.Duration;
import java.util.function.Supplier;

import static com.agiletec.aps.system.ApsSystemUtils.getEnv;

/**
 * Servizio gestore di sequenze univoche.
 *
 * @author S.Didaci - E.Santoboni
 */
public class KeyGeneratorManager extends AbstractService implements IKeyGeneratorManager, RefreshableBeanTenantAware {

	private final EntLogger logger = EntLogFactory.getSanitizedLogger(getClass());

	private static boolean KEYGEN_LOCK_ENABLED = IFeatureFlag.readEnablementStatus("KEYGEN_LOCK");

	private static Duration KEYGEN_LOCK_DURATION = Duration.ofMinutes(getEnv("KEYGEN_LOCK_DURATION", 5));

	private static Duration KEYGEN_LOCK_MAX_WAIT = Duration.ofMinutes(getEnv("KEYGEN_LOCK_MAX_WAIT_MILLIS", 250));

	private IKeyGeneratorDAO keyGeneratorDao;

	private IKeyGeneratorManagerCacheWrapper cacheWrapper;

	private IGlobalLockManager globalLockManager;

	@Override
	public void init() throws Exception {
		initTenantAware();
		if (this.isLockEnabled()) {
			logger.debug("{} ready. Lock feature is enabled.", this.getClass().getName(), this.getCacheWrapper().getUniqueKeyCurrentValue());
		} else {
			logger.debug("{} ready. : last loaded key {}", this.getClass().getName());
		}	}

	@Override
	protected void release() {
		releaseTenantAware();
		super.release();
	}

	@Override
	public void initTenantAware() throws Exception {
		if (this.isLockEnabled()) {
			this.getCacheWrapper().initCache(this.getKeyGeneratorDAO());
		}
	}

	@Override
	public void releaseTenantAware() {
		if (this.isLockEnabled()) {
			this.getCacheWrapper().release();
		}
	}
	/**
	 * Restituisce la chiave univoca corrente.
	 *
	 * @return La chiave univoca corrente.
	 * @throws EntException In caso di errore nell'aggiornamento della
	 * chiave corrente.
	 */
	@Override
	public int getUniqueKeyCurrentValue() throws EntException {
		IKeyGeneratorDAO keyGeneratorDAO = this.getKeyGeneratorDAO();
		if (this.isLockEnabled()) {
            return this.doOnLock(() ->
                    this.getCacheWrapper().getAndIncrementUniqueKeyCurrentValue(keyGeneratorDAO));
		} else {
			return keyGeneratorDAO.getNextUniqueKey();
		}
	}


	private <T> T doOnLock(Supplier<T> supplier) throws GlobalLockEntException {
		ApsSystemUtils.ApsDeepDebug.print("CACHE:TENANT", String.format("%s  - opening cache lock - tenant %s",
				this.getClass().getSimpleName(), ApsTenantApplicationUtils.getTenant().orElse("primary")));
		IGlobalLockManager globalLockManager = this.getGlobalLockManager();
		String lockKey = this.getLockKey();
		String token = globalLockManager.lock(lockKey, "system",
				KEYGEN_LOCK_DURATION,
				KEYGEN_LOCK_MAX_WAIT);

		T result = supplier.get();

		globalLockManager.unlock(lockKey, token);

		ApsSystemUtils.ApsDeepDebug.print("CACHE:TENANT", String.format("%s  - closing cache lock - tenant %s",
				this.getClass().getSimpleName(), ApsTenantApplicationUtils.getTenant().orElse("primary")));
		return result;
	}

	private String getLockKey() {
		String tenant = ApsTenantApplicationUtils.getTenant().orElse("primary");
		return tenant + "_keygen";
	}

	private boolean isLockEnabled() {
		return KEYGEN_LOCK_ENABLED;
	}

	protected IKeyGeneratorDAO getKeyGeneratorDAO() {
		return keyGeneratorDao;
	}

	public void setKeyGeneratorDAO(IKeyGeneratorDAO generatorDAO) {
		this.keyGeneratorDao = generatorDAO;
	}

	protected IKeyGeneratorManagerCacheWrapper getCacheWrapper() {
		return cacheWrapper;
	}

	public void setCacheWrapper(IKeyGeneratorManagerCacheWrapper cacheWrapper) {
		this.cacheWrapper = cacheWrapper;
	}

	protected IGlobalLockManager getGlobalLockManager() {
		return globalLockManager;
	}

	public void setGlobalLockManager(IGlobalLockManager globalLockManager) {
		this.globalLockManager = globalLockManager;
	}


}
