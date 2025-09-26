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
package org.entando.entando.aps.system.services.oauth2;

import com.agiletec.aps.system.common.FieldSearchFilter;
import org.entando.entando.ent.exception.EntException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import org.entando.entando.aps.system.services.oauth2.model.ConsumerRecordVO;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

public class OAuthConsumerManager extends AbstractOAuthManager implements IOAuthConsumerManager {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(OAuthConsumerManager.class);

    private IOAuthConsumerDAO consumerDAO;

    @Override
    public void init() throws Exception {
        logger.debug("{} ready", this.getClass().getName());
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        try {
            ConsumerRecordVO consumer = this.getConsumerDAO().getConsumer(clientId);
            if (null == consumer) {
                logger.warn("Client with id '{}' does not exist", clientId);
                return null;
            }
            if (null != consumer.getExpirationDate() && consumer.getExpirationDate().before(new Date())) {
                logger.warn("Client '{}' is expired", clientId);
                return null;
            }

            // Build RegisteredClient using the new Spring Security 6.x pattern
            RegisteredClient.Builder builder = RegisteredClient.withId(consumer.getKey())
                    .clientId(clientId)
                    .clientSecret(consumer.getSecret())
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

            // Add authorized grant types
            if (!StringUtils.isBlank(consumer.getAuthorizedGrantTypes())) {
                String[] grantTypes = consumer.getAuthorizedGrantTypes().split(",");
                for (String grantType : grantTypes) {
                    switch (grantType.trim()) {
                        case "authorization_code":
                            builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);
                            break;
                        case "client_credentials":
                            builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS);
                            break;
                        case "refresh_token":
                            builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN);
                            break;
                        case "implicit":
                            // Implicit grant is deprecated in OAuth 2.1 but still supported
                            builder.authorizationGrantType(new AuthorizationGrantType("implicit"));
                            break;
                        case "password":
                            // Resource Owner Password Credentials is deprecated but still supported
                            builder.authorizationGrantType(new AuthorizationGrantType("password"));
                            break;
                        default:
                            logger.warn("Unknown grant type: {}", grantType);
                    }
                }
            }

            // Add scopes
            if (!StringUtils.isBlank(consumer.getScope())) {
                String[] scopes = consumer.getScope().split(",");
                for (String scope : scopes) {
                    builder.scope(scope.trim());
                }
            }

            // Add redirect URI
            if (null != consumer.getCallbackUrl()) {
                builder.redirectUri(consumer.getCallbackUrl());
            }

            // Token validity settings (Note: Spring Authorization Server handles this differently)
            // These settings may need to be configured at the authorization server level

            return builder.build();

        } catch (Exception t) {
            logger.error("Error extracting consumer record by key {}", clientId, t);
            return null;
        }
    }

    @Override
    public RegisteredClient findById(String id) {
        // In Entando, the ID is typically the same as the client ID or consumer key
        // We can delegate to findByClientId or look up by the consumer key
        try {
            ConsumerRecordVO consumer = this.getConsumerDAO().getConsumer(id);
            if (consumer != null) {
                return findByClientId(consumer.getKey());
            }
        } catch (Exception e) {
            logger.error("Error finding client by ID: {}", id, e);
        }
        return null;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        try {
            // Convert RegisteredClient to ConsumerRecordVO
            ConsumerRecordVO consumer = new ConsumerRecordVO();
            consumer.setKey(registeredClient.getClientId());
            consumer.setSecret(registeredClient.getClientSecret());
            consumer.setName(registeredClient.getClientName());
            consumer.setDescription(registeredClient.getClientName() != null ? registeredClient.getClientName() : "Auto-registered client");

            // Set callback URL (use first redirect URI if available)
            if (!registeredClient.getRedirectUris().isEmpty()) {
                consumer.setCallbackUrl(registeredClient.getRedirectUris().iterator().next());
            }

            // Set scopes
            if (!registeredClient.getScopes().isEmpty()) {
                consumer.setScope(String.join(",", registeredClient.getScopes()));
            }

            // Set grant types
            Set<String> grantTypes = new HashSet<>();
            for (AuthorizationGrantType grantType : registeredClient.getAuthorizationGrantTypes()) {
                grantTypes.add(grantType.getValue());
            }
            consumer.setAuthorizedGrantTypes(String.join(",", grantTypes));

            // Set dates
            consumer.setIssuedDate(new Date());

            // Check if consumer already exists and update or add accordingly
            ConsumerRecordVO existingConsumer = this.getConsumerDAO().getConsumer(registeredClient.getClientId());
            if (existingConsumer != null) {
                this.updateConsumer(consumer);
            } else {
                this.addConsumer(consumer);
            }

            logger.debug("Successfully saved RegisteredClient: {}", registeredClient.getClientId());

        } catch (Exception e) {
            logger.error("Error saving RegisteredClient: {}", registeredClient.getClientId(), e);
            throw new RuntimeException("Failed to save RegisteredClient", e);
        }
    }

    /**
     * Utility method to remove a RegisteredClient using Entando's consumer management
     * Note: This is not part of RegisteredClientRepository interface in Spring Authorization Server 1.x
     * //TODO
     */
    public void removeRegisteredClient(RegisteredClient registeredClient) {
        try {
            this.deleteConsumer(registeredClient.getClientId());
            logger.debug("Successfully removed RegisteredClient: {}", registeredClient.getClientId());
        } catch (Exception e) {
            logger.error("Error removing client: {}", registeredClient.getClientId(), e);
            throw new RuntimeException("Failed to remove RegisteredClient: " + registeredClient.getClientId(), e);
        }
    }

    @Override
    public ConsumerRecordVO getConsumerRecord(String consumerKey) throws EntException {
        ConsumerRecordVO consumer = null;
        try {
            consumer = this.getConsumerDAO().getConsumer(consumerKey);
        } catch (Exception t) {
            logger.error("Error extracting consumer record by key {}", consumerKey, t);
            throw new EntException("Error extracting consumer record by key " + consumerKey, t);
        }
        return consumer;
    }

    @Override
    public ConsumerRecordVO addConsumer(ConsumerRecordVO consumer) throws EntException {
        try {
            return this.getConsumerDAO().addConsumer(consumer);
        } catch (Throwable t) {
            logger.error("Error adding consumer", t);
            throw new EntException("Error adding consumer", t);
        }
    }

    @Override
    public ConsumerRecordVO updateConsumer(ConsumerRecordVO consumer) throws EntException {
        try {
            return this.getConsumerDAO().updateConsumer(consumer);
        } catch (Throwable t) {
            logger.error("Error updating consumer", t);
            throw new EntException("Error updating consumer", t);
        }
    }

    @Override
    public void deleteConsumer(String clientId) throws EntException {
        try {
            this.getConsumerDAO().deleteConsumer(clientId);
        } catch (Throwable t) {
            logger.error("Error deleting consumer record by key {}", clientId, t);
            throw new EntException("Error deleting consumer record by key " + clientId, t);
        }
    }

    @Override
    public List<String> getConsumerKeys(FieldSearchFilter<?>[] filters) throws EntException {
        List<String> consumerKeys = null;
        try {
            consumerKeys = this.getConsumerDAO().getConsumerKeys(filters);
        } catch (Throwable t) {
            logger.error("Error extracting consumer keys", t);
            throw new EntException("Error extracting consumer keys", t);
        }
        return consumerKeys;
    }

    protected IOAuthConsumerDAO getConsumerDAO() {
        return consumerDAO;
    }

    public void setConsumerDAO(IOAuthConsumerDAO consumerDAO) {
        this.consumerDAO = consumerDAO;
    }

    @Override
    public List<ConsumerRecordVO> getConsumers(FieldSearchFilter<?>[] filters) throws EntException {
        try {
            return consumerDAO.getConsumers(filters);
        } catch (Throwable t) {
            logger.error("Error retrieving consumers", t);
            throw new EntException("Error retrieving consumers", t);
        }
    }
}
