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
package com.agiletec.aps.system.services.url;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.page.IPage;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * Rappresenta un URL ad una pagina del sistema. Oggetti di questa classe
 * devono avere un ciclo di vita limitato ad una richiesta; non devono
 * essere memorizzati in modo permanente.
 * @author M.Diana
 */
public class PageURL implements Serializable {
	
	/**
	 * Costruttore utilizzato dalla factory di questa classe 
	 * (IURLManager). Non deve essere utilizzato direttamente.
	 * @param urlManager The Url Manager
	 * @param reqCtx The Request Context
	 */
	public PageURL(IURLManager urlManager, RequestContext reqCtx) {
		this.urlManager = urlManager;
		this.reqCtx = reqCtx;
	}
	
	/**
	 * Imposta il codice della lingua richiesta.
	 * @param langCode Il codice della lingua da impostare.
	 */
	public void setLangCode(String langCode) {
		this.langCode = langCode;
	}

	/**
	 * Imposta la lingua richiesta. L'effetto è equivalente
	 * alla chiamata setLangCode(lang.getCode()) .
	 * @param lang Il codice della lingua da impostare.
	 */
	public void setLang(Lang lang) {
		this.langCode = lang.getCode();
	}
	/**
	 * Imposta il codice della pagina richiesta.
	 * @param pageCode Il codice della pagina da impostare.
	 */
	public void setPageCode(String pageCode) {
		this.pageCode = pageCode;
	}

	/**
	 * Imposta la pagina richiesta. L'effetto è equivalente
	 * alla chiamata setPageCode(page.getCode()) .
	 * @param page La pagina da impostare.
	 */
	public void setPage(IPage page) {
		this.pageCode = page.getCode();
	}
	
	/**
	 * Restituisce la lingua precedentemente impostata.
	 * @return Il codice lingua, o null se non è stata impostata.
	 */
	public String getLangCode() {
		return langCode;
	}

	/**
	 * @return Il codice pagina, o null se non è stata impostata.
	 */
	public String getPageCode() {
		return pageCode;
	}

	/**
	 * Aggiunge un parametro.
	 * @param name Il nome del parametro.
	 * @param value Il valore del parametro.
	 */
	public void addParam(String name, String value) {
		if (name != null) {
			if (this.params == null) {
				this.params = new HashMap<>();
			}
			String val = (value == null ? "" : value);
			this.params.put(name, val);
		}
	}
	
	/**
	 * Restituisce la mappa dei parametri, indicizzati in base al nome.
	 * @return La mappa dei parametri.
	 */
	public Map<String, String> getParams() {
		return params;
	}
	
	/**
	 * Restituisce l'URL utilizzabile. La costruzione dell'URL
	 * è delegata all'implementazione della classe AbstractURLManager.
	 * @return L'URL generato.
	 */
	public String getURL() {
		return this.urlManager.getURLString(this, reqCtx);
	}
	
	/**
	 * Repaat the parameters extracted from the request.
	 */
	public void setParamRepeat() {
		this.setParamRepeat(null);
	}
	
	/**
	 * Repaat the parameters extracted from the request.
	 * @param parametersToExclude The parameters to exclude.
	 */
	public void setParamRepeat(List<String> parametersToExclude) {
		if (null == this.reqCtx) {
			return;
		}
		HttpServletRequest req = this.reqCtx.getRequest();
		Map<String, String[]> paramsMap = req.getParameterMap();
		if (null != paramsMap && !paramsMap.isEmpty()) {
			for(String key : paramsMap.keySet()) {
				if (null == parametersToExclude || !parametersToExclude.contains(key)) {
					this.addParam(key, req.getParameter(key));
				}
			}
		}
	}
	
	public boolean isEscapeAmp() {
		return escapeAmp;
	}
	public void setEscapeAmp(boolean escapeAmp) {
		this.escapeAmp = escapeAmp;
	}

	public String getBaseUrlMode() {
		return baseUrlMode;
	}

	public void setBaseUrlMode(String baseUrlMode) {
		this.baseUrlMode = baseUrlMode;
	}

	private transient IURLManager urlManager;
	private transient RequestContext reqCtx;
	private String pageCode;
	private String langCode;
	private String  baseUrlMode = null;
	private Map<String, String> params;
	
	private boolean escapeAmp = true;
	
}
