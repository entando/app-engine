/*
 * Copyright 2023-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.system.services.userpreferences;

import java.util.Optional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.entando.entando.ent.exception.EntException;

@Slf4j
@Setter
public class UserPreferencesManager implements IUserPreferencesManager {

    private UserPreferencesDAO userPreferencesDAO;

    @Override
    public void addUserPreferences(UserPreferences userPreferences) throws EntException {
        userPreferencesDAO.addUserPreferences(userPreferences);
    }

    @Override
    public UserPreferences getUserPreferences(String username) throws EntException {
        return userPreferencesDAO.loadUserPreferences(username);
    }

    @Override
    public void updateUserPreferences(UserPreferences userPreferences) throws EntException {
        userPreferencesDAO.updateUserPreferences(userPreferences);
    }

    @Override
    public boolean isUserGravatarEnabled(String username) throws EntException {
        return Optional.ofNullable(this.getUserPreferences(username)).map(p -> p.isGravatar()).orElse(Boolean.FALSE);
    }

    @Override
    public void updateUserGravatarPreference(String username, boolean enabled) throws EntException {
        UserPreferences userPreferences = this.getUserPreferences(username);
        if (null != userPreferences) {
            userPreferences.setGravatar(enabled);
            this.updateUserPreferences(userPreferences);
        } else {
            userPreferences = this.createDefaultUserPreferences(username);
            userPreferences.setGravatar(enabled);
            this.addUserPreferences(userPreferences);
        }
    }
    
    @Override
    public void deleteUserPreferences(String username) throws EntException {
        userPreferencesDAO.deleteUserPreferences(username);
    }
    
}
