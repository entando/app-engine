/*
 * Copyright 2015-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.system.init;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import com.agiletec.aps.util.DateConverter;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.entando.entando.aps.system.init.model.Component;
import org.entando.entando.aps.system.init.model.DataSourceDumpReport;
import org.entando.entando.aps.system.init.model.SystemInstallationReport;
import org.entando.entando.aps.system.init.model.TableDumpReport;
import org.entando.entando.aps.system.init.util.TableDataUtils;
import org.entando.entando.aps.system.services.storage.IStorageManager;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.springframework.beans.BeansException;

/**
 * @author E.Santoboni
 */
public class DatabaseDumper extends AbstractDatabaseUtils {

    private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(DatabaseDumper.class);

    protected void createBackup(AbstractInitializerManager.Environment environment, SystemInstallationReport installationReport) throws EntException {
        try {
            DataSourceDumpReport report = new DataSourceDumpReport(installationReport);
            long start = System.currentTimeMillis();
            String backupSubFolder = (AbstractInitializerManager.Environment.develop.equals(environment))
                    ? environment.toString() : DateConverter.getFormattedDate(new Date(), "yyyyMMddHHmmss");
            report.setSubFolderName(backupSubFolder);
            List<Component> components = this.getComponents();
            for (int i = 0; i < components.size(); i++) {
                Component componentConfiguration = components.get(i);
                this.createBackup(componentConfiguration.getTableNames(), report, backupSubFolder);
            }
            long time = System.currentTimeMillis() - start;
            report.setRequiredTime(time);
            report.setDate(new Date());
            StringBuilder reportFolder = new StringBuilder(this.getLocalBackupsFolder());
            if (null != backupSubFolder) {
                reportFolder.append(backupSubFolder).append(File.separator);
            }
            this.save(DatabaseManager.DUMP_REPORT_FILE_NAME,
                    reportFolder.toString(), report.toXml());
        } catch (Throwable t) {
            _logger.error("error in ", t);
            throw new EntException("Error while creating backup", t);
        }
    }

    private void createBackup(Map<String, List<String>> tableMapping, DataSourceDumpReport report, String backupSubFolder) throws EntException {
        if (null == tableMapping || tableMapping.isEmpty()) {
            return;
        }
        try {
            String[] dataSourceNames = this.extractBeanNames(DataSource.class);
            for (String dataSourceName : dataSourceNames) {
                List<String> tableNames = tableMapping.get(dataSourceName);
                if (null == tableNames || tableNames.isEmpty()) {
                    continue;
                }
                DataSource dataSource = ApsTenantApplicationUtils.getTenant()
                        .map(tenantCode -> {
                            ITenantManager tenantManager = this.getBeanFactory().getBean(ITenantManager.class);
                            return tenantManager.getDatasource(tenantCode);
                        })
                        .orElse((DataSource) this.getBeanFactory().getBean(dataSourceName));
                for (String tableName : tableNames) {
                    this.dumpTableData(tableName, dataSourceName, dataSource, report, backupSubFolder);
                }
            }
        } catch (BeansException | EntException t) {
            _logger.error("Error while creating backup", t);
            throw new EntException("Error while creating backup", t);
        }
    }

    protected void dumpTableData(String tableName, String dataSourceName,
            DataSource dataSource, DataSourceDumpReport report, String backupSubFolder) throws EntException {
        String filename = tableName + ".sql";
        File tempFile = null;
        FileWriter fileWriter = null;
        BufferedWriter bufferWriter = null;
        try {
            tempFile = this.createEmptyTempFile(filename);
            fileWriter = new FileWriter(tempFile.getAbsolutePath());
            bufferWriter = new BufferedWriter(fileWriter);
            TableDumpReport tableDumpReport = TableDataUtils.dumpTable(bufferWriter, dataSource, tableName);
            report.addTableReport(dataSourceName, tableDumpReport);
        } catch (IOException t) {
            _logger.error("Error dumping table '{}' - datasource '{}'", tableName, dataSourceName, t);
            throw new EntException("Error dumping table '" + tableName + "' - datasource '" + dataSourceName + "'", t);
        } finally {
            try {
                if (null != bufferWriter) {
                    bufferWriter.close();
                }
                if (null != fileWriter) {
                    fileWriter.close();
                }
            } catch (IOException t2) {
                _logger.error("Error closing FileWriter and BufferedWriter of file '{}'", filename, t2);
                throw new EntException("Error closing FileWriter and BufferedWriter", t2);
            }
        }
        this.finalizeDumpFile(filename, dataSourceName, backupSubFolder, tempFile);
    }

    private void finalizeDumpFile(String filename, String dataSourceName, String backupSubFolder, File tempFile) throws EntException {
        InputStream is = null;
        try {
            StringBuilder dirName = new StringBuilder(this.getLocalBackupsFolder());
            if (null != backupSubFolder) {
                dirName.append(backupSubFolder).append(File.separator);
            }
            dirName.append(dataSourceName).append(File.separator);
            is = new FileInputStream(new File(tempFile.getAbsolutePath()));
            this.save(filename, dirName.toString(), is);
        } catch (EntException | IOException t) {
            _logger.error("Error saving dump file '{}'", tempFile.getName(), t);
            throw new EntException("Error saving dump file '" + tempFile.getName() + "'", t);
        } finally {
            try {
                if (null != is) {
                    is.close();
                }
            } catch (IOException t2) {
                _logger.error("Error closing FileInputstream of temp file '{}'", filename, t2);
            }
            if (null != tempFile) {
                boolean deleted = tempFile.delete();
                if(!deleted) {
                    _logger.warn("Failed to delete temp file {} ",tempFile.getAbsolutePath());
                }
            }
        }
    }

    protected File createEmptyTempFile(String filename) throws EntException {
        try {
            String tempDir = System.getProperty("java.io.tmpdir");
            String filePath = tempDir + File.separator + filename;
            File file = new File(filePath);
            boolean created = file.createNewFile();
            if(!created) {
                _logger.warn("Failed to create a temp file {} ",created);
            }
            return file;
        } catch (Throwable t) {
            _logger.error("Error saving new temp file '{}'", filename, t);
            throw new EntException("Error saving new temp file '" + filename + "'", t);
        }
    }

    protected void save(String filename, String folder, String content) throws EntException {
        ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        this.save(filename, folder, bais);
    }

    protected void save(String filename, String folder, InputStream is) throws EntException {
        try {
            IStorageManager storageManager = this.getStorageManager();
            String path = folder + filename;
            storageManager.saveFile(path, true, is);
        } catch (Throwable t) {
            _logger.error("Error saving backup '{}'", filename, t);
            throw new EntException("Error saving backup '" + filename + "'", t);
        }
    }

}
