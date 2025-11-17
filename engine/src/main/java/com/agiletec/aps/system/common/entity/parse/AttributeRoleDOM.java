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
package com.agiletec.aps.system.common.entity.parse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;

import com.agiletec.aps.system.common.entity.model.attribute.AttributeRole;
import org.entando.entando.ent.exception.EntException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Dom class parser of Attribute Role definitions.
 * @author E.Santoboni
 */
public class AttributeRoleDOM extends AbstractAttributeSupportObjectDOM {
    
    public static final String ROOT_ELEMENT = "roles";
	public static final String ROLE_ELEMENT = "role";
	public static final String NAME_ELEMENT = "name";
	public static final String DESCRIPTION_ELEMENT = "description";
	public static final String ALLOWED_TYPES_ELEMENT = "allowedTypes";
	public static final String FORM_FIELD_TYPE_ELEMENT = "formFieldType";
	
	public Map<String, AttributeRole> extractRoles(String xml, String definitionPath) throws EntException {
		this.validate(xml, definitionPath);
		Document document = this.decodeDOM(xml);
		return this.extractRoles(document);
	}
	
	@Override
	protected String getSchemaFileName() {
		return "attributeRoles-7.0.xsd";
	}
	
	private Map<String, AttributeRole> extractRoles(Document document) {
		Map<String, AttributeRole> roles = new HashMap<>();
		List<Element> roleElements = document.getRootElement().getChildren(ROLE_ELEMENT);
		for (int i=0; i<roleElements.size(); i++) {
			Element roleElement = roleElements.get(i);
			String name = roleElement.getChildText(NAME_ELEMENT);
			String description = roleElement.getChildText(DESCRIPTION_ELEMENT);
			String allowedTypesCSV = roleElement.getChildText(ALLOWED_TYPES_ELEMENT);
			String[] array = allowedTypesCSV.split(",");
			List<String> allowedTypes = Arrays.asList(array);
			AttributeRole role = new AttributeRole(name, description, allowedTypes);
			String formFieldTypeText = roleElement.getChildText(FORM_FIELD_TYPE_ELEMENT);
			if (null != formFieldTypeText) {
				role.setFormFieldType(Enum.valueOf(AttributeRole.FormFieldTypes.class, formFieldTypeText.toUpperCase()));
			}
			roles.put(name, role);
		}
		return roles;
	}
    
    public String getXml(Map<String, AttributeRole> roles) {
        XMLOutputter out = new XMLOutputter();
        Document document = new Document();
        Element rootElement = new Element(ROOT_ELEMENT);
        document.setRootElement(rootElement);
        roles.values().stream().forEach(r -> {
            Element roleElement = new Element(ROLE_ELEMENT);
            rootElement.addContent(roleElement);
            this.addElement(NAME_ELEMENT, r.getName(), roleElement);
            this.addElement(DESCRIPTION_ELEMENT, r.getDescription(), roleElement);
            this.addElement(ALLOWED_TYPES_ELEMENT, String.join(",", r.getAllowedAttributeTypes()), roleElement);
            if (null != r.getFormFieldType()) {
                this.addElement(FORM_FIELD_TYPE_ELEMENT, r.getFormFieldType().toString(), roleElement);
            }
        });
        Format format = Format.getPrettyFormat();
        format.setIndent("\t");
        out.setFormat(format);
        return out.outputString(document);
    }
    
    private void addElement(String name, String text, Element parent) {
        Element newElement = new Element(name);
        newElement.setText(text);
        parent.addContent(newElement);
    }
    
}
