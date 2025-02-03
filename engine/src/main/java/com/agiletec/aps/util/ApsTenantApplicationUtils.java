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
package com.agiletec.aps.util;

import static org.entando.entando.aps.system.services.tenants.ITenantManager.PRIMARY_CODE;

import com.agiletec.aps.system.EntThreadLocal;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.aps.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ApsTenantApplicationUtils {

	private static Logger logger = LoggerFactory.getLogger(ApsTenantApplicationUtils.class);

	private ApsTenantApplicationUtils(){}

	public static Optional<String> extractCurrentTenantCode(HttpServletRequest request) {
		String tenantCode = fetchTenantCodeFromEntandoHeader(request);
		System.out.println(" *** TenantApplicationUtils *** REQUEST CLASS: " + request.getClass().getName());
		if(StringUtils.isNotBlank(tenantCode)) {
			if(StringUtils.equalsIgnoreCase(tenantCode, PRIMARY_CODE)) {
				logger.debug("the tenantCode:'{}' contains primary code, return empty", tenantCode);
				return Optional.empty();
			} else {
				logger.debug("the tenantCode:'{}' contains a NOT primary code, return it", tenantCode);
				return Optional.of(tenantCode);
			}
		} else {
			logger.debug("the custom header:'{}' is empty or blank, skip it", UrlUtils.ENTANDO_TENANT_CODE_CUSTOM_HEADER);
			String domain = getDomainFromRequest(request);
			//String contextPath = request.getContextPath();
			//String context = contextPath.startsWith("/") ? contextPath.replaceFirst("/", "") : contextPath;
			String context = request.getParameter("virtual-context");
			System.out.println(" *** TenantApplicationUtils *** CONTEXT: `" + context + "`");
			ITenantManager tenantManager = ApsWebApplicationUtils.getBean(ITenantManager.class, request);
			String tenantCodeFromDomainAndContext = tenantManager.getTenantCodeByDomainAndContext(domain, context);
			System.out.println(" *** TenantApplicationUtils *** TENANT CODE: `" + tenantCodeFromDomainAndContext + "`");
			return Optional.ofNullable(tenantCodeFromDomainAndContext);
		}
	}

	private static String fetchTenantCodeFromEntandoHeader(HttpServletRequest request) {
		String tenantCode = UrlUtils.fetchTenantCode(request);
		logger.debug("Retrieved from custom header:'{}' the tenantCode:'{}'", UrlUtils.ENTANDO_TENANT_CODE_CUSTOM_HEADER, tenantCode);
		return tenantCode;
	}

	private static String getDomainFromRequest(HttpServletRequest request){
		String serverName = UrlUtils.fetchServer(request);
		logger.debug("Retrieved from serverName:'{}' the domain:'{}'", request.getServerName(), serverName);
		return serverName;
	}

	public static Optional<String> getTenant() {
		return Optional.ofNullable((String) EntThreadLocal.get(ITenantManager.THREAD_LOCAL_TENANT_CODE));
	}

	public static void setTenant(String value) {
		logger.debug("set tenant:'{}' into ThreadLocal", value);
		EntThreadLocal.set(ITenantManager.THREAD_LOCAL_TENANT_CODE, value);
	}

	public static void removeTenant() {
		EntThreadLocal.remove(ITenantManager.THREAD_LOCAL_TENANT_CODE);
	}


}
