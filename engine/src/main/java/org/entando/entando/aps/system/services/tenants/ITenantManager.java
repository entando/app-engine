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
package org.entando.entando.aps.system.services.tenants;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;

public interface ITenantManager {

    String THREAD_LOCAL_TENANT_CODE = "threadLocal_tenantCode";
    String PRIMARY_CODE = "primary";


    boolean exists(String tenantCode);

    List<String> getCodes();

    Map<String,TenantStatus> getStatuses();

    DataSource getDatasource(String tenantCode);

    Optional<TenantConfig> getConfigOfReadyTenant(String tenantCode);

    Optional<TenantConfig> getConfig(String tenantCode);

    Optional<TenantConfig> getTenantConfigByDomain(String domain);

    String getTenantCodeByDomain(String domain);

}
