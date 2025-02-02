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
package org.entando.entando.aps.system.services.userprofile;

import static org.mockito.Mockito.when;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.entity.model.attribute.MonoTextAttribute;
import com.agiletec.aps.system.common.entity.parse.attribute.MonoTextAttributeHandler;
import com.agiletec.aps.system.services.user.User;
import org.entando.entando.aps.system.services.userpreferences.IUserPreferencesManager;
import org.entando.entando.aps.system.services.userprofile.model.IUserProfile;
import org.entando.entando.aps.system.services.userprofile.model.UserProfile;
import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserManagementAspectTest {

	@InjectMocks
	private UserManagementAspect userManagementAspect;
    
	@Mock
	private UserProfileManager userProfileManager;
    @Mock
    private IAvatarService avatarService;
    @Mock
    private IUserPreferencesManager userPreferencesManager;
    
	@Test
    void testInjectProfile() throws EntException {
        IUserProfile returned = this.createFakeProfile("test", SystemConstants.DEFAULT_PROFILE_TYPE_CODE);
        when(userProfileManager.getProfile(Mockito.anyString())).thenReturn(returned);
        
        User user = new User();
        user.setUsername("test");
        Assertions.assertNull(user.getProfile());
        
        userManagementAspect.injectProfile(user);
        Mockito.verify(userProfileManager, Mockito.times(1)).getProfile("test");
        IUserProfile profile = (IUserProfile) user.getProfile();
        Assertions.assertNotNull(profile);
        Assertions.assertEquals("test", profile.getUsername());
    }
    
    @Test
    void testInjectProfileWithError() throws EntException {
        when(userProfileManager.getProfile(Mockito.anyString())).thenThrow(EntException.class);
        
        User user = new User();
        user.setUsername("test");
        Assertions.assertNull(user.getProfile());
        
        userManagementAspect.injectProfile(user);
        Mockito.verify(userProfileManager, Mockito.times(1)).getProfile("test");
        Assertions.assertNull(user.getProfile());
    }
    
    @Test
    void testInjectProfileWithNullUser() throws EntException {
        userManagementAspect.injectProfile(null);
        Mockito.verifyNoInteractions(userProfileManager);
    }
    
    @Test
    void testMissingInjectProfile() throws EntException {
        IUserProfile profile = this.createFakeProfile("test", SystemConstants.DEFAULT_PROFILE_TYPE_CODE);
        User user = new User();
        user.setUsername("test");
        user.setProfile(profile);
        
        userManagementAspect.injectProfile(user);
        Mockito.verify(userProfileManager, Mockito.times(0)).getProfile("test");
        Assertions.assertNotNull(user.getProfile());
        Assertions.assertSame(profile, user.getProfile());
    }
    
    @Test
    void testAddProfile() throws EntException {
        IUserProfile profile = this.createFakeProfile("test", SystemConstants.DEFAULT_PROFILE_TYPE_CODE);
        User user = new User();
        user.setUsername("test");
        user.setProfile(profile);
        userManagementAspect.addProfile(user);
        Mockito.verify(userProfileManager, Mockito.times(1)).addProfile("test", profile);
    }
    
    @Test
    void testAddProfileWithNullUser() throws EntException {
        userManagementAspect.addProfile(null);
        Mockito.verifyNoInteractions(userProfileManager);
    }
    
    @Test
    void testInvokeAddProfileWithUserWithNullProfile() throws EntException {
        User user = new User();
        user.setUsername("test");
        userManagementAspect.addProfile(user);
        Mockito.verifyNoInteractions(userProfileManager);
    }
    
    @Test
    void testInvokeAddProfileWithError() {
        try {
            Mockito.doThrow(EntException.class).when(userProfileManager).addProfile(Mockito.anyString(), Mockito.any());
            User user = new User();
            user.setUsername("test");
            user.setProfile(Mockito.mock(IUserProfile.class));
            userManagementAspect.addProfile(user);
            Mockito.verify(userProfileManager, Mockito.times(1)).addProfile(Mockito.anyString(), Mockito.any(IUserProfile.class));
        } catch (Exception e) {
            Assertions.fail();
        }
    }
    
    @Test
    void testUpdateProfile() throws EntException {
        IUserProfile profile = this.createFakeProfile("test", SystemConstants.DEFAULT_PROFILE_TYPE_CODE);
        User user = new User();
        user.setUsername("test");
        user.setProfile(profile);
        userManagementAspect.updateProfile(user);
        Mockito.verify(userProfileManager, Mockito.times(1)).updateProfile("test", profile);
    }
    
    @Test
    void testUpdateProfileWithNullUSer() throws EntException {
        userManagementAspect.updateProfile(null);
        Mockito.verifyNoInteractions(userProfileManager);
    }
    
    @Test
    void testInvokeUpdateProfileWithUserWithNullProfile() throws EntException {
        User user = new User();
        user.setUsername("test");
        userManagementAspect.updateProfile(user);
        Mockito.verifyNoInteractions(userProfileManager);
    }
    
    @Test
    void testInvokeUpdateProfileWithError() {
        try {
            Mockito.doThrow(EntException.class).when(userProfileManager).updateProfile(Mockito.anyString(), Mockito.any());
            User user = new User();
            user.setUsername("test");
            user.setProfile(Mockito.mock(IUserProfile.class));
            userManagementAspect.updateProfile(user);
            Mockito.verify(userProfileManager, Mockito.times(1)).updateProfile(Mockito.anyString(), Mockito.any(IUserProfile.class));
        } catch (Exception e) {
            Assertions.fail();
        }
    }
    
    @Test
    void testDeleteUserDataFromUser() throws EntException {
        User user = new User();
        user.setUsername("test");
        userManagementAspect.deleteUserData(user);
        Mockito.verify(userProfileManager, Mockito.times(1)).deleteProfile("test");
        Mockito.verify(userPreferencesManager, Mockito.times(1)).deleteUserPreferences("test");
        Mockito.verify(avatarService, Mockito.times(1)).deleteAvatar("test");
    }
    
    @Test
    void testDeleteUserDataFromUsername() throws EntException {
        userManagementAspect.deleteUserData("test");
        Mockito.verify(userProfileManager, Mockito.times(1)).deleteProfile("test");
        Mockito.verify(userPreferencesManager, Mockito.times(1)).deleteUserPreferences("test");
        Mockito.verify(avatarService, Mockito.times(1)).deleteAvatar("test");
    }
    
    @Test
    void testDeleteUserDataWithNullUsername() throws EntException {
        userManagementAspect.deleteUserData(null);
        Mockito.verifyNoInteractions(userProfileManager);
        Mockito.verifyNoInteractions(userPreferencesManager);
        Mockito.verifyNoInteractions(avatarService);
    }
    
    @Test
    void testDeleteUserDataWithErrorOnUserProfileManager() {
        try {
            Mockito.doThrow(EntException.class).when(userProfileManager).deleteProfile(Mockito.anyString());
            userManagementAspect.deleteUserData("username_error1");
            Mockito.verify(userProfileManager, Mockito.times(1)).deleteProfile("username_error1");
            Mockito.verify(userPreferencesManager, Mockito.times(1)).deleteUserPreferences("username_error1");
            Mockito.verify(avatarService, Mockito.times(1)).deleteAvatar("username_error1");
        } catch (Exception e) {
            Assertions.fail();
        }
    }
    
    @Test
    void testDeleteUserDataWithErrorOnUserAvatarService() {
        try {
            Mockito.doThrow(EntException.class).when(avatarService).deleteAvatar(Mockito.anyString());
            userManagementAspect.deleteUserData("username_error2");
            Mockito.verify(userProfileManager, Mockito.times(1)).deleteProfile("username_error2");
            Mockito.verify(userPreferencesManager, Mockito.times(1)).deleteUserPreferences("username_error2");
            Mockito.verify(avatarService, Mockito.times(1)).deleteAvatar("username_error2");
        } catch (Exception e) {
            Assertions.fail();
        }
    }
    
    @Test
    void testDeleteUserDataWithErrorOnUserPreference() {
        try {
            Mockito.doThrow(EntException.class).when(userPreferencesManager).deleteUserPreferences(Mockito.anyString());
            userManagementAspect.deleteUserData("username_error3");
            Mockito.verify(userProfileManager, Mockito.times(1)).deleteProfile("username_error3");
            Mockito.verify(userPreferencesManager, Mockito.times(1)).deleteUserPreferences("username_error3");
            Mockito.verify(avatarService, Mockito.times(1)).deleteAvatar("username_error3");
        } catch (Exception e) {
            Assertions.fail();
        }
    }

	private IUserProfile createFakeProfile(String username, String defaultProfileTypeCode) {
		UserProfile userProfile = new UserProfile();
        userProfile.setId(username);
		MonoTextAttribute monoTextAttribute = new MonoTextAttribute();
		monoTextAttribute.setName("Name");
		monoTextAttribute.setHandler(new MonoTextAttributeHandler());
		userProfile.addAttribute(monoTextAttribute);
		userProfile.setTypeCode(defaultProfileTypeCode);
		return userProfile;
	}
    
}
