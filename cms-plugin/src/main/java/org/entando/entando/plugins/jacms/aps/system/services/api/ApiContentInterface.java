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
package org.entando.entando.plugins.jacms.aps.system.services.api;

import com.agiletec.aps.system.RequestContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.api.IApiErrorCodes;
import org.entando.entando.aps.system.services.api.model.LegacyApiError;
import org.entando.entando.aps.system.services.api.model.ApiException;
import org.entando.entando.aps.system.services.api.model.StringApiResponse;
import org.entando.entando.aps.system.services.api.server.IResponseBuilder;
import org.entando.entando.plugins.jacms.aps.system.services.api.model.ApiContentListBean;
import org.entando.entando.plugins.jacms.aps.system.services.api.model.JAXBContent;
import org.entando.entando.plugins.jacms.aps.system.services.api.model.JAXBContentAttribute;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.entity.model.AttributeFieldError;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.common.entity.model.FieldError;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.ITextAttribute;
import org.entando.entando.ent.exception.EntException;
import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.aps.system.services.group.IGroupManager;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.agiletec.plugins.jacms.aps.system.services.content.ContentUtilizer;
import com.agiletec.plugins.jacms.aps.system.services.content.helper.IContentAuthorizationHelper;
import com.agiletec.plugins.jacms.aps.system.services.content.helper.IContentListHelper;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.content.model.ContentRecordVO;
import com.agiletec.plugins.jacms.aps.system.services.contentmodel.ContentModel;
import com.agiletec.plugins.jacms.aps.system.services.dispenser.ContentRenderizationInfo;
import com.agiletec.plugins.jacms.aps.system.services.dispenser.IContentDispenser;
import com.agiletec.plugins.jacms.aps.system.services.resource.IResourceManager;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import org.entando.entando.plugins.jacms.aps.system.services.api.model.CmsApiResponse;
import org.entando.entando.plugins.jacms.aps.system.services.api.model.JAXBCmsResult;
import org.springframework.http.HttpStatus;

/**
 * @author E.Santoboni
 */
public class ApiContentInterface extends AbstractCmsApiInterface {

    private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(ApiContentInterface.class);

    public List<String> getContents(Properties properties) throws ApiException, EntException {
        return this.extractContents(properties);
    }

    protected List<String> extractContents(Properties properties) throws ApiException, EntException {
        List<String> contentsId;
        try {
            ApiContentListBean bean = this.buildSearchBean(properties);
            UserDetails user = (UserDetails) properties.get(SystemConstants.API_USER_PARAMETER);
            contentsId = this.getContentListHelper().getContentsId(bean, user);
        } catch (ApiException ae) {
            throw ae;
        } catch (Throwable t) {
            _logger.error("error in extractContents", t);
            throw new EntException("Error into API method", t);
        }
        return contentsId;
    }

    protected ApiContentListBean buildSearchBean(Properties properties) throws ApiException, EntException {
        ApiContentListBean bean;
        try {
            String contentType = properties.getProperty("contentType");
            if (null == this.getContentManager().getSmallContentTypesMap().get(contentType)) {
                throw new ApiException(IApiErrorCodes.API_PARAMETER_VALIDATION_ERROR, "Content Type '" + contentType + "' does not exist", HttpStatus.CONFLICT);
            }
            String langCode = properties.getProperty(SystemConstants.API_LANG_CODE_PARAMETER);
            String filtersParam = properties.getProperty("filters");
            if (!StringUtils.isEmpty(filtersParam)) {
                filtersParam = URLDecoder.decode(filtersParam, StandardCharsets.UTF_8.toString());
            }
            EntitySearchFilter[] filters = this.getContentListHelper().getFilters(contentType, filtersParam, langCode);
            String[] categoryCodes = null;
            String categoriesParam = properties.getProperty("categories");
            boolean orClause = false;
            if (!StringUtils.isEmpty(categoriesParam)) {
                categoryCodes = categoriesParam.split(IContentListHelper.CATEGORIES_SEPARATOR);
                String orClauseString = properties.getProperty("orClauseCategoryFilter");
                orClause = !StringUtils.isEmpty(orClauseString) && orClauseString.trim().equalsIgnoreCase("true");
            }
            bean = new ApiContentListBean(contentType, filters, categoryCodes, orClause);
        } catch (ApiException ae) {
            throw ae;
        } catch (Throwable t) {
            _logger.error("error in buildSearchBean", t);
            throw new EntException("Error into API method", t);
        }
        return bean;
    }

