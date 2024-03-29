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
package org.entando.entando.apsadmin.user;

import com.agiletec.aps.system.common.entity.IEntityManager;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.apsadmin.system.entity.AbstractApsEntityFinderAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.userprofile.IUserProfileManager;
import org.entando.entando.aps.system.services.userprofile.model.IUserProfile;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;

/**
 * @author E.Santoboni
 */
public class UserProfileFinderAction extends AbstractApsEntityFinderAction {
	
	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(UserProfileFinderAction.class);
	
	public String list() {
		this.setUsername(null);
		return this.execute();
	}
    
	@Override
    public String execute() {
        try {
            String username = this.getUsername();
            EntitySearchFilter filter = null;
            if (username != null && username.trim().length() > 0) {
                filter = new EntitySearchFilter(IEntityManager.ENTITY_ID_FILTER_KEY, false, username, true);
            } else {
                filter = new EntitySearchFilter(IEntityManager.ENTITY_ID_FILTER_KEY, false);
            }
            filter.setOrder(EntitySearchFilter.ASC_ORDER);
            this.addFilter(filter);
        } catch (Throwable t) {
        	_logger.error("error in execute", t);
            return FAILURE;
        }
        return super.execute();
    }
    
	@Override
    public List<String> getSearchResult() {
        List<String> searchResult;
        try {
            Integer withProfile = this.getWithProfile();
            List<String> profileSearchResult = super.getSearchResult();
            if (super.isAddedAttributeFilter() ||
                    (null != withProfile && withProfile.intValue() == WITH_PROFILE_CODE) ||
                    (StringUtils.isNotBlank(super.getEntityTypeCode()))) {
                this.setWithProfile(WITH_PROFILE_CODE);
                return profileSearchResult;
            }
            List<String> usernames = this.getUserManager().searchUsernames(this.getUsername());
            searchResult = new ArrayList<>();
            for (String username : usernames) {
                if (null == withProfile ||
                        (withProfile.intValue() == WITHOUT_PROFILE_CODE && !profileSearchResult.contains(username))) {
                    searchResult.add(username);
                }
            }
            if (null == withProfile || withProfile.intValue() == WITH_PROFILE_CODE) {
                profileSearchResult.removeAll(usernames);
                if (!profileSearchResult.isEmpty()) {
                    searchResult.addAll(profileSearchResult);
                    Collections.sort(searchResult);
                }
            }
        } catch (Throwable t) {
        	_logger.error("Error while searching entity Ids", t);
            throw new RuntimeException("Error while searching entity Ids", t);
        }
        return searchResult;
    }
    
    public UserDetails getUser(String username) {
        UserDetails user = null;
        try {
            user = this.getUserManager().getUser(username);
        } catch (Throwable t) {
        	_logger.error("Error extracting user {}", username, t);
            throw new RuntimeException("Error extracting user " + username, t);
        }
        return user;
    }
    
    public String getEmailAttributeValue(String username) {
        IUserProfile userProfile = (IUserProfile) this.getEntity(username);
		return (String) userProfile.getValue(userProfile.getMailAttributeName());
    }
    
    public Lang getDefaultLang() {
        return this.getLangManager().getDefaultLang();
    }
    
    public String viewProfile() {
        return SUCCESS;
    }
    
    @Override
    protected void deleteEntity(String entityId) throws Throwable {
        //Not supported
    }
    
    public IUserProfile getUserProfile(String username) {
        return (IUserProfile) this.getEntity(username);
    }
    
    @Override
    protected IEntityManager getEntityManager() {
        return (IEntityManager) this.getUserProfileManager();
    }
    
    public String getUsername() {
        return _username;
    }
    public void setUsername(String username) {
        this._username = username;
    }
	
    public Integer getWithProfile() {
            return _withProfile;
    }
    public void setWithProfile(Integer withProfile) {
            this._withProfile = withProfile;
    }
    
    protected IUserManager getUserManager() {
        return _userManager;
    }
    public void setUserManager(IUserManager userManager) {
        this._userManager = userManager;
    }
    
    protected IUserProfileManager getUserProfileManager() {
        return _userProfileManager;
    }
    public void setUserProfileManager(IUserProfileManager userProfileManager) {
        this._userProfileManager = userProfileManager;
    }
    
    private String _username;
	private Integer _withProfile;
    
    private IUserManager _userManager;
    private IUserProfileManager _userProfileManager;
    
    private static final int WITHOUT_PROFILE_CODE = 0;
    private static final int WITH_PROFILE_CODE = 1;
    
}
