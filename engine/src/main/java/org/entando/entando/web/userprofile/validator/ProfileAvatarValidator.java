/*
 * Copyright 2023-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.user.UserDetails;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import javax.imageio.ImageIO;
import lombok.AllArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.userprofile.IUserProfileManager;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.entando.entando.web.common.RestErrorCodes;
import org.entando.entando.web.userprofile.model.ProfileAvatarRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
@AllArgsConstructor
public class ProfileAvatarValidator implements Validator {

    public static final String ERRCODE_INVALID_FILE_NAME = "1";
    public static final String ERRCODE_INVALID_FILE_TYPE = "2";
    public static final String ERRCODE_MISSING_EMAIL_ATTRIBUTE = "3";
    
    private IUserProfileManager userProfileManager;

    @Override
    public boolean supports(@NonNull Class<?> paramClass) {
        return (ProfileAvatarRequest.class.equals(paramClass));
    }
    
    @Override
    public void validate(@NonNull Object target, @NonNull Errors errors) {
        ProfileAvatarRequest request = (ProfileAvatarRequest) target;
        String filename = request.getFilename();
        if (StringUtils.isBlank(filename)) {
            errors.rejectValue("filename", RestErrorCodes.NOT_BLANK, new String[]{},
                    "avatar.filename.notBlank");
        } else if (StringUtils.isEmpty(FilenameUtils.getExtension(filename))) {
            errors.rejectValue("filename", ERRCODE_INVALID_FILE_NAME, new String[]{filename},
                    "fileBrowser.filename.invalidFilename");
            return;
        }
        byte[] base64 = request.getBase64();
        if (null == base64) {
            errors.rejectValue("base64", RestErrorCodes.NOT_EMPTY, new String[]{},
                    "fileBrowser.base64.notBlank");
        } else {
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(request.getBase64())) {
                if (ImageIO.read(byteArrayInputStream) == null) {
                    errors.rejectValue("base64", ERRCODE_INVALID_FILE_TYPE, "fileBrowser.file.invalidType");
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
    
    public void validate(@NonNull Object target, UserDetails user, @NonNull Errors errors) {
        ProfileAvatarRequest request = (ProfileAvatarRequest) target;
        if (!request.isUseGravatar()) {
            this.validate(target, errors);
            return;
        }
        try {
            Optional.ofNullable(this.userProfileManager.getProfile(user.getUsername()))
                    .map(up -> up.getAttributeByRole(SystemConstants.USER_PROFILE_ATTRIBUTE_ROLE_MAIL)).ifPresentOrElse(up -> {
            }, () -> errors.rejectValue("useGravatar", ERRCODE_MISSING_EMAIL_ATTRIBUTE, new String[]{},
                    "avatar.emailAttribute.missing"));
        } catch (EntException e) {
            throw new EntRuntimeException("Error validating user avatar", e);
        }
    }

}
