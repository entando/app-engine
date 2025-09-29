package com.agiletec.apsadmin.system.entity.type;

import static com.agiletec.apsadmin.system.entity.type.ICompositeAttributeConfigAction.COMPOSITE_ATTRIBUTE_ON_EDIT_SESSION_PARAM;
import static com.agiletec.apsadmin.system.entity.type.IEntityTypeConfigAction.ENTITY_TYPE_ON_EDIT_SESSION_PARAM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import com.agiletec.aps.system.common.entity.IEntityManager;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoListAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.TextAttribute;
import com.agiletec.apsadmin.system.ApsAdminSystemConstants;
import com.agiletec.apsadmin.system.BaseAction;
import org.apache.struts2.action.Action;
import org.apache.struts2.text.TextProvider;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;

@ExtendWith(MockitoExtension.class)
class CompositeAttributeConfigActionTest {

    private static final String COMPOSITE_ATTRIBUTE_NAME = "my_composite_attribute";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession session;
    @Mock
    private BeanFactory beanFactory;
    @Mock
    private TextProvider textProvider;
    @InjectMocks
    private CompositeAttributeConfigAction action;

    @BeforeEach
    void setUp() {
        Mockito.when(request.getSession()).thenReturn(session);

        CompositeAttribute compositeAttribute = new CompositeAttribute();
        compositeAttribute.setName(COMPOSITE_ATTRIBUTE_NAME);
        addTextAttribute(compositeAttribute, "attribute1");
        addTextAttribute(compositeAttribute, "attribute2");
        Mockito.lenient().when(session.getAttribute(COMPOSITE_ATTRIBUTE_ON_EDIT_SESSION_PARAM)).thenReturn(compositeAttribute);
    }

    private void addTextAttribute(CompositeAttribute compositeAttribute, String attributeName) {
        TextAttribute attribute = new TextAttribute();
        attribute.setName(attributeName);
        compositeAttribute.getAttributes().add(attribute);
        compositeAttribute.getAttributeMap().put(attribute.getName(), attribute);
    }

    @Test
    void testMoveAttribute() {
        action.setMovement(ApsAdminSystemConstants.MOVEMENT_UP_CODE);
        action.setAttributeIndex(1);
        action.moveAttributeElement();
        Mockito.verify(session, Mockito.times(1))
                .setAttribute(Mockito.eq(COMPOSITE_ATTRIBUTE_ON_EDIT_SESSION_PARAM), any());
    }

    @Test
    void testRemoveAttributeElement() {
        action.setAttributeIndex(0);
        action.removeAttributeElement();
        Mockito.verify(session, Mockito.times(1))
                .setAttribute(Mockito.eq(COMPOSITE_ATTRIBUTE_ON_EDIT_SESSION_PARAM), any());
    }


    @Test
    void testSaveAttributeElement() {
        String entityManagerName = "EntityManagerName";
        String attributeTypeCode = "typeCode";
        Mockito.when(session.getAttribute(IEntityTypeConfigAction.ENTITY_TYPE_MANAGER_SESSION_PARAM))
                .thenReturn(entityManagerName);
        Map<String, AttributeInterface> attributeTypes = new HashMap<>();
        attributeTypes.put(attributeTypeCode, new TextAttribute());
        IEntityManager entityManager = Mockito.mock(IEntityManager.class);
        Mockito.when(beanFactory.getBean(entityManagerName)).thenReturn(entityManager);
        Mockito.when(entityManager.getEntityAttributePrototypes()).thenReturn(attributeTypes);
        action.setAttributeTypeCode(attributeTypeCode);
        action.saveAttributeElement();
        Mockito.verify(session, Mockito.times(1))
                .setAttribute(Mockito.eq(COMPOSITE_ATTRIBUTE_ON_EDIT_SESSION_PARAM), any());
    }

