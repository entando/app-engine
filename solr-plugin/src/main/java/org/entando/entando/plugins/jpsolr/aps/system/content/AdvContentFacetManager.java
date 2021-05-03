/*
 * Copyright 2021-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpsolr.aps.system.content;

import com.agiletec.aps.system.exception.ApsSystemException;
import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.plugins.jacms.aps.system.services.content.widget.UserFilterOptionBean;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.exception.RestServerError;

import org.entando.entando.aps.system.services.searchengine.FacetedContentsResult;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter;
import org.entando.entando.plugins.jpsolr.web.content.AdvRestContentListRequest;
import org.entando.entando.web.common.model.PagedMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author E.Santoboni
 */
public class AdvContentFacetManager /*extends ContentFacetManager*/ implements IAdvContentFacetManager {

    private static final Logger logger = LoggerFactory.getLogger(AdvContentFacetManager.class);
    
    private ICategoryManager categoryManager;
    private ICmsSearchEngineManager searchEngineManager;
    private IAuthorizationManager authorizationManager;
    private ILangManager langManager;

    @Override
    public FacetedContentsResult getFacetResult(SearchEngineFilter[] baseFilters,
            List<String> facetNodeCodes, List<UserFilterOptionBean> beans, List<String> groupCodes) throws ApsSystemException {
        try {
            SearchEngineFilter[] filters = this.getFilters(baseFilters, beans);
            SearchEngineFilter[] categoryFilters = null;
            if (null != facetNodeCodes && !facetNodeCodes.isEmpty()) {
                List<SearchEngineFilter> categoryFiltersList = facetNodeCodes.stream()
                        .filter(c -> this.getCategoryManager().getCategory(c) != null)
                        .map(c -> new SearchEngineFilter("category", false, c)).collect(Collectors.toList());
                categoryFilters = categoryFiltersList.toArray(new SearchEngineFilter[categoryFiltersList.size()]);
            }
            return this.getSearchEngineManager().searchFacetedEntities(filters, categoryFilters, groupCodes);
        } catch (Exception t) {
            logger.error("Error loading facet result", t);
            throw new ApsSystemException("Error loading facet result", t);
        }
    }

    @Override
    public FacetedContentsResult getFacetResult(SearchEngineFilter[] baseFilters,
            SearchEngineFilter[] facetNodeCodes, List<UserFilterOptionBean> beans, List<String> groupCodes) throws ApsSystemException {
        try {
            SearchEngineFilter[] filters = this.getFilters(baseFilters, beans);
            return this.getSearchEngineManager().searchFacetedEntities(filters, facetNodeCodes, groupCodes);
        } catch (Exception t) {
            logger.error("Error loading facet result", t);
            throw new ApsSystemException("Error loading facet result", t);
        }
    }

    @Override
    public List<String> loadContentsId(SearchEngineFilter[] baseFilters, SearchEngineFilter[] facetNodeCodes, List<UserFilterOptionBean> beans, List<String> groupCodes) throws ApsSystemException {
        List<String> items = null;
        try {
            SearchEngineFilter[] filters = this.getFilters(baseFilters, beans);
            items = this.getSearchEngineManager().loadContentsId(filters, facetNodeCodes, groupCodes);
        } catch (Exception t) {
            logger.error("Error loading contents id", t);
            throw new ApsSystemException("Error loading contents id", t);
        }
        return items;
    }

    protected SearchEngineFilter[] getFilters(SearchEngineFilter[] baseFilters, List<UserFilterOptionBean> beans) {
        SearchEngineFilter[] filters = (null != baseFilters) ? baseFilters : new SearchEngineFilter[0];
        if (null != beans) {
            for (int i = 0; i < beans.size(); i++) {
                UserFilterOptionBean bean = beans.get(i);
                SearchEngineFilter sf = bean.extractFilter();
                if (null != sf) {
                    filters = ArrayUtils.add(filters, sf);
                }
            }
        }
        return filters;
    }

    @Override
    public FacetedContentsResult getFacetedContents(AdvRestContentListRequest requestList, UserDetails user) {
        FacetedContentsResult facetedResult = null;
        try {
            String langCode = (StringUtils.isBlank(requestList.getLang())) ? this.getLangManager().getDefaultLang().getCode() : requestList.getLang();
            SearchEngineFilter[] searchFilters = requestList.extractFilters(langCode);
            SearchEngineFilter[] categorySearchFilters = requestList.extractCategoryFilters();
            List<String> userGroupCodes = this.getAllowedGroups(user);
            facetedResult = this.getSearchEngineManager().searchFacetedEntities(searchFilters, categorySearchFilters, userGroupCodes);
        } catch (Exception t) {
            logger.error("error in search contents", t);
            throw new RestServerError("error in search contents", t);
        }
        return facetedResult;
    }

    @Override
    public PagedMetadata<String> getContents(AdvRestContentListRequest requestList, UserDetails user) {
        try {
            FacetedContentsResult facetedResult = this.getFacetedContents(requestList, user);
            List<String> result = facetedResult.getContentsId();
            List<String> sublist = requestList.getSublist(result);
            PagedMetadata<String> pagedMetadata = new PagedMetadata<>(requestList, result.size());
            pagedMetadata.setBody(sublist);
            return pagedMetadata;
        } catch (Exception t) {
            logger.error("error in search contents", t);
            throw new RestServerError("error in search contents", t);
        }
    }

    protected List<String> getAllowedGroups(UserDetails currentUser) {
        List<String> groupCodes = new ArrayList<>();
        if (null != currentUser) {
            List<Group> groups = this.getAuthorizationManager().getUserGroups(currentUser);
            groupCodes.addAll(groups.stream().map(Group::getName).collect(Collectors.toList()));
        }
        groupCodes.add(Group.FREE_GROUP_NAME);
        return groupCodes;
    }

    protected ICategoryManager getCategoryManager() {
        return categoryManager;
    }

    public void setCategoryManager(ICategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }

    protected ICmsSearchEngineManager getSearchEngineManager() {
        return searchEngineManager;
    }

    public void setSearchEngineManager(ICmsSearchEngineManager searchEngineManager) {
        this.searchEngineManager = searchEngineManager;
    }
    
    protected IAuthorizationManager getAuthorizationManager() {
        return authorizationManager;
    }

    public void setAuthorizationManager(IAuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    protected ILangManager getLangManager() {
        return langManager;
    }

    public void setLangManager(ILangManager langManager) {
        this.langManager = langManager;
    }
    
}
