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
package org.entando.entando.plugins.jpsolr.aps.system.solr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * @author E.Santoboni
 */
public class SolrSchemaClient {

    private static final Logger logger = LoggerFactory.getLogger(SolrSchemaClient.class);

    private SolrSchemaClient() {
        throw new IllegalStateException("Utility class");
    }

    public static List<Map<String, Object>> getFields(String solrUrl, String core) {
        List<Map<String, Object>> params = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = solrUrl.endsWith("/") ? solrUrl : solrUrl + "/";
        String url = baseUrl + core + "/schema/fields";
        String response = restTemplate.getForObject(url, String.class);
        JSONObject obj = new JSONObject(response);
        JSONArray jsonFields = obj.getJSONArray("fields");
        Iterator<Object> iter = jsonFields.iterator();
        while (iter.hasNext()) {
            JSONObject item = (JSONObject) iter.next();
            params.add(item.toMap());
        }
        return params;
    }

    public static boolean addField(String solrUrl, String core, Map<String, Object> properties) {
        return executePost(solrUrl, core, "add-field", properties);
    }

    public static boolean replaceField(String solrUrl, String core, Map<String, Object> properties) {
        return executePost(solrUrl, core, "replace-field", properties);
    }

    public static boolean deleteField(String solrUrl, String core, String fieldKey) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", fieldKey);
        return executePost(solrUrl, core, "delete-field", properties);
    }

    public static boolean deleteAllDocuments(String solrUrl, String core) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("query", "*:*");
        return executePost(solrUrl, core, "/update", "delete", properties);
    }

    private static boolean executePost(String solrUrl, String core, String actionName, Map<String, Object> properties) {
        return executePost(solrUrl, core, "/schema", actionName, properties);
    }

    private static boolean executePost(String solrUrl, String core, String subPath, String actionName,
            Map<String, Object> properties) {
        String baseUrl = solrUrl.endsWith("/") ? solrUrl : solrUrl + "/";
        String url = baseUrl + core + subPath;
        String response = null;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            JSONObject request = new JSONObject().put(actionName, properties);
            HttpEntity<String> entity = new HttpEntity<>(request.toString(), headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JSONObject obj = new JSONObject(responseEntity.getBody());
            int resultType = obj.getJSONObject("responseHeader").getInt("status");
            if (resultType != 0) {
                logger.error("invalid response --> " + response);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error calling Post {} - properties {} - response {} - errorMessage {}", url, properties,
                    response, e.getMessage());
            return false;
        }
        return true;
    }

}
