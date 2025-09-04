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
package com.agiletec.aps.system.common.entity.model.attribute;

import org.jdom2.Element;

import com.agiletec.aps.system.common.entity.model.attribute.util.IAttributeValidationRules;
import com.agiletec.aps.system.common.entity.model.attribute.util.TextAttributeValidationRules;
import com.agiletec.aps.system.common.searchengine.IndexableAttributeInterface;
import org.entando.entando.ent.exception.EntException;
import org.apache.commons.lang3.StringUtils;

/**
 * This abstract class is the base for the 'Text' Attributes.
 * @author E.Santoboni
 */
public abstract class AbstractTextAttribute extends AbstractAttribute implements IndexableAttributeInterface, ITextAttribute {
	
	@Override
	public boolean isTextAttribute() {
		return true;
	}

	@Override
	protected IAttributeValidationRules getValidationRuleNewIntance() {
		return new TextAttributeValidationRules();
	}
	
	@Override
	public int getMaxLength() {
		TextAttributeValidationRules validationRule = (TextAttributeValidationRules) this.getValidationRules();
		if (null != validationRule && null != validationRule.getMaxLength()) {
			return validationRule.getMaxLength();
		}
		return -1;
	}

	@Override
	public int getMinLength() {
		TextAttributeValidationRules validationRule = (TextAttributeValidationRules) this.getValidationRules();
		if (null != validationRule && null != validationRule.getMinLength()) {
			return validationRule.getMinLength();
		}
		return -1;
	}

	@Override
	public String getRegexp() {
		return ((TextAttributeValidationRules) this.getValidationRules()).getRegexp();
	}

	@Override
	protected AbstractJAXBAttribute getJAXBAttributeInstance() {
		return new JAXBTextAttribute();
	}
	
	@Override
	public AbstractJAXBAttribute getJAXBAttribute(String langCode) {
		JAXBTextAttribute jaxbTextAttribute = (JAXBTextAttribute) super.createJAXBAttribute(langCode);
		if (null == jaxbTextAttribute) return null;
		String text = this.getTextForLang(langCode);
		if (StringUtils.isNotEmpty(text)) {
			jaxbTextAttribute.setText(text);
		}
		return jaxbTextAttribute;
	}
    
}