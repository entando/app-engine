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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.entando.entando.aps.system.services.tenants.RefreshableBeanTenantAware;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.aps.system.common.AbstractService;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.i18n.cache.II18nManagerCacheWrapper;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.util.ApsProperties;

/**
 * Servizio che fornisce stringhe "localizzate". Le stringhe sono specificate da
 * una chiave di identificazione e dalla lingua di riferimento.
 */
public class I18nManager extends AbstractService implements II18nManager, RefreshableBeanTenantAware {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(I18nManager.class);

    private II18nManagerCacheWrapper cacheWrapper;

    private ILangManager langManager;

    private II18nDAO i18nDAO;

    protected II18nDAO getI18nDAO() {
        return i18nDAO;
    }

    public void setI18nDAO(II18nDAO i18nDao) {
        i18nDAO = i18nDao;
    }

    protected ILangManager getLangManager() {
        return langManager;
    }

    public void setLangManager(ILangManager langManager) {
        this.langManager = langManager;
    }

    protected II18nManagerCacheWrapper getCacheWrapper() {
        return cacheWrapper;
    }

    public void setCacheWrapper(II18nManagerCacheWrapper cacheWrapper) {
        this.cacheWrapper = cacheWrapper;
    }

    protected String getDefaultLang() {
        return this.getLangManager().getDefaultLang().getCode();
    }

    @Override
    public void init() throws Exception {
        initTenantAware();
        logger.debug("{} : initialized {} labels", this.getClass().getName(), this.getLabelGroups().size());
    }
    
    @Override
    protected void release() {
        releaseTenantAware();
        super.release();
    }

    @Override
    public void initTenantAware() throws Exception {
        this.getCacheWrapper().initCache(this.getI18nDAO());
    }

    @Override
    public void releaseTenantAware() {
        this.getCacheWrapper().release();
    }

    /**
     * Return the group of labels.
     *
     * @return The group of labels.
     */
    @Override
    public Map<String, ApsProperties> getLabelGroups() {
        return this.getCacheWrapper().getLabelGroups();
    }

    @Override
    public String renderLabel(String key, String renderingLang,
            boolean keyIfEmpty) throws EntException {
        String label = null;
        ApsProperties labelsProp = this.getLabelGroup(key);
        if (labelsProp != null) {
            label = labelsProp.getProperty(renderingLang);
            if (StringUtils.isEmpty(label)) {
                label = labelsProp.getProperty(this.getDefaultLang());
            }
        }
        if (keyIfEmpty && StringUtils.isEmpty(label)) {
            label = key;
        }
        return label;
    }

    @Override
    public String renderLabel(String key, String renderingLang,
            boolean keyIfEmpty, Map<String, String> params) throws EntException {
        String value = this.renderLabel(key, renderingLang, keyIfEmpty);
        if (params != null && !params.isEmpty() && value != null) {
            value = this.parseText(value, params);
        }
        return value;
    }

    protected String parseText(String defaultText, Map<String, String> params) {
        StringSubstitutor strSub = new StringSubstitutor(params);
        return strSub.replace(defaultText);
    }

    /**
     * Restituisce una label in base alla chiave ed alla lingua specificata.
     *
     * @param key La chiave
     * @param langCode Il codice della lingua.
     * @return La label richiesta.
     */
    @Override
    public String getLabel(String key, String langCode) {
        String label = null;
        ApsProperties labelsProp = this.getCacheWrapper().getLabelGroup(key);
        if (labelsProp != null) {
            label = labelsProp.getProperty(langCode);
        }
        return label;
    }

    @Override
    public ApsProperties getLabelGroup(String key) throws EntException {
        ApsProperties labelsProp = this.getCacheWrapper().getLabelGroup(key);
        if (null == labelsProp) {
            return null;
        }
        return labelsProp.clone();
    }

    /**
     * Add a group of labels on db.
     *
     * @param key The key of the labels.
     * @param labels The labels to add.
     * @throws EntException In case of Exception.
     */
    @Override
    public void addLabelGroup(String key, ApsProperties labels) throws EntException {
        try {
            this.getI18nDAO().addLabelGroup(key, labels);
            this.getCacheWrapper().addLabelGroup(key, labels);
        } catch (Throwable t) {
            logger.error("Error while adding a group of labels by key '{}'", key, t);
            throw new EntException("Error while adding a group of labels", t);
        }
    }

    /**
     * Delete a group of labels from db.
     *
     * @param key The key of the labels to delete.
     * @throws EntException In case of Exception.
     */
    @Override
    public void deleteLabelGroup(String key) throws EntException {
        try {
            this.getI18nDAO().deleteLabelGroup(key);
            this.getCacheWrapper().removeLabelGroup(key);
        } catch (Throwable t) {
            logger.error("Error while deleting a label by key {}", key, t);
            throw new EntException("Error while deleting a label", t);
        }
    }

    /**
     * Update a group of labels on db.
     *
     * @param key The key of the labels.
     * @param labels The key of the labels to update.
     * @throws EntException In case of Exception.
     */
    @Override
    public void updateLabelGroup(String key, ApsProperties labels) throws EntException {
        try {
            this.getI18nDAO().updateLabelGroup(key, labels);
            this.getCacheWrapper().updateLabelGroup(key, labels);
        } catch (Throwable t) {
            logger.error("Error while updating label with key {}", key, t);
            throw new EntException("Error while updating a label", t);
        }
    }

    /**
     * Restituisce la lista di chiavi di gruppi di labels in base ai parametri
     * segnalati.
     *
     * @param insertedText Il testo tramite il quale effettuare la ricerca.
     * @param doSearchByKey Specifica se effettuare la ricerca sulle chiavi, in
     * base al testo inserito.
     * @param doSearchByLang Specifica se effettuare la ricerca sul testo di una
     * lingua, in base al testo inserito.
     * @param langCode Specifica la lingua della label sulla quale effettuare la
     * ricerca, in base al testo inserito.
     * @return La lista di chiavi di gruppi di labels in base ai parametri
     * segbalati.
     */
    @Override
    public List<String> searchLabelsKey(String insertedText, boolean doSearchByKey,
            boolean doSearchByLang, String langCode) {
        List<String> keys = new ArrayList<>();
        Pattern pattern = Pattern.compile(insertedText, Pattern.CASE_INSENSITIVE + Pattern.LITERAL);
        Matcher matcher = pattern.matcher("");
        List<String> allKeys = new ArrayList<>(this.getLabelGroups().keySet());
        for (String key : allKeys) {
            ApsProperties properies = this.getLabelGroups().get(key);
            if (!doSearchByKey && !doSearchByLang) {
                matcher = matcher.reset(key);
                if (matcher.find()) {
                    keys.add(key);
                } else {
                    Enumeration<Object> langKeys = properies.keys();
                    while (langKeys.hasMoreElements()) {
                        String lang = (String) langKeys.nextElement();
                        String target = properies.getProperty(lang);
                        if (this.labelMatch(target, matcher)) {
                            keys.add(key);
                            break;
                        }
                    }
                }
            } else if (doSearchByKey && !doSearchByLang) {
                matcher = matcher.reset(key);
                if (matcher.find()) {
                    keys.add(key);
                }
            } else if (!doSearchByKey && doSearchByLang) {
                String target = properies.getProperty(langCode);
                if (this.labelMatch(target, matcher)) {
                    keys.add(key);
                }
            }
        }
        return keys;
    }

    private boolean labelMatch(String target, Matcher matcher) {
        boolean match = false;
        if (null != target) {
            matcher = matcher.reset(target);
            if (matcher.find()) {
                match = true;
            }
        }
        return match;
    }

}
