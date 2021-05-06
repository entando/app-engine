/*
 * Copyright 2021-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpsolr.web.config;

import com.agiletec.aps.system.services.role.Permission;
import java.util.List;
import org.entando.entando.plugins.jpsolr.aps.system.solr.ISolrSearchEngineManager;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.ContentTypeSettings;
import org.entando.entando.web.common.annotation.RestAccessControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author E.Santoboni
 */
@RestController
@RequestMapping(value = "/plugins/solr")
public class SolrConfigController {

    private static final Logger logger = LoggerFactory.getLogger(SolrConfigController.class);

    @Autowired
    private ISolrSearchEngineManager solrSearchEngineManager;

    protected ISolrSearchEngineManager getSolrSearchEngineManager() {
        return solrSearchEngineManager;
    }
    public void setSolrSearchEngineManager(ISolrSearchEngineManager solrSearchEngineManager) {
        this.solrSearchEngineManager = solrSearchEngineManager;
    }

    @RestAccessControl(permission = { Permission.SUPERUSER })
    @GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ContentTypeSettings>> getConfig() throws Exception {
        logger.debug("getting solr config");
        List<ContentTypeSettings> settings = this.getSolrSearchEngineManager().getContentTypesSettings();
        return new ResponseEntity<>(settings, HttpStatus.OK);
    }

}