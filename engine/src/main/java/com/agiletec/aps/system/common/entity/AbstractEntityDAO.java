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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.aps.system.common.AbstractDAO;
import com.agiletec.aps.system.common.entity.model.ApsEntityRecord;
import com.agiletec.aps.system.common.entity.model.AttributeSearchInfo;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.lang.ILangManager;

/**
 * Abstract DAO class used for the management of the ApsEntities.
 * @author E.Santoboni
 */
public abstract class AbstractEntityDAO extends AbstractDAO implements IEntityDAO {

	private static final EntLogger _logger =  EntLogFactory.getSanitizedLogger(AbstractEntityDAO.class);
	
	@Override
	public void addEntity(IApsEntity entity) {
		Connection conn = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			this.executeAddEntity(entity, conn);
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			_logger.error("Error adding new entity",  t);
			throw new RuntimeException("Error adding new entity", t);
		} finally {
			this.closeConnection(conn);
		}
	}
	
	protected void executeAddEntity(IApsEntity entity, Connection conn) throws Throwable {
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(this.getAddEntityRecordQuery());
			this.buildAddEntityStatement(entity, stat);
			stat.executeUpdate();
			this.addEntitySearchRecord(entity.getId(), entity, conn);
			this.addEntityAttributeRoleRecord(entity.getId(), entity, conn);
		} catch (Throwable t) {
			throw t;
		} finally {
			this.closeDaoResources(null, stat);
		}
	}
	
	protected abstract String getAddEntityRecordQuery();
	
	protected abstract void buildAddEntityStatement(IApsEntity entity, PreparedStatement stat) throws Throwable;
	
	@Override
	public void deleteEntity(String entityId) {
		Connection conn = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			this.executeDeleteEntity(entityId, conn);
			conn.commit();
		} catch (Throwable t) {
			_logger.error("Error deleting the entity by id '{}'", entityId,  t);
			throw new RuntimeException("Error deleting the entity by id", t);
		} finally {
			closeConnection(conn);
		}
	}
	
	protected void executeDeleteEntity(String entityId, Connection conn) throws Throwable {
		this.deleteRecordsByEntityId(entityId, this.getRemovingSearchRecordQuery(), conn);
		this.deleteRecordsByEntityId(entityId, this.getRemovingAttributeRoleRecordQuery(), conn);
		this.deleteRecordsByEntityId(entityId, this.getDeleteEntityRecordQuery(), conn);
	}
	
	protected abstract String getDeleteEntityRecordQuery();
	
	@Override
	public void updateEntity(IApsEntity entity) {
		Connection conn = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			this.executeUpdateEntity(entity, conn);
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			_logger.error("Error updating entity",  t);
			throw new RuntimeException("Error updating entity", t);
		} finally {
			this.closeConnection(conn);
		}
	}
	
	protected void executeUpdateEntity(IApsEntity entity, Connection conn) throws Throwable {
		PreparedStatement stat = null;
		try {
			this.deleteRecordsByEntityId(entity.getId(), this.getRemovingSearchRecordQuery(), conn);
			this.deleteRecordsByEntityId(entity.getId(), this.getRemovingAttributeRoleRecordQuery(), conn);
			stat = conn.prepareStatement(this.getUpdateEntityRecordQuery());
			this.buildUpdateEntityStatement(entity, stat);
			stat.executeUpdate();
			this.addEntitySearchRecord(entity.getId(), entity, conn);
			this.addEntityAttributeRoleRecord(entity.getId(), entity, conn);
		} catch (Throwable t) {
			throw t;
		} finally {
			this.closeDaoResources(null, stat);
		}
	}
	
	protected abstract String getUpdateEntityRecordQuery();
	
	protected abstract void buildUpdateEntityStatement(IApsEntity entity, PreparedStatement stat) throws Throwable;
	
	@Override
	public ApsEntityRecord loadEntityRecord(String id) {
		Connection conn = null;
		PreparedStatement stat = null;
		ResultSet res = null;
		ApsEntityRecord entityRecord = null;
		try {
			conn = this.getConnection();
			stat = conn.prepareStatement(this.getLoadEntityRecordQuery());
			stat.setString(1, id);
			res = stat.executeQuery();
			if (res.next()) {
				entityRecord = this.createEntityRecord(res);
			}
		} catch (Throwable t) {
			_logger.error("Error loading entity record '{}'", id,  t);
			throw new RuntimeException("Error loading entity record", t);
		} finally {
			closeDaoResources(res, stat, conn);
		}
		return entityRecord;
	}
	
	protected abstract String getLoadEntityRecordQuery();
	
	protected abstract ApsEntityRecord createEntityRecord(ResultSet res) throws Throwable;
	
	@Override
	public void reloadEntitySearchRecords(String id, IApsEntity entity) {
		Connection conn = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			this.executeReloadEntitySearchRecords(id, entity, conn);
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			_logger.error("Error detected while reloading references",  t);
			throw new RuntimeException("Error detected while reloading references", t);
		} finally {
			this.closeConnection(conn);
		}
	}
	
	protected void executeReloadEntitySearchRecords(String id, IApsEntity entity, Connection conn) throws Throwable {
		this.deleteRecordsByEntityId(id, this.getRemovingSearchRecordQuery(), conn);
		this.deleteRecordsByEntityId(id, this.getRemovingAttributeRoleRecordQuery(), conn);
		this.addEntitySearchRecord(id, entity, conn);
		this.addEntityAttributeRoleRecord(id, entity, conn);
	}
	
	protected void addEntitySearchRecord(String id, IApsEntity entity, Connection conn) throws EntException {
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(this.getAddingSearchRecordQuery());
			this.addEntitySearchRecord(id, entity, stat);
		} catch (Throwable t) {
			_logger.error("Error while adding a new record",  t);
			throw new RuntimeException("Error while adding a new record", t);
		} finally {
			closeDaoResources(null, stat);
		}
	}
	
	protected void addEntitySearchRecord(String id, IApsEntity entity, PreparedStatement stat) throws Throwable {
		// Stated rather than assumed: indexing a null entity is meaningless and no caller does it (each
		// one reads entity.getId() to obtain the id passed here). The planner below tolerates a null entity
		// because its other callers - the admin search forms - may have no prototype yet; this path never
		// does, so say so instead of letting the next line decide.
		Objects.requireNonNull(entity, "entity to index");
		// The whole write rule - which attributes are indexed, and under which attrname - is decided once,
		// by the engine's single traversal. This DAO does not walk the attribute tree, does not know that
		// lists are excluded from path indexing, and does not build a path: it writes what it is handed.
		for (SearchRecordSpec spec : NestedBooleanSearchSupport.planSearchRecords(entity,
				this.getSearchKeyStrategy())) {
			List<AttributeSearchInfo> infos = spec.attribute().getSearchInfos(this.getLangManager().getLangs());
			if (null != infos) {
				this.addAttributeSearchInfoRecords(id, spec.attrName(), infos, stat);
			}
		}
		stat.executeBatch();
	}

	private void addAttributeSearchInfoRecords(String id, String attrName,
			List<AttributeSearchInfo> infos, PreparedStatement stat) throws SQLException {
		// id and attrname are invariant across the info rows of this attribute; set them once and let
		// the per-row columns (3-6) be overwritten each iteration before addBatch().
		stat.setString(1, id);
		stat.setString(2, attrName);
		for (AttributeSearchInfo searchInfo : infos) {
			stat.setString(3, searchInfo.getString());
			if (searchInfo.getDate() != null) {
				stat.setTimestamp(4, new java.sql.Timestamp(searchInfo.getDate().getTime()));
			} else {
				stat.setDate(4, null);
			}
			stat.setBigDecimal(5, searchInfo.getBigDecimal());
			stat.setString(6, searchInfo.getLangCode());
			stat.addBatch();
		}
	}
	
	protected void addEntityAttributeRoleRecord(String id, IApsEntity entity, Connection conn) {
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(this.getAddingAttributeRoleRecordQuery());
			this.addEntityAttributeRoleRecord(id, entity, stat);
		} catch (Throwable t) {
			_logger.error("Error while adding a new attribute role record",  t);
			throw new RuntimeException("Error while adding a new attribute role record", t);
		} finally {
			closeDaoResources(null, stat);
		}
	}
	
	protected void addEntityAttributeRoleRecord(String id, IApsEntity entity, PreparedStatement stat) throws Throwable {
		List<AttributeInterface> attributes = entity.getAttributeList();
		for (int i = 0; i < attributes.size(); i++) {
			AttributeInterface currAttribute = attributes.get(i);
			String[] roleNames = currAttribute.getRoles();
			if (null != roleNames && roleNames.length > 0) {
				for (int j = 0; j < roleNames.length; j++) {
					String roleName = roleNames[j];
					stat.setString(1, id);
					stat.setString(2, currAttribute.getName());
					stat.setString(3, roleName);
					stat.addBatch();
					stat.clearParameters();
				}
			}
		}
		stat.executeBatch();
	}
	
	protected void deleteEntitySearchRecord(String id, Connection conn) throws EntException {
		this.deleteRecordsByEntityId(id, this.getRemovingSearchRecordQuery(), conn);
	}
	
	/**
	 * 'Utility' method. Delete entity records by entity id
	 * @param entityId the entity id to use for deleting records.
	 * @param query The sql query
	 * @param conn The connection.
	 */
	protected void deleteRecordsByEntityId(String entityId, String query, Connection conn) {
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(query);
			stat.setString(1, entityId);
			stat.executeUpdate();
		} catch (Throwable t) {
			_logger.error("Error deleting entity records by id '{}'", entityId,  t);
			throw new RuntimeException("Error deleting entity records by id " + entityId, t);
		} finally {
			closeDaoResources(null, stat);
		}
	}
	
	/**
	 * @deprecated deprecated from jAPS 2.0 version 2.0.9
	 */
	@Override
	public List<String> getAllEntityId() {
		Connection conn = null;
		Statement stat = null;
		ResultSet res = null;
		List<String> entitiesId = new ArrayList<>();
		try {
			conn = this.getConnection();
			stat = conn.createStatement();
			res = stat.executeQuery(this.getExtractingAllEntityIdQuery());
			while (res.next()) {
				entitiesId.add(res.getString(1));
			}
		} catch (EntException | SQLException t) {
			_logger.error("Error retrieving the list of entity IDs",  t);
			throw new RuntimeException("Error retrieving the list of entity IDs", t);
		} finally {
			closeDaoResources(res, stat, conn);
		}
		return entitiesId;
	}

	/**
	 * Return the specific query to add a new record of informations in the
	 * support database.
	 * The query must respect the following positions of the elements:<br />
	 * Position 1: entity ID<br />
	 * Position 2: attribute name<br />
	 * Position 3: searchable string<br />
	 * Position 4: searchable data<br />
	 * Position 5: searchable number<br />
	 * Position 6: Language code
	 * @return the query to add a look up record for the entity search. 
	 */
	protected abstract String getAddingSearchRecordQuery();
	
	protected abstract String getAddingAttributeRoleRecordQuery();
	
	/**
	 * Return the query to delete the record associated to an entity. The returned query will only need
	 * the declaration of the ID of the entity to delete.
	 * @return  The query to delete the look up record of a single entity.
	 */
	protected abstract String getRemovingSearchRecordQuery();
	
	protected abstract String getRemovingAttributeRoleRecordQuery();
	
	/**
	 * Return the query that extracts the list of entity IDs.
	 * @return The query that extracts the list of entity IDs.
	 * @deprecated As of jAPS 2.0 version 2.0.9
	 */
	protected abstract String getExtractingAllEntityIdQuery();
	
	/**
	 * The key strategy of the entity manager that owns this DAO - the same one its validation uses, so a
	 * key that was accepted at persist time is the key that gets written here. Defaults to the strategy
	 * matching the search tables the platform ships, so a DAO that is not explicitly wired keeps working.
	 */
	protected IEntitySearchKeyStrategy getSearchKeyStrategy() {
		return this.searchKeyStrategy;
	}

	/**
	 * @param searchKeyStrategy the strategy matching this DAO's search table; null restores the default.
	 * Must be the same instance (or an equivalent one) as the owning manager's, or the keys written here
	 * would not be the keys the readers resolve.
	 */
	public void setSearchKeyStrategy(IEntitySearchKeyStrategy searchKeyStrategy) {
		this.searchKeyStrategy = (null != searchKeyStrategy)
				? searchKeyStrategy : DefaultEntitySearchKeyStrategy.INSTANCE;
	}

	protected ILangManager getLangManager() {
		return _langManager;
	}
	public void setLangManager(ILangManager langManager) {
		this._langManager = langManager;
	}
	
	private ILangManager _langManager;

	private IEntitySearchKeyStrategy searchKeyStrategy = DefaultEntitySearchKeyStrategy.INSTANCE;
	
}
