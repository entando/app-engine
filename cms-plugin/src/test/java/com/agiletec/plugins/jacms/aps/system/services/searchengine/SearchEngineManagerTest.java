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
package com.agiletec.plugins.jacms.aps.system.services.searchengine;

import static com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager.STATUS_NEED_TO_RELOAD_INDEXES;
import static com.agiletec.plugins.jacms.aps.system.services.searchengine.ICmsSearchEngineManager.STATUS_READY;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.common.entity.event.EntityTypesChangingEvent;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.NumberAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.TextAttribute;
import com.agiletec.aps.system.common.searchengine.IndexableAttributeInterface;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.content.event.PublicContentChangedEvent;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author E.Santoboni
 */
@ExtendWith(MockitoExtension.class)
class SearchEngineManagerTest {

    @Mock
    private IContentManager contentManager;

    @Mock
    private ISearchEngineDAOFactory factory;

    @Mock
    private IIndexerDAO indexerDao;

    @Mock
    private ISearcherDAO searcherDao;

    @InjectMocks
    private SearchEngineManager searchEngineManager;

    @BeforeEach
    public void setUp() throws Exception {
        when(this.factory.getIndexer()).thenReturn(indexerDao);
        when(this.factory.getSearcher()).thenReturn(searcherDao);
        this.searchEngineManager.init();
    }

    @Test
    void addContentNotify() throws Exception {
        when(this.factory.checkCurrentSubfolder()).thenReturn(false);
        Content content = Mockito.mock(Content.class);
        PublicContentChangedEvent event = new PublicContentChangedEvent();
        event.setContent(content);
        event.setOperationCode(PublicContentChangedEvent.INSERT_OPERATION_CODE);
        this.searchEngineManager.updateFromPublicContentChanged(event);
        Mockito.verify(indexerDao, Mockito.times(1)).add(content);
        Mockito.verify(indexerDao, Mockito.times(0)).delete(Mockito.anyString(), Mockito.anyString());
        Mockito.verifyZeroInteractions(searcherDao);
        Mockito.verify(factory, Mockito.times(1)).init();
    }

    @Test
    void addContentNotify_withError() throws Exception {
        when(this.factory.checkCurrentSubfolder()).thenReturn(true);
        Mockito.doThrow(EntException.class).when(this.indexerDao).add(Mockito.any(Content.class));
        Content content = Mockito.mock(Content.class);
        PublicContentChangedEvent event = new PublicContentChangedEvent();
        event.setContent(content);
        event.setOperationCode(PublicContentChangedEvent.INSERT_OPERATION_CODE);
        this.searchEngineManager.updateFromPublicContentChanged(event);
        Mockito.verify(indexerDao, Mockito.times(1)).add(content);
        Mockito.verifyZeroInteractions(searcherDao);
        Mockito.verify(factory, Mockito.times(0)).init();
    }

    @Test
    void updateContentNotify() throws Exception {
        when(this.factory.checkCurrentSubfolder()).thenReturn(false);
        Content content = Mockito.mock(Content.class);
        when(content.getId()).thenReturn("ART123");
        PublicContentChangedEvent event = new PublicContentChangedEvent();
        event.setContent(content);
        event.setOperationCode(PublicContentChangedEvent.UPDATE_OPERATION_CODE);
        this.searchEngineManager.updateFromPublicContentChanged(event);
        Mockito.verify(indexerDao, Mockito.times(1)).add(content);
        Mockito.verify(indexerDao, Mockito.times(1)).delete(IIndexerDAO.CONTENT_ID_FIELD_NAME, "ART123");
        Mockito.verifyZeroInteractions(searcherDao);
    }

    @Test
    void updateContentNotify_withError() throws Exception {
        when(this.factory.checkCurrentSubfolder()).thenReturn(true);
        Mockito.doThrow(EntException.class).when(this.indexerDao).delete(Mockito.anyString(), Mockito.anyString());
        Content content = Mockito.mock(Content.class);
        when(content.getId()).thenReturn("ART124");
        PublicContentChangedEvent event = new PublicContentChangedEvent();
        event.setContent(content);
        event.setOperationCode(PublicContentChangedEvent.UPDATE_OPERATION_CODE);
        this.searchEngineManager.updateFromPublicContentChanged(event);
        Mockito.verify(indexerDao, Mockito.times(1)).delete(IIndexerDAO.CONTENT_ID_FIELD_NAME, "ART124");
        Mockito.verify(indexerDao, Mockito.times(0)).add(content);
        Mockito.verifyZeroInteractions(searcherDao);
    }

    @Test
    void deleteContentNotify() throws Exception {
        when(this.factory.checkCurrentSubfolder()).thenReturn(false);
        Content content = Mockito.mock(Content.class);
        when(content.getId()).thenReturn("ART125");
        PublicContentChangedEvent event = new PublicContentChangedEvent();
        event.setContent(content);
        event.setOperationCode(PublicContentChangedEvent.REMOVE_OPERATION_CODE);
        this.searchEngineManager.updateFromPublicContentChanged(event);
        Mockito.verify(indexerDao, Mockito.times(0)).add(content);
        Mockito.verify(indexerDao, Mockito.times(1)).delete(IIndexerDAO.CONTENT_ID_FIELD_NAME, "ART125");
        Mockito.verifyZeroInteractions(searcherDao);
    }

