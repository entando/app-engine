package org.entando.entando.plugins.jpsolr;

import java.lang.reflect.Field;
import java.util.Map;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.common.SolrException;
import org.entando.entando.aps.system.services.searchengine.SolrEnvironmentVariables;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;

public class SolrTestExtension implements BeforeAllCallback {

    public static final String RECREATE_CORE = "RECREATE_CORE";

    private static final int SOLR_PORT = 8983;
    private static final String SOLR_IMAGE = "solr:8.11.1";
    private static final String SOLR_CORE = "entando";

    private static GenericContainer solrContainer;

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        if (solrContainer == null) {
            solrContainer = new GenericContainer(SOLR_IMAGE).withExposedPorts(SOLR_PORT)
                    .withCommand("solr-precreate", SOLR_CORE);
            solrContainer.start();

            updateEnv("SOLR_ADDRESS", "http://localhost:" + solrContainer.getMappedPort(SOLR_PORT) + "/solr");
        }

        if (extensionContext.getTags().contains(RECREATE_CORE)) {
            solrContainer.execInContainer("bin/solr", "delete", "-c", SOLR_CORE);
            solrContainer.execInContainer("bin/solr", "create_core", "-c", SOLR_CORE);
        }

        waitSolrReady();
    }

    /**
     * Sometimes Solr is not ready even if the container is ready. This method attempts to read the schema fields.
     */
    private void waitSolrReady() throws Exception {
        int attempt = 0;
        do {
            SolrClient solrClient = new HttpSolrClient.Builder(SolrEnvironmentVariables.solrAddress())
                    .withConnectionTimeout(10000)
                    .withSocketTimeout(60000)
                    .build();
            try {
                solrClient.request(new SchemaRequest.Fields(), SolrEnvironmentVariables.solrCore());
                return;
            } catch (SolrServerException | SolrException ex) {
                attempt++;
            } finally {
                solrClient.close();
            }
        } while (attempt < 10);
    }

    /**
     * Some tests that use this extension start instances of NotifyingThread where we need to override the SOLR_ADDRESS
     * value. Unfortunately Mockito.mockStatic doesn't work in secondary threads, so we can't mock
     * SolrEnvironmentVariables static methods as done in RedisTestExtension. The following method is a workaround that
     * manipulates the env map directly.
     */
    private static void updateEnv(String name, String val) throws Exception {
        Map<String, String> env = System.getenv();
        Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        ((Map<String, String>) field.get(env)).put(name, val);
    }
}
