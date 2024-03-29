/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package com.agiletec.plugins.jacms.apsadmin.content.attribute.action.resource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoListAttribute;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.AbstractResourceAttribute;
import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.ResourceAttributeInterface;
import com.agiletec.plugins.jacms.aps.system.services.resource.IResourceManager;
import com.agiletec.plugins.jacms.aps.system.services.resource.model.ResourceInterface;
import com.agiletec.plugins.jacms.apsadmin.content.ContentActionConstants;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

/**
 * Classe helper base per le action delegata alla gestione delle operazione
 * sugli attributi risorsa.
 *
 * @author E.Santoboni
 */
public class ResourceAttributeActionHelper {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(ResourceAttributeActionHelper.class);

    public static final String ATTRIBUTE_NAME_SESSION_PARAM = "contentAttributeName";
    public static final String RESOURCE_TYPE_CODE_SESSION_PARAM = "resourceTypeCode";
    public static final String RESOURCE_LANG_CODE_SESSION_PARAM = "resourceLangCode";
    public static final String LIST_ELEMENT_INDEX_SESSION_PARAM = "listElementIndex";
    public static final String INCLUDED_ELEMENT_NAME_SESSION_PARAM = "includedElementName";

    public static void initSessionParams(IResourceAttributeAction action, HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (null != action.getParentAttributeName()) {
            session.setAttribute(ATTRIBUTE_NAME_SESSION_PARAM, action.getParentAttributeName());
            session.setAttribute(INCLUDED_ELEMENT_NAME_SESSION_PARAM, action.getAttributeName());
        } else {
            session.setAttribute(ATTRIBUTE_NAME_SESSION_PARAM, action.getAttributeName());
        }
        if (action.getElementIndex() >= 0) {
            session.setAttribute(LIST_ELEMENT_INDEX_SESSION_PARAM, action.getElementIndex());
        }
        session.setAttribute(RESOURCE_TYPE_CODE_SESSION_PARAM, action.getResourceTypeCode());
        session.setAttribute(RESOURCE_LANG_CODE_SESSION_PARAM, action.getResourceLangCode());
    }

    /**
     * Associa la risorsa all'attributo del contenuto o all'elemento
     * dell'attributo lista o all'elemento dell'attributo Composito (sia
     * semplice che in lista).
     *
     * @param resources The resources to join
     * @param request The http request
     */
    public static void joinResources(List<ResourceInterface> resources, HttpServletRequest request) {
        HttpSession session = request.getSession();
        try {
            Content currentContent = getContent(request);
            String attributeName = (String) session.getAttribute(ATTRIBUTE_NAME_SESSION_PARAM);
            AttributeInterface attribute = (AttributeInterface) currentContent.getAttribute(attributeName);
            IResourceManager resourceManager = (IResourceManager) ApsWebApplicationUtils.getBean(JacmsSystemConstants.RESOURCE_MANAGER, request);
            joinResources(attribute, resources, resourceManager.getMetadataMapping(), session);
            updateContent(currentContent, request);
        } catch (Exception e) {
            logger.error("error in joinResource", e);
            throw new RuntimeException("error in joinResource", e);
        }
        removeSessionParams(session);
    }

    /**
     * Associa le risorse all'attributo del contenuto o all'elemento
     * dell'attributo lista o all'elemento dell'attributo Composito (sia
     * semplice che in lista).
     */
    private static void joinResources(AttributeInterface attribute, List<ResourceInterface> resources, Map<String, List<String>> metadataMapping, HttpSession session) {
        if (attribute instanceof CompositeAttribute) {
            String includedAttributeName = (String) session.getAttribute(INCLUDED_ELEMENT_NAME_SESSION_PARAM);
            AttributeInterface includedAttribute = ((CompositeAttribute) attribute).getAttribute(includedAttributeName);
            joinResource(includedAttribute, resources.get(0), metadataMapping, session);
        } else if (attribute instanceof ResourceAttributeInterface) {
            ResourceInterface singleresource = resources.get(0);
            joinSingleResource(attribute, singleresource, metadataMapping, session);
        } else if (attribute instanceof MonoListAttribute) {
            int elementIndex = ((Integer) session.getAttribute(LIST_ELEMENT_INDEX_SESSION_PARAM)).intValue();
            MonoListAttribute monoListAttribute = (MonoListAttribute) attribute;
            if (null != resources) {
                for (int i = 0; i < resources.size(); i++) {
                    ResourceInterface resource = resources.get(i);
                    AttributeInterface attributeElement = (i == 0) ? monoListAttribute.getAttribute(elementIndex) : monoListAttribute.addAttribute();
                    joinResource(attributeElement, resource, metadataMapping, session);
                }
            }
        }
    }

    private static void joinResource(AttributeInterface attribute, ResourceInterface resource, Map<String, List<String>> metadataMapping, HttpSession session) {
        if (attribute instanceof CompositeAttribute) {
            String includedAttributeName = (String) session.getAttribute(INCLUDED_ELEMENT_NAME_SESSION_PARAM);
            AttributeInterface includedAttribute = ((CompositeAttribute) attribute).getAttribute(includedAttributeName);
            joinResource(includedAttribute, resource, metadataMapping, session);
        } else if (attribute instanceof ResourceAttributeInterface) {
            joinSingleResource(attribute, resource, metadataMapping, session);
        }
    }