    @Test
    void shouldMethodNotAddAttributeElement() {

        String entityManagerName = "EntityManagerName";
        Mockito.when(session.getAttribute(IEntityTypeConfigAction.ENTITY_TYPE_MANAGER_SESSION_PARAM))
                .thenReturn(entityManagerName);

        String attributeTypeCode = "typeCode";
        Map<String, AttributeInterface> attributeTypes = new HashMap<>();
        attributeTypes.put(attributeTypeCode, new TextAttribute());
        IEntityManager entityManager = Mockito.mock(IEntityManager.class);
        Mockito.when(entityManager.getEntityAttributePrototypes()).thenReturn(attributeTypes);
        Mockito.when(beanFactory.getBean(entityManagerName)).thenReturn(entityManager);
        action.setAttributeTypeCode("attributeTypeCodeNotExistent");

        Mockito.when(textProvider.getText(any(), (String[]) any())).thenReturn("label");

        String result = action.addAttributeElement();

        Assertions.assertEquals(Action.INPUT, result);
        Assertions.assertEquals(1, action.getFieldErrors().size());
    }

    @Test
    void shouldMethodAddAttributeElementRaiseFailure() {

        String entityManagerName = "EntityManagerName";
        Mockito.when(session.getAttribute(IEntityTypeConfigAction.ENTITY_TYPE_MANAGER_SESSION_PARAM))
                .thenReturn(entityManagerName);

        IEntityManager entityManager = Mockito.mock(IEntityManager.class);
        Mockito.when(entityManager.getEntityAttributePrototypes()).thenThrow(new RuntimeException());
        Mockito.when(beanFactory.getBean(entityManagerName)).thenReturn(entityManager);
        action.setAttributeTypeCode("");

        String result = action.addAttributeElement();

        Assertions.assertEquals(BaseAction.FAILURE, result);
    }

    @Test
    void shouldMethodAddAttributeElement() {

        String entityManagerName = "EntityManagerName";
        Mockito.when(session.getAttribute(IEntityTypeConfigAction.ENTITY_TYPE_MANAGER_SESSION_PARAM))
                .thenReturn(entityManagerName);

        String attributeTypeCode = "typeCode";
        Map<String, AttributeInterface> attributeTypes = new HashMap<>();
        attributeTypes.put(attributeTypeCode, new TextAttribute());
        IEntityManager entityManager = Mockito.mock(IEntityManager.class);
        Mockito.when(entityManager.getEntityAttributePrototypes()).thenReturn(attributeTypes);
        Mockito.when(beanFactory.getBean(entityManagerName)).thenReturn(entityManager);
        action.setAttributeTypeCode(attributeTypeCode);

        String result = action.addAttributeElement();

        Assertions.assertEquals(Action.SUCCESS, result);
        Assertions.assertEquals(0, action.getFieldErrors().size());
    }


    @Test
    void shouldMethodSaveCompositeAttributeSaveComposite() {

        IApsEntity entity = Mockito.mock(IApsEntity.class);
        Mockito.when(session.getAttribute(ENTITY_TYPE_ON_EDIT_SESSION_PARAM)).thenReturn(entity);

        CompositeAttribute compositeAttribute = new CompositeAttribute();
        compositeAttribute.setName(COMPOSITE_ATTRIBUTE_NAME);
        Mockito.when(entity.getAttribute(COMPOSITE_ATTRIBUTE_NAME)).thenReturn(compositeAttribute);

        action.saveCompositeAttribute();

        Mockito.verify(session, Mockito.times(1))
                .setAttribute(Mockito.eq(ENTITY_TYPE_ON_EDIT_SESSION_PARAM), any());

        Mockito.verify(session, Mockito.times(1))
                .removeAttribute(COMPOSITE_ATTRIBUTE_ON_EDIT_SESSION_PARAM);
    }

    @Test
    void shouldMethodSaveCompositeAttributeSaveMonolist() {
        MonoListAttribute attribute = new MonoListAttribute();
        IApsEntity entity = Mockito.mock(IApsEntity.class);
        Mockito.when(entity.getAttribute(COMPOSITE_ATTRIBUTE_NAME)).thenReturn(attribute);
        Mockito.when(session.getAttribute(ENTITY_TYPE_ON_EDIT_SESSION_PARAM)).thenReturn(entity);
        action.saveCompositeAttribute();
        Mockito.verify(session, Mockito.times(1))
                .setAttribute(Mockito.eq(ENTITY_TYPE_ON_EDIT_SESSION_PARAM), any());
    }

}
