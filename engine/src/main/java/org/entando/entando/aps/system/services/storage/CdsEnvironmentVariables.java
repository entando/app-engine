/*
 * Copyright 2022-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.system.services.storage;

import java.util.Optional;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

public final class CdsEnvironmentVariables {

    private static final String CDS_ACTIVE = "CDS_ENABLED";
    private CdsEnvironmentVariables() {
    }

    public static boolean active() {
        return BooleanUtils.toBoolean(Optional.ofNullable(System.getenv(CDS_ACTIVE))
                .filter(StringUtils::isNotBlank)
                .orElse("false"));
    }

}
