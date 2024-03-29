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
package com.agiletec.plugins.jacms.aps.system.services.content.parse.attribute;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.agiletec.aps.system.common.entity.parse.attribute.TextAttributeHandler;
import com.agiletec.plugins.jacms.aps.system.services.content.model.SymbolicLink;
import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.LinkAttribute;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe handler per l'interpretazione della porzione di xml 
 * relativo all'attributo di tipo link.
 * @author E.Santoboni
 */
public class LinkAttributeHandler extends TextAttributeHandler {

    @Override
    public void startAttribute(Attributes attributes, String qName) throws SAXException {
        if (qName.equals("link")) {
            startLink(attributes, qName);
        } else if (qName.equals("urldest")) {
            startUrlDest(attributes, qName);
        } else if (qName.equals("pagedest")) {
            startPageDest(attributes, qName);
        } else if (qName.equals("contentdest")) {
            startContentDest(attributes, qName);
        } else if (qName.equals("resourcedest")) {
            startResourceDest(attributes, qName);
        } else if (qName.equals("properties")) {
            startProperties(attributes, qName);
        } else if (qName.equals("property")) {
            startProperty(attributes, qName);
        } else {
            super.startAttribute(attributes, qName);
        }
    }

    private void startProperties(Attributes attributes, String qName) throws SAXException {
        this.langCode = extractAttribute(attributes, "lang", qName, false);
    }

    private void startProperty(Attributes attributes, String qName) throws SAXException {
        this.propertyKey = extractAttribute(attributes, "key", qName, true);
    }

    private void startLink(Attributes attributes, String qName) throws SAXException {
        this.linkType = extractAttribute(attributes, "type", qName, true);
        this.langCode = extractAttribute(attributes, "lang", qName, false);
    }

    private void startUrlDest(Attributes attributes, String qName) throws SAXException {
        return; // niente da fare
    }

    private void startPageDest(Attributes attributes, String qName) throws SAXException {
        return; // niente da fare
    }

    private void startContentDest(Attributes attributes, String qName) throws SAXException {
        return; // niente da fare
    }

    private void startResourceDest(Attributes attributes, String qName) throws SAXException {
        return; // niente da fare
    }

    @Override
    public void endAttribute(String qName, StringBuffer textBuffer) {
        if (qName.equals("link")) {
            endLink();
        } else if (qName.equals("urldest")) {
            endUrlDest(textBuffer);
        } else if (qName.equals("pagedest")) {
            endPageDest(textBuffer);
        } else if (qName.equals("contentdest")) {
            endContentDest(textBuffer);
        } else if (qName.equals("resourcedest")) {
            endResourceDest(textBuffer);
        } else if (qName.equals("property")) {
            endProperty(textBuffer);
        } else if (qName.equals("properties")) {
            endProperties(textBuffer);
        } else {
            super.endAttribute(qName, textBuffer);
        }
    }

    private void endProperties(StringBuffer textBuffer) {
        this.langCode = null;
    }

    private void endProperty(StringBuffer textBuffer) {
        if (null != textBuffer) {
            LinkAttribute linkAttribute = (LinkAttribute) this.getCurrentAttr();
            String currentLangCode = (null == this.langCode) ? linkAttribute.getDefaultLangCode() : this.langCode;
            Map<String, String> map = linkAttribute.getLinksProperties().get(currentLangCode);
            if (null == map) {
                map = new HashMap<>();
                linkAttribute.getLinksProperties().put(currentLangCode, map);
            }
            map.put(this.propertyKey, textBuffer.toString());
        }
        this.propertyKey = null;
    }

    private void endLink() {
        SymbolicLink symLink = new SymbolicLink();
        if (null != linkType) {
            if (linkType.equals("content")) {
                symLink.setDestinationToContent(contentDest);
            } else if (linkType.equals("external")) {
                symLink.setDestinationToUrl(urlDest);
            } else if (linkType.equals("page")) {
                symLink.setDestinationToPage(pageDest);
            } else if (linkType.equals("contentonpage")) {
                symLink.setDestinationToContentOnPage(contentDest, pageDest);
            } else if (linkType.equals("resource")) {
                symLink.setDestinationToResource(resourceDest);
            }
            ((LinkAttribute) this.getCurrentAttr()).setSymbolicLink(this.langCode, symLink);
        }
        this.langCode = null;
        this.contentDest = null;
        this.urlDest = null;
        this.pageDest = null;
        this.resourceDest = null;
    }

    private void endUrlDest(StringBuffer textBuffer) {
        if (null != textBuffer) {
            urlDest = textBuffer.toString();
        }
    }

    private void endPageDest(StringBuffer textBuffer) {
        if (null != textBuffer) {
            pageDest = textBuffer.toString();
        }
    }

    private void endContentDest(StringBuffer textBuffer) {
        if (null != textBuffer) {
            contentDest = textBuffer.toString();
        }
    }

    private void endResourceDest(StringBuffer textBuffer) {
        if (null != textBuffer) {
            resourceDest = textBuffer.toString();
        }
    }

    private String langCode;
    private String linkType;
    private String urlDest;
    private String pageDest;
    private String contentDest;
    private String resourceDest;
    private String propertyKey;

}
