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
package com.agiletec.plugins.jacms.apsadmin.content;

import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.aps.system.services.group.Group;
import com.agiletec.apsadmin.system.ApsAdminSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import java.util.Arrays;
import org.apache.commons.lang.StringUtils;
import org.entando.entando.aps.system.services.userpreferences.IUserPreferencesManager;
import org.entando.entando.aps.system.services.userpreferences.UserPreferences;

/**
 * Action gestore delle operazioni di creazione nuovo contenuto.
 * @author E.Santoboni
 */
public class IntroNewContentAction extends AbstractContentAction {

	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(IntroNewContentAction.class);
	
	/**
	 * Punto di ingresso della redazione nuovo contenuto.
	 * Apre l'interfaccia per la scelta del tipo di contenuto 
	 * da gestire con gli altri campi standard di Descrizione e Stato
	 * @return Il risultato dell'azione.
	 */
	public String openNew() {
		try {
			this.setContentStatus(Content.STATUS_DRAFT);
			if (this.getAuthorizationManager().isAuthOnGroup(this.getCurrentUser(), Group.FREE_GROUP_NAME)) {
				this.setContentMainGroup(Group.FREE_GROUP_NAME);
			}
		} catch (Throwable t) {
			_logger.error("error in openNew", t);
			return FAILURE;
		}
		return SUCCESS;
	}
	
	/**
	 * Crea e mette in sessione un nuovo contenuto del tipo richiesto.
	 * @return Il risultato dell'azione.
	 */
	public String createNewVoid() {
		try {
			Content prototype = this.getContentManager().createContentType(this.getContentTypeCode());
			prototype.setDescription(this.getContentDescription());
			prototype.setStatus(this.getContentStatus());
			prototype.setMainGroup(this.getContentMainGroup());
			prototype.setFirstEditor(this.getCurrentUser().getUsername());
			this.fillSessionAttribute(prototype);
		} catch (Throwable t) {
			_logger.error("error in createNewVoid", t);
			return FAILURE;
		}
		return SUCCESS;
	}
	
    public String createNew() {
        try {
            _logger.debug("Create new content");
            String username = getCurrentUser().getUsername();
            Content prototype = this.getContentManager().createContentType(this.getContentTypeCode());
            if (null == prototype) {
                this.addFieldError("contentTypeCode", this.getText("error.content.type.invalid"));
                _logger.debug("Invalid content type");
                return INPUT;
            }
            prototype.setFirstEditor(username);
            UserPreferences pref = this.getUserPreferencesManager().getUserPreferences(username);
            if (pref != null) {
                String defaultContentOwnerGroup = pref.getDefaultContentOwnerGroup();
                String defaultContentJoinGroups = pref.getDefaultContentJoinGroups();
                if (StringUtils.isNotBlank(defaultContentJoinGroups)) {
                    String[] joinGroup = defaultContentJoinGroups.split(";");
                    Arrays.stream(joinGroup).filter(c -> null != this.getGroup(c))
                            .forEach(g -> {
                                _logger.info("adding join group {} from user {} preferences", g, username);
                                prototype.addGroup(g);
                            });
                }
                if (StringUtils.isNotBlank(defaultContentOwnerGroup) && null != this.getGroup(defaultContentOwnerGroup)) {
                    prototype.setMainGroup(defaultContentOwnerGroup);
                    _logger.info("setting ownerGroup to {}", defaultContentOwnerGroup);
                }
            }
            this.fillSessionAttribute(prototype);
        } catch (Throwable t) {
            _logger.error("error in createNew", t);
            return FAILURE;
        }
        return SUCCESS;
    }
	
	protected void fillSessionAttribute(Content prototype) {
		if (this.getAuthorizationManager().isAuthOnGroup(this.getCurrentUser(), Group.FREE_GROUP_NAME)) {
			this.getRequest().getSession().setAttribute(ContentActionConstants.SESSION_PARAM_NAME_CURRENT_CONTENT_GROUP, Group.FREE_GROUP_NAME);
		}
		String marker = buildContentOnSessionMarker(prototype, ApsAdminSystemConstants.ADD);
		super.setContentOnSessionMarker(marker);
		this.getRequest().getSession().setAttribute(ContentActionConstants.SESSION_PARAM_NAME_CURRENT_CONTENT_PREXIX + marker, prototype);
		_logger.debug("Created ed inserted on session content prototype of type {}", prototype.getTypeCode());
	}
	
	/**
	 * Restituisce il tipo (codice) del contenuto.
	 * @return Il tipo (codice) del contenuto.
	 */
	public String getContentTypeCode() {
		return _contentTypeCode;
	}
	
	/**
	 * Setta il tipo (codice) del contenuto.
	 * @param contentTypeCode Il tipo (codice) del contenuto.
	 */
	public void setContentTypeCode(String contentTypeCode) {
		this._contentTypeCode = contentTypeCode;
	}
	
	/**
	 * Restituisce la descrizione del contenuto.
	 * @return La descrizione del contenuto.
	 */
	public String getContentDescription() {
		return _contentDescription;
	}
	
	/**
	 * Setta la descrizione del contenuto.
	 * @param contentDescription La descrizione del contenuto.
	 */
	public void setContentDescription(String contentDescription) {
		this._contentDescription = contentDescription;
	}
	
	public String getContentMainGroup() {
		return _contentMainGroup;
	}
	
	public void setContentMainGroup(String contentMainGroup) {
		this._contentMainGroup = contentMainGroup;
	}
	
	/**
	 * Restituisce lo stato del contenuto.
	 * @return Lo stato del contenuto.
	 */
	public String getContentStatus() {
		return _contentStatus;
	}
	
	/**
	 * Setta lo stato del contenuto.
	 * @param contentStatus Lo stato del contenuto.
	 */
	public void setContentStatus(String contentStatus) {
		this._contentStatus = contentStatus;
	}
    
    public IUserPreferencesManager getUserPreferencesManager() {
        return userPreferencesManager;
    }
    public void setUserPreferencesManager(IUserPreferencesManager userPreferencesManager) {
        this.userPreferencesManager = userPreferencesManager;
    }
	
	private String _contentTypeCode;
	private String _contentDescription;
	private String _contentMainGroup;
	private String _contentStatus;
    
    private transient IUserPreferencesManager userPreferencesManager;
	
}
