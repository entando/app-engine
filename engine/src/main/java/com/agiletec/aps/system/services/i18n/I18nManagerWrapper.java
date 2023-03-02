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
package com.agiletec.aps.system.services.i18n;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.services.i18n.wrapper.I18nLabelBuilder;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.ent.exception.EntException;

/**
 * Wrapper del Servizio I18N utilizzato nel contesto di Velocity per il parsing dei modelli.
 * Viene passato a Velocity già inizializzato con la lingua da utilizzare, perché per i 
 * modelli di contenuto la lingua deve essere "trasparente". 
 * Il servizio base richiede invece la specificazione della lingua ad ogni richiesta.
 * @author S.Didaci
 */
public class I18nManagerWrapper {
    
    private String currentLangCode;
    private Lang defaultLang;
    
    private II18nManager i18nManager;
    private RequestContext reqCtx;

	/**
	 * Inizializzazione del Wrapper.
	 * @param currentLangCode La lingua tramite il quale restituire la label.
	 * @param i18nManager Il manager gestore delle etichette.
	 */
	public I18nManagerWrapper(String currentLangCode, II18nManager i18nManager) {
		this.currentLangCode = currentLangCode;
		this.i18nManager = i18nManager;
	}

	public I18nManagerWrapper(String currentLangCode, II18nManager i18nManager, RequestContext reqCtx) {
		this(currentLangCode, i18nManager);
        this.reqCtx = reqCtx;
	}

	/**
	 * Restituisce la label data la chiave. 
	 * @param key La chiave tramite il quele estrarre la label.
	 * @return La label cercata.
	 * @throws EntException in caso di errori di parsing.
	 */
	public String getLabel(String key) throws EntException {
		String label = null;
		if (null != key) {
			label = this.i18nManager.getLabel(key, this.currentLangCode);
            if (StringUtils.isBlank(label) && null != reqCtx) {
                label = this.i18nManager.getLabel(key, this.getDefaultLang().getCode());
            }
		}
        if (StringUtils.isBlank(label)) {
            return key;
        }
		return label;
	}

	/**
	 * Returns a {@link I18nLabelBuilder} from a given key, that allows to translate a label containing parameters. 
	 * @param key The key of the desired label.
	 * @return A {@link I18nLabelBuilder} that allows you to replace the params of the label.
	 * @throws EntException in case of parsing errors.
	 */
	public I18nLabelBuilder getLabelWithParams(String key) throws EntException {
		String label = this.getLabel(key);
		return new I18nLabelBuilder(label);
	}
    
    private Lang getDefaultLang() {
        if (null == this.defaultLang) {
            ILangManager langManager = ApsWebApplicationUtils.getBean(ILangManager.class, this.reqCtx.getRequest());
            this.defaultLang = langManager.getDefaultLang();
        }
        return this.defaultLang;
    }

}
