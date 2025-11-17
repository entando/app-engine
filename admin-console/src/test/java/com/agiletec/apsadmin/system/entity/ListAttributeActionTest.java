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
package com.agiletec.apsadmin.system.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.agiletec.aps.system.common.entity.ApsEntityManager;
import com.agiletec.aps.system.common.entity.IEntityManager;
import com.agiletec.aps.system.common.entity.IEntityTypesConfigurer;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.ITextAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoListAttribute;
import com.agiletec.aps.system.common.searchengine.IndexableAttributeInterface;
import com.agiletec.apsadmin.ApsAdminBaseTestCase;
import com.agiletec.apsadmin.system.ApsAdminSystemConstants;
import com.agiletec.apsadmin.system.entity.attribute.action.list.IListAttributeAction;
import com.agiletec.apsadmin.system.entity.attribute.action.list.ListAttributeAction;
import com.agiletec.apsadmin.system.entity.type.EntityAttributeConfigAction;
import com.agiletec.apsadmin.system.entity.type.IEntityTypeConfigAction;
import com.agiletec.apsadmin.system.entity.type.IEntityTypesAction;
import org.apache.struts2.action.Action;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListAttributeActionTest extends ApsAdminBaseTestCase {

    @Mock
    private IApsEntity entity;

    @Test
    void abtrsactListAttributeActionShouldManageException()  {
        Mockito.when(entity.getAttribute(ArgumentMatchers.any())).thenThrow(new RuntimeException());

        IListAttributeAction listAtributeAction = new ListAttributeAction(){
            protected IApsEntity getCurrentApsEntity() {
                return entity;
            };
        };

        Assertions.assertEquals(ListAttributeAction.FAILURE,listAtributeAction.addListElement());
        Assertions.assertEquals(ListAttributeAction.FAILURE,listAtributeAction.moveListElement());
        Assertions.assertEquals(ListAttributeAction.FAILURE,listAtributeAction.removeListElement());
    }

}