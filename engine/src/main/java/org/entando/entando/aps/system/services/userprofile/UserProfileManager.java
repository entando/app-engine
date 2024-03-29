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
package org.entando.entando.aps.system.services.userprofile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.entando.entando.aps.system.services.cache.ICacheInfoManager;
import org.entando.entando.aps.system.services.userprofile.event.ProfileChangedEvent;
import org.entando.entando.aps.system.services.userprofile.model.IUserProfile;
import org.entando.entando.aps.system.services.userprofile.model.UserProfileRecord;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.springframework.cache.annotation.CacheEvict;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.entity.ApsEntityManager;
import com.agiletec.aps.system.common.entity.IEntityDAO;
import com.agiletec.aps.system.common.entity.IEntitySearcherDAO;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import org.entando.entando.ent.exception.EntException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

/**
 * Implementation of ProfileManager.
 *
 * @author E.Santoboni
 */
public class UserProfileManager extends ApsEntityManager implements IUserProfileManager {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(UserProfileManager.class);

    private static final String USER_PROFILES_CACHE_GROUP = "UserProfileTypes_cacheGroup";

    private transient IUserProfileDAO profileDAO;

    private transient IEntitySearcherDAO entitySearcherDAO;

    private transient ICacheInfoManager cacheInfoManager;

    @Override
    public IApsEntity getEntity(String entityId) throws EntException {
        return this.getProfile(entityId);
    }

    @Override
    public IUserProfile getDefaultProfileType() {
        IUserProfile profileType = (IUserProfile) super.getEntityPrototype(SystemConstants.DEFAULT_PROFILE_TYPE_CODE);
        if (null == profileType) {
            List<String> entityTypes = new ArrayList<>();
            entityTypes.addAll(this.getEntityPrototypes().keySet());
            if (!entityTypes.isEmpty()) {
                Collections.sort(entityTypes);
                profileType = (IUserProfile) super.getEntityPrototype(entityTypes.get(0));
            }
        }
        return profileType;
    }

    @Override
    public IUserProfile getProfileType(String typeCode) {
        return (IUserProfile) super.getEntityPrototype(typeCode);
    }

    @Override
    @CacheEvict(value = ICacheInfoManager.DEFAULT_CACHE_NAME, key = "'UserProfile_'.concat(#username)")
    public void addProfile(String username, IUserProfile profile) throws EntException {
        try {
            profile.setId(username);
            this.getProfileDAO().addEntity(profile);
            this.notifyProfileChanging(profile, ProfileChangedEvent.INSERT_OPERATION_CODE);
        } catch (Throwable t) {
            logger.error("Error saving profile - user: {}", username, t);
            throw new EntException("Error saving profile", t);
        }
    }

    @Override
    @CacheEvict(value = ICacheInfoManager.DEFAULT_CACHE_NAME, key = "'UserProfile_'.concat(#username)")
    public void deleteProfile(String username) throws EntException {
        try {
            IUserProfile profileToDelete = this.getProfile(username);
            if (null == profileToDelete) {
                return;
            }
            this.getProfileDAO().deleteEntity(username);
            this.notifyProfileChanging(profileToDelete, ProfileChangedEvent.REMOVE_OPERATION_CODE);
        } catch (Throwable t) {
            logger.error("Error deleting user profile {}", username, t);
            throw new EntException("Error deleting user profile", t);
        }
    }

    @Override
    @Cacheable(value = ICacheInfoManager.DEFAULT_CACHE_NAME, key = "'UserProfile_'.concat(#username)")
    public IUserProfile getProfile(String username) throws EntException {
        IUserProfile profile = null;
        try {
            UserProfileRecord profileVO = (UserProfileRecord) this.getProfileDAO().loadEntityRecord(username);
            if (profileVO != null) {
                profile = (IUserProfile) this.createEntityFromXml(profileVO.getTypeCode(), profileVO.getXml());
                profile.setPublicProfile(profileVO.isPublicProfile());
                String cacheKey = "UserProfile_" + username;
                this.getCacheInfoManager().putInGroup(ICacheInfoManager.DEFAULT_CACHE_NAME, cacheKey, new String[]{USER_PROFILES_CACHE_GROUP});
            }
        } catch (Throwable t) {
            logger.error("Error loading profile. user: {} ", username, t);
            throw new EntException("Error loading profile", t);
        }
        return profile;
    }

    @Override
    @CacheEvict(value = ICacheInfoManager.DEFAULT_CACHE_NAME, key = "'UserProfile_'.concat(#username)")
    public void updateProfile(String username, IUserProfile profile) throws EntException {
        try {
            profile.setId(username);
            this.getProfileDAO().updateEntity(profile);
            this.notifyProfileChanging(profile, ProfileChangedEvent.UPDATE_OPERATION_CODE);
        } catch (Throwable t) {
            logger.error("Error updating profile {}", username, t);
            throw new EntException("Error updating profile", t);
        }
    }

    private void notifyProfileChanging(IUserProfile profile, int operationCode) {
        ProfileChangedEvent event = new ProfileChangedEvent();
        event.setProfile(profile);
        event.setOperationCode(operationCode);
        this.notifyEvent(event);
    }

    @Override
    public void removeEntityPrototype(String entityTypeCode) throws EntException {
        super.removeEntityPrototype(entityTypeCode);
        this.getCacheInfoManager().flushGroup(ICacheInfoManager.DEFAULT_CACHE_NAME, USER_PROFILES_CACHE_GROUP);
    }

    @Override
    public void updateEntityPrototype(IApsEntity entityType) throws EntException {
        super.updateEntityPrototype(entityType);
        this.getCacheInfoManager().flushGroup(ICacheInfoManager.DEFAULT_CACHE_NAME, USER_PROFILES_CACHE_GROUP);
    }

    @Override
    protected IEntityDAO getEntityDao() {
        return (IEntityDAO) this.getProfileDAO();
    }

    @Override
    protected IEntitySearcherDAO getEntitySearcherDao() {
        return this.getEntitySearcherDAO();
    }

    protected IUserProfileDAO getProfileDAO() {
        return profileDAO;
    }

    public void setProfileDAO(IUserProfileDAO profileDAO) {
        this.profileDAO = profileDAO;
    }

    protected IEntitySearcherDAO getEntitySearcherDAO() {
        return entitySearcherDAO;
    }

    public void setEntitySearcherDAO(IEntitySearcherDAO entitySearcherDAO) {
        this.entitySearcherDAO = entitySearcherDAO;
    }

    protected ICacheInfoManager getCacheInfoManager() {
        return this.cacheInfoManager;
    }

    @Autowired
    public void setCacheInfoManager(ICacheInfoManager cacheInfoManager) {
        this.cacheInfoManager = cacheInfoManager;
    }

}
