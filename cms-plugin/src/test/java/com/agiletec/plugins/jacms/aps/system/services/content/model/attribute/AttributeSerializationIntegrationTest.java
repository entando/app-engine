package com.agiletec.plugins.jacms.aps.system.services.content.model.attribute;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.baseconfig.ConfigInterface;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.util.ApplicationContextProvider;
import com.agiletec.plugins.jacms.aps.system.JacmsSystemConstants;
import com.agiletec.plugins.jacms.aps.system.services.content.IContentManager;
import com.agiletec.plugins.jacms.aps.system.services.content.parse.attribute.ResourceAttributeHandler;
import com.agiletec.plugins.jacms.aps.system.services.linkresolver.ILinkResolverManager;
import com.agiletec.plugins.jacms.aps.system.services.resource.IResourceManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.ContextLoader;

class AttributeSerializationIntegrationTest extends BaseTestCase {

    private ConfigInterface configManager;
    private ILangManager langManager;
    private IResourceManager resourceManager;
    private IPageManager pageManager;
    private IContentManager contentManager;
    private ILinkResolverManager linkResolverManager;

    @BeforeEach
    public void init() {
        this.configManager = (ConfigInterface) this.getService(SystemConstants.BASE_CONFIG_MANAGER);
        this.langManager = (ILangManager) this.getService(SystemConstants.LANGUAGE_MANAGER);
        this.resourceManager = (IResourceManager) this.getService(JacmsSystemConstants.RESOURCE_MANAGER);
        this.pageManager = (IPageManager) this.getService(SystemConstants.PAGE_MANAGER);
        this.contentManager = (IContentManager) this.getService(JacmsSystemConstants.CONTENT_MANAGER);
        this.linkResolverManager = (ILinkResolverManager) this.getService(JacmsSystemConstants.LINK_RESOLVER_MANAGER);
    }

    @Test
    void testSerializeAttachAttribute() throws Exception {
        AttachAttribute attribute = new AttachAttribute();
        attribute.setName("testAttach");
        attribute.setResourceManager(resourceManager);
        // current web application context available
        attribute = testSerializeAndDeserialize(attribute);
        Assertions.assertNotNull(attribute.getResourceManager());
        // no current web application context -> re-wired through the ApplicationContextProvider fallback
        attribute = testSerializeAndDeserializeNullApplicationContext(attribute);
        Assertions.assertNotNull(attribute.getResourceManager());
        // neither the current context nor the provider are available -> stays null (warn path).
        // The raw field is checked (not the getter) so the self-healing getter does not re-resolve.
        attribute = testSerializeAndDeserializeNoContextAvailable(attribute);
        Assertions.assertNull(ReflectionTestUtils.getField(attribute, "resourceManager"));
    }

    @Test
    void testSerializeCmsHypertextAttribute() throws Exception {
        CmsHypertextAttribute attribute = new CmsHypertextAttribute();
        attribute.setName("testCmsHypertext");
        attribute.setContentManager(contentManager);
        attribute.setPageManager(pageManager);
        attribute.setResourceManager(resourceManager);
        attribute.setLangManager(langManager);
        // current web application context available
        attribute = testSerializeAndDeserialize(attribute);
        Assertions.assertNotNull(attribute.getContentManager());
        Assertions.assertNotNull(attribute.getPageManager());
        Assertions.assertNotNull(attribute.getResourceManager());
        Assertions.assertNotNull(ReflectionTestUtils.invokeGetterMethod(attribute, "langManager"));
        // no current web application context -> re-wired through the ApplicationContextProvider fallback
        attribute = testSerializeAndDeserializeNullApplicationContext(attribute);
        Assertions.assertNotNull(attribute.getContentManager());
        Assertions.assertNotNull(attribute.getPageManager());
        Assertions.assertNotNull(attribute.getResourceManager());
        // neither the current context nor the provider are available -> stays null (warn path).
        // Raw fields are checked (not the getters) so the self-healing getters do not re-resolve.
        attribute = testSerializeAndDeserializeNoContextAvailable(attribute);
        Assertions.assertNull(ReflectionTestUtils.getField(attribute, "contentManager"));
        Assertions.assertNull(ReflectionTestUtils.getField(attribute, "pageManager"));
        Assertions.assertNull(ReflectionTestUtils.getField(attribute, "resourceManager"));
    }

