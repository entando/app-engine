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
package com.agiletec.apsadmin.system.entity.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.system.common.entity.ApsEntityManager;
import com.agiletec.aps.system.common.entity.IEntityManager;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AbstractListAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.ITextAttribute;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import com.agiletec.apsadmin.system.ApsAdminSystemConstants;
import org.apache.struts2.action.Action;
import org.apache.struts2.ActionSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ListElementAttributeConfigActionIntegrationTest extends ApsAdminBaseTestCase {


    @Test
    void testSaveAttributeElement() throws Throwable {
        //Search an entity manager
        String[] defNames = this.getApplicationContext().getBeanNamesForType(ApsEntityManager.class);
        if (null == defNames || defNames.length == 0) {
            return;
        }
        String entityManagerName = defNames[0];
        IEntityManager entityManager = (IEntityManager) this.getApplicationContext().getBean(entityManagerName);
        //get the entites managed by the ApsEntityManager
        Map<String, IApsEntity> entities = entityManager.getEntityPrototypes();
        if (null == entities || entities.size() == 0) {
            return;
        }
        List<String> enitiesTypeCodes = new ArrayList<>(entities.keySet());
        //get the first entity type code available
        String entityTypeCode = enitiesTypeCodes.get(0);
        this.setUserOnSession("admin");
        IApsEntity entityType = entityManager.getEntityPrototype(entityTypeCode);
        this.getRequest().getSession().setAttribute(IEntityTypeConfigAction.ENTITY_TYPE_ON_EDIT_SESSION_PARAM, entityType);
        this.getRequest().getSession().setAttribute(IEntityTypeConfigAction.ENTITY_TYPE_MANAGER_SESSION_PARAM, entityManagerName);

        String attributeName = "monolist";
        String attributeTypeCode = "Monolist";
        int strutsAction = ApsAdminSystemConstants.ADD;
        String result = this.executeSaveAttribute(attributeName, attributeTypeCode, strutsAction, null);
        assertEquals(Action.INPUT, result);
        ActionSupport action = (ActionSupport) this.getAction();
        Map<String, List<String>> fieldErrors = action.getFieldErrors();
        assertEquals(1, fieldErrors.size());
        assertTrue(fieldErrors.containsKey("listNestedType"));

        Assertions.assertNull(super.getRequest().getSession().getAttribute(IListElementAttributeConfigAction.LIST_ELEMENT_ON_EDIT_SESSION_PARAM));

        Map<String, String> properties = new HashMap<>();
        properties.put("listNestedType", "Text");

        result = this.executeSaveAttribute(attributeName, attributeTypeCode, strutsAction, properties);
        assertEquals("configureListElementAttribute", result);
        Assertions.assertNotNull(super.getRequest().getSession().getAttribute(IListElementAttributeConfigAction.LIST_ELEMENT_ON_EDIT_SESSION_PARAM));

        this.initAction("/do/Entity/ListAttribute", "saveListElement");
        this.addParameter("attributeName", attributeName);
        this.addParameter("minLength", "100");
        this.addParameter("maxLength", "20");
        result = this.executeAction();
        assertEquals(Action.INPUT, result);
        action = (ActionSupport) this.getAction();
        fieldErrors = action.getFieldErrors();
        assertEquals(1, fieldErrors.size());
        assertTrue(fieldErrors.containsKey("maxLength"));

        IApsEntity entityTypeOnSession = (IApsEntity) this.getRequest().getSession().getAttribute(IEntityTypeConfigAction.ENTITY_TYPE_ON_EDIT_SESSION_PARAM);
        AbstractListAttribute listAttribute = (AbstractListAttribute) entityTypeOnSession.getAttribute(attributeName);
        ITextAttribute textAttribute = (ITextAttribute) listAttribute.getNestedAttributeType();
        Assertions.assertTrue(textAttribute.getMinLength() < 0);
        Assertions.assertTrue(textAttribute.getMaxLength() < 0);

        this.initAction("/do/Entity/ListAttribute", "saveListElement");
        this.addParameter("attributeName", attributeName);
        this.addParameter("minLength", "50");
        this.addParameter("maxLength", "200");

        result = this.executeAction();
        assertEquals(Action.SUCCESS, result);

        entityTypeOnSession = (IApsEntity) this.getRequest().getSession().getAttribute(IEntityTypeConfigAction.ENTITY_TYPE_ON_EDIT_SESSION_PARAM);
        listAttribute = (AbstractListAttribute) entityTypeOnSession.getAttribute(attributeName);
        textAttribute = (ITextAttribute) listAttribute.getNestedAttributeType();
        Assertions.assertTrue(textAttribute.getMinLength() > 0);
        Assertions.assertTrue(textAttribute.getMaxLength() > 0);
    }

    protected String executeSaveAttribute(String attributeName, String attributeTypeCode, int strutsAction, Map<String, String> extraProperties) throws Throwable {
        this.initAction("/do/Entity/Attribute", "saveAttribute");
        this.addParameter("attributeName", attributeName);
        this.addParameter("strutsAction", strutsAction);
        this.addParameter("attributeTypeCode", attributeTypeCode);
        if (null != extraProperties) {
            extraProperties.entrySet().stream().forEach(e -> this.addParameter(e.getKey(), e.getValue()));
        }
        return this.executeAction();
    }

}
