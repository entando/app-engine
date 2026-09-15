package com.agiletec.apsadmin.system.entity.type;

import static com.agiletec.apsadmin.system.entity.type.IEntityTypeConfigAction.ENTITY_TYPE_ON_EDIT_SESSION_PARAM;
import static com.agiletec.apsadmin.system.entity.type.IEntityTypeConfigAction.ENTITY_TYPE_OPERATION_ID_SESSION_PARAM;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.common.entity.IEntityManager;
import com.agiletec.aps.system.common.entity.model.ApsEntity;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.BooleanAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.TextAttribute;
import com.agiletec.apsadmin.system.ApsAdminSystemConstants;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanFactory;

@ExtendWith(MockitoExtension.class)
class EntityTypeConfigActionTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession session;
    @Mock
    private IApsEntity entityType;
    @Mock
    private BeanFactory beanFactory;
    @Mock
    private TextProvider textProvider;

    @InjectMocks
    @Spy
    private EntityTypeConfigAction action;

    @BeforeEach
    void setUp() {
        when(request.getSession()).thenReturn(session);
    }

    @Test
    void testAddAttribute() {
        String entityManagerName = "EntityManagerName";
        String attributeTypeCode = "typeCode";
        when(session.getAttribute(IEntityTypeConfigAction.ENTITY_TYPE_MANAGER_SESSION_PARAM))
                .thenReturn(entityManagerName);
        Map<String, AttributeInterface> attributeTypes = new HashMap<>();
        attributeTypes.put(attributeTypeCode, new TextAttribute());
        IEntityManager entityManager = mock(IEntityManager.class);
        when(session.getAttribute(ENTITY_TYPE_ON_EDIT_SESSION_PARAM)).thenReturn(entityType);
        when(beanFactory.getBean(entityManagerName)).thenReturn(entityManager);
        when(entityManager.getEntityAttributePrototypes()).thenReturn(attributeTypes);
        when(session.getAttribute(ENTITY_TYPE_OPERATION_ID_SESSION_PARAM)).thenReturn(1);
        action.setAttributeTypeCode(attributeTypeCode);
        String result = action.addAttribute();
        Assertions.assertEquals(Action.SUCCESS, result);
    }

    @Test
    void validateShouldRejectDuplicatedNestedBooleanSearchKey() {
        // top-level 'compo_flag' and composite 'compo' child 'flag' write the same DB attrname
        ApsEntity type = entityTypeOnEdit();
        type.addAttribute(booleanAttribute("compo_flag"));
        type.addAttribute(compositeWith("compo", booleanAttribute("flag")));

        action.validate();

        Assertions.assertTrue(action.hasFieldErrors());
        Assertions.assertEquals(1, action.getFieldErrors().get("entityTypeCode").size());
        ArgumentCaptor<String[]> args = ArgumentCaptor.forClass(String[].class);
        verify(textProvider)
                .getText(eq("error.entity.nestedBoolean.key.duplicated"), args.capture());
        Assertions.assertEquals("compo_flag", args.getValue()[0]);
        Assertions.assertEquals("compo_flag, compo > flag", args.getValue()[1]);
    }

    @Test
    void validateShouldRejectNestedBooleanSearchKeyLongerThanTheColumn() {
        ApsEntity type = entityTypeOnEdit();
        type.addAttribute(compositeWith("c".repeat(260), booleanAttribute("flag")));

        action.validate();

        Assertions.assertTrue(action.hasFieldErrors());
        ArgumentCaptor<String[]> args = ArgumentCaptor.forClass(String[].class);
        verify(textProvider)
                .getText(eq("error.entity.nestedBoolean.key.tooLong"), args.capture());
        Assertions.assertEquals("265", args.getValue()[1]);
        Assertions.assertEquals("255", args.getValue()[2]);
    }

    @Test
    void validateShouldAcceptSoundNestedBooleanSearchKeys() {
        ApsEntity type = entityTypeOnEdit();
        type.addAttribute(booleanAttribute("flag"));
        type.addAttribute(compositeWith("compo", booleanAttribute("certified")));

        action.validate();

        Assertions.assertFalse(action.hasFieldErrors());
    }

    private ApsEntity entityTypeOnEdit() {
        ApsEntity type = new ApsEntity();
        type.setTypeCode("TST");
        when(session.getAttribute(ENTITY_TYPE_ON_EDIT_SESSION_PARAM)).thenReturn(type);
        when(session.getAttribute(ENTITY_TYPE_OPERATION_ID_SESSION_PARAM))
                .thenReturn(ApsAdminSystemConstants.EDIT);
        return type;
    }

    private BooleanAttribute booleanAttribute(String name) {
        BooleanAttribute attribute = new BooleanAttribute();
        attribute.setName(name);
        attribute.setSearchable(true);
        return attribute;
    }

    private CompositeAttribute compositeWith(String name, AttributeInterface child) {
        CompositeAttribute composite = new CompositeAttribute();
        composite.setName(name);
        composite.getAttributes().add(child);
        composite.getAttributeMap().put(child.getName(), child);
        return composite;
    }
}
