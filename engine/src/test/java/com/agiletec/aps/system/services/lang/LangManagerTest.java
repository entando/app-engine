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
package com.agiletec.aps.system.services.lang;

import com.agiletec.aps.system.common.notify.INotifyManager;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.lang.cache.ILangManagerCacheWrapper;
import com.agiletec.aps.system.services.lang.events.LangsChangedEvent;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author E.Santoboni
 */
@ExtendWith(MockitoExtension.class)
class LangManagerTest {

	@Mock
	private ConfigInterface configManager;

	@Mock
	private INotifyManager notifyManager;

	@Mock
	private ILangManagerCacheWrapper cacheWrapper;

	@InjectMocks
	private LangManager langManager;

	@BeforeAll
	public static void setUp() throws Exception {
		MockitoAnnotations.initMocks(LangManagerTest.class);
	}

	@Test
	public void getDefaultLang() {
		Lang defaultLang = new Lang();
		defaultLang.setCode("de");
		defaultLang.setCode("German");
		Mockito.when(cacheWrapper.getDefaultLang()).thenReturn(defaultLang);
		Assertions.assertNotNull(this.langManager.getDefaultLang());
	}

	@Test
	public void getAssignableLangs() throws Throwable {
		List<Lang> langs = this.langManager.getAssignableLangs();
		Assertions.assertTrue(langs.size() > 100);
	}

	/*
	@Test(expected = NullPointerException.class)
	public void getAssignableLangsWithException() throws Throwable {
		Mockito.when(langManager.getClass().getResourceAsStream(Mockito.any(String.class))).thenReturn(null);
		this.langManager.getAssignableLangs();
	}
	 */
	@Test
	public void addLang() throws Throwable {
		this.langManager.addLang("de");
		Mockito.verify(cacheWrapper, Mockito.times(1)).addLang(Mockito.any(Lang.class));
		Mockito.verify(configManager, Mockito.times(1)).updateConfigItem(Mockito.any(String.class), Mockito.any(String.class));
		Mockito.verify(notifyManager, Mockito.times(1)).publishEvent(Mockito.any(LangsChangedEvent.class));
	}

	@Test
	void updateLang() throws Throwable {
		Lang requiredLang = new Lang();
		requiredLang.setCode("de");
		requiredLang.setCode("German");
		Mockito.when(cacheWrapper.getLang("de")).thenReturn(requiredLang);
		this.langManager.updateLang("de", "German lang");
		requiredLang.setDescr("German lang");
		Mockito.verify(cacheWrapper, Mockito.times(1)).updateLang(requiredLang);
		Mockito.verify(configManager, Mockito.times(1)).updateConfigItem(Mockito.any(String.class), Mockito.any(String.class));
		Mockito.verify(notifyManager, Mockito.times(1)).publishEvent(Mockito.any(LangsChangedEvent.class));
	}

	@Test
	void updateLangNullLang() throws Throwable {
		Mockito.when(cacheWrapper.getLang("et")).thenReturn(null);
		this.langManager.updateLang("et", "Estonian lang");
		Mockito.verify(cacheWrapper, Mockito.times(0)).updateLang(Mockito.any(Lang.class));
		Mockito.verify(configManager, Mockito.times(0)).updateConfigItem(Mockito.any(String.class), Mockito.any(String.class));
		Mockito.verify(notifyManager, Mockito.times(0)).publishEvent(Mockito.any(LangsChangedEvent.class));
	}

	@Test
	void removeLang() throws Throwable {
		Lang requiredLang = new Lang();
		requiredLang.setCode("de");
		requiredLang.setCode("German");
		Mockito.when(cacheWrapper.getLang("de")).thenReturn(requiredLang);
		this.langManager.removeLang("de");
		Mockito.verify(cacheWrapper, Mockito.times(1)).removeLang(requiredLang);
		Mockito.verify(configManager, Mockito.times(1)).updateConfigItem(Mockito.any(String.class), Mockito.any(String.class));
		Mockito.verify(notifyManager, Mockito.times(1)).publishEvent(Mockito.any(LangsChangedEvent.class));
	}

	@Test
	void removeLangNullLang() throws Throwable {
		Mockito.when(cacheWrapper.getLang("et")).thenReturn(null);
		this.langManager.removeLang("et");
		Mockito.verify(cacheWrapper, Mockito.times(0)).removeLang(Mockito.any(Lang.class));
		Mockito.verify(configManager, Mockito.times(0)).updateConfigItem(Mockito.any(String.class), Mockito.any(String.class));
		Mockito.verify(notifyManager, Mockito.times(0)).publishEvent(Mockito.any(LangsChangedEvent.class));
	}

	@Test
	void initTenantAwareShouldLoadAssignableLanguages() throws Exception {
		LangManager spy = Mockito.spy(langManager);
		spy.initTenantAware();
		Mockito.verify(spy, Mockito.times(1)).getAssignableLangs();
	}
}
