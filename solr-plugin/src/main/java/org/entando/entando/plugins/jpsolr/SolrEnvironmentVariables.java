package org.entando.entando.plugins.jpsolr;

import org.apache.commons.lang3.StringUtils;

public final class SolrEnvironmentVariables {

    private static final String SOLR_ACTIVE = "SOLR_ACTIVE";
    private static final String SOLR_ADDRESS = "SOLR_ADDRESS";
    private static final String SOLR_CORE = "SOLR_CORE";

    private SolrEnvironmentVariables() {
    }

    public static boolean active() {
        return Boolean.toString(true).equals(System.getenv(SOLR_ACTIVE));
    }

    public static String solrAddress() {
        return get(SOLR_ADDRESS, "http://localhost:8983/solr");
    }

    public static String solrCore() {
        return get(SOLR_CORE, "entando");
    }

    private static String get(String name, String defaultValue) {
        String valueFromEnv = System.getenv(name);
        if (StringUtils.isBlank(valueFromEnv)) {
            return defaultValue;
        }
        return valueFromEnv;
    }
}
