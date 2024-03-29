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
package com.agiletec.aps.system.common;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.aps.system.ApsSystemUtils;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.springframework.beans.factory.annotation.Autowired;
import java.sql.PreparedStatement;

/**
 * Classe contenente alcuni metodi di utilita per i DAO.
 * @author M.Diana - E.Santoboni
 */
public abstract class AbstractDAO implements Serializable {

	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(AbstractDAO.class);
	private transient DataSource dataSource;
	private transient ITenantManager tenantManager;

	/**
	 * Traccia un'eccezione e rilancia una eccezione runtime 
	 * con il messaggio specificato. Da usare nel catch delle eccezioni.
	 * @param t L'eccezione occorsa.
	 * @param message Il messaggio per la nuova ecceione da rilanciare
	 * @param methodName Il nome del metodo in cui si e verificata l'eccezione 
	 *                   (non indispensabile, può essere null)
	 */
	@Deprecated
	protected void processDaoException(Throwable t, String message, String methodName) {
		ApsSystemUtils.logThrowable(t, this, methodName, message);
		throw new RuntimeException(message, t);
	}

	/**
	 * Restituisce una connessione SQL relativa al datasource.
	 * @return La connessione richiesta.
	 * @throws EntException In caso di errore in apertura di connessione.
	 */
	protected Connection getConnection() throws EntException {
		Connection conn = null;
		try {
			DataSource ds = ApsTenantApplicationUtils.getTenant()
					.map(tenantCode -> getTenantManager().getDatasource(tenantCode))
					.orElse(getDataSource());
			conn = ds.getConnection();

		} catch (SQLException e) {
			_logger.error("Error getting connection to the datasource", e);
			throw new EntException("Error getting connection to the datasource", e);
		}
		return conn;
	}

	/**
	 * Chiude in modo controllato un resultset, uno statement e la connessione, 
	 * senza rilanciare eccezioni. Da usare nel finally di gestione di
	 * una eccezione.
	 * @param res Il resultset da chiudere; può esser null
	 * @param stat Lo statement da chiudere; può esser null
	 * @param conn La connessione al db; può esser null
	 */
	protected void closeDaoResources(ResultSet res, Statement stat, Connection conn) {
		this.closeDaoResources(res, stat);
		this.closeConnection(conn);
	}

	/**
	 * Chiude in modo controllato un resultset e uno statement, 
	 * senza rilanciare eccezioni. Da usare nel finally di gestione di
	 * una eccezione.
	 * @param res Il resultset da chiudere; può esser null
	 * @param stat Lo statement da chiudere; può esser null
	 */
	protected void closeDaoResources(ResultSet res, Statement stat) {
		if (res != null) {
			try {
				res.close();
			} catch (Throwable t) {
				_logger.error("Error while closing the resultset", t);
			}
		}
		if (stat != null) {
			try {
				stat.close();
			} catch (Throwable t) {
				_logger.error("Error while closing the resultset", t);
			}
		}
	}

	/**
	 * Esegue un rollback, senza rilanciare eccezioni. 
	 * Da usare nel blocco catch di gestione di una eccezione. 
	 * @param conn La connessione al db.
	 */
	protected void executeRollback(Connection conn) {
		try {
			if (conn != null) conn.rollback();
		} catch (SQLException e) {
			_logger.error("Error on connection rollback", e);
		}
	}

	/**
	 * Chiude in modo controllato una connessione, 
	 * senza rilanciare eccezioni. Da usare nel finally di gestione di
	 * una eccezione.
	 * @param conn La connessione al db; può esser null
	 */
	protected void closeConnection(Connection conn) {
		try {
			if (conn != null) conn.close();
		} catch (Throwable t) {
			_logger.error("Error closing the connection", t);
		}
	}
	
	protected void executeQueryWithoutResultset(String query, Object... args) {
		Connection conn = null;
		try {
			conn = this.getConnection();
			conn.setAutoCommit(false);
			this.executeQueryWithoutResultset(conn, query, args);
			conn.commit();
		} catch (Throwable t) {
			this.executeRollback(conn);
			_logger.error("Error executing query",  t);
			throw new RuntimeException("Error executing query", t);
		} finally {
			this.closeConnection(conn);
		}
	}
	
	protected void executeQueryWithoutResultset(Connection conn, String query, Object... args) {
		PreparedStatement stat = null;
    	try {
    		stat = conn.prepareStatement(query);
			for (int i = 0; i < args.length; i++) {
				Object object = args[i];
				stat.setObject(i+1, object);
			}
    		stat.executeUpdate();
    	} catch (Throwable t) {
    		_logger.error("Error executing query",  t);
			throw new RuntimeException("Error executing query", t);
    	} finally {
    		closeDaoResources(null, stat);
    	}
	}
	
	protected DataSource getDataSource() {
		return this.dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	protected ITenantManager getTenantManager() {
		return tenantManager;
	}

	@Autowired
	public void setTenantManager(ITenantManager tenantManager) {
		this.tenantManager = tenantManager;
	}
}
