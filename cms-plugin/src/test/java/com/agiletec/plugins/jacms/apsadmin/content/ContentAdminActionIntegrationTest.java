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
package com.agiletec.plugins.jacms.apsadmin.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.IManager;
import com.agiletec.aps.system.common.entity.ApsEntityManager;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.apsadmin.system.BaseAction;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.resource.IResourceManager;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.SearchEngineManager;
import com.agiletec.plugins.jacms.apsadmin.content.util.AbstractBaseTestContentAction;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author E.Mezzano
 */
class ContentAdminActionIntegrationTest extends AbstractBaseTestContentAction {

    private IResourceManager resourceManager;

    private ConfigInterface configManager;

    @Test
    void testOpenIndexProspect() throws Throwable {
        ICmsSearchEngineManager searchEngineManager = getApplicationContext().getBean(ICmsSearchEngineManager.class);
        ((IManager)searchEngineManager).refresh();
        String result = this.executeOpenIndexProspect("admin");
        assertEquals(BaseAction.SUCCESS, result);
        ContentAdminAction contentAdminAction = (ContentAdminAction) this.getAction();
        assertEquals(IContentManager.STATUS_READY, contentAdminAction.getContentManagerStatus());
        assertEquals(ICmsSearchEngineManager.STATUS_READY, contentAdminAction.getSearcherManagerStatus());
        assertNull(contentAdminAction.getLastReloadInfo());
        Map<String, List<String>> mapping = contentAdminAction.getMapping();
        assertEquals(4, mapping.size());
        assertEquals("metadataKey1,metadataKey2,metadatakey3,xxx,yyy", contentAdminAction.buildCsv(IResourceManager.ALT_METADATA_KEY));
        assertEquals("metadataKeyA,metadataKeyB,JPEG Comment", contentAdminAction.buildCsv(IResourceManager.DESCRIPTION_METADATA_KEY));
        assertEquals("metadataKeyX,metadataKeyY,metadatakeyX,YYYY,Detected File Type Long Name,WWWWW", contentAdminAction.buildCsv(IResourceManager.LEGEND_METADATA_KEY));
        assertEquals("metadataKeyG,metadataKeyK,metadatakeyF", contentAdminAction.buildCsv(IResourceManager.TITLE_METADATA_KEY));
    }

    @Test
    void testReloadContentsIndex() throws Throwable {
        String result = this.executeReloadContentsIndex("admin");
        assertEquals(BaseAction.SUCCESS, result);
        this.waitReloadThreads();
        ContentAdminAction contentAdminAction = (ContentAdminAction) this.getAction();
        assertEquals(IContentManager.STATUS_READY, contentAdminAction.getContentManagerStatus());
        assertEquals(ICmsSearchEngineManager.STATUS_READY, contentAdminAction.getSearcherManagerStatus());
        assertNotNull(contentAdminAction.getLastReloadInfo());
    }

    @Test
    void testReloadContentsReference() throws Throwable {
        String result = this.executeReloadContentsReference("admin");
        assertEquals(BaseAction.SUCCESS, result);
        this.waitReloadThreads();
        ContentAdminAction contentAdminAction = (ContentAdminAction) this.getAction();
        assertEquals(IContentManager.STATUS_READY, contentAdminAction.getContentManagerStatus());
        assertEquals(ICmsSearchEngineManager.STATUS_READY, contentAdminAction.getSearcherManagerStatus());
    }

    @Test
    void testUpdateResourceMapping() throws Throwable {
        Map<String, List<String>> defaultMapping = this.resourceManager.getMetadataMapping();
        try {
            this.initAction("/do/jacms/Content/Admin", "updateSystemParams");
            this.setUserOnSession("admin");
            super.addParameter("resourceMetadata_mapping_" + IResourceManager.ALT_METADATA_KEY,
                    "metadataKey1,metadataKey2,metadatakey3,metadatakey4,metadatakey5");
            super.addParameter("resourceMetadata_mapping_" + IResourceManager.DESCRIPTION_METADATA_KEY,
                    "newMetadataKeyA,newMetadataKeyB,JPEG Comment,Component 2");
            super.addParameter("resourceMetadata_mapping_" + IResourceManager.LEGEND_METADATA_KEY, "");
            super.addParameter("resourceMetadata_mapping_" + IResourceManager.TITLE_METADATA_KEY, "metadataKeyG,metadataKeyK,metadatakeyF,metadatakeyZ");
            super.addParameter("metadataKeys", new String[]{IResourceManager.ALT_METADATA_KEY, IResourceManager.DESCRIPTION_METADATA_KEY,
                IResourceManager.LEGEND_METADATA_KEY, IResourceManager.TITLE_METADATA_KEY, "test_parameter"});
            String result = this.executeAction();
            assertEquals(BaseAction.SUCCESS, result);
            Map<String, List<String>> newMapping = this.resourceManager.getMetadataMapping();
            assertEquals(5, newMapping.size());
            assertEquals(5, newMapping.get(IResourceManager.ALT_METADATA_KEY).size());
            assertEquals("metadatakey4", newMapping.get(IResourceManager.ALT_METADATA_KEY).get(3));
            assertEquals(4, newMapping.get(IResourceManager.DESCRIPTION_METADATA_KEY).size());
            assertEquals("Component 2", newMapping.get(IResourceManager.DESCRIPTION_METADATA_KEY).get(3));
            assertEquals(4, newMapping.get(IResourceManager.TITLE_METADATA_KEY).size());
            assertEquals(0, newMapping.get("test_parameter").size());
        } catch (Throwable e) {
            throw e;
        } finally {
            this.resourceManager.updateMetadataMapping(defaultMapping);
        }
    }

