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
package com.agiletec.plugins.jacms.apsadmin.content.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.page.Page;
import com.agiletec.aps.system.services.page.PageMetadata;
import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.aps.system.services.pagemodel.IPageModelManager;
import com.agiletec.aps.system.services.pagemodel.PageModel;
import com.agiletec.aps.util.ApsProperties;
import java.util.List;
import java.util.Map;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import com.agiletec.apsadmin.system.ApsAdminSystemConstants;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.contentmodel.ContentModel;
import com.agiletec.plugins.jacms.aps.system.services.contentmodel.model.ContentModelReference;
import com.agiletec.plugins.jacms.aps.system.services.contentmodel.IContentModelManager;
import com.opensymphony.xwork2.Action;
import org.entando.entando.aps.system.services.widgettype.IWidgetTypeManager;
import org.entando.entando.aps.system.services.widgettype.WidgetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author E.Santoboni
 */
class TestContentModelAction extends ApsAdminBaseTestCase {

    @Test
    void testNewModel() throws Throwable {
        this.setUserOnSession("admin");
        this.initAction("/do/jacms/ContentModel", "new");
        String result = this.executeAction();
        assertEquals(Action.SUCCESS, result);
    }

    @Test
    void testEdit() throws Throwable {
        long modelId = 1;
        this.setUserOnSession("admin");
        this.initAction("/do/jacms/ContentModel", "edit");
        ContentModelAction action = (ContentModelAction) this.getAction();
        this.addParameter("modelId", Long.valueOf(modelId).toString());
        String result = this.executeAction();
        assertEquals(Action.SUCCESS, result);
        assertEquals(ApsAdminSystemConstants.EDIT, action.getStrutsAction());
        ContentModel currentModel = _contentModelManager.getContentModel(modelId);
        assertEquals(currentModel.getId(), new Long(action.getModelId()).longValue());
        assertEquals(currentModel.getContentShape(), action.getContentShape());
        assertEquals(currentModel.getContentType(), action.getContentType());
        assertEquals(currentModel.getDescription(), action.getDescription());
        assertEquals(currentModel.getStylesheet(), action.getStylesheet());
    }

    @Test
    void testSaveWithErrors_1() throws Throwable {
        this.setUserOnSession("admin");
        this.initAction("/do/jacms/ContentModel", "save");
        addParameter("strutsAction", new Integer(ApsAdminSystemConstants.ADD).toString());
        String result = this.executeAction();
        assertEquals(Action.INPUT, result);
        Map<String, List<String>> fieldErrors = this.getAction().getFieldErrors();
        assertNotNull(fieldErrors);
        assertEquals(4, fieldErrors.size());
    }

    @Test
    void testSaveWithErrors_2() throws Throwable {
        this.setUserOnSession("admin");
        this.initAction("/do/jacms/ContentModel", "save");
        addParameter("contentType", "EVN");
        addParameter("strutsAction", new Integer(ApsAdminSystemConstants.ADD).toString());
        addParameter("description", "contentModel description");
        addParameter("contentShape", "contentShape field value");
        addParameter("modelId", "2");
        String result = this.executeAction();
        assertEquals(Action.INPUT, result);
        Map<String, List<String>> fieldErrors = this.getAction().getFieldErrors();
        assertEquals(1, fieldErrors.size());
        assertEquals(1, fieldErrors.get("modelId").size());//duplicate modelId

        this.initAction("/do/jacms/ContentModel", "save");
        addParameter("contentType", "EVN");
        addParameter("strutsAction", new Integer(ApsAdminSystemConstants.ADD).toString());
        addParameter("description", "contentModel description");
        addParameter("contentShape", "contentShape field value");
        addParameter("modelId", "khtds");
        result = this.executeAction();
        assertEquals(Action.INPUT, result);
        fieldErrors = this.getAction().getFieldErrors();
        assertEquals(1, fieldErrors.size());
        assertEquals(2, fieldErrors.get("modelId").size());//wrong format
    }

    @Test
    void testSaveWithErrors_3() throws Throwable {
        String veryLongDescription = "Very but very very very long description (upper than 50 characters) for invoke description's length validation";
        int negativeModelId = 0;
        try {
            this.setUserOnSession("admin");
            this.initAction("/do/jacms/ContentModel", "save");
            addParameter("contentType", "EVN");
            addParameter("strutsAction", new Integer(ApsAdminSystemConstants.ADD).toString());
            addParameter("description", veryLongDescription);
            addParameter("contentShape", "contentShape field value");
            addParameter("modelId", String.valueOf(negativeModelId));
            String result = this.executeAction();
            assertEquals(Action.INPUT, result);
            Map<String, List<String>> fieldErrors = this.getAction().getFieldErrors();
            assertEquals(2, fieldErrors.size());
            assertEquals(1, fieldErrors.get("modelId").size());
            assertEquals(1, fieldErrors.get("description").size());
        } catch (Throwable t) {
            throw t;
        } finally {
            ContentModel model = this._contentModelManager.getContentModel(negativeModelId);
            if (null != model) {
                this._contentModelManager.removeContentModel(model);
            }
        }
    }

