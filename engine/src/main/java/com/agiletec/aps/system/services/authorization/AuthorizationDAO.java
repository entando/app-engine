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
package com.agiletec.aps.system.services.authorization;

import com.agiletec.aps.system.common.AbstractSearcherDAO;
import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.common.dao.DuplicateKeyDetector;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.role.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;

/**
 * @author E.Santoboni
 */
public class AuthorizationDAO extends AbstractSearcherDAO implements IAuthorizationDAO {
	
	public static final int BATCH_SIZE_FLUSH = 50;

	private static final EntLogger _logger =  EntLogFactory.getSanitizedLogger(AuthorizationDAO.class);
	
	@Override
	public void addUserAuthorization(String username, Authorization authorization) {
		if (null == authorization || null == username) return;
		String groupName = (null != authorization.getGroup()) ? authorization.getGroup().getName() : null;
		String roleName = (null != authorization.getRole()) ? authorization.getRole().getName() : null;
		super.executeQueryWithoutResultset(ADD_AUTHORIZATION, username, groupName, roleName);
	}
	
	@Override
	public void addUserAuthorizations(String username, List<Authorization> authorizations) {
		this.addUpdateUserAuthorizations(username, authorizations, false);
	}
	
	@Override
	public void updateUserAuthorizations(String username, List<Authorization> authorizations) {
		this.addUpdateUserAuthorizations(username, authorizations, true);
	}
	
