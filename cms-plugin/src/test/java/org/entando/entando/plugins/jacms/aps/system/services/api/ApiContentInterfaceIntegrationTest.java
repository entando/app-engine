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
package org.entando.entando.plugins.jacms.aps.system.services.api;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.common.entity.model.attribute.AbstractComplexAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.AbstractListAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.util.DateConverter;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.content.model.Content;
import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.AbstractResourceAttribute;
import com.agiletec.plugins.jacms.aps.system.services.content.model.attribute.LinkAttribute;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.entando.entando.aps.system.services.api.ApiBaseTestCase;
import org.entando.entando.aps.system.services.api.LegacyApiUnmarshaller;
import org.entando.entando.aps.system.services.api.model.ApiMethod;
import org.entando.entando.aps.system.services.api.model.ApiResource;
import org.entando.entando.aps.system.services.api.server.IResponseBuilder;
import org.entando.entando.plugins.jacms.aps.system.services.api.model.CmsApiResponse;
import org.entando.entando.plugins.jacms.aps.system.services.api.model.JAXBContent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * @author E.Santoboni
 */
class ApiContentInterfaceIntegrationTest extends ApiBaseTestCase {

	@Test
    void testGetXmlContent() throws Throwable {
		MediaType mediaType = MediaType.APPLICATION_XML;
		this.testGetContent(mediaType, "admin", "ALL4", "it");
		this.testGetContent(mediaType, "admin", "ART111", "it");
	}

	@Test
    void testGetJsonContent() throws Throwable {
		MediaType mediaType = MediaType.APPLICATION_JSON;
		this.testGetContent(mediaType, "admin", "ALL4", "en");
		this.testGetContent(mediaType, "admin", "ART111", "en");
	}

	@Test
    void testCreateNewContentFromXml() throws Throwable {
		MediaType mediaType = MediaType.APPLICATION_XML;
		this.testCreateNewContent(mediaType, "ALL4");
	}

	@Test
    void testCreateNewContentFromJson() throws Throwable {
		MediaType mediaType = MediaType.APPLICATION_JSON;
		this.testCreateNewContent(mediaType, "ALL4");
		this.testCreateNewContent(mediaType, "ART111");
	}

	protected void testCreateNewContent(MediaType mediaType, String contentId) throws Throwable {
		String dateNow = DateConverter.getFormattedDate(new Date(), JacmsSystemConstants.CONTENT_METADATA_DATE_FORMAT);
		EntitySearchFilter filter = new EntitySearchFilter(IContentManager.CONTENT_CREATION_DATE_FILTER_KEY, false, dateNow, null);
		EntitySearchFilter[] filters = {filter};
		List<String> ids = this.contentManager.searchId(filters);
		Assertions.assertTrue(ids.isEmpty());
		JAXBContent jaxbContent = this.testGetContent(mediaType, "admin", contentId, "it");
		ApiResource contentResource = this.getApiCatalogManager().getResource("jacms", "content");
		ApiMethod postMethod = contentResource.getPostMethod();
		Properties properties = super.createApiProperties("admin", "it", mediaType);
		try {
			jaxbContent.setId(null);
			Object response = this.getResponseBuilder().createResponse(postMethod, jaxbContent, properties);
			Assertions.assertNotNull(response);
			Assertions.assertTrue(response instanceof CmsApiResponse);
			Assertions.assertEquals(IResponseBuilder.SUCCESS, ((CmsApiResponse) response).getResult().getStatus());
			ids = this.contentManager.searchId(filters);
			Assertions.assertEquals(1, ids.size());
			String newContentId = ids.get(0);
			Assertions.assertNotEquals(newContentId, contentId);
			Content newContent = this.contentManager.loadContent(newContentId, false);
			Content masterContent = this.contentManager.loadContent(contentId, true);
			List<AttributeInterface> attributes = masterContent.getAttributeList();
			for (int i = 0; i < attributes.size(); i++) {
				AttributeInterface attribute = attributes.get(i);
				AttributeInterface newAttribute = (AttributeInterface) newContent.getAttribute(attribute.getName());
				this.checkAttributes(attribute, newAttribute);
			}
		} catch (Exception e) {
			throw e;
		} finally {
			ids = this.contentManager.searchId(filters);
			if (!ids.isEmpty()) {
				for (int i = 0; i < ids.size(); i++) {
					String id = ids.get(i);
					Content content = this.contentManager.loadContent(id, false);
					this.contentManager.deleteContent(content);
				}
			}
		}
	}

