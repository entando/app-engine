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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.MultiUpdate;
import org.apache.solr.client.solrj.request.schema.SchemaRequest.Update;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.entando.entando.plugins.jpsolr.aps.system.solr.model.SolrFields;

@Slf4j
public class SolrSchemaDAO implements ISolrSchemaDAO {

    private final SolrClient solrClient;
    private final String solrCore;

    public SolrSchemaDAO(SolrClient solrClient, String solrCore) {
        this.solrClient = solrClient;
        this.solrCore = solrCore;
    }

    @Override
    public List<Map<String, ?>> getFields() {
        SchemaRequest.Fields getFieldsRequest = new SchemaRequest.Fields();
        List<Map<String, ?>> fields = new ArrayList<>();
        try {
            List<SimpleOrderedMap<Object>> items = (List<SimpleOrderedMap<Object>>) solrClient.request(getFieldsRequest, this.solrCore).get("fields");
            for (SimpleOrderedMap<Object> item : items) {
                fields.add(item.asMap());
            }
        } catch (SolrServerException | IOException ex) {
            log.error("Error retrieving fields from Solr", ex);
        }
        return fields;
    }

    @Override
    public synchronized boolean updateFields(List<Map<String, ?>> fieldsToAdd, List<Map<String, ?>> fieldsToReplace) {
        try {
            List<Update> updates = new ArrayList<>();
            for (Map<String, ?> fieldToAdd : fieldsToAdd) {
                SchemaRequest.AddField addFieldRequest = new SchemaRequest.AddField((Map<String, Object>) fieldToAdd);
                updates.add(addFieldRequest);
            }
            for (Map<String, ?> fieldToReplace : fieldsToReplace) {
                SchemaRequest.ReplaceField replaceFieldRequest
                        = new SchemaRequest.ReplaceField((Map<String, Object>) fieldToReplace);
                updates.add(replaceFieldRequest);
            }
            SchemaRequest.MultiUpdate multiUpdateRequest = new MultiUpdate(updates);
            solrClient.request(multiUpdateRequest, this.solrCore);
            if (null != fieldsToAdd) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    List<String> addedFields = fieldsToAdd.stream().map(m -> m.get(SolrFields.SOLR_FIELD_NAME).toString()).collect(Collectors.toList());
                    int counter = 0;
                    while (counter < 10 && !checkCondition(addedFields)) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                            log.info("Check of added fields - fields {} - counter {}", addedFields, counter);
                        } catch (InterruptedException e) {
                            log.error("Error sleeping", e);
                            Thread.currentThread().interrupt();
                        } finally {
                            counter++;
                        }
                    }
                });
                future.get();
            }
        } catch (ExecutionException | InterruptedException ex) {
            log.error("Error executing check of fields", ex);
            Thread.currentThread().interrupt();
            return false;
        } catch (SolrServerException | IOException ex) {
            log.error("Error executing Solr multi-update request", ex);
            return false;
        }
        return true;
    }
    
    private boolean checkCondition(List<String> addedFields) {
        List<Map<String, ?>> fields = this.getFields();
        List<String> fieldsAlreadyPresent = fields.stream().map(m -> m.get(SolrFields.SOLR_FIELD_NAME).toString()).collect(Collectors.toList());
        return fieldsAlreadyPresent.containsAll(addedFields);
    }
    
}
