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
package com.agiletec.apsadmin.common;

import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;


import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.apsadmin.system.BaseAction;

/**
 * Action specifica per la gestione della password utente corrente.
 * @author E.Santoboni
 */
public class BaseCommonAction extends BaseAction {

	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(BaseCommonAction.class);
	
	@Override
	public void validate() {
		super.validate();
		if (!this.getFieldErrors().isEmpty()) {
			return;
		}
		try {
			UserDetails currentUser = this.getCurrentUser();
			if (!currentUser.isEntandoUser()) {
				this.addFieldError("username", this.getText("error.user.changePassword.currentUserNotLocal"));
			} else if (null == this.getUserManager().getUser(currentUser.getUsername(), this.getOldPassword())) {
				this.addFieldError("oldPassword", this.getText("error.user.changePassword.wrongOldPassword"));
			}
		} catch (EntException e) {
			throw new RuntimeException("Error extracting user", e);
		}
	}
	
	public String intro() {
		return SUCCESS;
	}
	
	public String editPassword() {
		return SUCCESS;
	}
	
	public String changePassword() {
		try {
			this.getUserManager().changePassword(this.getUsername(), this.getPassword());
			this.addActionMessage(this.getText("message.passwordChanged"));
		} catch (Throwable t) {
			_logger.error("error in changePassword", t);
			return FAILURE;
		}
		return SUCCESS;
	}

	/**
	 * Portal action {@code /do/Front/CurrentUser/edit}.
	 * @deprecated no widget uses this action; use {@code /do/Front/CurrentUser/Profile/edit}.
	 * To be removed in a future release.
	 */
	@Deprecated(forRemoval = true)
	public String frontEdit() {
		logDeprecatedFrontAction("edit", "/do/Front/CurrentUser/Profile/edit");
		return SUCCESS;
	}

	/**
	 * Portal action {@code /do/Front/CurrentUser/editPassword}.
	 * @deprecated no widget uses this action; the password is changed in the identity provider.
	 * To be removed in a future release.
	 */
	@Deprecated(forRemoval = true)
	public String frontEditPassword() {
		logDeprecatedFrontAction("editPassword", "the identity provider");
		return this.editPassword();
	}

	/**
	 * Portal action {@code /do/Front/CurrentUser/changePassword}.
	 * @deprecated no widget uses this action; the password is changed in the identity provider.
	 * To be removed in a future release.
	 */
	@Deprecated(forRemoval = true)
	public String frontChangePassword() {
		logDeprecatedFrontAction("changePassword", "the identity provider");
		return this.changePassword();
	}

	private static void logDeprecatedFrontAction(String actionName, String replacement) {
		_logger.warn("Deprecated action '/do/Front/CurrentUser/{}' invoked; it will be removed in a future release. Use {} instead.",
				actionName, replacement);
	}

	public String getUsername() {
		return this.getCurrentUser().getUsername();
	}
	
	public String getOldPassword() {
		return _oldPassword;
	}
	public void setOldPassword(String oldPassword) {
		this._oldPassword = oldPassword;
	}
	
	public String getPassword() {
		return _password;
	}
	public void setPassword(String password) {
		this._password = password;
	}
	
	public String getPasswordConfirm() {
		return _passwordConfirm;
	}
	public void setPasswordConfirm(String passwordConfirm) {
		this._passwordConfirm = passwordConfirm;
	}
	
	protected IUserManager getUserManager() {
		return _userManager;
	}
	public void setUserManager(IUserManager userManager) {
		this._userManager = userManager;
	}
	
	private IUserManager _userManager;
	
	private String _oldPassword;
	private String _password;
	private String _passwordConfirm;
	
}