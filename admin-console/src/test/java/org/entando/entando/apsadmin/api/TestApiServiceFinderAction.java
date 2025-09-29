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
package org.entando.entando.apsadmin.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.entando.entando.aps.system.services.api.IApiCatalogManager;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import org.apache.struts2.action.Action;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author E.Santoboni
 */
class TestApiServiceFinderAction extends ApsAdminBaseTestCase {
	
	@Test
	void testServiceList() throws Throwable {
		String result = this.executeListServices("admin");
		assertEquals(Action.SUCCESS, result);
		
		result = this.executeListServices(null);
		assertEquals("apslogin", result);
	}
	
	private String executeListServices(String username) throws Throwable {
		this.setUserOnSession(username);
		this.initAction("/do/Api/Service", "list");
		return this.executeAction();
	}
	
    @BeforeEach
	private void init() throws Exception {
    	try {
    		this._apiCatalogManager = (IApiCatalogManager) this.getService(SystemConstants.API_CATALOG_MANAGER);
    	} catch (Throwable t) {
            throw new Exception(t);
        }
    }
    
    private IApiCatalogManager _apiCatalogManager = null;
	
}