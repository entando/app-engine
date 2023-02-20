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

import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.plugins.jacms.aps.system.services.content.widget.UserFilterOptionBean;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.exception.RestServerError;
import org.entando.entando.aps.system.services.searchengine.FacetedContentsResult;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.plugins.jpsolr.aps.system.solr.SolrSearchEngineManager;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFacetedContentsResult;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrSearchEngineFilter;
import org.entando.entando.plugins.jpsolr.web.content.model.AdvRestContentListRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author E.Santoboni
 */
@Service
public class AdvContentFacetManager implements IAdvContentFacetManager {

    private final ICategoryManager categoryManager;
    private final SolrSearchEngineManager searchEngineManager;
    private final IAuthorizationManager authorizationManager;
    private final ILangManager langManager;

    @Autowired
    public AdvContentFacetManager(ICategoryManager categoryManager, SolrSearchEngineManager searchEngineManager,
            IAuthorizationManager authorizationManager, ILangManager langManager) {
        this.categoryManager = categoryManager;
        this.searchEngineManager = searchEngineManager;
        this.authorizationManager = authorizationManager;
        this.langManager = langManager;
    }

    @Override
    public SolrFacetedContentsResult getFacetResult(SearchEngineFilter[] baseFilters,
            List<String> facetNodeCodes, List<UserFilterOptionBean> beans, List<String> groupCodes)
            throws EntException {
        try {
            SearchEngineFilter[] filters = this.getFilters(baseFilters, beans);
            SearchEngineFilter[] categoryFilters = null;
            if (null != facetNodeCodes && !facetNodeCodes.isEmpty()) {
                List<SearchEngineFilter<String>> categoryFiltersList = facetNodeCodes.stream()
                        .filter(c -> this.categoryManager.getCategory(c) != null)
                        .map(c -> new SearchEngineFilter<>("category", false, c)).collect(Collectors.toList());
                categoryFilters = categoryFiltersList.toArray(new SearchEngineFilter[categoryFiltersList.size()]);
            }
            return (SolrFacetedContentsResult) this.searchEngineManager
                    .searchFacetedEntities(filters, categoryFilters, groupCodes);
        } catch (Exception ex) {
            throw new EntException("Error loading facet result", ex);
        }
    }

    @Override
    public FacetedContentsResult getFacetResult(SearchEngineFilter[] baseFilters,
            SearchEngineFilter[] facetNodeCodes, List<UserFilterOptionBean> beans, List<String> groupCodes)
            throws EntException {
        SearchEngineFilter[] filters = this.getFilters(baseFilters, beans);
        return this.searchEngineManager.searchFacetedEntities(filters, facetNodeCodes, groupCodes);
    }

    protected SearchEngineFilter[] getFilters(SearchEngineFilter[] baseFilters, List<UserFilterOptionBean> beans) {
        SearchEngineFilter[] filters = (null != baseFilters) ? baseFilters : new SearchEngineFilter[0];
        if (null != beans) {
            for (UserFilterOptionBean bean : beans) {
                SearchEngineFilter<?> sf = bean.extractFilter();
                if (null != sf) {
                    filters = ArrayUtils.add(filters, sf);
                }
            }
        }
        return filters;
    }

    @Override
    public SolrFacetedContentsResult getFacetedContents(AdvRestContentListRequest requestList, UserDetails user) {
        SolrFacetedContentsResult facetedResult;
        try {
            String langCode =
                    (StringUtils.isBlank(requestList.getLang())) ? this.langManager.getDefaultLang().getCode()
                            : requestList.getLang();
            SolrSearchEngineFilter[] searchFilters = requestList.extractFilters(langCode);
            SolrSearchEngineFilter[][] doubleFilters = requestList.extractDoubleFilters(langCode);
            if (null != searchFilters) {
                for (SolrSearchEngineFilter<?> searchFilter : searchFilters) {
                    SolrSearchEngineFilter[] filters = new SolrSearchEngineFilter[]{searchFilter};
                    doubleFilters = ArrayUtils.add(doubleFilters, filters);
                }
            }
            SolrSearchEngineFilter[] categorySearchFilters = requestList.extractCategoryFilters();
            List<String> userGroupCodes = this.getAllowedGroups(user);
            facetedResult = this.searchEngineManager.searchFacetedEntities(
                    doubleFilters, categorySearchFilters, userGroupCodes);
        } catch (EntException ex) {
            throw new RestServerError("error in search contents", ex);
        }
        return facetedResult;
    }

    protected List<String> getAllowedGroups(UserDetails currentUser) {
        List<String> groupCodes = new ArrayList<>();
        if (null != currentUser) {
            List<Group> groups = this.authorizationManager.getUserGroups(currentUser);
            groupCodes.addAll(groups.stream().map(Group::getName).collect(Collectors.toList()));
        }
        groupCodes.add(Group.FREE_GROUP_NAME);
        return groupCodes;
    }

}