    @Test
    void testSerializeLinkAttributeAttribute() throws Exception {
        LinkAttribute attribute = new LinkAttribute();
        attribute.setName("testLink");
        attribute.setContentManager(contentManager);
        attribute.setPageManager(pageManager);
        attribute.setResourceManager(resourceManager);
        attribute.setLinkResolverManager(linkResolverManager);
        attribute.setLangManager(langManager);
        // current web application context available
        attribute = testSerializeAndDeserialize(attribute);
        Assertions.assertNotNull(attribute.getContentManager());
        Assertions.assertNotNull(attribute.getPageManager());
        Assertions.assertNotNull(attribute.getResourceManager());
        Assertions.assertNotNull(attribute.getLinkResolverManager());
        Assertions.assertNotNull(ReflectionTestUtils.invokeGetterMethod(attribute, "langManager"));
        // no current web application context -> re-wired through the ApplicationContextProvider fallback
        attribute = testSerializeAndDeserializeNullApplicationContext(attribute);
        Assertions.assertNotNull(attribute.getContentManager());
        Assertions.assertNotNull(attribute.getPageManager());
        Assertions.assertNotNull(attribute.getResourceManager());
        Assertions.assertNotNull(attribute.getLinkResolverManager());
        // neither the current context nor the provider are available -> stays null (warn path).
        // Raw fields are checked (not the getters) so the self-healing getters do not re-resolve.
        attribute = testSerializeAndDeserializeNoContextAvailable(attribute);
        Assertions.assertNull(ReflectionTestUtils.getField(attribute, "contentManager"));
        Assertions.assertNull(ReflectionTestUtils.getField(attribute, "pageManager"));
        Assertions.assertNull(ReflectionTestUtils.getField(attribute, "resourceManager"));
        Assertions.assertNull(ReflectionTestUtils.getField(attribute, "linkResolverManager"));
        Assertions.assertNull(ReflectionTestUtils.getField(attribute, "_langManager"));
    }

    @Test
    void testSerializeResourceAttributeHandler() throws Exception {
        ResourceAttributeHandler attributeHandler = new ResourceAttributeHandler();
        attributeHandler.setResourceManager(resourceManager);
        // current web application context available
        attributeHandler = testSerializeAndDeserialize(attributeHandler);
        Assertions.assertNotNull(ReflectionTestUtils.invokeGetterMethod(attributeHandler, "resourceManager"));
        // no current web application context -> re-wired through the ApplicationContextProvider fallback
        attributeHandler = testSerializeAndDeserializeNullApplicationContext(attributeHandler);
        Assertions.assertNotNull(ReflectionTestUtils.invokeGetterMethod(attributeHandler, "resourceManager"));
        // neither the current context nor the provider are available -> stays null (warn path).
        attributeHandler = testSerializeAndDeserializeNoContextAvailable(attributeHandler);
        Assertions.assertNull(ReflectionTestUtils.getField(attributeHandler, "resourceManager"));
    }

    private <T> T testSerializeAndDeserializeNullApplicationContext(T attribute) throws Exception {
        try (MockedStatic<ContextLoader> contextLoader = Mockito.mockStatic(ContextLoader.class)) {
            contextLoader.when(() -> ContextLoader.getCurrentWebApplicationContext()).thenReturn(null);
            return testSerializeAndDeserialize(attribute);
        }
    }

    private <T> T testSerializeAndDeserializeNoContextAvailable(T attribute) throws Exception {
        try (MockedStatic<ContextLoader> contextLoader = Mockito.mockStatic(ContextLoader.class);
                MockedStatic<ApplicationContextProvider> provider =
                        Mockito.mockStatic(ApplicationContextProvider.class)) {
            contextLoader.when(() -> ContextLoader.getCurrentWebApplicationContext()).thenReturn(null);
            provider.when(() -> ApplicationContextProvider.resolveBean(Mockito.any())).thenReturn(null);
            provider.when(ApplicationContextProvider::getBeanFactory).thenReturn(null);
            return testSerializeAndDeserialize(attribute);
        }
    }

    private <T> T testSerializeAndDeserialize(T object) throws Exception {

        byte[] data;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(os)) {
            objectOutputStream.writeObject(object);
            data = os.toByteArray();
        }

        try (ByteArrayInputStream is = new ByteArrayInputStream(data);
                ObjectInputStream objectInputStream = new ObjectInputStream(is)) {
            return (T) objectInputStream.readObject();
        }
    }
}
