/*
 * Copyright 2023-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpsolr.aps.system.solr;

import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.plugins.jacms.aps.system.services.searchengine.IIndexerDAO;
import java.util.stream.Stream;
import org.entando.entando.ent.exception.EntException;

public interface ISolrIndexerDAO extends IIndexerDAO {

    void addBulk(Stream<IApsEntity> entityStream) throws EntException;

    boolean deleteAllDocuments();
}
