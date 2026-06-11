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
package com.agiletec;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

import jakarta.servlet.ServletContext;

import org.apache.commons.dbcp2.BasicDataSource;
import org.entando.entando.aps.system.init.InitializerManager;
import org.entando.entando.ent.util.EntLogging;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.XmlWebApplicationContext;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Classe di utilità per i test. Fornisce la lista di file di configurazione di
 * spring e il contesto. La classe và estesa nel caso si intenda modificare od
 * aggiungere file di configurazione di spring esterni.
 *
 * @author W.Ambu - E.Santoboni
 */
public class ConfigTestUtils {

    private static final EntLogging.EntLogger logger = EntLogging.EntLogFactory.getSanitizedLogger(ConfigTestUtils.class);

    /**
     * Crea e restituisce il Contesto dell'Applicazione.
     *
     * @param srvCtx Il Contesto della Servlet.
     * @return Il Contesto dell'Applicazione.
     */
    public ApplicationContext createApplicationContext(ServletContext srvCtx) {
        this.createNamingContext();
        XmlWebApplicationContext applicationContext = new XmlWebApplicationContext();
        applicationContext.setConfigLocations(this.getSpringConfigFilePaths());
        applicationContext.setServletContext(srvCtx);
        ContextLoader contextLoader = new ContextLoader(applicationContext);
        contextLoader.initWebApplicationContext(srvCtx);
        applicationContext.refresh();
        // Post-init processes used to be triggered during refresh() by SystemPostProcessor;
        // in production they are now executed by StartupListener after the context is fully
        // initialized. Replicate the same behavior here for the test context.
        applicationContext.getBean(InitializerManager.class).executePostInitProcesses();
        return applicationContext;
    }

