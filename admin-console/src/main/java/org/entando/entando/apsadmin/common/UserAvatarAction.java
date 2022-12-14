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
package org.entando.entando.apsadmin.common;

import com.agiletec.aps.system.SystemConstants;

import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.apsadmin.system.BaseAction;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

import org.entando.entando.aps.system.services.userprofile.IUserProfileManager;
import org.entando.entando.aps.system.services.userprofile.model.IUserProfile;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author E.Santoboni
 */
public class UserAvatarAction extends BaseAction {

	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(UserAvatarAction.class);
	
	public String returnAvatarStream() {
		if (!this.isGravatarIntegrationEnabled()) {
			return this.extractDefaultAvatarStream();
		}
		return this.extractGravatar();
	}
	
	protected String extractGravatar() {
		try {
			IUserProfile profile = this.getUserProfile();
			if (null == profile) {
				return this.extractDefaultAvatarStream();
			}
			String email = (String) profile.getValueByRole(SystemConstants.USER_PROFILE_ATTRIBUTE_ROLE_MAIL);
			if (null == email) {
				return this.extractDefaultAvatarStream();
			}
			URL url = new URL(this.createGravatarUri(email));
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			this.setInputStream(conn.getInputStream());
		} catch (FileNotFoundException fnfe) {
			_logger.info("avatar not available", fnfe);
			return this.extractDefaultAvatarStream();
        } catch (Throwable t) {
			_logger.error("error in returnAvatarStream", t);
			return this.extractDefaultAvatarStream();
        }
		return SUCCESS;
	}
	
	private String createGravatarUri(String emailAddress) throws Throwable {
		StringBuilder sb = new StringBuilder(this.getGravatarUrl());
		sb.append(this.createMd5Hex(emailAddress));
		sb.append(".").append(this.getGravatarExtension()).append("?d=404&s=").append(this.getGravatarSize());
		return sb.toString();
	}
	
	private String createMd5Hex(String emailAddress) throws Throwable {
		StringBuilder sb = new StringBuilder();
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] array = md.digest(emailAddress.getBytes("CP1252"));
			for (int i = 0; i < array.length; ++i) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));        
			}
		} catch (Throwable t) {
			_logger.error("Error creating Md5 Hex", t);
			throw new RuntimeException("Error creating Md5 Hex", t);
		}
		return sb.toString();
	}
	
	protected String extractDefaultAvatarStream() {
		try {
			Resource resource = new ClassPathResource("avatar-default.png");
			this.setInputStream(resource.getInputStream());
		} catch (Throwable t) {
			_logger.error("error in extractDefaultAvatarStream", t);
            return FAILURE;
        }
		return SUCCESS;
	}
	
	protected IUserProfile getUserProfile() throws EntException {
		if (null == this.getUsername()) {
			return null;
		}
		return this.getUserProfileManager().getProfile(this.getUsername());
	}
	
	public String getMimeType() {
		return "image/png";
	}
	
	protected boolean isGravatarIntegrationEnabled() {
		String param = this.getUserManager().getConfig(IUserManager.CONFIG_PARAM_GRAVATAR_INTEGRATION_ENABLED);
		boolean enabled = false;
		try {
			enabled = Boolean.parseBoolean(param);
		} catch (Exception e) {}
		return enabled;
	}
	
	protected String getGravatarUrl() {
		return _gravatarUrl;
	}
	public void setGravatarUrl(String gravatarUrl) {
		this._gravatarUrl = gravatarUrl;
	}
	
	public String getGravatarExtension() {
		return _gravatarExtension;
	}
	public void setGravatarExtension(String gravatarExtension) {
		this._gravatarExtension = gravatarExtension;
	}
	
	public String getGravatarSize() {
		return _gravatarSize;
	}
	public void setGravatarSize(String gravatarSize) {
		this._gravatarSize = gravatarSize;
	}
	
	public String getUsername() {
		return _username;
	}
	public void setUsername(String username) {
		this._username = username;
	}
	
	public InputStream getInputStream() {
		return _inputStream;
	}
	public void setInputStream(InputStream inputStream) {
		this._inputStream = inputStream;
	}
	
	protected IUserProfileManager getUserProfileManager() {
		return _userProfileManager;
	}
	public void setUserProfileManager(IUserProfileManager userProfileManager) {
		this._userProfileManager = userProfileManager;
	}

	protected IUserManager getUserManager() {
		return userManager;
	}
	public void setUserManager(IUserManager userManager) {
		this.userManager = userManager;
	}

	private String _gravatarUrl;
	
	private String _gravatarExtension = "png";
	private String _gravatarSize = "56";
	
	private String _username;
	private InputStream _inputStream;
	
	private IUserProfileManager _userProfileManager;
	private transient IUserManager userManager;
	
}
