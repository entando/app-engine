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
package com.agiletec.aps.system.services.pagemodel.cache;

import com.agiletec.aps.system.common.AbstractGenericCacheWrapper;
import java.util.Optional;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.pagemodel.IPageModelDAO;
import com.agiletec.aps.system.services.pagemodel.PageModel;
import java.util.Collection;
import java.util.Map;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.springframework.cache.Cache;

/**
 * @author E.Santoboni
 */
public class PageModelManagerCacheWrapper extends AbstractGenericCacheWrapper<PageModel> implements IPageModelManagerCacheWrapper {

	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(PageModelManagerCacheWrapper.class);

	@Override
	public void initCache(IPageModelDAO pageModelDAO) throws EntException {
		try {
			Cache cache = this.getCache();
			Map<String, PageModel> models = pageModelDAO.loadModels();
			this.insertAndCleanCache(cache, models);
		} catch (Throwable t) {
			_logger.error("Error loading page templates", t);
			throw new EntException("Error loading page templates", t);
		}
	}

	@Override
	public PageModel getPageModel(String name) {
		return Optional.ofNullable(this.get(PAGE_MODEL_CACHE_NAME_PREFIX + name, PageModel.class))
				.map(PageModel::clone).orElse(null);
	}

	@Override
	public Collection<PageModel> getPageModels() {
		Map<String, PageModel> map = super.getObjectMap();
		return map.values();
	}

	@Override
	public void addPageModel(PageModel pageModel) {
		this.manage(pageModel.getCode(), pageModel, Action.ADD);
	}

	@Override
	public void updatePageModel(PageModel pageModel) {
		this.manage(pageModel.getCode(), pageModel, Action.UPDATE);
	}

	@Override
	public void deletePageModel(String code) {
		this.manage(code, new PageModel(), Action.DELETE);
	}

	@Override
	protected String getCodesCacheKey() {
		return PAGE_MODEL_CODES_CACHE_NAME;
	}

	@Override
	protected String getCacheKeyPrefix() {
		return PAGE_MODEL_CACHE_NAME_PREFIX;
	}

	@Override
	protected String getCacheName() {
		return PAGE_MODEL_MANAGER_CACHE_NAME;
	}

}
