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
package org.entando.entando.plugins.jpsolr.web;

import com.agiletec.aps.system.common.notify.NotifyManager;
import javax.annotation.Resource;

import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import com.agiletec.aps.system.services.user.UserDetails;
import javax.servlet.Filter;
import org.entando.entando.TestEntandoJndiUtils;
import org.entando.entando.aps.system.services.oauth2.IApiOAuth2TokenManager;
import org.entando.entando.web.AuthRequestBuilder;
import org.entando.entando.web.common.interceptor.EntandoOauth2Interceptor;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CorsFilter;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
    "classpath*:spring/testpropertyPlaceholder.xml",
    "classpath*:spring/baseSystemConfig.xml",
    "classpath*:spring/aps/**/**.xml",
    "classpath*:spring/apsadmin/**/**.xml",
    "classpath*:spring/plugins/**/aps/**/**.xml",
    "classpath*:spring/plugins/**/apsadmin/**/**.xml",
        "classpath*:spring/plugins/jpsolr/aps/**.xml", //extension of default class
        "classpath*:spring/plugins/jpsolr/apsadmin/**.xml", //extension of default class
    "classpath*:spring/web/**.xml",})
@WebAppConfiguration(value = "")
public class AbstractControllerIntegrationTest {

    protected MockMvc mockMvc;

    private String accessToken;

    @Resource
    protected WebApplicationContext webApplicationContext;

    @Mock
    protected IApiOAuth2TokenManager apiOAuth2TokenManager;

    @Mock
    protected IAuthenticationProviderManager authenticationProviderManager;

    @Mock
    protected IAuthorizationManager authorizationManager;

    @Autowired
    protected CorsFilter corsFilter;

    @Autowired
    @InjectMocks
    protected EntandoOauth2Interceptor entandoOauth2Interceptor;
    
    @BeforeAll
    public static void setup() throws Exception {
        TestEntandoJndiUtils.setupJndi();
    }
    
    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(corsFilter).build();
        //workaround for dirty context
        entandoOauth2Interceptor.setAuthenticationProviderManager(authenticationProviderManager);
    }
    
    protected String mockOAuthInterceptor(UserDetails user) {
        return OAuth2TestUtils.mockOAuthInterceptor(apiOAuth2TokenManager, authenticationProviderManager, authorizationManager, user);
    }
    
    protected AuthRequestBuilder createAuthRequest(MockHttpServletRequestBuilder requestBuilder) {
        return new AuthRequestBuilder(mockMvc, getAccessToken(), requestBuilder);
    }

    private String getAccessToken() {
        if (this.accessToken == null) {
            UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
            this.accessToken = OAuth2TestUtils.mockOAuthInterceptor(apiOAuth2TokenManager, authenticationProviderManager, authorizationManager, user);
        }
        return this.accessToken;
    }
    
    protected void waitNotifyingThread() throws InterruptedException {
        this.waitThreads(NotifyManager.NOTIFYING_THREAD_NAME);
    }

    protected void waitThreads(String threadNamePrefix) throws InterruptedException {
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
