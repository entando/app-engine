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
package com.agiletec.aps.system.services.baseconfig;

import org.entando.entando.ent.exception.EntException;
import java.util.Map;

/**
 * Interfaccia per il servizio di configurazione.
 *
 * @author M.Diana - E.Santoboni
 */
public interface ConfigInterface {

    public static final String LEGACY_PASSWORDS_UPDATED = "legacyPasswordsUpdated";

    /**
     * Restituisce una voce di configurazione. La voce è un elemento di testo
     * che può essere complesso (es. XML). I valori restituiti sono relativi
     * alla versione di configurazione con cui è stato avviato il sistema.
     *
     * @param name Il codice della voce da restituire.
     * @return Il testo della voce di configurazione.
     */
    public String getConfigItem(String name);

    /**
     * Aggiorna una voce di configurazione. La voce è un elemento di testo che
     * può essere complesso (es. XML).
     *
     * @param name Il codice della voceda aggiornare.
     * @param config Il testo della voce di configurazione da aggiornare.
     * @throws EntException In caso di errore nell'aggiornamento
     */
    public void updateConfigItem(String name, String config) throws EntException;

    /**
     * Restituisce un parametro di configurazione. Un parametro è una stringa
     * semplice.
     *
     * @param name Il nome del parametro
     * @return Il valore del parametro
     */
    public String getParam(String name);

    public void updateParam(String name, String value) throws EntException;

    public void updateParams(Map<String, String> params) throws EntException;

    public void updateParam(String name, String value, boolean addIfNew) throws EntException;

    public void updateParams(Map<String, String> params, boolean addNewOnes) throws EntException;
    
}
