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
package org.entando.entando.web.userprofile.validator;

import com.agiletec.aps.system.common.entity.IEntityManager;
import org.entando.entando.aps.system.services.entity.model.EntityDto;
import org.entando.entando.aps.system.services.userprofile.IUserProfileManager;
import org.entando.entando.web.common.exceptions.ValidationConflictException;
import org.entando.entando.web.entity.validator.EntityValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;

/**
 * @author E.Santoboni
 */
@Component
public class ProfileValidator extends EntityValidator {

    public static final String PROFILE_NAME_VALIDATOR_REGEX = "^(?=.{2,70}$)\\S[a-zA-ZÀ-ÿ0-9 _-]*\\S$";
    public static final String ERRCODE_PROFILE_NAME_NOT_FOUND = "5";
    public static final String ERRCODE_PROFILE_NAME_NOT_VALID = "6";
    public static final String FULLNAME = "fullname";
    public static final String ATTRIBUTES = "attributes";

    @Autowired
    private IUserProfileManager userProfileManager;

    public boolean existProfile(String username) {
        return super.existEntity(username);
    }

    @Override
    protected IEntityManager getEntityManager() {
        return this.userProfileManager;
    }

    public void validateFullName(EntityDto bodyRequest, BindingResult bindingResult) {
        String fullName = (String) bodyRequest.getAttributes().stream().filter(e -> e.getCode().equals(FULLNAME))
                .findFirst()
                .orElseThrow(() -> {
                    bindingResult.rejectValue(ATTRIBUTES, ERRCODE_PROFILE_NAME_NOT_FOUND, "user.fullName.notFound");
                    return new ValidationConflictException(bindingResult);
                }).getValue();
        if (!fullName.matches(PROFILE_NAME_VALIDATOR_REGEX)) {
            bindingResult.rejectValue(ATTRIBUTES, ERRCODE_PROFILE_NAME_NOT_VALID, "user.fullName.invalid");
            throw new ValidationConflictException(bindingResult);
        }
    }

}
