/*
 * Copyright 2018-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.entando;

import org.apache.commons.dbcp2.BasicDataSource;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;


//TODO duplicata, vedi ConfigTestUtils
public class TestEntandoJndiUtils {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(TestEntandoJndiUtils.class);

    public static void setupJndi() {
        InitialContext builder = null;
        try {
            String path = "target/test/conf/contextTestParams.properties";
            logger.debug("CREATING JNDI RESOURCES BASED ON {} (test)", path);

            InputStream in = new FileInputStream(path);
            System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.osjava.sj.MemoryContextFactory");
            System.setProperty("org.osjava.sj.jndi.shared", "true");
            System.setProperty("org.osjava.sj.jndi.ignoreClose", "true");
            builder = new InitialContext();
            builder.createSubcontext("java:comp/env");
            builder.createSubcontext("java:comp/env/jdbc");

            Properties testConfig = new Properties();
            testConfig.load(in);
            in.close();

            buildContextProperties(builder, testConfig);

            createDatasources(builder, testConfig);
//            builder.activate();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException("Error on creation naming context", t);
        }

    }

    private static void buildContextProperties(InitialContext builder, Properties testConfig) throws NamingException {
        builder.bind("java:comp/env/logName", testConfig.getProperty("logName"));
        builder.bind("java:comp/env/logFileRotatePattern", testConfig.getProperty("logFileRotatePattern"));
        builder.bind("java:comp/env/logLevel", testConfig.getProperty("logLevel"));
        builder.bind("java:comp/env/logFileSize", testConfig.getProperty("logFileSize"));
        builder.bind("java:comp/env/logFilesCount", testConfig.getProperty("logFilesCount"));

        builder.bind("java:comp/env/configVersion", testConfig.getProperty("configVersion"));

        builder.bind("java:comp/env/applicationBaseURL", testConfig.getProperty("applicationBaseURL"));
        builder.bind("java:comp/env/resourceRootURL", testConfig.getProperty("resourceRootURL"));
        builder.bind("java:comp/env/protectedResourceRootURL", testConfig.getProperty("protectedResourceRootURL"));
        builder.bind("java:comp/env/resourceDiskRootFolder", testConfig.getProperty("resourceDiskRootFolder"));
        builder.bind("java:comp/env/protectedResourceDiskRootFolder", testConfig.getProperty("protectedResourceDiskRootFolder"));

        builder.bind("java:comp/env/indexDiskRootFolder", testConfig.getProperty("indexDiskRootFolder"));
        builder.bind("java:comp/env/portDataSourceClassName", testConfig.getProperty("portDataSourceClassName"));
        builder.bind("java:comp/env/servDataSourceClassName", testConfig.getProperty("servDataSourceClassName"));
//        Iterator<Entry<Object, Object>> configIter = testConfig.entrySet().iterator();
//        while (configIter.hasNext()) {
//            Entry<Object, Object> entry = configIter.next();
//            builder.bind("java:comp/env/" + (String) entry.getKey(), (String) entry.getValue());
//            logger.trace("{} : {}", entry.getKey(), entry.getValue());
//        }
    }

    private static void createDatasources(InitialContext builder, Properties testConfig) {
        List<String> dsNameControlKeys = new ArrayList<String>();
        Enumeration<Object> keysEnum = testConfig.keys();
        while (keysEnum.hasMoreElements()) {
            String key = (String) keysEnum.nextElement();
            if (key.startsWith("jdbc.")) {
                String[] controlKeys = key.split("\\.");
                String dsNameControlKey = controlKeys[1];
                if (!dsNameControlKeys.contains(dsNameControlKey)) {
                    createDatasource(dsNameControlKey, builder, testConfig);
                    dsNameControlKeys.add(dsNameControlKey);
                }
            }
        }
    }

    private static void createDatasource(String dsNameControlKey, InitialContext builder, Properties testConfig) {
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
            builder.bind("java:comp/env/jdbc/" + beanName, ds);
        } catch (Throwable t) {
            throw new RuntimeException("Error on creation datasource '" + beanName + "'", t);
        }
        logger.debug("created datasource {}", beanName);
    }

    public static void destroyContext(ApplicationContext applicationContext) throws Exception {
        if (applicationContext instanceof AbstractApplicationContext) {
            ((AbstractApplicationContext) applicationContext).close();
        }
    }

    public static void destroyJndiContext() {
        try {
            InitialContext context = new InitialContext();
            context.destroySubcontext("java:comp/env/jdbc");
            context.destroySubcontext("java:comp/env");
            // Clear system properties to allow fresh context creation
            System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
            System.clearProperty("org.osjava.sj.jndi.shared");
            System.clearProperty("org.osjava.sj.jndi.ignoreClose");
        } catch (Exception e) {
            // Ignore exceptions during cleanup
            logger.debug("Error destroying JNDI context: {}", e.getMessage());
        }
    }
}
