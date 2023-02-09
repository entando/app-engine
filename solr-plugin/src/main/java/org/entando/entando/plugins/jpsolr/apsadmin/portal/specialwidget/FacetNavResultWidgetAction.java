/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpsolr.apsadmin.portal.specialwidget;

import com.agiletec.apsadmin.portal.specialwidget.SimpleWidgetConfigAction;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.content.model.SmallContentType;
import java.util.List;
import java.util.Map;
import org.entando.entando.aps.system.services.widgettype.WidgetType;
import org.entando.entando.aps.system.services.widgettype.WidgetTypeParameter;
import org.entando.entando.plugins.jpsolr.aps.system.JpSolrSystemConstants;
import org.entando.entando.plugins.jpsolr.apsadmin.portal.specialwidget.util.FacetNavWidgetHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author E.Santoboni
 */
public class FacetNavResultWidgetAction extends SimpleWidgetConfigAction {

    private static final Logger logger = LoggerFactory.getLogger(FacetNavResultWidgetAction.class);

    @Override
    public void validate() {
        try {
            super.validate();
            this.createValuedShowlet();
            this.validateContentTypes();
        } catch (RuntimeException ex) {
            logger.error("Exception in validate", ex);
        }
    }

    protected void validateContentTypes() {
        List<String> contentTypes = this.getContentTypeCodes();
        Map<String, SmallContentType> smallContentTypes = this.getContentManager().getSmallContentTypesMap();
        for (String typeCode : contentTypes) {
            if (!smallContentTypes.containsKey(typeCode)) {
                String[] args = {typeCode};
                String fieldName = JpSolrSystemConstants.CONTENT_TYPES_FILTER_WIDGET_PARAM_NAME;
                this.addFieldError(fieldName, this.getText("message.facetNavWidget.contentTypesFilter.notValid", args));
            }
        }
    }

    @Override
    public String init() {
        String result = super.init();
        try {
            if (result.equals(SUCCESS)) {
                this.initSpecialParams();
            }
        } catch (RuntimeException ex) {
            logger.error("Exception in init", ex);
            return FAILURE;
        }
        return result;
    }

    /**
     * Add a content type to the associated content types
     *
     * @return The code describing the result of the operation.
     */
    public String joinContentType() {
        try {
            this.createValuedShowlet();
            List<String> contentTypes = this.getContentTypeCodes();
            String typeCode = this.getContentTypeCode();
            if (typeCode != null && typeCode.length() > 0 && !contentTypes.contains(typeCode)
                    && this.getContentType(typeCode) != null) {
                contentTypes.add(typeCode);
                String typesFilter = FacetNavWidgetHelper.concatStrings(contentTypes, ",");
                String configParamName = JpSolrSystemConstants.CONTENT_TYPES_FILTER_WIDGET_PARAM_NAME;
                this.getWidget().getConfig().setProperty(configParamName, typesFilter);
                this.setContentTypesFilter(typesFilter);
            }
        } catch (RuntimeException ex) {
            logger.error("Exception in joinContentType", ex);
            return FAILURE;
        }
        return SUCCESS;
    }

    /**
     * Remove a content type from the associated content types
     *
     * @return The code describing the result of the operation.
     */
    public String removeContentType() {
        try {
            this.createValuedShowlet();
            List<String> contentTypes = this.getContentTypeCodes();
            String typeCode = this.getContentTypeCode();
            if (typeCode != null) {
                contentTypes.remove(typeCode);
                String typesFilter = FacetNavWidgetHelper.concatStrings(contentTypes, ",");
                String configParamName = JpSolrSystemConstants.CONTENT_TYPES_FILTER_WIDGET_PARAM_NAME;
                this.getWidget().getConfig().setProperty(configParamName, typesFilter);
                this.setContentTypesFilter(typesFilter);
            }
        } catch (RuntimeException ex) {
            logger.error("Exception in removeContentType", ex);
            return FAILURE;
        }
        return SUCCESS;
    }

    /**
     * Prepare action with the parameters contained into widget.
     */
    protected void initSpecialParams() {
        if (null != this.getWidget().getConfig()) {
            String paramName = JpSolrSystemConstants.CONTENT_TYPES_FILTER_WIDGET_PARAM_NAME;
            String configParamName = this.getWidget().getConfig().getProperty(paramName);
            this.setContentTypesFilter(configParamName);
        }
    }

    /**
     * Returns widget type parameter
     *
     * @param paramName
     * @return the Widget type parameter
     */
    public WidgetTypeParameter getWidgetTypeParameter(String paramName) {
        WidgetType widgetType = this.getWidgetTypeManager().getWidgetType(this.getWidget().getTypeCode());
        List<WidgetTypeParameter> parameters = widgetType.getTypeParameters();
        for (WidgetTypeParameter param : parameters) {
            if (param.getName().equals(paramName)) {
                return param;
            }
        }
        return null;
    }

    public List<SmallContentType> getContentTypes() {
        return this.getContentManager().getSmallContentTypes();
    }

    public SmallContentType getContentType(String contentTypeCode) {
        return this.getContentManager().getSmallContentTypesMap().get(contentTypeCode);
    }

    public String getContentTypeCode() {
        return contentTypeCode;
    }

    public void setContentTypeCode(String contentTypeCode) {
        this.contentTypeCode = contentTypeCode;
    }

    public List<String> getContentTypeCodes() {
        String contentTypesParam = this.getContentTypesFilter();
        return FacetNavWidgetHelper.splitValues(contentTypesParam, ",");
    }

    public void setContentTypesFilter(String contentTypesFilter) {
        this.contentTypesFilter = contentTypesFilter;
    }

    public String getContentTypesFilter() {
        return contentTypesFilter;
    }

    protected IContentManager getContentManager() {
        return contentManager;
    }

    public void setContentManager(IContentManager contentManager) {
        this.contentManager = contentManager;
    }

    private String contentTypeCode;
    private String contentTypesFilter;

    private IContentManager contentManager;

}