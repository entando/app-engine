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
package com.agiletec.aps.system.services.role;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.agiletec.aps.system.common.AbstractService;
import org.entando.entando.aps.system.services.tenants.RefreshableBeanTenantAware;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.role.cache.IPermissionCacheWrapper;
import com.agiletec.aps.system.services.role.cache.IRoleCacheWrapper;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

/**
 * Servizio di gestione dei ruoli.
 *
 * @author M.Diana - E.Santoboni
 */
public class RoleManager extends AbstractService implements IRoleManager, RefreshableBeanTenantAware {

	private static final EntLogger logger = EntLogFactory.getSanitizedLogger(RoleManager.class);

	private IRoleDAO roleDao;
	private IPermissionDAO permissionDao;
	private IRoleCacheWrapper roleCacheWrapper;
	private IPermissionCacheWrapper permissionCacheWrapper;

	@Override
	public void init() throws Exception {
		initTenantAware();
		logger.debug("{} : initialized", this.getClass().getName());
	}
    
    @Override
    protected void release() {
		releaseTenantAware();
        super.release();
    }

	@Override
	public void initTenantAware() throws Exception {
		this.getPermissionCacheWrapper().initCache(this.getPermissionDAO());
		this.getRoleCacheWrapper().initCache(this.getRoleDAO());
	}

	@Override
	public void releaseTenantAware() {
		this.getRoleCacheWrapper().release();
		this.getPermissionCacheWrapper().release();
	}

	/**
	 * Restituisce la lista dei ruoli esistenti.
	 *
	 * @return La lista dei ruoli esistenti.
	 */
	@Override
	public List<Role> getRoles() {
		return this.getRoleCacheWrapper().getRoles();
	}

	/**
	 * Restituisce un ruolo in base al nome.
	 *
	 * @param roleName Il nome del ruolo richesto.
	 * @return Il ruolo cercato.
	 */
	@Override
	public Role getRole(String roleName) {
		return this.getRoleCacheWrapper().getRole(roleName);
	}

	/**
	 * Rimuove un ruolo dal db e dalla mappa dei ruoli.
	 *
	 * @param role Oggetto di tipo Role relativo al ruolo da rimuovere.
	 * @throws EntException in caso di errore nell'accesso al db.
	 */
	@Override
	public void removeRole(Role role) throws EntException {
		try {
			this.getRoleDAO().deleteRole(role);
			this.getRoleCacheWrapper().removeRole(role);
		} catch (Throwable t) {
			logger.error("Error while removing a role", t);
			throw new EntException("Error while removing a role", t);
		}
	}

	/**
	 * Aggiorna un ruolo sul db ed sulla mappa dei ruoli.
	 *
	 * @param role Il ruolo da aggiornare.
	 * @throws EntException in caso di errore nell'accesso al db.
	 */
	@Override
	public void updateRole(Role role) throws EntException {
		try {
			this.getRoleDAO().updateRole(role);
			this.getRoleCacheWrapper().updateRole(role);
		} catch (Throwable t) {
			logger.error("Error while updating a role", t);
			throw new EntException("Error while updating a role", t);
		}
	}

	/**
	 * Aggiunge un ruolo al db ed alla mappa dei ruoli.
	 *
	 * @param role Oggetto di tipo Role relativo al ruolo da aggiungere.
	 * @throws EntException in caso di errore nell'accesso al db.
	 */
	@Override
	public void addRole(Role role) throws EntException {
		try {
			this.getRoleDAO().addRole(role);
			this.getRoleCacheWrapper().addRole(role);
		} catch (Throwable t) {
			logger.error("Error while adding a role", t);
			throw new EntException("Error while adding a role", t);
		}
	}

	/**
	 * Restituisce la lista ordinata (secondo il nome) dei permessi di
	 * autorizzazione.
	 *
	 * @return La lista ordinata dei permessi
	 */
	@Override
	public List<Permission> getPermissions() {
		List<Permission> permissions = this.getPermissionCacheWrapper().getPermissions();
		Collections.sort(permissions);
		return permissions;
	}

