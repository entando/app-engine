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
package com.agiletec.aps.system.common.entity;

import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;

/**
 * One search-table record group to write for one attribute: the {@code attrname} it is stored under and
 * the attribute that supplies the values ({@code getSearchInfos}, one row per language).
 *
 * <p>Produced by {@link NestedBooleanSearchSupport#planSearchRecords}, which owns every rule that
 * decides <i>whether</i> an attribute is indexed and <i>under which name</i>: its own name for a
 * top-level or list-reached attribute (unchanged legacy behaviour), its path key when it is a
 * boolean-like attribute nested in a Composite. {@code AbstractEntityDAO} consumes this list and does
 * nothing else - it no longer traverses the attribute tree, and no longer knows the naming rule.</p>
 *
 * <p>This is a top-level type rather than a member of {@link NestedBooleanSearchSupport} for the same
 * reason as {@link SearchableAttributeRef}: it appears in the signature a subclass of
 * {@code AbstractEntityDAO} may override, so nesting it would bake the support class's name into a
 * protected API.</p>
 *
 * @param attrName the value to write in the search table's {@code attrname} column.
 * @param attribute the attribute supplying the search values; never a copy.
 * @author Entando
 */
public record SearchRecordSpec(String attrName, AttributeInterface attribute) {

}
