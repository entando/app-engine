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
package com.agiletec.aps.system.common.entity;

import com.agiletec.aps.system.common.IManager;
import java.util.List;
import java.util.Map;

import com.agiletec.aps.system.common.entity.model.ApsEntityRecord;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.SmallEntityType;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeRole;
import org.entando.entando.ent.exception.EntException;

/**
 * Base interface for the entity managers.
 *
 * @author E.Santoboni
 */
public interface IEntityManager extends IManager {

    /**
     * Return the entity class handled by the manager.
     *
     * @return The class of the managed entity.
     */
    public Class getEntityClass();

    public String getConfigItem() throws EntException;

    /**
     * Search entities.
     *
     * @param filters The filters used to find an sort the entities IDs that
     * match the given criteria.
     * @return The list of the IDs found.
     * @throws EntException In case of error.
     */
    public List<String> searchId(EntitySearchFilter[] filters) throws EntException;

    /**
     * Search entities.
     *
     * @param typeCode The code of the Entity Types to look for.
     * @param filters The search filters to apply to find and sort the ID found.
     * @return The list of the ID found.
     * @throws EntException In case of error.
     */
    public List<String> searchId(String typeCode, EntitySearchFilter[] filters) throws EntException;

    /**
     * Search the entity record
     *
     * @param filters The filters applied to
     * @return a list of entity records
     * @throws EntException
     */
    public List<ApsEntityRecord> searchRecords(EntitySearchFilter[] filters) throws EntException;

    /**
     * How this manager addresses nested attributes in its search table: the width of its
     * {@code attrname} column, which attribute types may be reached by a path, and how a path becomes a
     * key.
     *
     * <p>Each manager owns its own search table, so the length bound is a property of <i>this</i>
     * manager rather than of the platform. The default is
     * {@link DefaultEntitySearchKeyStrategy#INSTANCE}, which matches the tables the platform ships; a
     * manager whose table differs - including one in a custom project - returns its own.</p>
     *
     * @return the search key strategy; never null.
     */
    default IEntitySearchKeyStrategy getSearchKeyStrategy() {
        return DefaultEntitySearchKeyStrategy.INSTANCE;
    }

    /**
     * What the given entity type offers to a search: the attributes a form can filter on, their labels,
     * the key each is addressed by, and the defects of that key set.
     *
     * <p>All of it is a pure function of the type, so an implementation is expected to compute it once
     * per type and keep it - {@link ApsEntityManager} does. The default computes it on demand, which
     * keeps any other implementation of this interface working.</p>
     *
     * @param typeCode the entity type code.
     * @return the schema; never null, empty when the type does not exist.
     */
    default EntitySearchSchema getSearchSchema(String typeCode) {
        return EntitySearchSchema.build(this.getEntityPrototype(typeCode), this.getSearchKeyStrategy());
    }

    /**
     * Create an object from the prototype.
     *
     * @param typeCode The type of the prototype to return.
     * @return The object created from the prototype.
     */
    public IApsEntity getEntityPrototype(String typeCode);

    /**
     * Get the entity identified by its ID.
     *
     * @param entityId The ID of the entity.
     * @return The requested entity.
     * @throws EntException In case of error.
     */
    public IApsEntity getEntity(String entityId) throws EntException;

    /**
     * Return the map of entity prototypes.
     *
     * @return The map of entity prototypes, indexed by the Entity Type
     */
    public Map<String, IApsEntity> getEntityPrototypes();

    /**
     * Get the prototype of the Entity Attributes.
     *
     * @return The map of the attribute prototypes, indxed
     */
    public Map<String, AttributeInterface> getEntityAttributePrototypes();

    public List<SmallEntityType> getSmallEntityTypes();

    /**
     * Check if the service uses the search engine or not.
     *
     * @return true if the service uses the search engine, false otherwise.
     */
    public boolean isSearchEngineUser();

    public Thread reloadEntitiesReferences(String typeCode);

    public int getStatus(String typeCode);

    public int getStatus();

    public Map<String, String> getAttributeDisablingCodes();

    public List<AttributeRole> getAttributeRoles();

    public AttributeRole getAttributeRole(String roleName);

    public static final String ENTITY_ID_FILTER_KEY = "entityId";

    public static final String ENTITY_TYPE_CODE_FILTER_KEY = "typeCode";

    public static final int STATUS_READY = 0;
    public static final int STATUS_RELOADING_REFERENCES_IN_PROGRESS = 1;
    public static final int STATUS_NEED_TO_RELOAD_REFERENCES = 2;

    public static final String DEFAULT_ATTRIBUTE_ROLES_FILE_NAME = "attributeRoles.xml";

    public static final String DEFAULT_ATTRIBUTE_DISABLING_CODES_FILE_NAME = "attributeDisablingCodes.xml";

}