    @Test
    void testAddNewModel() throws Throwable {
        List<ContentModel> eventModels = this._contentModelManager.getModelsForContentType("EVN");
        assertEquals(0, eventModels.size());
        long modelIdToAdd = 99;
        try {
            this.setUserOnSession("admin");
            this.initAction("/do/jacms/ContentModel", "save");
            addParameter("contentType", "EVN");
            addParameter("strutsAction", new Integer(ApsAdminSystemConstants.ADD).toString());
            addParameter("description", "contentModel description");
            addParameter("contentShape", "contentShape field value\r\n");
            addParameter("modelId", String.valueOf(modelIdToAdd));
            String result = this.executeAction();
            assertEquals(Action.SUCCESS, result);

            eventModels = this._contentModelManager.getModelsForContentType("EVN");
            assertEquals(1, eventModels.size());

            ContentModel model = eventModels.get(0);
            assertEquals("contentShape field value\r\n", model.getContentShape());
        } catch (Throwable t) {
            throw t;
        } finally {
            ContentModel model = this._contentModelManager.getContentModel(modelIdToAdd);
            if (null != model) {
                this._contentModelManager.removeContentModel(model);
            }
            eventModels = this._contentModelManager.getModelsForContentType("EVN");
            assertEquals(0, eventModels.size());
        }
    }

    @Test
    void testUpdateModel() throws Throwable {
        List<ContentModel> eventModels = this._contentModelManager.getModelsForContentType("EVN");
        assertEquals(0, eventModels.size());
        long modelId = 99;
        this.addModelForTest(modelId, "EVN");
        eventModels = this._contentModelManager.getModelsForContentType("EVN");
        assertEquals(1, eventModels.size());
        ContentModel model = (ContentModel) eventModels.get(0);
        try {
            this.setUserOnSession("admin");

            this.initAction("/do/jacms/ContentModel", "save");
            this.addParameter("strutsAction", new Integer(ApsAdminSystemConstants.EDIT).toString());
            this.addParameter("modelId", new Long(modelId).toString());
            this.addParameter("description", "updated description");
            this.addParameter("contentType", model.getContentType());
            this.addParameter("contentShape", model.getContentShape());
            String result = this.executeAction();
            assertEquals(Action.SUCCESS, result);

            eventModels = this._contentModelManager.getModelsForContentType("EVN");
            assertEquals(1, eventModels.size());

            model = this._contentModelManager.getContentModel(modelId);
            assertEquals("updated description", model.getDescription());
        } catch (Throwable t) {
            throw t;
        } finally {
            model = this._contentModelManager.getContentModel(modelId);
            if (null != model) {
                this._contentModelManager.removeContentModel(model);
            }
            eventModels = this._contentModelManager.getModelsForContentType("EVN");
            assertEquals(0, eventModels.size());
        }
    }

    @Test
    void testTrashModel() throws Throwable {
        long modelId = 1;
        this.setUserOnSession("admin");

        this.initAction("/do/jacms/ContentModel", "trash");
        this.addParameter("modelId", String.valueOf(modelId));
        String result = this.executeAction();
        assertEquals(Action.SUCCESS, result);
    }

    @Test
    void testTrashReferencedModel() throws Throwable {
        long modelId = 2;
        this.setUserOnSession("admin");
        this.initAction("/do/jacms/ContentModel", "trash");
        this.addParameter("modelId", String.valueOf(modelId));
        String result = this.executeAction();
        assertEquals("references", result);
    }

    @Test
    void testDeleteModel() throws Throwable {
        List<ContentModel> eventModels = this._contentModelManager.getModelsForContentType("EVN");
        assertEquals(0, eventModels.size());
        long modelId = 99;
        this.addModelForTest(modelId, "EVN");
        eventModels = this._contentModelManager.getModelsForContentType("EVN");
        assertEquals(1, eventModels.size());
        ContentModel model = (ContentModel) eventModels.get(0);
        try {
            this.setUserOnSession("admin");

            this.initAction("/do/jacms/ContentModel", "delete");
            this.addParameter("modelId", String.valueOf(modelId));
            String result = this.executeAction();
            assertEquals(Action.SUCCESS, result);
            model = this._contentModelManager.getContentModel(modelId);
            assertNull(model);

            eventModels = this._contentModelManager.getModelsForContentType("EVN");
            assertEquals(0, eventModels.size());
        } catch (Throwable t) {
            model = this._contentModelManager.getContentModel(modelId);
            if (null != model) {
                this._contentModelManager.removeContentModel(model);
            }
            eventModels = this._contentModelManager.getModelsForContentType("EVN");
            assertEquals(0, eventModels.size());
            throw t;
        }
    }

