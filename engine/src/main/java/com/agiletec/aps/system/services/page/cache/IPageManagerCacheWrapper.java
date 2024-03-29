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
package com.agiletec.aps.system.services.page.cache;

import com.agiletec.aps.system.common.ICacheWrapper;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageDAO;
import com.agiletec.aps.system.services.page.PagesStatus;
import java.util.List;

/**
 * @author E.Santoboni
 */
public interface IPageManagerCacheWrapper extends ICacheWrapper {

    public static final String PAGE_MANAGER_CACHE_NAME = "Entando_PageManager";
    public static final String ONLINE_PAGE_CACHE_NAME_PREFIX = "PageManager_onLine_";
    public static final String DRAFT_PAGE_CACHE_NAME_PREFIX = "PageManager_draft_";
    public static final String ONLINE_ROOT_CACHE_NAME = "PageManager_onLineRoot";
    public static final String DRAFT_ROOT_CACHE_NAME = "PageManager_draftRoot";
    public static final String PAGE_STATUS_CACHE_NAME = "PageManager_pagesStatus";
    public static final String DRAFT_PAGE_CODES_CACHE_NAME = "PageManager_draftCodes";
    public static final String ONLINE_PAGE_CODES_CACHE_NAME = "PageManager_onlineCodes";

    public static final String ONLINE_WIDGET_UTILIZER_CACHE_NAME_PREFIX = "PageManager_onlineUtilizer_";
    public static final String DRAFT_WIDGET_UTILIZER_CACHE_NAME_PREFIX = "PageManager_draftUtilizer_";
    
    public static final String WIDGET_UTILIZER_KEYS_CACHE_NAME = "PageManager_widgetUtilizersCacheKeys";

    public void initCache(IPageDAO pageDao) throws EntException;

    public PagesStatus getPagesStatus();

    public IPage getOnlinePage(String pageCode);

    public IPage getDraftPage(String pageCode);

    public IPage getOnlineRoot();

    public IPage getDraftRoot();
    
    public List<String> getOnlineWidgetUtilizers(String widgetTypeCode) throws EntException;

    public List<String> getDraftWidgetUtilizers(String widgetTypeCode) throws EntException;
    
    public void deleteDraftPage(String pageCode);
    
    public void addDraftPage(IPage page);
    
    public void updateDraftPage(IPage page);
    
    public void moveUpDown(String pageDown, String pageUp);
    
    public void setPageOnline(String pageCode);
    
    public void setPageOffline(String pageCode);
    
    public void movePage(String pageCode, String newParentCode);
    
}