	private void checkAttributes(AttributeInterface oldAttribute, AttributeInterface newAttribute) {
		if (null == newAttribute) {
			Assertions.fail();
		}
		Assertions.assertEquals(oldAttribute.getName(), newAttribute.getName());
		Assertions.assertEquals(oldAttribute.getType(), newAttribute.getType());
		if (!oldAttribute.isSimple()) {
			if (oldAttribute instanceof AbstractListAttribute) {
				List<AttributeInterface> oldListAttributes = ((AbstractComplexAttribute) oldAttribute).getAttributes();
				List<AttributeInterface> newListAttributes = ((AbstractComplexAttribute) newAttribute).getAttributes();
				Assertions.assertEquals(oldListAttributes.size(), newListAttributes.size());
				for (int i = 0; i < oldListAttributes.size(); i++) {
					AttributeInterface oldElement = oldListAttributes.get(i);
					AttributeInterface newElement = newListAttributes.get(i);
					this.checkAttributes(oldElement, newElement);
				}
			} else if (oldAttribute instanceof CompositeAttribute) {
				Map<String, AttributeInterface> oldAttributeMap = ((CompositeAttribute) oldAttribute).getAttributeMap();
				Map<String, AttributeInterface> newAttributeMap = ((CompositeAttribute) newAttribute).getAttributeMap();
				Assertions.assertEquals(oldAttributeMap.size(), newAttributeMap.size());
				Iterator<String> iterator = oldAttributeMap.keySet().iterator();
				while (iterator.hasNext()) {
					String key = iterator.next();
					AttributeInterface oldElement = oldAttributeMap.get(key);
					AttributeInterface newElement = newAttributeMap.get(key);
					this.checkAttributes(oldElement, newElement);
				}
			}
		} else {
			if (oldAttribute instanceof AbstractResourceAttribute || oldAttribute instanceof LinkAttribute) {
				return;
			}
			Assertions.assertEquals(oldAttribute.getValue(), newAttribute.getValue());
		}
	}

	protected JAXBContent testGetContent(MediaType mediaType, String username, String contentId, String langCode) throws Throwable {
		ApiResource contentResource
				= this.getApiCatalogManager().getResource("jacms", "content");
		ApiMethod getMethod = contentResource.getGetMethod();
		Properties properties = super.createApiProperties(username, langCode, mediaType);
		properties.put("id", contentId);
		Object result = this.getResponseBuilder().createResponse(getMethod, properties);
		Assertions.assertNotNull(result);
		ApiContentInterface apiContentInterface = (ApiContentInterface) this.getApplicationContext().getBean("jacmsApiContentInterface");
		Object singleResult = apiContentInterface.getContent(properties);
		Assertions.assertNotNull(singleResult);
		String toString = this.marshall(singleResult, mediaType);
		InputStream stream = new ByteArrayInputStream(toString.getBytes());
		JAXBContent jaxbContent = unmarshaller.unmarshal(mediaType, stream, JAXBContent.class);
		Assertions.assertNotNull(jaxbContent);
		return jaxbContent;
	}

    @Override
    @BeforeEach
	public void init() {
        super.init();
		this.contentManager = (IContentManager) this.getApplicationContext().getBean(JacmsSystemConstants.CONTENT_MANAGER);
		this.unmarshaller = (LegacyApiUnmarshaller) this.getApplicationContext().getBean(SystemConstants.LEGACY_API_UNMARSHALLER);
	}

	private IContentManager contentManager;
	private LegacyApiUnmarshaller unmarshaller;

}
