/*
 * Copyright 2023-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpsolr.aps.system.solr;

import static com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager.STATUS_NEED_TO_RELOAD_INDEXES;
import static com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager.STATUS_READY;
import static com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager.STATUS_RELOADING_INDEXES_IN_PROGRESS;

public class SolrIndexStatus implements ISolrIndexStatus {

    private int statusValue;
    private int previousValue;

    @Override
    public synchronized int getValue() {
        return statusValue;
    }

    @Override
    public synchronized void setValue(int value) {
        this.statusValue = value;
    }

    public synchronized boolean canReloadIndexes() {
        return this.statusValue == STATUS_READY || this.statusValue == STATUS_NEED_TO_RELOAD_INDEXES;
    }

    public synchronized void setReloadInProgress() {
        this.previousValue = this.statusValue;
        this.statusValue = STATUS_RELOADING_INDEXES_IN_PROGRESS;
    }

    public synchronized void rollbackStatus() {
        this.statusValue = this.previousValue;
    }

    public synchronized void setReadyIfPossible() {
        if (this.statusValue != STATUS_NEED_TO_RELOAD_INDEXES) {
            this.statusValue = STATUS_READY;
        }
    }
}
