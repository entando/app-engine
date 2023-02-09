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
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.entando.entando.aps.system.exception.RestServerError;
import org.entando.entando.plugins.jpsolr.aps.system.solr.ISolrSearchEngineManager;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.ContentTypeSettings;
import org.entando.entando.web.common.annotation.RestAccessControl;
import org.entando.entando.web.common.model.SimpleRestResponse;
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

    public static final String CONTENT_TYPE_CODE = "contentTypeCode";

    @Autowired
    private ISolrSearchEngineManager solrSearchEngineManager;

    protected ISolrSearchEngineManager getSolrSearchEngineManager() {
        return solrSearchEngineManager;
    }

    public void setSolrSearchEngineManager(ISolrSearchEngineManager solrSearchEngineManager) {
        this.solrSearchEngineManager = solrSearchEngineManager;
    }

    @RestAccessControl(permission = {Permission.SUPERUSER})
    @GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ContentTypeSettings>> getConfig() {
        logger.debug("getting solr config");
        try {
            List<ContentTypeSettings> settings = this.getSolrSearchEngineManager().getContentTypesSettings();
            return new ResponseEntity<>(settings, HttpStatus.OK);
        } catch (Exception e) {
            throw new RestServerError("Error extracting configuration", e);
        }
    }

    @RestAccessControl(permission = Permission.SUPERUSER)
    @PostMapping("/config/{contentTypeCode}")
    public ResponseEntity<SimpleRestResponse<Map>> reloadReferences(@PathVariable String contentTypeCode) {
        logger.debug("REST request - reload content type references {}", contentTypeCode);
        try {
            this.getSolrSearchEngineManager().refreshContentType(contentTypeCode);
            Map<String, String> result = ImmutableMap.of(
                    "status", "success",
                    CONTENT_TYPE_CODE, contentTypeCode
            );
            return ResponseEntity.ok(new SimpleRestResponse<>(result));
        } catch (Exception e) {
            throw new RestServerError("Error refreshing type " + contentTypeCode, e);
        }
    }

}
