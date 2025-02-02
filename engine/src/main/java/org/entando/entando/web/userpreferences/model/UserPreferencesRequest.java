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

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter@Setter
public class UserPreferencesRequest {

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

    @Override
    public String toString() {
        return "UserPreferencesRequest{" +
                "wizard=" + wizard +
                ", loadOnPageSelect=" + loadOnPageSelect +
                ", translationWarning=" + translationWarning +
                ", defaultPageOwnerGroup='" + defaultPageOwnerGroup + '\'' +
                ", defaultPageJoinGroups=" + defaultPageJoinGroups +
                ", defaultContentOwnerGroup='" + defaultContentOwnerGroup + '\'' +
                ", defaultContentJoinGroups=" + defaultContentJoinGroups +
                ", defaultWidgetOwnerGroup='" + defaultWidgetOwnerGroup + '\'' +
                ", defaultWidgetJoinGroups=" + defaultWidgetJoinGroups + '\'' +
                ", disableContentMenu=" + disableContentMenu + '\'' +
                ", gravatar=" + gravatar +
                '}';
    }
}
