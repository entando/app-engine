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
package com.agiletec.aps.system.services.category;

import java.util.List;

import org.entando.entando.ent.exception.EntException;

/**
 * Interfaccia base per i servizi, i cui elementi gestiti, fanno uso delle
 * categorie.
 *
 * @author E.Santoboni
 */
public interface CategoryUtilizer<T, Z> {

    /**
     * Restituisce l'identificativo del servizio utilizzatore.
     *
     * @return L'identificativo del servizio utilizzatore.
     */
    public String getName();

    /**
     * Restituisce la lista degli oggetti referenzianti la categoria
     * identificata dal codice specificato.
     *
     * @param categoryCode Il codice della categoria.
     * @return La lista degli oggetti referenzianti la categoria identificata
     * dal codice specificato.
     * @throws EntException In caso di errore.
     */
    public List<T> getCategoryUtilizers(String categoryCode) throws EntException;

    /**
     * Reload the category references.
     * <p>
     * Target object are the result of getCategoryUtilizersForReloadReferences
     *
     * @param categoryCode The category code
     * @throws EntException In case of error
     */
    public void reloadCategoryReferences(String categoryCode) throws EntException;

    /**
     * Returns a list of identifiers of objects referenced by the categoryCode
     * provided.
     * <p>
     * Changes according to the single implementation and may coincide with
     * getCategoryUtilizers
     *
     * @param categoryCode The category code
     * @return
     * @throws EntException In case of error
     */
    public List<Z> getCategoryUtilizersForReloadReferences(String categoryCode) throws EntException;

}
