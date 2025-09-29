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
package org.entando.entando.apsadmin.portal.model.helper;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;


import com.agiletec.aps.system.services.pagemodel.PageModel;
import org.entando.entando.ent.exception.EntException;

/**
 * Helper interface for PageModel Actions.
 * @author E.Santoboni
 */
public interface IPageModelActionHelper {
	
	public Map<String, List<Object>> getReferencingObjects(PageModel pageModel, HttpServletRequest request) throws EntException;
	
}
