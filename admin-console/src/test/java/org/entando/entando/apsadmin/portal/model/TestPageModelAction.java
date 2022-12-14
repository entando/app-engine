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
package org.entando.entando.apsadmin.portal.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.pagemodel.PageModel;
import com.agiletec.aps.system.services.pagemodel.PageModelDOM;
import com.agiletec.apsadmin.system.ApsAdminSystemConstants;

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionSupport;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author E.Santoboni
 */
class TestPageModelAction extends AbstractTestPageModelAction {

    @Test
    void testEditPageModels() throws Throwable {
        String testPageModelCode = "test_pagemodel";
        assertNull(this._pageModelManager.getPageModel(testPageModelCode));
        try {
            String result = this.executeAction("admin", "edit", testPageModelCode);
            assertEquals("pageModelList", result);
            PageModel mockModel = this.createMockPageModel(testPageModelCode);
            this._pageModelManager.addPageModel(mockModel);
            result = this.executeAction("admin", "edit", testPageModelCode);
            assertEquals(Action.SUCCESS, result);
        } catch (Exception e) {
            throw e;
        } finally {
            this._pageModelManager.deletePageModel(testPageModelCode);
            assertNull(this._pageModelManager.getPageModel(testPageModelCode));
        }
    }

    @Test
    void testValidate_1() throws Throwable {
        String testPageModelCode = "test_pagemodel";
        assertNull(this._pageModelManager.getPageModel(testPageModelCode));
        try {
            this.setUserOnSession("admin");
            this.initAction("/do/PageModel", "save");
            super.addParameter("code", testPageModelCode);
            super.addParameter("strutsAction", ApsAdminSystemConstants.ADD);
            String result = this.executeAction();
            ActionSupport action = super.getAction();
            assertEquals(Action.INPUT, result);
            assertEquals(3, action.getFieldErrors().size());
            assertNotNull(action.getFieldErrors().get("description"));
            assertNotNull(action.getFieldErrors().get("template"));
            assertNotNull(action.getFieldErrors().get("xmlConfiguration"));
        } catch (Exception e) {
            this._pageModelManager.deletePageModel(testPageModelCode);
            assertNull(this._pageModelManager.getPageModel(testPageModelCode));
            throw e;
        }
    }

    @Test
    void testValidate_2() throws Throwable {
        String testPageModelCode = "internal";
        PageModel model = this._pageModelManager.getPageModel(testPageModelCode);
        assertNotNull(model);
        try {
            this.setUserOnSession("admin");
            this.initAction("/do/PageModel", "save");
            super.addParameter("code", testPageModelCode);
            super.addParameter("description", "Description");
            super.addParameter("strutsAction", ApsAdminSystemConstants.ADD);
            String result = this.executeAction();
            ActionSupport action = super.getAction();
            assertEquals(Action.INPUT, result);
            assertEquals(3, action.getFieldErrors().size());
            assertNotNull(action.getFieldErrors().get("code"));
            assertNotNull(action.getFieldErrors().get("template"));
            assertNotNull(action.getFieldErrors().get("xmlConfiguration"));
        } catch (Exception e) {
            this._pageModelManager.updatePageModel(model);
            throw e;
        }
    }

    @Test
    void testSave() throws Throwable {
        String testPageModelCode = "test_pagemodel";
        assertNull(this._pageModelManager.getPageModel(testPageModelCode));
        try {
            PageModel mockModel = this.createMockPageModel(testPageModelCode);
            this.setUserOnSession("admin");
            this.initAction("/do/PageModel", "save");
            super.addParameter("code", mockModel.getCode());
            super.addParameter("description", mockModel.getDescription());
            super.addParameter("template", mockModel.getTemplate());
            PageModelDOM dom = new PageModelDOM(mockModel);
            super.addParameter("xmlConfiguration", dom.getXMLDocument());
            super.addParameter("strutsAction", ApsAdminSystemConstants.ADD);
            String result = this.executeAction();
            assertEquals(Action.SUCCESS, result);
            assertNotNull(this._pageModelManager.getPageModel(testPageModelCode));
        } catch (Exception e) {
            throw e;
        } finally {
            this._pageModelManager.deletePageModel(testPageModelCode);
            assertNull(this._pageModelManager.getPageModel(testPageModelCode));
        }
    }

