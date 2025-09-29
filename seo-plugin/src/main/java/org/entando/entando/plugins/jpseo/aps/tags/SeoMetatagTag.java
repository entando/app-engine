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
package org.entando.entando.plugins.jpseo.aps.tags;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.jsp.JspException;

import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeRole;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.PageMetadata;
import com.agiletec.aps.util.ApsProperties;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.tags.ExtendedTagSupport;
import org.entando.entando.plugins.jpseo.aps.system.services.metatag.Metatag;
import org.entando.entando.plugins.jpseo.aps.system.services.page.PageMetatag;
import org.entando.entando.plugins.jpseo.aps.system.services.page.SeoPageMetadata;

public class SeoMetatagTag extends ExtendedTagSupport {

    private static final EntLogger _logger =  EntLogFactory.getSanitizedLogger(SeoMetatagTag.class);

    @Override
    public int doEndTag() throws JspException {
        ServletRequest request = this.pageContext.getRequest();
        RequestContext reqCtx = (RequestContext) request.getAttribute(RequestContext.REQCTX);
        Lang currentLang = (Lang) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG);
        try {
            IPage page = (IPage) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE);
            PageMetadata pageMetadata = page.getMetadata();
            if (!(pageMetadata instanceof SeoPageMetadata)) {
                this.release();
                return EVAL_PAGE;
            }
            if (CurrentPageTag.DESCRIPTION_INFO.equals(this.getKey())) {
                ApsProperties descriptions = ((SeoPageMetadata) pageMetadata).getDescriptions();
                this.setSeoValue(descriptions, currentLang);
            } else if (CurrentPageTag.KEYWORDS_INFO.equals(this.getKey())) {
                ApsProperties keywords = ((SeoPageMetadata) pageMetadata).getKeywords();
                this.setSeoValue(keywords, currentLang);
            } else if (CurrentPageTag.FRIENDLY_CODE_INFO.equals(this.getKey())) {
                ApsProperties friendlyCodes = ((SeoPageMetadata) pageMetadata).getFriendlyCodes();
                this.setSeoValue(friendlyCodes, currentLang);
            } else {
                ILangManager langManager = (ILangManager) ApsWebApplicationUtils.getBean(SystemConstants.LANGUAGE_MANAGER, this.pageContext);
                IContentManager contentManager = (IContentManager) ApsWebApplicationUtils.getBean(JacmsSystemConstants.CONTENT_MANAGER, this.pageContext);
                AttributeRole role = contentManager.getAttributeRole(this.getKey());
                if (null != role) {
                    this.manageAttributeWithRole(currentLang, langManager, reqCtx);
                } else {
                    this.manageComplexParameters(currentLang, langManager, pageMetadata);
                }
            }
            if (null != this.getValue()) {
                this.evalValue();
            }
        } catch (Exception t) {
            _logger.error("error in doEndTag", t);
            throw new JspException("Error in doEndTag", t);
        }
        this.release();
        return EVAL_PAGE;
    }
    
    protected void manageAttributeWithRole(Lang currentLang, ILangManager langManager, RequestContext reqCtx) {
        Serializable attrValue = (Serializable) reqCtx.getExtraParam(JacmsSystemConstants.ATTRIBUTE_WITH_ROLE_CTX_PREFIX + this.getKey());
        if (null != attrValue) {
            if (attrValue instanceof Map) {
                Lang lang = (null == currentLang) ? langManager.getDefaultLang() : currentLang;
                Object elementValue = ((Map) attrValue).get(lang.getCode());
                if (null == elementValue) {
                    Lang defaultLang = langManager.getDefaultLang();
                    elementValue = ((Map) attrValue).get(defaultLang.getCode());
                }
                if (null != elementValue) {
                    this.setValue(elementValue);
                }
            } else {
                this.setValue(attrValue);
            }
            this.setAttributeName(Metatag.ATTRIBUTE_NAME_NAME);
        }
    }
    
    protected void manageComplexParameters(Lang currentLang, ILangManager langManager, PageMetadata pageMetadata) {
        Map<String, Map<String, PageMetatag>> complexParameters = ((SeoPageMetadata) pageMetadata).getComplexParameters();
        if (null != complexParameters) {
            Lang lang = (null == currentLang) ? langManager.getDefaultLang() : currentLang;
            Map<String, PageMetatag> mapValue = complexParameters.get(lang.getCode());
            Map<String, PageMetatag> defaultMapValue = complexParameters.get("default");
            if (null == defaultMapValue) {
                Lang defaultLang = langManager.getDefaultLang();
                defaultMapValue = complexParameters.get(defaultLang.getCode());
            }
            if (null != mapValue) {
                PageMetatag pageMetatag = mapValue.get(this.getKey());
                if (null != pageMetatag && !pageMetatag.isUseDefaultLangValue() && !StringUtils.isBlank(pageMetatag.getValue())) {
                    this.setValue(pageMetatag.getValue());
                    this.setAttributeName(pageMetatag.getKeyAttribute());
                }
            }
            if (null == this.getValue() && null != defaultMapValue) {
                PageMetatag pageMetatag = defaultMapValue.get(this.getKey());
                if (null != pageMetatag && !StringUtils.isBlank(pageMetatag.getValue())) {
                    this.setValue(pageMetatag.getValue());
                    this.setAttributeName(pageMetatag.getKeyAttribute());
                }
            }
        }
    }

    protected void setSeoValue(ApsProperties properties, Lang currentLang) {
        PageMetatag metatag = null;
        if (currentLang != null) {
            metatag = (PageMetatag) properties.get(currentLang.getCode());
        } else {
            ILangManager langManager = (ILangManager) ApsWebApplicationUtils.getBean(SystemConstants.LANGUAGE_MANAGER, this.pageContext);
            metatag = (PageMetatag) properties.get(langManager.getDefaultLang().getCode());
        }
        if (null != metatag) {
            this.setValue(metatag.getValue());
        }
    }

    protected void evalValue() throws JspException {
        if (this.getVar() != null) {
            this.pageContext.setAttribute(this.getVar(), this.getValue());
            if (null != this.getAttributeName()) {
                this.pageContext.setAttribute(this.getKey()+"_attribute", this.getAttributeName());
            }
        } else {
            try {
                if (this.getEscapeXml()) {
                    out(this.pageContext, this.getEscapeXml(), this.getValue());
                } else {
                    this.pageContext.getOut().print(this.getValue());
                }
            } catch (IOException e) {
                _logger.error("error in doEndTag", e);
                throw new JspException("Error closing tag ", e);
            }
        }
    }

    @Override
    public void release() {
        this.key = null;
        this.pgcVar = null;
        this.value = null;
        this.attributeName = null;
        super.setEscapeXml(true);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setVar(String pgcVar) {
        this.pgcVar = pgcVar;
    }

    protected String getVar() {
        return pgcVar;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    private String key;

    private String pgcVar;
    private transient Object value;
    private String attributeName;

}