    @Test
    void testAddNewMetadata() throws Throwable {
        Map<String, List<String>> defaultMapping = this.resourceManager.getMetadataMapping();
        try {
            this.initAction("/do/jacms/Content/Admin", "addMetadata");
            this.setUserOnSession("admin");
            super.addParameter("resourceMetadata_mapping_test_1", "metadataKey1,metadataKey2");
            super.addParameter("resourceMetadata_mapping_test_2", "newMetadataKeyA,newMetadataKeyB");
            super.addParameter("metadataKeys", new String[]{"test_1", "test_2"});
            super.addParameter("metadataKey", "test_3");
            String result = this.executeAction();
            ContentAdminAction action = (ContentAdminAction) super.getAction();
            assertEquals(BaseAction.SUCCESS, result);
            assertEquals(0, action.getFieldErrors().size());
            Map<String, List<String>> mappingIntoAction = action.getMapping();
            assertEquals(3, mappingIntoAction.size());
            assertEquals(0, mappingIntoAction.get("test_3").size());
            assertEquals(2, mappingIntoAction.get("test_1").size());
            assertEquals(2, mappingIntoAction.get("test_2").size());
        } catch (Throwable e) {
            throw e;
        } finally {
            this.resourceManager.updateMetadataMapping(defaultMapping);
        }
    }

    @Test
    void testRemoveMetadata() throws Throwable {
        Map<String, List<String>> defaultMapping = this.resourceManager.getMetadataMapping();
        try {
            this.initAction("/do/jacms/Content/Admin", "removeMetadata");
            this.setUserOnSession("admin");
            super.addParameter("resourceMetadata_mapping_test_1", "metadataKey1,metadataKey2");
            super.addParameter("resourceMetadata_mapping_test_2", "newMetadataKeyA,newMetadataKeyB");
            super.addParameter("metadataKeys", new String[]{"test_1", "test_2"});
            super.addParameter("metadataKey", "test_1");
            String result = this.executeAction();
            ContentAdminAction action = (ContentAdminAction) super.getAction();
            assertEquals(BaseAction.SUCCESS, result);
            assertEquals(0, action.getFieldErrors().size());
            Map<String, List<String>> mappingIntoAction = action.getMapping();
            assertEquals(1, mappingIntoAction.size());
            assertEquals(2, mappingIntoAction.get("test_2").size());
        } catch (Throwable e) {
            throw e;
        } finally {
            this.resourceManager.updateMetadataMapping(defaultMapping);
        }
    }

    @Test
    void testValidateNewMetadata() throws Throwable {
        this.executeValidateNewMetadata("wrongKey_&&");
        this.executeValidateNewMetadata("wrong key");
        this.executeValidateNewMetadata("tes");
        this.executeValidateNewMetadata("very_long_key_1234567890");
        this.executeValidateNewMetadata(IResourceManager.TITLE_METADATA_KEY);
    }

    @Test
    void testAddValidAspectRatio() throws Throwable {
        Map<String, List<String>> defaultMapping = this.resourceManager.getMetadataMapping();
        String defaultXmlParameters = this.configManager.getConfigItem(SystemConstants.CONFIG_ITEM_PARAMS);
        try {
            this.initAction("/do/jacms/Content/Admin", "updateSystemParams");
            this.setUserOnSession("admin");
            this.addParameter("ratio", new String[]{"16:9", "4:3"});
            String result = this.executeAction();
            ContentAdminAction action = (ContentAdminAction) super.getAction();
            assertEquals(BaseAction.SUCCESS, result);
            assertEquals(0, action.getFieldErrors().size());
            assertEquals("16:9;4:3", action.getAspectRatio());
            assertEquals(2, action.getRatio().size());
            String config = this.configManager.getParam(JacmsSystemConstants.CONFIG_PARAM_ASPECT_RATIO);
            assertEquals("16:9;4:3", config);
            
            this.initAction("/do/jacms/Content/Admin", "updateSystemParams");
            this.setUserOnSession("admin");
            this.addParameter("ratio", new String[0]);
            result = this.executeAction();
            assertEquals(BaseAction.SUCCESS, result);
            assertEquals(0, action.getFieldErrors().size());
            config = this.configManager.getParam(JacmsSystemConstants.CONFIG_PARAM_ASPECT_RATIO);
            assertEquals("", config);
        } catch (Throwable e) {
            throw e;
        } finally {
            this.resourceManager.updateMetadataMapping(defaultMapping);
            this.configManager.updateConfigItem(SystemConstants.CONFIG_ITEM_PARAMS, defaultXmlParameters);
        }
    }

