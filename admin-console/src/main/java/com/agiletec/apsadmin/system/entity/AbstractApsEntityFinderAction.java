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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

import org.entando.entando.aps.system.common.entity.search.EntitySearchKeys;
import org.entando.entando.aps.system.common.entity.search.EntitySearchSchema;
import com.agiletec.aps.system.common.entity.IEntityManager;
import org.entando.entando.aps.system.common.entity.search.SearchableAttributeRef;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeRole;
import com.agiletec.apsadmin.system.BaseAction;

/**
 * @author E.Santoboni
 */
public abstract class AbstractApsEntityFinderAction extends BaseAction implements IApsEntityFinderAction {

	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(AbstractApsEntityFinderAction.class);
	
	@Override
	public String execute() {
		try {
			this.createBaseFilters();
		} catch (Throwable t) {
			_logger.error("error in execute", t);
			return FAILURE;
		}
		return SUCCESS;
	}
	
	protected void createBaseFilters() {
		try {
			int initSize = this.getFilters().length;
			EntitySearchFilter[] roleFilters = this.getEntityActionHelper().getRoleFilters(this);
			this.addFilters(roleFilters);
			IApsEntity prototype = this.getEntityPrototype();
			if (null != prototype) {
				EntitySearchFilter filterToAdd = new EntitySearchFilter(IEntityManager.ENTITY_TYPE_CODE_FILTER_KEY, false, prototype.getTypeCode(), false);
				this.addFilter(filterToAdd);
				EntitySearchFilter[] filters = this.getEntityActionHelper().getAttributeFilters(this, prototype);
				this.addFilters(filters);
			}
			this.setAddedAttributeFilter(this.getFilters().length > initSize);
		} catch (Throwable t) {
			_logger.error("Error while creating entity filters", t);
			//ApsSystemUtils.logThrowable(t, this, "createBaseFilters");
			throw new RuntimeException("Error while creating entity filters", t);
		}
	}
	
	@Override
	public List<String> getSearchResult() {
		List<String> result = null;
		try {
			IEntityManager entityManager = this.getEntityManager();
			result = entityManager.searchId(this.getFilters());
		} catch (Throwable t) {
			_logger.error("Error while searching entity Ids", t);
			throw new RuntimeException("Error while searching entity Ids", t);
		}
		return result;
	}
	
	protected void addFilters(EntitySearchFilter[] filters) {
		for (int i = 0; i < filters.length; i++) {
			EntitySearchFilter filterToAdd = filters[i];
			this.addFilter(filterToAdd);
		}
	}
	
	protected void addFilter(EntitySearchFilter filterToAdd) {
		EntitySearchFilter[] filters = this.getFilters();
		int len = filters.length;
		EntitySearchFilter[] newFilters = new EntitySearchFilter[len + 1];
		for(int i=0; i < len; i++){
			newFilters[i] = filters[i];
		}
		newFilters[len] = filterToAdd;
		this.setFilters(newFilters);
	}
	
	@Override
	public String trash() {
		try {
			String checkResult = this.checkDeletingEntity();
			if (null != checkResult) return checkResult;
		} catch (Throwable t) {
			_logger.error("Error while trashing entity", t);
			throw new RuntimeException("Error while trashing entity", t);
		}
		return SUCCESS;
	}
	
	@Override
	public String delete() {
		try {
			String checkResult = this.checkDeletingEntity();
			if (null != checkResult) return checkResult;
			this.deleteEntity(this.getEntityId());
		} catch (Throwable t) {
			_logger.error("Error while deleting entity", t);
			throw new RuntimeException("Error while deleting entity", t);
		}
		return SUCCESS;
	}
	
	protected abstract void deleteEntity(String entityId) throws Throwable;
	
	protected String checkDeletingEntity() throws Throwable {
		IApsEntity entity = this.getEntity(this.getEntityId());
		if (null == entity) {
			String[] args = {this.getEntityId()};
			this.addFieldError("entityId", this.getText("error.entity.null",args));
			return INPUT;
		}
		return null;
	}
	
	public IApsEntity getEntityPrototype() {
		IEntityManager entityManager = this.getEntityManager();
		return entityManager.getEntityPrototype(this.getEntityTypeCode());
	}
	
	public List<IApsEntity> getEntityPrototypes() {
		List<IApsEntity> entityPrototypes = null;
		try {
			Map<String, IApsEntity> modelMap = this.getEntityManager().getEntityPrototypes();
			entityPrototypes = new ArrayList<IApsEntity>(modelMap.values());
			BeanComparator comparator = new BeanComparator("typeDescr");
			Collections.sort(entityPrototypes, comparator);
		} catch (Throwable t) {
			_logger.error("Error while extracting entity prototypes", t);
			throw new RuntimeException("Error while extracting entity prototypes", t);
		}
		return entityPrototypes;
	}
	
	protected IApsEntity getEntity(String entityId) {
		IApsEntity entity = null;
		try {
			IEntityManager entityManager = this.getEntityManager();
			entity = entityManager.getEntity(entityId);
		} catch (Throwable t) {
			_logger.error("Error while extracting entity", t);
			throw new RuntimeException("Error while extracting entity", t);
		}
		return entity;
	}
	
	public String getSearchFormFieldValue(String inputFieldName) {
		String val = this.getRequest().getParameter(inputFieldName);
		if (StringUtils.isBlank(val)) {
			val = (String) this.getRequest().getAttribute(inputFieldName);
		}
		return val;
	}
	
	/**
	 * @return the same list as {@link #getSearchableAttributes()}.
	 * @deprecated the name is misspelled; use {@link #getSearchableAttributes()}. Not removable yet:
	 * {@code webdynamicform-plugin}'s {@code messageFinding.jsp} still binds to
	 * {@code searcheableAttributes}, and no JSP is compiled by this build, so deleting this would break
	 * that page silently. Retire it together with that binding.
	 */
	@Deprecated
	public List<AttributeInterface> getSearcheableAttributes() {
		return this.getSearchableAttributes();
	}

