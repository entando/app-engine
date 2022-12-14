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
package org.entando.entando.plugins.jacms.aps.servlet;

import com.agiletec.aps.system.RequestContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.entando.entando.aps.servlet.IProtectedResourceProvider;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.aps.system.SystemConstants;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.url.IURLManager;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.plugins.jacms.aps.system.services.content.helper.IContentAuthorizationHelper;
import com.agiletec.plugins.jacms.aps.system.services.content.helper.PublicContentAuthorizationInfo;
import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.AbstractResourceAttribute;
import com.agiletec.plugins.jacms.aps.system.services.resource.IResourceManager;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.AbstractMonoInstanceResource;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.AbstractMultiInstanceResource;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ResourceInstance;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ResourceInterface;

/**
 * Provider bean of protected cms resources.
 *
 * @author E.Santoboni
 */
public class ProtectedResourceProvider implements IProtectedResourceProvider {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(ProtectedResourceProvider.class);

    private IUserManager userManager;
    private IAuthorizationManager authorizationManager;
    private IURLManager urlManager;
    private IPageManager pageManager;
    private ILangManager langManager;
    private IResourceManager resourceManager;
    private IContentAuthorizationHelper contentAuthorizationHelper;

    @Override
    public boolean provideProtectedResource(HttpServletRequest request, HttpServletResponse response) throws EntException {
        try {
            String[] uriSegments = request.getRequestURI().split("/");
            int segments = uriSegments.length;
            //CONTROLLO ASSOCIAZIONE RISORSA A CONTENUTO
            int indexGuardian = 0;
            String checkContentAssociation = uriSegments[segments - 2];
            if (checkContentAssociation.equals(AbstractResourceAttribute.REFERENCED_RESOURCE_INDICATOR)) {
                // LA Sintassi /<RES_ID>/<SIZE>/<LANG_CODE>/<REFERENCED_RESOURCE_INDICATOR>/<CONTENT_ID>
                indexGuardian = 2;
            }
            UserDetails currentUser = (UserDetails) request.getSession().getAttribute(SystemConstants.SESSIONPARAM_CURRENT_USER);
            if (currentUser == null) {
                currentUser = this.getUserManager().getGuestUser();
            }
            String resId = uriSegments[segments - 3 - indexGuardian];
            boolean isAuthForProtectedRes = false;
            if (indexGuardian != 0) {
                if (this.isAuthOnProtectedRes(currentUser, resId, uriSegments[segments - 1])) {
                    isAuthForProtectedRes = true;
                } else {
                    return handleRedirect(currentUser, request, response);
                }
            }
            ResourceInterface resource = this.getResourceManager().loadResource(resId);
            if (resource == null) {
                this.executeNotFoundRedirect(request, response);
                return false;
            }
            IAuthorizationManager authManager = this.getAuthorizationManager();
            if (isAuthForProtectedRes
                    || authManager.isAuthOnGroup(currentUser, resource.getMainGroup())
                    || authManager.isAuthOnGroup(currentUser, Group.ADMINS_GROUP_NAME)) {
                ResourceInstance instance = null;
                if (resource.isMultiInstance()) {
                    String sizeStr = uriSegments[segments - 2 - indexGuardian];
                    if (!this.isValidNumericString(sizeStr)) {
                        return false;
                    }
                    int size = Integer.parseInt(sizeStr);
                    String langCode = uriSegments[segments - 1 - indexGuardian];
                    instance = ((AbstractMultiInstanceResource) resource).getInstance(size, langCode);
                } else {
                    instance = ((AbstractMonoInstanceResource) resource).getInstance();
                }
                this.createResponse(response, resource, instance);
                return true;
            } else {
                return handleRedirect(currentUser, request, response);
            }
        } catch (Throwable t) {
            logger.error("Error extracting protected resource", t);
            throw new EntException("Error extracting protected resource", t);
        }
    }

    protected boolean isAuthOnProtectedRes(UserDetails currentUser, String resourceId, String contentId) {
        PublicContentAuthorizationInfo authInfo = this.getContentAuthorizationHelper().getAuthorizationInfo(contentId);
        if (null == authInfo) {
            return false;
        }
        IAuthorizationManager authManager = this.getAuthorizationManager();
        return (authInfo.isProtectedResourceReference(resourceId) && authInfo.isUserAllowed(authManager.getUserGroups(currentUser)));
    }

