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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;

import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.parse.IEntityTypeFactory;
import com.agiletec.aps.system.common.notify.INotifyManager;
import com.agiletec.aps.system.services.category.Category;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.content.parse.ContentDOM;
import com.agiletec.plugins.jacms.aps.system.services.content.parse.ContentTypeDOM;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;

@ExtendWith(MockitoExtension.class)
class ContentManagerTest {

    @Mock
    private IEntityTypeFactory entityTypeFactory;

    @Mock
    private ContentTypeDOM entityTypeDom;

    @Mock
    private ContentDOM entityDom;

    @Mock
    private ContentDAO contentDAO;

    @Mock
    private BeanFactory beanFactory;

    @Mock
    private INotifyManager notifyManager;

    private String beanName = "jacmsContentManager";

    private String className = "com.agiletec.plugins.jacms.aps.system.services.content.model.Content";

    @InjectMocks
    private ContentManager contentManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        this.contentManager.setEntityClassName(className);
        this.contentManager.setConfigItemName(JacmsSystemConstants.CONFIG_ITEM_CONTENT_TYPES);
        this.contentManager.setBeanName(this.beanName);
    }

    @Test
    void testCreateContent() throws EntException {
        String typeCode = "ART";
        // @formatter:off
        when(entityTypeFactory.extractEntityType(
                                                 typeCode,
                                                 Content.class,
                                                 contentManager.getConfigItemName(),
                                                 this.entityTypeDom,
                                                 contentManager.getName(),
                                                 this.entityDom))
        .thenReturn(this.createFakeEntity(typeCode, "contentview", "1"));
        // @formatter:on
        Content content = this.contentManager.createContentType(typeCode);
        assertThat(content, is(not(nullValue())));
        assertThat(content.getViewPage(), is("contentview"));
        assertThat(content.getDefaultModel(), is("1"));
    }

    @Test
    void testGetXML() throws Throwable {
        String typeCode = "ART";
        // @formatter:off
        when(entityTypeFactory.extractEntityType(
                                                 typeCode,
                                                 Content.class,
                                                 contentManager.getConfigItemName(),
                                                 this.entityTypeDom,
                                                 contentManager.getName(),
                                                 this.entityDom))
        .thenReturn(this.createFakeEntity(typeCode, "contentview", "1"));
        // @formatter:on
        Content content = contentManager.createContentType(typeCode);
        ContentDOM contentDOM = new ContentDOM();
        contentDOM.setRootElementName("content");
        content.setEntityDOM(contentDOM);


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

    private IApsEntity createFakeEntity(String typeCode, String viewPage, String defaultModel) {
        Content content = new Content();
        content.setTypeCode(typeCode);
        content.setViewPage(viewPage);
        content.setDefaultModel(defaultModel);
        return content;
    }
    
    @Test
    void failGetDefaultModelById() throws Exception {
        Assertions.assertThrows(EntRuntimeException.class, () -> {
            lenient().when(this.entityTypeFactory.extractEntityType(anyString(), any(Class.class), 
                    anyString(), eq(this.entityTypeDom), eq(this.beanName), eq(this.entityDom))).thenThrow(EntException.class);
            String modelId = this.contentManager.getDefaultModel("ART123");
        });
    }

    @Test
    void getEntityPrototypeShouldReturnNullForNullTypeCode() {
        Assertions.assertNull(this.contentManager.getEntityPrototype(null));
    }

    @Test
    void shouldReturnNullFromModelAccessorsWhenTypeNotFound() {
        ContentManager spyManager = spy(this.contentManager);
        doReturn(null).when(spyManager).getTypeById(anyString());

        Assertions.assertNull(spyManager.getViewPage("XYZ123"));
        Assertions.assertNull(spyManager.getDefaultModel("XYZ123"));
        Assertions.assertNull(spyManager.getListModel("XYZ123"));
    }
}