    /**
     * JNDI setup using Simple-JNDI. This part remains unchanged.
     */
    protected void createNamingContext() {
        try {
            String path = "target/test/conf/contextTestParams.properties";
            logger.debug("ATTEMPTING TO CREATE JNDI RESOURCES BASED ON " + path + " (test)");

            // Use the exact same approach as TestEntandoJndiUtils which works
            System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.osjava.sj.MemoryContextFactory");
            System.setProperty("org.osjava.sj.jndi.shared", "true");

            // Additional Simple-JNDI properties for better context sharing
            System.setProperty("org.osjava.sj.jndi.ignoreClose", "true");

            // Load properties file to get the JNDI names for datasources
            Properties testConfig = new Properties();
            try (InputStream in = new FileInputStream(path)) {
                testConfig.load(in);
            }

            InitialContext builder = new InitialContext();

            try {
                builder.createSubcontext("java:comp/env");
            } catch (javax.naming.NameAlreadyBoundException e) {
                // Context already exists, continue
                logger.debug("Context already exists, continue");
            }
            try {
                builder.createSubcontext("java:comp/env/jdbc");
            } catch (javax.naming.NameAlreadyBoundException e) {
                // Context already exists, continue
                logger.debug("Context already exists, continue");
            }

            buildContextProperties(builder, testConfig);

            createDatasources(builder, testConfig);

            logger.debug("JNDI RESOURCES CREATED SUCCESSFULLY");
        } catch (Throwable t) {
            logger.debug("JNDI setup failed. " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static void buildContextProperties(InitialContext builder, Properties testConfig) throws NamingException {
        bindOrRebind(builder, "java:comp/env/logName", testConfig.getProperty("logName"));
        bindOrRebind(builder, "java:comp/env/logFileRotatePattern", testConfig.getProperty("logFileRotatePattern"));
        bindOrRebind(builder, "java:comp/env/logLevel", testConfig.getProperty("logLevel"));
        bindOrRebind(builder, "java:comp/env/logFileSize", testConfig.getProperty("logFileSize"));
        bindOrRebind(builder, "java:comp/env/logFilesCount", testConfig.getProperty("logFilesCount"));

        bindOrRebind(builder, "java:comp/env/configVersion", testConfig.getProperty("configVersion"));

        bindOrRebind(builder, "java:comp/env/applicationBaseURL", testConfig.getProperty("applicationBaseURL"));
        bindOrRebind(builder, "java:comp/env/resourceRootURL", testConfig.getProperty("resourceRootURL"));
        bindOrRebind(builder, "java:comp/env/protectedResourceRootURL", testConfig.getProperty("protectedResourceRootURL"));
        bindOrRebind(builder, "java:comp/env/resourceDiskRootFolder", testConfig.getProperty("resourceDiskRootFolder"));
        bindOrRebind(builder, "java:comp/env/protectedResourceDiskRootFolder", testConfig.getProperty("protectedResourceDiskRootFolder"));

        bindOrRebind(builder, "java:comp/env/indexDiskRootFolder", testConfig.getProperty("indexDiskRootFolder"));
        bindOrRebind(builder, "java:comp/env/portDataSourceClassName", testConfig.getProperty("portDataSourceClassName"));
        bindOrRebind(builder, "java:comp/env/servDataSourceClassName", testConfig.getProperty("servDataSourceClassName"));
    }

    private static void bindOrRebind(InitialContext context, String name, Object value) throws NamingException {
        try {
            context.bind(name, value);
        } catch (javax.naming.NameAlreadyBoundException e) {
            context.rebind(name, value);
        }
    }

    private void createDatasources(InitialContext builder, Properties testConfig) {
        List<String> dsNameControlKeys = new ArrayList<String>();
        Enumeration<Object> keysEnum = testConfig.keys();
        while (keysEnum.hasMoreElements()) {
            String key = (String) keysEnum.nextElement();
            if (key.startsWith("jdbc.")) {
                String[] controlKeys = key.split("\\.");
                String dsNameControlKey = controlKeys[1];
                if (!dsNameControlKeys.contains(dsNameControlKey)) {
                    this.createDatasource(dsNameControlKey, builder, testConfig);
                    dsNameControlKeys.add(dsNameControlKey);
                }
            }
        }
    }

    private void createDatasource(String dsNameControlKey, InitialContext builder, Properties testConfig) {
        String beanName = testConfig.getProperty("jdbc." + dsNameControlKey + ".beanName");
        try {
            String className = testConfig.getProperty("jdbc." + dsNameControlKey + ".driverClassName");
            String url = testConfig.getProperty("jdbc." + dsNameControlKey + ".url");
            String username = testConfig.getProperty("jdbc." + dsNameControlKey + ".username");
            String password = testConfig.getProperty("jdbc." + dsNameControlKey + ".password");
            Class.forName(className);
            BasicDataSource ds = new BasicDataSource();
            ds.setUrl(url);
            ds.setUsername(username);
            ds.setPassword(password);
            ds.setMaxTotal(12);
            ds.setMaxIdle(4);
            ds.setDriverClassName(className);
            bindOrRebind(builder, "java:comp/env/jdbc/" + beanName, ds);
            logger.debug("created datasource " + beanName);
        } catch (Throwable t) {
            throw new RuntimeException("Error on creation datasource '" + beanName + "'", t);
        }
    }

    /**
     * Restituisce l'insieme dei file di configurazione dei bean definiti nel
     * sistema. Il metodo và esteso nel caso si inseriscano file di
     * configurazioni esterni al Core ed ai Plugin.
     *
     * @return L'insieme dei file di configurazione definiti nel sistema.
     */
    protected String[] getSpringConfigFilePaths() {
        String[] filePaths = new String[6];
        filePaths[0] = "classpath:spring/testpropertyPlaceholder.xml";
        filePaths[1] = "classpath:spring/baseSystemConfig.xml";
        filePaths[2] = "classpath*:spring/aps/**/**.xml";
        filePaths[3] = "classpath*:spring/apsadmin/**/**.xml";
        filePaths[4] = "classpath*:spring/plugins/**/aps/**/**.xml";
        filePaths[5] = "classpath*:spring/plugins/**/apsadmin/**/**.xml";
        return filePaths;
    }

    public void destroyContext(ApplicationContext applicationContext) throws Exception {
        if (applicationContext instanceof AbstractApplicationContext) {
            ((AbstractApplicationContext) applicationContext).close();
        }
    }

    /**
     * Effettua la chiusura dei datasource definiti nel contesto.
     *
     * @param applicationContext Il contesto dell'applicazione.
     * @throws Exception In caso di errore nel recupero o chiusura dei
     * DataSource.
     */
    public void closeDataSources(ApplicationContext applicationContext) throws Exception {
        String[] dataSourceNames = applicationContext.getBeanNamesForType(BasicDataSource.class);
        for (int i = 0; i < dataSourceNames.length; i++) {
            BasicDataSource dataSource = (BasicDataSource) applicationContext.getBean(dataSourceNames[i]);
            if (null != dataSource) {
                dataSource.close();
            }
        }
    }

}