    @Test
    void testDeleteReferencedModel() throws Throwable {
        this.setUserOnSession("admin");

        this.initAction("/do/jacms/ContentModel", "trash");
        this.addParameter("modelId", "2");
        String result = this.executeAction();
        assertEquals("references", result);
        ContentModelAction action = (ContentModelAction) this.getAction();

        List<ContentModelReference> references = action.getContentModelReferences();
        assertEquals(5, references.size());

        ContentModelReference ref0 = references.get(0);
        assertEquals(1, ref0.getContentsId().size());
        assertEquals("ART1", ref0.getContentsId().get(0));
        assertEquals("homepage", ref0.getPageCode());
        assertEquals(3, ref0.getWidgetPosition());
        assertEquals(false, ref0.isOnline());

        ContentModelReference ref1 = references.get(1);
        assertEquals(1, ref1.getContentsId().size());
        assertEquals("ART1", ref1.getContentsId().get(0));
        assertEquals("homepage", ref1.getPageCode());
        assertEquals(3, ref1.getWidgetPosition());
        assertEquals(true, ref1.isOnline());

        ContentModelReference ref2 = references.get(2);
        assertEquals("ART1", ref2.getContentsId().get(0));
        assertEquals("referencing_page", ref2.getPageCode());
        assertEquals(0, ref2.getWidgetPosition());
        assertEquals(false, ref2.isOnline());
        
        ContentModelReference ref3 = references.get(3);
        assertEquals(2, ref3.getContentsId().size());
        assertEquals("referencing_page", ref3.getPageCode());
        assertEquals(1, ref3.getWidgetPosition());
        assertEquals(false, ref3.isOnline());
        
        ContentModelReference ref4 = references.get(4);
        assertFalse(ref4.getContentsId().isEmpty());
        assertEquals("referencing_page", ref4.getPageCode());
        assertEquals(2, ref4.getWidgetPosition());
        assertEquals(false, ref4.isOnline());
    }

    private void addModelForTest(long id, String contentType) throws Throwable {
        ContentModel model = new ContentModel();
        model.setId(id);
        model.setContentType(contentType);
        model.setDescription("contentModel description");
        model.setContentShape("contentShape field value");
        this._contentModelManager.addContentModel(model);
    }

    /**
     * Sets up a page referencing some content models.
     */
    private void addReferencingPage() throws Exception {
        this.deleteReferencingPage();
        IPage root = this._pageManager.getDraftRoot();

        Page page = new Page();
        page.setCode("referencing_page");
        page.setTitle("en", "Test");
        page.setTitle("it", "Test");
        page.setParentCode(root.getCode());
        page.setGroup(root.getGroup());
        PageMetadata pageMetadata = new PageMetadata();
        pageMetadata.setGroup(root.getGroup());
        pageMetadata.setMimeType("text/html");
        pageMetadata.setModelCode(root.getModelCode());
        pageMetadata.setTitles(page.getTitles());
        page.setMetadata(pageMetadata);
        PageModel pageModel = this.pageModelManager.getPageModel(root.getModelCode());
        page.setWidgets(new Widget[pageModel.getFrames().length]);

        this._pageManager.addPage(page);

        this._pageManager.joinWidget("referencing_page", getSingleContentWidget(), 0);
        this._pageManager.joinWidget("referencing_page", getMultipleContentWidget(), 1);
        this._pageManager.joinWidget("referencing_page", getListOfContentsWidget(), 2);
    }

    private Widget getSingleContentWidget() {
        Widget widget = new Widget();
        widget.setTypeCode("content_viewer");
        ApsProperties widgetConfig = new ApsProperties();
        widgetConfig.put("contentId", "ART1");
        widgetConfig.put("modelId", "2");
        widget.setConfig(widgetConfig);
        return widget;
    }
    
    private Widget getMultipleContentWidget() {
        Widget widget = new Widget();
        widget.setTypeCode("row_content_viewer_list");
        ApsProperties widgetConfig = new ApsProperties();
        widgetConfig.put("contents", "[{contentId=ART1,modelId=2},{modelId=2,contentId=ART187}]");
        widgetConfig.put("modelId", "2");
        widget.setConfig(widgetConfig);
        return widget;
    }
    
    private Widget getListOfContentsWidget() {
        Widget widget = new Widget();
        widget.setTypeCode("content_viewer_list");
        ApsProperties widgetConfig = new ApsProperties();
        widgetConfig.put("maxElemForItem", "5");
        widgetConfig.put("contentType", "ART");
        widgetConfig.put("modelId", "2");
        widget.setConfig(widgetConfig);
        return widget;
    }

    @BeforeEach
    private void init() throws Exception {
        this._widgetTypeManager = (IWidgetTypeManager) this.getService(SystemConstants.WIDGET_TYPE_MANAGER);
        this._pageManager = (IPageManager) this.getService(SystemConstants.PAGE_MANAGER);
        this.pageModelManager = (IPageModelManager) this.getService(SystemConstants.PAGE_MODEL_MANAGER);
        this._contentModelManager = (IContentModelManager) this.getService(JacmsSystemConstants.CONTENT_MODEL_MANAGER);
        this.addReferencingPage();
    }

    @AfterEach
    private void deleteReferencingPage() throws Exception {
        this._pageManager.deletePage("referencing_page");
    }

    private IContentModelManager _contentModelManager;
    private IPageManager _pageManager;
    private IPageModelManager pageModelManager;
    private IWidgetTypeManager _widgetTypeManager;
}
