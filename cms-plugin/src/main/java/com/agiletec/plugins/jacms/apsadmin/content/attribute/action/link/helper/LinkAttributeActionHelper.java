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
package com.agiletec.plugins.jacms.apsadmin.content.attribute.action.link.helper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoListAttribute;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.content.model.SymbolicLink;
import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.LinkAttribute;
import com.agiletec.plugins.jacms.apsadmin.content.ContentActionConstants;
import com.agiletec.plugins.jacms.apsadmin.content.attribute.action.link.ILinkAttributeAction;
import com.agiletec.plugins.jacms.apsadmin.content.attribute.action.link.ILinkAttributeTypeAction;
import java.util.HashMap;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.ent.exception.EntRuntimeException;

/**
 * Classe helper base per le action delegata 
 * alla gestione delle operazione sugli attributi link.
 * @author E.Santoboni
 */
public class LinkAttributeActionHelper implements ILinkAttributeActionHelper {

    @Override
    public void initSessionParams(ILinkAttributeAction action, HttpServletRequest request) {
        AttributeInterface attribute = null;
        HttpSession session = request.getSession();
        if (null != action.getParentAttributeName()) {
            attribute = (AttributeInterface) getContent(request).getAttribute(action.getParentAttributeName());
            session.setAttribute(ATTRIBUTE_NAME_SESSION_PARAM, action.getParentAttributeName());
            session.setAttribute(INCLUDED_ELEMENT_NAME_SESSION_PARAM, action.getAttributeName());
        } else {
            attribute = (AttributeInterface) getContent(request).getAttribute(action.getAttributeName());
            session.setAttribute(ATTRIBUTE_NAME_SESSION_PARAM, action.getAttributeName());
        }
        if (action.getElementIndex() >= 0) {
            session.setAttribute(LIST_ELEMENT_INDEX_SESSION_PARAM, new Integer(action.getElementIndex()));
        }
        session.setAttribute(LINK_LANG_CODE_SESSION_PARAM, action.getLangCodeOfLink());
        LinkAttribute linkAttribute = (LinkAttribute) getLinkAttribute(attribute, request);
        session.setAttribute(SYMBOLIC_LINK_SESSION_PARAM, linkAttribute.getSymbolicLink(action.getLangCodeOfLink()));
        session.setAttribute(LINK_PROPERTIES_MAP_SESSION_PARAM, linkAttribute.getLinkProperties(action.getLangCodeOfLink()));
    }

    @Override
    public void joinLink(String[] destinations, int destType, Map<String, String> properties, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Content currentContent = getContent(request);
        String attributeName = (String) session.getAttribute(ATTRIBUTE_NAME_SESSION_PARAM);
        AttributeInterface attribute = (AttributeInterface) currentContent.getAttribute(attributeName);
        joinLink(attribute, destinations, destType, properties, request);
        removeSessionParams(session);
        this.updateContent(currentContent, request);
    }

    @Override
    public void removeLink(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Content currentContent = getContent(request);
        String attributeName = (String) session.getAttribute(ATTRIBUTE_NAME_SESSION_PARAM);
        AttributeInterface attribute = (AttributeInterface) currentContent.getAttribute(attributeName);
        removeLink(attribute, request);
        removeSessionParams(session);
        this.updateContent(currentContent, request);
    }

    @Override
    public void removeSessionParams(HttpSession session) {
        session.removeAttribute(ATTRIBUTE_NAME_SESSION_PARAM);
        session.removeAttribute(LINK_LANG_CODE_SESSION_PARAM);
        session.removeAttribute(LIST_ELEMENT_INDEX_SESSION_PARAM);
        session.removeAttribute(INCLUDED_ELEMENT_NAME_SESSION_PARAM);
        session.removeAttribute(SYMBOLIC_LINK_SESSION_PARAM);

    }