	protected void addUpdateUserAuthorizations(String username, List<Authorization> authorizations, boolean update) {
		Connection conn = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			if (update) {
				super.executeQueryWithoutResultset(conn, DELETE_USER_AUTHORIZATIONS, username);
			}
			this.addUserAuthorizations(username, authorizations, conn);
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			_logger.error("Error detected while addind user authorizations",  t);
			throw new RuntimeException("Error detected while addind user authorizations", t);
		} finally {
			this.closeConnection(conn);
		}
	}
	
	protected void addUserAuthorizations(String username, List<Authorization> authorizations, Connection conn) {
		PreparedStatement stat = null;
		try {
			stat = conn.prepareStatement(ADD_AUTHORIZATION);
			for (int i=0; i<authorizations.size(); i++) {
				Authorization auth = authorizations.get(i);
				if (null == auth) continue;
				stat.setString(1, username);
				if (null != auth.getGroup()) {
					stat.setString(2, auth.getGroup().getName());
				} else {
					stat.setNull(2, Types.VARCHAR);
				}
				if (null != auth.getRole()) {
					stat.setString(3, auth.getRole().getName());
				} else {
					stat.setNull(3, Types.VARCHAR);
				}
				stat.addBatch();
				stat.clearParameters();
			}
			stat.executeBatch();
		} catch (Throwable t) {
			_logger.error("Error detected while addind user authorizations",  t);
			throw new RuntimeException("Error detected while addind user authorizations", t);
		} finally {
			this.closeDaoResources(null, stat);
		}
	}
	
	@Override
	public void deleteUserAuthorization(String username, String groupname, String rolename) {
		super.executeQueryWithoutResultset(DELETE_AUTHORIZATION, username, groupname, rolename);
	}
	
	@Override
	public List<Authorization> getUserAuthorizations(String username, Map<String, Group> groups, Map<String, Role> roles) {
		Connection conn = null;
		List<Authorization> authorizations = new ArrayList<Authorization>();
		PreparedStatement stat = null;
		ResultSet res = null;
		try {
			conn = this.getConnection();
			stat = conn.prepareStatement(GET_USER_AUTHORIZATIONS);
			stat.setString(1, username);
			res = stat.executeQuery();
			while (res.next()) {
				String groupname = res.getString(1);
				Group group = (null != groupname) ? groups.get(groupname) : null;
				String rolename = res.getString(2);
				Role role = (null != rolename) ? roles.get(rolename) : null;
				Authorization authorization = new Authorization(group, role);
				if (!authorizations.contains(authorization)) {
					authorizations.add(authorization);
				}
			}
		} catch (Throwable t) {
			_logger.error("Error loading user authorization",  t);
			throw new RuntimeException("Error loading user authorization", t);
		} finally {
			closeDaoResources(res, stat, conn);
		}
		return authorizations;
	}
	
	@Override
	public void deleteUserAuthorizations(String username) {
		super.executeQueryWithoutResultset(DELETE_USER_AUTHORIZATIONS, username);
	}
	
	@Override
	public List<String> getUsersByAuthorities(List<String> groupNames, List<String> roleNames) {
		FieldSearchFilter[] filters = {};
		if (CollectionUtils.isNotEmpty(groupNames)) {
			FieldSearchFilter filter = new FieldSearchFilter("groupname", groupNames, false);
			filters = super.addFilter(filters, filter);
		}
		if (CollectionUtils.isNotEmpty(roleNames)) {
			FieldSearchFilter filter = new FieldSearchFilter("rolename", roleNames, false);
			filters = super.addFilter(filters, filter);
		}
		return super.searchId(filters);
	}
	
	@Override
	public int deleteUserAuthorizationByGroupAndRole(final String username, final List<String> groups,
			final List<String> roles) {
		final boolean hasRoles = roles != null && !roles.isEmpty();
		final boolean hasGroups = groups != null && !groups.isEmpty();
		Connection conn = null;
		PreparedStatement stat = null;

		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);

			final int rowsDeleted = doDeleteUserAuthorizationByGroupAndRole(conn, username, groups, roles);
			conn.commit();
			return rowsDeleted;
		} catch (Exception e) {
			this.executeRollback(conn);
			throw new RuntimeException("Error detected while deleting user authorizations", e);
		} finally {
			this.closeDaoResources(null, stat, conn);
		}
	}

	private int doDeleteUserAuthorizationByGroupAndRole(final Connection conn, final String username, final List<String> groups,
			final List<String> roles) {
		final boolean hasRoles = roles != null && !roles.isEmpty();
		final boolean hasGroups = groups != null && !groups.isEmpty();

		try (PreparedStatement stat = conn.prepareStatement(createSqlForAuthDeletion(groups, roles))) {

			// username
			int index = 1;

			stat.setString(index++, username);
			// groups
			if (hasGroups) {
				for (String role : groups) {
					stat.setString(index++, role);
				}
			}
			// roles
			if (hasRoles) {
				for (String role : roles) {
					stat.setString(index++, role);
				}
			}
			return stat.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("Error deleting user authorization", e);
		}
	}

	// Returns true if the user's external authentication synchronization is up to date
	@Override
	public boolean externalAuthSyncCheck(final String username, final Long iat) {
		Connection conn = null;
		PreparedStatement stat = null;

		try {
			conn = this.getConnection();

			Long lastSyncedIat = null;
			String userId = null;

			try (PreparedStatement selectStmt = conn.prepareStatement(
					QUERY_SYNC_STATUS)) {
				selectStmt.setString(1, username);

				try (ResultSet rs = selectStmt.executeQuery()) {
					if (rs.next()) {
						userId = rs.getString("username");
						lastSyncedIat = rs.getLong("iat");
					}
				}
			}

			if (StringUtils.isBlank(userId)) {
				_logger.debug("must track user {}", username);
				return false;
			} else if (iat > lastSyncedIat) {
				_logger.debug("must synchronize user {}", username);
				return false;
			}
		} catch (Exception e) {
			throw new RuntimeException("Error detected while checking user synchronization", e);
		} finally {
			this.closeConnection(conn);
		}
		return true;
	}

	@Override
	public void externalAuthSync(final String username, final Long iat,
			final List<Authorization> toAdd, final List<Authorization> toRemove) {
		Connection conn = null;
		PreparedStatement stat = null;

		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);

			Long oldIat = null;
			String usernameTracked = null;

			try (PreparedStatement selectStmt = conn.prepareStatement(
					QUERY_SYNC_STATUS)) {
				selectStmt.setString(1, username);

				try (ResultSet rs = selectStmt.executeQuery()) {
					if (rs.next()) {
						usernameTracked = rs.getString("username");
						oldIat = rs.getLong("iat");
					}
				}
			}

			if (usernameTracked == null) {
				_logger.debug("creating entry for user {}", username);
				trackUserAndAuthorizations(username, iat, toAdd, toRemove, conn);

			} else if (iat > oldIat) {
				_logger.debug("updating entry for user {}", username);
				updateIatAndAuthorizations(username, iat, toAdd, toRemove, conn);
			} else {
				_logger.debug("no need to sync {}", username);
			}
			conn.commit();
		} catch (SQLException e) {
			// questa eccezione è aspettata in partenza!
			if (isDuplicateKey(e)) {
				_logger.debug("Integrity constraint violation detected, can be ignored (unless systemic!)");
			} else {
				throw new RuntimeException("Unexpected SQL exception", e);
			}
		} catch (Exception e) {
			this.executeRollback(conn);
			throw new RuntimeException("Error detected while checking user synchronization", e);
		} finally {
			this.closeConnection(conn);
		}
	}

	private void trackUserAndAuthorizations(String username, Long iat, List<Authorization> toAdd, List<Authorization> toRemove,
			Connection conn) throws SQLException {
		try (PreparedStatement insertStmt = conn.prepareStatement(
				CREATE_SYNC_STATUS)) {
			insertStmt.setString(1, username);
			insertStmt.setLong(2, iat);
			insertStmt.executeUpdate();
		}
		// update authorizations
		deleteAuthorities(conn, username, toRemove);
		addAuthorities(conn, username, toAdd);
	}

	private void updateIatAndAuthorizations(String username, Long iat, List<Authorization> toAdd, List<Authorization> toRemove,
			Connection conn) throws SQLException {
		// Aggiorna iat
		try (PreparedStatement updateIat = conn.prepareStatement(
				UPDATE_SYNC_STATUS
		)) {
			updateIat.setLong(1, iat);
			updateIat.setString(2, username);
			updateIat.setLong(3, iat);

			int rows = updateIat.executeUpdate();
			if (rows > 0) {
				// update authorizations
				deleteAuthorities(conn, username, toRemove);
				addAuthorities(conn, username, toAdd);
				_logger.debug("updated {} row with iat {} for username {}", rows, iat, username);
			}
		}
	}

	private boolean isDuplicateKey(SQLException e) {
		return DuplicateKeyDetector.isDuplicateKey(e);
	}

	private void deleteAuthorities(final Connection conn, final String username, final List<Authorization> list) {
		if (conn == null || list == null || list.isEmpty() || StringUtils.isBlank(username)) return;

		final List<String> groups = new ArrayList<>();
		final List<String> roles = new ArrayList<>();

		for (Authorization cur: list) {
			if (cur.getGroup() != null) {
				groups.add(cur.getGroup().getName());
			}

			if (cur.getRole() != null) {
				roles.add(cur.getRole().getName());
			}
		}
		doDeleteUserAuthorizationByGroupAndRole(conn, username, groups, roles);
	}

	private void addAuthorities(final Connection conn, final String username, final List<Authorization> list)
            throws SQLException {
		if (conn == null || list == null || list.isEmpty() || StringUtils.isBlank(username)) return;

		try (PreparedStatement stmt = conn.prepareStatement(
				ADD_AUTHORIZATION
		)) {
			int batchSize = 0;

			stmt.setString(1, username);

			for (Authorization cur : list) {


				if (cur.getGroup() != null) {
					stmt.setString(2, cur.getGroup().getName());
				} else {
					stmt.setNull(2, Types.VARCHAR);
				}

				if (cur.getRole() != null) {
					stmt.setString(3, cur.getRole().getName());
				} else {
					stmt.setNull(3, Types.VARCHAR);
				}
				stmt.addBatch();

				// avoid memory leaking
				if (++batchSize % BATCH_SIZE_FLUSH == 0) {
					stmt.executeBatch();
				}
			}
			stmt.executeBatch();
		}
	}

	private String createSqlForAuthDeletion(final List<String> groups, final List<String> roles) {
		final StringBuilder sb = new StringBuilder(DELETE_USER_AUTHORIZATIONS);
		final boolean hasRoles = roles != null && !roles.isEmpty();
		final boolean hasGroups = groups != null && !groups.isEmpty();

		sb.append(" AND ("); // apertura AND

		if (hasGroups) {
			final String placeholders = String.join(", ",
					Collections.nCopies(groups.size(), "?"));

			sb.append("groupname IN ( ");
			sb.append(placeholders);
			sb.append(") "); // chiusura groupname
			// append OR if needed
			if (hasRoles) {
				sb.append("OR ");
			}
		}
		if (hasRoles) {
			final String placeholders = String.join(", ",
					Collections.nCopies(roles.size(), "?"));
			sb.append("rolename IN ( ");
			sb.append(placeholders);
			sb.append(") "); // chiusura rolename
		}
		sb.append(")"); // chiusura AND
		return sb.toString();
	}

	@Override
	public int externalAuthSyncClean(Instant cutoff, int batchSize) throws SQLException {
		int deleted = 0;
		Connection conn = null;
		final long epochSeconds = cutoff.getEpochSecond();

		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);

			deleted = doBatchDeletion(conn, epochSeconds, batchSize);
			conn.commit();
		} catch (Exception e) {
			this.executeRollback(conn);
			_logger.error("Error cleaning synchronization status for threshold '{}'", cutoff, e);
		} finally {
			this.closeConnection(conn);
		}
		return deleted;
	}

	public int doBatchDeletion(Connection conn, long epochSeconds, int batchSize) throws SQLException {
		final String selectQry = "SELECT username FROM authusersextsync WHERE iat < ?";
		final String deleteQry = "DELETE FROM authusersextsync WHERE username = ?";

		int deleted = 0;

		try (PreparedStatement stat = conn.prepareStatement(selectQry)) {

			stat.setLong(1, epochSeconds);
			stat.setMaxRows(batchSize);

			try (ResultSet rs = stat.executeQuery();
					PreparedStatement deleteStmt = conn.prepareStatement(deleteQry)) {

				while (rs.next()) {
					String username = rs.getString(1);

					deleteStmt.setString(1, username);
					deleteStmt.addBatch();
					deleted++;
				}
				deleteStmt.executeBatch();
			}
		}
		return deleted;
	}


	@Override
	protected String getTableFieldName(String metadataFieldKey) {
		return metadataFieldKey;
	}
	
	@Override
	protected String getMasterTableName() {
		return "authusergrouprole";
	}
	
	@Override
	protected String getMasterTableIdFieldName() {
		return "username";
	}
	
	private final String ADD_AUTHORIZATION =
		"INSERT INTO authusergrouprole(username, groupname, rolename) VALUES ( ? , ? , ? )";
	
	private final String DELETE_USER_AUTHORIZATIONS =
		"DELETE FROM authusergrouprole WHERE username = ?";
	
	private final String DELETE_AUTHORIZATION =
		DELETE_USER_AUTHORIZATIONS + " AND groupname = ? AND rolename = ? ";
	
	private final String GET_USER_AUTHORIZATIONS = 
		"SELECT groupname, rolename FROM authusergrouprole WHERE username = ? ";
	
	public static final String UPDATE_SYNC_STATUS =
			"UPDATE authusersextsync SET iat = ? WHERE username = ? AND iat < ?";

	public static final String CREATE_SYNC_STATUS =
			"INSERT INTO authusersextsync (username, iat) VALUES (?, ?)";

	public static final String QUERY_SYNC_STATUS =
			"SELECT username, iat FROM authusersextsync WHERE username = ? FOR UPDATE";

	public static final String DELETE_SYNC_STATUS =
			"DELETE FROM authusersextsync WHERE iat < ?";
}
