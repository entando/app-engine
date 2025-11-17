package com.agiletec.apsadmin.system.entity.type;

import static com.agiletec.apsadmin.system.entity.type.IEntityTypeConfigAction.ENTITY_TYPE_ON_EDIT_SESSION_PARAM;
import static com.agiletec.apsadmin.system.entity.type.IEntityTypeConfigAction.ENTITY_TYPE_OPERATION_ID_SESSION_PARAM;

import com.agiletec.aps.system.common.entity.IEntityManager;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.TextAttribute;
import org.apache.struts2.action.Action;
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

    @InjectMocks
    @Spy
    private EntityTypeConfigAction action;

    @BeforeEach
    void setUp() {
        Mockito.when(request.getSession()).thenReturn(session);
    }

    @Test
    void testAddAttribute() {
        String entityManagerName = "EntityManagerName";
        String attributeTypeCode = "typeCode";
        Mockito.when(session.getAttribute(IEntityTypeConfigAction.ENTITY_TYPE_MANAGER_SESSION_PARAM))
                .thenReturn(entityManagerName);
        Map<String, AttributeInterface> attributeTypes = new HashMap<>();
        attributeTypes.put(attributeTypeCode, new TextAttribute());
        IEntityManager entityManager = Mockito.mock(IEntityManager.class);
        Mockito.when(session.getAttribute(ENTITY_TYPE_ON_EDIT_SESSION_PARAM)).thenReturn(entityType);
        Mockito.when(beanFactory.getBean(entityManagerName)).thenReturn(entityManager);
        Mockito.when(entityManager.getEntityAttributePrototypes()).thenReturn(attributeTypes);
        Mockito.when(session.getAttribute(ENTITY_TYPE_OPERATION_ID_SESSION_PARAM)).thenReturn(1);
        action.setAttributeTypeCode(attributeTypeCode);
        String result = action.addAttribute();
        Assertions.assertEquals(Action.SUCCESS, result);
    }
}
