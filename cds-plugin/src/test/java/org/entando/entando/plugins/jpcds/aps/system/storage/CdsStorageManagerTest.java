/*
 * Copyright 2024-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.entando.entando.aps.system.services.storage.BasicFileAttributeView;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.aps.system.services.tenants.TenantConfig;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.exception.EntResourceNotFoundException;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

/**
 * @author S.Loru - E.Santoboni
 */
@ExtendWith(MockitoExtension.class)
class CdsStorageManagerTest {
    @Mock
    private CdsRemoteCaller cdsRemoteCaller;
    @Mock
    private ITenantManager tenantManager;
    @Mock
    private CdsConfiguration cdsConfiguration;
    @InjectMocks
    private CdsStorageManager cdsStorageManager;

    @Test
    void shouldCreateDirectory() throws Exception {
        Map<String,String> configMap = Map.of("cdsPublicUrl","http://my-server/tenant1/cms-resources",
                "cdsPrivateUrl","http://cds-kube-service:8081/",
                "cdsPath","/mytenant/api/v1");
        TenantConfig tc = new TenantConfig(configMap);
        Mockito.when(tenantManager.getConfig("my-tenant")).thenReturn(Optional.ofNullable(tc));

        CdsCreateResponseDto ret = new CdsCreateResponseDto();
        ret.setStatusOk(true);
        ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);
        Mockito.when(cdsRemoteCaller.executePostCall(eq(URI.create("http://cds-kube-service:8081/mytenant/api/v1/upload/")),
                eq("/sub-path-testy"),
                eq(false),
                any(),
                any(),
                eq(false))).thenReturn(ret);

        ApsTenantApplicationUtils.setTenant("my-tenant");
        cdsStorageManager.createDirectory("/sub-path-testy",false);


        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("path", "/sub-path-testy");
        body.add("protected", false);

