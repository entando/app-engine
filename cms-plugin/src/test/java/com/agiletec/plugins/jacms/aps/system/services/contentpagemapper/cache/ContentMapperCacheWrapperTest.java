/*
 * Copyright 2018-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package com.agiletec.plugins.jacms.aps.system.services.contentpagemapper.cache;

import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.page.Page;
import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.aps.system.services.pagemodel.Frame;
import com.agiletec.aps.system.services.pagemodel.IPageModelManager;
import com.agiletec.aps.system.services.pagemodel.PageModel;
import com.agiletec.aps.util.ApsProperties;
import com.agiletec.plugins.jacms.aps.system.services.contentpagemapper.ContentPageMapper;
import java.util.Arrays;
import java.util.List;
import org.entando.entando.aps.system.services.widgettype.WidgetType;
import org.entando.entando.aps.system.services.widgettype.WidgetTypeParameter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * @author E.Santoboni
 */
@ExtendWith(MockitoExtension.class)
class ContentMapperCacheWrapperTest {

	@Mock
	private CacheManager cacheManager;

	@Mock
	private IPageManager pageManager;

	@Mock
	private IPageModelManager pageModelManager;

	@Mock
	private Cache cache;

	@Mock
	private Cache.ValueWrapper valueWrapper;

	@InjectMocks
	private ContentMapperCacheWrapper cacheWrapper;

	@BeforeEach
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void testInitCacheWithError() throws Throwable {
        Assertions.assertThrows(EntException.class, () -> {
            this.cacheWrapper.initCache(this.pageManager, this.pageModelManager);
        });
	}

	@Test
	void testInitCache() throws Throwable {
		Mockito.when(pageManager.getOnlineRoot()).thenReturn(this.createMockPage());
		Mockito.when(cacheManager.getCache(IContentMapperCacheWrapper.CONTENT_MAPPER_CACHE_NAME)).thenReturn(this.cache);
		cacheWrapper.initCache(this.pageManager, this.pageModelManager);
        Assertions.assertNotNull(this.cacheWrapper);
	}

	@Test
	void testGetPageCode() throws Throwable {
		ContentPageMapper contentPageMapper = new ContentPageMapper();
		contentPageMapper.add("ART12", "temp_page");
		contentPageMapper.add("NEW56", "wring_page");
		Mockito.when(valueWrapper.get()).thenReturn(contentPageMapper);
		Mockito.when(cache.get(Mockito.anyString())).thenReturn(valueWrapper);
		Mockito.when(cacheManager.getCache(Mockito.anyString())).thenReturn(this.cache);
		String pageCode = this.cacheWrapper.getPageCode("ART12");
		Assertions.assertNotNull(pageCode);
		Assertions.assertEquals("temp_page", pageCode);
	}

	private IPage createMockPage() {
		Page root = new Page();
		root.setCode("root_code");
		root.setModelCode(this.createMockPageModel().getCode());
		root.setGroup(Group.FREE_GROUP_NAME);
		Widget[] widgets = new Widget[]{this.createMockWidget()};
		root.setWidgets(widgets);
		root.setChildrenCodes(new String[]{});
		return root;
	}

	private PageModel createMockPageModel() {
		PageModel model = new PageModel();
		model.setCode("temp_model");
		Frame frame = new Frame();
		frame.setMainFrame(true);
		frame.setDescription("Main Frame");
		frame.setPos(0);
		Frame[] configuration = new Frame[]{frame};
		model.setConfiguration(configuration);
		model.setMainFrame(0);
		return model;
	}

	private Widget createMockWidget() {
		Widget widget = new Widget();
		WidgetType type = new WidgetType();
		type.setCode("type");
		WidgetTypeParameter param1 = new WidgetTypeParameter();
		param1.setName("contentId");
		WidgetTypeParameter param2 = new WidgetTypeParameter();
		param2.setName("testParam");
		List<WidgetTypeParameter> params = Arrays.asList(new WidgetTypeParameter[]{param1, param2});
		type.setTypeParameters(params);
		widget.setTypeCode(type.getCode());
		ApsProperties props = new ApsProperties();
		props.put("contentId", "ART1");
		props.put("testParam", "test");
		widget.setConfig(props);
		return widget;
	}

}