    @Test
    void testTrashPageModels_1() throws Throwable {
        String result = this.executeAction("admin", "trash", null);
        assertEquals("pageModelList", result);
        result = this.executeAction("admin", "trash", "invalidCode");
        assertEquals("pageModelList", result);
        result = this.executeAction("admin", "trash", "home");
        assertEquals("references", result);
        PageModelAction pageModelAction = (PageModelAction) this.getAction();
        Map<String, List<Object>> references = pageModelAction.getReferences();
        assertFalse(references.isEmpty());
        assertEquals(1, references.size());
        List<Object> referendedPages = references.get("PageManagerUtilizers");
        assertEquals(23, referendedPages.size());
        this.checkUtilizers(referendedPages, 12, 11);
    }

    @Test
    void testTrashPageModels_2() throws Throwable {
        String testPageModelCode = "test_pagemodel";
        assertNull(this._pageModelManager.getPageModel(testPageModelCode));
        try {
            PageModel mockModel = this.createMockPageModel(testPageModelCode);
            this._pageModelManager.addPageModel(mockModel);
            String result = this.executeAction("admin", "trash", testPageModelCode);
            assertEquals(Action.SUCCESS, result);
            PageModelAction pageModelAction = (PageModelAction) this.getAction();
            Map<String, List<Object>> references = pageModelAction.getReferences();
            assertTrue(null == references || references.isEmpty());
        } catch (Exception e) {
            throw e;
        } finally {
            this._pageModelManager.deletePageModel(testPageModelCode);
            assertNull(this._pageModelManager.getPageModel(testPageModelCode));
        }
    }

    @Test
    void testDeletePageModels_1() throws Throwable {
        String result = this.executeAction("admin", "delete", null);
        assertEquals("pageModelList", result);
        result = this.executeAction("admin", "delete", "invalidCode");
        assertEquals("pageModelList", result);
        result = this.executeAction("admin", "delete", "home");
        assertEquals("references", result);
        PageModelAction pageModelAction = (PageModelAction) this.getAction();
        Map<String, List<Object>> references = pageModelAction.getReferences();
        assertFalse(references.isEmpty());
        assertEquals(1, references.size());
        List<Object> referendedPages = references.get("PageManagerUtilizers");
        assertEquals(23, referendedPages.size());
        this.checkUtilizers(referendedPages, 12, 11);
    }

    private void checkUtilizers(List<Object> pageUtilizers, int expectedDraft, int expectedOnline) {
        int online = 0;
        int draft = 0;
        for (int i = 0; i < pageUtilizers.size(); i++) {
            IPage page = (IPage) pageUtilizers.get(i);
            if (page.isOnlineInstance()) {
                online++;
            } else {
                draft++;
            }
        }
        assertEquals(expectedOnline, online);
        assertEquals(expectedDraft, draft);
    }

    @Test
    void testDeletePageModels_2() throws Throwable {
        String testPageModelCode = "test_pagemodel";
        assertNull(this._pageModelManager.getPageModel(testPageModelCode));
        try {
            PageModel mockModel = this.createMockPageModel(testPageModelCode);
            this._pageModelManager.addPageModel(mockModel);
            String result = this.executeAction("admin", "delete", testPageModelCode);
            assertEquals(Action.SUCCESS, result);
            PageModelAction pageModelAction = (PageModelAction) this.getAction();
            Map<String, List<Object>> references = pageModelAction.getReferences();
            assertTrue(null == references || references.isEmpty());
        } catch (Exception e) {
            this._pageModelManager.deletePageModel(testPageModelCode);
            throw e;
        } finally {
            assertNull(this._pageModelManager.getPageModel(testPageModelCode));
        }
    }

    private String executeAction(String currentUser, String actionName, String modelCode) throws Throwable {
        this.setUserOnSession(currentUser);
        this.initAction("/do/PageModel", actionName);
        super.addParameter("code", modelCode);
        return this.executeAction();
    }

}
