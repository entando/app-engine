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
package org.entando.entando.plugins.jpsolr.aps.system.solr;

import com.agiletec.plugins.jacms.aps.system.services.searchengine.LastReloadInfo;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author E.Santoboni
 */
public class SolrLastReloadInfo extends LastReloadInfo implements Serializable {

    private Map<String, Date> datesByType = new HashMap<>();

    public Map<String, Date> getDatesByType() {
        return datesByType;
    }

    public void setDatesByType(Map<String, Date> datesByType) {
        this.datesByType = datesByType;
    }

    public Date getDateByType(String typeCode) {
        Date date = this.getDatesByType().get(typeCode);
        if (null == date) {
            return super.getDate();
        }
        return date;
    }

}