    public String getContentsToHtml(Properties properties) throws ApiException, EntException {
        StringBuilder render = new StringBuilder();
        try {
            String modelId = properties.getProperty("modelId");
            if (null == modelId || modelId.trim().length() == 0) {
                return null;
            }
            String contentType = properties.getProperty("contentType");
            Content prototype = (Content) this.getContentManager().getEntityPrototype(contentType);
            Integer modelIdInteger = this.checkModel(modelId, prototype);
            if (null == modelIdInteger) {
                return null;
            }
            List<String> contentsId = this.extractContents(properties);
            HttpServletRequest request = (HttpServletRequest) properties.get(SystemConstants.API_REQUEST_PARAMETER);
            String langCode = properties.getProperty(SystemConstants.API_LANG_CODE_PARAMETER);
            render.append(this.getItemsStartElement());
            for (int i = 0; i < contentsId.size(); i++) {
                render.append(this.getItemStartElement());
                String renderedContent = this.getRenderedContent(contentsId.get(i), modelIdInteger, langCode, request);
                if (null != renderedContent) {
                    render.append(renderedContent);
                }
                render.append(this.getItemEndElement());
            }
            render.append(this.getItemsEndElement());
        } catch (ApiException ae) {
            throw ae;
        } catch (Throwable t) {
            _logger.error("error in getContentsToHtml", t);
            throw new EntException("Error into API method", t);
        }
        return render.toString();
    }

