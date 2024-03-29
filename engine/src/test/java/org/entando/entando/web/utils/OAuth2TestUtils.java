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
package org.entando.entando.web.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.services.authorization.Authorization;
import com.agiletec.aps.system.services.authorization.AuthorizationManager;
import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.role.Permission;
import com.agiletec.aps.system.services.role.Role;
import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import com.agiletec.aps.system.services.user.User;
import com.agiletec.aps.system.services.user.UserDetails;
import java.util.Calendar;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.oauth2.IApiOAuth2TokenManager;
import org.entando.entando.aps.system.services.oauth2.model.OAuth2AccessTokenImpl;
import org.entando.entando.aps.system.services.userprofile.model.IUserProfile;
import org.entando.entando.ent.exception.EntException;
import org.mockito.Mockito;
import org.springframework.security.oauth2.common.DefaultOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

public class OAuth2TestUtils {

    public static OAuth2AccessToken getOAuth2Token(String username, String accessToken) {
        OAuth2AccessTokenImpl oAuth2Token = new OAuth2AccessTokenImpl(accessToken);
        oAuth2Token.setRefreshToken(new DefaultOAuth2RefreshToken("refresh_token"));
        oAuth2Token.setLocalUser(username);
        Calendar calendar = Calendar.getInstance(); // gets a calendar using the default time zone and locale.
        calendar.add(Calendar.SECOND, 3600);
        oAuth2Token.setExpiration(calendar.getTime());
        oAuth2Token.setGrantType("password");
        return oAuth2Token;
    }

    public static String getValidAccessToken() {
        return getAccessToken(true);
    }

    public static String getAccessToken(boolean valid) {
        if (valid) {
            return "valid_token";
        }
        return "wrong_token";
    }

    public static User createMockUser(String username, String pws) {
        User rawUser = new User();
        rawUser.setUsername(username);
        rawUser.setPassword(pws);
        return rawUser;
    }

    public static void addAuthorization(User user, String groupName, String roleName, String[] permissions) {
        Group group = null;
        if (StringUtils.isNotBlank(groupName)) {
            group = new Group();
            group.setName(groupName);
            group.setDescription(groupName + " descr");
        }
        Role role = null;
        if (StringUtils.isNotBlank(roleName)) {
            role = new Role();
            role.setName(roleName);
            role.setDescription(roleName + " descr");
            if (null != permissions) {
                for (String permissionName : permissions) {
                    role.addPermission(permissionName);
                }
            }
        }
        Authorization auth = new Authorization(group, role);
        user.addAuthorization(auth);
    }

    public static String mockOAuthInterceptor(IApiOAuth2TokenManager apiOAuth2TokenManager,
            IAuthenticationProviderManager authenticationProviderManager,
            IAuthorizationManager authorizationManager,
            UserDetails user) {
        try {
            String accessToken = OAuth2TestUtils.getValidAccessToken();
            when(apiOAuth2TokenManager.readAccessToken(Mockito.anyString())).thenReturn(OAuth2TestUtils.getOAuth2Token(user.getUsername(), accessToken));
            when(authenticationProviderManager.getUser(user.getUsername())).thenReturn(user);
            Mockito.lenient().when(authorizationManager.isAuthOnPermission(any(UserDetails.class), anyString())).then(invocation -> {
                UserDetails user1 = (UserDetails) invocation.getArguments()[0];
                String permissionName = (String) invocation.getArguments()[1];
                return new AuthorizationManager().isAuthOnPermission(user1, permissionName);
            });
            return accessToken;
        } catch (EntException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static class UserBuilder {

        private User user;

        public UserBuilder(String username, String password) {
            this.user = OAuth2TestUtils.createMockUser(username, password);
        }

        public UserBuilder withGroup(String groupName) {
            OAuth2TestUtils.addAuthorization(this.user, groupName, null, null);
            return this;
        }

        public UserBuilder withAuthorization(String groupName, String roleName, String... permissions) {
            OAuth2TestUtils.addAuthorization(this.user, groupName, roleName, permissions);
            return this;
        }

        public UserBuilder grantedToRoleAdmin() {

            OAuth2TestUtils.addAuthorization(this.user, "administrators", "admin", new String[]{Permission.SUPERUSER});
            return this;
        }

        public UserBuilder withUserProfile(IUserProfile userProfile) {
            this.user.setProfile(userProfile);
            return this;
        }

        public User build() {
            return user;
        }

    }

}