    @Override
    public String buildEntryContentAnchorDest(HttpSession session) {
        StringBuilder buffer = new StringBuilder("contentedit_");
        buffer.append(session.getAttribute(LINK_LANG_CODE_SESSION_PARAM));
        buffer.append("_" + session.getAttribute(ATTRIBUTE_NAME_SESSION_PARAM));
        return buffer.toString();
    }

    protected AttributeInterface getLinkAttribute(AttributeInterface attribute, HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (attribute instanceof CompositeAttribute) {
            String includedAttributeName = (String) session.getAttribute(INCLUDED_ELEMENT_NAME_SESSION_PARAM);
            AttributeInterface includedAttribute = ((CompositeAttribute) attribute).getAttribute(includedAttributeName);
            return getLinkAttribute(includedAttribute, request);
        } else if (attribute instanceof MonoListAttribute) {
            Integer elementIndex = (Integer) session.getAttribute(LIST_ELEMENT_INDEX_SESSION_PARAM);
            AttributeInterface attributeElement = ((MonoListAttribute) attribute).getAttribute(elementIndex.intValue());
            return getLinkAttribute(attributeElement, request);
        } else if (attribute instanceof LinkAttribute) {
            return attribute;
        } else {
            throw new RuntimeException("Caso non gestito : Atttributo tipo " + attribute.getClass());
        }
    }

    protected void joinLink(AttributeInterface attribute, String[] destinations, int destType, Map<String, String> properties, HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (attribute instanceof CompositeAttribute) {
            String includedAttributeName = (String) session.getAttribute(INCLUDED_ELEMENT_NAME_SESSION_PARAM);
            AttributeInterface includedAttribute = ((CompositeAttribute) attribute).getAttribute(includedAttributeName);
            updateLink(includedAttribute, destinations, destType, properties, request);
        } else if (attribute instanceof MonoListAttribute) {
            Integer elementIndex = (Integer) session.getAttribute(LIST_ELEMENT_INDEX_SESSION_PARAM);
            AttributeInterface attributeElement = ((MonoListAttribute) attribute).getAttribute(elementIndex.intValue());
            joinLink(attributeElement, destinations, destType, properties, request);
        } else if (attribute instanceof LinkAttribute) {
            updateLink(attribute, destinations, destType, properties, request);
        }
    }

    protected void updateLink(AttributeInterface currentAttribute,
            String[] destinations, int destType, Map<String, String> properties, HttpServletRequest request) {
        HttpSession session = request.getSession();
        String langCode = (String) session.getAttribute(LINK_LANG_CODE_SESSION_PARAM);
        if (StringUtils.isBlank(langCode)) {
            throw new EntRuntimeException("Missing link lang code");
        }
        if (destinations.length != 3) {
            throw new EntRuntimeException("Destinations not recognized");
        }
        SymbolicLink symbolicLink = new SymbolicLink();
        switch (destType) {
            case (SymbolicLink.CONTENT_TYPE):
                symbolicLink.setDestinationToContent(destinations[1]);
                break;
            case (SymbolicLink.CONTENT_ON_PAGE_TYPE):
                symbolicLink.setDestinationToContentOnPage(destinations[1], destinations[2]);
                break;
            case SymbolicLink.PAGE_TYPE:
                symbolicLink.setDestinationToPage(destinations[2]);
                break;
            case SymbolicLink.URL_TYPE:
                symbolicLink.setDestinationToUrl(destinations[0]);
                break;
            case SymbolicLink.RESOURCE_TYPE:
                symbolicLink.setDestinationToResource(destinations[3]);
                break;
            default:
                symbolicLink.setDestinationToContent("");
                break;
        }
        ((LinkAttribute) currentAttribute).setSymbolicLink(langCode, symbolicLink);
        ((LinkAttribute) currentAttribute).getLinksProperties().put(langCode, properties);
    }

