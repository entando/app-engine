/*
 * Copyright 2026-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.apache.struts2.action.Action;
import org.junit.jupiter.api.Test;

@SuppressWarnings("removal")
class BaseCommonActionFrontTest {

    @Test
    void frontEditReturnsSuccess() {
        assertEquals(Action.SUCCESS, new BaseCommonAction().frontEdit());
    }

    @Test
    void frontEditPasswordDelegatesToEditPassword() {
        BaseCommonAction action = spy(new BaseCommonAction());
        assertEquals(Action.SUCCESS, action.frontEditPassword());
        verify(action).editPassword();
    }

    @Test
    void frontChangePasswordDelegatesToChangePassword() {
        BaseCommonAction action = spy(new BaseCommonAction());
        doReturn(Action.ERROR).when(action).changePassword();
        assertEquals(Action.ERROR, action.frontChangePassword());
        verify(action).changePassword();
    }

}