    @Test
    void addEntityTypeNotify() throws Exception {
        EntityTypesChangingEvent event = this.initEntityTypeNotify();
        event.setOperationCode(EntityTypesChangingEvent.INSERT_OPERATION_CODE);
        this.searchEngineManager.updateFromEntityTypesChanging(event);
        Assertions.assertEquals(STATUS_READY, this.searchEngineManager.getStatus());
    }

    @Test
    void updateEntityTypeNotify() throws Exception {
        EntityTypesChangingEvent event = this.initEntityTypeNotify();
        event.setOperationCode(EntityTypesChangingEvent.UPDATE_OPERATION_CODE);
        this.searchEngineManager.updateFromEntityTypesChanging(event);
        Assertions.assertEquals(STATUS_NEED_TO_RELOAD_INDEXES, this.searchEngineManager.getStatus());
    }

    @Test
    void removeEntityTypeNotify() throws Exception {
        EntityTypesChangingEvent event = this.initEntityTypeNotify();
        event.setOperationCode(EntityTypesChangingEvent.REMOVE_OPERATION_CODE);
        this.searchEngineManager.updateFromEntityTypesChanging(event);
        Assertions.assertEquals(STATUS_NEED_TO_RELOAD_INDEXES, this.searchEngineManager.getStatus());
    }

    private EntityTypesChangingEvent initEntityTypeNotify() {
        this.searchEngineManager.setStatus(STATUS_READY);
        EntityTypesChangingEvent event = new EntityTypesChangingEvent();
        Content type1 = Mockito.mock(Content.class);
        Content type2 = Mockito.mock(Content.class);
        AttributeInterface attributeType1 = Mockito.mock(AttributeInterface.class);
        Mockito.lenient().when(attributeType1.getIndexingType())
                .thenReturn(IndexableAttributeInterface.INDEXING_TYPE_TEXT);
        Mockito.lenient().when(type1.getAttributeMap()).thenReturn(Map.of("type1", attributeType1));
        AttributeInterface attributeType2 = Mockito.mock(AttributeInterface.class);
        Mockito.lenient().when(attributeType2.getIndexingType()).thenReturn(null);
        Mockito.lenient().when(type2.getAttributeMap()).thenReturn(Map.of("type2", attributeType2));
        event.setOldEntityType(type1);
        event.setNewEntityType(type2);
        event.setEntityManagerName(JacmsSystemConstants.CONTENT_MANAGER);
        when(this.contentManager.getName()).thenReturn(JacmsSystemConstants.CONTENT_MANAGER);
        return event;
    }

