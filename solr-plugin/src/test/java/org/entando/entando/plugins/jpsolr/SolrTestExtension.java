package org.entando.entando.plugins.jpsolr;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;

public class SolrTestExtension implements BeforeAllCallback {

    public static final String RECREATE_CORE = "RECREATE_CORE";

    private static final int SOLR_PORT = 8983;
    private static final String SOLR_IMAGE = "solr:9";
    private static final String SOLR_CORE = "entando";

    private static GenericContainer solrContainer;

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        if (solrContainer == null) {
            solrContainer = new GenericContainer(SOLR_IMAGE).withExposedPorts(SOLR_PORT)
                    .withCommand("solr-precreate", SOLR_CORE);
            solrContainer.start();

            System.setProperty("SOLR_ADDRESS", "http://localhost:" + solrContainer.getMappedPort(SOLR_PORT) + "/solr");
            System.setProperty("SOLR_CORE", SOLR_CORE);
        }

        if (extensionContext.getTags().contains(RECREATE_CORE)) {
            solrContainer.execInContainer("bin/solr", "delete", "-c", SOLR_CORE);
            solrContainer.execInContainer("bin/solr", "create_core", "-c", SOLR_CORE);
        }
    }
}