    private static void joinSingleResource(AttributeInterface attribute, ResourceInterface resource, Map<String, List<String>> metadataMapping, HttpSession session) {
        String langCode = (String) session.getAttribute(RESOURCE_LANG_CODE_SESSION_PARAM);
        ((ResourceAttributeInterface) attribute).setResource(resource, langCode);
        AbstractResourceAttribute resourceAttribute = (AbstractResourceAttribute) attribute;
        resourceAttribute.setText(resource.getDescription(), langCode);
        if (null == resource.getMetadata()) {
            return;
        }
        metadataMapping.keySet().stream().forEach(mappingKey -> {
            List<String> mapping = metadataMapping.get(mappingKey);
            if (null != mapping) {
                String rightKey = mapping.stream()
                        .filter(key -> !StringUtils.isBlank(resource.getMetadata().get(key))).
                        findFirst().orElse(null);
                if (null != rightKey) {
                    String value = resource.getMetadata().get(rightKey);
                    resourceAttribute.setMetadata(mappingKey, langCode, value);
                }
            }
        });
    }

    protected static void removeSessionParams(HttpSession session) {
        session.removeAttribute(ATTRIBUTE_NAME_SESSION_PARAM);
        session.removeAttribute(RESOURCE_TYPE_CODE_SESSION_PARAM);
        session.removeAttribute(RESOURCE_LANG_CODE_SESSION_PARAM);
        session.removeAttribute(LIST_ELEMENT_INDEX_SESSION_PARAM);
        session.removeAttribute(INCLUDED_ELEMENT_NAME_SESSION_PARAM);
    }

    protected static String buildEntryContentAnchorDest(HttpSession session) {
        StringBuilder buffer = new StringBuilder("contentedit_");
        buffer.append(session.getAttribute(ResourceAttributeActionHelper.RESOURCE_LANG_CODE_SESSION_PARAM));
        buffer.append("_" + session.getAttribute(ResourceAttributeActionHelper.ATTRIBUTE_NAME_SESSION_PARAM));
        return buffer.toString();
    }

    public static Content getContent(HttpServletRequest request) {
        String contentOnSessionMarker = extractContentMarker(request);
        return (Content) request.getSession()
                .getAttribute(ContentActionConstants.SESSION_PARAM_NAME_CURRENT_CONTENT_PREXIX + contentOnSessionMarker);
    }

    protected static void updateContent(Content content, HttpServletRequest request) {
        String contentOnSessionMarker = extractContentMarker(request);
        request.getSession().setAttribute(ContentActionConstants.SESSION_PARAM_NAME_CURRENT_CONTENT_PREXIX + contentOnSessionMarker, content);
    }

    protected static String extractContentMarker(HttpServletRequest request) {
        String contentOnSessionMarker = (String) request.getAttribute("contentOnSessionMarker");
        if (null == contentOnSessionMarker || contentOnSessionMarker.trim().length() == 0) {
            contentOnSessionMarker = request.getParameter("contentOnSessionMarker");
        }
        return contentOnSessionMarker;
    }

    public static void removeResource(HttpServletRequest request) {
        Content currentContent = getContent(request);
        HttpSession session = request.getSession();
        String attributeName = (String) session.getAttribute(ATTRIBUTE_NAME_SESSION_PARAM);
        AttributeInterface attribute = currentContent.getAttribute(attributeName);
        removeResource(attribute, request);
        removeSessionParams(session);
        updateContent(currentContent, request);
    }

    private static void removeResource(AttributeInterface attribute, HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (attribute instanceof CompositeAttribute) {
            String includedAttributeName = (String) session.getAttribute(INCLUDED_ELEMENT_NAME_SESSION_PARAM);
            AttributeInterface includedAttribute = ((CompositeAttribute) attribute).getAttribute(includedAttributeName);
            removeResource(includedAttribute, request);
        } else if (attribute instanceof AbstractResourceAttribute) {
            ILangManager langManager = (ILangManager) ApsWebApplicationUtils.getBean(SystemConstants.LANGUAGE_MANAGER, request);
            String langCode = (String) session.getAttribute(RESOURCE_LANG_CODE_SESSION_PARAM);
            AbstractResourceAttribute resourceAttribute = (AbstractResourceAttribute) attribute;
            Map<String, Map<String, String>> metadatas = resourceAttribute.getMetadatas();
            if (langCode == null
                    || langCode.length() == 0
                    || langManager.getDefaultLang().getCode().equals(langCode)) {
                resourceAttribute.getResources().clear();
                resourceAttribute.getTextMap().clear();
                if (null != metadatas) {
                    metadatas.clear();
                }
            } else {
                resourceAttribute.setResource(null, langCode);
                resourceAttribute.setText(null, langCode);
                if (null != metadatas) {
                    metadatas.keySet().stream().forEach(key -> {
                        Map<String, String> singleMap = metadatas.get(key);
                        singleMap.remove(langCode);
                    });
                }
            }
        } else if (attribute instanceof MonoListAttribute) {
            int elementIndex = ((Integer) session.getAttribute(LIST_ELEMENT_INDEX_SESSION_PARAM)).intValue();
            AttributeInterface attributeElement = ((MonoListAttribute) attribute).getAttribute(elementIndex);
            removeResource(attributeElement, request);
        }
    }

}
