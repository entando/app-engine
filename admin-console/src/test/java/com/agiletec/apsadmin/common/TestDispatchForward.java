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
package com.agiletec.apsadmin.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import org.apache.struts2.action.Action;
import org.junit.jupiter.api.Test;

/**
 * @version 1.0
 * @author E.Santoboni
 */
class TestDispatchForward extends ApsAdminBaseTestCase {
	
	@Test
	void testGoOnMainPage() throws Throwable {
    	this.initAction("/do", "main");
    	this.setUserOnSession("admin");
    	String result = super.executeAction();
		assertEquals(Action.SUCCESS, result);
	}
	
    @Test
	void testGoOnMainPageWithUserNotAbilitated() throws Throwable {
    	this.initAction("/do", "main");
    	this.setUserOnSession("guest");
    	String result = super.executeAction();
		assertEquals("apslogin", result);
	}
    
    @Test
	void testGoOnMainPageWithNullUser() throws Throwable {
    	this.initAction("/do", "main");
    	this.removeUserOnSession();
    	String result = super.executeAction();
		assertEquals("apslogin", result);
	}
    
}