	@Override
    public List<String> getPermissionsCodes() {
        List<String> permissions = this.getPermissionCacheWrapper().getPermissionsCodes();
        Collections.sort(permissions);
        return permissions;
    }

    @Override
	public Permission getPermission(String permissionName) {
		return this.getPermissionCacheWrapper().getPermission(permissionName);
	}

	/**
	 * Rimuove il permesso specificato dal db e dai ruoli.
	 *
	 * @param permissionName Il permesso da rimuovere dal ruolo.
	 * @throws EntException in caso di errore nell'accesso al db.
	 */
	@Override
	public void removePermission(String permissionName) throws EntException {
		try {
			this.getPermissionDAO().deletePermission(permissionName);
			this.getPermissionCacheWrapper().removePermission(permissionName);
			List<Role> roles = this.getRolesWithPermission(permissionName);
			Iterator<Role> roleIt = roles.iterator();
			while (roleIt.hasNext()) {
				Role role = roleIt.next();
				role.removePermission(permissionName);
				this.getRoleCacheWrapper().updateRole(role);
			}
		} catch (Throwable t) {
			logger.error("Error while deleting permission {}", permissionName, t);
			throw new EntException("Error while deleting a permission", t);
		}
	}

	/**
	 * Aggiorna un permesso di autorizzazione nel db.
	 *
	 * @param permission Il permesso da aggiornare nel db.
	 * @throws EntException in caso di errore nell'accesso al db.
	 */
	@Override
	public void updatePermission(Permission permission) throws EntException {
		try {
			this.getPermissionDAO().updatePermission(permission);
			this.getPermissionCacheWrapper().updatePermission(permission);
		} catch (Throwable t) {
			logger.error("Error updating permission", t);
			throw new EntException("Error while updating perrmission", t);
		}
	}

	/**
	 * Aggiunge un permesso di autorizzazione nel db.
	 *
	 * @param permission Il permesso da aggiungere nel db.
	 * @throws EntException in caso di errore nell'accesso al db.
	 */
	@Override
	public void addPermission(Permission permission) throws EntException {
		try {
			this.getPermissionDAO().addPermission(permission);
			this.getPermissionCacheWrapper().addPermission(permission);
		} catch (Throwable t) {
			logger.error("Error while adding a permission", t);
			throw new EntException("Error while adding a permission", t);
		}
	}

	@Override
	public List<Role> getRolesWithPermission(String permissionName) {
		List<Role> rolesWithPerm = new ArrayList<Role>();
		Iterator<Role> iter = this.getRoles().iterator();
		while (iter.hasNext()) {
			Role role = (Role) iter.next();
			if (role.hasPermission(permissionName)) {
				rolesWithPerm.add(role);
			}
		}
		return rolesWithPerm;
	}

	protected IPermissionDAO getPermissionDAO() {
		return permissionDao;
	}

	public void setPermissionDAO(IPermissionDAO permissionDao) {
		this.permissionDao = permissionDao;
	}

	protected IRoleDAO getRoleDAO() {
		return roleDao;
	}

	public void setRoleDAO(IRoleDAO roleDao) {
		this.roleDao = roleDao;
	}

	protected IRoleCacheWrapper getRoleCacheWrapper() {
		return roleCacheWrapper;
	}

	public void setRoleCacheWrapper(IRoleCacheWrapper roleCacheWrapper) {
		this.roleCacheWrapper = roleCacheWrapper;
	}

	protected IPermissionCacheWrapper getPermissionCacheWrapper() {
		return permissionCacheWrapper;
	}

	public void setPermissionCacheWrapper(IPermissionCacheWrapper permissionCacheWrapper) {
		this.permissionCacheWrapper = permissionCacheWrapper;
	}

}
