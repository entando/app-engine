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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.imageio.ImageIO;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.web.filebrowser.model.FileBrowserFileRequest;
import org.entando.entando.web.userprofile.model.ProfileAvatarRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * @author eu
 */
@Component
public class ProfileAvatarValidator implements Validator {

    private final EntLogger logger = EntLogFactory.getSanitizedLogger(this.getClass());

    public static final String ERRCODE_INVALID_FILE_NAME = "1";
    public static final String ERRCODE_INVALID_FILE_TYPE = "2";

    @Override
    public boolean supports(Class<?> paramClass) {
        return (FileBrowserFileRequest.class.equals(paramClass));
    }

    @Override
    public void validate(Object target, Errors errors) {
        ProfileAvatarRequest request = (ProfileAvatarRequest) target;
        String filename = request.getFilename();
        if (filename.contains("/")) {
            errors.rejectValue("path", ERRCODE_INVALID_FILE_NAME, new String[]{filename}, "fileBrowser.filename.invalidFilename");
            return;
        }

        try {
            if (ImageIO.read(new ByteArrayInputStream(request.getBase64())) == null) {
                errors.rejectValue("path", ERRCODE_INVALID_FILE_TYPE,"fileBrowser.file.invalidType");
                throw new IllegalArgumentException("The requested file is not an image");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