    @Test
    void testSearchIds() throws Exception {
        when(this.searcherDao.searchContentsId(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(
                new ArrayList<>(Arrays.asList("Art123", "Art456")));
        when(this.factory.checkCurrentSubfolder()).thenReturn(Boolean.TRUE);
        List<String> resources = this.searchEngineManager.searchEntityId("it", "test",
                Arrays.asList("group1", "group2"));
        Assertions.assertEquals(2, resources.size());
    }

    @Test
    void testSearchIds_withErrors() throws Exception {
        Mockito.doThrow(EntException.class).when(this.searcherDao)
                .searchContentsId(Mockito.any(), Mockito.any(), Mockito.any());
        when(this.factory.checkCurrentSubfolder()).thenReturn(Boolean.TRUE);
        Assertions.assertThrows(EntException.class, () -> {
            this.searchEngineManager.searchEntityId("it", "test", Arrays.asList("group1", "group2"));
        });
    }

    @Test
    void reloadIndexesShouldBeNecessaryIfIndexableOptionIsAdded() {
        Mockito.when(this.contentManager.getName()).thenReturn(JacmsSystemConstants.CONTENT_MANAGER);

        EntityTypesChangingEvent event = new EntityTypesChangingEvent();
        event.setOperationCode(EntityTypesChangingEvent.UPDATE_OPERATION_CODE);
        event.setEntityManagerName(JacmsSystemConstants.CONTENT_MANAGER);

        Content oldContentType = new Content();
        oldContentType.addAttribute(getTextAttribute("field1"));
        event.setOldEntityType(oldContentType);

        Content newContentType = new Content();
        TextAttribute newAttribute = getTextAttribute("field1");
        newAttribute.setIndexingType(IndexableAttributeInterface.INDEXING_TYPE_TEXT);
        newContentType.addAttribute(newAttribute);
        event.setNewEntityType(newContentType);

        Assertions.assertEquals(STATUS_READY, searchEngineManager.getStatus());
        searchEngineManager.updateFromEntityTypesChanging(event);
        Assertions.assertEquals(STATUS_NEED_TO_RELOAD_INDEXES, searchEngineManager.getStatus());
    }

    @Test
    void reloadIndexesShouldBeNecessaryIfIndexableOptionIsRemoved() {
        Mockito.when(this.contentManager.getName()).thenReturn(JacmsSystemConstants.CONTENT_MANAGER);

        EntityTypesChangingEvent event = new EntityTypesChangingEvent();
        event.setOperationCode(EntityTypesChangingEvent.UPDATE_OPERATION_CODE);
        event.setEntityManagerName(JacmsSystemConstants.CONTENT_MANAGER);

        Content oldContentType = new Content();
        TextAttribute oldAttribute = getTextAttribute("field1");
        oldAttribute.setIndexingType(IndexableAttributeInterface.INDEXING_TYPE_TEXT);
        oldContentType.addAttribute(oldAttribute);
        event.setOldEntityType(oldContentType);

        Content newContentType = new Content();
        newContentType.addAttribute(getTextAttribute("field1"));
        event.setNewEntityType(newContentType);

        Assertions.assertEquals(STATUS_READY, searchEngineManager.getStatus());
        searchEngineManager.updateFromEntityTypesChanging(event);
        Assertions.assertEquals(STATUS_NEED_TO_RELOAD_INDEXES, searchEngineManager.getStatus());
    }

    @Test
    void reloadIndexesShouldBeNecessaryIfAttributeIsRemoved() {
        Mockito.when(this.contentManager.getName()).thenReturn(JacmsSystemConstants.CONTENT_MANAGER);

        EntityTypesChangingEvent event = new EntityTypesChangingEvent();
        event.setOperationCode(EntityTypesChangingEvent.UPDATE_OPERATION_CODE);
        event.setEntityManagerName(JacmsSystemConstants.CONTENT_MANAGER);

        Content oldContentType = new Content();
        oldContentType.addAttribute(getTextAttribute("field1"));
        oldContentType.addAttribute(getTextAttribute("field2"));
        event.setOldEntityType(oldContentType);

        Content newContentType = new Content();
        newContentType.addAttribute(getTextAttribute("field1"));
        event.setNewEntityType(newContentType);

        Assertions.assertEquals(STATUS_READY, searchEngineManager.getStatus());
        searchEngineManager.updateFromEntityTypesChanging(event);
        Assertions.assertEquals(STATUS_NEED_TO_RELOAD_INDEXES, searchEngineManager.getStatus());
    }

    @Test
    void reloadIndexesShouldNotBeNecessaryIfAttributeIsAdded() {
        Mockito.when(this.contentManager.getName()).thenReturn(JacmsSystemConstants.CONTENT_MANAGER);

        EntityTypesChangingEvent event = new EntityTypesChangingEvent();
        event.setOperationCode(EntityTypesChangingEvent.UPDATE_OPERATION_CODE);
        event.setEntityManagerName(JacmsSystemConstants.CONTENT_MANAGER);

        Content oldContentType = new Content();
        event.setOldEntityType(oldContentType);

        Content newContentType = new Content();
        newContentType.addAttribute(getTextAttribute("field1"));
        event.setNewEntityType(newContentType);

        Assertions.assertEquals(STATUS_READY, searchEngineManager.getStatus());
        searchEngineManager.updateFromEntityTypesChanging(event);
        Assertions.assertEquals(STATUS_READY, searchEngineManager.getStatus());
    }

    @Test
    void reloadIndexesShouldBeNecessaryIfAttributeTypeIsChanged() {
        Mockito.when(this.contentManager.getName()).thenReturn(JacmsSystemConstants.CONTENT_MANAGER);

        EntityTypesChangingEvent event = new EntityTypesChangingEvent();
        event.setOperationCode(EntityTypesChangingEvent.UPDATE_OPERATION_CODE);
        event.setEntityManagerName(JacmsSystemConstants.CONTENT_MANAGER);

        Content oldContentType = new Content();
        oldContentType.addAttribute(getNumberAttribute("field1"));
        event.setOldEntityType(oldContentType);

        Content newContentType = new Content();
        newContentType.addAttribute(getTextAttribute("field1"));
        event.setNewEntityType(newContentType);

        Assertions.assertEquals(STATUS_READY, searchEngineManager.getStatus());
        searchEngineManager.updateFromEntityTypesChanging(event);
        Assertions.assertEquals(STATUS_NEED_TO_RELOAD_INDEXES, searchEngineManager.getStatus());
    }

    private TextAttribute getTextAttribute(String name) {
        TextAttribute textAttribute = new TextAttribute();
        textAttribute.setName(name);
        textAttribute.setType("Text");
        return textAttribute;
    }

    private NumberAttribute getNumberAttribute(String name) {
        NumberAttribute numberAttribute = new NumberAttribute();
        numberAttribute.setName(name);
        numberAttribute.setType("Number");
        return numberAttribute;
    }
}
