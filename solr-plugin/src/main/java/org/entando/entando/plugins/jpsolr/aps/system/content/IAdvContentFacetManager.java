/*
 * Copyright 2019-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.plugins.jacms.aps.system.services.content.widget.UserFilterOptionBean;

import java.util.List;

import org.entando.entando.aps.system.services.searchengine.FacetedContentsResult;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter;
import org.entando.entando.plugins.jpsolr.web.content.AdvRestContentListRequest;
import org.entando.entando.web.common.model.PagedMetadata;

/**
 * @author E.Santoboni
 */
public interface IAdvContentFacetManager /*extends IContentFacetManager*/ {

    public FacetedContentsResult getFacetResult(SearchEngineFilter[] baseFilters,
            List<String> facetNodeCodes, List<UserFilterOptionBean> beans, List<String> groupCodes) throws ApsSystemException;

    public FacetedContentsResult getFacetResult(SearchEngineFilter[] baseFilters,
            SearchEngineFilter[] facetNodeCodes, List<UserFilterOptionBean> beans, List<String> groupCodes) throws ApsSystemException;

    public List<String> loadContentsId(SearchEngineFilter[] baseFilters,
            SearchEngineFilter[] facetNodeCodes, List<UserFilterOptionBean> beans, List<String> groupCodes) throws ApsSystemException;
    
    public FacetedContentsResult getFacetedContents(AdvRestContentListRequest requestList, UserDetails extractCurrentUser);
    
    public PagedMetadata<String> getContents(AdvRestContentListRequest requestList, UserDetails extractCurrentUser);
    
}
