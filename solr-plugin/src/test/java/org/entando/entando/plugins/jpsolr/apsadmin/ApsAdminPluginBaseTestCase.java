/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.entando.entando.plugins.jpsolr.apsadmin;

import com.agiletec.ConfigTestUtils;
import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.IManager;
import com.agiletec.aps.system.common.notify.NotifyManager;
import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionProxy;
import com.opensymphony.xwork2.ActionProxyFactory;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.inject.Container;
import com.opensymphony.xwork2.inject.ContainerBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.dispatcher.Dispatcher;
import org.apache.struts2.dispatcher.HttpParameters;
import org.apache.struts2.dispatcher.Parameter;
import org.apache.struts2.spring.StrutsSpringObjectFactory;
import org.entando.entando.plugins.jpsolr.CustomConfigTestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;

public class ApsAdminPluginBaseTestCase {

    private static ApplicationContext applicationContext;
    private static Dispatcher dispatcher;
    private ActionProxy proxy;
    private static MockServletContext servletContext;
    private static MockHttpServletRequest request;
    private static MockHttpServletResponse response;
    private ActionSupport action;

    private Map<String, Parameter> parameters = new HashMap<String, Parameter>();

    @BeforeAll
    protected static void setUp() throws Exception {
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
        RequestContext reqCtx = BaseTestCase.createRequestContext(applicationContext, servletContext);
        request = new MockHttpServletRequest();
        request.setAttribute(RequestContext.REQCTX, reqCtx);
        response = new MockHttpServletResponse();
        request.setSession(new MockHttpSession(servletContext));
        if (refresh) {
            try {
                ApsWebApplicationUtils.executeSystemRefresh(request);
                waitNotifyingThread();
            } catch (Throwable e) {
            }
        }
        // Use spring as the object factory for Struts
        StrutsSpringObjectFactory ssf = new StrutsSpringObjectFactory(null, null, null, null, servletContext, null, createContainer());
        ssf.setApplicationContext(applicationContext);
        // Dispatcher is the guy that actually handles all requests.  Pass in
        // an empty Map as the parameters but if you want to change stuff like
        // what config files to read, you need to specify them here
        // (see Dispatcher's source code)
        java.net.URL url = ClassLoader.getSystemResource("struts.properties");
        Properties props = new Properties();
        props.load(url.openStream());
        setInitParameters(props);
        Map params = new HashMap(props);
        dispatcher = new Dispatcher(servletContext, params);
        dispatcher.init();
        Dispatcher.setInstance(dispatcher);
    }

    protected <T> T getContainerObject(Class<T> requiredType) {
        return this.dispatcher.getContainer().getInstance(requiredType);
    }

    protected static Container createContainer() {
        ContainerBuilder builder = new ContainerBuilder();
        builder.constant("devMode", "false");
        return builder.create(true);
    }

    @AfterAll
    protected static void tearDown() throws Exception {
        waitThreads(SystemConstants.ENTANDO_THREAD_NAME_PREFIX);
        // This should not be called after each test method, because the
        // applicationContext used in this class is a static instance.
        // dbcp-commons 1.4+ doesn't allow reusing a closed DataSource.
        //this.getConfigUtils().closeDataSources(this.getApplicationContext());
    }

    protected static void waitNotifyingThread() throws InterruptedException {
        waitThreads(NotifyManager.NOTIFYING_THREAD_NAME);
    }

    protected static void waitThreads(String threadNamePrefix) throws InterruptedException {
        Thread[] threads = new Thread[20];
        Thread.enumerate(threads);
        for (int i = 0; i < threads.length; i++) {
            Thread currentThread = threads[i];
            if (currentThread != null
                    && currentThread.getName().startsWith(threadNamePrefix)) {
                currentThread.join();
            }
        }
    }

    protected void initAction(String namespace, String name) throws Exception {
        this.initAction(namespace, name, false);
    }

