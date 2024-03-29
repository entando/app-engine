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
package com.agiletec.plugins.jacms.apsadmin.portal.specialwidget.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.aps.util.ApsProperties;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author E.Santoboni
 */
class TestContentViewerWidgetAction extends ApsAdminBaseTestCase {

	@Test
    void testInitConfigViewerwithNoWidgetCode() throws Throwable {
		String result = this.executeConfigViewer("admin", "homepage", "1", null);
		assertEquals("pageTree", result);
		assertEquals(1, this.getAction().getActionErrors().size());
	}

	@Test
    void testInitConfigViewer_1() throws Throwable {
		String result = this.executeConfigViewer("admin", "homepage", "1", "content_viewer");
		assertEquals(Action.SUCCESS, result);
		ContentViewerWidgetAction action = (ContentViewerWidgetAction) this.getAction();
		Widget widget = action.getWidget();
		assertNotNull(widget);
		assertEquals(0, widget.getConfig().size());
	}

	@Test
    void testInitConfigViewer_2() throws Throwable {
		String result = this.executeConfigViewer("admin", "homepage", "3", null);
		assertEquals(Action.SUCCESS, result);
		ContentViewerWidgetAction action = (ContentViewerWidgetAction) this.getAction();
		Widget widget = action.getWidget();
		assertNotNull(widget);
		ApsProperties props = widget.getConfig();
		assertEquals(2, props.size());
		assertEquals("ART1", props.getProperty("contentId"));
		assertEquals("2", props.getProperty("modelId"));
	}

	private String executeConfigViewer(String userName,
			String pageCode, String frame, String widgetTypeCode) throws Throwable {
		this.setUserOnSession(userName);
		this.initAction("/do/Page/SpecialWidget", "viewerConfig");
		this.addParameter("pageCode", pageCode);
		this.addParameter("frame", frame);
		if (null != widgetTypeCode && widgetTypeCode.trim().length() > 0) {
			this.addParameter("widgetTypeCode", widgetTypeCode);
		}
		return this.executeAction();
	}

	@Test
    void testFailureJoinContent_1() throws Throwable {
		String result = this.executeJoinContent("admin", "pagina_11", "1", null);//ID Nullo
		assertEquals(Action.INPUT, result);

		ActionSupport action = this.getAction();
		Map<String, List<String>> fieldErrors = action.getFieldErrors();
		assertEquals(1, fieldErrors.size());
		List<String> contentIdFieldErrors = fieldErrors.get("contentId");
		assertEquals(1, contentIdFieldErrors.size());
	}

	@Test
    void testFailureJoinContent_2() throws Throwable {
		String result = this.executeJoinContent("admin", "pagina_11", "1", "ART179");//ID di contenuto non pubblico
		assertEquals(Action.INPUT, result);

		ActionSupport action = this.getAction();
		Map<String, List<String>> fieldErrors = action.getFieldErrors();
		assertEquals(1, fieldErrors.size());
		List<String> contentIdFieldErrors = (List<String>) fieldErrors.get("contentId");
		assertEquals(1, contentIdFieldErrors.size());
	}

	@Test
    void testFailureJoinContent_3() throws Throwable {
		String result = this.executeJoinContent("admin", "pagina_11", "1", "ART122");//ID di contenuto non autorizzato
		assertEquals(Action.INPUT, result);

		ActionSupport action = this.getAction();
		Map<String, List<String>> fieldErrors = action.getFieldErrors();
		assertEquals(1, fieldErrors.size());
		List<String> contentIdFieldErrors = (List<String>) fieldErrors.get("contentId");
		assertEquals(1, contentIdFieldErrors.size());
	}

	@Test
    void testJoinContent_1() throws Throwable {
		String result = this.executeJoinContent("admin", "pagina_11", "1", "EVN24");//Contenuto Free
		assertEquals(Action.SUCCESS, result);
		ContentViewerWidgetAction action = (ContentViewerWidgetAction) this.getAction();
		Widget newWidget = action.getWidget();
		assertNotNull(newWidget);
		assertEquals("EVN24", newWidget.getConfig().getProperty("contentId"));
		assertNull(newWidget.getConfig().getProperty("modelId"));

		result = this.executeJoinContent("admin", "pagina_11", "1", "ART121");//Contenuto del gruppo "administrators" ma autorizzato ai free
		assertEquals(Action.SUCCESS, result);
		action = (ContentViewerWidgetAction) this.getAction();
		newWidget = action.getWidget();
		assertNotNull(newWidget);
		assertEquals("ART121", newWidget.getConfig().getProperty("contentId"));
		assertNull(newWidget.getConfig().getProperty("modelId"));
	}

