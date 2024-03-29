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
package org.entando.entando.plugins.jacms.apsadmin.content.bulk.commands;

/**
 * Codes used in the event of errors in the execution of a command, relative to a single item.
 * @author E.Mezzano
 *
 */
public enum ApsCommandErrorCode {
    /**
     * Returned when the item on which to run the command is not found.
     */
    NOT_FOUND,
    /**
     * Returned when the command is not executable, by the given user, on the given item.
     */
    USER_NOT_ALLOWED,
    /**
     * Returned when some parameters of the command are not valid.
     */
    PARAMS_NOT_VALID,
    /**
     * Returned when the command is not applicable on the given item.
     */
    NOT_APPLICABLE,
    /**
     * Returned on all the other error cases.
     */
    ERROR
}