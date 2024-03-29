
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
package com.agiletec.plugins.jacms.aps.system.services.content.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.entando.entando.aps.system.services.widgettype.IWidgetTypeManager;
import org.entando.entando.aps.system.services.widgettype.WidgetType;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.common.entity.model.attribute.ITextAttribute;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.aps.util.ApsProperties;
import com.agiletec.aps.util.DateConverter;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import java.util.ArrayList;
import org.apache.commons.collections4.CollectionUtils;
import org.entando.entando.aps.system.services.cache.ICacheInfoManager;
import org.junit.jupiter.api.*;
import org.springframework.cache.CacheManager;

/**
 * @author E.Santoboni
 */
class ContentListHelperIntegrationTest extends BaseTestCase {

    private IContentManager contentManager;
    private CacheManager springCacheManager;
    private IPageManager pageManager = null;
    private ILangManager langManager = null;
    private IWidgetTypeManager widgetTypeManager;
    private IContentListWidgetHelper helper;

    @Test
    void testGetFilters() throws Throwable {
        String filtersShowletParam = "(key=DataInizio;attributeFilter=true;start=21/10/2007;order=DESC)+(key=Titolo;attributeFilter=true;order=ASC)";
        EntitySearchFilter[] filters = this.helper.getFilters("EVN", filtersShowletParam, this.getRequestContext());
        assertEquals(2, filters.length);
        EntitySearchFilter filter = filters[0];
        assertEquals("DataInizio", filter.getKey());
        assertEquals(DateConverter.parseDate("21/10/2007", "dd/MM/yyyy"), filter.getStart());
        assertNull(filter.getEnd());
        assertNull(filter.getValue());
        assertEquals("DESC", filter.getOrder().toString());
    }

    @Test
    void testGetFilters_OneDefinition() {
        RequestContext reqCtx = this.getRequestContext();
        String contentType = "ART";
        String showletParam = "(key=Titolo;attributeFilter=TRUE;start=START;end=END;like=FALSE;order=ASC)";
        EntitySearchFilter[] filters = this.helper.getFilters(contentType, showletParam, reqCtx);

        assertNotNull(filters);
        assertEquals(1, filters.length);

        EntitySearchFilter entitySearchFilter = filters[0];
        assertNotNull(entitySearchFilter);

        assertEquals("Titolo", entitySearchFilter.getKey());
        assertEquals("START", entitySearchFilter.getStart());
        assertEquals("END", entitySearchFilter.getEnd());
        assertEquals("ASC", entitySearchFilter.getOrder().toString());

        contentType = "ART";
        showletParam = "(key=Titolo;attributeFilter=TRUE;start=START;end=END;like=FALSE;order=DESC)";
        filters = this.helper.getFilters(contentType, showletParam, reqCtx);

        assertNotNull(filters);
        assertEquals(1, filters.length);

        entitySearchFilter = filters[0];
        assertNotNull(entitySearchFilter);

        assertEquals("Titolo", entitySearchFilter.getKey());
        assertEquals("START", entitySearchFilter.getStart());
        assertEquals("END", entitySearchFilter.getEnd());
        assertEquals("DESC", entitySearchFilter.getOrder().toString());

        contentType = "ART";
        showletParam = "(key=descr;value=VALUE;attributeFilter=FALSE;order=ASC)";
        filters = this.helper.getFilters(contentType, showletParam, reqCtx);

        assertNotNull(filters);
        assertEquals(1, filters.length);

        entitySearchFilter = filters[0];
        assertNotNull(entitySearchFilter);

        assertEquals("descr", entitySearchFilter.getKey());
        assertEquals(null, entitySearchFilter.getStart());
        assertEquals(null, entitySearchFilter.getEnd());
        assertEquals("ASC", entitySearchFilter.getOrder().toString());
    }

