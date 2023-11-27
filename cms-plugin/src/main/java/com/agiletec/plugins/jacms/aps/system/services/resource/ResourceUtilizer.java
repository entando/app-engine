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
package com.agiletec.plugins.jacms.aps.system.services.resource;

import java.util.List;

import org.entando.entando.ent.exception.EntException;

/**
 * Interfaccia base per i servizi, i cui elementi gestiti, 
 * possono presentare delle referenziazione con delle risorse.
 * @author E.Santoboni
 * @param <T>
 */
public interface ResourceUtilizer<T> {
	
	/**
	 * Restituisce l'identificativo del servizio utilizzatore.
	 * @return L'identificativo del servizio utilizzatore.
	 */
	public String getName();
	
	/**
	 * Restituisce la lista degli oggetti referenzianti la risorsa 
	 * identificata dal codice immesso.
	 * @param resourceId L'identificativo della risorsa.
	 * @return La lista degli oggetti referenzianti la risorsa.
	 * @throws EntException in caso di errore.
	 */
	public List<T> getResourceUtilizers(String resourceId) throws EntException;
	
}
