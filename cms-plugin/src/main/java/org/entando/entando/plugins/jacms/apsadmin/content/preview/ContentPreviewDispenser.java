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
package org.entando.entando.plugins.jacms.apsadmin.content.preview;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeRole;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.user.UserDetails;

import com.agiletec.plugins.jacms.aps.system.services.content.helper.PublicContentAuthorizationInfo;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.dispenser.BaseContentDispenser;
import com.agiletec.plugins.jacms.aps.system.services.dispenser.ContentRenderizationInfo;
import com.agiletec.plugins.jacms.apsadmin.content.ContentActionConstants;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

/**
 * @author E.Santoboni
 */
public class ContentPreviewDispenser extends BaseContentDispenser {

    private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(ContentPreviewDispenser.class);

    private ILangManager langManager;

    public ContentRenderizationInfo getRenderizationInfoForPreview(String contentId, long modelId, String langCode, RequestContext reqCtx) {
        PublicContentAuthorizationInfo authInfo = null;
        try {
            Content content = this.extractContentOnSession(reqCtx);
            authInfo = new PublicContentAuthorizationInfo(content, this.getLangManager().getLangs());
        } catch (Throwable t) {
            _logger.error("error in getAuthorizationInfo for content {}", contentId, t);
        }
        return this.getRenderizationInfo(authInfo, contentId, modelId, langCode, reqCtx, false);
    }

    @Override
    public ContentRenderizationInfo getBaseRenderizationInfo(PublicContentAuthorizationInfo authInfo,
            String contentId, long modelId, String langCode, UserDetails currentUser, RequestContext reqCtx) {
        ContentRenderizationInfo renderInfo = null;
        try {
            List<Group> userGroups = (null != currentUser) ? this.getAuthorizationManager().getUserGroups(currentUser) : new ArrayList<>();
            if (authInfo.isUserAllowed(userGroups)) {
                Content contentOnSession = this.extractContentOnSession(reqCtx);
                String renderedContent = this.buildRenderedContent(contentOnSession, modelId, langCode, reqCtx);
                if (null != renderedContent && renderedContent.trim().length() > 0) {
                    List<AttributeRole> roles = this.getContentManager().getAttributeRoles();
                    renderInfo = new ContentRenderizationInfo(contentOnSession, renderedContent, modelId, langCode, roles);
                }
            }
        } catch (Throwable t) {
            _logger.error("Error while rendering content {}", contentId, t);
            return null;
        }
        return renderInfo;
    }

    private Content extractContentOnSession(RequestContext reqCtx) {
        HttpServletRequest request = reqCtx.getRequest();
        String contentOnSessionMarker = (String) request.getAttribute("contentOnSessionMarker");
        if (null == contentOnSessionMarker || contentOnSessionMarker.trim().length() == 0) {
            contentOnSessionMarker = request.getParameter("contentOnSessionMarker");
        }
        return (Content) request.getSession()
                .getAttribute(ContentActionConstants.SESSION_PARAM_NAME_CURRENT_CONTENT_PREXIX + contentOnSessionMarker);
    }

    protected ILangManager getLangManager() {
        return langManager;
    }
    public void setLangManager(ILangManager langManager) {
        this.langManager = langManager;
    }

}
