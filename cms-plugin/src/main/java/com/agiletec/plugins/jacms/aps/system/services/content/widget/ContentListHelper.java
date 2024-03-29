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
package com.agiletec.plugins.jacms.aps.system.services.content.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.ListUtils;
import org.entando.entando.aps.system.services.cache.ICacheInfoManager;
import org.entando.entando.aps.system.services.searchengine.IEntitySearchEngineManager;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.entity.helper.IEntityFilterBean;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.aps.util.ApsProperties;
import com.agiletec.plugins.jacms.aps.system.services.cache.CmsCacheWrapperManager;
import com.agiletec.plugins.jacms.aps.system.services.content.helper.BaseContentListHelper;
import com.agiletec.plugins.jacms.aps.system.services.content.widget.util.FilterUtils;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Classe helper per la widget di erogazione contenuti in lista.
 *
 * @author E.Santoboni
 */
public class ContentListHelper extends BaseContentListHelper implements IContentListWidgetHelper {

    private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(ContentListHelper.class);

    private String userFilterDateFormat;

    private IEntitySearchEngineManager searchEngineManager;

    @Override
    public EntitySearchFilter[] getFilters(String contentType, String filtersShowletParam, RequestContext reqCtx) {
        Lang currentLang = (Lang) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG);
        return super.getFilters(contentType, filtersShowletParam, currentLang.getCode());
    }

    @Override
    public EntitySearchFilter getFilter(String contentType, IEntityFilterBean bean, RequestContext reqCtx) {
        Lang currentLang = (Lang) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG);
        return super.getFilter(contentType, bean, currentLang.getCode());
    }

    @Override
    public UserFilterOptionBean getUserFilterOption(String contentType, IEntityFilterBean bean, RequestContext reqCtx) {
        FilterUtils filterUtils = new FilterUtils();
        return filterUtils.getUserFilter(contentType, bean, this.getContentManager(), this.getUserFilterDateFormat(), reqCtx);
    }

    @Override
    @Deprecated
    public String getShowletParam(EntitySearchFilter[] filters) {
        return super.getFilterParam(filters);
    }

    @Override
    public List<String> getContentsId(IContentListTagBean bean, RequestContext reqCtx) throws Throwable {
        String key = ContentListHelper.buildCacheKey(bean, reqCtx);
        this.releaseCache(bean, reqCtx);
        boolean cacheable = bean.isCacheable() && !isUserFilterExecuted(bean);
        List<String> contentsId = null;
        if (cacheable) {
            contentsId = (List<String>) this.getCacheInfoManager().getFromCache(ICacheInfoManager.DEFAULT_CACHE_NAME, key);
            if (null != contentsId) {
                return contentsId;
            }
        }
        try {
            contentsId = this.extractContentsId(bean, reqCtx);
            contentsId = this.executeFullTextSearch(bean, contentsId, reqCtx);
        } catch (Throwable t) {
            _logger.error("Error extracting contents id", t);
            throw new EntException("Error extracting contents id", t);
        }
        if (cacheable) {
            String[] groups = CmsCacheWrapperManager.getContentListCacheGroups(bean, reqCtx);
            this.getCacheInfoManager().putInCache(ICacheInfoManager.DEFAULT_CACHE_NAME, key, contentsId, groups);
            Calendar expiration = Calendar.getInstance();
            expiration.add(Calendar.MINUTE, 30);
            this.getCacheInfoManager().setExpirationTime(ICacheInfoManager.DEFAULT_CACHE_NAME, key, expiration.getTime());
        }
        return contentsId;
    }

    private void releaseCache(IContentListTagBean bean, RequestContext reqCtx) {
        String key = ContentListHelper.buildCacheKey(bean, reqCtx);
        boolean isExpired = this.getCacheInfoManager().isExpired(ICacheInfoManager.DEFAULT_CACHE_NAME, key);
        if (isExpired) {
            this.getCacheInfoManager().flushEntry(ICacheInfoManager.DEFAULT_CACHE_NAME, key);
        }
    }

    public static boolean isUserFilterExecuted(IContentListTagBean bean) {
        if (null == bean) {
            return false;
        }
        List<UserFilterOptionBean> filterOptions = bean.getUserFilterOptions();
        if (null == filterOptions || filterOptions.isEmpty()) {
            return false;
        }
        for (UserFilterOptionBean userFilter : filterOptions) {
            if (null != userFilter.getFormFieldValues() && userFilter.getFormFieldValues().size() > 0) {
                return true;
            }
        }
        return false;
    }

    protected List<String> extractContentsId(IContentListTagBean bean, RequestContext reqCtx) throws EntException {
        List<String> contentsId = null;
        try {
            List<UserFilterOptionBean> userFilters = bean.getUserFilterOptions();
            Widget widget = (Widget) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_WIDGET);
            ApsProperties config = (null != widget) ? widget.getConfig() : null;
            if (null == bean.getContentType() && null != config) {
                bean.setContentType(config.getProperty(WIDGET_PARAM_CONTENT_TYPE));
            }
            if (null == bean.getContentType()) {
                throw new EntException("Tipo contenuto non definito");
            }
            if (null == bean.getCategory() && null != config && null != config.getProperty(SHOWLET_PARAM_CATEGORY)) {
                bean.setCategory(config.getProperty(SHOWLET_PARAM_CATEGORY));
            }
            EntitySearchFilter[] filtersToUse = this.createWidgetFilters(bean, config, WIDGET_PARAM_FILTERS, reqCtx);
            if (null != userFilters && userFilters.size() > 0) {
                for (UserFilterOptionBean userFilter : userFilters) {
                    EntitySearchFilter filter = userFilter.getEntityFilter();
                    if (null != filter) {
                        filtersToUse = ArrayUtils.add(filtersToUse, filter);
                    }
                }
            }
            String[] categories = this.getCategories(bean.getCategories(), config, userFilters);
            Collection<String> userGroupCodes = this.getAllowedGroups(reqCtx);
            boolean orCategoryFilterClause = this.extractOrCategoryFilterClause(config);
            contentsId = this.getContentManager().loadPublicContentsId(bean.getContentType(),
                    categories, orCategoryFilterClause, filtersToUse, userGroupCodes);
        } catch (Throwable t) {
            _logger.error("Error extracting contents id", t);
            throw new EntException("Error extracting contents id", t);
        }
        return contentsId;
    }

    protected boolean extractOrCategoryFilterClause(ApsProperties config) {
        if (null == config) {
            return false;
        }
        String param = config.getProperty(WIDGET_PARAM_OR_CLAUSE_CATEGORY_FILTER);
        if (null == param) {
            return false;
        }
        return Boolean.parseBoolean(param);
    }

    protected List<String> executeFullTextSearch(IContentListTagBean bean,
            List<String> masterContentsId, RequestContext reqCtx) throws EntException {
        UserFilterOptionBean fullTextUserFilter = null;
        List<UserFilterOptionBean> userFilterOptions = bean.getUserFilterOptions();
        if (null != userFilterOptions) {
            for (UserFilterOptionBean userFilter : userFilterOptions) {
                if (null != userFilter.getFormFieldValues() && userFilter.getFormFieldValues().size() > 0) {
                    if (!userFilter.isAttributeFilter()
                            && userFilter.getKey().equals(UserFilterOptionBean.KEY_FULLTEXT)) {
                        fullTextUserFilter = userFilter;
                    }
                }
            }
        }
        if (fullTextUserFilter != null && null != fullTextUserFilter.getFormFieldValues()) {
            String word = fullTextUserFilter.getFormFieldValues().get(fullTextUserFilter.getFormFieldNames()[0]);
            Lang currentLang = (Lang) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG);
            List<String> fullTextResult = this.getSearchEngineManager().searchEntityId(currentLang.getCode(), word, this.getAllowedGroups(reqCtx));
            if (null != fullTextResult) {
                return ListUtils.intersection(fullTextResult, masterContentsId);
            } else {
                return new ArrayList<>();
            }
        } else {
            return masterContentsId;
        }
    }

    protected String[] getCategories(String[] categories, ApsProperties config, List<UserFilterOptionBean> userFilters) {
        Set<String> codes = new HashSet<>();
        if (null != categories) {
            for (String category : categories) {
                codes.add(category);
            }
        }
        String categoriesParam = (null != config) ? config.getProperty(WIDGET_PARAM_CATEGORIES) : null;
        if (null != categoriesParam && categoriesParam.trim().length() > 0) {
            List<String> categoryCodes = splitValues(categoriesParam, CATEGORIES_SEPARATOR);
            for (String categoryCode : categoryCodes) {
                codes.add(categoryCode);
            }
        }
        if (null != userFilters) {
            for (UserFilterOptionBean userFilterBean : userFilters) {
                if (!userFilterBean.isAttributeFilter()
                        && userFilterBean.getKey().equals(UserFilterOptionBean.KEY_CATEGORY)
                        && null != userFilterBean.getFormFieldValues()) {
                    codes.add(userFilterBean.getFormFieldValues().get(userFilterBean.getFormFieldNames()[0]));
                }
            }
        }
        if (codes.isEmpty()) {
            return null;
        }
        String[] categoryCodes = new String[codes.size()];
        Iterator<String> iter = codes.iterator();
        int i = 0;
        while (iter.hasNext()) {
            categoryCodes[i++] = iter.next();
        }
        return categoryCodes;
    }

    protected EntitySearchFilter[] createWidgetFilters(IContentListTagBean bean, ApsProperties widgetParams, String widgetParamName, RequestContext reqCtx) {
        if (null == widgetParams) {
            return bean.getFilters();
        }
        String widgetFilters = widgetParams.getProperty(widgetParamName);
        EntitySearchFilter[] filters = this.getFilters(bean.getContentType(), widgetFilters, reqCtx);
        if (null == filters) {
            return bean.getFilters();
        }
        EntitySearchFilter[] filtersToReturn = null;
        if (null != bean.getFilters()) {
            filtersToReturn = Arrays.copyOf(bean.getFilters(), bean.getFilters().length);
        } else {
            filtersToReturn = new EntitySearchFilter[0];
        }
        filtersToReturn = ArrayUtils.addAll(filtersToReturn, filters);
        return filtersToReturn;
    }

    @Deprecated
    protected List<String> getContentsId(IContentListTagBean bean, String[] categories, RequestContext reqCtx) throws Throwable {
        return this.getContentsId(bean, reqCtx);
    }

    protected Collection<String> getAllowedGroups(RequestContext reqCtx) {
        UserDetails currentUser = (UserDetails) reqCtx.getRequest().getSession().getAttribute(SystemConstants.SESSIONPARAM_CURRENT_USER);
        return getAllowedGroupCodes(currentUser);
    }

    public static String buildCacheKey(IContentListTagBean bean, RequestContext reqCtx) {
        UserDetails currentUser = (UserDetails) reqCtx.getRequest().getSession().getAttribute(SystemConstants.SESSIONPARAM_CURRENT_USER);
        StringBuilder baseCacheKey = ContentListHelper.buildStringBuilderCacheKey(bean, currentUser);
        Lang currentLang = (Lang) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG);
        if (null != currentLang) {
            baseCacheKey.append("_LANG_").append(currentLang.getCode());
        }
        IPage page = (IPage) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE);
        if (null == page) {
            baseCacheKey.append("_PAGENOTFOUND");
        } else {
            baseCacheKey.append("_PAGE_").append(page.getCode());
        }
        Widget currentWidget = (Widget) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_WIDGET);
        if (null != currentWidget && null != currentWidget.getConfig()) {
            List<String> paramKeys = new ArrayList(currentWidget.getConfig().keySet());
            Collections.sort(paramKeys);
            for (int i = 0; i < paramKeys.size(); i++) {
                if (i == 0) {
                    baseCacheKey.append("_WIDGETPARAM");
                } else {
                    baseCacheKey.append(",");
                }
                String paramkey = (String) paramKeys.get(i);
                baseCacheKey.append(paramkey).append("=").append(currentWidget.getConfig().getProperty(paramkey));
            }
        }
        return DigestUtils.sha256Hex(baseCacheKey.toString());
    }

    @Override
    public List<UserFilterOptionBean> getConfiguredUserFilters(IContentListTagBean bean, RequestContext reqCtx) throws EntException {
        List<UserFilterOptionBean> userEntityFilters = null;
        try {
            Widget widget = (Widget) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_WIDGET);
            ApsProperties config = (null != widget) ? widget.getConfig() : null;
            if (null == config || null == config.getProperty(WIDGET_PARAM_CONTENT_TYPE)) {
                return null;
            }
            String contentTypeCode = config.getProperty(WIDGET_PARAM_CONTENT_TYPE);
            IApsEntity prototype = this.getContentManager().getEntityPrototype(contentTypeCode);
            if (null == prototype) {
                _logger.error("Null content type by code '{}'", contentTypeCode);
                return null;
            }
            Integer currentFrame = (Integer) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_FRAME);
            Lang currentLang = (Lang) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG);
            String userFilters = config.getProperty(WIDGET_PARAM_USER_FILTERS);
            if (null != userFilters && userFilters.length() > 0) {
                userEntityFilters = FilterUtils.getUserFilters(userFilters, currentFrame, currentLang, prototype, this.getUserFilterDateFormat(), reqCtx.getRequest());
            }
        } catch (Throwable t) {
            _logger.error("Error extracting user filters", t);
            throw new EntException("Error extracting user filters", t);
        }
        return userEntityFilters;
    }

    protected String getUserFilterDateFormat() {
        return userFilterDateFormat;
    }

    public void setUserFilterDateFormat(String userFilterDateFormat) {
        this.userFilterDateFormat = userFilterDateFormat;
    }

    protected IEntitySearchEngineManager getSearchEngineManager() {
        return searchEngineManager;
    }

    public void setSearchEngineManager(IEntitySearchEngineManager searchEngineManager) {
        this.searchEngineManager = searchEngineManager;
    }

}
