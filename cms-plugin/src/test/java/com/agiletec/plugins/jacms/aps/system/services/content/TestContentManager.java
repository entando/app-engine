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
package com.agiletec.plugins.jacms.aps.system.services.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.common.entity.ApsEntityManager;
import com.agiletec.aps.system.common.entity.IEntityTypesConfigurer;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeRole;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CheckBoxAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.DateAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.EnumeratorAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.HypertextAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.ListAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoListAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoTextAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.NumberAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.TextAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.ThreeStateAttribute;
import com.agiletec.aps.system.common.model.dao.SearcherDaoPaginatedResult;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.category.Category;
import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.group.IGroupManager;
import com.agiletec.aps.util.DateConverter;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.content.model.SmallContentType;
import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.AttachAttribute;
import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.ImageAttribute;
import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.LinkAttribute;
import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.ResourceAttributeInterface;
import com.agiletec.plugins.jacms.aps.system.services.resource.IResourceManager;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.entando.entando.aps.system.common.entity.model.attribute.EnumeratorMapAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author M. Morini - E.Santoboni
 */
class TestContentManager extends BaseTestCase {

    private List<String> freeGroup = Arrays.asList("free");

    @Test
    void testSearchContents_1_1() throws Throwable {
        List<String> contentIds = this._contentManager.searchId(null);
        assertNotNull(contentIds);
        assertEquals(25, contentIds.size());

        EntitySearchFilter creationOrder = new EntitySearchFilter(IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false);
        creationOrder.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter descrFilter = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "Cont", true);
        EntitySearchFilter[] filters1 = {creationOrder, descrFilter};
        contentIds = this._contentManager.searchId(filters1);
        assertNotNull(contentIds);
        String[] expected1 = {"RAH101", "ART102", "EVN103", "ART104", "ART111", "ART112", "ART120", "ART121", "ART122"};
        assertEquals(expected1.length, contentIds.size());
        this.verifyOrder(contentIds, expected1);

        EntitySearchFilter lastEditorFilter = new EntitySearchFilter(IContentManager.CONTENT_LAST_EDITOR_FILTER_KEY, false, "admin", true);
        EntitySearchFilter[] filters2 = {creationOrder, descrFilter, lastEditorFilter};
        contentIds = this._contentManager.searchId(filters2);
        assertNotNull(contentIds);
        assertEquals(expected1.length, contentIds.size());
        this.verifyOrder(contentIds, expected1);

