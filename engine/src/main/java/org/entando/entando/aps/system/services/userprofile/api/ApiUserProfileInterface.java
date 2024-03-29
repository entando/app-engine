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
package org.entando.entando.aps.system.services.userprofile.api;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.entity.helper.BaseFilterUtils;
import com.agiletec.aps.system.common.entity.model.AttributeFieldError;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.common.entity.model.FieldError;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.services.group.IGroupManager;
import com.agiletec.aps.system.services.lang.ILangManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.entando.entando.aps.system.services.api.IApiErrorCodes;
import org.entando.entando.aps.system.services.api.model.LegacyApiError;
import org.entando.entando.aps.system.services.api.model.ApiException;
import org.entando.entando.aps.system.services.api.model.StringApiResponse;
import org.entando.entando.aps.system.services.api.server.IResponseBuilder;
import org.entando.entando.aps.system.services.userprofile.IUserProfileManager;
import org.entando.entando.aps.system.services.userprofile.api.model.JAXBUserProfile;
import org.entando.entando.aps.system.services.userprofile.model.IUserProfile;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.springframework.http.HttpStatus;

/**
 * @author E.Santoboni
 */
public class ApiUserProfileInterface {

    private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(ApiUserProfileInterface.class);

    public List<String> getUserProfiles(Properties properties) throws ApiException, EntException {
        List<String> usernames;
        try {
            String userProfileType = properties.getProperty("typeCode");
            IUserProfile prototype = (IUserProfile) this.getUserProfileManager().getEntityPrototype(userProfileType);
            if (null == prototype) {
                throw new ApiException(IApiErrorCodes.API_PARAMETER_VALIDATION_ERROR,
                        getProfileTypeDoesNotExistMessage(userProfileType), HttpStatus.CONFLICT);
            }
            String langCode = properties.getProperty(SystemConstants.API_LANG_CODE_PARAMETER);
            String filtersParam = properties.getProperty("filters");
            BaseFilterUtils filterUtils = new BaseFilterUtils();
            EntitySearchFilter[] filters = filterUtils.getFilters(prototype, filtersParam, langCode);
            usernames = this.getUserProfileManager().searchId(userProfileType, filters);
        } catch (ApiException ae) {
            throw ae;
        } catch (Throwable t) {
            _logger.error("Error searching usernames", t);
            throw new EntException("Error searching usernames", t);
        }
        return usernames;
    }

    public JAXBUserProfile getUserProfile(Properties properties) throws ApiException, EntException {
        JAXBUserProfile jaxbUserProfile;
        try {
            String username = properties.getProperty("username");
            IUserProfile userProfile = this.getUserProfileManager().getProfile(username);
            if (null == userProfile) {
                throw new ApiException(IApiErrorCodes.API_PARAMETER_VALIDATION_ERROR,
                        getProfileDoesNotExistMessage(username), HttpStatus.CONFLICT);
            }
            String langCode = properties.getProperty(SystemConstants.API_LANG_CODE_PARAMETER);
            jaxbUserProfile = new JAXBUserProfile(userProfile, langCode);
        } catch (ApiException ae) {
            throw ae;
        } catch (Throwable t) {
            _logger.error("Error extracting user profile", t);
            throw new EntException("Error extracting user profile", t);
        }
        return jaxbUserProfile;
    }

