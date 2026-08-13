package com.agiletec.aps.system.common.entity.model.attribute;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.util.ApplicationContextProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

@Isolated
class AttributeSerializationIntegrationTest extends BaseTestCase {

    private ILangManager langManager;

    @BeforeEach
    public void init() {
        this.langManager = (ILangManager) this.getService(SystemConstants.LANGUAGE_MANAGER);
    }

    @Test
    void testSerializeTextAttribute() throws Exception {
        TextAttribute attribute = new TextAttribute();
        attribute.setName("testAttribute");
        attribute.setLangManager(langManager);
        // current web application context available -> manager re-wired from it
        attribute = testSerializeAndDeserializeWithApplicationContext(attribute);
        Assertions.assertNotNull(attribute.getLangManager());
        // no current web application context (e.g. Redis/lettuce thread or startup):
        // manager is re-wired through the ApplicationContextProvider fallback
        attribute = testSerializeAndDeserializeNullApplicationContext(attribute);
        Assertions.assertNotNull(attribute.getLangManager());
        // neither the current context nor the provider are available -> stays null (warn path).
        // The raw field is checked (not the getter) so the self-healing getter does not re-resolve.
        attribute = testSerializeAndDeserializeNoContextAvailable(attribute);
        Assertions.assertNull(ReflectionTestUtils.getField(attribute, "_langManager"));
    }

    @Test
    void testSerializeEnumeratorAttribute() throws Exception {
        EnumeratorAttribute attribute = new EnumeratorAttribute();
        attribute.setName("testEnumerator");
        attribute.setBeanFactory(this.getApplicationContext());
        attribute.setLangManager(langManager);
        // current web application context available
        attribute = testSerializeAndDeserializeWithApplicationContext(attribute);
        Assertions.assertNotNull(attribute.getBeanFactory());
        Assertions.assertNotNull(attribute.getLangManager());
        // no current web application context -> re-wired through the ApplicationContextProvider fallback
        attribute = testSerializeAndDeserializeNullApplicationContext(attribute);
        Assertions.assertNotNull(attribute.getBeanFactory());
        Assertions.assertNotNull(attribute.getLangManager());
        // neither the current context nor the provider are available -> stays null (warn path).
        // The raw fields are checked (not the getters) so the self-healing getters do not re-resolve.
        attribute = testSerializeAndDeserializeNoContextAvailable(attribute);
        Assertions.assertNull(ReflectionTestUtils.getField(attribute, "_beanFactory"));
        Assertions.assertNull(ReflectionTestUtils.getField(attribute, "_langManager"));
    }

    private <T> T testSerializeAndDeserializeWithApplicationContext(T attribute) throws Exception {
        try (MockedStatic<ContextLoader> contextLoader = Mockito.mockStatic(ContextLoader.class)) {
            contextLoader.when(ContextLoader::getCurrentWebApplicationContext)
                    .thenReturn((WebApplicationContext) getApplicationContext());
            return testSerializeAndDeserialize(attribute);
        }
    }

    private <T> T testSerializeAndDeserializeNullApplicationContext(T attribute) throws Exception {
        try (MockedStatic<ContextLoader> contextLoader = Mockito.mockStatic(ContextLoader.class)) {
            contextLoader.when(ContextLoader::getCurrentWebApplicationContext).thenReturn(null);
            return testSerializeAndDeserialize(attribute);
        }
    }

    private <T> T testSerializeAndDeserializeNoContextAvailable(T attribute) throws Exception {
        try (MockedStatic<ContextLoader> contextLoader = Mockito.mockStatic(ContextLoader.class);
                MockedStatic<ApplicationContextProvider> provider =
                        Mockito.mockStatic(ApplicationContextProvider.class)) {
            contextLoader.when(ContextLoader::getCurrentWebApplicationContext).thenReturn(null);
            provider.when(() -> ApplicationContextProvider.resolveBean(Mockito.any())).thenReturn(null);
            provider.when(ApplicationContextProvider::getBeanFactory).thenReturn(null);
            return testSerializeAndDeserialize(attribute);
        }
    }

    private <T> T testSerializeAndDeserialize(T attribute) throws Exception {

        byte[] data;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(os)) {
            objectOutputStream.writeObject(attribute);
            data = os.toByteArray();
        }

        try (ByteArrayInputStream is = new ByteArrayInputStream(data);
                ObjectInputStream objectInputStream = new ObjectInputStream(is)) {
            return (T) objectInputStream.readObject();
        }
    }
}
