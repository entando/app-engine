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
package com.agiletec.aps.system.services.group;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.agiletec.aps.system.common.AbstractService;
import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.common.model.dao.SearcherDaoPaginatedResult;
import org.entando.entando.aps.system.services.tenants.RefreshableBeanTenantAware;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.group.cache.IGroupManagerCacheWrapper;
import java.util.Collections;
import org.apache.commons.beanutils.BeanComparator;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

/**
 * Servizio gestore dei gruppi.
 *
 * @author E.Santoboni Modificato da emarcias
 */
public class GroupManager extends AbstractService implements IGroupManager, RefreshableBeanTenantAware {

    private final EntLogger logger = EntLogFactory.getSanitizedLogger(getClass());

    private IGroupDAO groupDao;
    private IGroupManagerCacheWrapper cacheWrapper;

    protected IGroupDAO getGroupDAO() {
        return this.groupDao;
    }

    public void setGroupDAO(IGroupDAO groupDao) {
        this.groupDao = groupDao;
    }

    protected IGroupManagerCacheWrapper getCacheWrapper() {
        return cacheWrapper;
    }

    public void setCacheWrapper(IGroupManagerCacheWrapper cacheWrapper) {
        this.cacheWrapper = cacheWrapper;
    }

    @Override
    public void init() throws Exception {
        initTenantAware();
        logger.debug("{} ready. Initialized", this.getClass().getName());
    }
    
    @Override
    protected void release() {
        releaseTenantAware();
        super.release();
    }

    @Override
    public void initTenantAware() throws Exception {
        this.getCacheWrapper().initCache(this.getGroupDAO());
    }

    @Override
    public void releaseTenantAware() {
        this.getCacheWrapper().release();
    }

    /**
     * Aggiunge un gruppo nel sistema.
     *
     * @param group Il gruppo da aggiungere.
     * @throws EntException In caso di errori in accesso al db.
     */
    @Override
    public void addGroup(Group group) throws EntException {
        try {
            this.getGroupDAO().addGroup(group);
            this.getCacheWrapper().addGroup(group);
        } catch (Throwable t) {
            logger.error("Error detected while adding a group", t);
            throw new EntException("Error detected while adding a group", t);
        }
    }

    /**
     * Rimuove un gruppo dal sistema.
     *
     * @param group Il gruppo da rimuovere.
     * @throws EntException In caso di errori in accesso al db.
     */
    @Override
    public void removeGroup(Group group) throws EntException {
        try {
            this.getGroupDAO().deleteGroup(group);
            this.getCacheWrapper().removeGroup(group);
        } catch (Throwable t) {
            logger.error("Error while removing a group", t);
            throw new EntException("Error while removing a group", t);
        }
    }

    /**
     * Aggiorna un gruppo di sistema.
     *
     * @param group Il gruppo da aggiornare.
     * @throws EntException In caso di errori in accesso al db.
     */
    @Override
    public void updateGroup(Group group) throws EntException {
        try {
            this.getGroupDAO().updateGroup(group);
            this.getCacheWrapper().updateGroup(group);
        } catch (Throwable t) {
            logger.error("Error updating a group", t);
            throw new EntException("Error updating a group", t);
        }
    }

    /**
     * Restituisce la lista dei gruppi presenti nel sistema.
     *
     * @return La lista dei gruppi presenti nel sistema.
     */
    @Override
    public List<Group> getGroups() {
        List<Group> groups = new ArrayList<Group>(this.getGroupsMap().values());
        BeanComparator c = new BeanComparator("description");
        Collections.sort(groups, c);
        return groups;
    }

    /**
     * Restituisce la mappa dei gruppi presenti nel sistema. La mappa è indicizzata in base al nome del gruppo.
     *
     * @return La mappa dei gruppi presenti nel sistema.
     */
    @Override
    public Map<String, Group> getGroupsMap() {
        return this.getCacheWrapper().getGroups();
    }

    /**
     * Restituisce un gruppo in base al nome.
     *
     * @param groupName Il nome del gruppo.
     * @return Il gruppo cercato.
     */
    @Override
    public Group getGroup(String groupName) {
        return this.getCacheWrapper().getGroup(groupName);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public SearcherDaoPaginatedResult<Group> getGroups(FieldSearchFilter[] filters) throws EntException {
        SearcherDaoPaginatedResult<Group> pagedResult = null;
        try {
            List<Group> groups = new ArrayList<>();
            int count = this.getGroupDAO().countGroups(filters);

            List<String> groupNames = this.getGroupDAO().searchGroups(filters);
            for (String groupName : groupNames) {
                groups.add(this.getGroup(groupName));
            }
            pagedResult = new SearcherDaoPaginatedResult<Group>(count, groups);
        } catch (Throwable t) {
            logger.error("Error searching groups", t);
            throw new EntException("Error searching groups", t);
        }
        return pagedResult;
    }

    @Override
    public SearcherDaoPaginatedResult<Group> getGroups(List<FieldSearchFilter> filters) throws EntException {
        FieldSearchFilter[] array = null;
        if (null != filters) {
            array = filters.toArray(new FieldSearchFilter[filters.size()]);
        }
        return this.getGroups(array);
    }

}
