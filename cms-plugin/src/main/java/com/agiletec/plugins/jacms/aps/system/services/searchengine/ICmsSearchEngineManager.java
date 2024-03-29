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

import com.agiletec.aps.system.common.tree.ITreeNode;
import java.util.Collection;
import java.util.List;
import org.entando.entando.aps.system.services.searchengine.FacetedContentsResult;
import org.entando.entando.aps.system.services.searchengine.IEntitySearchEngineManager;
import org.entando.entando.aps.system.services.searchengine.SearchEngineFilter;
import org.entando.entando.ent.exception.EntException;

/**
 * Interfaccia base per i servizi detentori delle operazioni di indicizzazione
 * di oggetti ricercabili tramite motore di ricerca.
 *
 * @author W.Ambu - E.Santoboni
 */
public interface ICmsSearchEngineManager extends IEntitySearchEngineManager {

    public static final String RELOAD_THREAD_NAME_PREFIX = "RELOAD_INDEX_";

    public static final int STATUS_READY = 0;
    public static final int STATUS_RELOADING_INDEXES_IN_PROGRESS = 1;
    public static final int STATUS_NEED_TO_RELOAD_INDEXES = 2;

    /**
     * Ricarica in blocco le indicizzazioni dei contenuti necessari per le
     * diverse ricerche sui contenuti.
     *
     * @return Il thread lanciato in esecuzione.
     * @throws EntException in caso di errore in inizializzazione
     * processo.
     */
    public Thread startReloadContentsReferences() throws EntException;

    @Deprecated(since = "7.2.0")
    public Thread startReloadContentsReferences(String subDirectory) throws EntException;

    /**
     * Return the service status id.
     *
     * @return The service status id.
     */
    public int getStatus();

    /**
     * Restituisce le informazioni sull'ultimo ricaricamento della
     * configurazione.
     *
     * @return Le informazioni sull'ultimo ricaricamento della configurazione.
     */
    public LastReloadInfo getLastReloadInfo();
    
    public FacetedContentsResult searchFacetedEntities(SearchEngineFilter[] filters,
            Collection<ITreeNode> categories, Collection<String> allowedGroups) throws EntException;

    public FacetedContentsResult searchFacetedEntities(SearchEngineFilter[] filters,
            SearchEngineFilter[] categories, Collection<String> allowedGroups) throws EntException;

    public List<String> loadContentsId(SearchEngineFilter[] filters,
            SearchEngineFilter[] facetNodeCodes, List<String> groupCodes) throws EntException;

}
