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

import com.agiletec.aps.system.common.AbstractService;
import org.entando.entando.aps.system.services.tenants.RefreshableBeanTenantAware;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.keygenerator.cache.IKeyGeneratorManagerCacheWrapper;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

/**
 * Servizio gestore di sequenze univoche.
 *
 * @author S.Didaci - E.Santoboni
 */
public class KeyGeneratorManager extends AbstractService implements IKeyGeneratorManager, RefreshableBeanTenantAware {

	private final EntLogger logger = EntLogFactory.getSanitizedLogger(getClass());

	private IKeyGeneratorDAO keyGeneratorDao;

	private IKeyGeneratorManagerCacheWrapper cacheWrapper;

	@Override
	public void init() throws Exception {
		initTenantAware();
		logger.debug("{} ready. : last loaded key {}", this.getClass().getName(), this.getCacheWrapper().getUniqueKeyCurrentValue());
	}

	@Override
	protected void release() {
		releaseTenantAware();
		super.release();
	}

	@Override
	public void initTenantAware() throws Exception {
		this.getCacheWrapper().initCache(this.getKeyGeneratorDAO());
	}

	@Override
	public void releaseTenantAware() {
		this.getCacheWrapper().release();
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
		return this.getCacheWrapper().getAndIncrementUniqueKeyCurrentValue(this.getKeyGeneratorDAO());
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

}
