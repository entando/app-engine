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
package org.entando.entando.plugins.jpsolr;

import com.agiletec.aps.system.common.notify.NotifyManager;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.common.SolrException;
import org.entando.entando.aps.system.services.searchengine.SolrEnvironmentVariables;
import org.testcontainers.containers.GenericContainer;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

/**
 * @author E.Santoboni
 */
public class SolrTestUtils {
    
    private static final int SOLR_PORT = 8983;
    private static final String SOLR_IMAGE = "solr:9";
    private static final String SOLR_CORE = "entando";

    public static void waitNotifyingThread() throws InterruptedException {
        waitThreads(NotifyManager.NOTIFYING_THREAD_NAME);
    }

    public static void waitThreads(String threadNamePrefix) throws InterruptedException {
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        for (int i = 0; i < threads.length; i++) {
            Thread currentThread = threads[i];
            if (currentThread != null
                    && currentThread.getName().startsWith(threadNamePrefix)) {
                currentThread.join();
            }
        }
    }
    
    public static GenericContainer startContainer(GenericContainer solrContainer, EnvironmentVariables environmentVariables) throws Exception {
        if (solrContainer == null) {
            solrContainer = new GenericContainer(SOLR_IMAGE).withExposedPorts(SOLR_PORT)
                    .withCommand("solr-precreate", SOLR_CORE);
            solrContainer.start();
            environmentVariables.set("SOLR_ADDRESS", "http://localhost:" + solrContainer.getMappedPort(SOLR_PORT) + "/solr");
        }
        waitSolrReady();
        return solrContainer;
    }

    /**
     * Sometimes Solr is not ready even if the container is ready. This method attempts to read the schema fields.
     */
    private static void waitSolrReady() throws Exception {
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
    
    
    
    

}