        Mockito.verify(cdsRemoteCaller, Mockito.times(1))
                .executePostCall(eq(URI.create("http://cds-kube-service:8081/mytenant/api/v1/upload/")),
                        eq("/sub-path-testy"),
                        eq(false),
                        any(),
                        any(),
                        eq(false));

    }

    @Test
    void shouldNotCreateDirectory() throws Exception {
        Map<String,String> configMap = Map.of("cdsPublicUrl","http://my-server/tenant1/cms-resources",
                "cdsPrivateUrl","http://cds-kube-service:8081/",
                "cdsPath","/mytenant/api/v1");
        TenantConfig tc = new TenantConfig(configMap);
        Mockito.when(tenantManager.getConfig("my-tenant")).thenReturn(Optional.ofNullable(tc));
        ApsTenantApplicationUtils.setTenant("my-tenant");

        Assertions.assertThatThrownBy(
                ()-> cdsStorageManager.createDirectory("../../sub-path-testy",false)
        ).isInstanceOf(EntRuntimeException.class).hasMessageStartingWith("Path validation failed:");

        Assertions.assertThatThrownBy(
                ()-> cdsStorageManager.createDirectory(null,false)
        ).isInstanceOf(EntRuntimeException.class).hasMessageStartingWith("Error validating path");


        CdsCreateResponseDto ret = new CdsCreateResponseDto();
        ret.setStatusOk(false);
        ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);
        Mockito.when(cdsRemoteCaller.executePostCall(any(),
                eq("/sub-path-testy"),
                eq(false),
                any(),
                any(),
                eq(false))).thenReturn(ret);

        ApsTenantApplicationUtils.setTenant("my-tenant");
        Assertions.assertThatThrownBy(
                ()-> cdsStorageManager.createDirectory("/sub-path-testy",false)
        ).isInstanceOf(EntRuntimeException.class).hasMessageStartingWith("Invalid status - Response");


        ApsTenantApplicationUtils.setTenant("my-notexist-tenant");
        Assertions.assertThatThrownBy(
                ()-> cdsStorageManager.createDirectory("/sub-path-testy",false)
        ).isInstanceOf(EntRuntimeException.class).hasMessage("Error saving file/directory");

    }

    @Test
    void shouldCreateFile() throws Exception {
        Map<String,String> configMap = Map.of("cdsPublicUrl","http://my-server/tenant1/cms-resources",
                "cdsPrivateUrl","http://cds-kube-service:8081/",
                "cdsPath","/mytenant/api/v1/");
        TenantConfig tc = new TenantConfig(configMap);
        Mockito.when(tenantManager.getConfig("my-tenant")).thenReturn(Optional.ofNullable(tc));

        CdsCreateResponseDto ret = new CdsCreateResponseDto();
        ret.setStatusOk(true);
        ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);
        Mockito.when(cdsRemoteCaller.executePostCall(any(),
                eq("/sub-path-testy/myfilename"),
                eq(false),
                any(),
                any(),
                eq(false))).thenReturn(ret);

        ApsTenantApplicationUtils.setTenant("my-tenant");
        InputStream is = new ByteArrayInputStream("testo a casos".getBytes(StandardCharsets.UTF_8));
        cdsStorageManager.saveFile("/sub-path-testy/myfilename",false, is);

        Mockito.verify(cdsRemoteCaller, Mockito.times(1))
                .executePostCall(eq(URI.create("http://cds-kube-service:8081/mytenant/api/v1/upload/")),
                        eq("/sub-path-testy/myfilename"),
                        eq(false),
                        any(),
                        any(),
                        eq(false));

    }

    @Test
    void shouldNotDeleteDirectory() throws Exception {
        Map<String,String> configMap = Map.of("cdsPublicUrl","http://my-server/tenant1/cms-resources",
                "cdsPrivateUrl","http://cds-kube-service:8081/",
                "cdsPath","/mytenant/api/v1");
        TenantConfig tc = new TenantConfig(configMap);
        Mockito.when(tenantManager.getConfig("my-tenant")).thenReturn(Optional.ofNullable(tc));
        ApsTenantApplicationUtils.setTenant("my-tenant");

        Assertions.assertThatThrownBy(
                ()-> cdsStorageManager.deleteDirectory("../../sub-path-testy",false)
        ).isInstanceOf(EntRuntimeException.class).hasMessageStartingWith("Path validation failed:");

        Assertions.assertThatThrownBy(
                ()-> cdsStorageManager.deleteDirectory(null,false)
        ).isInstanceOf(EntRuntimeException.class).hasMessageStartingWith("Error validating path");

        Mockito.when(cdsRemoteCaller.executeDeleteCall(any(),
                any(),
                eq(false))).thenReturn(false);
        Assertions.assertThatNoException().isThrownBy(() -> cdsStorageManager.deleteDirectory("/sub-path-testy",false));

        ApsTenantApplicationUtils.setTenant("my-non-exist-tenant");
        Assertions.assertThatThrownBy(
                ()-> cdsStorageManager.deleteDirectory("/sub-path-testy",false)
        ).isInstanceOf(EntRuntimeException.class).hasMessage("Error deleting file");

    }

    @Test
    void shouldBlockPathTraversalWhenCallGetStream() {
        String testFilePath = "../testfolder/test.txt";

        Map<String,String> configMap = Map.of("cdsPublicUrl","http://my-server/tenant1/cms-resources",
                "cdsPrivateUrl","http://cds-kube-service:8081/",
                "cdsPath","/mytenant/api/v1/");
        TenantConfig tc = new TenantConfig(configMap);
        Mockito.when(tenantManager.getConfig("my-tenant")).thenReturn(Optional.ofNullable(tc));

        ApsTenantApplicationUtils.setTenant("my-tenant");

        Assertions.assertThatThrownBy(
                ()-> cdsStorageManager.getStream(testFilePath,false)
        ).isInstanceOf(EntRuntimeException.class).hasMessageStartingWith("Path validation failed:");

    }

    @Test
    void shouldManageErrorWhenCallGetStream() throws Exception {

        final String baseUrl = "http://my-server/tenant1/cms-resources";
        Map<String,String> configMap = Map.of("cdsPublicUrl", baseUrl,
                "cdsPrivateUrl","http://cds-kube-service:8081/",
                "cdsPublicPath","/custom-path",
                "cdsInternalPublicSection", "/custom-path",
                "cdsPath","/mytenant/api/v1/");
        TenantConfig tc = new TenantConfig(configMap);
        Mockito.when(tenantManager.getConfig("my-tenant")).thenReturn(Optional.ofNullable(tc));
        ApsTenantApplicationUtils.setTenant("my-tenant");

        Assertions.assertThatThrownBy(
                ()-> cdsStorageManager.getStream(null,false)
        ).isInstanceOf(EntRuntimeException.class).hasMessageStartingWith("Error validating path");

        String testFilePath = "/testfolder/test.txt";
        
        Assertions.assertThatThrownBy(
                ()-> cdsStorageManager.getStream(testFilePath, false)
        ).isInstanceOf(EntResourceNotFoundException.class).hasMessageStartingWith("File \"" + testFilePath);
        
        URI testFile = URI.create( baseUrl + "/custom-path" + testFilePath);
        this.mockExecuteListFolder("http://cds-kube-service:8081/mytenant/api/v1/list/protected/testfolder", "test.txt");
        Mockito.when(cdsRemoteCaller.getFile(eq(testFile),
                any(),
                eq(false))).thenReturn(null);
        Assertions.assertThatThrownBy(
                ()-> cdsStorageManager.getStream(testFilePath,false)
        ).isInstanceOf(EntResourceNotFoundException.class).hasMessageStartingWith("Error extracting file");
        
        String testFilePathBadGateway = "/testfolder/test-badgw.txt";
        URI testFileBadGateway = URI.create( baseUrl + "/custom-path" + testFilePathBadGateway);
        Mockito.when(cdsRemoteCaller.getFile(eq(testFileBadGateway),
                any(),
                eq(false))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_GATEWAY));

        Assertions.assertThatThrownBy(
                ()-> cdsStorageManager.getStream(testFilePathBadGateway,false)
        ).isInstanceOf(EntException.class).hasMessageStartingWith("Error extracting file");

        String testFilePathNotFound = "/testfolder/test-notfound.txt";
        Assertions.assertThatThrownBy(() -> cdsStorageManager.getStream(testFilePathNotFound,false))
                .isInstanceOf(EntResourceNotFoundException.class).hasMessageStartingWith("Error extracting file");
    }

    @Test
    void shouldReturnDataWhenCallGetStream() throws Exception {
        String testFilePath = "/test-folder/test.txt";

        Map<String,String> configMap = Map.of("cdsPublicUrl","http://my-server/tenant1/cms-resources",
                "cdsPrivateUrl","http://cds-kube-service:8081/",
                "cdsPath","/mytenant/api/v1/");
        TenantConfig tc = new TenantConfig(configMap);
        Mockito.when(tenantManager.getConfig("my-tenant")).thenReturn(Optional.ofNullable(tc));
        
        Mockito.when(cdsRemoteCaller.getFile(eq(URI.create("http://my-server/tenant1/cms-resources/test-folder/test.txt")),
                any(),
                eq(false))).thenReturn(Optional.ofNullable(new ByteArrayInputStream("text random".getBytes(StandardCharsets.UTF_8))));
        
        this.mockExecuteListFolder("http://cds-kube-service:8081/mytenant/api/v1/list/test-folder", "test.txt");
        ApsTenantApplicationUtils.setTenant("my-tenant");
        InputStream is = cdsStorageManager.getStream(testFilePath,false);
        Assertions.assertThat(new BufferedReader(new InputStreamReader(is))
                .lines().collect(Collectors.joining(""))).isEqualTo("text random");
        
        Mockito.when(cdsRemoteCaller.getFile(eq(URI.create("http://cds-kube-service:8081/mytenant/api/v1/protected/test-folder/test.txt")),
                any(),
                eq(true))).thenReturn(Optional.ofNullable(new ByteArrayInputStream("text random".getBytes(StandardCharsets.UTF_8))));
        
        this.mockExecuteListFolder("http://cds-kube-service:8081/mytenant/api/v1/list/protected/test-folder", "test.txt");
        is = cdsStorageManager.getStream(testFilePath,true);
        Assertions.assertThat(new BufferedReader(new InputStreamReader(is))
                .lines().collect(Collectors.joining(""))).isEqualTo("text random");
    }

    @Test
    void shouldReturnRightUrlWhenCallGetResourceUrl() throws Exception {
        String testFilePath = "/test-folder/test.txt";
        Map<String,String> configMap = Map.of("cdsPublicUrl","http://my-server/tenant1/cms-resources",
                "cdsPrivateUrl","http://cds-tenant1-kube-service:8081/",
                "cdsPublicPath","/public",
                "cdsPath","/mytenant/api/v1/");
        TenantConfig tc = new TenantConfig(configMap);
        Mockito.when(tenantManager.getConfig("my-tenant")).thenReturn(Optional.ofNullable(tc));

        ApsTenantApplicationUtils.setTenant("my-tenant");
        String resourceUrl = cdsStorageManager.getResourceUrl(testFilePath,false);
        Assertions.assertThat(resourceUrl).isEqualTo("http://my-server/tenant1/cms-resources/public/test-folder/test.txt");

        resourceUrl = cdsStorageManager.getResourceUrl(testFilePath,true);
        Assertions.assertThat(resourceUrl).isEqualTo("http://cds-tenant1-kube-service:8081/protected/test-folder/test.txt");

        resourceUrl = cdsStorageManager.createFullPath(testFilePath,true);
        Assertions.assertThat(resourceUrl).isEqualTo("http://cds-tenant1-kube-service:8081/protected/test-folder/test.txt");
    }

    @Test
    void shouldManageUrlWhenCallGetResourceUrl() throws Exception {
        Map<String,String> configMap = Map.of("cdsPublicUrl","http://my-server/tenant1/cms-resources",
                "cdsPrivateUrl","http://cds-tenant1-kube-service:8081/",
                "cdsPath","/mytenant/api/v1/");
        TenantConfig tc = new TenantConfig(configMap);
        Mockito.when(tenantManager.getConfig("my-tenant")).thenReturn(Optional.ofNullable(tc));

        ApsTenantApplicationUtils.setTenant("my-tenant");

        Assertions.assertThatThrownBy(
                ()-> cdsStorageManager.getResourceUrl("../../test",false)
        ).isInstanceOf(EntRuntimeException.class).hasMessageStartingWith("Error extracting resource url");
    }

    @Test
    void shouldWorkFineWhenCallExists() throws Exception {
        String testFilePath = "/test-folder/test.txt";

        Map<String,String> configMap = Map.of("cdsPublicUrl","http://my-server/tenant1/cms-resources",
                "cdsPrivateUrl","http://cds-tenant1-kube-service:8081/",
                "cdsPath","/mytenant/api/v1/");
        TenantConfig tc = new TenantConfig(configMap);
        Mockito.when(tenantManager.getConfig("my-tenant")).thenReturn(Optional.ofNullable(tc));

        ApsTenantApplicationUtils.setTenant("my-tenant");

        Assertions.assertThatThrownBy(
                ()-> cdsStorageManager.exists("../../test",false)
        ).isInstanceOf(EntRuntimeException.class).hasMessageStartingWith("Path validation failed:");


        Mockito.when(cdsRemoteCaller.getFileAttributeView(any(),
                any())).thenReturn(Optional.ofNullable(new CdsFileAttributeViewDto[]{}));
        Assertions.assertThat(cdsStorageManager.exists(testFilePath,false)).isFalse();

        CdsFileAttributeViewDto file = new CdsFileAttributeViewDto();
        file.setName("test.txt");
        Mockito.when(cdsRemoteCaller.getFileAttributeView(eq(URI.create(
                "http://cds-tenant1-kube-service:8081/mytenant/api/v1/list/protected/test-folder")),
                any())).thenReturn(Optional.ofNullable(new CdsFileAttributeViewDto[]{file}));
        Assertions.assertThat(cdsStorageManager.exists(testFilePath,true)).isTrue();
    }

    @Test
    void shouldWorkFineWhenCallExistsWithRootAsEmpty() throws Exception {
        String testFilePath = "";
        Map<String,String> configMap = Map.of("cdsPublicUrl","http://my-server/tenant1/cms-resources",
                "cdsPrivateUrl","http://cds-tenant1-kube-service:8081/",
                "cdsPublicPath","/public",
                "cdsInternalPublicSection", "/public",
                "cdsPath","/mytenant/api/v1/");
        TenantConfig tc = new TenantConfig(configMap);
        Mockito.when(tenantManager.getConfig("my-tenant")).thenReturn(Optional.ofNullable(tc));

        ApsTenantApplicationUtils.setTenant("my-tenant");

        CdsFileAttributeViewDto bundlesDir = new CdsFileAttributeViewDto();
        bundlesDir.setName("bundles");
        bundlesDir.setDirectory(true);
        CdsFileAttributeViewDto cmsDir = new CdsFileAttributeViewDto();
        cmsDir.setName("cms");
        cmsDir.setDirectory(true);

        Mockito.when(cdsRemoteCaller.getFileAttributeView(eq(URI.create(
                        "http://cds-tenant1-kube-service:8081/mytenant/api/v1/list/public")),
                any())).thenReturn(Optional.ofNullable(new CdsFileAttributeViewDto[]{ bundlesDir, cmsDir }));
        Assertions.assertThat(cdsStorageManager.exists(testFilePath,false)).isTrue();

    }

    @Test
    void shouldWorkFineWhenCallGetAttributes() throws Exception {
        String testFilePath = "/test-folder/test.txt";

        Map<String,String> configMap = Map.of("cdsPublicUrl","http://my-server/tenant1/cms-resources",
                "cdsPrivateUrl","http://cds-tenant1-kube-service:8081/",
                "cdsPath","/mytenant/api/v1/");
        TenantConfig tc = new TenantConfig(configMap);
        Mockito.when(tenantManager.getConfig("my-tenant")).thenReturn(Optional.ofNullable(tc));

        ApsTenantApplicationUtils.setTenant("my-tenant");

        CdsFileAttributeViewDto file = new CdsFileAttributeViewDto();
        file.setName("test.txt");
        file.setDirectory(false);
        CdsFileAttributeViewDto dir = new CdsFileAttributeViewDto();
        dir.setName("test-folder");
        dir.setDirectory(true);


        Mockito.when(cdsRemoteCaller.getFileAttributeView(eq(URI.create(
                        "http://cds-tenant1-kube-service:8081/mytenant/api/v1/list/protected/test-folder")),
                any())).thenReturn(Optional.ofNullable(new CdsFileAttributeViewDto[]{file, dir}));

        BasicFileAttributeView response = cdsStorageManager.getAttributes(testFilePath,true);
        Assertions.assertThat(response).isNotNull();

        Mockito.when(cdsRemoteCaller.getFileAttributeView(eq(URI.create(
                        "http://cds-tenant1-kube-service:8081/mytenant/api/v1/list/protected/test-folder")),
                any())).thenReturn(Optional.ofNullable(new CdsFileAttributeViewDto[]{dir}));
        response = cdsStorageManager.getAttributes(testFilePath,true);
        Assertions.assertThat(response).isNull();

    }

    @Test
    void shouldFilterWorkFineWhenCallList() throws Exception {
        String testFilePath = "/test-folder/";

        Map<String,String> configMap = Map.of("cdsPublicUrl","http://my-server/tenant1/cms-resources",
                "cdsPrivateUrl","http://cds-tenant1-kube-service:8081/",
                "cdsPath","/mytenant/api/v1/");
        TenantConfig tc = new TenantConfig(configMap);
        Mockito.when(tenantManager.getConfig("my-tenant")).thenReturn(Optional.ofNullable(tc));

        ApsTenantApplicationUtils.setTenant("my-tenant");

        Assertions.assertThatThrownBy(
                ()-> cdsStorageManager.exists("../../test",false)
        ).isInstanceOf(EntRuntimeException.class).hasMessageStartingWith("Path validation failed:");


        CdsFileAttributeViewDto dir1 = new CdsFileAttributeViewDto();
        dir1.setName("/test");
        dir1.setDirectory(true);
        CdsFileAttributeViewDto file1 = new CdsFileAttributeViewDto();
        file1.setName("test.txt");
        file1.setDirectory(false);
        Mockito.when(cdsRemoteCaller.getFileAttributeView(any(),
                any())).thenReturn(Optional.ofNullable(new CdsFileAttributeViewDto[]{dir1, file1}));
        String[] list = cdsStorageManager.listDirectory(testFilePath,false);
        Assertions.assertThat(list).isNotEmpty().hasSize(1);

        CdsFileAttributeViewDto file = new CdsFileAttributeViewDto();
        file.setName("test.txt");
        file.setDirectory(false);
        Mockito.when(cdsRemoteCaller.getFileAttributeView(eq(URI.create(
                        "http://cds-tenant1-kube-service:8081/mytenant/api/v1/list/protected/test-folder/")),
                any())).thenReturn(Optional.ofNullable(new CdsFileAttributeViewDto[]{file}));
        list = cdsStorageManager.listFile(testFilePath,true);
        Assertions.assertThat(list).isNotEmpty().hasSize(1);

    }

    @Test
    void shouldReadFile() throws Exception {
        String testFilePath = "/testfolder/test.txt";

        Map<String,String> configMap = Map.of("cdsPublicUrl","http://my-server/tenant1/cms-resources",
                "cdsPrivateUrl","http://cdsmaster-kube-service:8081/",
                "cdsPath","/mytenant/api/v1/");
        TenantConfig tc = new TenantConfig(configMap);
        Mockito.when(tenantManager.getConfig("my-tenant")).thenReturn(Optional.ofNullable(tc));

        Mockito.when(cdsRemoteCaller.getFile(any(),
                any(),
                eq(false))).thenReturn(Optional.ofNullable(new ByteArrayInputStream("text random".getBytes(StandardCharsets.UTF_8))));
        
        this.mockExecuteListFolder("http://cdsmaster-kube-service:8081/mytenant/api/v1/list/testfolder", "test.txt");
        
        ApsTenantApplicationUtils.setTenant("my-tenant");
        Assertions.assertThat(cdsStorageManager.readFile(testFilePath,false))
                .isEqualTo("text random");
    }

    @Test
    void shouldManageExceptionWhenReadFile() throws Exception {
        String testFilePath = "/testfolder/subfolder/test.txt";

        Map<String,String> configMap = Map.of("cdsPublicUrl","http://my-server/tenant1/cms-resources",
                "cdsPrivateUrl","http://cds-kube-service:8081/",
                "cdsPath","/mytenant/api/v1/");
        TenantConfig tc = new TenantConfig(configMap);

        Mockito.when(tenantManager.getConfig("my-tenant")).thenReturn(Optional.ofNullable(tc));
        Mockito.when(cdsRemoteCaller.getFile(any(),
                any(),
                eq(false))).thenReturn(Optional.ofNullable(new ByteArrayInputStream("text random".getBytes(StandardCharsets.UTF_8))));

        ApsTenantApplicationUtils.setTenant("my-tenant");

        try (MockedStatic<IOUtils> ioUtils = Mockito.mockStatic(IOUtils.class)) {
            ioUtils.when(() -> IOUtils.toString(any(InputStream.class), eq(StandardCharsets.UTF_8)))
                    .thenThrow(new IOException());
            this.mockExecuteListFolder("http://cds-kube-service:8081/mytenant/api/v1/list/testfolder/subfolder", "test.txt");
            Assertions.assertThatThrownBy(() -> cdsStorageManager.readFile(testFilePath, false))
                    .isInstanceOf(EntException.class)
                    .hasMessageStartingWith("Error extracting text");
        }

        Mockito.when(cdsRemoteCaller.getFile(any(),
                any(),
                eq(false))).thenThrow(new EntRuntimeException("testEx"));
        Assertions.assertThatThrownBy(() -> cdsStorageManager.readFile(testFilePath,false))
                .isInstanceOf(EntRuntimeException.class)
                .hasMessage("testEx");

    }

    @Test
    void shouldEditFile() throws Exception {
        Map<String,String> configMap = Map.of("cdsPublicUrl","http://my-server/tenant1/cms-resources",
                "cdsPrivateUrl","http://cds-kube-service:8081/",
                "cdsPath","/mytenant/api/v1/");
        TenantConfig tc = new TenantConfig(configMap);
        Mockito.when(tenantManager.getConfig("my-tenant")).thenReturn(Optional.ofNullable(tc));

        CdsCreateResponseDto ret = new CdsCreateResponseDto();
        ret.setStatusOk(true);
        ret.setList(Arrays.asList(new CdsCreateRowResponseDto[]{}));
        ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);
        Mockito.when(cdsRemoteCaller.executePostCall(any(),
                eq("/sub-path-testy/myfilename"),
                eq(false),
                any(),
                any(),
                eq(false))).thenReturn(ret);

        ApsTenantApplicationUtils.setTenant("my-tenant");
        InputStream is = new ByteArrayInputStream("testo a casos".getBytes(StandardCharsets.UTF_8));
        cdsStorageManager.editFile("/sub-path-testy/myfilename",false, is);


        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("path", "/sub-path-testy");
        body.add("protected", false);
        body.add("filename", "myfilename");
        body.add("file", new InputStreamResource(is));

        Mockito.verify(cdsRemoteCaller, Mockito.times(1))
                .executePostCall(eq(URI.create("http://cds-kube-service:8081/mytenant/api/v1/upload/")),
                        eq("/sub-path-testy/myfilename"),
                        eq(false),
                        any(),
                        any(),
                        eq(false));

    }

    //@Test
    void testListAttributes() throws Throwable {
        Map<String,String> configMap = Map.of("cdsPublicUrl","http://my-server/tenant1/cms-resources",
                "cdsPrivateUrl","http://cds-kube-service:8081/",
                "cdsPath","/mytenant/api/v1/");
        TenantConfig tc = new TenantConfig(configMap);
        Mockito.when(tenantManager.getConfig("my-tenant")).thenReturn(Optional.ofNullable(tc));

        ApsTenantApplicationUtils.setTenant("my-tenant");

        BasicFileAttributeView[] fileAttributes = cdsStorageManager.listAttributes("", false);
        boolean containsCms = false;
        boolean prevDirectory = true;
        String prevName = null;
        for (int i = 0; i < fileAttributes.length; i++) {
            BasicFileAttributeView bfav = fileAttributes[i];
            if (!prevDirectory && bfav.isDirectory()) {
                fail();
            }
            if (bfav.isDirectory() && bfav.getName().equals("cms")) {
                containsCms = true;
            }
            if ((bfav.isDirectory() == prevDirectory) && null != prevName) {
                assertTrue(bfav.getName().compareTo(prevName) > 0);
            }
            prevName = bfav.getName();
            prevDirectory = bfav.isDirectory();
        }
        assertTrue(containsCms);
    }
    
    private void mockExecuteListFolder(String uri, String filename) {
        CdsFileAttributeViewDto file = new CdsFileAttributeViewDto();
        file.setName(filename);
        Mockito.when(cdsRemoteCaller.getFileAttributeView(eq(URI.create(uri)),
                any())).thenReturn(Optional.ofNullable(new CdsFileAttributeViewDto[]{file}));
    }

}
