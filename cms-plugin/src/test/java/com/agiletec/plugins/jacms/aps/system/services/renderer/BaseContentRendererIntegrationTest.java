/*
 * Copyright 2023-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package com.agiletec.plugins.jacms.aps.system.services.renderer;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.services.i18n.II18nManager;
import com.agiletec.aps.util.ApsProperties;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.contentmodel.ContentModel;
import com.agiletec.plugins.jacms.aps.system.services.contentmodel.IContentModelManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BaseContentRendererIntegrationTest extends BaseTestCase {
    
    @Test
    void testRenderLabel() throws Throwable {
        String key1 = "TEST_LABEL";
        ApsProperties labels1 = new ApsProperties();
        labels1.put("it", "Testo1 Italiano (test 1)");
        labels1.put("en", "English text1 (test 1)");
        String key2 = "INCOMPLETE_LABEL";
        ApsProperties labels2 = new ApsProperties();
        labels2.put("it", "Testo2 Italiano (test 2)");
        String contentId = "ART120";
        String contentShapeModel = "text=$content.Titolo.getText() - label $i18n.getLabel(\"TEST_LABEL\") - label $i18n.getLabel(\"INCOMPLETE_LABEL\") - missing $i18n.getLabel(\"MISSING_LABEL\")";
        int modelId = 1972;
        try {
            Assertions.assertNull(this.i18nManager.getLabelGroups().get(key1));
            Assertions.assertNull(this.i18nManager.getLabelGroups().get(key2));
            this.i18nManager.addLabelGroup(key1, labels1);
            Assertions.assertNotNull(this.i18nManager.getLabelGroups().get(key1));
            this.i18nManager.addLabelGroup(key2, labels2);
            Assertions.assertNotNull(this.i18nManager.getLabelGroups().get(key2));
            
            
            this.addNewContentModel(modelId, contentShapeModel, "ART");
            RequestContext reqCtx = this.getRequestContext();
            this.setUserOnSession("admin");
            
            Content content = this._contentManager.loadContent(contentId, true);
            
            String render = contentRenderer.render(content, modelId, "it", reqCtx);
            Assertions.assertEquals("text=Titolo Contenuto degli &quot;Amministratori&quot; - label Testo1 Italiano (test 1) - label Testo2 Italiano (test 2) - missing MISSING_LABEL", render);
            
            render = contentRenderer.render(content, modelId, "en", reqCtx);
            Assertions.assertEquals("text=Title of Administrator's Content - label English text1 (test 1) - label Testo2 Italiano (test 2) - missing MISSING_LABEL", render);
        } catch (Exception e) {
            throw e;
        } finally {
            ContentModel model = this._contentModelManager.getContentModel(modelId);
            if (null != model) {
                this._contentModelManager.removeContentModel(model);
            }
            this.i18nManager.deleteLabelGroup(key1);
            Assertions.assertNull(this.i18nManager.getLabelGroups().get(key1));
            this.i18nManager.deleteLabelGroup(key2);
            Assertions.assertNull(this.i18nManager.getLabelGroups().get(key2));
        }
    }
    
    public void addNewContentModel(int id, String shape, String contentTypeCode) throws Throwable {
        ContentModel model = new ContentModel();
        model.setContentType(contentTypeCode);
        model.setDescription("test");
        model.setId(id);
        model.setContentShape(shape);
        this._contentModelManager.addContentModel(model);
    }
    
    @BeforeEach
    void init() throws Exception {
        try {
            this.contentRenderer = this.getApplicationContext().getBean(IContentRenderer.class);
            this.i18nManager = this.getApplicationContext().getBean(II18nManager.class);
            this._contentManager = (IContentManager) this.getService(JacmsSystemConstants.CONTENT_MANAGER);
            this._contentModelManager = (IContentModelManager) this.getService(JacmsSystemConstants.CONTENT_MODEL_MANAGER);
        } catch (Throwable t) {
            throw new Exception(t);
        }
    }
    
    private IContentRenderer contentRenderer;
    private IContentManager _contentManager = null;
    private IContentModelManager _contentModelManager = null;
    private II18nManager i18nManager;
    
    
    
    
    
}
