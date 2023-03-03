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

import static java.lang.Math.log;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.i18n.wrapper.I18nLabelBuilder;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;

/**
 * Wrapper del Servizio I18N utilizzato nel contesto di Velocity per il parsing dei modelli.
 * Viene passato a Velocity già inizializzato con la lingua da utilizzare, perché per i 
 * modelli di contenuto la lingua deve essere "trasparente". 
 * Il servizio base richiede invece la specificazione della lingua ad ogni richiesta.
 * @author S.Didaci
 */
public class I18nManagerWrapper {
    
    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(I18nManagerWrapper.class);

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
    
    private Optional<String> getLabel(String key, String langCode) {
        try {
            return Optional.ofNullable(this.i18nManager.getLabel(key, langCode));
        } catch (EntException ex) {
            logger.error("Error extracting label for key {} anc lang {}", key, langCode, ex);
            return Optional.empty();
        }
    }

    /**
     * Restituisce la label data la chiave.
     * @param key La chiave tramite il quele estrarre la label.
     * @return La label cercata.
     * @throws EntException in caso di errore.
     */
    public String getLabel(String key) throws EntException {
        String label = null;
        if (null != key) {
            label = this.getLabel(key, this.currentLangCode).filter(StringUtils::isNotBlank)
                    .orElseGet(() -> Optional.ofNullable(this.getDefaultLang())
                        .flatMap(df -> this.getLabel(key, df.getCode())).orElse(null));
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
        if (null == this.defaultLang && null != this.reqCtx) {
            ILangManager langManager = (ILangManager) ApsWebApplicationUtils.getBean(SystemConstants.LANGUAGE_MANAGER, this.reqCtx.getRequest());
            this.defaultLang = langManager.getDefaultLang();
        }
        return this.defaultLang;
    }

}
