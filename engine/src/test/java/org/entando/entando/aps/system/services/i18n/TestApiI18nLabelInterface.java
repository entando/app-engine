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
package org.entando.entando.aps.system.services.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.i18n.II18nManager;
import com.agiletec.aps.util.ApsProperties;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;
import org.entando.entando.aps.system.services.api.ApiBaseTestCase;
import org.entando.entando.aps.system.services.api.LegacyApiUnmarshaller;
import org.entando.entando.aps.system.services.api.model.ApiMethod;
import org.entando.entando.aps.system.services.api.model.ApiResource;
import org.entando.entando.aps.system.services.api.model.StringApiResponse;
import org.entando.entando.aps.system.services.api.server.IResponseBuilder;
import org.entando.entando.aps.system.services.i18n.model.JAXBI18nLabel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * @author E.Santoboni
 */
class TestApiI18nLabelInterface extends ApiBaseTestCase {
	
    @Test
	void testGetXmlLabel() throws Throwable {
		MediaType mediaType = MediaType.APPLICATION_XML;
		this.testGetLabel(mediaType, "admin", "PAGE_TITLE");
	}
	
	@Test
	void testGetJsonLabel() throws Throwable {
		MediaType mediaType = MediaType.APPLICATION_JSON;
		this.testGetLabel(mediaType, "admin", "PAGE_TITLE");
	}
	
	@Test
	void testCreateNewLabelFromXml() throws Throwable {
		MediaType mediaType = MediaType.APPLICATION_XML;
		this.testCreateNewLabel(mediaType);
	}
	
	@Test
	void testCreateNewContentFromJson() throws Throwable {
		MediaType mediaType = MediaType.APPLICATION_JSON;
		this.testCreateNewLabel(mediaType);
	}
	
	protected void testCreateNewLabel(MediaType mediaType) throws Throwable {
		String key = "TEST_LABEL_KEY";
		String label = this.i18nManager.getLabel(key, "it");
		assertNull(label);
		ApsProperties labels = new ApsProperties();
		labels.put("en", "Test label");
		labels.put("it", "Label di Test");
		JAXBI18nLabel jaxbLabel = new JAXBI18nLabel(key, labels);
		ApiResource labelResource = this.getApiCatalogManager().getResource("core", "i18nlabel");
		ApiMethod postMethod = labelResource.getPostMethod();
		Properties properties = super.createApiProperties("admin", "it", mediaType);
		try {
			Object response = this.getResponseBuilder().createResponse(postMethod, jaxbLabel, properties);
			assertNotNull(response);
			assertTrue(response instanceof StringApiResponse);
			assertEquals(IResponseBuilder.SUCCESS, ((StringApiResponse) response).getResult());
			label = this.i18nManager.getLabel(key, "it");
			assertEquals("Label di Test", label);
		} catch (Exception e) {
			throw e;
		} finally {
			this.i18nManager.deleteLabelGroup(key);
		}
	}
	
	protected JAXBI18nLabel testGetLabel(MediaType mediaType, String username, String key) throws Throwable {
		ApiResource contentResource = 
				this.getApiCatalogManager().getResource("core", "i18nlabel");
		ApiMethod getMethod = contentResource.getGetMethod();
		
		Properties properties = super.createApiProperties(username, "en", mediaType);
		properties.put("key", key);
		Object result = this.getResponseBuilder().createResponse(getMethod, properties);
		assertNotNull(result);
		ApiI18nLabelInterface apiLabelInterface = (ApiI18nLabelInterface) this.getApplicationContext().getBean("ApiI18nLabelInterface");
		Object singleResult = apiLabelInterface.getLabel(properties);
		assertNotNull(singleResult);
		String toString = this.marshall(singleResult, mediaType);
		InputStream stream = new ByteArrayInputStream(toString.getBytes());
		JAXBI18nLabel jaxbLabel = this.unmarshaller.unmarshal(mediaType, stream, JAXBI18nLabel.class);
		assertNotNull(jaxbLabel);
		return jaxbLabel;
	}
	
    @BeforeEach
	protected void init() {
        super.init();
		this.i18nManager = (II18nManager) this.getApplicationContext().getBean(SystemConstants.I18N_MANAGER);
		this.unmarshaller = (LegacyApiUnmarshaller) this.getApplicationContext().getBean(SystemConstants.LEGACY_API_UNMARSHALLER);
    }
	
	private II18nManager i18nManager;
	private LegacyApiUnmarshaller unmarshaller;
	
}
