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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanComparator;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import com.agiletec.aps.system.common.entity.IEntityManager;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AbstractListAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.CompositeAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.MonoListAttribute;

import com.agiletec.apsadmin.system.ApsAdminSystemConstants;

/**
 * @author E.Santoboni
 */
public class CompositeAttributeConfigAction extends AbstractBaseEntityAttributeConfigAction implements ICompositeAttributeConfigAction, BeanFactoryAware {

	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(CompositeAttributeConfigAction.class);
	
	@Override
	public void validate() {
		super.validate();
		CompositeAttribute composite = this.getCompositeAttributeOnEdit();
		if (null != composite.getAttribute(this.getAttributeName())) {
			String[] args = {this.getAttributeName()};
			this.addFieldError("attributeName", this.getText("error.entity.attribute.name.already.exists", args));
		}
	}
	
	@Override
	public String addAttributeElement() {
		this.setStrutsAction(ApsAdminSystemConstants.ADD);
		try {
			AttributeInterface attribute = super.getAttributePrototype(this.getAttributeTypeCode());
			if (attribute == null) {
				String[] args = {this.getAttributeTypeCode()};
				this.addFieldError("attributeTypeCode", this.getText("error.attribute.not.exists",args));
				return INPUT;
			}
		} catch (Exception ex) {
			_logger.error("error in addAttributeElement", ex);
			return FAILURE;
		}
		return SUCCESS;
	}
	
	@Override
	public String moveAttributeElement() {
		try {
			CompositeAttribute composite = this.getCompositeAttributeOnEdit();
			int elementIndex = this.getAttributeIndex();
			String movement = this.getMovement();
			List<AttributeInterface> attributes = composite.getAttributes();
			if (!(elementIndex==0 && movement.equals(ApsAdminSystemConstants.MOVEMENT_UP_CODE)) && 
					!(elementIndex==attributes.size()-1 && movement.equals(ApsAdminSystemConstants.MOVEMENT_DOWN_CODE))) {
				AttributeInterface attributeToMove = attributes.get(elementIndex);
				attributes.remove(elementIndex);
				if (movement.equals(ApsAdminSystemConstants.MOVEMENT_UP_CODE)) {
					attributes.add(elementIndex-1, attributeToMove);
				}
				if (movement.equals(ApsAdminSystemConstants.MOVEMENT_DOWN_CODE)) {
					attributes.add(elementIndex+1, attributeToMove);
				}
			}
			this.updateCompositeAttributeOnEdit(composite);
		} catch (Throwable t) {
			_logger.error("error in moveAttribute", t);
			return FAILURE;
		}
		return SUCCESS;
	}
	
	@Override
	public String removeAttributeElement() {
		try {
			int elementIndex = this.getAttributeIndex();
			CompositeAttribute composite = this.getCompositeAttributeOnEdit();
			AttributeInterface attributeToRemove = composite.getAttributes().get(elementIndex);
			composite.getAttributes().remove(elementIndex);
			composite.getAttributeMap().remove(attributeToRemove.getName());
			this.updateCompositeAttributeOnEdit(composite);
		} catch (Throwable t) {
			_logger.error("error in removeAttribute", t);
			return FAILURE;
		}
		return SUCCESS;
	}
	
	@Override
	public String saveAttributeElement() {
		try {
			CompositeAttribute composite = this.getCompositeAttributeOnEdit();
			if (this.getStrutsAction() == ApsAdminSystemConstants.EDIT) {
				throw new RuntimeException("The edit operation on this attribute is not supported");
			} else {
				AttributeInterface attribute = this.getAttributePrototype(this.getAttributeTypeCode());
				attribute.setName(this.getAttributeName());
				super.fillAttributeFields(attribute);
				this.clearSearchableWithinList(attribute);
				composite.getAttributes().add(attribute);
				composite.getAttributeMap().put(attribute.getName(), attribute);
			}
			this.updateCompositeAttributeOnEdit(composite);
		} catch (Throwable t) {
			_logger.error("error in saveAttributeElement", t);
			return FAILURE;
		}
		return SUCCESS;
	}
	
