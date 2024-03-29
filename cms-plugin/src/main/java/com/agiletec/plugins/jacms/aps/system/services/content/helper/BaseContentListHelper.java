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
package com.agiletec.plugins.jacms.aps.system.services.content.helper;

import com.agiletec.aps.system.common.entity.helper.BaseFilterUtils;
import com.agiletec.aps.system.common.entity.helper.IEntityFilterBean;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.services.authorization.Authorization;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.entando.entando.aps.system.services.cache.ICacheInfoManager;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;

/**
 * @author E.Santoboni
 */
public class BaseContentListHelper implements IContentListHelper {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(BaseContentListHelper.class);

    private IContentManager contentManager;
    private ICacheInfoManager cacheInfoManager;

    @Override
    public EntitySearchFilter[] getFilters(String contentType, String filtersShowletParam, String langCode) {
        Content contentPrototype = this.getContentManager().createContentType(contentType);
        if (null == filtersShowletParam || filtersShowletParam.trim().length() == 0 || null == contentPrototype) {
            return null;
        }
        BaseFilterUtils dom = new BaseFilterUtils();
        return dom.getFilters(contentPrototype, filtersShowletParam, langCode);
    }

    @Override
    public EntitySearchFilter getFilter(String contentType, IEntityFilterBean bean, String langCode) {
        BaseFilterUtils dom = new BaseFilterUtils();
        Content contentPrototype = this.getContentManager().createContentType(contentType);
        if (null == contentPrototype) {
            return null;
        }
        return dom.getFilter(contentPrototype, bean, langCode);
    }

    @Override
    public String getFilterParam(EntitySearchFilter[] filters) {
        BaseFilterUtils dom = new BaseFilterUtils();
        return dom.getFilterParam(filters);
    }

    @Override
    public List<String> getContentsId(IContentListBean bean, UserDetails user) throws Throwable {
        String cacheKey = buildCacheKey(bean, user);
        if (bean.isCacheable()) {
            List<String> values = (List<String>) this.getCacheInfoManager().getFromCache(ICacheInfoManager.DEFAULT_CACHE_NAME, cacheKey);
            if (values != null) {
                return values;
            }
        }
        String key = BaseContentListHelper.buildCacheKey(bean, user);
        this.releaseCache(bean, user);
        List<String> contentsId = null;
        try {
            if (null == bean.getContentType()) {
                throw new EntException("Content type not defined");
            }
            Collection<String> userGroupCodes = getAllowedGroupCodes(user);
            contentsId = this.getContentManager().loadPublicContentsId(bean.getContentType(), bean.getCategories(), bean.isOrClauseCategoryFilter(), bean.getFilters(), userGroupCodes);
            this.getCacheInfoManager().putInGroup(ICacheInfoManager.DEFAULT_CACHE_NAME, key, new String[]{JacmsSystemConstants.CONTENTS_ID_CACHE_GROUP_PREFIX + bean.getContentType()});
            if (bean.isCacheable()) {
                this.getCacheInfoManager().putInCache(ICacheInfoManager.DEFAULT_CACHE_NAME, cacheKey, contentsId);
            }
        } catch (Throwable t) {
            logger.error("Error extracting contents id", t);
            throw new EntException("Error extracting contents id", t);
        }
        return contentsId;
    }

    private void releaseCache(IContentListBean bean, UserDetails user) {
        String key = BaseContentListHelper.buildCacheKey(bean, user);
        boolean isExpired = this.getCacheInfoManager().isExpired(ICacheInfoManager.DEFAULT_CACHE_NAME, key);
        if (isExpired) {
            this.getCacheInfoManager().flushEntry(ICacheInfoManager.DEFAULT_CACHE_NAME, key);
        }
    }

    /**
     * Return the groups to witch execute the filter to contents. The User
     * object is non null, extract the groups from the user, else return a
     * collection with only the "free" group.
     *
     * @param user The user. Can be null.
     * @return The groups to witch execute the filter to contents.
     * @deprecated
     */
    protected Collection<String> getAllowedGroups(UserDetails user) {
        return getAllowedGroupCodes(user);
    }

