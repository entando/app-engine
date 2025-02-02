/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package com.agiletec.aps.system.services.lang.cache;

import java.util.ArrayList;
import java.util.List;

import java.util.function.Function;
import java.util.stream.Collectors;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.springframework.cache.Cache;

import com.agiletec.aps.system.common.AbstractGenericCacheWrapper;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.lang.LangDOM;
import java.util.HashMap;
import java.util.Map;

/**
 * @author E.Santoboni
 */
public class LangManagerCacheWrapper extends AbstractGenericCacheWrapper<Lang> implements ILangManagerCacheWrapper {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(LangManagerCacheWrapper.class);

    @Override
    public void initCache(String xmlConfig, List<Lang> assignableLanguages) throws EntException {
        try {
            Cache cache = this.getCache();
            LangDOM langDom = new LangDOM(xmlConfig);
            Map<String, Lang> langMap = new HashMap<>();
            List<Lang> systemLangs = langDom.getLangs();
            // Builds a map of assignable languages in order to speed up the search by language code
            Map<String, Lang> assignableLangMap = computeAssignableLangMap(assignableLanguages);
            for (Lang lang : systemLangs) {
                // replace custom description with the official one from ISO 639-1
                replaceDescription(assignableLangMap, lang);
                if (lang.isDefault()) {
                    cache.put(LANG_DEFAULT_CACHE_NAME, lang);
                }
                langMap.put(lang.getCode(), lang);
            }
            this.insertAndCleanCache(cache, langMap);
        } catch (Throwable t) {
            logger.error("Error loading the system langs", t);
            throw new EntException("Error loading the system langs", t);
        }
    }

    @Override
    public List<Lang> getLangs() {
        List<Lang> langs = new ArrayList<>();
        Cache cache = this.getCache();
        List<String> codes = (List<String>) this.get(cache, LANG_CODES_CACHE_NAME, List.class);
        if (null != codes) {
            for (String code : codes) {
                Lang lang = this.get(cache, LANG_CACHE_NAME_PREFIX + code, Lang.class);
                if (lang.isDefault()) {
                    langs.add(0, lang);
                } else {
                    langs.add(lang);
                }
            }
        }
        return langs;
    }

    @Override
    public Lang getDefaultLang() {
        return this.get(this.getCache(), LANG_DEFAULT_CACHE_NAME, Lang.class);
    }

    @Override
    public Lang getLang(String code) {
        return this.get(this.getCache(), LANG_CACHE_NAME_PREFIX + code, Lang.class);
    }

    @Override
    public void addLang(Lang lang) {
        this.manage(lang.getCode(), lang, AbstractGenericCacheWrapper.Action.ADD);
    }

    @Override
    public void updateLang(Lang lang) {
        this.manage(lang.getCode(), lang, AbstractGenericCacheWrapper.Action.UPDATE);
    }

    @Override
    public void removeLang(Lang lang) {
        this.manage(lang.getCode(), lang, AbstractGenericCacheWrapper.Action.DELETE);
    }

    @Override
    protected String getCodesCacheKey() {
        return LANG_CODES_CACHE_NAME;
    }

    @Override
    protected String getCacheKeyPrefix() {
        return LANG_CACHE_NAME_PREFIX;
    }

    @Override
    protected String getCacheName() {
        return LANG_MANAGER_CACHE_NAME;
    }

    private static Map<String, Lang> computeAssignableLangMap(List<Lang> assignableLanguages) {
        return assignableLanguages.stream()
                .collect(Collectors.toMap(Lang::getCode, Function.identity()));
    }

    private static void replaceDescription(Map<String, Lang> assignableLangMap, Lang lang) {
        if (assignableLangMap.containsKey(lang.getCode())) {
            lang.setDescr(assignableLangMap.get(lang.getCode()).getDescr());
        }
    }
}
