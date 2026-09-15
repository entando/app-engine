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
package com.agiletec.aps.util;

import com.agiletec.aps.system.services.lang.ILangManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

@Isolated
class ApplicationContextProviderTest {

    private ConfigurableListableBeanFactory previousBeanFactory;

    @BeforeEach
    void setUp() {
        this.previousBeanFactory = ApplicationContextProvider.getBeanFactory();
        ApplicationContextProvider.clear();
    }

    @AfterEach
    void tearDown() {
        ApplicationContextProvider.clear();
        if (null != this.previousBeanFactory) {
            new ApplicationContextProvider().postProcessBeanFactory(this.previousBeanFactory);
        }
    }

    @Test
    void shouldCaptureAndReleaseTheBeanFactory() {
        ConfigurableListableBeanFactory factory = Mockito.mock(ConfigurableListableBeanFactory.class);
        new ApplicationContextProvider().postProcessBeanFactory(factory);
        Assertions.assertSame(factory, ApplicationContextProvider.getBeanFactory());
        ApplicationContextProvider.clear();
        Assertions.assertNull(ApplicationContextProvider.getBeanFactory());
    }

    @Test
    void getBeanShouldReturnNullWhenTheFactoryIsNotAvailableYet() {
        Assertions.assertNull(ApplicationContextProvider.getBean(ILangManager.class));
    }

    @Test
    void getBeanShouldReturnTheBeanOfTheCapturedFactory() {
        ILangManager langManager = Mockito.mock(ILangManager.class);
        new ApplicationContextProvider().postProcessBeanFactory(this.mockFactory(langManager));
        Assertions.assertSame(langManager, ApplicationContextProvider.getBean(ILangManager.class));
    }

    @Test
    void getBeanShouldReturnNullWhenTheFactoryCannotResolveTheBean() {
        ConfigurableListableBeanFactory factory = Mockito.mock(ConfigurableListableBeanFactory.class);
        Mockito.when(factory.getBean(ILangManager.class))
                .thenThrow(new NoSuchBeanDefinitionException(ILangManager.class));
        new ApplicationContextProvider().postProcessBeanFactory(factory);
        Assertions.assertNull(ApplicationContextProvider.getBean(ILangManager.class));
    }

    @Test
    void resolveBeanShouldPreferTheCurrentWebApplicationContext() {
        ILangManager fromContext = Mockito.mock(ILangManager.class);
        new ApplicationContextProvider().postProcessBeanFactory(this.mockFactory(Mockito.mock(ILangManager.class)));
        WebApplicationContext ctx = Mockito.mock(WebApplicationContext.class);
        Mockito.when(ctx.getBean(ILangManager.class)).thenReturn(fromContext);
        try (MockedStatic<ContextLoader> contextLoader = Mockito.mockStatic(ContextLoader.class)) {
            contextLoader.when(ContextLoader::getCurrentWebApplicationContext).thenReturn(ctx);
            Assertions.assertSame(fromContext, ApplicationContextProvider.resolveBean(ILangManager.class));
        }
    }

    @Test
    void resolveBeanShouldFallBackWhenNoCurrentWebApplicationContextIsBoundToTheThread() {
        ILangManager fromFactory = Mockito.mock(ILangManager.class);
        new ApplicationContextProvider().postProcessBeanFactory(this.mockFactory(fromFactory));
        try (MockedStatic<ContextLoader> contextLoader = Mockito.mockStatic(ContextLoader.class)) {
            contextLoader.when(ContextLoader::getCurrentWebApplicationContext).thenReturn(null);
            Assertions.assertSame(fromFactory, ApplicationContextProvider.resolveBean(ILangManager.class));
        }
    }

    @Test
    void resolveBeanShouldFallBackWhenTheCurrentWebApplicationContextCannotResolveTheBean() {
        ILangManager fromFactory = Mockito.mock(ILangManager.class);
        new ApplicationContextProvider().postProcessBeanFactory(this.mockFactory(fromFactory));
        WebApplicationContext ctx = Mockito.mock(WebApplicationContext.class);
        Mockito.when(ctx.getBean(ILangManager.class))
                .thenThrow(new NoSuchBeanDefinitionException(ILangManager.class));
        try (MockedStatic<ContextLoader> contextLoader = Mockito.mockStatic(ContextLoader.class)) {
            contextLoader.when(ContextLoader::getCurrentWebApplicationContext).thenReturn(ctx);
            Assertions.assertSame(fromFactory, ApplicationContextProvider.resolveBean(ILangManager.class));
        }
    }

    @Test
    void resolveBeanShouldReturnNullWhenNoSourceIsAvailable() {
        try (MockedStatic<ContextLoader> contextLoader = Mockito.mockStatic(ContextLoader.class)) {
            contextLoader.when(ContextLoader::getCurrentWebApplicationContext).thenReturn(null);
            Assertions.assertNull(ApplicationContextProvider.resolveBean(ILangManager.class));
        }
    }

    @Test
    void resolveIfNullShouldKeepTheReferenceAlreadyWired() {
        ILangManager current = Mockito.mock(ILangManager.class);
        Assertions.assertSame(current, ApplicationContextProvider.resolveIfNull(current, ILangManager.class));
    }

    @Test
    void resolveIfNullShouldResolveAMissingReference() {
        ILangManager fromFactory = Mockito.mock(ILangManager.class);
        new ApplicationContextProvider().postProcessBeanFactory(this.mockFactory(fromFactory));
        try (MockedStatic<ContextLoader> contextLoader = Mockito.mockStatic(ContextLoader.class)) {
            contextLoader.when(ContextLoader::getCurrentWebApplicationContext).thenReturn(null);
            Assertions.assertSame(fromFactory, ApplicationContextProvider.resolveIfNull(null, ILangManager.class));
        }
    }

    @Test
    void beanFactoryIfNullShouldKeepTheFactoryAlreadyWired() {
        ConfigurableListableBeanFactory current = Mockito.mock(ConfigurableListableBeanFactory.class);
        Assertions.assertSame(current, ApplicationContextProvider.beanFactoryIfNull(current));
    }

    @Test
    void beanFactoryIfNullShouldFallBackToTheCapturedFactory() {
        ConfigurableListableBeanFactory factory = Mockito.mock(ConfigurableListableBeanFactory.class);
        new ApplicationContextProvider().postProcessBeanFactory(factory);
        Assertions.assertSame(factory, ApplicationContextProvider.beanFactoryIfNull(null));
        ApplicationContextProvider.clear();
        Assertions.assertNull(ApplicationContextProvider.beanFactoryIfNull(null));
    }

    private ConfigurableListableBeanFactory mockFactory(ILangManager langManager) {
        ConfigurableListableBeanFactory factory = Mockito.mock(ConfigurableListableBeanFactory.class);
        Mockito.when(factory.getBean(ILangManager.class)).thenReturn(langManager);
        return factory;
    }
}