    public JAXBContent getContent(Properties properties) throws ApiException, EntException {
        JAXBContent jaxbContent;
        String id = properties.getProperty("id");
        try {
            String langCode = properties.getProperty(SystemConstants.API_LANG_CODE_PARAMETER);
            Content mainContent = this.getPublicContent(id);
            jaxbContent = this.getJAXBContentInstance(mainContent, langCode);
            UserDetails user = (UserDetails) properties.get(SystemConstants.API_USER_PARAMETER);
            if (null == user) {
                user = this.getUserManager().getGuestUser();
            }
            if (!this.getContentAuthorizationHelper().isAuth(user, mainContent)) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "Required content '" + id + "' is not allowed", HttpStatus.FORBIDDEN);
            }
        } catch (ApiException ae) {
            throw ae;
        } catch (Throwable t) {
            _logger.error("error in getContent", t);
            throw new EntException("Error into API method", t);
        }
        return jaxbContent;
    }

    protected JAXBContent getJAXBContentInstance(Content mainContent, String langCode) {
        return new JAXBContent(mainContent, langCode);
    }

    public String getContentToHtml(Properties properties) throws ApiException, EntException {
        String render = null;
        String id = properties.getProperty("id");
        String modelId = properties.getProperty("modelId");
        try {
            if (null == modelId || modelId.trim().length() == 0) {
                return null;
            }
            Content mainContent = this.getPublicContent(id);
            Integer modelIdInteger = this.checkModel(modelId, mainContent);
            if (null != modelIdInteger) {
                String langCode = properties.getProperty(SystemConstants.API_LANG_CODE_PARAMETER);
                HttpServletRequest request = (HttpServletRequest) properties.get(SystemConstants.API_REQUEST_PARAMETER);
                render = this.getRenderedContent(id, modelIdInteger, langCode, request);
            }
        } catch (ApiException ae) {
            throw ae;
        } catch (Throwable t) {
            _logger.error("error in getContentToHtml", t);
            throw new EntException("Error into API method", t);
        }
        return render;
    }

    protected String getRenderedContent(String id, int modelId, String langCode, HttpServletRequest request) {
        RequestContext reqCtx = new RequestContext();
        reqCtx.setRequest(request);
        Lang currentLang = this.getLangManager().getLang(langCode);
        if (null == currentLang) {
            currentLang = this.getLangManager().getDefaultLang();
        }
        reqCtx.addExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG, currentLang);
        String renderedContent = null;
        ContentRenderizationInfo renderizationInfo = this.getContentDispenser().getRenderizationInfo(id, modelId, currentLang.getCode(), reqCtx, true);
        if (null != renderizationInfo) {
            this.getContentDispenser().resolveLinks(renderizationInfo, reqCtx);
            renderedContent = renderizationInfo.getRenderedContent();
        }
        return renderedContent;
    }

    protected Content getPublicContent(String id) throws ApiException, EntException {
        Content content;
        try {
            content = this.getContentManager().loadContent(id, true);
            if (null == content) {
                throw new ApiException(IApiErrorCodes.API_PARAMETER_VALIDATION_ERROR, "Null content by id '" + id + "'", HttpStatus.CONFLICT);
            }
        } catch (ApiException ae) {
            throw ae;
        } catch (Throwable t) {
            _logger.error("Error extracting content by id '{}'", id, t);
            throw new EntException("Error extracting content by id '" + id + "'", t);
        }
        return content;
    }

    protected Integer checkModel(String modelId, Content content) throws ApiException, EntException {
        Integer modelIdInteger;
        try {
            if (null == modelId || modelId.trim().length() == 0) {
                return null;
            }
            if (modelId.equals(ContentModel.MODEL_ID_DEFAULT)) {
                if (null == content.getDefaultModel()) {
                    throw new ApiException(IApiErrorCodes.API_PARAMETER_VALIDATION_ERROR,
                            "Invalid 'default' system model for content type '" + content.getTypeCode() + "' - Contact the administrators",
                            HttpStatus.BAD_REQUEST);
                }
                modelIdInteger = Integer.parseInt(content.getDefaultModel());
            } else if (modelId.equals(ContentModel.MODEL_ID_LIST)) {
                if (null == content.getListModel()) {
                    throw new ApiException(IApiErrorCodes.API_PARAMETER_VALIDATION_ERROR,
                            "Invalid 'list' system model for content type '" + content.getTypeCode() + "' - Contact the administrators",
                            HttpStatus.BAD_REQUEST);
                }
                modelIdInteger = Integer.parseInt(content.getListModel());
            } else {
                try {
                    modelIdInteger = Integer.parseInt(modelId);
                } catch (NumberFormatException t) {
                    throw new ApiException(IApiErrorCodes.API_PARAMETER_VALIDATION_ERROR,
                            "The model id must be an integer or 'default' or 'list' - '" + modelId + "'",
                            HttpStatus.BAD_REQUEST);
                }
            }
            ContentModel model = this.getContentModelManager().getContentModel(modelIdInteger);
            if (model == null) {
                throw new ApiException(IApiErrorCodes.API_PARAMETER_VALIDATION_ERROR, "The content model with id '" + modelId + "' does not exist", HttpStatus.BAD_REQUEST);
            } else if (!content.getTypeCode().equals(model.getContentType())) {
                throw new ApiException(IApiErrorCodes.API_PARAMETER_VALIDATION_ERROR,
                        "The content model with id '" + modelId + "' does not match with content of type '" + content.getTypeCode() + "'",
                        HttpStatus.BAD_REQUEST);
            }
        } catch (ApiException ae) {
            throw ae;
        } catch (Throwable t) {
            _logger.error("Error checking model id '{}'", modelId, t);
            throw new EntException("Error checking model id '" + modelId + "'", t);
        }
        return modelIdInteger;
    }

    public CmsApiResponse addContent(JAXBContent jaxbContent, Properties properties) throws EntException {
        CmsApiResponse response = new CmsApiResponse();
        try {
            String typeCode = jaxbContent.getTypeCode();
            Content prototype = (Content) this.getContentManager().getEntityPrototype(typeCode);
            if (null == prototype) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "Content type with code '" + typeCode + "' does not exist", HttpStatus.CONFLICT);
            }
            Content content = (Content) jaxbContent.buildEntity(prototype, this.getCategoryManager(), null);
            if (null != content.getId()) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "You cannot specify Content Id", HttpStatus.CONFLICT);
            }
            UserDetails user = (UserDetails) properties.get(SystemConstants.API_USER_PARAMETER);
            content.setFirstEditor((null != user) ? user.getUsername() : SystemConstants.GUEST_USER_NAME);
            response = this.validateAndSaveContent(content, properties);
        } catch (ApiException ae) {
            response.addErrors(ae.getErrors());
            response.setResult(IResponseBuilder.FAILURE, null);
        } catch (Throwable t) {
            _logger.error("Error adding content", t);
            throw new EntException("Error adding content", t);
        }
        return response;
    }

    public CmsApiResponse updateContent(JAXBContent jaxbContent, Properties properties) throws EntException {
        CmsApiResponse response = new CmsApiResponse();
        try {
            String typeCode = jaxbContent.getTypeCode();
            Content prototype = (Content) this.getContentManager().getEntityPrototype(typeCode);
            if (null == prototype) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "Content type with code '" + typeCode + "' does not exist", HttpStatus.CONFLICT);
            }
            Content masterContent = this.getContentManager().loadContent(jaxbContent.getId(), false);
            String mergeString = properties.getProperty("merge");
            boolean merge = (null != mergeString) ? Boolean.parseBoolean(mergeString) : false;
            Content contentToFill = (merge) ? masterContent : prototype;
            String langCode = (merge) ? properties.getProperty(SystemConstants.API_LANG_CODE_PARAMETER) : null;
            Content content = (Content) jaxbContent.buildEntity(contentToFill, this.getCategoryManager(), langCode);
            if (null == masterContent) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "Content with code '" + content.getId() + "' does not exist", HttpStatus.CONFLICT);
            } else if (!masterContent.getMainGroup().equals(content.getMainGroup())) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR,
                        "Invalid main group " + content.getMainGroup() + " not equal to master " + masterContent.getMainGroup(),
                        HttpStatus.CONFLICT);
            }
            response = this.validateAndSaveContent(content, properties);
        } catch (ApiException ae) {
            response.addErrors(ae.getErrors());
            response.setResult(IResponseBuilder.FAILURE, null);
        } catch (Throwable t) {
            _logger.error("Error updating content", t);
            throw new EntException("Error updating content", t);
        }
        return response;
    }

    protected CmsApiResponse validateAndSaveContent(Content content, Properties properties) throws EntException {
        CmsApiResponse response = new CmsApiResponse();
        try {
            UserDetails user = (UserDetails) properties.get(SystemConstants.API_USER_PARAMETER);
            if (null == user) {
                user = this.getUserManager().getGuestUser();
            }
            if (!this.getContentAuthorizationHelper().isAuth(user, content)) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR,
                        "Content groups makes the new content not allowed for user " + user.getUsername(),
                        HttpStatus.FORBIDDEN);
            }
            List<LegacyApiError> errors = this.validate(content);
            if (errors.size() > 0) {
                response.addErrors(errors);
                response.setResult(IResponseBuilder.FAILURE, null);
                return response;
            }
            String insertOnLineString = properties.getProperty("insertOnLine");
            boolean insertOnLine = (null != insertOnLineString) ? Boolean.parseBoolean(insertOnLineString) : false;
            if (!insertOnLine) {
                this.getContentManager().saveContent(content);
            } else {
                this.getContentManager().insertOnLineContent(content);
            }
            JAXBCmsResult cms = new JAXBCmsResult();

            cms.setId(content.getId());
            cms.setStatus(IResponseBuilder.SUCCESS);
