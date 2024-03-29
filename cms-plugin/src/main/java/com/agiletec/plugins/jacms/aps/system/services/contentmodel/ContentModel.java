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
package com.agiletec.plugins.jacms.aps.system.services.contentmodel;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;

/**
 * Rappresenta un modello di contenuto. 
 * L'attributo contentShape rappresenta come il contenuto è formattato (il template di velocity).
 * @author M.Diana - E.Santoboni
 */
@XmlRootElement(name = "contentModel")
@XmlType(propOrder = {"id", "contentType", "description", "contentShape", "stylesheet"})
public class ContentModel implements Comparable, Serializable {

	private static final EntLogger logger = EntLogFactory.getSanitizedLogger(ContentModel.class);

	public static final String MODEL_ID_DEFAULT = "default";
	public static final String MODEL_ID_LIST = "list";

	/**
	 * Restituisce l'identificativo del modello.
	 * @return L'identificativo del modello.
	 */
	@XmlElement(name = "id", required = true)
	public long getId() {
		return _id;
	}
	
	/**
	 * Setta l'identificativo del modello.
	 * @param id L'identificativo del modello.
	 */
	public void setId(long id) {
		this._id = id;
	}
	
	/**
	 * Restituisce il tipo di contenuto a cui si applica il modello.
	 * @return Il tipo di contenuto a cui si applica il modello.
	 */
	@XmlElement(name = "contentType", required = true)
	public String getContentType() {
		return _contentType;
	}
	
	/**
	 * Setta il tipo di contenuto.
	 * @param contentType Il tipo di contenuto da settare
	 */
	public void setContentType(String contentType) {
	    this._contentType = contentType;
	}
	
	/**
	 * Restituisce la descrizione del modello.
	 * @return La descrizione del modello.
	 */
	@XmlElement(name = "description", required = true)
	public String getDescription() {
		return _description;
	}
	
	/**
	 * Setta la descrizione del modello.
	 * @param descr La descrizione del modello.
	 */
	public void setDescription(String descr) {
		this._description = descr;
	}
	
	/**
	 * @return Returns the contentShape.
	 */
	@XmlElement(name = "shape", required = true)
	public String getContentShape() {
		return _contentShape;
	}
	
	/**
	 * @param shape The contentShape to set.
	 */
	public void setContentShape(String shape) {
		this._contentShape = shape;
	}
	
	/**
	 * Restituisce il nome del foglio di stile particolare per questo modello.
	 * @return Il nome del foglio di stile. Può essere null.
	 */
	@XmlElement(name = "stylesheet", required = false)
	public String getStylesheet() {
		return _stylesheet;
	}
	
	/**
	 * Imposta il nome del foglio di stile particolare per questo modello.
	 * @param stylesheet Il nome del foglio di stile
	 */
	public void setStylesheet(String stylesheet) {
		this._stylesheet = stylesheet;
	}
	
	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Object model) {
		int result = this.getContentType().compareTo(((ContentModel) model).getContentType());
		if (result == 0) {
			if (this.getId()>(((ContentModel) model).getId())) {
				return 1;
			} else return -1;
		}
		return result;
	}
	
	private long _id;
	private String _contentType;
	private String _description;
	private String _contentShape;
	private String _stylesheet;
	

	/**
	 * tells if a model_id is valid
	 */
	public static boolean isValidModelId(String modelId) {
		try {
			return modelId != null && Long.parseLong(modelId) > 0;
		} catch (NumberFormatException ignored) {
			// IGNORE
		}
		logger.warn("Detected invalid model_id: \"{}\"", modelId);
		return false;
	}

	/**
	 * tells if a model_id parameter is valid
	 * <p>
	 * for parameter is meant a value coming from the user/representation level
	 * that so may be an actual model_id or a special value
	 */
	public static boolean isValidModelIdParam(String modelId) {
		return modelId == null ||
				modelId.equals(MODEL_ID_DEFAULT) ||
				modelId.equals(MODEL_ID_LIST) ||
				isValidModelId(modelId);
	}
}
