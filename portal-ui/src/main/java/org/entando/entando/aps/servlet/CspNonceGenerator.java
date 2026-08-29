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
package org.entando.entando.aps.servlet;

import java.security.SecureRandom;

/**
 * Generates the per-request secure random tokens used as Content-Security-Policy nonces,
 * shared between the portal front-controller and the back-office CSP filter.
 */
public final class CspNonceGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private CspNonceGenerator() {
        // utility class
    }

    public static String createNonce() {
        int leftLimit = 48;
        int rightLimit = 122;
        int targetStringLength = 64;
        return RANDOM.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

}
