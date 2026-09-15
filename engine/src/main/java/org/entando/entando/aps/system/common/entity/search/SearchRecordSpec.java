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
package org.entando.entando.aps.system.common.entity.search;

import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;

/**
 * One attribute to index, and the {@code attrname} to index it under. Produced by
 * {@link NestedSearchSupport#planSearchRecords}, consumed by {@code AbstractEntityDAO}.
 *
 * @param attrName the {@code attrname} column value.
 * @param attribute the attribute supplying the values; not a copy.
 */
public record SearchRecordSpec(String attrName, AttributeInterface attribute) {

}
