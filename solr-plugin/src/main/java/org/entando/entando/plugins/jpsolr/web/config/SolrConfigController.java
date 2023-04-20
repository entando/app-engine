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
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.entando.entando.aps.system.exception.RestServerError;
import org.entando.entando.plugins.jpsolr.aps.system.solr.ISolrSearchEngineManager;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.ContentTypeSettings;
import org.entando.entando.plugins.jpsolr.conditions.SolrActive;
import org.entando.entando.web.common.annotation.RestAccessControl;
import org.entando.entando.web.common.model.SimpleRestResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author E.Santoboni
 */
@Slf4j
@SolrActive(true)
@RestController
@RequestMapping(value = "/plugins/solr")
public class SolrConfigController {

    public static final String CONTENT_TYPE_CODE = "contentTypeCode";

    private final ICmsSearchEngineManager solrSearchEngineManager;

    @Autowired
    public SolrConfigController(ICmsSearchEngineManager solrSearchEngineManager) {
        this.solrSearchEngineManager = solrSearchEngineManager;
    }

    private ISolrSearchEngineManager getSolrSearchEngineManager() {
        return (ISolrSearchEngineManager) solrSearchEngineManager;
    }

    @RestAccessControl(permission = Permission.SUPERUSER)
    @GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimpleRestResponse<List<ContentTypeSettings>>> getConfig() {
        log.debug("getting solr config");
        try {
            List<ContentTypeSettings> settings = this.getSolrSearchEngineManager().getContentTypesSettings();
            return new ResponseEntity<>(new SimpleRestResponse<>(settings), HttpStatus.OK);
        } catch (Exception e) {
            throw new RestServerError("Error extracting configuration", e);
        }
    }

    @RestAccessControl(permission = Permission.SUPERUSER)
    @PostMapping("/config/{contentTypeCode}")
    public ResponseEntity<SimpleRestResponse<Map<String, String>>> reloadReferences(
            @PathVariable String contentTypeCode) {
        log.debug("REST request - reload content type references {}", contentTypeCode);
        try {
            this.getSolrSearchEngineManager().refreshContentType(contentTypeCode);
            Map<String, String> result = Map.of(
                    "status", "success",
                    CONTENT_TYPE_CODE, contentTypeCode
            );
            return ResponseEntity.ok(new SimpleRestResponse<>(result));
        } catch (Exception e) {
            throw new RestServerError("Error refreshing type " + contentTypeCode, e);
        }
    }

}
