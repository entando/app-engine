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
package org.entando.entando.web.userpreferences.model;

import static org.entando.entando.aps.system.services.userpreferences.UserPreferencesService.DEFAULT_JOIN_GROUP_DELIMITER;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.userpreferences.UserPreferences;

@Getter@Setter
public class UserPreferencesDto {
    
    private Boolean wizard;
    private Boolean loadOnPageSelect;
    private Boolean translationWarning;
    private String defaultPageOwnerGroup;
    private List<String> defaultPageJoinGroups;
    private String defaultContentOwnerGroup;
    private List<String> defaultContentJoinGroups;
    private String defaultWidgetOwnerGroup;
    private List<String> defaultWidgetJoinGroups;
    private Boolean disableContentMenu;
    private Boolean gravatar;

    public UserPreferencesDto(UserPreferences userPreferences) {
        wizard = userPreferences.isWizard();
        loadOnPageSelect = userPreferences.isLoadOnPageSelect();
        translationWarning = userPreferences.isTranslationWarning();
        disableContentMenu = userPreferences.getDisableContentMenu();

        defaultPageOwnerGroup = userPreferences.getDefaultPageOwnerGroup();
        String defaultJoinPageGroupsString = userPreferences.getDefaultPageJoinGroups();
        if (defaultJoinPageGroupsString != null) {
            if (defaultPageJoinGroups == null) {
                defaultPageJoinGroups = new ArrayList<>();
            }
            if (!StringUtils.isEmpty(defaultJoinPageGroupsString)) {
                for (String group : userPreferences.getDefaultPageJoinGroups().split(DEFAULT_JOIN_GROUP_DELIMITER)) {
                    defaultPageJoinGroups.add(group);
                }
            }
        }

        defaultContentOwnerGroup = userPreferences.getDefaultContentOwnerGroup();
        String defaultJoinContentGroupsString = userPreferences.getDefaultContentJoinGroups();
        if (defaultJoinContentGroupsString != null) {
            if (defaultContentJoinGroups == null) {
                defaultContentJoinGroups = new ArrayList<>();
            }
            if (!StringUtils.isEmpty(defaultJoinContentGroupsString)) {
                for (String group : userPreferences.getDefaultContentJoinGroups().split(DEFAULT_JOIN_GROUP_DELIMITER)) {
                    defaultContentJoinGroups.add(group);
                }
            }
        }

        defaultWidgetOwnerGroup = userPreferences.getDefaultWidgetOwnerGroup();
        String defaultJoinWidgetGroupsString = userPreferences.getDefaultWidgetJoinGroups();
        if (defaultJoinWidgetGroupsString != null) {
            if (defaultWidgetJoinGroups == null) {
                defaultWidgetJoinGroups = new ArrayList<>();
            }
            if (!StringUtils.isEmpty(defaultJoinWidgetGroupsString)) {
                for (String group : userPreferences.getDefaultWidgetJoinGroups().split(DEFAULT_JOIN_GROUP_DELIMITER)) {
                    defaultWidgetJoinGroups.add(group);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "UserPreferencesDto{" +
                "wizard=" + wizard +
                ", loadOnPageSelect=" + loadOnPageSelect +
                ", translationWarning=" + translationWarning +
                ", defaultPageOwnerGroup='" + defaultPageOwnerGroup + '\'' +
                ", defaultPageJoinGroups=" + defaultPageJoinGroups +
                ", defaultContentOwnerGroup='" + defaultContentOwnerGroup + '\'' +
                ", defaultContentJoinGroups=" + defaultContentJoinGroups +
                ", defaultWidgetOwnerGroup='" + defaultWidgetOwnerGroup + '\'' +
                ", defaultWidgetJoinGroups=" + defaultWidgetJoinGroups +
                ", disableContentMenu=" + disableContentMenu +
                '}';
    }
}
