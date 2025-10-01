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
package org.entando.entando.web;

import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.entando.entando.aps.system.services.api.DefaultJsonTypesProvider;
import org.entando.entando.aps.system.services.api.JsonTypesProvider;
import org.entando.entando.aps.system.services.api.ObjectMapperConfiguration;
import org.entando.entando.aps.system.services.oauth2.IApiOAuth2TokenManager;
import org.entando.entando.web.common.handlers.RestExceptionHandler;
import org.entando.entando.web.common.interceptor.EntandoOauth2Interceptor;
import org.entando.entando.web.common.model.PagedMetadata;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.ExceptionHandlerMethodResolver;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

@ExtendWith(MockitoExtension.class)
public class AbstractControllerTest {

    protected MockMvc mockMvc;

    @Mock
    protected IApiOAuth2TokenManager apiOAuth2TokenManager;

    @Mock
    protected IAuthenticationProviderManager authenticationProviderManager;

    @Mock
    protected IAuthorizationManager authorizationManager;

    @InjectMocks
    protected EntandoOauth2Interceptor entandoOauth2Interceptor;

    protected List<JsonTypesProvider> getJsonTypesProviders() {
        return List.of(new DefaultJsonTypesProvider());
    }

    protected ObjectMapperConfiguration mapperConfiguration = new ObjectMapperConfiguration();

    /**
     * The returned list contains an {@link HandlerExceptionResolver} built with
     * an instance of a {@link ResourceBundleMessageSource}, that points to the
     * default baseName and with an instance of {@link RestExceptionHandler},
     * the global exceptionHandler
     *
     * A typical use is:
     * <pre>
     * <code>
     * mockMvc = MockMvcBuilders.standaloneSetup(someControllerUnderTest)
     * .addInterceptors(...)
     * .setHandlerExceptionResolvers(createHandlerExceptionResolver())
     * .build();
     * </code>
     * </pre>
     *
     *
     * @return
     */
    protected List<HandlerExceptionResolver> createHandlerExceptionResolver() {
        List<HandlerExceptionResolver> handlerExceptionResolvers = new ArrayList<>();
        ExceptionHandlerExceptionResolver exceptionHandlerExceptionResolver = createExceptionResolver();
        handlerExceptionResolvers.add(exceptionHandlerExceptionResolver);
        return handlerExceptionResolvers;
    }

    protected ExceptionHandlerExceptionResolver createExceptionResolver() {

        final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("rest/messages");
        messageSource.setUseCodeAsDefaultMessage(true);

        ExceptionHandlerExceptionResolver exceptionResolver = new ExceptionHandlerExceptionResolver() {

            @Override
            protected ServletInvocableHandlerMethod getExceptionHandlerMethod(HandlerMethod handlerMethod, Exception exception, ServletWebRequest webRequest) {
                Method method = new ExceptionHandlerMethodResolver(RestExceptionHandler.class).resolveMethod(exception);
                if (method != null) {
                    RestExceptionHandler validationHandler = new RestExceptionHandler();
                    validationHandler.setMessageSource(messageSource);
                    return new ServletInvocableHandlerMethod(validationHandler, method);
                }
                return super.getExceptionHandlerMethod(handlerMethod, exception, webRequest);
            }
        };

        exceptionResolver.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        exceptionResolver.afterPropertiesSet();
        return exceptionResolver;
    }

    protected HttpMessageConverter<?>[] getMessageConverters() {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON));
        jsonConverter.setObjectMapper(mapperConfiguration.defaultObjectMapper());
        MappingJackson2XmlHttpMessageConverter xmlConverter = new MappingJackson2XmlHttpMessageConverter();
        xmlConverter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_XML));
        xmlConverter.setObjectMapper(mapperConfiguration.xmlMapper());
        return new HttpMessageConverter<?>[]{jsonConverter, xmlConverter};
    }

    protected Object createPagedMetadata(String json) throws IOException, JsonParseException, JsonMappingException {
        ObjectMapper mapper = new ObjectMapper();
        Object result = mapper.readValue(json, PagedMetadata.class);
        return result;
    }

    protected Object createMetadata(String json, Class classType) throws IOException, JsonParseException, JsonMappingException {
        ObjectMapper mapper = new ObjectMapper();
        Object result = mapper.readValue(json, classType);
        return result;
    }

    /**
     * Creates a configured validator for use with MockMvc standalone setup.
     * This enables Jakarta Bean Validation (@Valid, @NotBlank, etc.) in unit tests.
     *
     * @return configured LocalValidatorFactoryBean
     */
    protected org.springframework.validation.Validator createValidator() {
        org.springframework.validation.beanvalidation.LocalValidatorFactoryBean validator =
                new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean();
        validator.setMessageInterpolator(new org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator());
        validator.afterPropertiesSet();
        return validator;
    }

    protected byte[] convertObjectToJsonBytes(Object object) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.writeValueAsBytes(object);
    }

    protected String mockOAuthInterceptor(UserDetails user) {
        return OAuth2TestUtils.mockOAuthInterceptor(apiOAuth2TokenManager, authenticationProviderManager, authorizationManager, user);
    }

    protected AuthRequestBuilder createAuthRequest(MockHttpServletRequestBuilder requestBuilder) {
        return new AuthRequestBuilder(mockMvc, getAccessToken(), requestBuilder);
    }
    
    @BeforeEach
    public void cleanToken() {
        accessToken = null;
        mapperConfiguration.setJsonTypesProviders(getJsonTypesProviders());
    }

    private String accessToken;

    private String getAccessToken() {
        if (this.accessToken == null) {
            UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24").grantedToRoleAdmin().build();
            this.accessToken = OAuth2TestUtils.mockOAuthInterceptor(apiOAuth2TokenManager, authenticationProviderManager, authorizationManager, user);
        }
        return this.accessToken;
    }
}