    public static Collection<String> getAllowedGroupCodes(UserDetails user) {
        Set<String> codes = new HashSet<>();
        codes.add(Group.FREE_GROUP_NAME);
        List<Authorization> auths = (null != user) ? user.getAuthorizations() : null;
        if (null != auths) {
            for (Authorization auth : auths) {
                if (null != auth && null != auth.getGroup()) {
                    codes.add(auth.getGroup().getName());
                }
            }
        }
        return codes;
    }

    public static String buildCacheKey(IContentListBean bean, UserDetails user) {
        Collection<String> userGroupCodes = getAllowedGroupCodes(user);
        return buildCacheKey(bean, userGroupCodes);
    }

    protected static String buildCacheKey(IContentListBean bean, Collection<String> userGroupCodes) {
        return buildStringBuilderCacheKey(bean, userGroupCodes).toString();
    }

    protected static StringBuilder buildStringBuilderCacheKey(IContentListBean bean, UserDetails user) {
        Collection<String> userGroupCodes = getAllowedGroupCodes(user);
        return buildStringBuilderCacheKey(bean, userGroupCodes);
    }

    protected static StringBuilder buildStringBuilderCacheKey(IContentListBean bean, Collection<String> userGroupCodes) {
        StringBuilder cacheKey = new StringBuilder();
        if (null != bean.getListName()) {
            cacheKey.append("LISTNAME_").append(bean.getListName());
        }
        if (null != bean.getContentType()) {
            cacheKey.append("TYPE_").append(bean.getContentType());
        }
        List<String> groupCodes = new ArrayList<>(userGroupCodes);
        if (!groupCodes.contains(Group.FREE_GROUP_NAME)) {
            groupCodes.add(Group.FREE_GROUP_NAME);
        }
        Collections.sort(groupCodes);
        for (int i = 0; i < groupCodes.size(); i++) {
            if (i == 0) {
                cacheKey.append("-GROUPS");
            }
            String code = groupCodes.get(i);
            cacheKey.append("_").append(code);
        }
        if (null != bean.getCategories() && bean.getCategories().length > 0) {
            List<String> categoryCodes = Arrays.asList(bean.getCategories());
            Collections.sort(categoryCodes);
            for (int j = 0; j < categoryCodes.size(); j++) {
                if (j == 0) {
                    cacheKey.append("-CATEGORIES");
                }
                String code = categoryCodes.get(j);
                cacheKey.append("_").append(code);
            }
            cacheKey.append("_ORCLAUSE_").append(bean.isOrClauseCategoryFilter());
        }
        if (null != bean.getFilters()) {
            for (int k = 0; k < bean.getFilters().length; k++) {
                if (k == 0) {
                    cacheKey.append("-FILTERS");
                }
                EntitySearchFilter filter = bean.getFilters()[k];
                cacheKey.append("_").append(filter.toString());
            }
        }
        return cacheKey;
    }

    public static String concatStrings(Collection<String> values, String separator) {
        StringBuilder concatedValues = new StringBuilder();
        if (null == values) {
            return concatedValues.toString();
        }
        boolean first = true;
        Iterator<String> valuesIter = values.iterator();
        while (valuesIter.hasNext()) {
            if (!first) {
                concatedValues.append(separator);
            }
            concatedValues.append(valuesIter.next());
            first = false;
        }
        return concatedValues.toString();
    }

    public static List<String> splitValues(String concatedValues, String separator) {
        List<String> values = new ArrayList<>();
        if (concatedValues != null && concatedValues.trim().length() > 0) {
            String[] codes = concatedValues.split(separator);
            for (String code : codes) {
                values.add(code);
            }
        }
        return values;
    }

    protected IContentManager getContentManager() {
        return contentManager;
    }

    public void setContentManager(IContentManager contentManager) {
        this.contentManager = contentManager;
    }

    protected ICacheInfoManager getCacheInfoManager() {
        return cacheInfoManager;
    }

    public void setCacheInfoManager(ICacheInfoManager cacheInfoManager) {
        this.cacheInfoManager = cacheInfoManager;
    }

}
