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

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.entando.entando.aps.system.services.userprofile.model.IUserProfile;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import com.agiletec.aps.system.services.user.AbstractUser;
import com.agiletec.aps.system.services.user.UserDetails;
import org.entando.entando.aps.system.services.userpreferences.IUserPreferencesManager;
import org.entando.entando.ent.exception.EntException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of ProfileManager Aspect. This class join a user with his
 * Profile whatever implementation of User Management.
 *
 * @author E.Santoboni
 */
@Aspect
public class UserManagementAspect {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(UserManagementAspect.class);
    
    private final IUserProfileManager userProfileManager;
    private final IAvatarService avatarService;
    private final IUserPreferencesManager userPreferencesManager;
    
    @Autowired
    public UserManagementAspect(IUserProfileManager userProfileManager, 
            IAvatarService avatarService, IUserPreferencesManager userPreferencesManager) {
        this.userProfileManager = userProfileManager;
        this.userPreferencesManager = userPreferencesManager;
        this.avatarService = avatarService;
    }
    
    @AfterReturning(pointcut = "execution(* com.agiletec.aps.system.services.user.IUserManager.getUser(..))", returning = "user")
    public void injectProfile(Object user) {
        if (user != null) {
            AbstractUser userDetails = (AbstractUser) user;
            if (null == userDetails.getProfile()) {
                try {
                    IUserProfile profile = this.getUserProfileManager().getProfile(userDetails.getUsername());
                    userDetails.setProfile(profile);
                } catch (Throwable t) {
                    logger.error("Error injecting profile on user {}", userDetails.getUsername(), t);
                }
            }
        }
    }

    @AfterReturning(pointcut = "execution(* com.agiletec.aps.system.services.user.IUserManager.addUser(..)) && args(user,..)")
    public void addProfile(Object user) {
        if (user != null) {
            UserDetails userDetails = (UserDetails) user;
            Object profile = userDetails.getProfile();
            if (null != profile) {
                try {
                    this.getUserProfileManager().addProfile(userDetails.getUsername(), (IUserProfile) profile);
                } catch (Throwable t) {
                    logger.error("Error adding profile on user {}", userDetails.getUsername(), t);
                }
            }
        }
    }
    
    @AfterReturning(pointcut = "execution(* com.agiletec.aps.system.services.user.IUserManager.updateUser(..)) && args(user,..)")
    public void updateProfile(Object user) {
        if (user != null) {
            UserDetails userDetails = (UserDetails) user;
            Object profile = userDetails.getProfile();
            if (null != profile) {
                try {
                    this.getUserProfileManager().updateProfile(userDetails.getUsername(), (IUserProfile) profile);
                } catch (Throwable t) {
                    logger.error("Error updating profile to user {}", userDetails.getUsername(), t);
                }
            }
        }
    }

    @AfterReturning(pointcut = "execution(* com.agiletec.aps.system.services.user.IUserManager.removeUser(..)) && args(key)")
    public void deleteUserData(Object key) {
        String username = null;
        if (key instanceof String) {
            username = key.toString();
        } else if (key instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) key;
            username = userDetails.getUsername();
        }
        if (username != null) {
            try {
                this.getUserProfileManager().deleteProfile(username);
            } catch (EntException t) {
                logger.error("Error deleting user profile. user: {}", username, t);
            }
            try {
                this.avatarService.deleteAvatar(username);
            } catch (EntException t) {
                logger.error("Error deleting user avatar. user: {}", username, t);
            }
            try {
                this.userPreferencesManager.deleteUserPreferences(username);
            } catch (EntException t) {
                logger.error("Error deleting user preverences. user: {}", username, t);
            }
        }
    }

    protected IUserProfileManager getUserProfileManager() {
        return userProfileManager;
    }
    
}