    @Test
    void testAddInvalidAspectRatio() throws Throwable {
        Map<String, List<String>> defaultMapping = this.resourceManager.getMetadataMapping();
        try {
            this.initAction("/do/jacms/Content/Admin", "updateSystemParams");
            this.setUserOnSession("admin");
            this.addParameter("ratio", new String[]{"invalid"});
            String result = this.executeAction();
            ContentAdminAction action = (ContentAdminAction) super.getAction();
            assertEquals(BaseAction.INPUT, result);
            assertEquals(1, action.getFieldErrors().size());
        } catch (Throwable e) {
            throw e;
        } finally {
            this.resourceManager.updateMetadataMapping(defaultMapping);
        }
    }

    protected void executeValidateNewMetadata(String newWrongMetadata) throws Throwable {
        Map<String, List<String>> defaultMapping = this.resourceManager.getMetadataMapping();
        try {
            this.initAction("/do/jacms/Content/Admin", "addMetadata");
            this.setUserOnSession("admin");
            super.addParameter("resourceMetadata_mapping_" + IResourceManager.ALT_METADATA_KEY,
                    "metadataKey1,metadataKey2,metadatakey3,metadatakey4,metadatakey5");
            super.addParameter("resourceMetadata_mapping_" + IResourceManager.DESCRIPTION_METADATA_KEY,
                    "newMetadataKeyA,newMetadataKeyB,JPEG Comment,Component 2");
            super.addParameter("resourceMetadata_mapping_" + IResourceManager.LEGEND_METADATA_KEY, "");
            super.addParameter("resourceMetadata_mapping_" + IResourceManager.TITLE_METADATA_KEY,
                    "metadataKeyG,metadataKeyK,metadatakeyF");
            super.addParameter("metadataKeys", new String[]{IResourceManager.ALT_METADATA_KEY, IResourceManager.DESCRIPTION_METADATA_KEY,
                IResourceManager.LEGEND_METADATA_KEY, IResourceManager.TITLE_METADATA_KEY, "test_metadata_1", "test_metadata_2"});
            super.addParameter("metadataKey", newWrongMetadata);
            String result = this.executeAction();
            ContentAdminAction action = (ContentAdminAction) super.getAction();
            assertEquals(BaseAction.INPUT, result);
            assertEquals(1, action.getFieldErrors().size());
            Map<String, List<String>> mappingIntoAction = action.getMapping();
            assertEquals(6, mappingIntoAction.size());
            assertEquals(5, mappingIntoAction.get(IResourceManager.ALT_METADATA_KEY).size());
            assertEquals("metadatakey4", mappingIntoAction.get(IResourceManager.ALT_METADATA_KEY).get(3));
            assertEquals(4, mappingIntoAction.get(IResourceManager.DESCRIPTION_METADATA_KEY).size());
            assertEquals("Component 2", mappingIntoAction.get(IResourceManager.DESCRIPTION_METADATA_KEY).get(3));
            assertEquals(3, mappingIntoAction.get(IResourceManager.TITLE_METADATA_KEY).size());
            assertEquals(0, mappingIntoAction.get(IResourceManager.LEGEND_METADATA_KEY).size());
            assertEquals(0, mappingIntoAction.get("test_metadata_1").size());
            assertEquals(0, mappingIntoAction.get("test_metadata_2").size());
        } catch (Throwable e) {
            throw e;
        } finally {
            this.resourceManager.updateMetadataMapping(defaultMapping);
        }
    }

    private String executeOpenIndexProspect(String currentUserName) throws Throwable {
        this.initAction("/do/jacms/Content/Admin", "openIndexProspect");
        this.setUserOnSession(currentUserName);
        return this.executeAction();
    }

    private String executeReloadContentsIndex(String currentUserName) throws Throwable {
        this.initAction("/do/jacms/Content/Admin", "reloadContentsIndex");
        this.setUserOnSession(currentUserName);
        return this.executeAction();
    }

    private String executeReloadContentsReference(String currentUserName) throws Throwable {
        this.initAction("/do/jacms/Content/Admin", "reloadContentsReference");
        this.setUserOnSession(currentUserName);
        return this.executeAction();
    }

    private void waitReloadThreads() throws InterruptedException {
        Thread[] threads = new Thread[40];
        Thread.enumerate(threads);
        for (int i = 0; i < threads.length; i++) {
            Thread currentThread = threads[i];
            if (currentThread != null
                    && (currentThread.getName().startsWith(SearchEngineManager.RELOAD_THREAD_NAME_PREFIX) || currentThread.getName().startsWith(ApsEntityManager.RELOAD_REFERENCES_THREAD_NAME_PREFIX))) {
                currentThread.join();
            }
        }
    }

    @BeforeEach
    protected void init() throws Exception {
        try {
            this.resourceManager = this.getApplicationContext().getBean(IResourceManager.class);
            this.configManager = this.getApplicationContext().getBean(ConfigInterface.class);
        } catch (Throwable t) {
            throw new Exception(t);
        }
    }

}