    @Test
    void testGetFilters_TwoDefinition() {
        RequestContext reqCtx = this.getRequestContext();
        String contentType = "ART";
        String showletParam = "(key=Titolo;attributeFilter=TRUE;start=START;end=END;like=FALSE;order=ASC)+(key=descr;value=VALUE;attributeFilter=FALSE;order=ASC)";
        EntitySearchFilter[] filters = this.helper.getFilters(contentType, showletParam, reqCtx);

        assertNotNull(filters);
        assertEquals(2, filters.length);

        EntitySearchFilter entitySearchFilter = filters[0];
        assertNotNull(entitySearchFilter);

        assertEquals("Titolo", entitySearchFilter.getKey());
        assertEquals("START", entitySearchFilter.getStart());
        assertEquals("END", entitySearchFilter.getEnd());
        assertEquals("ASC", entitySearchFilter.getOrder().toString());
        assertEquals(null, entitySearchFilter.getValue());
        assertTrue(entitySearchFilter.isAttributeFilter());

        entitySearchFilter = filters[1];
        assertNotNull(entitySearchFilter);

        assertEquals("descr", entitySearchFilter.getKey());
        assertEquals(null, entitySearchFilter.getStart());
        assertEquals(null, entitySearchFilter.getEnd());
        assertEquals("ASC", entitySearchFilter.getOrder().toString());
        assertFalse(entitySearchFilter.isAttributeFilter());
        Object obj = entitySearchFilter.getValue();
        assertNotNull(obj);
        assertEquals(String.class, obj.getClass());
        assertEquals("VALUE", (String) obj);
    }