	@Test
    void testJoinContent_2() throws Throwable {
		String result = this.executeJoinContent("admin", "customers_page", "1", "EVN191");//Contenuto Free su pagina non free
		assertEquals(Action.SUCCESS, result);
		ContentViewerWidgetAction action = (ContentViewerWidgetAction) this.getAction();
		Widget newWidget = action.getWidget();
		assertNotNull(newWidget);
		assertEquals("EVN191", newWidget.getConfig().getProperty("contentId"));
		assertNull(newWidget.getConfig().getProperty("modelId"));

		result = this.executeJoinContent("admin", "customers_page", "1", "EVN25");//Contenuto del gruppo "non free" su pagina di gruppo diverso ma autorizzato ai free
		assertEquals(Action.SUCCESS, result);
		action = (ContentViewerWidgetAction) this.getAction();
		newWidget = action.getWidget();
		assertNotNull(newWidget);
		assertEquals("EVN25", newWidget.getConfig().getProperty("contentId"));
		assertNull(newWidget.getConfig().getProperty("modelId"));
	}

	private String executeJoinContent(String currentUserName, String pageCode, String frame, String contentId) throws Throwable {
		this.setUserOnSession(currentUserName);
		this.initAction("/do/jacms/Page/SpecialWidget/Viewer", "executeJoinContent");
		this.addParameter("pageCode", pageCode);
		this.addParameter("frame", frame);
		this.addParameter("widgetTypeCode", "content_viewer");
		if (null != contentId) {
			this.addParameter("contentId", contentId);
		}
		return this.executeAction();
	}

	@Test
    void testSave_1() throws Throwable {
		String pageCode = "pagina_2";
		int frame = 3;
		IPage page = this._pageManager.getDraftPage(pageCode);
		IPage onlinePage = this._pageManager.getOnlinePage(pageCode);
		Widget widget = page.getWidgets()[frame];
		assertNull(widget);
		assertNull(onlinePage.getWidgets()[frame]);
		try {
			this.setUserOnSession("admin");
			this.initAction("/do/jacms/Page/SpecialWidget/Viewer", "saveViewerConfig");
			this.addParameter("pageCode", pageCode);
			this.addParameter("frame", String.valueOf(frame));
			this.addParameter("widgetTypeCode", "content_viewer");
			this.addParameter("contentId", "ART187");
			this.addParameter("modelId", "1");
			String result = this.executeAction();
			assertEquals("configure", result);
			page = this._pageManager.getDraftPage(pageCode);
			onlinePage = this._pageManager.getOnlinePage(pageCode);
			widget = page.getWidgets()[frame];
			assertNotNull(widget);
			assertNull(onlinePage.getWidgets()[frame]);
			assertEquals("content_viewer", widget.getTypeCode());
			assertEquals(2, widget.getConfig().size());
			assertEquals("ART187", widget.getConfig().getProperty("contentId"));
			assertEquals("1", widget.getConfig().getProperty("modelId"));
		} catch (Throwable t) {
			throw t;
		} finally {
			page = this._pageManager.getDraftPage(pageCode);
			page.getWidgets()[frame] = null;
			this._pageManager.updatePage(page);
		}
	}

	@Test
    void testSave_2() throws Throwable {
		this.testSave_2("ART102", "customer_subpage_1", 0, Action.INPUT);
		this.testSave_2("ART104", "customer_subpage_1", 0, Action.INPUT);
		this.testSave_2("ART111", "customer_subpage_1", 0, "configure");
		this.testSave_2("ART122", "customer_subpage_1", 0, Action.INPUT);
		this.testSave_2("ART121", "customer_subpage_1", 0, "configure");
	}

	private void testSave_2(String contentId, String pageCode, int frame, String expectedResult) throws Throwable {
		try {
			this.intSaveViewerConfig(contentId, pageCode, frame);
			String result = this.executeAction();
			assertEquals(expectedResult, result);
			if (expectedResult.equals(Action.INPUT)) {
				ActionSupport action = this.getAction();
				assertEquals(1, action.getFieldErrors().size());
				assertEquals(1, action.getFieldErrors().get("contentId").size());
			}
		} catch (Throwable t) {
			throw t;
		} finally {
			IPage page = this._pageManager.getDraftPage(pageCode);
			page.getWidgets()[frame] = null;
			this._pageManager.updatePage(page);
		}
	}

	private void intSaveViewerConfig(String contentId, String pageCode, int frame) throws Throwable {
		IPage page = this._pageManager.getDraftPage(pageCode);
		Widget widget = page.getWidgets()[frame];
		assertNull(widget);
		this.setUserOnSession("admin");
		this.initAction("/do/jacms/Page/SpecialWidget/Viewer", "saveViewerConfig");
		Map<String, String> params = new HashMap<String, String>();
		params.put("pageCode", pageCode);
		params.put("frame", String.valueOf(frame));
		params.put("widgetTypeCode", "content_viewer");
		params.put("contentId", contentId);
		this.addParameters(params);
	}

    @BeforeEach
	private void init() throws Exception {
		try {
			this._pageManager = (IPageManager) this.getService(SystemConstants.PAGE_MANAGER);
		} catch (Throwable t) {
			throw new Exception(t);
		}
	}

	private IPageManager _pageManager = null;

}