    protected void createResponse(HttpServletResponse resp, ResourceInterface resource,
            ResourceInstance instance) throws IOException, ServletException {
        resp.setContentType(instance.getMimeType());
        resp.setHeader("Content-Disposition", "inline; filename=" + instance.getFileName());
        ServletOutputStream out = resp.getOutputStream();
        try {
            InputStream is = resource.getResourceStream(instance);
            if (null != is) {
                byte[] buffer = new byte[2048];
                int length = -1;
                // Transfer the data
                while ((length = is.read(buffer)) != -1) {
                    out.write(buffer, 0, length);
                    out.flush();
                }
                is.close();
            }
        } catch (Throwable t) {
            logger.error("Error extracting protected resource", t);
            throw new ServletException("Error extracting protected resource", t);
        } finally {
            out.close();
        }
    }
    
    protected boolean handleRedirect(UserDetails currentUser, HttpServletRequest request, HttpServletResponse response) throws ServletException {
        if (null == currentUser || currentUser.getUsername().equals(SystemConstants.GUEST_USER_NAME)) {
            this.executeNotFoundRedirect(request, response);
        } else {
            this.executeErrorRedirect(request, response);
        }
        return true;
    }

    protected void executeErrorRedirect(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        Map<String, String> params = new HashMap<>();
        params.put(RequestContext.PAR_REDIRECT_FLAG, "1");
        params.put("userUnauthorized", Boolean.TRUE.toString());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        this.executeRedirect(request, response, IPageManager.CONFIG_PARAM_ERROR_PAGE_CODE, params);
    }

    protected void executeNotFoundRedirect(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        this.executeRedirect(request, response, IPageManager.CONFIG_PARAM_NOT_FOUND_PAGE_CODE, null);
    }

    protected void executeRedirect(HttpServletRequest request, HttpServletResponse response, String destPageParam, Map<String, String> params) throws ServletException {
        try {
            String pageCode = this.getPageManager().getConfig(destPageParam);
            IPage page = this.getPageManager().getOnlinePage(pageCode);
            if (null == page) {
                throw new EntException("Destination target null - param " + destPageParam + " - pagecode " + pageCode);
            }
            Lang defaultLang = this.getLangManager().getDefaultLang();
            String url = this.getUrlManager().createURL(page, defaultLang, params, false, request);
            response.sendRedirect(url);
        } catch (Exception t) {
            logger.error("Error executing redirect", t);
            throw new ServletException("Error executing redirect", t);
        }
    }

    protected boolean isValidNumericString(String integerNumber) {
        return (integerNumber.trim().length() > 0 && integerNumber.matches("\\d+"));
    }

    protected IResourceManager getResourceManager() {
        return resourceManager;
    }
    public void setResourceManager(IResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    protected IContentAuthorizationHelper getContentAuthorizationHelper() {
        return contentAuthorizationHelper;
    }
    public void setContentAuthorizationHelper(IContentAuthorizationHelper contentAuthorizationHelper) {
        this.contentAuthorizationHelper = contentAuthorizationHelper;
    }

    protected IUserManager getUserManager() {
        return userManager;
    }
    public void setUserManager(IUserManager userManager) {
        this.userManager = userManager;
    }

    protected IAuthorizationManager getAuthorizationManager() {
        return authorizationManager;
    }
    public void setAuthorizationManager(IAuthorizationManager authorizationManager) {
        this.authorizationManager = authorizationManager;
    }

    protected IURLManager getUrlManager() {
        return urlManager;
    }
    public void setUrlManager(IURLManager urlManager) {
        this.urlManager = urlManager;
    }

    protected IPageManager getPageManager() {
        return pageManager;
    }
    public void setPageManager(IPageManager pageManager) {
        this.pageManager = pageManager;
    }

    protected ILangManager getLangManager() {
        return langManager;
    }
    public void setLangManager(ILangManager langManager) {
        this.langManager = langManager;
    }

}
