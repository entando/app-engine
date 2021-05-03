/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.entando.entando.plugins.jpsolr.aps.system.solr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.client.RestTemplate;

/**
 * @author E.Santoboni
 */
public class SolrSchemaClient {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(SolrSchemaClient.class);

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
        return executePost(solrUrl, core, properties, "add-field");
    }

    public static boolean replaceField(String solrUrl, String core, Map<String, Object> properties) {
        return executePost(solrUrl, core, properties, "replace-field");
    }

    public static boolean deleteField(String solrUrl, String core, String fieldKey) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", fieldKey);
        return executePost(solrUrl, core, properties, "delete-field");
    }

    private static boolean executePost(String solrUrl, String core, Map<String, Object> properties, String actionName) {
        JSONObject request = new JSONObject().put(actionName, properties);
        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = solrUrl.endsWith("/") ? solrUrl : solrUrl + "/";
        String url = baseUrl + core + "/schema";
        String response = restTemplate.postForObject(url, request.toString(), String.class);
        JSONObject obj = new JSONObject(response);
        int resultType = obj.getJSONObject("responseHeader").getInt("status");
        if (resultType != 0) {
            logger.error("invalid response --> " + response);
            return false;
        }
        return true;
    }


}
