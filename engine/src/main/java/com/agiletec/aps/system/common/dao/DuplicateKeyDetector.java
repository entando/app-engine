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
package com.agiletec.aps.system.common.dao;

import java.sql.SQLException;

public final class DuplicateKeyDetector {

    private DuplicateKeyDetector() {
        // utility class
    }

    public static boolean isDuplicateKey(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException && isDuplicateKey((SQLException) current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public static boolean isDuplicateKey(SQLException exception) {
        if (exception == null) {
            return false;
        }
        String sqlState = exception.getSQLState();
        int errorCode = exception.getErrorCode();

        return "23505".equals(sqlState)
                || errorCode == 1062
                || errorCode == 1586
                || errorCode == 1
                || errorCode == 2601
                || errorCode == 2627;
    }
}
