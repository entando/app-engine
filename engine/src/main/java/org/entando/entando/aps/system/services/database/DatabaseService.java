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
package org.entando.entando.aps.system.services.database;

import com.agiletec.aps.system.common.model.dao.SearcherDaoPaginatedResult;
import com.agiletec.aps.util.FileTextReader;
import java.security.SecureRandom;
import org.entando.entando.aps.system.exception.ResourceNotFoundException;
import org.entando.entando.aps.system.exception.RestServerError;
import org.entando.entando.aps.system.init.IComponentManager;
import org.entando.entando.aps.system.init.IDatabaseManager;
import org.entando.entando.aps.system.init.model.DataSourceDumpReport;
import org.entando.entando.aps.system.services.database.model.ComponentDto;
import org.entando.entando.aps.system.services.database.model.DumpReportDto;
import org.entando.entando.aps.system.services.database.model.ShortDumpReportDto;

import static org.entando.entando.aps.system.services.storage.StorageManagerUtil.mustBeValidFilename;

import org.entando.entando.web.common.model.PagedMetadata;
import org.entando.entando.web.common.model.RestListRequest;
import org.entando.entando.web.database.validator.DatabaseValidator;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.springframework.validation.BeanPropertyBindingResult;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author E.Santoboni
 */
public class DatabaseService implements IDatabaseService {

    private final EntLogger logger = EntLogFactory.getSanitizedLogger(this.getClass());

    private IDatabaseManager databaseManager;
    private IComponentManager componentManager;

    @Override
    public int getStatus() {
        return this.getDatabaseManager().getStatus();
    }

    @Override
    public PagedMetadata<ShortDumpReportDto> getShortDumpReportDtos(RestListRequest requestList) {
        PagedMetadata<ShortDumpReportDto> result = null;
        List<ShortDumpReportDto> dtos = new ArrayList<>();
        try {
            List<DataSourceDumpReport> reports = this.getDatabaseManager().getBackupReports();
            if (null != reports) {
                reports.stream().forEach(report -> dtos.add(new ShortDumpReportDto(report)));
            }
            List<ShortDumpReportDto> sublist = requestList.getSublist(dtos);
            int size = (null != reports) ? reports.size() : 0;
            SearcherDaoPaginatedResult searchResult = new SearcherDaoPaginatedResult(size, sublist);
            result = new PagedMetadata<>(requestList, searchResult);
            result.setBody(sublist);
        } catch (Throwable t) {
            logger.error("error extracting database reports", t);
            throw new RestServerError("error extracting database reports", t);
        }
        return result;
    }

    @Override
    public DumpReportDto getDumpReportDto(String reportCode) {
        DumpReportDto dtos = null;
        try {
            DataSourceDumpReport report = this.getDatabaseManager().getBackupReport(reportCode);
            if (null == report) {
                logger.warn("no dump found with code {}", reportCode);
                throw new ResourceNotFoundException(DatabaseValidator.ERRCODE_NO_DUMP_FOUND, "reportCode", reportCode);
            }
            dtos = new DumpReportDto(report, this.getComponentManager());
        } catch (ResourceNotFoundException r) {
            throw r;
        } catch (Throwable t) {
            logger.error("error extracting database report {}", reportCode, t);
            throw new RestServerError("error extracting database report " + reportCode, t);
        }
        return dtos;
    }

    @Override
    public void deleteDumpReport(String reportCode) {
        try {
            this.getDatabaseManager().deleteBackup(reportCode);
        } catch (Throwable t) {
            logger.error("error deleting database report {}", reportCode, t);
            throw new RestServerError("error deleting database report " + reportCode, t);
        }
    }

    @Override
    public List<ComponentDto> getCurrentComponents() {
        List<ComponentDto> dtos = new ArrayList<>();
        this.getComponentManager().getCurrentComponents()
                .stream().forEach(component -> dtos.add(new ComponentDto(component)));
        return dtos;
    }

    @Override
    public void startDatabaseBackup() {
        try {
            this.getDatabaseManager().createBackup();
        } catch (Throwable t) {
            logger.error("error starting backup", t);
            throw new RestServerError("error starting backup", t);
        }
    }

    @Override
    public void startDatabaseRestore(String reportCode) {
        try {
            DataSourceDumpReport report = this.getDatabaseManager().getBackupReport(reportCode);
            if (null == report) {
                logger.warn("no dump found with code {}", reportCode);
                throw new ResourceNotFoundException(DatabaseValidator.ERRCODE_NO_DUMP_FOUND, "reportCode", reportCode);
            }
            this.getDatabaseManager().dropAndRestoreBackup(reportCode);
        } catch (ResourceNotFoundException r) {
            throw r;
        } catch (Throwable t) {
            logger.error("error starting restore", t);
            throw new RestServerError("error starting restore", t);
        }
    }

    @Override
    public byte[] getTableDump(String reportCode, String dataSource, String tableName) {
        File tempFile = null;
        byte[] bytes = null;
        try {
            InputStream stream = this.getDatabaseManager().getTableDump(tableName, dataSource, reportCode);
            String safeReportCode = mustBeValidFilename(reportCode);
            if (null == stream) {
                logger.warn("no dump found with code {}, dataSource {}, table {}",
                        reportCode, dataSource, tableName);
                BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(tableName, "tableName");
                bindingResult.reject(DatabaseValidator.ERRCODE_NO_TABLE_DUMP_FOUND, new Object[]{reportCode, dataSource, tableName}, "database.dump.table.notFound");
                throw new ResourceNotFoundException("code - dataSource - table",
                        "'" + safeReportCode + "' - '" + dataSource + "' - '" + tableName + "'");
            }
            tempFile = FileTextReader.createTempFile(new SecureRandom().nextInt(100) + safeReportCode + "_" + dataSource + "_" + tableName, stream);
            File tempDir = new File(FileTextReader.getTempDirectory());
            bytes = FileTextReader.fileToByteArray(tempFile, tempDir);
        } catch (ResourceNotFoundException r) {
            throw r;
        } catch (Throwable t) {
            logger.error("error extracting database dump with code {}, dataSource {}, table {}", reportCode, dataSource, tableName, t);
            throw new RestServerError("error extracting database dump", t);
        } finally {
            if (null != tempFile) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    logger.warn("Failed to create temp file {}", tempFile.getAbsolutePath());
                }
            }
        }
        return bytes;
    }

    public IDatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public void setDatabaseManager(IDatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public IComponentManager getComponentManager() {
        return componentManager;
    }

    public void setComponentManager(IComponentManager componentManager) {
        this.componentManager = componentManager;
    }

}