    protected void removeLink(AttributeInterface attribute, HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (attribute instanceof CompositeAttribute) {
            String includedAttributeName = (String) session.getAttribute(INCLUDED_ELEMENT_NAME_SESSION_PARAM);
            AttributeInterface includedAttribute = ((CompositeAttribute) attribute).getAttribute(includedAttributeName);
            removeLink(includedAttribute, request);
        } else if (attribute instanceof LinkAttribute) {
            String langCode = (String) session.getAttribute(LINK_LANG_CODE_SESSION_PARAM);
            ILangManager langManager = ApsWebApplicationUtils.getBean(ILangManager.class, request);
            if (langCode.equalsIgnoreCase(langManager.getDefaultLang().getCode())) {
                ((LinkAttribute) attribute).getSymbolicLinks().clear();
                ((LinkAttribute) attribute).getTextMap().clear();
                ((LinkAttribute) attribute).getLinksProperties().clear();
            } else {
                ((LinkAttribute) attribute).getSymbolicLinks().remove(langCode);
                ((LinkAttribute) attribute).getLinksProperties().remove(langCode);
            }
        } else if (attribute instanceof MonoListAttribute) {
            Integer elementIndex = (Integer) session.getAttribute(LIST_ELEMENT_INDEX_SESSION_PARAM);
            AttributeInterface attributeElement = ((MonoListAttribute) attribute).getAttribute(elementIndex.intValue());
            removeLink(attributeElement, request);
        }
    }

    /**
     * Restituisce il contenuto in sessione.
     * @return Il contenuto in sessione.
     */
    protected Content getContent(HttpServletRequest request) {
        String contentOnSessionMarker = this.extractContentMarker(request);
        return (Content) request.getSession()
                .getAttribute(ContentActionConstants.SESSION_PARAM_NAME_CURRENT_CONTENT_PREXIX + contentOnSessionMarker);
    }

    protected void updateContent(Content content, HttpServletRequest request) {
        String contentOnSessionMarker = this.extractContentMarker(request);
        request.getSession().setAttribute(ContentActionConstants.SESSION_PARAM_NAME_CURRENT_CONTENT_PREXIX + contentOnSessionMarker, content);
    }

    protected String extractContentMarker(HttpServletRequest request) {
        String contentOnSessionMarker = (String) request.getAttribute("contentOnSessionMarker");
        if (null == contentOnSessionMarker || contentOnSessionMarker.trim().length() == 0) {
            contentOnSessionMarker = request.getParameter("contentOnSessionMarker");
        }
        return contentOnSessionMarker;
    }

    @Override
    public Map<String, String> createLinkProperties(ILinkAttributeTypeAction action) {
        Map<String, String> properties = new HashMap<>();
        if (StringUtils.isNotBlank(action.getLinkAttributeRel())) {
            properties.put(LinkAttribute.REL_ATTRIBUTE, action.getLinkAttributeRel());
        }
        if (StringUtils.isNotBlank(action.getLinkAttributeTarget())) {
            properties.put(LinkAttribute.TARGET_ATTRIBUTE, action.getLinkAttributeTarget());
        }
        if (StringUtils.isNotBlank(action.getLinkAttributeHRefLang())) {
            properties.put(LinkAttribute.HREFLANG_ATTRIBUTE, action.getLinkAttributeHRefLang());
        }
        return properties;
    }

    @Override
    public void initLinkProperties(ILinkAttributeTypeAction action, HttpServletRequest request) {
        Map<String, String> properties = (Map<String, String>) request.getSession().getAttribute(LINK_PROPERTIES_MAP_SESSION_PARAM);
        if (null != properties) {
            action.setLinkAttributeRel(properties.get(LinkAttribute.REL_ATTRIBUTE));
            action.setLinkAttributeHRefLang(properties.get(LinkAttribute.HREFLANG_ATTRIBUTE));
            action.setLinkAttributeTarget(properties.get(LinkAttribute.TARGET_ATTRIBUTE));
        }
    }

}