//            response.setResult(IResponseBuilder.SUCCESS, null);
            response.setResult(cms, null);
        } catch (ApiException ae) {
            response.addErrors(ae.getErrors());
            response.setResult(IResponseBuilder.FAILURE, null);
        } catch (Throwable t) {
            _logger.error("error in validateAndSaveContent", t);
            throw new EntException("Error adding content", t);
        }
        return response;
    }

    private List<LegacyApiError> validate(Content content) throws EntException {
        List<LegacyApiError> errors = new ArrayList<>();
        try {
            if (null == content.getMainGroup()) {
                errors.add(new LegacyApiError(IApiErrorCodes.API_VALIDATION_ERROR, "Main group null", HttpStatus.CONFLICT));
            }
            List<FieldError> fieldErrors = content.validate(this.getGroupManager(), this.getLangManager());
            if (null != fieldErrors) {
                for (FieldError fieldError : fieldErrors) {
                    if (fieldError instanceof AttributeFieldError) {
                        AttributeFieldError attributeError = (AttributeFieldError) fieldError;
                        errors.add(new LegacyApiError(IApiErrorCodes.API_VALIDATION_ERROR, attributeError.getFullMessage(), HttpStatus.CONFLICT));
                    } else {
                        errors.add(new LegacyApiError(IApiErrorCodes.API_VALIDATION_ERROR, fieldError.getMessage(), HttpStatus.CONFLICT));
                    }
                }
            }
        } catch (Exception t) {
            _logger.error("Error validating content", t);
            throw new EntException("Error validating content", t);
        }
        return errors;
    }

    public StringApiResponse deleteContent(Properties properties) throws EntException {
        StringApiResponse response = new StringApiResponse();
        try {
            String id = properties.getProperty("id");
            Content masterContent = this.getContentManager().loadContent(id, false);
            if (null == masterContent) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "Content with code '" + id + "' does not exist", HttpStatus.CONFLICT);
            }
            UserDetails user = (UserDetails) properties.get(SystemConstants.API_USER_PARAMETER);
            if (null == user) {
                user = this.getUserManager().getGuestUser();
            }
            if (!this.getContentAuthorizationHelper().isAuth(user, masterContent)) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR,
                        "Content groups makes the new content not allowed for user " + user.getUsername(),
                        HttpStatus.FORBIDDEN);
            }
            List<String> references = ((ContentUtilizer) this.getContentManager()).getContentUtilizers(id);
            if (references != null && references.size() > 0) {
                boolean found = false;
                for (int i = 0; i < references.size(); i++) {
                    String reference = references.get(i);
                    ContentRecordVO record = this.getContentManager().loadContentVO(reference);
                    if (null != record) {
                        found = true;
                        response.addError(new LegacyApiError(IApiErrorCodes.API_VALIDATION_ERROR,
                                "Content " + id + " referenced to content " + record.getId() + " - '" + record.getDescription() + "'",
                                HttpStatus.CONFLICT));
                    }
                }
                if (found) {
                    response.setResult(IResponseBuilder.FAILURE, null);
                    return response;
                }
            }
            if (masterContent.isOnLine()) {
                this.getContentManager().removeOnLineContent(masterContent);
            }
            String removeWorkVersionString = properties.getProperty("removeWorkVersion");
            boolean removeWorkVersion = (null != removeWorkVersionString) ? Boolean.parseBoolean(removeWorkVersionString) : false;
            if (removeWorkVersion) {
                this.getContentManager().deleteContent(masterContent);
            }
            response.setResult(IResponseBuilder.SUCCESS, null);
        } catch (ApiException ae) {
            response.addErrors(ae.getErrors());
            response.setResult(IResponseBuilder.FAILURE, null);
        } catch (Throwable t) {
            _logger.error("Error deleting content", t);
            throw new EntException("Error deleting content", t);
        }
        return response;
    }

    public void updateContentText(JAXBContentAttribute jaxbContentAttribute) throws ApiException, EntException {
        try {
            String contentId = jaxbContentAttribute.getContentId();
            Content masterContent = this.getContentManager().loadContent(jaxbContentAttribute.getContentId(), true);
            if (null == masterContent) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "Content with code '" + contentId + "' does not exist", HttpStatus.CONFLICT);
            }
            String attributeName = jaxbContentAttribute.getAttributeName();
            AttributeInterface attribute = masterContent.getAttribute(attributeName);
            if (null == attribute) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR,
                        "Content Attribute with code '" + attributeName + "' does not exist into content " + contentId,
                        HttpStatus.CONFLICT);
            } else if (!(attribute instanceof ITextAttribute)) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR,
                        "Content Attribute with code '" + attributeName + "' isn't a Text Attribute",
                        HttpStatus.CONFLICT);
            }
            String langCode = jaxbContentAttribute.getLangCode();
            String value = jaxbContentAttribute.getValue();
            if (StringUtils.isEmpty(langCode) || StringUtils.isEmpty(value)) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "LangCode or value is Empty", HttpStatus.CONFLICT);
            }
            ((ITextAttribute) attribute).setText(value, langCode);
            this.getContentManager().insertOnLineContent(masterContent);
        } catch (ApiException ae) {
            throw ae;
        } catch (Throwable t) {
            _logger.error("Error updating content attribute", t);
            throw new EntException("Error updating content attribute", t);
        }
    }

    protected IContentListHelper getContentListHelper() {
        return _contentListHelper;
    }

    public void setContentListHelper(IContentListHelper contentListHelper) {
        this._contentListHelper = contentListHelper;
    }

    protected IUserManager getUserManager() {
        return _userManager;
    }
    public void setUserManager(IUserManager userManager) {
        this._userManager = userManager;
    }

    public ILangManager getLangManager() {
        return _langManager;
    }
    public void setLangManager(ILangManager langManager) {
        this._langManager = langManager;
    }

    protected ICategoryManager getCategoryManager() {
        return _categoryManager;
    }

    public void setCategoryManager(ICategoryManager categoryManager) {
        this._categoryManager = categoryManager;
    }

    protected IGroupManager getGroupManager() {
        return _groupManager;
    }

    public void setGroupManager(IGroupManager groupManager) {
        this._groupManager = groupManager;
    }

    protected IPageManager getPageManager() {
        return _pageManager;
    }

    public void setPageManager(IPageManager pageManager) {
        this._pageManager = pageManager;
    }

    protected IResourceManager getResourceManager() {
        return _resourceManager;
    }

    public void setResourceManager(IResourceManager resourceManager) {
        this._resourceManager = resourceManager;
    }

    protected IContentAuthorizationHelper getContentAuthorizationHelper() {
        return _contentAuthorizationHelper;
    }

    public void setContentAuthorizationHelper(IContentAuthorizationHelper contentAuthorizationHelper) {
        this._contentAuthorizationHelper = contentAuthorizationHelper;
    }

    protected IContentDispenser getContentDispenser() {
        return _contentDispenser;
    }

    public void setContentDispenser(IContentDispenser contentDispenser) {
        this._contentDispenser = contentDispenser;
    }

    public String getItemsStartElement() {
        return _itemsStartElement;
    }

    public void setItemsStartElement(String itemsStartElement) {
        this._itemsStartElement = itemsStartElement;
    }

    public String getItemStartElement() {
        return _itemStartElement;
    }

    public void setItemStartElement(String itemStartElement) {
        this._itemStartElement = itemStartElement;
    }

    public String getItemEndElement() {
        return _itemEndElement;
    }

    public void setItemEndElement(String itemEndElement) {
        this._itemEndElement = itemEndElement;
    }

    public String getItemsEndElement() {
        return _itemsEndElement;
    }

    public void setItemsEndElement(String itemsEndElement) {
        this._itemsEndElement = itemsEndElement;
    }

    private IContentListHelper _contentListHelper;
    private ILangManager _langManager;
    private IUserManager _userManager;
    private ICategoryManager _categoryManager;
    private IGroupManager _groupManager;
    private IPageManager _pageManager;
    private IResourceManager _resourceManager;
    private IContentAuthorizationHelper _contentAuthorizationHelper;
    private IContentDispenser _contentDispenser;
    private String _itemsStartElement = "<ul>";
    private String _itemStartElement = "<li>";
    private String _itemEndElement = "</li>";
    private String _itemsEndElement = "</ul>";

}
