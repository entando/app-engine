/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.common.notify.NotifyManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @author E.Santoboni
 */
public class SolrTestUtils {

	public static void startContainer() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", "docker-compose up -d");
        executeCommand(processBuilder);
	}
    
    public static void stopContainer() throws Exception {
        BaseTestCase.tearDown();
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", "docker-compose stop && docker-compose rm -f");
        executeCommand(processBuilder);
    }
    
    private static void executeCommand(ProcessBuilder processBuilder) throws Exception {
        try {
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
            }
            int exitVal = process.waitFor();
            if (exitVal == 0) {
                System.out.println("executed!");
                System.out.println(output);
            } else {
                System.out.println("Invalid exit! code " + exitVal);
            }
        } catch (Exception e) {
            throw e;
        }
    }

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
    
}