    @Test
    void testGetContents_1() throws Throwable {
        String newContentId = null;
        String pageCode = "pagina_1";
        int frame = 1;
        try {
            this.setUserOnSession("guest");
            RequestContext reqCtx = this.valueRequestContext(pageCode, frame, null, null);
            MockContentListTagBean bean = new MockContentListTagBean();
            bean.setContentType("EVN");
            EntitySearchFilter filter = new EntitySearchFilter("DataInizio", true);
            filter.setOrder(EntitySearchFilter.DESC_ORDER);
            bean.addFilter(filter);
            String cacheKey = ContentListHelper.buildCacheKey(bean, reqCtx);
            assertNull(this.springCacheManager.getCache(ICacheInfoManager.DEFAULT_CACHE_NAME).get(cacheKey));

            List<String> contents = this.helper.getContentsId(bean, reqCtx);
            String[] expected = {"EVN194", "EVN193", "EVN24", "EVN23", "EVN25", "EVN20", "EVN21", "EVN192", "EVN191"};
            assertEquals(expected.length, contents.size());
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], contents.get(i));
            }
            assertNotNull(this.springCacheManager.getCache(ICacheInfoManager.DEFAULT_CACHE_NAME).get(cacheKey));

            Content content = this.contentManager.createContentType("EVN");
            content.setDescription("Test Content");
            content.setMainGroup(Group.FREE_GROUP_NAME);
            contentManager.insertOnLineContent(content);
            newContentId = content.getId();
            assertNotNull(newContentId);
            assertNull(this.springCacheManager.getCache(ICacheInfoManager.DEFAULT_CACHE_NAME).get(cacheKey));
        } catch (Throwable t) {
            throw t;
        } finally {
            this.setPageWidgets(pageCode, frame, null);
            this.deleteTestContent(newContentId);
        }
    }

    @Test
    void testGetContents_2() throws Throwable {
        String newContentId = null;
        String pageCode = "pagina_1";
        int frame = 1;
        try {
            this.setUserOnSession("admin");
            RequestContext reqCtx = this.valueRequestContext(pageCode, frame, null, null);
            MockContentListTagBean bean = new MockContentListTagBean();
            bean.setContentType("EVN");
            bean.addCategory("evento");
            EntitySearchFilter filter = new EntitySearchFilter("DataInizio", true);
            filter.setOrder(EntitySearchFilter.DESC_ORDER);
            bean.addFilter(filter);
            String cacheKey = ContentListHelper.buildCacheKey(bean, reqCtx);
            assertNull(this.springCacheManager.getCache(ICacheInfoManager.DEFAULT_CACHE_NAME).get(cacheKey));
            List<String> contents = this.helper.getContentsId(bean, reqCtx);
            String[] expected = {"EVN193", "EVN192"};
            assertEquals(expected.length, contents.size());
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], contents.get(i));
            }
            assertNotNull(this.springCacheManager.getCache(ICacheInfoManager.DEFAULT_CACHE_NAME).get(cacheKey));

            Content content = this.contentManager.createContentType("EVN");
            content.setDescription("Test Content");
            content.setMainGroup(Group.ADMINS_GROUP_NAME);
            contentManager.insertOnLineContent(content);
            newContentId = content.getId();
            assertNotNull(newContentId);
            assertNull(this.springCacheManager.getCache(ICacheInfoManager.DEFAULT_CACHE_NAME).get(cacheKey));

        } catch (Throwable t) {
            throw t;
        } finally {
            this.setPageWidgets(pageCode, frame, null);
            this.deleteTestContent(newContentId);
        }
    }

    @Test
    // synchronized modifier is used here to avoid issues caused by concurrent calls of
    // helper.getContentsId() method when running tests in CI environment
    synchronized void testGetContents_3() throws Throwable {
        String newContentId = null;
        String pageCode = "pagina_1";
        int frame = 1;
        try {
            List<String> userGroupCodes = new ArrayList<>();
            userGroupCodes.add(Group.ADMINS_GROUP_NAME);
            List<String> contents = this.contentManager.loadPublicContentsId("ART", null, null, userGroupCodes);

            this.setUserOnSession("admin");
            RequestContext reqCtx = this.valueRequestContext(pageCode, frame, null, null);
            MockContentListTagBean bean = new MockContentListTagBean();
            bean.setContentType("ART");

            String cacheKey = ContentListHelper.buildCacheKey(bean, reqCtx);
            assertNull(this.springCacheManager.getCache(ICacheInfoManager.DEFAULT_CACHE_NAME).get(cacheKey));

            List<String> extractedContents = this.helper.getContentsId(bean, reqCtx);
            assertEquals(contents.size(), extractedContents.size());
            CollectionUtils.containsAll(extractedContents, contents);

            assertNotNull(this.springCacheManager.getCache(ICacheInfoManager.DEFAULT_CACHE_NAME).get(cacheKey));

            Content content = this.contentManager.createContentType("ART");
            content.setDescription("Test Content");
            content.setMainGroup(Group.ADMINS_GROUP_NAME);
            contentManager.insertOnLineContent(content);
            newContentId = content.getId();
            assertNotNull(newContentId);
            assertNull(this.springCacheManager.getCache(ICacheInfoManager.DEFAULT_CACHE_NAME).get(cacheKey));
        } catch (Throwable t) {
            throw t;
        } finally {
            this.setPageWidgets(pageCode, frame, null);
            this.deleteTestContent(newContentId);
        }
    }
    
    @Test
    void testGetContents_4() throws Throwable {
        String newContentId = null;
        String pageCode = "pagina_1";
        int frame = 1;
        try {
            ApsProperties config = new ApsProperties();
            config.setProperty("contentType", "EVN");
            config.setProperty("filters", "(key=Titolo;attributeFilter=true;order=ASC)");
            this.setUserOnSession("admin");
            RequestContext reqCtx = this.valueRequestContext(pageCode, frame, "it", config);
            List<String> extractedItContents = this.helper.getContentsId(new MockContentListTagBean(), reqCtx);
            String[] expectedIt = {"EVN24", "EVN23", "EVN21", "EVN20", "EVN41", "EVN25", "EVN191", "EVN192", "EVN193", "EVN103", "EVN194"};
            for (int i = 0; i < expectedIt.length; i++) {
                assertEquals(expectedIt[i], extractedItContents.get(i));
            }
            reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG, this.langManager.getLang("en"));
            List<String> extractedEnContents = this.helper.getContentsId(new MockContentListTagBean(), reqCtx);
            String[] expectedEn = {"EVN41", "EVN24", "EVN103", "EVN23", "EVN21", "EVN194", "EVN192", "EVN191", "EVN193", "EVN25", "EVN20"};
            for (int i = 0; i < expectedEn.length; i++) {
                assertEquals(expectedEn[i], extractedEnContents.get(i));
            }
            Content content = this.contentManager.createContentType("EVN");
            content.setDescription("Test Content");
            content.setMainGroup(Group.ADMINS_GROUP_NAME);
            ITextAttribute titleAttribute = (ITextAttribute) content.getAttribute("Titolo");
            titleAttribute.setText("Aaa Title", "it");
            titleAttribute.setText("Zzz Title", "en");
            this.contentManager.insertOnLineContent(content);
            newContentId = content.getId();
            
            reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG, this.langManager.getLang("it"));
            List<String> newExtractedItContents = this.helper.getContentsId(new MockContentListTagBean(), reqCtx);
            assertEquals(newExtractedItContents.size(), extractedItContents.size()+1);
            assertEquals(newContentId, newExtractedItContents.get(0));
            for (int i = 0; i < expectedIt.length; i++) {
                assertEquals(expectedIt[i], newExtractedItContents.get(i+1));
            }
            
            reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG, this.langManager.getLang("en"));
            List<String> newExtractedEnContents = this.helper.getContentsId(new MockContentListTagBean(), reqCtx);
            assertEquals(newExtractedEnContents.size(), extractedEnContents.size()+1);
            assertEquals(newContentId, newExtractedEnContents.get(newExtractedEnContents.size()-1));
            for (int i = 0; i < expectedEn.length; i++) {
                assertEquals(expectedEn[i], newExtractedEnContents.get(i));
            }
        } catch (Throwable t) {
            throw t;
        } finally {
            this.setPageWidgets(pageCode, frame, null);
            this.deleteTestContent(newContentId);
        }
    }

    private void deleteTestContent(String newContentId) throws EntException {
        if (null != newContentId) {
            Content newContent = this.contentManager.loadContent(newContentId, false);
            if (null != newContent) {
                this.contentManager.removeOnLineContent(newContent);
                this.contentManager.deleteContent(newContent);
                assertNull(this.contentManager.loadContent(newContentId, false));
            }
        }
    }

    private RequestContext valueRequestContext(String pageCode, int frame, String langCode, ApsProperties config) throws Throwable {
        RequestContext reqCtx = this.getRequestContext();
        try {
            Widget widgetToAdd = this.getWidgetForTest("content_viewer_list", config);
            this.setPageWidgets(pageCode, frame, widgetToAdd);
            IPage page = this.pageManager.getOnlinePage(pageCode);
            reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE, page);
            Widget widget = page.getWidgets()[frame];
            reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_WIDGET, widget);
            reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_FRAME, frame);
            if (null != langCode && null != this.langManager.getLang(langCode)) {
                reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG, this.langManager.getLang(langCode));
            } else {
                reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG, this.langManager.getDefaultLang());
            }
        } catch (Throwable t) {
            this.setPageWidgets(pageCode, frame, null);
            throw t;
        }
        return reqCtx;
    }

    private void setPageWidgets(String pageCode, int frame, Widget widget) throws EntException {
        IPage page = this.pageManager.getDraftPage(pageCode);
        page.getWidgets()[frame] = widget;
        page.getWidgets()[frame] = widget;
        this.pageManager.updatePage(page);
        this.pageManager.setPageOnline(pageCode);
    }

    private Widget getWidgetForTest(String widgetTypeCode, ApsProperties config) throws Throwable {
        Widget widget = new Widget();
        widget.setTypeCode(widgetTypeCode);
        if (null != config) {
            widget.setConfig(config);
        }
        return widget;
    }

    @BeforeEach
    private void init() throws Exception {
        try {
            this.springCacheManager = (CacheManager) this.getApplicationContext().getBean(CacheManager.class);
            this.contentManager = (IContentManager) this.getService(JacmsSystemConstants.CONTENT_MANAGER);
            this.pageManager = (IPageManager) this.getService(SystemConstants.PAGE_MANAGER);
            this.langManager = (ILangManager) this.getService(SystemConstants.LANGUAGE_MANAGER);
            this.widgetTypeManager = (IWidgetTypeManager) this.getService(SystemConstants.WIDGET_TYPE_MANAGER);
            this.helper = (IContentListWidgetHelper) this.getApplicationContext().getBean(JacmsSystemConstants.CONTENT_LIST_HELPER);
        } catch (Throwable t) {
            throw new Exception(t);
        }
    }

}
