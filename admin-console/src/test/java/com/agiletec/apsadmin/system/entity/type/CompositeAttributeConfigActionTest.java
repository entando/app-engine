package com.agiletec.apsadmin.system.entity.type;

import static com.agiletec.apsadmin.system.entity.type.ICompositeAttributeConfigAction.COMPOSITE_ATTRIBUTE_ON_EDIT_SESSION_PARAM;
import static com.agiletec.apsadmin.system.entity.type.IEntityTypeConfigAction.ENTITY_TYPE_ON_EDIT_SESSION_PARAM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.common.entity.IEntityManager;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CheckBoxAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoListAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.TextAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.ThreeStateAttribute;
import com.agiletec.apsadmin.system.ApsAdminSystemConstants;
import com.agiletec.apsadmin.system.BaseAction;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import org.apache.struts2.action.Action;
import org.apache.struts2.text.TextProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

    private CompositeAttribute compositeAttribute;

    @BeforeEach
    void setUp() {
        when(request.getSession()).thenReturn(session);

        this.compositeAttribute = new CompositeAttribute();
        compositeAttribute.setName(COMPOSITE_ATTRIBUTE_NAME);
        addTextAttribute(compositeAttribute, "attribute1");
        addTextAttribute(compositeAttribute, "attribute2");
        lenient().when(session.getAttribute(COMPOSITE_ATTRIBUTE_ON_EDIT_SESSION_PARAM)).thenReturn(compositeAttribute);
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
        verify(session, times(1))
                .setAttribute(eq(COMPOSITE_ATTRIBUTE_ON_EDIT_SESSION_PARAM), any());
    }

    @Test
    void testRemoveAttributeElement() {
        action.setAttributeIndex(0);
        action.removeAttributeElement();
        verify(session, times(1))
                .setAttribute(eq(COMPOSITE_ATTRIBUTE_ON_EDIT_SESSION_PARAM), any());
    }


    @Test
    void testSaveAttributeElement() {
        String entityManagerName = "EntityManagerName";
        String attributeTypeCode = "typeCode";
        when(session.getAttribute(IEntityTypeConfigAction.ENTITY_TYPE_MANAGER_SESSION_PARAM))
                .thenReturn(entityManagerName);
        Map<String, AttributeInterface> attributeTypes = new HashMap<>();
        attributeTypes.put(attributeTypeCode, new TextAttribute());
        IEntityManager entityManager = mock(IEntityManager.class);
        when(beanFactory.getBean(entityManagerName)).thenReturn(entityManager);
        when(entityManager.getEntityAttributePrototypes()).thenReturn(attributeTypes);
        action.setAttributeTypeCode(attributeTypeCode);
        action.saveAttributeElement();
        verify(session, times(1))
                .setAttribute(eq(COMPOSITE_ATTRIBUTE_ON_EDIT_SESSION_PARAM), any());
    }

    @Test
    void searchableFlagIsClearedWhenTheCompositeIsNestedInAList() {
        MonoListAttribute list = new MonoListAttribute();
        list.setName("rows");
        when(session.getAttribute(IListElementAttributeConfigAction.LIST_ATTRIBUTE_ON_EDIT_SESSION_PARAM))
                .thenReturn(list);

        AttributeInterface saved = saveBooleanAttributeElement("featured", true);

        // a boolean reached through a list is indexed by no engine, so the flag must not survive
        Assertions.assertFalse(saved.isSearchable());
    }

    @Test
    void searchableFlagIsKeptWhenTheCompositeIsNotNestedInAList() {
        AttributeInterface saved = saveBooleanAttributeElement("featured", true);

        Assertions.assertTrue(saved.isSearchable());
    }

    /**
     * Drive {@code saveAttributeElement} for a Boolean child and return the attribute it added to the
     * Composite being edited.
     */
    private AttributeInterface saveBooleanAttributeElement(String attributeName, boolean searchable) {
        String entityManagerName = "EntityManagerName";
        String attributeTypeCode = "Boolean";
        when(session.getAttribute(IEntityTypeConfigAction.ENTITY_TYPE_MANAGER_SESSION_PARAM))
                .thenReturn(entityManagerName);
        Map<String, AttributeInterface> attributeTypes = new HashMap<>();
        attributeTypes.put(attributeTypeCode, new BooleanAttribute());
        IEntityManager entityManager = mock(IEntityManager.class);
        when(beanFactory.getBean(entityManagerName)).thenReturn(entityManager);
        when(entityManager.getEntityAttributePrototypes()).thenReturn(attributeTypes);
        action.setAttributeTypeCode(attributeTypeCode);
        action.setAttributeName(attributeName);
        action.setSearchable(searchable);

        action.saveAttributeElement();

        return compositeAttribute.getAttribute(attributeName);
    }

    @Test
    void testNestedSearchableOptionSupportedForBooleanLikes() {
        String entityManagerName = "EntityManagerName";
        when(session.getAttribute(IEntityTypeConfigAction.ENTITY_TYPE_MANAGER_SESSION_PARAM))
                .thenReturn(entityManagerName);
        Map<String, AttributeInterface> attributeTypes = new HashMap<>();
        attributeTypes.put("Boolean", new BooleanAttribute());
        attributeTypes.put("CheckBox", new CheckBoxAttribute());
        attributeTypes.put("ThreeState", new ThreeStateAttribute());
        attributeTypes.put("Text", new TextAttribute());
        IEntityManager entityManager = mock(IEntityManager.class);
        when(beanFactory.getBean(entityManagerName)).thenReturn(entityManager);
        when(entityManager.getEntityAttributePrototypes()).thenReturn(attributeTypes);

        // every boolean-like composite child may be flagged searchable; other types may not
        Assertions.assertTrue(action.isNestedSearchableOptionSupported("Boolean"));
        Assertions.assertTrue(action.isNestedSearchableOptionSupported("CheckBox"));
        Assertions.assertTrue(action.isNestedSearchableOptionSupported("ThreeState"));
        Assertions.assertFalse(action.isNestedSearchableOptionSupported("Text"));
    }

    @Test
    void testNestedSearchableOptionSupportedHandlesException() {
        String entityManagerName = "EntityManagerName";
        when(session.getAttribute(IEntityTypeConfigAction.ENTITY_TYPE_MANAGER_SESSION_PARAM))
                .thenReturn(entityManagerName);
        IEntityManager entityManager = mock(IEntityManager.class);
        when(entityManager.getEntityAttributePrototypes()).thenThrow(new RuntimeException());
        when(beanFactory.getBean(entityManagerName)).thenReturn(entityManager);

        Assertions.assertFalse(action.isNestedSearchableOptionSupported("Boolean"));
    }

    @Test
    void shouldMethodNotAddAttributeElement() {

        String entityManagerName = "EntityManagerName";
        when(session.getAttribute(IEntityTypeConfigAction.ENTITY_TYPE_MANAGER_SESSION_PARAM))
                .thenReturn(entityManagerName);

        String attributeTypeCode = "typeCode";
        Map<String, AttributeInterface> attributeTypes = new HashMap<>();
        attributeTypes.put(attributeTypeCode, new TextAttribute());
        IEntityManager entityManager = mock(IEntityManager.class);
        when(entityManager.getEntityAttributePrototypes()).thenReturn(attributeTypes);
        when(beanFactory.getBean(entityManagerName)).thenReturn(entityManager);
        action.setAttributeTypeCode("attributeTypeCodeNotExistent");

        when(textProvider.getText(any(), (String[]) any())).thenReturn("label");

        String result = action.addAttributeElement();

        Assertions.assertEquals(Action.INPUT, result);
        Assertions.assertEquals(1, action.getFieldErrors().size());
    }

    @Test
    void shouldMethodAddAttributeElementRaiseFailure() {

        String entityManagerName = "EntityManagerName";
        when(session.getAttribute(IEntityTypeConfigAction.ENTITY_TYPE_MANAGER_SESSION_PARAM))
                .thenReturn(entityManagerName);

        IEntityManager entityManager = mock(IEntityManager.class);
        when(entityManager.getEntityAttributePrototypes()).thenThrow(new RuntimeException());
        when(beanFactory.getBean(entityManagerName)).thenReturn(entityManager);
        action.setAttributeTypeCode("");

        String result = action.addAttributeElement();

        Assertions.assertEquals(BaseAction.FAILURE, result);
    }

    @Test
    void shouldMethodAddAttributeElement() {

        String entityManagerName = "EntityManagerName";
        when(session.getAttribute(IEntityTypeConfigAction.ENTITY_TYPE_MANAGER_SESSION_PARAM))
                .thenReturn(entityManagerName);

        String attributeTypeCode = "typeCode";
        Map<String, AttributeInterface> attributeTypes = new HashMap<>();
        attributeTypes.put(attributeTypeCode, new TextAttribute());
        IEntityManager entityManager = mock(IEntityManager.class);
        when(entityManager.getEntityAttributePrototypes()).thenReturn(attributeTypes);
        when(beanFactory.getBean(entityManagerName)).thenReturn(entityManager);
        action.setAttributeTypeCode(attributeTypeCode);

        String result = action.addAttributeElement();

        Assertions.assertEquals(Action.SUCCESS, result);
        Assertions.assertEquals(0, action.getFieldErrors().size());
    }


    @Test
    void shouldMethodSaveCompositeAttributeSaveComposite() {

        IApsEntity entity = mock(IApsEntity.class);
        when(session.getAttribute(ENTITY_TYPE_ON_EDIT_SESSION_PARAM)).thenReturn(entity);

        CompositeAttribute savedComposite = new CompositeAttribute();
        savedComposite.setName(COMPOSITE_ATTRIBUTE_NAME);
        when(entity.getAttribute(COMPOSITE_ATTRIBUTE_NAME)).thenReturn(savedComposite);

        action.saveCompositeAttribute();

        verify(session, times(1))
                .setAttribute(eq(ENTITY_TYPE_ON_EDIT_SESSION_PARAM), any());

        verify(session, times(1))
                .removeAttribute(COMPOSITE_ATTRIBUTE_ON_EDIT_SESSION_PARAM);
    }

    @Test
    void shouldMethodSaveCompositeAttributeSaveMonolist() {
        MonoListAttribute attribute = new MonoListAttribute();
        IApsEntity entity = mock(IApsEntity.class);
        when(entity.getAttribute(COMPOSITE_ATTRIBUTE_NAME)).thenReturn(attribute);
        when(session.getAttribute(ENTITY_TYPE_ON_EDIT_SESSION_PARAM)).thenReturn(entity);
        action.saveCompositeAttribute();
        verify(session, times(1))
                .setAttribute(eq(ENTITY_TYPE_ON_EDIT_SESSION_PARAM), any());
    }

}
