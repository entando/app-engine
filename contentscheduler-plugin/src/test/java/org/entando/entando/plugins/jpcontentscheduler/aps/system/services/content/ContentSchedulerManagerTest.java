/*
 * Copyright 2017-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.entando.entando.plugins.jpcontentscheduler.aps.system.services.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.common.notify.INotifyManager;
import com.agiletec.aps.system.services.keygenerator.IKeyGeneratorManager;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.event.PublicContentChangedEvent;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import org.entando.entando.aps.system.services.cache.ICacheInfoManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEvent;

/**
 * The scheduler publishes contents through the core ContentManager, which notifies the
 * PublicContentChangedEvent on its own, but it unpublishes and archives them through its
 * own manager: these tests cover the notifications that manager has to send by itself,
 * otherwise the search engine indexes keep the unpublished contents (ECS-596).
 */
@ExtendWith(MockitoExtension.class)
class ContentSchedulerManagerTest {

    private static final String CONTENT_ID = "ART123";
    private static final String TYPE_CODE = "ART";

    @Mock
    private IContentSchedulerDAO contentSchedulerDAO;
    @Mock
    private INotifyManager notifyManager;
    @Mock
    private ICacheInfoManager cacheInfoManager;
    @Mock
    private IKeyGeneratorManager keyGeneratorManager;

    @Captor
    private ArgumentCaptor<ApplicationEvent> eventCaptor;

    private ContentSchedulerManager contentSchedulerManager;

    @BeforeEach
    void setUp() {
        this.contentSchedulerManager = new ContentSchedulerManager();
        this.contentSchedulerManager.setContentSchedulerDAO(this.contentSchedulerDAO);
        this.contentSchedulerManager.setNotifyManager(this.notifyManager);
        this.contentSchedulerManager.setCacheInfoManager(this.cacheInfoManager);
        this.contentSchedulerManager.setKeyGeneratorManager(this.keyGeneratorManager);
    }

    @Test
    void removeOnLineContentShouldNotifyTheRemoveOperation() throws Exception {
        Content content = this.createContent(true, Content.STATUS_PUBLIC);

        this.contentSchedulerManager.removeOnLineContent(content, false);

        verify(this.contentSchedulerDAO).unpublishOnLineContent(content);
        PublicContentChangedEvent event = this.captureNotifiedEvent();
        assertEquals(PublicContentChangedEvent.REMOVE_OPERATION_CODE, event.getOperationCode());
        assertEquals(CONTENT_ID, event.getContentId());
        assertEquals(Content.STATUS_READY, content.getStatus());
    }

    @Test
    void notifiedEventShouldBeAddressedToTheContentChannel() throws Exception {
        Content content = this.createContent(true, Content.STATUS_PUBLIC);

        this.contentSchedulerManager.removeOnLineContent(content, false);

        // RedisNotifyManager republishes an event only when both channel and message are
        // set, so a bare "new PublicContentChangedEvent()" would never leave the instance.
        PublicContentChangedEvent event = this.captureNotifiedEvent();
        assertEquals(JacmsSystemConstants.CONTENT_EVENT_CHANNEL, event.getChannel());
        assertNotNull(event.getMessage());
        assertTrue(event.getMessage().contains(CONTENT_ID));
        assertTrue(event.getMessage().contains(String.valueOf(PublicContentChangedEvent.REMOVE_OPERATION_CODE)));
    }

    @Test
    @SuppressWarnings("deprecation")
    void notifiedEventShouldCarryTheContentObject() throws Exception {
        Content content = this.createContent(true, Content.STATUS_PUBLIC);

        this.contentSchedulerManager.removeOnLineContent(content, false);

        // SeoMappingManager discards the event when getContent() is null, so the
        // deprecated content object has to be set as ContentManager does.
        assertEquals(content, this.captureNotifiedEvent().getContent());
    }

    @Test
    void moveOnLineContentShouldNotifyTheUpdateOperationForAPublishedContent() throws Exception {
        Content content = this.createContent(true, Content.STATUS_PUBLIC);

        this.contentSchedulerManager.moveOnLineContent(content, false, false);

        verify(this.contentSchedulerDAO).updateContent(content, false);
        verify(this.contentSchedulerDAO).publishContent(content);
        PublicContentChangedEvent event = this.captureNotifiedEvent();
        assertEquals(PublicContentChangedEvent.UPDATE_OPERATION_CODE, event.getOperationCode());
        assertEquals(CONTENT_ID, event.getContentId());
    }

    @Test
    void moveOnLineContentShouldNotifyTheInsertOperationForAnUnpublishedContent() throws Exception {
        Content content = this.createContent(false, Content.STATUS_READY);

        this.contentSchedulerManager.moveOnLineContent(content, false, false);

        assertEquals(PublicContentChangedEvent.INSERT_OPERATION_CODE,
                this.captureNotifiedEvent().getOperationCode());
    }

    @Test
    void moveOnLineContentShouldNotNotifyWhenTheContentIsJustCreated() throws Exception {
        when(this.keyGeneratorManager.getUniqueKeyCurrentValue()).thenReturn(99);
        Content content = this.createContent(false, Content.STATUS_READY);
        content.setId(null);

        this.contentSchedulerManager.moveOnLineContent(content, false, false);

        // Nothing gets published here: the content is only added to the work version.
        assertEquals(TYPE_CODE + "99", content.getId());
        verify(this.contentSchedulerDAO).addEntity(content);
        verify(this.notifyManager, never()).publishEvent(any());
    }

    private Content createContent(boolean onLine, String status) {
        Content content = new Content();
        content.setId(CONTENT_ID);
        content.setTypeCode(TYPE_CODE);
        content.setDescription("test content");
        content.setStatus(status);
        content.setOnLine(onLine);
        return content;
    }

    private PublicContentChangedEvent captureNotifiedEvent() {
        verify(this.notifyManager).publishEvent(this.eventCaptor.capture());
        return assertInstanceOf(PublicContentChangedEvent.class, this.eventCaptor.getValue());
    }

}