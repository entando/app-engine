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
package com.agiletec.aps;

import com.agiletec.ConfigTestUtils;
import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.IManager;
import com.agiletec.aps.system.common.notify.NotifyManager;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author W.Ambu - E.Santoboni
 */
public class BaseTestCase {

    private static ApplicationContext applicationContext;
    private static MockServletContext servletContext;
    private static MockHttpServletRequest request;
    private static RequestContext reqCtx;

    @BeforeAll
    public static void setUp() throws Exception {
        boolean refresh = false;
        if (null == applicationContext) {
            // Link the servlet context and the Spring context
            servletContext = new MockServletContext("", new FileSystemResourceLoader());
            applicationContext = getConfigUtils().createApplicationContext(servletContext);
            servletContext.setAttribute(
                    WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);
        } else {
            refresh = true;
        }
        reqCtx = createRequestContext(applicationContext, servletContext);
        request = createRequest();
        request.setAttribute(RequestContext.REQCTX, reqCtx);
        request.setSession(new MockHttpSession(servletContext));
        reqCtx.setRequest(request);
        reqCtx.setResponse(new MockHttpServletResponse());
        if (refresh) {
            try {
                ApsWebApplicationUtils.executeSystemRefresh(request);
                waitNotifyingThread();
            } catch (Throwable e) {
            }
        }
    }

    public static RequestContext createRequestContext(ApplicationContext applicationContext, ServletContext srvCtx) {
        RequestContext reqCtx = new RequestContext();
        srvCtx.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);
        ILangManager langManager = (ILangManager) applicationContext.getBean(SystemConstants.LANGUAGE_MANAGER);
        Lang defaultLang = langManager.getDefaultLang();
        reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG, defaultLang);
        return reqCtx;
    }

    public static MockHttpServletRequest createRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("www.entando.com");
        request.addHeader("Host", "www.entando.com");
        request.setContextPath("/Entando");
        return request;
    }

    @AfterAll
    public static void tearDown() throws Exception {
        waitThreads(SystemConstants.ENTANDO_THREAD_NAME_PREFIX);
        Set<Thread> setOfThread = Thread.getAllStackTraces().keySet();
        //Iterate over set to find yours
        for (Thread thread : setOfThread) {
            if (thread.getName().matches("pool-(.*)-thread-(.*)")) {
                if (!thread.isInterrupted()) {
                    thread.interrupt();
                }
            }
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

    /**
     * Return a user (with his authority) by username.
     *
     * @param username The username
     * @param password The password
     * @return The required user.
     * @throws Exception In case of error.
     */
    protected static UserDetails getUser(String username, String password) throws Exception {
        IAuthenticationProviderManager provider = (IAuthenticationProviderManager) getService(
                SystemConstants.AUTHENTICATION_PROVIDER_MANAGER);
        IUserManager userManager = (IUserManager) getService(SystemConstants.USER_MANAGER);
        UserDetails user = null;
        if (username.equals(SystemConstants.GUEST_USER_NAME)) {
            user = userManager.getGuestUser();
        } else {
            user = provider.getUser(username, password);
        }
        return user;
    }

    /**
     * Return a user (with his autority) by username, with the password equals than username.
     *
     * @param username The username
     * @return The required user.
     * @throws Exception In case of error.
     */
    protected static UserDetails getUser(String username) throws Exception {
        return getUser(username, username);
    }

    protected static void setUserOnSession(String username) throws Exception {
        UserDetails currentUser = getUser(username);
        HttpSession session = request.getSession();
        session.setAttribute(SystemConstants.SESSIONPARAM_CURRENT_USER, currentUser);
    }

    protected static RequestContext getRequestContext() {
        return reqCtx;
    }

    protected static IManager getService(String name) {
        return (IManager) getApplicationContext().getBean(name);
    }

    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    protected static ConfigTestUtils getConfigUtils() {
        return new ConfigTestUtils();
    }

}
