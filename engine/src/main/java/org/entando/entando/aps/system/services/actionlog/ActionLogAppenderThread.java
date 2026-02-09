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
package org.entando.entando.aps.system.services.actionlog;

import com.agiletec.aps.system.ApsSystemUtils;
import com.agiletec.aps.util.ApsTenantApplicationUtils;
import org.entando.entando.aps.system.services.actionlog.model.ActionLogRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author E.Santoboni
 */
public class ActionLogAppenderThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ActionLogAppenderThread.class);

    private final ActionLogRecord actionRecordToAdd;
    private final ActionLogManager actionLogManager;

    public ActionLogAppenderThread(ActionLogRecord actionRecordToAdd,
            ActionLogManager actionLogManager) {
        this.actionLogManager = actionLogManager;
        this.actionRecordToAdd = actionRecordToAdd;
    }

    @Override
    public void run() {
        try {
            ApsSystemUtils.ApsDeepDebug.print("TENANT", String.format("%s  - start - tenant %s",
                    this.getClass().getSimpleName(), ApsTenantApplicationUtils.getTenant().orElse("primary")));
            this.actionLogManager.addActionRecordByThread(this.actionRecordToAdd);
            ApsSystemUtils.ApsDeepDebug.print("TENANT", String.format("%s  - end - tenant %s",
                    this.getClass().getSimpleName(), ApsTenantApplicationUtils.getTenant().orElse("primary")));
        } catch (Throwable t) {
            logger.error("error in run", t);
        }
    }

}