    public StringApiResponse addUserProfile(JAXBUserProfile jaxbUserProfile) throws EntException {
        StringApiResponse response = new StringApiResponse();
        try {
            String username = jaxbUserProfile.getId();
            if (null != this.getUserProfileManager().getProfile(username)) {
                throw new ApiException(IApiErrorCodes.API_PARAMETER_VALIDATION_ERROR,
                        getProfileAlreadyExistsMessage(username), HttpStatus.CONFLICT);
            }
            IApsEntity profilePrototype = this.getUserProfileManager().getEntityPrototype(jaxbUserProfile.getTypeCode());
            if (null == profilePrototype) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR,
                        getProfileTypeDoesNotExistMessage(jaxbUserProfile.getTypeCode()), HttpStatus.CONFLICT);
            }
            IUserProfile userProfile = (IUserProfile) jaxbUserProfile.buildEntity(profilePrototype, null);
            List<LegacyApiError> errors = this.validate(userProfile);
            if (errors.size() > 0) {
                response.addErrors(errors);
                response.setResult(IResponseBuilder.FAILURE, null);
                return response;
            }
            this.getUserProfileManager().addProfile(username, userProfile);
            response.setResult(IResponseBuilder.SUCCESS, null);
        } catch (ApiException ae) {
            response.addErrors(ae.getErrors());
            response.setResult(IResponseBuilder.FAILURE, null);
        } catch (Throwable t) {
            _logger.error("Error adding user profile", t);
            throw new EntException("Error adding user profile", t);
        }
        return response;
    }

    public StringApiResponse updateUserProfile(JAXBUserProfile jaxbUserProfile) throws EntException {
        StringApiResponse response = new StringApiResponse();
        try {
            String username = jaxbUserProfile.getId();
            if (null == this.getUserProfileManager().getProfile(username)) {
                throw new ApiException(IApiErrorCodes.API_PARAMETER_VALIDATION_ERROR,
                        getProfileDoesNotExistMessage(username), HttpStatus.CONFLICT);
            }
            IApsEntity profilePrototype = this.getUserProfileManager().getEntityPrototype(jaxbUserProfile.getTypeCode());
            if (null == profilePrototype) {
                throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR,
                        getProfileTypeDoesNotExistMessage(jaxbUserProfile.getTypeCode()), HttpStatus.CONFLICT);
            }
            IUserProfile userProfile = (IUserProfile) jaxbUserProfile.buildEntity(profilePrototype, null);
            List<LegacyApiError> errors = this.validate(userProfile);
            if (errors.size() > 0) {
                response.addErrors(errors);
                response.setResult(IResponseBuilder.FAILURE, null);
                return response;
            }
            this.getUserProfileManager().updateProfile(username, userProfile);
            response.setResult(IResponseBuilder.SUCCESS, null);
        } catch (ApiException ae) {
            response.addErrors(ae.getErrors());
            response.setResult(IResponseBuilder.FAILURE, null);
        } catch (Throwable t) {
            _logger.error("Error updating user profile", t);
            throw new EntException("Error updating user profile", t);
        }
        return response;
    }

    private List<LegacyApiError> validate(IUserProfile userProfile) throws EntException {
        List<LegacyApiError> errors = new ArrayList<>();
        try {
            List<FieldError> fieldErrors = userProfile.validate(this.getGroupManager(), this.getLangManager());
            if (null != fieldErrors) {
                for (int i = 0; i < fieldErrors.size(); i++) {
                    FieldError fieldError = fieldErrors.get(i);
                    if (fieldError instanceof AttributeFieldError) {
                        AttributeFieldError attributeError = (AttributeFieldError) fieldError;
                        errors.add(new LegacyApiError(IApiErrorCodes.API_VALIDATION_ERROR,
                                attributeError.getFullMessage(), HttpStatus.CONFLICT));
                    } else {
                        errors.add(new LegacyApiError(IApiErrorCodes.API_VALIDATION_ERROR,
                                fieldError.getMessage(), HttpStatus.CONFLICT));
                    }
                }
            }
        } catch (Throwable t) {
            _logger.error("Error validating profile", t);
            throw new EntException("Error validating profile", t);
        }
        return errors;
    }

    public StringApiResponse deleteUserProfile(Properties properties) throws ApiException, EntException {
        StringApiResponse response = new StringApiResponse();
        try {
            String username = properties.getProperty("username");
            IUserProfile userProfile = this.getUserProfileManager().getProfile(username);
            if (null == userProfile) {
                throw new ApiException(IApiErrorCodes.API_PARAMETER_VALIDATION_ERROR,
                        getProfileDoesNotExistMessage(username), HttpStatus.CONFLICT);
            }
            this.getUserProfileManager().deleteProfile(username);
            response.setResult(IResponseBuilder.SUCCESS, null);
        } catch (ApiException ae) {
            response.addErrors(ae.getErrors());
            response.setResult(IResponseBuilder.FAILURE, null);
        } catch (Throwable t) {
            _logger.error("Error deleting user Profile", t);
            throw new EntException("Error deleting user Profile", t);
        }
        return response;
    }

    private static String getProfileDoesNotExistMessage(String username) {
        return String.format("Profile of user '%s' does not exist", username);
    }

    private static String getProfileAlreadyExistsMessage(String username) {
        return String.format("Profile of user '%s' already exist", username);
    }

    private static String getProfileTypeDoesNotExistMessage(String userProfileType) {
        return String.format("User Profile type with code '%s' does not exist", userProfileType);
    }

    protected IUserProfileManager getUserProfileManager() {
        return userProfileManager;
    }

    public void setUserProfileManager(IUserProfileManager userProfileManager) {
        this.userProfileManager = userProfileManager;
    }

    public ILangManager getLangManager() {
        return langManager;
    }

    public void setLangManager(ILangManager langManager) {
        this.langManager = langManager;
    }

    protected IGroupManager getGroupManager() {
        return groupManager;
    }

    public void setGroupManager(IGroupManager groupManager) {
        this.groupManager = groupManager;
    }

    private IUserProfileManager userProfileManager;
    private ILangManager langManager;
    private IGroupManager groupManager;

}
