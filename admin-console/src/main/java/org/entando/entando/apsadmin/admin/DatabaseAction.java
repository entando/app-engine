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
package org.entando.entando.apsadmin.admin;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.entando.entando.aps.system.init.IDatabaseManager;
import org.entando.entando.aps.system.init.model.Component;
import org.entando.entando.aps.system.init.model.ComponentInstallationReport;
import org.entando.entando.aps.system.init.model.DataSourceDumpReport;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.apsadmin.system.BaseAction;
import org.entando.entando.aps.system.init.ComponentManager;

/**
 * @author E.Santoboni
 */
public class DatabaseAction extends BaseAction {

    private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(DatabaseAction.class);

    public String executeBackup() {
        try {
            this.getDatabaseManager().createBackup();
        } catch (Throwable t) {
            _logger.error("error in executeBackup", t);
            return FAILURE;
        }
        return SUCCESS;
    }

    public List<DataSourceDumpReport> getDumpReports() {
        try {
            return this.getDatabaseManager().getBackupReports();
        } catch (Throwable t) {
            _logger.error("Error extracting dump reports", t);
            throw new RuntimeException("Error extracting dump reports", t);
        }
    }

    public DataSourceDumpReport getDumpReport(String subFolderName) {
        try {
            return this.getDatabaseManager().getBackupReport(subFolderName);
        } catch (Throwable t) {
            _logger.error("Error extracting report of subfolder {}", subFolderName, t);
            throw new RuntimeException("Error extracting report of subfolder " + subFolderName, t);
        }
    }

    public String entryBackupDetails() {
        String check = this.checkBackupCode(this.getSubFolderName());
        if (null != check) {
            return check;
        }
        return SUCCESS;
    }
	
    public String redirectRestoreIntro() {
        try {
            String check = this.checkRestore(this.getSubFolderName());
            if (null != check) {
                return check;
            }
        } catch (Throwable t) {
            _logger.error("error in redirectRestoreIntro", t);
            return FAILURE;
        }
        return SUCCESS;
    }
	
    public String restoreBackup() {
        try {
            String check = this.checkRestore(this.getSubFolderName());
            if (null != check) {
                return check;
            }
            this.getDatabaseManager().dropAndRestoreBackup(this.getSubFolderName());
            this.addActionMessage(this.getText("message.restore.done"));
        } catch (Throwable t) {
            _logger.error("error in restoreBackup", t);
            return FAILURE;
        }
        return SUCCESS;
    }
	
    public String extractTableDump() {
        try {
            String check = this.checkBackupCode(this.getSubFolderName());
            if (null != check) {
                return check;
            }
            InputStream stream = this.getDatabaseManager().getTableDump(this.getTableName(), this.getDataSourceName(), this.getSubFolderName());
            this.setInputStream(stream);
        } catch (Throwable t) {
            _logger.error("error in extractLastTableDump", t);
            return FAILURE;
        }
        return SUCCESS;
    }
	
    public String trashBackup() {
        try {
            String check = this.checkBackupCode(this.getSubFolderName());
            if (null != check) {
                return check;
            }
        } catch (Throwable t) {
            _logger.error("error in trashBackup", t);
            return FAILURE;
        }
        return SUCCESS;
    }
	
    public String deleteBackup() {
        try {
            String check = this.checkBackupCode(this.getSubFolderName());
            if (null != check) {
                return check;
            }
            this.getDatabaseManager().deleteBackup(this.getSubFolderName());
            String[] args = {this.getSubFolderName()};
            this.addActionMessage(this.getText("message.backup.deleteDone", args));
        } catch (Throwable t) {
            _logger.error("error in deleteBackup", t);
            return FAILURE;
        }
        return SUCCESS;
    }
	
	protected String checkRestore(String backupCode) {
		if (null == this.getRestoreEnabled() || !this.getRestoreEnabled()) {
			this.addFieldError("restoreEnabled", this.getText("error.restore.disabled"));
            return INPUT;
		} else {
			return this.checkBackupCode(backupCode);
		}
    }
	
    protected String checkBackupCode(String backupCode) {
		if (null == backupCode) {
            this.addFieldError("subFolderName", this.getText("error.backup.nullCode"));
            return INPUT;
        } else if (null == this.getDumpReport(backupCode)) {
            String[] args = {backupCode};
            this.addFieldError("subFolderName", this.getText("error.backup.invalidCode", args));
            return INPUT;
        }
        return null;
    }
	
    public int getManagerStatus() {
        return this.getDatabaseManager().getStatus();
    }
	
    public List<Component> getCurrentComponents() {
        List<Component> components = null;
        try {
            components = this.getComponentManager().getCurrentComponents();
        } catch (Throwable t) {
            _logger.error("Error extracting current components", t);
            throw new RuntimeException("Error extracting current components", t);
        }
        return components;
    }
	
    public boolean checkRestore(List<Component> currentComponents, DataSourceDumpReport report) {
        List<String> codes = new ArrayList<String>();
        codes.add("entandoCore");
        for (int i = 0; i < currentComponents.size(); i++) {
            Component component = currentComponents.get(i);
            codes.add(component.getCode());
        }
        List<ComponentInstallationReport> reports = report.getComponentsHistory();
        if (reports.size() != codes.size()) {
            return false;
        }
        for (int i = 0; i < reports.size(); i++) {
            ComponentInstallationReport componentReport = reports.get(i);
            if (!codes.contains(componentReport.getComponentCode())) {
                return false;
            }
        }
        return true;
    }
	
    public String getSubFolderName() {
        return _subFolderName;
    }
    public void setSubFolderName(String subFolderName) {
        this._subFolderName = subFolderName;
    }
	
    public String getDataSourceName() {
        return _dataSourceName;
    }
    public void setDataSourceName(String dataSourceName) {
        this._dataSourceName = dataSourceName;
    }
	
    public String getTableName() {
        return _tableName;
    }
    public void setTableName(String tableName) {
        this._tableName = tableName;
    }
	
    public InputStream getInputStream() {
        return _inputStream;
    }
    protected void setInputStream(InputStream inputStream) {
        this._inputStream = inputStream;
    }
	
	public Boolean getRestoreEnabled() {
		return _restoreEnabled;
	}
	public void setRestoreEnabled(Boolean restoreEnabled) {
		this._restoreEnabled = restoreEnabled;
	}
	
    protected IDatabaseManager getDatabaseManager() {
        return _databaseManager;
    }
    public void setDatabaseManager(IDatabaseManager databaseManager) {
        this._databaseManager = databaseManager;
    }
	
    private String _subFolderName;
	
    private String _tableName;
    private String _dataSourceName;
    private InputStream _inputStream;
	
	private Boolean _restoreEnabled;
	
    private IDatabaseManager _databaseManager;
	
}
