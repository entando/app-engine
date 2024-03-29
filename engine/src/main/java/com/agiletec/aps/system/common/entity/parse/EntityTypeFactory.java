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
package com.agiletec.aps.system.common.entity.parse;

import java.util.Map;

import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.aps.system.common.entity.ApsEntityManager;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.SmallEntityType;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeRole;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import java.util.List;

/**
 * This class, which serves the ApsEntity managers, is used to obtain the Entity Types.
 * This class is utilized by default in the Spring bean configuration that defines the base
 * {@link ApsEntityManager} Entity Manager
 * @author E.Santoboni
 */
public class EntityTypeFactory implements IEntityTypeFactory {

	private static final EntLogger logger = EntLogFactory.getSanitizedLogger(EntityTypeFactory.class);
	
	private ConfigInterface configManager;

	@Override
	public String extractConfigItem(String configItemName) throws EntException {
		return this.getConfigManager().getConfigItem(configItemName);
	}

	@Override
	public List<SmallEntityType> extractSmallEntityTypes(String configItemName, IEntityTypeDOM entityTypeDom) throws EntException {
		String xml = this.extractConfigItem(configItemName);
		return entityTypeDom.extractSmallEntityTypes(xml);
	}
	
	@Override
	public IApsEntity extractEntityType(String typeCode, Class entityClass, String configItemName, 
			IEntityTypeDOM entityTypeDom, String entityManagerName, IApsEntityDOM entityDom) throws EntException {
		String xml = this.extractConfigItem(configItemName);
		logger.debug("{} : {}", configItemName , xml);
		return entityTypeDom.extractEntityType(typeCode, xml, entityClass, entityDom, entityManagerName);
	}
	
	/**
	 * Return the Map of the prototypes of the Entity Types (indexed by their code) that the
	 * entity service is going to handle.
	 * The structure of the Entity Types is obtained from a configuration XML.
	 * @param entityClass The class of the entity. 
	 * @param configItemName The configuration item where the Entity Types are defined.
	 * @param entityTypeDom The DOM class that parses the configuration XML.
	 * @param entityDom The DOM class that parses the XML representing the single (implemented) entity.
	 * @param entityManagerName The entity manager name
	 * @return The map of the Entity Types Prototypes, indexed by code. 
	 * @throws EntException If errors occurs during the parsing process of the XML.
	 */
	@Override
	public Map<String, IApsEntity> extractEntityTypes(Class entityClass, String configItemName, 
			IEntityTypeDOM entityTypeDom, String entityManagerName, IApsEntityDOM entityDom) throws EntException {
		Map<String, IApsEntity> entityTypes = null;
		try {
			String xml = this.extractConfigItem(configItemName);
			logger.debug("{} : {}", configItemName , xml);
			entityTypes = entityTypeDom.extractEntityTypes(xml, entityClass, entityDom, entityManagerName);
		} catch (Throwable t) {
			logger.error("Error in the entities initialization process. configItemName:{}", configItemName, t);
			throw new EntException("Error in the entities initialization process", t);
		}
		return entityTypes;
	}
	
	@Override
	public void updateEntityTypes(Map<String, IApsEntity> entityTypes, String configItemName, IEntityTypeDOM entityTypeDom) throws EntException {
		try {
			String xml = entityTypeDom.getXml(entityTypes);
			this.getConfigManager().updateConfigItem(configItemName, xml);
		} catch (Throwable t) {
			logger.error("Error detected while updating the Entity Types. configItemName: {}", configItemName, t);
			throw new EntException("Error detected while updating the Entity Types", t);
		}
	}

    @Override
    public void updateAttributeRoles(String configItemName, Map<String, AttributeRole> roles) throws EntException {
        try {
            AttributeRoleDOM dom = new AttributeRoleDOM();
            String xml = dom.getXml(roles);
            this.getConfigManager().updateConfigItem(configItemName, xml);
        } catch (EntException e) {
            logger.error("Error detected while updating attribute roles. configItemName: {}", configItemName, e);
            throw new EntException("Error detected while updating attribute roles", e);
        }
    }
	
	protected ConfigInterface getConfigManager() {
		return this.configManager;
	}
	
	public void setConfigManager(ConfigInterface configManager) {
		this.configManager = configManager;
	}
	
}
