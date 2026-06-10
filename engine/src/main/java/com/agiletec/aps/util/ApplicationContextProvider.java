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

import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

/**
 * Thread-independent, startup-safe holder for the root application bean factory.
 *
 * <p>Implemented as a {@link BeanFactoryPostProcessor} so the reference is captured during
 * {@code invokeBeanFactoryPostProcessors}, which always completes before {@code preInstantiateSingletons}.
 * This guarantees the factory is available before any singleton initialization (including the
 * startup post-init self-REST seeding) and before any attribute deserialization can occur.</p>
 *
 * <p>It is used as a <em>fallback</em> for re-wiring transient manager references on attributes that
 * are deserialized on threads where {@code ContextLoader.getCurrentWebApplicationContext()} returns
 * {@code null} (e.g. the Redis/lettuce event loop, or the main thread while still inside
 * {@code refresh()} during startup).</p>
 */
public class ApplicationContextProvider implements BeanFactoryPostProcessor {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(ApplicationContextProvider.class);

    private static volatile ConfigurableListableBeanFactory beanFactory;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        ApplicationContextProvider.beanFactory = beanFactory;
    }

    /**
     * @return the captured bean factory, or {@code null} if not available yet.
     */
    public static ConfigurableListableBeanFactory getBeanFactory() {
        return beanFactory;
    }

    /**
     * Resolves a singleton bean by type from the captured bean factory.
     *
     * @param type the bean type.
     * @return the bean instance, or {@code null} if the factory is not available yet or the bean
     * cannot be resolved (callers must treat {@code null} as "not available").
     */
    public static <T> T getBean(Class<T> type) {
        ConfigurableListableBeanFactory factory = beanFactory;
        if (factory == null) {
            return null;
        }
        try {
            return factory.getBean(type);
        } catch (BeansException e) {
            logger.warn("Unable to resolve bean of type '{}' from ApplicationContextProvider", type.getName(), e);
            return null;
        }
    }

    /**
     * Resolves a singleton bean trying the current {@link WebApplicationContext} first (preserving the
     * original behaviour), then falling back to the captured bean factory when the current context is
     * not available on the calling thread.
     *
     * @param type the bean type.
     * @return the bean instance, or {@code null} if it cannot be resolved from either source.
     */
    public static <T> T resolveBean(Class<T> type) {
        WebApplicationContext ctx = ContextLoader.getCurrentWebApplicationContext();
        if (ctx != null) {
            try {
                return ctx.getBean(type);
            } catch (BeansException e) {
                logger.warn("Unable to resolve bean of type '{}' from current WebApplicationContext", type.getName(), e);
            }
        }
        return getBean(type);
    }

    /**
     * Test hook to reset the captured factory.
     */
    public static void clear() {
        ApplicationContextProvider.beanFactory = null;
    }
}