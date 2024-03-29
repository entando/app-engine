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
package com.agiletec.apsadmin.portal.specialwidget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.aps.util.ApsProperties;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import com.opensymphony.xwork2.Action;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version 1.0
 * @author E.Santoboni
 */
class TestSimpleWidgetConfigAction extends ApsAdminBaseTestCase {

	@Test
	void testInitConfigSimpleParameter_1() throws Throwable {
		String result = this.executeConfigSimpleParameter("admin", "homepage", "1", "formAction");
		assertEquals(Action.SUCCESS, result);
		SimpleWidgetConfigAction action = (SimpleWidgetConfigAction) this.getAction();
		Widget widget = action.getWidget();
		assertNotNull(widget);
		assertEquals(0, widget.getConfig().size());
	}

	@Test
	void testInitConfigSimpleParameter_withNoShowletCode() throws Throwable {
		String result = this.executeConfigSimpleParameter("admin", "homepage", "1", null);
		assertEquals("pageTree", result);
		assertEquals(1, this.getAction().getActionErrors().size());
	}

	@Test
	void testInitConfigSimpleParameter_2() throws Throwable {
		String result = this.executeConfigSimpleParameter("admin", "pagina_2", "2", null);
		assertEquals(Action.SUCCESS, result);
		SimpleWidgetConfigAction action = (SimpleWidgetConfigAction) this.getAction();
		Widget widget = action.getWidget();
		assertNotNull(widget);
		ApsProperties props = widget.getConfig();
		assertEquals(1, props.size());
		String value = props.getProperty("actionPath");
		assertEquals("/do/login", value);
	}

	private String executeConfigSimpleParameter(String userName,
			String pageCode, String frame, String widgetTypeCode) throws Throwable {
		this.setUserOnSession(userName);
		this.initAction("/do/Page/SpecialWidget", "configSimpleParameter");
		this.addParameter("pageCode", pageCode);
		this.addParameter("frame", frame);
		if (null != widgetTypeCode && widgetTypeCode.trim().length() > 0) {
			this.addParameter("widgetTypeCode", widgetTypeCode);
		}
		return this.executeAction();
	}

	@Test
	void testSave() throws Throwable {
		String pageCode = "pagina_2";
		int frame = 3;
		IPage page = this._pageManager.getDraftPage(pageCode);
		Widget widget = page.getWidgets()[frame];
		assertNull(widget);
		try {
			this.setUserOnSession("admin");
			this.initAction("/do/Page/SpecialWidget", "saveConfigSimpleParameter");
			this.addParameter("pageCode", pageCode);
			this.addParameter("frame", String.valueOf(frame));
			this.addParameter("widgetTypeCode", "formAction");
			this.addParameter("actionPath", "/WEB-INF/pippo.jsp");
			String result = this.executeAction();
			assertEquals("configure", result);
			page = this._pageManager.getDraftPage(pageCode);
			widget = page.getWidgets()[frame];
			assertNotNull(widget);
			assertEquals("formAction", widget.getTypeCode());
			assertEquals(1, widget.getConfig().size());
			assertEquals("/WEB-INF/pippo.jsp", widget.getConfig().getProperty("actionPath"));
		} catch (Throwable t) {
			throw t;
		} finally {
			page = this._pageManager.getDraftPage(pageCode);
			page.getWidgets()[frame] = null;
			this._pageManager.updatePage(page);
		}
	}

    @BeforeEach
	private void init() throws Exception {
		try {
			_pageManager = (IPageManager) this.getService(SystemConstants.PAGE_MANAGER);
		} catch (Throwable t) {
			throw new Exception(t);
		}
	}

	private IPageManager _pageManager = null;

}
