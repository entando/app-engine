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
package com.agiletec.aps.system.common.notify;

import java.io.Serializable;
import java.util.Date;

import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.util.DateConverter;

/**
 * Servizio notificatore eventi.
 * @author M.Diana - E.Santoboni
 */
public class NotifyManager implements INotifyManager, ApplicationListener,
		BeanFactoryAware, ApplicationEventPublisherAware, Serializable {

	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(NotifyManager.class);
	
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApsEvent) {
			NotifyingEventThread thread = new NotifyingEventThread(this, (ApsEvent) event);
			thread.setName(NOTIFYING_THREAD_NAME + "_" + DateConverter.getFormattedDate(new Date(), "yyyy-MM-dd-HH-mm-ss"));
			thread.start();
			return;
		}
		_logger.debug("Unhandled generic event detected: {}", event.getClass().getName());
	}

	/**
	 * Notifica un evento ai corrispondenti servizi osservatori.
	 * @param event L'evento da notificare.
	 */
	protected void notify(ApsEvent event) {
		if (null == event.getObserverInterface()) {
			_logger.debug("No observer interface defined foe event {}", event.getClass().getName());
			return;
		}
		ListableBeanFactory factory = (ListableBeanFactory) this._beanFactory;
		String[] defNames = factory.getBeanNamesForType(event.getObserverInterface());
		for (int i = 0; i < defNames.length; i++) {
			Object observer = null;
			try {
				observer = this._beanFactory.getBean(defNames[i]);
			} catch (Throwable t) {
				observer = null;
			}
			if (observer != null) {
				((ObserverService) observer).update(event);
				_logger.debug("The event {} was notified to the {} service", event.getClass().getName(), observer.getClass().getName());
			}
		}
		_logger.debug("The {} has been notified", event.getClass().getName());
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this._beanFactory = beanFactory;
	}

	/**
	 * Notifica un'evento a tutti i listener definiti nel sistema.
	 * @param event L'evento da notificare.
	 */
	@Override
	public void publishEvent(ApplicationEvent event) {
		this._eventPublisher.publishEvent(event);
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this._eventPublisher = eventPublisher;
	}

	private ApplicationEventPublisher _eventPublisher;

	private BeanFactory _beanFactory;
	
	public static final String NOTIFYING_THREAD_NAME = SystemConstants.ENTANDO_THREAD_NAME_PREFIX + "NotifyingThreadName";
	
}
