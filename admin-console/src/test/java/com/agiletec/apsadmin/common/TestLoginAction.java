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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import org.apache.struts2.action.Action;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version 1.0
 * @author E.Santoboni
 */
class TestLoginAction extends ApsAdminBaseTestCase {
	
    @BeforeEach
	protected void init() throws Exception {
		this.initAction("/do", "doLogin");
	}
	
	@Test
	void testSuccessfulLogin1() throws Throwable {
		String result = this.executeLogin("admin", "admin");
		assertEquals(Action.SUCCESS, result);
    }
	
	@Test
	void testFailedLogin1() throws Throwable {
		String result = this.executeLogin("", "");
		assertEquals(Action.INPUT, result);
		
		Map<String, List<String>> fieldsError = this.getAction().getFieldErrors();
		Collection<String> actionError = this.getAction().getActionErrors();
		assertEquals(2, fieldsError.size());
		assertEquals(0, actionError.size());
    }
	
	@Test
	void testFailedLogin2() throws Throwable {
		String result = this.executeLogin("pippo", "");
		assertEquals(Action.INPUT, result);
		
		Map<String, List<String>> fieldsError = this.getAction().getFieldErrors();
		Collection<String> actionError = this.getAction().getActionErrors();
		assertEquals(1, fieldsError.size());
		assertEquals(0, actionError.size());
    }
	
	@Test
	void testFailedLogin3() throws Throwable {
		String result = this.executeLogin("admin", "wrongPassword");
		assertEquals(Action.INPUT, result);
		
		Map<String, List<String>> fieldsError = this.getAction().getFieldErrors();
		Collection<String> actionError = this.getAction().getActionErrors();
		assertEquals(0, fieldsError.size());
		assertEquals(1, actionError.size());
    }
	
	@Test
	void testFailedLogin4() throws Throwable {
		String result = this.executeLogin("guest", "guest");
		assertEquals(Action.INPUT, result);
		
		Map<String, List<String>> fieldsError = this.getAction().getFieldErrors();
		Collection<String> actionError = this.getAction().getActionErrors();
		assertEquals(0, fieldsError.size());
		assertEquals(1, actionError.size());
    }
	
	private String executeLogin(String username, String password) throws Throwable {
		this.addParameter("username", username);
		this.addParameter("password", password);
		String result = super.executeAction();
		return result;
	}
	
}
