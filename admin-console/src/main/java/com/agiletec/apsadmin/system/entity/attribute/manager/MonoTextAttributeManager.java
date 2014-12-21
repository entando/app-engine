/*
 * Copyright 2013-Present Entando Corporation (http://www.entando.com) All rights reserved.
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
package com.agiletec.apsadmin.system.entity.attribute.manager;

import com.agiletec.aps.system.common.entity.model.AttributeFieldError;
import com.agiletec.aps.system.common.entity.model.FieldError;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.ITextAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoTextAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.util.TextAttributeValidationRules;
import com.opensymphony.xwork2.ActionSupport;

/**
 * Manager class for the 'Monotext' Attribute.
 * @author E.Santoboni
 */
public class MonoTextAttributeManager extends AbstractMonoLangAttributeManager {

	@Override
    protected void setValue(AttributeInterface attribute, String value) {
        ((MonoTextAttribute) attribute).setText(value);
    }

	@Override
    protected String getCustomAttributeErrorMessage(AttributeFieldError attributeFieldError, ActionSupport action) {
        AttributeInterface attribute = attributeFieldError.getAttribute();
        TextAttributeValidationRules valRules = (TextAttributeValidationRules) attribute.getValidationRules();
        if (null != valRules) {
            ITextAttribute textAttribute = (ITextAttribute) attribute;
            String text = textAttribute.getTextForLang(null);
            String errorCode = attributeFieldError.getErrorCode();
            if (errorCode.equals(FieldError.INVALID_MIN_LENGTH)) {
                String[] args = {String.valueOf(text.length()), String.valueOf(valRules.getMinLength())};
                return action.getText("MonotextAttribute.fieldError.invalidMinLength", args);
            } else if (errorCode.equals(FieldError.INVALID_MAX_LENGTH)) {
                String[] args = {String.valueOf(text.length()), String.valueOf(valRules.getMaxLength())};
                return action.getText("MonotextAttribute.fieldError.invalidMaxLength", args);
            } else if (errorCode.equals(FieldError.INVALID_FORMAT)) {
                return action.getText("MonotextAttribute.fieldError.invalidInsertedText");
            }
        }
        return action.getText(this.getInvalidAttributeMessage());
    }

}