    /**
     * Created action class based on namespace and name
     *
     * @param namespace The namespace
     * @param name The name of the action
     * @throws java.lang.Exception In case of error
     */
    protected void initAction(String namespace, String name, boolean refreshResponse) throws Exception {
        // create a proxy class which is just a wrapper around the action call.
        // The proxy is created by checking the namespace and name against the
        // struts.xml configuration
        ActionProxyFactory proxyFactory = (ActionProxyFactory) this.dispatcher.getContainer().getInstance(ActionProxyFactory.class);
        this.proxy = proxyFactory.createActionProxy(namespace, name, null, null, true, false);

        // set to true if you want to process Freemarker or JSP results
        this.proxy.setExecuteResult(false);
        // by default, don't pass in any request parameters

        // set the actions context to the one which the proxy is using
        this.proxy.getInvocation().getInvocationContext().setSession(new HashMap<>());
        ServletActionContext.setContext(this.proxy.getInvocation().getInvocationContext());
        ServletActionContext.setRequest(this.request);
        if (refreshResponse) {
            response = new MockHttpServletResponse();
            RequestContext reqCtx = (RequestContext) getRequest().getAttribute(RequestContext.REQCTX);
            reqCtx.setResponse(response);
        }
        ServletActionContext.setResponse(this.response);
        ServletActionContext.setServletContext(servletContext);
        this.action = (ActionSupport) this.proxy.getAction();

        //reset previsious params
        List<String> paramNames = new ArrayList<String>(this.request.getParameterMap().keySet());
        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = (String) paramNames.get(i);
            this.removeParameter(paramName);
        }
    }

    /**
     * Metodo da estendere in caso che si voglia impiantare un'altro
     * struts-config.
     *
     * @param params The parameters
     */
    protected static void setInitParameters(Properties params) {
        params.setProperty("config",
                "struts-default.xml,struts-plugin.xml,struts.xml,entando-struts-plugin.xml,japs-struts-plugin.xml");
    }

    /**
     * Return a user (with his autority) by username.
     *
     * @param username The username
     * @param password The password
     * @return The required user.
     * @throws Exception In case of error.
     */
    protected UserDetails getUser(String username, String password) throws Exception {
        IAuthenticationProviderManager provider = (IAuthenticationProviderManager) this.getService(SystemConstants.AUTHENTICATION_PROVIDER_MANAGER);
        IUserManager userManager = (IUserManager) this.getService(SystemConstants.USER_MANAGER);
        UserDetails user = null;
        if (username.equals(SystemConstants.GUEST_USER_NAME)) {
            user = userManager.getGuestUser();
        } else {
            user = provider.getUser(username, password);
        }
        return user;
    }

    /**
     * Return a user (with his autority) by username, with the password equals
     * than username.
     *
     * @param username The username
     * @return The required user.
     * @throws Exception In case of error.
     */
    protected UserDetails getUser(String username) throws Exception {
        return this.getUser(username, username);
    }

    protected void setUserOnSession(String username) throws Exception {
        if (null == username) {
            this.removeUserOnSession();
            return;
        }
        UserDetails currentUser = this.getUser(username, username);//nel database di test, username e password sono uguali
        HttpSession session = this.request.getSession();
        session.setAttribute(SystemConstants.SESSIONPARAM_CURRENT_USER, currentUser);
    }

    protected void removeUserOnSession() throws Exception {
        HttpSession session = this.request.getSession();
        session.removeAttribute(SystemConstants.SESSIONPARAM_CURRENT_USER);
    }

    protected void addParameters(Map params) {
        Iterator iter = params.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            this.addParameter(key, params.get(key).toString());
        }
    }

    protected void addParameter(String name, String[] values) {
        if (null != values) {
            this.addParameter(name, Arrays.asList(values));
        }
    }

    protected void addParameter(String name, Collection<String> value) {
        this.request.removeParameter(name);
        if (null == value) {
            return;
        }
        String[] array = new String[value.size()];
        Iterator<String> iter = value.iterator();
        int i = 0;
        while (iter.hasNext()) {
            String stringValue = iter.next();
            this.request.addParameter(name, stringValue);
            array[i++] = stringValue;
        }
        Parameter.Request parameter = new Parameter.Request(name, array);
        this.parameters.put(name, parameter);
    }

    protected void addParameter(String name, Object value) {
        this.request.removeParameter(name);
        if (null == value) {
            return;
        }
        this.request.addParameter(name, value.toString());
        Parameter.Request parameter = new Parameter.Request(name, value.toString());
        this.parameters.put(name, parameter);
    }

    protected void addAttribute(String name, Object value) {
        this.request.removeAttribute(name);
        if (null == value) {
            return;
        }
        this.request.setAttribute(name, value);
    }

    private void removeParameter(String name) {
        this.request.removeParameter(name);
        this.request.removeAttribute(name);
        this.parameters.remove(name);
    }

    protected String executeAction() throws Throwable {
        ActionContext ac = this.getActionContext();
        ac.setParameters(HttpParameters.create(this.request.getParameterMap()).build());
        ac.getParameters().appendAll(this.parameters);
        String result = this.proxy.execute();
        return result;
    }

    protected ActionInvocation getActionInvocation() {
        return this.proxy.getInvocation();
    }

    protected ActionContext getActionContext() {
        return this.getActionInvocation().getInvocationContext();
    }

    protected IManager getService(String name) {
        return (IManager) this.getApplicationContext().getBean(name);
    }

    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    protected static ConfigTestUtils getConfigUtils() {
        return new CustomConfigTestUtils();
    }

    protected ActionSupport getAction() {
        return this.action;
    }

    protected HttpServletRequest getRequest() {
        return this.request;
    }

}