        descrFilter = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "Cont", true, FieldSearchFilter.LikeOptionType.RIGHT);
        EntitySearchFilter[] filters3 = {creationOrder, descrFilter};
        contentIds = this._contentManager.searchId(filters3);
        assertNotNull(contentIds);
        String[] expected3 = expected1;
        assertEquals(expected3.length, contentIds.size());
        this.verifyOrder(contentIds, expected3);

        descrFilter = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "Cont", true, FieldSearchFilter.LikeOptionType.LEFT);
        EntitySearchFilter[] filters4 = {creationOrder, descrFilter};
        contentIds = this._contentManager.searchId(filters4);
        assertNotNull(contentIds);
        assertTrue(contentIds.isEmpty());

        descrFilter = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "1", true, FieldSearchFilter.LikeOptionType.LEFT);
        EntitySearchFilter[] filters5 = {creationOrder, descrFilter};
        contentIds = this._contentManager.searchId(filters5);
        assertNotNull(contentIds);
        String[] expected5 = {"EVN191", "ART120"};
        assertEquals(expected5.length, contentIds.size());
        this.verifyOrder(contentIds, expected5);
    }
    
    @Test
    void testSearchPaginatedContents_1_1() throws Throwable {
        List<String> groupCodes = this._groupManager.getGroups().stream().map(group -> group.getName()).collect(Collectors.toList());
        EntitySearchFilter<String> creationOrder = new EntitySearchFilter(IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false);
        creationOrder.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter<String> descrFilter = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "Cont", true);
        EntitySearchFilter<String> paginationFilter = new EntitySearchFilter<>(3, 0);
        EntitySearchFilter[] filters1 = {creationOrder, descrFilter, paginationFilter};
        SearcherDaoPaginatedResult<String> paginatedContentsId = this._contentManager.getPaginatedWorkContentsId(null, false, filters1, groupCodes);
        assertNotNull(paginatedContentsId);
        String[] totalExpected = {"RAH101", "ART102", "EVN103", "ART104", "ART111", "ART112", "ART120", "ART121", "ART122"};
        assertEquals(totalExpected.length, paginatedContentsId.getCount().intValue());
        assertEquals(3, paginatedContentsId.getList().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(totalExpected[i], paginatedContentsId.getList().get(i));
        }
        
        EntitySearchFilter<String> paginationFilter2 = new EntitySearchFilter<>(5, 2);
        EntitySearchFilter[] filters2 = {creationOrder, descrFilter, paginationFilter2};
        paginatedContentsId = this._contentManager.getPaginatedWorkContentsId(null, false, filters2, groupCodes);
        assertNotNull(paginatedContentsId);
        assertEquals(totalExpected.length, paginatedContentsId.getCount().intValue());
        assertEquals(5, paginatedContentsId.getList().size());
        for (int i = 2; i < (5+2); i++) {
            assertEquals(totalExpected[i], paginatedContentsId.getList().get(i-2));
        }
    }
    
    @Test
    void testSearchContents_1_2() throws Throwable {
        EntitySearchFilter versionFilter = new EntitySearchFilter(IContentManager.CONTENT_CURRENT_VERSION_FILTER_KEY, false, "0.", true);
        EntitySearchFilter[] filters3 = {versionFilter};
        List<String> contentIds = this._contentManager.searchId(filters3);
        assertNotNull(contentIds);
        String[] expected2 = {"ART179"};
        assertEquals(expected2.length, contentIds.size());
        this.verifyOrder(contentIds, expected2);

        versionFilter = new EntitySearchFilter(IContentManager.CONTENT_CURRENT_VERSION_FILTER_KEY, false, ".0", true);
        EntitySearchFilter[] filters4 = {versionFilter};
        contentIds = this._contentManager.searchId(filters4);
        assertNotNull(contentIds);
        assertEquals(22, contentIds.size());
    }

    @Test
    void testSearchContents_1_4() throws Throwable {
        List<String> contentIds = this._contentManager.searchId(null);
        assertNotNull(contentIds);
        assertEquals(25, contentIds.size());

        EntitySearchFilter creationOrder = new EntitySearchFilter(IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false);
        creationOrder.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter descrFilter = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "CoNt", true);
        EntitySearchFilter[] filters1 = {creationOrder, descrFilter};
        contentIds = this._contentManager.searchId(filters1);
        assertNotNull(contentIds);
        String[] expected1 = {"RAH101", "ART102", "EVN103", "ART104", "ART111", "ART112", "ART120", "ART121", "ART122"};
        assertEquals(expected1.length, contentIds.size());
        this.verifyOrder(contentIds, expected1);

        EntitySearchFilter lastEditorFilter = new EntitySearchFilter(IContentManager.CONTENT_LAST_EDITOR_FILTER_KEY, false, "AdMin", true);
        EntitySearchFilter[] filters2 = {creationOrder, descrFilter, lastEditorFilter};
        contentIds = this._contentManager.searchId(filters2);
        assertNotNull(contentIds);
        assertEquals(expected1.length, contentIds.size());
        this.verifyOrder(contentIds, expected1);
    }
    
    @Test
    void testSearchContents_1_5() throws Throwable {
        EntitySearchFilter creationOrder = new EntitySearchFilter(IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false);
        creationOrder.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter descrFilter = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "co", true, FieldSearchFilter.LikeOptionType.COMPLETE);
        EntitySearchFilter[] filters1 = {creationOrder, descrFilter};
        List<String> contentIds = this._contentManager.searchId(filters1);
        assertNotNull(contentIds);
        String[] expected1 = {"ART1", "RAH1", "ART187", "RAH101", "ART102", "EVN103", "ART104", "ART111", "ART112", "EVN23", "ART120", "ART121", "ART122"};
        assertEquals(expected1.length, contentIds.size());
        this.verifyOrder(contentIds, expected1);

        descrFilter = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "co", true, FieldSearchFilter.LikeOptionType.RIGHT);
        EntitySearchFilter[] filters2 = {creationOrder, descrFilter};
        contentIds = this._contentManager.searchId(filters2);
        assertNotNull(contentIds);
        String[] expected2 = {"RAH101", "ART102", "EVN103", "ART104", "ART111", "ART112", "EVN23", "ART120", "ART121", "ART122"};
        assertEquals(expected2.length, contentIds.size());
        this.verifyOrder(contentIds, expected2);

        EntitySearchFilter idFilter = new EntitySearchFilter(IContentManager.ENTITY_ID_FILTER_KEY, false, "1", true, FieldSearchFilter.LikeOptionType.LEFT);
        EntitySearchFilter[] filters3 = {creationOrder, descrFilter, idFilter};
        contentIds = this._contentManager.searchId(filters3);
        assertNotNull(contentIds);
        String[] expected3 = {"RAH101", "ART111", "ART121"};
        assertEquals(expected3.length, contentIds.size());
        this.verifyOrder(contentIds, expected3);

        descrFilter = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "co", true, FieldSearchFilter.LikeOptionType.LEFT);
        EntitySearchFilter[] filters4 = {creationOrder, descrFilter};
        contentIds = this._contentManager.searchId(filters4);
        assertNotNull(contentIds);
        String[] expected4 = {};
        assertEquals(expected4.length, contentIds.size());
    }
    
    @Test
    void testSearchPaginatedContents_1_5() throws Throwable {
        List<String> groupCodes = this._groupManager.getGroups().stream().map(group -> group.getName()).collect(Collectors.toList());
        EntitySearchFilter creationOrder = new EntitySearchFilter(IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false);
        creationOrder.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter descrFilter = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "co", true, FieldSearchFilter.LikeOptionType.COMPLETE);
        EntitySearchFilter<String> paginationFilter = new EntitySearchFilter<>(4, 0);
        EntitySearchFilter[] filters1 = {creationOrder, descrFilter, paginationFilter};
        SearcherDaoPaginatedResult<String> paginatedContentsId1 = this._contentManager.getPaginatedWorkContentsId(null, true, filters1, groupCodes);
        assertNotNull(paginatedContentsId1);
        String[] totalExpected1 = {"ART1", "RAH1", "ART187", "RAH101", "ART102", "EVN103", "ART104", "ART111", "ART112", "EVN23", "ART120", "ART121", "ART122"};
        assertEquals(totalExpected1.length, paginatedContentsId1.getCount().intValue());
        assertEquals(4, paginatedContentsId1.getList().size());
        for (int i = 0; i < 4; i++) {
            assertEquals(totalExpected1[i], paginatedContentsId1.getList().get(i));
        }
        
        descrFilter = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "co", true, FieldSearchFilter.LikeOptionType.RIGHT);
        EntitySearchFilter<String> paginationFilter2 = new EntitySearchFilter<>(6, 3);
        EntitySearchFilter[] filters2 = {creationOrder, descrFilter, paginationFilter2};
        SearcherDaoPaginatedResult<String> paginatedContentsId2 = this._contentManager.getPaginatedWorkContentsId(null, true, filters2, groupCodes);
        assertNotNull(paginatedContentsId2);
        String[] totalExpected2 = {"RAH101", "ART102", "EVN103", "ART104", "ART111", "ART112", "EVN23", "ART120", "ART121", "ART122"};
        assertEquals(totalExpected2.length, paginatedContentsId2.getCount().intValue());
        assertEquals(6, paginatedContentsId2.getList().size());
        for (int i = 3; i < (6+3); i++) {
            assertEquals(totalExpected2[i], paginatedContentsId2.getList().get(i-3));
        }
    }
    
    @Test
    void testSearchContents_1_6() throws Throwable {
        EntitySearchFilter creationOrder = new EntitySearchFilter(IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false);
        creationOrder.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter descrFilter = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "CoNt", true);
        EntitySearchFilter[] filters1 = {creationOrder, descrFilter};
        List<String> contentIds = this._contentManager.searchId(filters1);
        assertEquals(9, contentIds.size());

        EntitySearchFilter lastEditorFilter = new EntitySearchFilter(IContentManager.CONTENT_LAST_EDITOR_FILTER_KEY, false, "AdMin", true);
        EntitySearchFilter[] filters2 = {creationOrder, descrFilter, lastEditorFilter};
        contentIds = this._contentManager.searchId(filters2);
        assertNotNull(contentIds);
        assertEquals(9, contentIds.size());
    }

    @Test
    void testSearchContents_2() throws Throwable {
        EntitySearchFilter creationOrder = new EntitySearchFilter(IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false);
        creationOrder.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter groupFilter = new EntitySearchFilter(IContentManager.CONTENT_MAIN_GROUP_FILTER_KEY, false, "coach", false);
        EntitySearchFilter[] filters = {creationOrder, groupFilter};
        List<String> contentIds = this._contentManager.searchId(filters);
        assertNotNull(contentIds);
        String[] expected = {"EVN103", "ART104", "ART111", "ART112", "EVN25", "EVN41"};
        assertEquals(expected.length, contentIds.size());
        this.verifyOrder(contentIds, expected);
    }

    @Test
    void testSearchContents_3() throws Throwable {
        EntitySearchFilter modifyOrder = new EntitySearchFilter(IContentManager.CONTENT_MODIFY_DATE_FILTER_KEY, false);
        modifyOrder.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter onlineFilter = new EntitySearchFilter(IContentManager.CONTENT_ONLINE_FILTER_KEY, false, "encoding=", true);
        EntitySearchFilter[] filters = {modifyOrder, onlineFilter};
        List<String> contentIds = this._contentManager.searchId(filters);
        assertNotNull(contentIds);
        String[] expected = {"ART187", "ART1", "EVN193", "EVN194", "ART180", "RAH1",
                "EVN191", "EVN192", "RAH101", "EVN103", "ART104", "ART102", "EVN23",
                "EVN24", "EVN25", "EVN41", "EVN20", "EVN21", "ART111", "ART120", "ART121", "ART122", "ART112", "ALL4"};
        this.verifyOrder(contentIds, expected);
    }

    @Test
    void testSearchWorkContents() throws Throwable {
        List<String> contents = this._contentManager.loadWorkContentsId(null, null);
        assertNotNull(contents);
        assertEquals(0, contents.size());

        EntitySearchFilter creationOrder = new EntitySearchFilter(IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false);
        creationOrder.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter typeFilter = new EntitySearchFilter(IContentManager.ENTITY_TYPE_CODE_FILTER_KEY, false, "ART", false);
        EntitySearchFilter[] filters1 = {creationOrder, typeFilter};
        contents = this._contentManager.loadWorkContentsId(filters1, null);
        assertEquals(0, contents.size());

        List<String> groupCodes = new ArrayList<String>();
        groupCodes.add("customers");
        contents = this._contentManager.loadWorkContentsId(filters1, groupCodes);
        String[] order1 = {"ART102"};
        assertEquals(order1.length, contents.size());
        this.verifyOrder(contents, order1);

        groupCodes.add(Group.FREE_GROUP_NAME);
        EntitySearchFilter statusFilter = new EntitySearchFilter(IContentManager.CONTENT_STATUS_FILTER_KEY, false, Content.STATUS_DRAFT, false);
        EntitySearchFilter[] filters2 = {creationOrder, typeFilter, statusFilter};
        contents = this._contentManager.loadWorkContentsId(filters2, groupCodes);
        String[] order2 = {"ART102", "ART187", "ART179", "ART1"};
        assertEquals(order2.length, contents.size());
        this.verifyOrder(contents, order2);

        EntitySearchFilter onlineFilter = new EntitySearchFilter(IContentManager.CONTENT_ONLINE_FILTER_KEY, false);
        EntitySearchFilter[] filters3 = {creationOrder, typeFilter, onlineFilter};
        contents = this._contentManager.loadWorkContentsId(filters3, groupCodes);
        String[] order3 = {"ART102", "ART187", "ART180", "ART1"};
        assertEquals(order3.length, contents.size());
        this.verifyOrder(contents, order3);

        onlineFilter.setNullOption(true);
        contents = this._contentManager.loadWorkContentsId(filters3, groupCodes);
        String[] order4 = {"ART179"};
        assertEquals(order4.length, contents.size());
        this.verifyOrder(contents, order4);

        onlineFilter.setNullOption(false);
        EntitySearchFilter descrFilter = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "scr", true);
        EntitySearchFilter[] filters5 = {creationOrder, typeFilter, onlineFilter, descrFilter};
        contents = this._contentManager.loadWorkContentsId(filters5, groupCodes);
        String[] order5 = {"ART187", "ART180"};
        assertEquals(order5.length, contents.size());
        this.verifyOrder(contents, order5);

        groupCodes.clear();
        groupCodes.add(Group.ADMINS_GROUP_NAME);
        contents = this._contentManager.loadWorkContentsId(null, groupCodes);
        assertNotNull(contents);
        assertEquals(25, contents.size());
    }

    @Test
    void testSearchWorkContents_2_b() throws Throwable {
        List<String> groupCodes = new ArrayList<>();
        groupCodes.add("customers");
        groupCodes.add(Group.FREE_GROUP_NAME);
        EntitySearchFilter creationOrder = new EntitySearchFilter(IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false);
        creationOrder.setOrder(EntitySearchFilter.DESC_ORDER);

        EntitySearchFilter descrFilter_1 = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "eScR", true);
        EntitySearchFilter[] filters_1 = {creationOrder, descrFilter_1};
        List<String> contents = this._contentManager.loadWorkContentsId(filters_1, groupCodes);
        String[] order = {"ALL4", "ART187", "ART180", "ART179"};
        assertEquals(order.length, contents.size());
        this.verifyOrder(contents, order);
    }

    @Test
    void testSearchWorkContents_3() throws Throwable {
        List<String> groupCodes = new ArrayList<>();
        groupCodes.add(Group.ADMINS_GROUP_NAME);
        EntitySearchFilter creationOrder = new EntitySearchFilter(IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false);
        creationOrder.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter[] filters = {creationOrder};
        String[] categories_1 = {"general_cat2"};
        List<String> contents = this._contentManager.loadWorkContentsId(categories_1, filters, groupCodes);
        String[] order_a = {"ART120", "EVN25", "ART112", "ART111", "EVN193", "ART179"};
        assertEquals(order_a.length, contents.size());
        this.verifyOrder(contents, order_a);

        String[] categories_2 = {"general_cat1", "general_cat2"};
        contents = this._contentManager.loadWorkContentsId(categories_2, filters, groupCodes);
        String[] order_b = {"EVN25", "ART111", "ART179"};
        assertEquals(order_b.length, contents.size());
        assertEquals(order_b[0], contents.get(0));

        Content newContent = this._contentManager.loadContent("EVN193", false);
        newContent.setId(null);
        String newId = null;
        try {
            newId = this._contentManager.saveContent(newContent);
            assertNotNull(newId);
            contents = this._contentManager.loadWorkContentsId(categories_1, filters, groupCodes);
            String[] order_c = {newContent.getId(), "ART120", "EVN25", "ART112", "ART111", "EVN193", "ART179"};
            assertEquals(order_c.length, contents.size());
            this.verifyOrder(contents, order_c);

            ICategoryManager categoryManager = (ICategoryManager) this.getService(SystemConstants.CATEGORY_MANAGER);
            newContent.addCategory(categoryManager.getCategory("general_cat1"));
            this._contentManager.saveContent(newContent);
            contents = this._contentManager.loadWorkContentsId(categories_2, filters, groupCodes);
            String[] order_d = {newId, "EVN25", "ART111", "ART179"};
            assertEquals(order_d.length, contents.size());
            this.verifyOrder(contents, order_d);
        } catch (Throwable t) {
            throw t;
        } finally {
            this._contentManager.deleteContent(newId);
            assertNull(this._contentManager.loadContent(newId, false));
        }
    }
    
    private void verifyOrder(List<String> contents, String[] order) {
        for (int i = 0; i < contents.size(); i++) {
            assertEquals(order[i], contents.get(i));
        }
    }
    
    @Test
    void testLoadContent() throws Throwable {
        Content content = this._contentManager.loadContent("ART111", false);
        assertEquals(Content.STATUS_PUBLIC, content.getStatus());
        assertEquals("coach", content.getMainGroup());
        assertEquals(2, content.getGroups().size());
        assertTrue(content.getGroups().contains("customers"));
        assertTrue(content.getGroups().contains("helpdesk"));

        Map<String, AttributeInterface> attributes = content.getAttributeMap();
        assertEquals(7, attributes.size());

        TextAttribute title = (TextAttribute) attributes.get("Titolo");
        assertEquals("Titolo Contenuto 3 Coach", title.getTextForLang("it"));
        assertNull(title.getTextForLang("en"));

        MonoListAttribute authors = (MonoListAttribute) attributes.get("Autori");
        assertEquals(4, authors.getAttributes().size());

        LinkAttribute link = (LinkAttribute) attributes.get("VediAnche");
        assertNull(link.getSymbolicLink());

        HypertextAttribute hypertext = (HypertextAttribute) attributes.get("CorpoTesto");
        assertEquals("<p>Corpo Testo Contenuto 3 Coach</p>", hypertext.getTextForLang("it").trim());
        assertNull(hypertext.getTextForLang("en"));

        ResourceAttributeInterface image = (ResourceAttributeInterface) attributes.get("Foto");
        assertNull(image.getResource());

        DateAttribute date = (DateAttribute) attributes.get("Data");
        assertEquals("13/12/2006", DateConverter.getFormattedDate(date.getDate(), "dd/MM/yyyy"));
    }

    @Test
    void testLoadFullContent() throws Throwable {
        Content content = this._contentManager.loadContent("ALL4", false);
        assertEquals(Content.STATUS_PUBLIC, content.getStatus());
        assertEquals(Group.FREE_GROUP_NAME, content.getMainGroup());
        assertEquals(0, content.getGroups().size());
        Map<String, AttributeInterface> attributes = content.getAttributeMap();
        assertEquals(43, attributes.size());
        AttachAttribute attachAttribute = (AttachAttribute) attributes.get("Attach");
        assertNotNull(attachAttribute);
        assertEquals("7", attachAttribute.getResource("it").getId());
        assertEquals("text Attach", attachAttribute.getTextForLang("it"));
        CheckBoxAttribute checkBoxAttribute = (CheckBoxAttribute) attributes.get("CheckBox");
        assertNotNull(checkBoxAttribute);
        assertNull(checkBoxAttribute.getBooleanValue());
        DateAttribute dateAttribute = (DateAttribute) attributes.get("Date");
        assertNotNull(dateAttribute);
        assertEquals("20100321", DateConverter.getFormattedDate(dateAttribute.getDate(), "yyyyMMdd"));
        DateAttribute dateAttribute2 = (DateAttribute) attributes.get("Date2");
        assertNotNull(dateAttribute2);
        assertEquals("20120321", DateConverter.getFormattedDate(dateAttribute2.getDate(), "yyyyMMdd"));
        EnumeratorAttribute enumeratorAttribute = (EnumeratorAttribute) attributes.get("Enumerator");
        assertNotNull(enumeratorAttribute);
        assertEquals("a", enumeratorAttribute.getText());
        EnumeratorMapAttribute enumeratorMapAttribute = (EnumeratorMapAttribute) attributes.get("EnumeratorMap");
        assertNotNull(enumeratorMapAttribute);
        assertEquals("02", enumeratorMapAttribute.getMapKey());
        assertEquals("Value 2", enumeratorMapAttribute.getMapValue());
        HypertextAttribute hypertextAttribute = (HypertextAttribute) attributes.get("Hypertext");
        assertNotNull(hypertextAttribute);
        assertEquals("<p>text Hypertext</p>", hypertextAttribute.getTextForLang("it"));
        ImageAttribute imageAttribute = (ImageAttribute) attributes.get("Image");
        assertNotNull(imageAttribute);
        assertEquals("44", imageAttribute.getResource("it").getId());
        assertEquals("text image", imageAttribute.getTextForLang("it"));
        TextAttribute longtextAttribute = (TextAttribute) attributes.get("Longtext");
        assertNotNull(longtextAttribute);
        assertEquals("text Longtext", longtextAttribute.getTextForLang("it"));
        MonoTextAttribute monoTextAttribute = (MonoTextAttribute) attributes.get("Monotext");
        assertNotNull(monoTextAttribute);
        assertEquals("text Monotext", monoTextAttribute.getText());
        MonoTextAttribute monoTextAttribute2 = (MonoTextAttribute) attributes.get("Monotext2");
        assertNotNull(monoTextAttribute2);
        assertEquals("aaaa@entando.com", monoTextAttribute2.getText());
        NumberAttribute numberAttribute = (NumberAttribute) attributes.get("Number");
        assertNotNull(numberAttribute);
        assertEquals(25, numberAttribute.getValue().intValue());
        NumberAttribute numberAttribute2 = (NumberAttribute) attributes.get("Number2");
        assertNotNull(numberAttribute2);
        assertEquals(85, numberAttribute2.getValue().intValue());
        TextAttribute textAttribute = (TextAttribute) attributes.get("Text");
        assertNotNull(textAttribute);
        assertEquals("text Text", textAttribute.getTextForLang("it"));
        TextAttribute textAttribute2 = (TextAttribute) attributes.get("Text2");
        assertNotNull(textAttribute2);
        assertEquals("bbbb@entando.com", textAttribute2.getTextForLang("it"));
        ThreeStateAttribute threeStateAttribute = (ThreeStateAttribute) attributes.get("ThreeState");
        assertNotNull(threeStateAttribute);
        assertEquals(Boolean.FALSE, threeStateAttribute.getBooleanValue());
        CompositeAttribute compositeAttribute = (CompositeAttribute) attributes.get("Composite");
        assertNotNull(compositeAttribute);
        assertEquals(13, compositeAttribute.getAttributeMap().size());
        ListAttribute listAttribute1 = (ListAttribute) attributes.get("ListBoolea");
        assertNotNull(listAttribute1);
        assertEquals(2, listAttribute1.getAttributeList("it").size());
        ListAttribute listAttribute2 = (ListAttribute) attributes.get("ListCheck");
        assertNotNull(listAttribute2);
        assertEquals(2, listAttribute2.getAttributeList("it").size());
        ListAttribute listAttribute3 = (ListAttribute) attributes.get("ListDate");
        assertNotNull(listAttribute3);
        assertEquals(2, listAttribute3.getAttributeList("it").size());
        ListAttribute listAttribute4 = (ListAttribute) attributes.get("ListEnum");
        assertNotNull(listAttribute4);
        assertEquals(2, listAttribute4.getAttributeList("it").size());
        ListAttribute listAttribute5 = (ListAttribute) attributes.get("ListMonot");
        assertNotNull(listAttribute5);
        assertEquals(2, listAttribute5.getAttributeList("it").size());
        ListAttribute listAttribute6 = (ListAttribute) attributes.get("ListNumber");
        assertNotNull(listAttribute6);
        assertEquals(2, listAttribute6.getAttributeList("it").size());
        ListAttribute listAttribute7 = (ListAttribute) attributes.get("List3Stat");
        assertNotNull(listAttribute7);
        assertEquals(3, listAttribute7.getAttributeList("it").size());
        MonoListAttribute monoListAttribute1 = (MonoListAttribute) attributes.get("MonoLAtta");
        assertNotNull(monoListAttribute1);
        assertEquals(1, monoListAttribute1.getAttributes().size());
        MonoListAttribute monoListAttribute2 = (MonoListAttribute) attributes.get("MonoLBool");
        assertNotNull(monoListAttribute2);
        assertEquals(2, monoListAttribute2.getAttributes().size());
        MonoListAttribute monoListAttribute3 = (MonoListAttribute) attributes.get("MonoLChec");
        assertNotNull(monoListAttribute3);
        assertEquals(2, monoListAttribute3.getAttributes().size());
        MonoListAttribute monoListAttribute4 = (MonoListAttribute) attributes.get("MonoLCom");
        assertNotNull(monoListAttribute4);
        assertEquals(1, monoListAttribute4.getAttributes().size());
        MonoListAttribute monoListAttribute5 = (MonoListAttribute) attributes.get("MonoLCom2");
        assertNotNull(monoListAttribute5);
        assertEquals(2, monoListAttribute5.getAttributes().size());
        MonoListAttribute monoListAttribute6 = (MonoListAttribute) attributes.get("MonoLDate");
        assertNotNull(monoListAttribute6);
        assertEquals(2, monoListAttribute6.getAttributes().size());
        MonoListAttribute monoListAttribute7 = (MonoListAttribute) attributes.get("MonoLEnum");
        assertNotNull(monoListAttribute7);
        assertEquals(2, monoListAttribute7.getAttributes().size());
        MonoListAttribute monoListAttribute8 = (MonoListAttribute) attributes.get("MonoLHyper");
        assertNotNull(monoListAttribute8);
        assertEquals(2, monoListAttribute8.getAttributes().size());
        MonoListAttribute monoListAttribute9 = (MonoListAttribute) attributes.get("MonoLImage");
        assertNotNull(monoListAttribute9);
        assertEquals(1, monoListAttribute9.getAttributes().size());
        MonoListAttribute monoListAttribute10 = (MonoListAttribute) attributes.get("MonoLLink");
        assertNotNull(monoListAttribute10);
        assertEquals(2, monoListAttribute10.getAttributes().size());
        MonoListAttribute monoListAttribute11 = (MonoListAttribute) attributes.get("MonoLLong");
        assertNotNull(monoListAttribute11);
        assertEquals(1, monoListAttribute11.getAttributes().size());
        MonoListAttribute monoListAttribute12 = (MonoListAttribute) attributes.get("MonoLMonot");
        assertNotNull(monoListAttribute12);
        assertEquals(2, monoListAttribute12.getAttributes().size());
        MonoListAttribute monoListAttribute13 = (MonoListAttribute) attributes.get("MonoLNumb");
        assertNotNull(monoListAttribute13);
        assertEquals(2, monoListAttribute13.getAttributes().size());
        MonoListAttribute monoListAttribute14 = (MonoListAttribute) attributes.get("MonoLText");
        assertNotNull(monoListAttribute14);
        assertEquals(2, monoListAttribute14.getAttributes().size());
        MonoListAttribute monoListAttribute15 = (MonoListAttribute) attributes.get("MonoL3stat");
        assertNotNull(monoListAttribute15);
        assertEquals(3, monoListAttribute15.getAttributes().size());
        EnumeratorMapAttribute enumeratorMapAttribute2 = (EnumeratorMapAttribute) attributes.get("EnumeratorMapBis");
        assertNotNull(enumeratorMapAttribute2);
        assertEquals("01", enumeratorMapAttribute2.getMapKey());
        assertEquals("Value 1 Bis", enumeratorMapAttribute2.getMapValue());
    }

    @Test
    void testGetContentTypes() {
        Map<String, SmallContentType> smallContentTypes = _contentManager.getSmallContentTypesMap();
        assertEquals(4, smallContentTypes.size());
    }

    @Test
    void testCreateContent() {
        Content contentType = _contentManager.createContentType("ART");
        assertNotNull(contentType);
    }

    @Test
    void testCreateContentWithViewPage() {
        Content content = _contentManager.createContentType("ART");
        String viewPage = content.getViewPage();
        assertEquals(viewPage, "contentview");
    }

    @Test
    void testCreateContentWithDefaultModel() {
        Content content = _contentManager.createContentType("ART");
        String defaultModel = content.getDefaultModel();
        assertEquals(defaultModel, "1");
    }

    @Test
    void testGetXML() throws Throwable {
        Content content = this._contentManager.createContentType("ART");
        content.setId("ART1");
        content.setTypeCode("Articolo");
        content.setTypeDescription("Articolo");
        content.setDescription("descrizione");
        content.setStatus(Content.STATUS_DRAFT);
        content.setMainGroup("free");
        Category cat13 = new Category();
        cat13.setCode("13");
        content.addCategory(cat13);
        Category cat19 = new Category();
        cat19.setCode("19");
        content.addCategory(cat19);
        String xml = content.getXML();
        assertNotNull(xml);
        assertTrue(xml.indexOf("<content id=\"ART1\" typecode=\"Articolo\" typedescr=\"Articolo\">") != -1);
        assertTrue(xml.indexOf("<descr>descrizione</descr>") != -1);
        assertTrue(xml.indexOf("<status>" + Content.STATUS_DRAFT + "</status>") != -1);
        assertTrue(xml.indexOf("<category id=\"13\" />") != -1);
        assertTrue(xml.indexOf("<category id=\"19\" />") != -1);
    }

    @Test
    void testLoadPublicContents() throws EntException {
        List<String> contents = _contentManager.loadPublicContentsId(null, null, freeGroup);
        assertEquals(15, contents.size());
    }
    
    @Test
    void testLoadPublicEvents_1() throws EntException {
        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, null, freeGroup);
        String[] expectedFreeContentsId = {"EVN194", "EVN193",
            "EVN24", "EVN23", "EVN25", "EVN20", "EVN21", "EVN192", "EVN191"};
        assertEquals(expectedFreeContentsId.length, contents.size());
        for (int i = 0; i < expectedFreeContentsId.length; i++) {
            assertTrue(contents.contains(expectedFreeContentsId[i]));
        }
        assertFalse(contents.contains("EVN103"));

        List<String> groups = new ArrayList<>();
        groups.add("coach");
        groups.add("free");
        contents = _contentManager.loadPublicContentsId("EVN", null, null, groups);
        assertEquals(expectedFreeContentsId.length + 2, contents.size());
        for (int i = 0; i < expectedFreeContentsId.length; i++) {
            assertTrue(contents.contains(expectedFreeContentsId[i]));
        }
        assertTrue(contents.contains("EVN103"));
        assertTrue(contents.contains("EVN41"));
    }
    
    @Test
    void testLoadPaginatedPublicEvents_1() throws EntException {
        List<String> groupCodes = new ArrayList<>();
        groupCodes.add(Group.FREE_GROUP_NAME);
        EntitySearchFilter filterId = new EntitySearchFilter(IContentManager.ENTITY_ID_FILTER_KEY, false);
        filterId.setOrder(FieldSearchFilter.Order.ASC);
        EntitySearchFilter filterType = new EntitySearchFilter(IContentManager.ENTITY_TYPE_CODE_FILTER_KEY, false, "EVN", true);
        EntitySearchFilter filterLimit = new EntitySearchFilter(5, 3);
        EntitySearchFilter[] filters = {filterId, filterType, filterLimit};
        
        SearcherDaoPaginatedResult<String> result = _contentManager.getPaginatedPublicContentsId(null, true, filters, groupCodes);
        String[] expectedFreeContentsId = {/*"EVN191", "EVN192", "EVN193", */"EVN194",
            "EVN20", "EVN21", "EVN23", "EVN24"/*, "EVN25"*/};
        assertEquals(9, result.getCount().intValue());
        assertEquals(expectedFreeContentsId.length, result.getList().size());
        for (int i = 0; i < expectedFreeContentsId.length; i++) {
            assertEquals(result.getList().get(i), expectedFreeContentsId[i]);
        }
    }
    
    @Test
    void testLoadPublicEvents_2() throws EntException {
        List<String> groups = new ArrayList<>();
        groups.add("coach");
        groups.add(Group.ADMINS_GROUP_NAME);
        Date start = DateConverter.parseDate("1997-06-10", "yyyy-MM-dd");
        Date end = DateConverter.parseDate("2020-09-19", "yyyy-MM-dd");
        EntitySearchFilter filter = new EntitySearchFilter("DataInizio", true, start, end);
        EntitySearchFilter filter2 = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "Even", true);
        filter2.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter[] filters = {filter, filter2};

        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters, groups);
        assertEquals(2, contents.size());
        assertEquals("EVN193", contents.get(0));
        assertEquals("EVN192", contents.get(1));

        filter2 = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false);
        filter2.setOrder(EntitySearchFilter.DESC_ORDER);

        EntitySearchFilter[] filters2 = {filter, filter2};
        contents = _contentManager.loadPublicContentsId("EVN", null, filters2, groups);

        String[] expectedOrderedContentsId = {"EVN25", "EVN21", "EVN20", "EVN41", "EVN193",
            "EVN192", "EVN103", "EVN23", "EVN24"};
        assertEquals(expectedOrderedContentsId.length, contents.size());
        for (int i = 0; i < expectedOrderedContentsId.length; i++) {
            assertEquals(expectedOrderedContentsId[i], contents.get(i));
        }

        contents = _contentManager.loadPublicContentsId("EVN", null, filters2, freeGroup);
        String[] expectedFreeOrderedContentsId = {"EVN25", "EVN21", "EVN20", "EVN193",
            "EVN192", "EVN23", "EVN24"};
        assertEquals(expectedFreeOrderedContentsId.length, contents.size());
        for (int i = 0; i < expectedFreeOrderedContentsId.length; i++) {
            assertEquals(expectedFreeOrderedContentsId[i], contents.get(i));
        }
    }

    @Test
    void testLoadPublicEvents_2_1() throws EntException {
        List<String> groups = new ArrayList<>();
        groups.add("coach");
        groups.add(Group.ADMINS_GROUP_NAME);
        Date start = DateConverter.parseDate("1997-06-10", "yyyy-MM-dd");
        Date end = DateConverter.parseDate("2020-09-19", "yyyy-MM-dd");
        EntitySearchFilter filter = new EntitySearchFilter("DataInizio", true, start, end);
        EntitySearchFilter filter2 = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "even", true);
        filter2.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter[] filters = {filter, filter2};

        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters, groups);
        assertEquals(2, contents.size());
        assertEquals("EVN193", contents.get(0));
        assertEquals("EVN192", contents.get(1));
    }

    @Test
    void testLoadPublicEvents_2_2() throws EntException {
        List<String> groups = new ArrayList<>();
        groups.add("coach");
        groups.add(Group.ADMINS_GROUP_NAME);
        Date start = DateConverter.parseDate("1997-06-10", "yyyy-MM-dd");
        Date end = DateConverter.parseDate("2020-09-19", "yyyy-MM-dd");
        EntitySearchFilter filter = new EntitySearchFilter("DataInizio", true, start, end);
        EntitySearchFilter filter_x1 = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "even", true);
        filter_x1.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter[] filters_1 = {filter, filter_x1};

        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters_1, groups);
        assertEquals(2, contents.size());

        EntitySearchFilter filter_x2 = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, "Even", true);
        filter_x2.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter[] filters_2 = {filter, filter_x2};

        contents = _contentManager.loadPublicContentsId("EVN", null, filters_2, groups);
        assertEquals(2, contents.size());
        assertEquals("EVN193", contents.get(0));
        assertEquals("EVN192", contents.get(1));
    }

    @Test
    void testLoadPublicEvents_3() throws EntException {
        List<String> groups = new ArrayList<>();
        groups.add(Group.ADMINS_GROUP_NAME);
        Date value = DateConverter.parseDate("1999-04-14", "yyyy-MM-dd");
        EntitySearchFilter filter = new EntitySearchFilter("DataInizio", true, value, false);
        EntitySearchFilter[] filters = {filter};

        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters, groups);
        assertEquals(1, contents.size());
        assertEquals("EVN192", contents.get(0));
    }

    @Test
    void testLoadPublicEvents_4() throws EntException {
        this.testLoadPublicEvents_4(true);
        this.testLoadPublicEvents_4(false);
    }

    protected void testLoadPublicEvents_4(boolean useRoleFilter) throws EntException {
        List<String> groups = new ArrayList<>();
        groups.add(Group.ADMINS_GROUP_NAME);
        EntitySearchFilter filter1 = (useRoleFilter)
                ? EntitySearchFilter.createRoleFilter(JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE, "Ce", "TF")
                : new EntitySearchFilter("Titolo", true, "Ce", "TF");
        filter1.setLangCode("it");
        filter1.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter[] filters1 = {filter1};
        List<String> contents = this._contentManager.loadPublicContentsId("EVN", null, filters1, groups);
        String[] expectedOrderedContentsId = {"EVN25", "EVN41", "EVN20", "EVN21", "EVN23"};
        assertEquals(expectedOrderedContentsId.length, contents.size());
        for (int i = 0; i < expectedOrderedContentsId.length; i++) {
            assertEquals(expectedOrderedContentsId[i], contents.get(i));
        }
        filter1 = new EntitySearchFilter("Titolo", true, null, "TF");
        filter1.setLangCode("it");
        filter1.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter[] filters2 = {filter1};
        contents = this._contentManager.loadPublicContentsId("EVN", null, filters2, groups);
        String[] expectedOrderedContentsId2 = {"EVN25", "EVN41", "EVN20", "EVN21", "EVN23", "EVN24"};
        assertEquals(expectedOrderedContentsId2.length, contents.size());
        for (int i = 0; i < expectedOrderedContentsId2.length; i++) {
            assertEquals(expectedOrderedContentsId2[i], contents.get(i));
        }
    }

    @Test
    void testLoadPublicEvents_5() throws EntException {
        List<String> groups = new ArrayList<>();
        groups.add(Group.ADMINS_GROUP_NAME);
        List<Date> allowedDates = new ArrayList<>();
        allowedDates.add(DateConverter.parseDate("1999-04-14", "yyyy-MM-dd"));//EVN192
        allowedDates.add(DateConverter.parseDate("2008-02-13", "yyyy-MM-dd"));//EVN23
        EntitySearchFilter filter = new EntitySearchFilter("DataInizio", true, allowedDates, false);
        filter.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter[] filters = {filter};
        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters, groups);
        String[] expectedOrderedContentsId2 = {"EVN23", "EVN192"};
        assertEquals(expectedOrderedContentsId2.length, contents.size());
        for (int i = 0; i < expectedOrderedContentsId2.length; i++) {
            assertEquals(expectedOrderedContentsId2[i], contents.get(i));
        }
    }

    @Test
    void testLoadPublicEvents_6() throws EntException {
        this.testLoadPublicEvents_6(true);
        this.testLoadPublicEvents_6(false);
    }
    
    protected void testLoadPublicEvents_6(boolean useRoleFilter) throws EntException {
        List<String> groups = new ArrayList<>();
        groups.add(Group.ADMINS_GROUP_NAME);
        List<String> allowedDescription = new ArrayList<>();
        allowedDescription.add("Mostra");//EVN21, EVN20
        allowedDescription.add("Collezione");//EVN23
        EntitySearchFilter filter = (useRoleFilter)
                ? EntitySearchFilter.createRoleFilter(JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE, allowedDescription, true)
                : new EntitySearchFilter("Titolo", true, allowedDescription, true);
        filter.setLangCode("it");
        filter.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter[] filters = {filter};
        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters, groups);
        String[] expectedOrderedContentsId2 = {"EVN20", "EVN21", "EVN23"};
        assertEquals(expectedOrderedContentsId2.length, contents.size());
        for (int i = 0; i < expectedOrderedContentsId2.length; i++) {
            assertEquals(expectedOrderedContentsId2[i], contents.get(i));
        }
    }

    @Test
    void testLoadPublicEvents_7() throws EntException {
        this.testLoadPublicEvents_7(true);
        this.testLoadPublicEvents_7(false);
    }

    protected void testLoadPublicEvents_7(boolean useRoleFilter) throws EntException {
        List<String> groups = new ArrayList<>();
        groups.add(Group.ADMINS_GROUP_NAME);
        List<String> allowedDescription = new ArrayList<>();
        allowedDescription.add("Zootechnical exposure");//EVN20
        allowedDescription.add("Title B - Event 2");//EVN192
        EntitySearchFilter filter1 = (useRoleFilter)
                ? EntitySearchFilter.createRoleFilter(JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE, allowedDescription, false)
                : new EntitySearchFilter("Titolo", true, allowedDescription, false);
        filter1.setLangCode("en");
        EntitySearchFilter filter2 = new EntitySearchFilter("DataInizio", true);
        filter2.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter[] filters = {filter1, filter2};
        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters, groups);
        String[] expectedOrderedContentsId2 = {"EVN192", "EVN20"};
        assertEquals(expectedOrderedContentsId2.length, contents.size());
        for (int i = 0; i < expectedOrderedContentsId2.length; i++) {
            assertEquals(expectedOrderedContentsId2[i], contents.get(i));
        }
    }

    @Test
    void testLoadPublicEvents_8() throws EntException {
        this.testLoadPublicEvents_8(true);
        this.testLoadPublicEvents_8(false);
    }

    protected void testLoadPublicEvents_8(boolean useRoleFilter) throws EntException {
        List<String> groups = new ArrayList<>();
        groups.add(Group.ADMINS_GROUP_NAME);
        List<String> allowedDescription = new ArrayList<>();
        allowedDescription.add("Castello");//EVN24
        allowedDescription.add("dei bambini");//EVN24
        EntitySearchFilter filter = (useRoleFilter)
                ? EntitySearchFilter.createRoleFilter(JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE, allowedDescription, true)
                : new EntitySearchFilter("Titolo", true, allowedDescription, true);
        filter.setLangCode("it");
        filter.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter[] filters = {filter};
        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters, groups);
        String[] expectedOrderedContentsId2 = {"EVN24"};
        assertEquals(expectedOrderedContentsId2.length, contents.size());
        for (int i = 0; i < expectedOrderedContentsId2.length; i++) {
            assertEquals(expectedOrderedContentsId2[i], contents.get(i));
        }
    }

    @Test
    void testLoadPublicEvents_9_b() throws EntException {
        this.testLoadPublicEvents_9_b(true);
        this.testLoadPublicEvents_9_b(false);
    }

    protected void testLoadPublicEvents_9_b(boolean useRoleFilter) throws EntException {
        List<String> groups = new ArrayList<>();
        groups.add(Group.ADMINS_GROUP_NAME);
        EntitySearchFilter filter = (useRoleFilter)
                ? EntitySearchFilter.createRoleFilter(JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE, "le", true)
                : new EntitySearchFilter("Titolo", true, "le", true);
        filter.setLangCode("it");
        filter.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter[] filters = {filter};
        List<String> contents = this._contentManager.loadPublicContentsId("EVN", null, filters, groups);
        String[] expectedOrderedContentsId2 = {"EVN25", "EVN21", "EVN23"};
        assertEquals(expectedOrderedContentsId2.length, contents.size());
        for (int i = 0; i < expectedOrderedContentsId2.length; i++) {
            assertEquals(expectedOrderedContentsId2[i], contents.get(i));
        }
    }

    @Test
    void testLoadPublicEvents_9_c() throws EntException {
        this.testLoadPublicEvents_9_c(true);
        this.testLoadPublicEvents_9_c(false);
    }

    protected void testLoadPublicEvents_9_c(boolean useRoleFilter) throws EntException {
        List<String> groups = new ArrayList<>();
        groups.add(Group.ADMINS_GROUP_NAME);
        EntitySearchFilter filter = (useRoleFilter)
                ? EntitySearchFilter.createRoleFilter(JacmsSystemConstants.ATTRIBUTE_ROLE_TITLE, "LE", true)
                : new EntitySearchFilter("Titolo", true, "LE", true);
        filter.setLangCode("it");
        filter.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter[] filters = {filter};
        List<String> contents = this._contentManager.loadPublicContentsId("EVN", null, filters, groups);
        String[] expectedOrderedContentsId2 = {"EVN25", "EVN21", "EVN23"};
        assertEquals(expectedOrderedContentsId2.length, contents.size());
        for (int i = 0; i < expectedOrderedContentsId2.length; i++) {
            assertEquals(expectedOrderedContentsId2[i], contents.get(i));
        }
    }

    @Test
    void testLoadWorkEvents_1_b() throws EntException {
        List<String> groups = new ArrayList<>();
        groups.add(Group.ADMINS_GROUP_NAME);
        List<String> allowedDescription = new ArrayList<>();
        allowedDescription.add("descrizione");//"ART179" "ART180" "ART187"
        allowedDescription.add("on line");//"ART179"
        allowedDescription.add("customers");//"ART102" "RAH101"
        EntitySearchFilter filter = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false, allowedDescription, true);
        EntitySearchFilter filter2 = new EntitySearchFilter(IContentManager.ENTITY_ID_FILTER_KEY, false);
        filter2.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter[] filters = {filter, filter2};
        List<String> contents = _contentManager.loadWorkContentsId(filters, groups);
        String[] expectedOrderedContentsId2 = {"ART102", "ART179", "ART180", "ART187", "RAH101"};
        assertEquals(expectedOrderedContentsId2.length, contents.size());
        for (int i = 0; i < expectedOrderedContentsId2.length; i++) {
            assertEquals(expectedOrderedContentsId2[i], contents.get(i));
        }
    }

    @Test
    void testLoadOrderedPublicEvents_1() throws EntException {
        EntitySearchFilter filterForDescr = new EntitySearchFilter(IContentManager.CONTENT_DESCR_FILTER_KEY, false);
        filterForDescr.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter[] filters = {filterForDescr};
        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters, freeGroup);

        String[] expectedFreeContentsId = {"EVN24", "EVN23", "EVN191",
            "EVN192", "EVN193", "EVN194", "EVN20", "EVN21", "EVN25"};
        assertEquals(expectedFreeContentsId.length, contents.size());
        for (int i = 0; i < expectedFreeContentsId.length; i++) {
            assertEquals(expectedFreeContentsId[i], contents.get(i));
        }

        filterForDescr.setOrder(EntitySearchFilter.DESC_ORDER);
        contents = _contentManager.loadPublicContentsId("EVN", null, filters, freeGroup);

        assertEquals(expectedFreeContentsId.length, contents.size());
        for (int i = 0; i < expectedFreeContentsId.length; i++) {
            assertEquals(expectedFreeContentsId[expectedFreeContentsId.length - i - 1], contents.get(i));
        }
    }

    @Test
    void testLoadOrderedPublicEvents_2() throws EntException {
        EntitySearchFilter filterForCreation = new EntitySearchFilter(IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false);
        filterForCreation.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter[] filters = {filterForCreation};

        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters, freeGroup);
        String[] expectedFreeOrderedContentsId = {"EVN191", "EVN192", "EVN193", "EVN194",
            "EVN20", "EVN23", "EVN24", "EVN25", "EVN21"};
        assertEquals(expectedFreeOrderedContentsId.length, contents.size());
        for (int i = 0; i < expectedFreeOrderedContentsId.length; i++) {
            assertEquals(expectedFreeOrderedContentsId[i], contents.get(i));
        }

        filterForCreation.setOrder(EntitySearchFilter.DESC_ORDER);
        contents = _contentManager.loadPublicContentsId("EVN", null, filters, freeGroup);
        assertEquals(expectedFreeOrderedContentsId.length, contents.size());
        for (int i = 0; i < expectedFreeOrderedContentsId.length; i++) {
            assertEquals(expectedFreeOrderedContentsId[expectedFreeOrderedContentsId.length - i - 1], contents.get(i));
        }
    }

    @Test
    void testLoadOrderedPublicEvents_3() throws EntException {
        EntitySearchFilter filterForCreation = new EntitySearchFilter(IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false);
        filterForCreation.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter filterForDate = new EntitySearchFilter("DataInizio", true);
        filterForDate.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter[] filters = {filterForCreation, filterForDate};

        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters, freeGroup);
        String[] expectedFreeOrderedContentsId = {"EVN21", "EVN25", "EVN24", "EVN23",
            "EVN20", "EVN194", "EVN193", "EVN192", "EVN191"};
        assertEquals(expectedFreeOrderedContentsId.length, contents.size());
        for (int i = 0; i < expectedFreeOrderedContentsId.length; i++) {
            assertEquals(expectedFreeOrderedContentsId[i], contents.get(i));
        }

        EntitySearchFilter[] filters2 = {filterForDate, filterForCreation};

        List<String> contents2 = _contentManager.loadPublicContentsId("EVN", null, filters2, freeGroup);
        String[] expectedFreeOrderedContentsId2 = {"EVN194", "EVN193", "EVN24",
            "EVN23", "EVN25", "EVN20", "EVN21", "EVN192", "EVN191"};
        assertEquals(expectedFreeOrderedContentsId2.length, contents2.size());
        for (int i = 0; i < expectedFreeOrderedContentsId2.length; i++) {
            assertEquals(expectedFreeOrderedContentsId2[i], contents2.get(i));
        }
    }

    @Test
    void testLoadOrderedPublicEvents_4() throws Throwable {
        Content masterContent = this._contentManager.loadContent("EVN193", true);
        masterContent.setId(null);
        DateAttribute dateAttribute = (DateAttribute) masterContent.getAttribute("DataInizio");
        dateAttribute.setDate(DateConverter.parseDate("17/06/2019", "dd/MM/yyyy"));
        String newId = null;
        try {
            newId = this._contentManager.saveContent(masterContent);
            assertNotNull(newId);
            String checkId = this._contentManager.insertOnLineContent(masterContent);
            assertEquals(newId, checkId);
            this.waitNotifyingThread();

            EntitySearchFilter filterForDate = new EntitySearchFilter("DataInizio", true);
            filterForDate.setOrder(EntitySearchFilter.DESC_ORDER);
            EntitySearchFilter[] filters = {filterForDate};

            List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters, freeGroup);
            String[] expectedFreeOrderedContentsId = {"EVN194", newId, "EVN193", "EVN24",
                "EVN23", "EVN25", "EVN20", "EVN21", "EVN192", "EVN191"};
            assertEquals(expectedFreeOrderedContentsId.length, contents.size());
            for (int i = 0; i < expectedFreeOrderedContentsId.length; i++) {
                assertEquals(expectedFreeOrderedContentsId[i], contents.get(i));
            }
        } catch (Throwable t) {
            throw t;
        } finally {
            if (null != newId && !"EVN193".equals(newId)) {
                Content content = this._contentManager.loadContent(newId, false);
                this._contentManager.removeOnLineContent(content);
                this._contentManager.deleteContent(newId);
            }
        }
    }

    @Test
    void testLoadFutureEvents_1() throws EntException {
        Date today = DateConverter.parseDate("2005-01-01", "yyyy-MM-dd");
        EntitySearchFilter filter = new EntitySearchFilter("DataInizio", true, today, null);
        filter.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter[] filters = {filter};
        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters, freeGroup);
        String[] expectedOrderedContentsId = {"EVN21", "EVN20", "EVN25", "EVN23",
            "EVN24", "EVN193", "EVN194"};
        assertEquals(expectedOrderedContentsId.length, contents.size());
        for (int i = 0; i < expectedOrderedContentsId.length; i++) {
            assertEquals(expectedOrderedContentsId[i], contents.get(i));
        }
    }

    @Test
    void testLoadFutureEvents_2() throws EntException {
        Date today = DateConverter.parseDate("2005-01-01", "yyyy-MM-dd");
        EntitySearchFilter filter = new EntitySearchFilter("DataInizio", true, today, null);
        filter.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter[] filters = {filter};
        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters, freeGroup);
        String[] expectedOrderedContentsId = {"EVN194", "EVN193", "EVN24",
            "EVN23", "EVN25", "EVN20", "EVN21"};
        assertEquals(expectedOrderedContentsId.length, contents.size());
        for (int i = 0; i < expectedOrderedContentsId.length; i++) {
            assertEquals(expectedOrderedContentsId[i], contents.get(i));
        }
    }

    @Test
    void testLoadFutureEvents_3() throws EntException {
        Date today = DateConverter.parseDate("2005-01-01", "yyyy-MM-dd");
        List<String> groups = new ArrayList<>();
        groups.add("coach");
        groups.add("free");
        EntitySearchFilter filter = new EntitySearchFilter("DataInizio", true, today, null);
        filter.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter[] filters = {filter};
        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters, groups);
        String[] expectedOrderedContentsId = {"EVN194", "EVN193", "EVN24",
            "EVN23", "EVN41", "EVN25", "EVN20", "EVN21"};
        assertEquals(expectedOrderedContentsId.length, contents.size());
        for (int i = 0; i < expectedOrderedContentsId.length; i++) {
            assertEquals(expectedOrderedContentsId[i], contents.get(i));
        }
    }

    @Test
    void testLoadPastEvents_1() throws EntException {
        Date today = DateConverter.parseDate("2008-10-01", "yyyy-MM-dd");

        EntitySearchFilter filter = new EntitySearchFilter("DataInizio", true, null, today);
        filter.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter[] filters = {filter};

        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters, freeGroup);
        String[] expectedOrderedContentsId = {"EVN191", "EVN192",
            "EVN21", "EVN20", "EVN25", "EVN23"};
        assertEquals(expectedOrderedContentsId.length, contents.size());
        for (int i = 0; i < expectedOrderedContentsId.length; i++) {
            assertEquals(expectedOrderedContentsId[i], contents.get(i));
        }
    }

    @Test
    void testLoadPastEvents_2() throws EntException {
        Date today = DateConverter.parseDate("2008-10-01", "yyyy-MM-dd");

        EntitySearchFilter filter = new EntitySearchFilter("DataInizio", true, null, today);
        filter.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter[] filters = {filter};

        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters, freeGroup);
        String[] expectedOrderedContentsId = {"EVN23", "EVN25",
            "EVN20", "EVN21", "EVN192", "EVN191"};
        assertEquals(expectedOrderedContentsId.length, contents.size());
        for (int i = 0; i < expectedOrderedContentsId.length; i++) {
            assertEquals(expectedOrderedContentsId[i], contents.get(i));
        }
    }

    @Test
    void testLoadPastEvents_3() throws EntException {
        Date today = DateConverter.parseDate("2008-02-13", "yyyy-MM-dd");
        EntitySearchFilter filter = new EntitySearchFilter("DataInizio", true, null, today);
        filter.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter[] filters = {filter};

        List<String> groups = new ArrayList<>();
        groups.add("coach");
        groups.add("free");
        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, filters, groups);
        String[] expectedOrderedContentsId = {"EVN191", "EVN192", "EVN103",
            "EVN21", "EVN20", "EVN25", "EVN41", "EVN23"};
        assertEquals(expectedOrderedContentsId.length, contents.size());
        for (int i = 0; i < expectedOrderedContentsId.length; i++) {
            assertEquals(expectedOrderedContentsId[i], contents.get(i));
        }
    }

    @Test
    void testLoadWorkContentsForCategory_1() throws EntException {
        List<String> groups = new ArrayList<>();
        groups.add(Group.ADMINS_GROUP_NAME);
        String[] categories1 = {"general_cat1"};
        List<String> contents = this._contentManager.loadWorkContentsId(categories1, null, groups);
        assertEquals(7, contents.size());
        assertTrue(contents.contains("ART179"));
        assertTrue(contents.contains("ART180"));
        assertTrue(contents.contains("ART102"));
        assertTrue(contents.contains("ART111"));
        assertTrue(contents.contains("EVN192"));
        assertTrue(contents.contains("EVN23"));
        assertTrue(contents.contains("EVN25"));

        String[] categories2 = {"general_cat1", "general_cat2"};

        contents = this._contentManager.loadWorkContentsId(categories2, null, groups);
        assertEquals(3, contents.size());
        assertTrue(contents.contains("ART111"));
        assertTrue(contents.contains("ART179"));
        assertTrue(contents.contains("EVN25"));
    }

    @Test
    void testLoadWorkContentsForCategory_2() throws EntException {
        List<String> groups = new ArrayList<>();
        groups.add(Group.ADMINS_GROUP_NAME);
        String[] categories1 = {"general_cat1"};
        EntitySearchFilter filter1 = new EntitySearchFilter(IContentManager.ENTITY_TYPE_CODE_FILTER_KEY, false, "ART", false);
        EntitySearchFilter[] filters = {filter1};
        List<String> contents = this._contentManager.loadWorkContentsId(categories1, filters, groups);
        assertEquals(4, contents.size());
        assertTrue(contents.contains("ART102"));
        assertTrue(contents.contains("ART111"));
        assertTrue(contents.contains("ART180"));
        assertTrue(contents.contains("ART179"));

        String[] categories2 = {"general_cat2"};
        contents = this._contentManager.loadWorkContentsId(categories2, filters, groups);
        assertEquals(4, contents.size());
        assertTrue(contents.contains("ART111"));
        assertTrue(contents.contains("ART112"));
        assertTrue(contents.contains("ART120"));
        assertTrue(contents.contains("ART179"));

        String[] categories12 = {"general_cat1", "general_cat2"};
        contents = this._contentManager.loadWorkContentsId(categories12, false, filters, groups);
        assertEquals(2, contents.size());
        assertTrue(contents.contains("ART111"));
        assertTrue(contents.contains("ART179"));
        contents = this._contentManager.loadWorkContentsId(categories12, true, filters, groups);
        assertEquals(6, contents.size());
        assertTrue(contents.contains("ART102"));
        assertTrue(contents.contains("ART111"));
        assertTrue(contents.contains("ART112"));
        assertTrue(contents.contains("ART120"));
        assertTrue(contents.contains("ART180"));
        assertTrue(contents.contains("ART179"));

        String[] categories3 = {"general_cat3"};
        contents = this._contentManager.loadWorkContentsId(categories3, filters, groups);
        assertEquals(3, contents.size());
        assertTrue(contents.contains("ART120"));
        assertTrue(contents.contains("ART121"));
        assertTrue(contents.contains("ART122"));

        String[] categories23 = {"general_cat2", "general_cat3"};
        contents = this._contentManager.loadWorkContentsId(categories23, false, filters, groups);
        assertEquals(1, contents.size());
        assertTrue(contents.contains("ART120"));
        contents = this._contentManager.loadWorkContentsId(categories23, true, filters, groups);
        assertEquals(6, contents.size());
        assertTrue(contents.contains("ART111"));
        assertTrue(contents.contains("ART112"));
        assertTrue(contents.contains("ART120"));
        assertTrue(contents.contains("ART179"));
        assertTrue(contents.contains("ART121"));
        assertTrue(contents.contains("ART122"));

        String[] categories123 = {"general_cat1", "general_cat2", "general_cat3"};
        contents = this._contentManager.loadWorkContentsId(categories123, false, filters, groups);
        assertEquals(0, contents.size());
        contents = this._contentManager.loadWorkContentsId(categories123, true, filters, groups);
        assertEquals(8, contents.size());
        assertTrue(contents.contains("ART102"));
        assertTrue(contents.contains("ART111"));
        assertTrue(contents.contains("ART112"));
        assertTrue(contents.contains("ART120"));
        assertTrue(contents.contains("ART121"));
        assertTrue(contents.contains("ART122"));
        assertTrue(contents.contains("ART180"));
        assertTrue(contents.contains("ART179"));
    }

    @Test
    void testLoadPublicContentsForCategory() throws EntException {
        String[] categories1 = {"evento"};
        List<String> contents = _contentManager.loadPublicContentsId(categories1, null, freeGroup);
        assertEquals(2, contents.size());
        assertTrue(contents.contains("EVN192"));
        assertTrue(contents.contains("EVN193"));

        String[] categories2 = {"cat1"};
        contents = _contentManager.loadPublicContentsId(categories2, null, freeGroup);
        assertEquals(1, contents.size());
        assertTrue(contents.contains("ART180"));
    }

    @Test
    void testLoadPublicEventsForCategory_1() throws EntException {
        String[] categories = {"evento"};
        List<String> contents = _contentManager.loadPublicContentsId("EVN", categories, null, freeGroup);
        assertEquals(2, contents.size());
        assertTrue(contents.contains("EVN192"));
        assertTrue(contents.contains("EVN193"));

        Date today = DateConverter.parseDate("2005-02-13", "yyyy-MM-dd");
        EntitySearchFilter filter = new EntitySearchFilter("DataInizio", true, null, today);
        filter.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter[] filters = {filter};
        contents = _contentManager.loadPublicContentsId("EVN", categories, filters, freeGroup);
        assertEquals(1, contents.size());
        assertTrue(contents.contains("EVN192"));
    }

    @Test
    void testLoadPublicEventsForCategory_2() throws EntException {
        List<String> groups = new ArrayList<>();
        groups.add(Group.ADMINS_GROUP_NAME);
        String[] categories1 = {"general_cat1"};
        EntitySearchFilter filter1 = new EntitySearchFilter(IContentManager.ENTITY_TYPE_CODE_FILTER_KEY, false, "ART", false);
        EntitySearchFilter[] filters = {filter1};
        List<String> contents = this._contentManager.loadPublicContentsId(categories1, filters, groups);
        assertEquals(2, contents.size());
        assertTrue(contents.contains("ART102"));
        assertTrue(contents.contains("ART111"));

        String[] categories2 = {"general_cat2"};
        contents = this._contentManager.loadPublicContentsId(categories2, filters, groups);
        assertEquals(2, contents.size());
        assertTrue(contents.contains("ART111"));
        assertTrue(contents.contains("ART120"));

        String[] categories12 = {"general_cat1", "general_cat2"};
        contents = this._contentManager.loadPublicContentsId(categories12, false, filters, groups);
        assertEquals(1, contents.size());
        assertTrue(contents.contains("ART111"));
        contents = this._contentManager.loadPublicContentsId(categories12, true, filters, groups);
        assertEquals(3, contents.size());
        assertTrue(contents.contains("ART102"));
        assertTrue(contents.contains("ART111"));
        assertTrue(contents.contains("ART120"));

        String[] categories3 = {"general_cat3"};
        contents = this._contentManager.loadPublicContentsId(categories3, filters, groups);
        assertEquals(2, contents.size());
        assertTrue(contents.contains("ART120"));
        assertTrue(contents.contains("ART122"));

        String[] categories23 = {"general_cat2", "general_cat3"};
        contents = this._contentManager.loadPublicContentsId(categories23, false, filters, groups);
        assertEquals(1, contents.size());
        assertTrue(contents.contains("ART120"));
        contents = this._contentManager.loadPublicContentsId(categories23, true, filters, groups);
        assertEquals(3, contents.size());
        assertTrue(contents.contains("ART111"));
        assertTrue(contents.contains("ART120"));
        assertTrue(contents.contains("ART122"));

        String[] categories123 = {"general_cat1", "general_cat2", "general_cat3"};
        contents = this._contentManager.loadPublicContentsId(categories123, false, filters, groups);
        assertEquals(0, contents.size());
        contents = this._contentManager.loadPublicContentsId(categories123, true, filters, groups);
        assertEquals(4, contents.size());
        assertTrue(contents.contains("ART102"));
        assertTrue(contents.contains("ART111"));
        assertTrue(contents.contains("ART120"));
        assertTrue(contents.contains("ART122"));
    }

    @Test
    void testLoadEventsForGroup() throws EntException {
        List<String> contents = _contentManager.loadPublicContentsId("EVN", null, null, freeGroup);
        String[] expectedFreeContentsId = {"EVN191", "EVN192", "EVN193", "EVN194",
            "EVN20", "EVN23", "EVN21", "EVN24", "EVN25"};
        assertEquals(expectedFreeContentsId.length, contents.size());

        for (int i = 0; i < expectedFreeContentsId.length; i++) {
            assertTrue(contents.contains(expectedFreeContentsId[i]));
        }

        Collection<String> allowedGroup = new HashSet<>();
        allowedGroup.add(Group.FREE_GROUP_NAME);
        allowedGroup.add("customers");
        allowedGroup.add("free");

        contents = _contentManager.loadPublicContentsId("EVN", null, null, allowedGroup);
        assertEquals(expectedFreeContentsId.length, contents.size());
        for (int i = 0; i < expectedFreeContentsId.length; i++) {
            assertTrue(contents.contains(expectedFreeContentsId[i]));
        }
        assertFalse(contents.contains("EVN103"));//evento coach

        allowedGroup.remove("customers");
        allowedGroup.remove(Group.FREE_GROUP_NAME);
        allowedGroup.add(Group.ADMINS_GROUP_NAME);

        contents = _contentManager.loadPublicContentsId("EVN", null, null, allowedGroup);
        assertEquals(11, contents.size());
        for (int i = 0; i < expectedFreeContentsId.length; i++) {
            assertTrue(contents.contains(expectedFreeContentsId[i]));
        }
        assertTrue(contents.contains("EVN103"));
        assertTrue(contents.contains("EVN41"));
    }

    @Test
    void testLoadWorkContentsByAttribute_1() throws EntException {
        List<String> groups = new ArrayList<>();
        groups.add(Group.ADMINS_GROUP_NAME);

        EntitySearchFilter filter0 = new EntitySearchFilter(IContentManager.ENTITY_ID_FILTER_KEY, false);
        filter0.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter filter1 = new EntitySearchFilter(IContentManager.ENTITY_TYPE_CODE_FILTER_KEY, false, "ART", true);
        EntitySearchFilter filter2 = new EntitySearchFilter("Numero", true);
        filter2.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter[] filters = {filter0, filter1, filter2};
        String[] expectedContentsId = {"ART120", "ART121"};

        List<String> contents = this._contentManager.loadWorkContentsId(filters, groups);
        assertEquals(expectedContentsId.length, contents.size());
        for (int i = 0; i < expectedContentsId.length; i++) {
            assertEquals(expectedContentsId[i], contents.get(i));
        }

        filter2.setNullOption(true);
        EntitySearchFilter[] filters2 = {filter0, filter1, filter2};
        String[] expectedContentsId2 = {"ART1", "ART102", "ART104",
            "ART111", "ART112", "ART122", "ART179", "ART180", "ART187"};

        contents = this._contentManager.loadWorkContentsId(filters2, groups);
        assertEquals(expectedContentsId2.length, contents.size());
        for (int i = 0; i < expectedContentsId2.length; i++) {
            assertEquals(expectedContentsId2[i], contents.get(i));
        }
    }

    @Test
    void testLoadWorkContentsByAttribute_2() throws EntException {
        List<String> groups = new ArrayList<>();
        groups.add(Group.ADMINS_GROUP_NAME);
        EntitySearchFilter filter0 = new EntitySearchFilter(IContentManager.ENTITY_ID_FILTER_KEY, false);
        filter0.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter filter1 = new EntitySearchFilter(IContentManager.ENTITY_TYPE_CODE_FILTER_KEY, false, "EVN", true);
        EntitySearchFilter filter2 = new EntitySearchFilter("Titolo", true);
        filter2.setOrder(EntitySearchFilter.ASC_ORDER);
        EntitySearchFilter[] filters = {filter0, filter1, filter2};
        String[] expectedContentsId = {"EVN103", "EVN191", "EVN192",
            "EVN193", "EVN194", "EVN20", "EVN21", "EVN23", "EVN24", "EVN25", "EVN41"};

        List<String> contents = this._contentManager.loadWorkContentsId(filters, groups);
        assertEquals(expectedContentsId.length, contents.size());
        for (int i = 0; i < expectedContentsId.length; i++) {
            assertEquals(expectedContentsId[i], contents.get(i));
        }

        filter2.setNullOption(true);
        EntitySearchFilter[] filters2 = {filter0, filter1, filter2};

        contents = this._contentManager.loadWorkContentsId(filters2, groups);
        assertEquals(0, contents.size());

        filter2.setLangCode("it");
        EntitySearchFilter[] filters3 = {filter0, filter1, filter2};
        contents = this._contentManager.loadWorkContentsId(filters3, groups);
        assertEquals(0, contents.size());
    }

    @Test
    void testLoadWorkContentsByAttribute_3() throws Throwable {
        List<String> groups = new ArrayList<String>();
        String[] masterContentIds = {"EVN193", "EVN191", "EVN192", "EVN194", "EVN23", "EVN24"};
        String[] newContentIds = null;
        try {
            newContentIds = this.addDraftContentsForTest(masterContentIds, false);
            for (int i = 0; i < newContentIds.length; i++) {
                Content content = this._contentManager.loadContent(newContentIds[i], false);
                TextAttribute titleAttribute = (TextAttribute) content.getAttribute("Titolo");
                if (i % 2 == 1) {
                    titleAttribute.setText(null, "en");
                }
                titleAttribute.setText(null, "it");
                this._contentManager.saveContent(content);
            }
            groups.add(Group.ADMINS_GROUP_NAME);
            EntitySearchFilter filter0 = new EntitySearchFilter(IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false);
            filter0.setOrder(EntitySearchFilter.ASC_ORDER);
            EntitySearchFilter filter1 = new EntitySearchFilter(IContentManager.ENTITY_TYPE_CODE_FILTER_KEY, false, "EVN", false);
            EntitySearchFilter filter2 = new EntitySearchFilter(IContentManager.ENTITY_ID_FILTER_KEY, false);
            filter2.setOrder(EntitySearchFilter.ASC_ORDER);
            EntitySearchFilter[] filters = {filter0, filter1, filter2};
            String[] expectedContentsId = {"EVN191", "EVN192", "EVN193", "EVN194",
                "EVN103", "EVN20", "EVN23", "EVN24", "EVN25", "EVN41", "EVN21",
                newContentIds[0], newContentIds[1], newContentIds[2], newContentIds[3], newContentIds[4], newContentIds[5]};

            List<String> contents = this._contentManager.loadWorkContentsId(filters, groups);
            assertEquals(expectedContentsId.length, contents.size());
            for (int i = 0; i < expectedContentsId.length; i++) {
                assertEquals(expectedContentsId[i], contents.get(i));
            }

            EntitySearchFilter filter3 = new EntitySearchFilter("Titolo", true);
            filter3.setLangCode("en");
            filter3.setOrder(EntitySearchFilter.ASC_ORDER);
            EntitySearchFilter[] filters1 = {filter0, filter1, filter2, filter3};
            String[] expectedContentsId1 = {"EVN191", "EVN192", "EVN193", "EVN194",
                "EVN103", "EVN20", "EVN23", "EVN24", "EVN25", "EVN41", "EVN21",
                newContentIds[0], newContentIds[2], newContentIds[4]};

            contents = this._contentManager.loadWorkContentsId(filters1, groups);
            assertEquals(expectedContentsId1.length, contents.size());
            for (int i = 0; i < expectedContentsId1.length; i++) {
                assertEquals(expectedContentsId1[i], contents.get(i));
            }

            filter3.setNullOption(true);
            filter3.setLangCode("it");
            EntitySearchFilter[] filters2 = {filter0, filter1, filter2, filter3};
            String[] expectedContentsId2 = {newContentIds[0], newContentIds[1],
                newContentIds[2], newContentIds[3], newContentIds[4], newContentIds[5]};

            contents = this._contentManager.loadWorkContentsId(filters2, groups);
            assertEquals(expectedContentsId2.length, contents.size());
            for (int i = 0; i < expectedContentsId2.length; i++) {
                assertEquals(expectedContentsId2[i], contents.get(i));
            }

            filter3.setNullOption(true);
            filter3.setLangCode("en");
            EntitySearchFilter[] filters3 = {filter0, filter1, filter2, filter3};
            String[] expectedContentsId3 = {newContentIds[1], newContentIds[3], newContentIds[5]};

            contents = this._contentManager.loadWorkContentsId(filters3, groups);
            assertEquals(expectedContentsId3.length, contents.size());
            for (int i = 0; i < expectedContentsId3.length; i++) {
                assertEquals(expectedContentsId3[i], contents.get(i));
            }

            filter2.setNullOption(true);
            EntitySearchFilter[] filters4 = {filter0, filter1, filter2};

            contents = this._contentManager.loadWorkContentsId(filters4, groups);
            assertEquals(0, contents.size());

        } catch (Throwable t) {
            throw t;
        } finally {
            this.deleteContents(newContentIds);
        }
    }
    
    @Test
    void testLoadWorkContentsByAttribute_4() throws Throwable {
        Content prototype = this._contentManager.createContentType("ALL");
        AttributeInterface booleanAttribute = prototype.getAttribute("Boolean");
        booleanAttribute.setSearchable(true);
        AttributeInterface checkBoxAttribute = prototype.getAttribute("CheckBox");
        checkBoxAttribute.setSearchable(true);
        AttributeInterface threeStateAttribute = prototype.getAttribute("ThreeState");
        threeStateAttribute.setSearchable(true);
        List<String> addedContents = new ArrayList<>();
        try {
            ((IEntityTypesConfigurer) this._contentManager).updateEntityPrototype(prototype);
            this._contentManager.reloadEntitiesReferences("ALL");
            super.waitThreads(ApsEntityManager.RELOAD_REFERENCES_THREAD_NAME_PREFIX);
            String[] booleanAttributeCodes = {"Boolean", "CheckBox", "ThreeState"};
            for (int i = 0; i < 15; i++) {
                Content clone = this._contentManager.loadContent("ALL4", false);
                clone.setId(null);
                Boolean valueToSet = null;
                if (i%3 == 0) {
                    // leave null
                } else if (i%2 == 0) {
                    valueToSet = Boolean.TRUE;
                } else {
                    valueToSet = Boolean.FALSE;
                }
                this.setBooleanValue(clone, booleanAttributeCodes, valueToSet);
                this._contentManager.saveContent(clone);
                addedContents.add(clone.getId());
            }
            this.testBooleanAttribute_test4("Boolean", new String[]{}, // null
                    new String[]{addedContents.get(0), addedContents.get(1), addedContents.get(3), addedContents.get(5), addedContents.get(6), 
                        addedContents.get(7), addedContents.get(9), addedContents.get(11), addedContents.get(12), addedContents.get(13)}, // FALSE
                    new String[]{"ALL4", addedContents.get(2), addedContents.get(4), addedContents.get(8), addedContents.get(10), addedContents.get(14)}); // TRUE
            this.testBooleanAttribute_test4("CheckBox", new String[]{}, 
                    new String[]{"ALL4", addedContents.get(0), addedContents.get(1), addedContents.get(3), addedContents.get(5), addedContents.get(6), 
                        addedContents.get(7), addedContents.get(9), addedContents.get(11), addedContents.get(12), addedContents.get(13)}, // FALSE
                    new String[]{addedContents.get(2), addedContents.get(4), addedContents.get(8), addedContents.get(10), addedContents.get(14)}); // TRUE
            this.testBooleanAttribute_test4("ThreeState", new String[]{addedContents.get(0), addedContents.get(3), addedContents.get(6), addedContents.get(9), addedContents.get(12)}, // NULL
                    new String[]{"ALL4", addedContents.get(1), addedContents.get(5), addedContents.get(7), addedContents.get(11), addedContents.get(13)}, // FALSE
                    new String[]{addedContents.get(2), addedContents.get(4), addedContents.get(8), addedContents.get(10), addedContents.get(14)}); // TRUE
        } catch (Exception e) {
            throw e;
        } finally {
            for (int i = 0; i < addedContents.size(); i++) {
                String id = addedContents.get(i);
                this._contentManager.deleteContent(id);
                assertNull(this._contentManager.loadContent(id, false));
            }
            booleanAttribute.setSearchable(false);
            checkBoxAttribute.setSearchable(false);
            threeStateAttribute.setSearchable(false);
            ((IEntityTypesConfigurer) this._contentManager).updateEntityPrototype(prototype);
            this._contentManager.reloadEntitiesReferences("ALL");
            super.waitThreads(ApsEntityManager.RELOAD_REFERENCES_THREAD_NAME_PREFIX);
        }
    }
    
    private void setBooleanValue(Content content, String[] attributeCodes, Boolean value) {
        for (int i = 0; i < attributeCodes.length; i++) {
            BooleanAttribute booleanAttribute = (BooleanAttribute) content.getAttribute(attributeCodes[i]);
            booleanAttribute.setBooleanValue(value);
        }
    }
    
    private void testBooleanAttribute_test4(String booleanAttribute, String[] nullResults, String[] falseResults, String[] trueResults) throws Exception {
        EntitySearchFilter<String> filterForTrue = new EntitySearchFilter<>(booleanAttribute, true, "true", false);
        this.checkResult_test4(trueResults, filterForTrue);
        EntitySearchFilter<String> filterForFalse = new EntitySearchFilter<>(booleanAttribute, true, "false", false);
        this.checkResult_test4(falseResults, filterForFalse);
        EntitySearchFilter<String> filterForNull = new EntitySearchFilter<>(booleanAttribute, true);
        filterForNull.setNullOption(true);
        this.checkResult_test4(nullResults, filterForNull);
    }
    
    private void checkResult_test4(String[] expected, EntitySearchFilter<String> filterForBoolean) throws Exception {
        List<String> groups = new ArrayList<>();
        groups.add(Group.ADMINS_GROUP_NAME);
        EntitySearchFilter<String> filterForType = new EntitySearchFilter<>(IContentManager.ENTITY_TYPE_CODE_FILTER_KEY, false, "ALL", false);
        EntitySearchFilter[] filtersForTrue = {filterForType, filterForBoolean};
        List<String> result = this._contentManager.loadWorkContentsId(filtersForTrue, groups);
        assertEquals(expected.length, result.size());
        for (int i = 0; i < expected.length; i++) {
            String id = expected[i];
            assertTrue(result.contains(id));
        }
    }
    
    @Test
    void testGetLinkProperties() throws Throwable {
        EntitySearchFilter creationOrder = new EntitySearchFilter(IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false);
        creationOrder.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter typeFilter = new EntitySearchFilter(IContentManager.ENTITY_TYPE_CODE_FILTER_KEY, false, "ALL", false);
        EntitySearchFilter[] filters = {creationOrder, typeFilter};
        List<String> userGroups = new ArrayList<>();
        userGroups.add(Group.ADMINS_GROUP_NAME);
        List<String> contents = null;
        try {

            contents = this._contentManager.loadWorkContentsId(filters, userGroups);
            assertEquals(1, contents.size());
            Content content = this._contentManager.loadContent("ALL4", false);
            assertEquals(Content.STATUS_PUBLIC, content.getStatus());
            assertEquals(Group.FREE_GROUP_NAME, content.getMainGroup());
            Map<String, AttributeInterface> attributes = content.getAttributeMap();
            MonoListAttribute monoListAttribute10 = (MonoListAttribute) attributes.get("MonoLLink");
            assertNotNull(monoListAttribute10);
            assertEquals(2, monoListAttribute10.getAttributes().size());

            LinkAttribute attributeToModify = (LinkAttribute) monoListAttribute10.getAttributes().get(0);
            Map<String, String> map = new HashMap<>();
            map.put("key1", "value1");
            map.put("key2", "value2");
            attributeToModify.getLinksProperties().put(attributeToModify.getDefaultLangCode(), map);

            content.setId(null);
            String id = this._contentManager.saveContent(content);

            Content extractedContent = this._contentManager.loadContent(id, false);
            attributes = extractedContent.getAttributeMap();
            monoListAttribute10 = (MonoListAttribute) attributes.get("MonoLLink");
            assertNotNull(monoListAttribute10);
            assertEquals(2, monoListAttribute10.getAttributes().size());
            LinkAttribute attributeModified = (LinkAttribute) monoListAttribute10.getAttributes().get(0);
            assertEquals(1, attributeModified.getLinksProperties().size());
            Map<String, String> mapForDefaultLang = attributeModified.getLinksProperties().get(attributeToModify.getDefaultLangCode());
            assertEquals(2, mapForDefaultLang.size());
            assertEquals("value1", mapForDefaultLang.get("key1"));
            assertEquals("value2", mapForDefaultLang.get("key2"));
        } catch (Exception e) {
            throw e;
        } finally {
            contents = this._contentManager.loadWorkContentsId(filters, userGroups);
            if (contents.size() > 1) {
                Content contentToDelete = this._contentManager.loadContent(contents.get(0), false);
                this._contentManager.deleteContent(contentToDelete);
            }
        }
    }

    @Test
    void testGetResourceProperties() throws Throwable {
        EntitySearchFilter creationOrder = new EntitySearchFilter(IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false);
        creationOrder.setOrder(EntitySearchFilter.DESC_ORDER);
        EntitySearchFilter typeFilter = new EntitySearchFilter(IContentManager.ENTITY_TYPE_CODE_FILTER_KEY, false, "ALL", false);
        EntitySearchFilter[] filters = {creationOrder, typeFilter};
        List<String> userGroups = new ArrayList<>();
        userGroups.add(Group.ADMINS_GROUP_NAME);
        List<String> contents = null;
        try {
            contents = this._contentManager.loadWorkContentsId(filters, userGroups);
            assertEquals(1, contents.size());
            Content content = this._contentManager.loadContent("ALL4", false);
            assertEquals(Content.STATUS_PUBLIC, content.getStatus());
            assertEquals(Group.FREE_GROUP_NAME, content.getMainGroup());
            Map<String, AttributeInterface> attributes = content.getAttributeMap();
            MonoListAttribute monoListAttribute = (MonoListAttribute) attributes.get("MonoLImage");
            assertNotNull(monoListAttribute);
            assertEquals(1, monoListAttribute.getAttributes().size());

            ImageAttribute attributeToModify = (ImageAttribute) monoListAttribute.getAttributes().get(0);
            attributeToModify.setMetadata(IResourceManager.ALT_METADATA_KEY, "en", "ALT en");
            attributeToModify.setMetadata(IResourceManager.ALT_METADATA_KEY, "it", "ALT it");
            attributeToModify.setMetadata(IResourceManager.DESCRIPTION_METADATA_KEY, "en", "Description en");
            attributeToModify.setMetadata(IResourceManager.LEGEND_METADATA_KEY, "it", "Legend it");
            attributeToModify.setMetadata(IResourceManager.TITLE_METADATA_KEY, "it", "Title it");
            attributeToModify.setMetadata(IResourceManager.TITLE_METADATA_KEY, "en", "Title en");

            content.setId(null);
            String id = this._contentManager.saveContent(content);

            Content extractedContent = this._contentManager.loadContent(id, false);
            attributes = extractedContent.getAttributeMap();
            monoListAttribute = (MonoListAttribute) attributes.get("MonoLImage");
            assertNotNull(monoListAttribute);
            assertEquals(1, monoListAttribute.getAttributes().size());
            ImageAttribute attributeModified = (ImageAttribute) monoListAttribute.getAttributes().get(0);
            assertEquals(2, attributeModified.getResourceAltMap().size());
            assertEquals(1, attributeModified.getResourceDescriptionMap().size());
            assertEquals(1, attributeModified.getResourceLegendMap().size());
            assertEquals(2, attributeModified.getResourceTitleMap().size());
            assertEquals("ALT it", attributeModified.getResourceAltForLang("it"));
            assertEquals("Description en", attributeModified.getResourceDescriptionForLang("en"));
            assertEquals("", attributeModified.getResourceDescriptionForLang("it"));
            assertNull(attributeModified.getResourceDescriptionMap().get("it"));
            assertEquals("Legend it", attributeModified.getResourceLegendForLang("it"));
            assertEquals("Title en", attributeModified.getResourceTitleForLang("en"));
        } catch (Exception e) {
            throw e;
        } finally {
            contents = this._contentManager.loadWorkContentsId(filters, userGroups);
            if (contents.size() > 1) {
                Content contentToDelete = this._contentManager.loadContent(contents.get(0), false);
                this._contentManager.deleteContent(contentToDelete);
            }
        }
    }

    @Test
    void testLoadContentsWithoutGroupFilter() throws Exception {
        SearcherDaoPaginatedResult<String> workContentResult = this._contentManager.getPaginatedWorkContentsId(null, false, null, null);
        assertEquals(0, workContentResult.getCount());
        assertEquals(0, workContentResult.getList().size());

        workContentResult = this._contentManager.getPaginatedWorkContentsId(null, false, null, List.of());
        assertEquals(0, workContentResult.getCount());
        assertEquals(0, workContentResult.getList().size());

        SearcherDaoPaginatedResult<String> publishedContentResult = this._contentManager.getPaginatedPublicContentsId(null, false, null, null);
        assertEquals(0, publishedContentResult.getCount());
        assertEquals(0, publishedContentResult.getList().size());

        publishedContentResult = this._contentManager.getPaginatedPublicContentsId(null, false, null, List.of());
        assertEquals(0, publishedContentResult.getCount());
        assertEquals(0, publishedContentResult.getList().size());
    }

    protected String[] addDraftContentsForTest(String[] masterContentIds, boolean publish) throws Throwable {
        String[] newContentIds = new String[masterContentIds.length];
        for (int i = 0; i < masterContentIds.length; i++) {
            Content content = this._contentManager.loadContent(masterContentIds[i], false);
            content.setId(null);
            newContentIds[i] = this._contentManager.saveContent(content);
            if (publish) {
                String check = this._contentManager.insertOnLineContent(content);
                assertEquals(newContentIds[i], check);
            }
        }
        for (int i = 0; i < newContentIds.length; i++) {
            Content content = this._contentManager.loadContent(newContentIds[i], false);
            assertNotNull(content);
        }
        return newContentIds;
    }

    private void deleteContents(String[] contentIds) throws Throwable {
        for (int i = 0; i < contentIds.length; i++) {
            Content content = this._contentManager.loadContent(contentIds[i], false);
            if (null != content) {
                this._contentManager.removeOnLineContent(content);
                this._contentManager.deleteContent(content);
            }
        }
    }
    
    @Test
    void testLoadRoles() throws Exception {
        List<AttributeRole> roles = this._contentManager.getAttributeRoles();
        assertEquals(4, roles.size());
        AttributeRole role = this._contentManager.getAttributeRole("social:image");
        assertNotNull(role);
        assertEquals(1, role.getAllowedAttributeTypes().size());
        assertEquals("Image", role.getAllowedAttributeTypes().get(0));
    }

    @BeforeEach
    private void init() throws Exception {
        try {
            this._contentManager = (IContentManager) this.getService(JacmsSystemConstants.CONTENT_MANAGER);
            this._groupManager = (IGroupManager) this.getService(SystemConstants.GROUP_MANAGER);
        } catch (Throwable t) {
            throw new Exception(t);
        }
    }

    private IContentManager _contentManager = null;
    private IGroupManager _groupManager;
    
}
