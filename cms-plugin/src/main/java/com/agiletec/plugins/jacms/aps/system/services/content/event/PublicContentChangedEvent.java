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
package com.agiletec.plugins.jacms.aps.system.services.content.event;

import com.agiletec.aps.system.common.IManager;
import com.agiletec.aps.system.common.notify.ApsEvent;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import java.util.Map;

/**
 * Evento specifico da rilanciare in corrispondenza 
 * di approvazione o disapprovazione di un contenuto.
 * @author E.Santoboni - M.Diana
 */
public class PublicContentChangedEvent extends ApsEvent {

    public PublicContentChangedEvent() {}

    public PublicContentChangedEvent(String channel, Map<String, String> properties) {
        super(channel, properties);
    }

    @Override
    public void notify(IManager srv) {
        ((PublicContentChangedObserver) srv).updateFromPublicContentChanged(this);
    }

    @Override
    public Class getObserverInterface() {
        return PublicContentChangedObserver.class;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    @Deprecated(since = "7.2.1")
    public Content getContent() {
        return content;
    }

    @Deprecated(since = "7.2.1")
    public void setContent(Content content) {
        if (null != content) {
            this.setContentId(content.getId());
        }
        this.content = content;
    }

    /**
     * Restituisce il codice dell'operazione 
     * che si stà eseguendo sul contenuto pubblico.
     * @return Il codice dell'operazione.
     */
    public int getOperationCode() {
        return operationCode;
    }

    /**
     * Setta il codice dell'operazione 
     * che si stà eseguendo sul contenuto pubblico.
     * @param operationCode Il codice dell'operazione.
     */
    public void setOperationCode(int operationCode) {
        this.operationCode = operationCode;
    }

    private String contentId;

    private Content content;

    private int operationCode;

    /**
     * Codice dell'operazione di inserimento del contenuto onLine.
     */
    public static final int INSERT_OPERATION_CODE = 1;

    /**
     * Codice dell'operazione di rimozione del contenuto onLine.
     */
    public static final int REMOVE_OPERATION_CODE = 2;

    /**
     * Codice dell'operazione di aggiornamento del contenuto onLine.
     */
    public static final int UPDATE_OPERATION_CODE = 3;

}
