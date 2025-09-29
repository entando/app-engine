/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.entando.entando.plugins.jpseo.aps.system.services.url;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.plugins.jpseo.aps.system.services.mapping.ISeoMappingManager;
import org.entando.entando.plugins.jpseo.aps.system.services.page.SeoPageMetadata;

/**
 * @author E.Santoboni
 */
public class URLManager extends com.agiletec.aps.system.services.url.URLManager {
    
    private static final EntLogger _logger =  EntLogFactory.getSanitizedLogger(URLManager.class);

    private ISeoMappingManager seoMappingManager;
    private IContentManager contentManager;

    @Override
    public PageURL createURL(RequestContext reqCtx) {
        return new PageURL(this, reqCtx);
    }

    @Override
    public String getURLString(com.agiletec.aps.system.services.url.PageURL pageUrl, RequestContext reqCtx) {
        try {
            if (!(pageUrl instanceof PageURL)) {
                return super.getURLString(pageUrl, reqCtx);
            }
            Lang lang = this.extractLang(pageUrl, reqCtx);
            IPage destPage = this.extractDestPage(pageUrl, reqCtx);
            String friendlyCode = ((PageURL) pageUrl).getFriendlyCode();
            if (StringUtils.isBlank(friendlyCode)) {
                friendlyCode = this.extractFriendlyCode(destPage, lang, pageUrl);
            }
            HttpServletRequest request = (null != reqCtx) ? reqCtx.getRequest() : null;
            String url = null;
            if (StringUtils.isBlank(friendlyCode)) {
                url = super.createURL(destPage, lang, pageUrl.getParams(), pageUrl.isEscapeAmp(), pageUrl.getBaseUrlMode(), request);
            } else {
                url = this.createFriendlyUrl(friendlyCode, lang, pageUrl.getParams(), pageUrl.isEscapeAmp(), pageUrl.getBaseUrlMode(), request);
            }
            if (null != reqCtx && null != reqCtx.getResponse() && this.useJsessionId()) {
                HttpServletResponse resp = reqCtx.getResponse();
                return resp.encodeURL(url.toString());
            } else {
                return url;
            }
        } catch (Exception e) {
            _logger.error("Error creating url", e);
            throw new RuntimeException("Error creating url", e);
        }
    }

    private Lang extractLang(com.agiletec.aps.system.services.url.PageURL pageUrl, RequestContext reqCtx) {
        Lang lang = null;
        String langCode = pageUrl.getLangCode();
        if (null != langCode) {
            lang = this.getLangManager().getLang(langCode);
        }
        if (lang == null && null != reqCtx) {
            lang = (Lang) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG);
        }
        if (lang == null) {
            lang = this.getLangManager().getDefaultLang();
        }
        return lang;
    }

    private IPage extractDestPage(com.agiletec.aps.system.services.url.PageURL pageUrl, RequestContext reqCtx) {
        IPage page = null;
        String pageCode = pageUrl.getPageCode();
        if (pageCode != null) {
            page = this.getPageManager().getOnlinePage(pageCode);
        }
        if (page == null && null != reqCtx) {
            page = (IPage) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE);
        }
        if (page == null) {
            page = this.getPageManager().getOnlineRoot();
        }
        return page;
    }

    private String extractFriendlyCode(IPage destPage, Lang lang, com.agiletec.aps.system.services.url.PageURL pageUrl) {
        Map params = pageUrl.getParams();
        return this.extractFriendlyCode(destPage, lang, params);
    }

    private String extractFriendlyCode(IPage destPage, Lang lang, Map params) {
        String friendlyCode = null;
        if (params != null) {
            String contentId = (String) params.get("contentId");
            if (contentId!=null) {
                String viewPageCode = this.getContentManager().getViewPage(contentId);
                if (destPage.getCode().equals(viewPageCode)) {
                    friendlyCode = this.getSeoMappingManager().getContentReference(contentId, lang.getCode());
                }
            }
        }
        if (friendlyCode==null && destPage.getMetadata() instanceof SeoPageMetadata) {
            friendlyCode = ((SeoPageMetadata) destPage.getMetadata()).getFriendlyCode(lang.getCode());
        }
        return friendlyCode;
    }

    @Override
    public String createURL(IPage requiredPage, Lang requiredLang, Map<String, String> params, boolean escapeAmp) {
        return this.createURL(requiredPage, requiredLang, params, escapeAmp, null);
    }

    @Override
    public String createURL(IPage requiredPage, Lang requiredLang, Map<String, String> params, boolean escapeAmp, HttpServletRequest request) {
        try {
            String friendlyCode = this.extractFriendlyCode(requiredPage, requiredLang, params);
            if (StringUtils.isBlank(friendlyCode)) {
                return super.createURL(requiredPage, requiredLang, params, escapeAmp, request);
            }
            return this.createFriendlyUrl(friendlyCode, requiredLang, params, escapeAmp, null, request);
        } catch (Throwable t) {
            _logger.error("Error creating url", t);
            throw new RuntimeException("Error creating url", t);
        }
    }

    protected String createFriendlyUrl(String friendlyCode, Lang requiredLang, Map<String, String> params, boolean escapeAmp, HttpServletRequest request) {
        return this.createFriendlyUrl(friendlyCode, requiredLang, params, escapeAmp, null, request);
    }

    protected String createFriendlyUrl(String friendlyCode, Lang requiredLang, Map<String, String> params, boolean escapeAmp, String forcedBaseUrlMode, HttpServletRequest request) {
        StringBuilder url = new StringBuilder();
        try {
            this.addBaseURL(url, forcedBaseUrlMode, request);
            if (!url.toString().endsWith("/")) {
                url.append("/");
            }
            url.append("page/");
            url.append(requiredLang.getCode()).append('/');
            url.append(friendlyCode);
            String queryString = this.createQueryString(params, escapeAmp);
            url.append(queryString);
        } catch (Exception t) {
            _logger.error("Error creating friendly url", t);
            throw new RuntimeException("Error creating friendly url", t);
        }
        return url.toString();
    }

    protected ISeoMappingManager getSeoMappingManager() {
        return seoMappingManager;
    }
    public void setSeoMappingManager(ISeoMappingManager seoMappingManager) {
        this.seoMappingManager = seoMappingManager;
    }

    protected IContentManager getContentManager() {
        return contentManager;
    }
    public void setContentManager(IContentManager contentManager) {
        this.contentManager = contentManager;
    }

}
