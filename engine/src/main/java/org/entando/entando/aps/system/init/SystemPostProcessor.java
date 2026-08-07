/*
 * Copyright 2015-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.system.init;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * @author E.Santoboni
 * @deprecated the post-init processes are no longer executed during the Spring refresh phase:
 * doing so ran the self-REST seeding against a half-initialized context (causing failures with
 * background threads, e.g. Redis deserialization, on first boot). They are now triggered by
 * {@link org.entando.entando.aps.servlet.StartupListener} after the WebApplicationContext is
 * fully refreshed and registered. This class is kept as a no-op to avoid breaking external
 * Spring configurations that may still declare it.
 */
@Deprecated(since = "7.5.1")
public class SystemPostProcessor implements BeanPostProcessor {

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		//Nothing to do
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		//Nothing to do: post-init processes are executed by StartupListener after context refresh
		return bean;
	}

}