	/**
	 * The searchable <b>top-level</b> attributes, as real {@link AttributeInterface} instances.
	 *
	 * <p>This deliberately keeps its historical return type. It is a {@code public} method on a
	 * {@code public abstract} class that downstream projects extend, and its elements are addressed by
	 * custom JSPs through arbitrary attribute properties ({@code #attribute.items},
	 * {@code #attribute.roles}, ...). Narrowing it to a projection would compile cleanly here and then
	 * fail at runtime in customer code - silently in JSPs, which this build never compiles.</p>
	 *
	 * <p>Nested searchable attributes are <b>not</b> included here, because they cannot be represented
	 * as a plain attribute: their form field is named after a path key, not after
	 * {@code attribute.getName()}. Search forms that support them must iterate
	 * {@link #getSearchableAttributeRefs()} instead.</p>
	 *
	 * @return the ordered list of searchable top-level attributes; never null.
	 */
	public List<AttributeInterface> getSearchableAttributes() {
		List<AttributeInterface> searchableAttributes = new ArrayList<>();
		IApsEntity prototype = this.getEntityPrototype();
		if (null == prototype) {
			return searchableAttributes;
		}
		for (AttributeInterface attribute : prototype.getAttributeList()) {
			if (attribute.isActive() && attribute.isSearchable()) {
				searchableAttributes.add(attribute);
			}
		}
		return searchableAttributes;
	}

	/**
	 * The attributes the search form offers: searchable top-level attributes (legacy behaviour) plus
	 * boolean-like attributes nested inside Composites, addressed by their path key
	 * {@code <composite>_<boolean>}.
	 *
	 * <p>Each entry is a {@link SearchableAttributeRef} - the key, the display label and the <b>real</b>
	 * attribute - not a renamed copy of the attribute. OGNL resolves {@code #attribute.name} to the key,
	 * {@code #attribute.type} and {@code #attribute.textAttribute} to the real attribute's own values;
	 * anything else is reached through {@code #attribute.source.<property>}.</p>
	 *
	 * <p>This is additive: {@link #getSearchableAttributes()} keeps its original contract for
	 * pre-existing callers, and only forms that need nested attributes bind to this one.</p>
	 *
	 * @return the ordered list of searchable attribute references; never null.
	 */
	public List<SearchableAttributeRef> getSearchableAttributeRefs() {
		return this.getSearchSchema().getSearchableAttributes();
	}

	/**
	 * Display labels for {@link #getSearchableAttributeRefs()}, keyed by the attribute's machine key.
	 * A nested boolean's label is its hierarchy (e.g. {@code "compo > cmp_bool"}) reconstructed from the
	 * real attribute tree, so the search form renders it verbatim instead of splitting the flattened key
	 * on '_' - which would mis-segment a name that itself contains '_'.
	 * @return a map from machine key to display label; never null.
	 * @deprecated a form iterating {@link #getSearchableAttributeRefs()} already holds the label:
	 * {@code #attribute.label}. The core finder JSPs no longer bind this. Not removable yet: a downstream
	 * JSP may still bind {@code searchableAttributeLabels}, and no JSP is compiled by this build, so
	 * deleting it would break that page silently.
	 */
	@Deprecated
	public Map<String, String> getSearchableAttributeLabels() {
		return this.getSearchSchema().getLabels();
	}

	/**
	 * What the current entity type offers to a search: the attributes, their labels and the key each is
	 * addressed by. Read from the entity manager, which computes it once per type and keeps it, so a
	 * form render no longer walks the attribute tree once per question it asks.
	 * @return the schema of the current type; never null.
	 */
	protected EntitySearchSchema getSearchSchema() {
		IEntityManager entityManager = this.getEntityManager();
		EntitySearchSchema schema = (null == entityManager)
				? null : entityManager.getSearchSchema(this.getEntityTypeCode());
		// a finder without a manager has nothing to offer, and must not throw while a form renders
		return (null != schema) ? schema
				: EntitySearchSchema.build(null, EntitySearchKeys.DEFAULT_MAX_KEY_LENGTH);
	}

	public List<AttributeRole> getAttributeRoles() {
		return this.getEntityManager().getAttributeRoles();
	}
	
	protected abstract IEntityManager getEntityManager();
	
	public String getEntityId() {
		return _entityId;
	}
	public void setEntityId(String entityId) {
		this._entityId = entityId;
	}
	
	public String getEntityTypeCode() {
		return _entityTypeCode;
	}
	public void setEntityTypeCode(String entityTypeCode) {
		this._entityTypeCode = entityTypeCode;
	}
	
	protected boolean isAddedAttributeFilter() {
		return _addedAttributeFilter;
	}
	protected void setAddedAttributeFilter(boolean addedAttributeFilter) {
		this._addedAttributeFilter = addedAttributeFilter;
	}
	
	protected IEntityActionHelper getEntityActionHelper() {
		return _entityActionHelper;
	}
	public void setEntityActionHelper(IEntityActionHelper entityActionHelper) {
		this._entityActionHelper = entityActionHelper;
	}
	
	protected EntitySearchFilter[] getFilters() {
		return _filters;
	}
	protected void setFilters(EntitySearchFilter[] filters) {
		this._filters = filters;
	}
	
	private String _entityId;
	private String _entityTypeCode;
	
	private boolean _addedAttributeFilter;
	
	private IEntityActionHelper _entityActionHelper;
	
	private EntitySearchFilter[] _filters = new EntitySearchFilter[0];
	
} 