	@Override
	public String saveCompositeAttribute() {
		try {
			CompositeAttribute composite = this.getCompositeAttributeOnEdit();
			IApsEntity entityType = this.getEntityType();
			AttributeInterface attribute = (AttributeInterface) entityType.getAttribute(composite.getName());
			if(attribute instanceof CompositeAttribute) {
				entityType.addOrReplaceAttribute(composite);
			} else if (attribute instanceof MonoListAttribute) {
				((MonoListAttribute) attribute).setNestedAttributeType(composite);
			} else if (!attribute.getName().equals(composite.getName())) {
				throw new EntException("Attribute Name '" + attribute.getName() + "' incompatible with composite Attribute name '" + composite.getName() + "'");
			} else if (!attribute.getType().equals(composite.getType())) {
				throw new EntException("Attribute Type '" + attribute.getType() + "' incompatible with composite Attribute type '" + composite.getType() + "'");
			}
			this.updateEntityType(entityType);
			this.getRequest().getSession().removeAttribute(COMPOSITE_ATTRIBUTE_ON_EDIT_SESSION_PARAM);
		} catch (Throwable t) {
			_logger.error("error in saveAttribute", t);
			return FAILURE;
		}
		return SUCCESS;
	}
	
	/**
	 * Force the {@code searchable} flag off when the Composite being edited is the nested type of a
	 * List/Monolist. A boolean reached through a list is never indexed as a per-attribute filter by
	 * either search engine, so the flag would be inert; the form does not offer it in that case, but
	 * this also covers a stale or forged submission.
	 * @param attribute the composite child being saved.
	 */
	private void clearSearchableWithinList(AttributeInterface attribute) {
		if (null != this.getListAttribute() && attribute.isSearchable()) {
			_logger.debug("Ignoring the searchable flag on '{}': the composite is nested in the list '{}'",
					attribute.getName(), this.getListAttribute().getName());
			attribute.setSearchable(false);
		}
	}

	public List<AttributeInterface> getAllowedAttributeElementTypes() {
		List<AttributeInterface> attributes = new ArrayList<AttributeInterface>();
		try {
			IEntityManager entityManager = this.getEntityManager();
			Map<String, AttributeInterface> attributeTypes = entityManager.getEntityAttributePrototypes();
			Iterator<AttributeInterface> attributeIter = attributeTypes.values().iterator();
			while (attributeIter.hasNext()) {
				AttributeInterface attribute = attributeIter.next();
				if (attribute.isSimple()) {
					attributes.add(attribute);
				}
			}
			Collections.sort(attributes, new BeanComparator("type"));
		} catch (Throwable t) {
			_logger.error("Error extracting the allowed types of attribute elements", t);
			throw new RuntimeException("Error extracting the allowed types of attribute elements", t);
		}
		return attributes;
	}
	
	public CompositeAttribute getCompositeAttributeOnEdit() {
		return (CompositeAttribute) this.getRequest().getSession().getAttribute(COMPOSITE_ATTRIBUTE_ON_EDIT_SESSION_PARAM);
	}

	public void updateCompositeAttributeOnEdit(CompositeAttribute compositeAttribute) {
		this.getRequest().getSession().setAttribute(COMPOSITE_ATTRIBUTE_ON_EDIT_SESSION_PARAM, compositeAttribute);
	}
	
	public AbstractListAttribute getListAttribute() {
		return (AbstractListAttribute) this.getRequest().getSession().getAttribute(IListElementAttributeConfigAction.LIST_ATTRIBUTE_ON_EDIT_SESSION_PARAM);
	}
	
	public int getAttributeIndex() {
		return _attributeIndex;
	}
	public void setAttributeIndex(int attributeIndex) {
		this._attributeIndex = attributeIndex;
	}
	
	public String getMovement() {
		return _movement;
	}
	public void setMovement(String movement) {
		this._movement = movement;
	}
	
	private int _attributeIndex;
	private String _movement;
	
}