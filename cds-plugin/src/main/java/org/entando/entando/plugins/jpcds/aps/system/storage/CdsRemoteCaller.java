/*
 * Copyright 2022-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpcds.aps.system.storage;

import static org.entando.entando.aps.system.services.tenants.ITenantManager.PRIMARY_CODE;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.tenants.TenantConfig;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.entando.entando.plugins.jpcds.aps.system.storage.CdsUrlUtils.EntSubPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.entando.entando.aps.system.services.storage.CdsActive;

@Component
@CdsActive(true)
public class CdsRemoteCaller  {

    private static final Logger logger = LoggerFactory.getLogger(CdsRemoteCaller.class);
    private static final String REST_ERROR_MSG = "Invalid operation '%s', response status:'%s' for url:'%s'";
    private static final String GENERIC_REST_ERROR_MSG = "Generic error in a rest call for url:'%s'";
    private static final String CDS_RETURN_STATE_OK = "Ok";
    private final RestTemplate restTemplate;
    private final RestTemplate restTemplateWithRedirect;
    private final CdsConfiguration configuration;

    // used weak map to skip excessive growth
    private Map<String, String> tenantsToken = new WeakHashMap<>();

    @Autowired
    public CdsRemoteCaller(@Qualifier("keycloakRestTemplate")RestTemplate restTemplate,
            @Qualifier("keycloakRestTemplateWithRedirect")RestTemplate restTemplateWithRedirect,
            CdsConfiguration configuration) {
        this.restTemplate = restTemplate;
        this.restTemplateWithRedirect = restTemplateWithRedirect;
        this.configuration = configuration;
    }

    public CdsCreateResponseDto executePostCall(URI url,
            String subPath,
            boolean isProtectedResource,
            Optional<InputStream> fileInputStream ,
            Optional<TenantConfig> config,
            boolean forceTokenRetrieve) {

        try {
            logger.debug("Trying to call POST on url:'{}' with isProtectedResource:'{}' forceTokenRetrieve:'{}' isFile:'{}' and is config tenant empty:'{}'",
                    url,
                    isProtectedResource,
                    forceTokenRetrieve,
                    fileInputStream.isPresent(),
                    config.isEmpty());

            HttpHeaders headers = this.getBaseHeader(Arrays.asList(MediaType.ALL), config, forceTokenRetrieve);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = fileInputStream
                    .map(is -> buildFileBodyRequest(subPath, isProtectedResource, is))
                    .orElseGet(() -> buildDirectoryBodyRequest(subPath, isProtectedResource));

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<List<CdsCreateRowResponseDto>> fullResponse = restTemplate.exchange(url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<CdsCreateRowResponseDto>>(){});

            List<CdsCreateRowResponseDto> responseList = Optional
                    .ofNullable(fullResponse.getBody())
                    .orElse(new ArrayList<>());

            CdsCreateResponseDto response = new CdsCreateResponseDto();
            response.setStatusOk(responseList.stream()
                    .map(CdsCreateRowResponseDto::getStatus)
                    .map(CDS_RETURN_STATE_OK::equalsIgnoreCase)
                    .findFirst()
                    .orElse(false));
            response.setList(responseList);

            return response;
        } catch (HttpClientErrorException e) {
            if (!forceTokenRetrieve && (e.getStatusCode().equals(HttpStatus.UNAUTHORIZED))) {
                return this.executePostCall(url, subPath, isProtectedResource, fileInputStream, config, true);
            } else {
                throw buildExceptionWithMessage("POST", e.getStatusCode() , url.toString());
            }
        } catch(Exception ex){
            throw new EntRuntimeException(String.format(GENERIC_REST_ERROR_MSG, url.toString()), ex);
        }
    }

    private MultiValueMap<String, Object> buildDirectoryBodyRequest(String subPath, boolean isProtectedResource){
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("path", subPath);
        body.add("protected", isProtectedResource);
        return body;
    }

    private MultiValueMap<String, Object> buildFileBodyRequest(String subPath, boolean isProtectedResource,
            InputStream fileInputStream){
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        EntSubPath subPathParsed = org.entando.entando.plugins.jpcds.aps.system.storage.CdsUrlUtils.extractPathAndFilename(subPath);
        InputStreamResource resource = new InputStreamResource(fileInputStream);
        body.add("path", subPathParsed.getPath());
        body.add("protected", isProtectedResource);
        body.add("filename", subPathParsed.getFileName());
        body.add("file", resource);

        return body;
    }


    public boolean executeDeleteCall(URI url, Optional<TenantConfig> config, boolean forceTokenRetrieve) {
        try {
            logger.debug("Trying to call Delete on url:'{}' with forceTokenRetrieve:'{}' and is config tenant empty:'{}'",
                    url,
                    forceTokenRetrieve,
                    config.isEmpty());

            HttpHeaders headers = this.getBaseHeader(null, config, forceTokenRetrieve);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map<String,String>> responseEntity = restTemplate
                    .exchange(url, HttpMethod.DELETE, entity, new ParameterizedTypeReference<Map<String, String>>(){});

            return fetchDeleteStatus(responseEntity.getBody());

        } catch (HttpClientErrorException e) {
            if (!forceTokenRetrieve && e.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                return this.executeDeleteCall(url, config, true);
            } else {
                throw buildExceptionWithMessage("DELETE", e.getStatusCode() , url.toString());
            }
        }
    }

    private boolean fetchDeleteStatus(Map<String,String> body) {
        return body != null && CDS_RETURN_STATE_OK.equalsIgnoreCase(body.get("status"));
    }

    public Optional<CdsFileAttributeViewDto[]> getFileAttributeView(URI url, Optional<TenantConfig> config) {
        logger.debug("Trying to call GET (getFileAttributeView) on url:'{}' and is config tenant empty:'{}'",
                url,
                config.isEmpty());

        return this.executeGetCall(url,
                Arrays.asList(MediaType.APPLICATION_JSON),
                config,
                false,
                new ParameterizedTypeReference<CdsFileAttributeViewDto[]>() {});
    }

    public Optional<ByteArrayInputStream> getFile(URI url, Optional<TenantConfig> config, boolean isProtectedResource){
        logger.debug("Trying to call GET (getFile) on url:'{}' with isProtectedResource:'{}' and is config tenant empty:'{}'",
                url,
                isProtectedResource,
                config.isEmpty());

        Optional<byte[]> bytes;
        if (isProtectedResource) {
            bytes = executeGetCall(url, null, config,  false, new ParameterizedTypeReference<byte[]>(){});
        } else {
            try {
                bytes = Optional.ofNullable(restTemplate.getForObject(url, byte[].class));
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                    logger.info("File Not found - uri {}", url);
                    return Optional.empty();
                }
                throw buildExceptionWithMessage("GET", e.getStatusCode(), url.toString());
            }

        }
        return bytes.map(ByteArrayInputStream::new);
    }

    private <T> Optional<T> executeGetCall(URI url, List<MediaType> acceptableMediaTypes, Optional<TenantConfig> config,
            boolean forceTokenRetrieve,
            ParameterizedTypeReference<T> responseType) {
        try {
            HttpHeaders headers = this.getBaseHeader(acceptableMediaTypes, config, forceTokenRetrieve);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<T> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
            return Optional.ofNullable(responseEntity.getBody());

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                logger.info("File Not found - uri {}", url);
                return Optional.empty();
            }
            if (!forceTokenRetrieve && e.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                return this.executeGetCall(url, acceptableMediaTypes, config, true, responseType);
            } else {
                throw buildExceptionWithMessage("GET", e.getStatusCode(), url.toString());
            }
        }
    }

    private EntRuntimeException buildExceptionWithMessage(String method, HttpStatus statusCode, String url){
        return new EntRuntimeException(String.format(REST_ERROR_MSG, method, statusCode, url));
    }

    private HttpHeaders getBaseHeader(List<MediaType> acceptableMediaTypes, Optional<TenantConfig> config, boolean forceToken) {
        String token = this.extractToken(config, forceToken);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + token);
        if (null != acceptableMediaTypes) {
            headers.setAccept(acceptableMediaTypes);
        }
        return headers;
    }

    private String extractToken(Optional<TenantConfig> config, boolean force) {
        return config.map(tc -> getTenantToken(tc,force))
                .orElseGet(() -> getPrimaryToken(force));
    }

    private String getTenantToken(TenantConfig config, boolean force){
        logger.debug("Trying to retrieve token for tenantCode:'{}' with force:'{}'", config.getTenantCode(), force);
        String token = tenantsToken.get(config.getTenantCode());
        if (force || StringUtils.isBlank(token)) {
            logger.debug("Retrieve token from auth server, not from internal map");
            token = this.extractToken(config.getKcAuthUrl(), config.getKcRealm(), config.getKcClientId(), config.getKcClientSecret());
            tenantsToken.put(config.getTenantCode(), ""+token);
        }
        logger.trace("For tenantCode:'{}' retrieved token:'{}'", config.getTenantCode(), token);
        return token;
    }

    private String getPrimaryToken(boolean force){
        logger.debug("Trying to retrieve token for primary with force:'{}'", force);
        String token = this.tenantsToken.get(PRIMARY_CODE);
        if (force || StringUtils.isBlank(token)) {
            logger.debug("Retrieve token from auth server, not from internal map");
            token = this.extractToken(configuration.getKcAuthUrl(), configuration.getKcRealm(), configuration.getKcClientId(), configuration.getKcClientSecret());
            this.tenantsToken.put(PRIMARY_CODE, token);
        }
        logger.trace("For primary retrieved token:'{}'",token);
        return token;
    }

    private String extractToken(String kcUrl, String kcRealm, String clientId, String clientSecret) {
        String encodedClientData = Base64Utils.encodeToString((clientId + ":" + clientSecret).getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("Authorization", "Basic " + encodedClientData);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        String url = String.format("%s/realms/%s/protocol/openid-connect/token", kcUrl, kcRealm);

        logger.debug("Trying to call POST on url:'{}' with clientId:'{}'", url, clientId);
        ResponseEntity<Map<String,Object>> responseEntity =
                restTemplateWithRedirect.exchange(url, HttpMethod.POST, request, new ParameterizedTypeReference<Map<String,Object>>(){});

        return Optional.ofNullable(responseEntity).filter(r -> HttpStatus.OK.equals(r.getStatusCode()))
                .map(r -> r.getBody())
                .map(b -> (String)b.get("access_token"))
                .orElseThrow(() -> new EntRuntimeException(String.format(
                        "Call to '%s' result in response code:'%s' when use clientId:'%s'",
                        url, responseEntity.getStatusCode().value(), clientId)));
    }

}
