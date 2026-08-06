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

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

import org.entando.entando.aps.system.common.entity.search.NestedSearchSupport;
import org.entando.entando.aps.system.common.entity.search.SearchableAttributeRef;
import com.agiletec.aps.system.common.entity.model.ApsEntity;
import com.agiletec.aps.system.common.entity.model.AttributeFieldError;
import com.agiletec.aps.system.common.entity.model.AttributeTracer;
import com.agiletec.aps.system.common.entity.model.EntitySearchFilter;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.AbstractAttribute;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeInterface;
import com.agiletec.aps.system.common.entity.model.attribute.AttributeRole;
import com.agiletec.aps.system.common.entity.model.attribute.ThreeStateAttribute;
import org.entando.entando.aps.system.common.entity.search.SearchFieldType;
import com.agiletec.aps.util.CheckFormatUtil;
import com.agiletec.aps.util.DateConverter;
import com.agiletec.apsadmin.system.BaseActionHelper;
import com.agiletec.apsadmin.system.entity.attribute.manager.AbstractAttributeManager;
import com.agiletec.apsadmin.system.entity.attribute.manager.AttributeManagerInterface;
import org.apache.struts2.ActionSupport;

/**
 * This abstract class supports all the helper classes that, in turn, support those
 * classes which handle elements built with the "ApsEntity' entries.
 * @author E.Santoboni
 */
// NOTE: java:S2143 ("use the java.time API") is intentionally suppressed. Legacy java.util.Date is used
// only to parse the date-range search form fields (via DateConverter); java.time migration is out of
// scope for ESB-1133 (boolean search) and tracked separately.
@SuppressWarnings("java:S2143")
public class EntityActionHelper extends BaseActionHelper implements IEntityActionHelper, BeanFactoryAware {

	private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(EntityActionHelper.class);


	@Override
	public void updateEntity(IApsEntity currentEntity, HttpServletRequest request) {
		try {
			List<AttributeInterface> attributes = currentEntity.getAttributeList();
			for (int i = 0; i < attributes.size(); i++) {
				AttributeInterface attribute = attributes.get(i);
				if (attribute.isActive()) {
					AttributeManagerInterface attributeManager = this.getManager(attribute);
					if (attributeManager != null) {
						attributeManager.updateEntityAttribute(attribute, request);
					}
				}
			}
		} catch (Throwable t) {
			_logger.error("Error updating Entity", t);
			throw new RuntimeException("Error updating Entity", t);
		}
	}
    
	@Override
	public void scanEntity(IApsEntity currentEntity, ActionSupport action) {
		try {
			List<AttributeInterface> attributes = currentEntity.getAttributeList();
			for (int i = 0; i < attributes.size(); i++) {
				AttributeInterface entityAttribute = attributes.get(i);
				if (entityAttribute.isActive()) {
					List<AttributeFieldError> errors = entityAttribute.validate(new AttributeTracer(), super.getLangManager(), this.getBeanFactory());
					if (null != errors && errors.size() > 0) {
						for (int j = 0; j < errors.size(); j++) {
							AttributeFieldError attributeFieldError = errors.get(j);
							AttributeTracer tracer = attributeFieldError.getTracer();
							AttributeInterface attribute = attributeFieldError.getAttribute();
							String messageAttributePositionPrefix = this.createErrorMessageAttributePositionPrefix(action, attribute, tracer);
							AttributeManagerInterface attributeManager = this.getManager(attribute);
							String errorMessage = attributeManager.getErrorMessage(attributeFieldError, action);
							String formFieldName = tracer.getFormFieldName(attributeFieldError.getAttribute());
							action.addFieldError(formFieldName, messageAttributePositionPrefix + " " + errorMessage);
						}
					}
				}
			}
		} catch (Throwable t) {
			_logger.error("Error scanning Entity", t);
			throw new RuntimeException("Error scanning Entity", t);
		}
	}
    
    private String createErrorMessageAttributePositionPrefix(ActionSupport action, AttributeInterface attribute, com.agiletec.aps.system.common.entity.model.AttributeTracer tracer) {
        if (tracer.isMonoListElement()) {
            if (tracer.isCompositeElement()) {
                String[] args = {tracer.getParentAttribute().getName(), String.valueOf(tracer.getListIndex() + 1), attribute.getName()};
                return action.getText("EntityAttribute.compositeListAttributeElement.errorMessage.prefix", args);
            } else {
                String[] args = {attribute.getName(), String.valueOf(tracer.getListIndex() + 1)};
                return action.getText("EntityAttribute.monolistAttributeElement.errorMessage.prefix", args);
            }
        } else if (tracer.isCompositeElement()) {
            String[] args = {tracer.getParentAttribute().getName(), attribute.getName()};
            return action.getText("EntityAttribute.compositeAttributeElement.errorMessage.prefix", args);
        } else if (tracer.isListElement()) {
            String[] args = {attribute.getName(), tracer.getListLang().getDescr(), String.valueOf(tracer.getListIndex() + 1)};
            return action.getText("EntityAttribute.listAttributeElement.errorMessage.prefix", args);
        } else {
            String[] args = {attribute.getName()};
            return action.getText("EntityAttribute.singleAttribute.errorMessage.prefix", args);
        }
    }
    
	protected AttributeManagerInterface getManager(AttributeInterface attribute) {
		String managerClassName = attribute.getAttributeManagerClassName();
        try {
			if (null == managerClassName) return null;
            Class managerClass = Class.forName(managerClassName);
            Object managerInstance = managerClass.newInstance();
            if (managerInstance instanceof AbstractAttributeManager) {
				AbstractAttributeManager manager = (AbstractAttributeManager) managerInstance;
				manager.setBeanFactory(this.getBeanFactory());
				return manager;
			}
        } catch (Throwable t) {
            String message = "Error creating manager of attribute '"
                    + attribute.getName() + "' type '" + attribute.getType() + "' -  Manager class '" + managerClassName + "'";
            _logger.error("Error creating manager of attribute '{}', type: {} - Manager class: {}", attribute.getName(),attribute.getType(), managerClassName,  t);
            throw new RuntimeException(message, t);
        }
        return null;
    }
	
	@Override
	@Deprecated
	public EntitySearchFilter[] getSearchFilters(AbstractApsEntityFinderAction entityFinderAction, IApsEntity prototype) {
		return this.getAttributeFilters(entityFinderAction, prototype);
	}
	
	@Override
	public EntitySearchFilter[] getRoleFilters(AbstractApsEntityFinderAction entityFinderAction) {
		EntitySearchFilter[] filters = new EntitySearchFilter[0];
		List<AttributeRole> attributeRoles = entityFinderAction.getAttributeRoles();
		if (null != attributeRoles) {
			for (int i = 0; i < attributeRoles.size(); i++) {
				AttributeRole attributeRole = attributeRoles.get(i);
				if (AttributeRole.FormFieldTypes.TEXT.equals(attributeRole.getFormFieldType())) {
					String insertedText = entityFinderAction.getSearchFormFieldValue(attributeRole.getName() + "_textFieldName");
					if (null != insertedText && insertedText.trim().length() > 0) {
						EntitySearchFilter filterToAdd = EntitySearchFilter.createRoleFilter(attributeRole.getName(), insertedText.trim(), true);
						filters = this.addFilter(filters, filterToAdd);
					}
				} else if (AttributeRole.FormFieldTypes.DATE.equals(attributeRole.getFormFieldType())) {
					Date dateStart = this.getDateSearchFormValue(entityFinderAction, attributeRole.getName(), "_dateStartFieldName", true);
					Date dateEnd = this.getDateSearchFormValue(entityFinderAction, attributeRole.getName(), "_dateEndFieldName", false);
					if (null != dateStart || null != dateEnd) {
						EntitySearchFilter filterToAdd = EntitySearchFilter.createRoleFilter(attributeRole.getName(), dateStart, dateEnd);
						filters = this.addFilter(filters, filterToAdd);
					}
				} else if (AttributeRole.FormFieldTypes.BOOLEAN.equals(attributeRole.getFormFieldType())) {
					String booleanValue = entityFinderAction.getSearchFormFieldValue(attributeRole.getName() + "_booleanFieldName");
					if (null != booleanValue && booleanValue.trim().length() > 0) {
						EntitySearchFilter filterToAdd = EntitySearchFilter.createRoleFilter(attributeRole.getName(), booleanValue, false);
						filters = this.addFilter(filters, filterToAdd);
					}
				} else if (AttributeRole.FormFieldTypes.NUMBER.equals(attributeRole.getFormFieldType())) {
					BigDecimal numberStart = this.getNumberSearchFormValue(entityFinderAction, attributeRole.getName(), "_numberStartFieldName", true);
					BigDecimal numberEnd = this.getNumberSearchFormValue(entityFinderAction, attributeRole.getName(), "_numberEndFieldName", false);
					if (null != numberStart || null != numberEnd) {
						EntitySearchFilter filterToAdd = EntitySearchFilter.createRoleFilter(attributeRole.getName(), numberStart, numberEnd);
						filters = this.addFilter(filters, filterToAdd);
					}
				}
			}
		}
		return filters;
	}
	
	@Override
	public EntitySearchFilter[] getAttributeFilters(AbstractApsEntityFinderAction entityFinderAction, IApsEntity prototype) {
		EntitySearchFilter[] filters = new EntitySearchFilter[0];
		if (null == prototype) {
			return filters;
		}
		// Same list the search form is built from: searchable top-level attributes plus Composite-nested
		// boolean-like attributes keyed by "<composite>_<boolean>". Iterating the identical list
		// guarantees the parser resolves exactly the field names the form submitted. The eligibility
		// gate (active/searchable, boolean-like when nested) is applied once, by collectSearchable; the
		// dispatch below reads the REAL attribute, so the type is always the genuine one.
		List<SearchableAttributeRef> searchableAttributes = NestedSearchSupport
				.collectSearchable(prototype);
		for (SearchableAttributeRef ref : searchableAttributes) {
			String key = ref.key();
			// One dispatch mechanism, the same one the finder JSPs use: the attribute's declared search
			// field type, except for text. TEXT and isTextAttribute() happen to coincide for every type
			// the platform ships, but they are different questions - TEXT is "indexable as free text",
			// isTextAttribute() is "carries a per-language text a filter can match" - so the text branch
			// keeps asking the narrower one it has always asked.
			if (ref.isTextAttribute()) {
				String insertedText = entityFinderAction.getSearchFormFieldValue(key + "_textFieldName");
				if (null != insertedText && insertedText.trim().length() > 0) {
					EntitySearchFilter filterToAdd = new EntitySearchFilter(key, true, insertedText.trim(), true);
					filters = this.addFilter(filters, filterToAdd);
				}
			} else if (ref.isDate()) {
				Date dateStart = this.getDateSearchFormValue(entityFinderAction, key, "_dateStartFieldName", true);
				Date dateEnd = this.getDateSearchFormValue(entityFinderAction, key, "_dateEndFieldName", false);
				if (null != dateStart || null != dateEnd) {
					EntitySearchFilter filterToAdd = new EntitySearchFilter(key, true, dateStart, dateEnd);
					filters = this.addFilter(filters, filterToAdd);
				}
			} else if (ref.isTristate()) {
				// Three states: "true"/"false" filter by value; the "not set" literal matches the unset
				// state, which on the DB search path is the ABSENCE of a record (a ThreeState writes no
				// row when unset) - so it is queried via the null option, not a value; blank means "Any".
				EntitySearchFilter filterToAdd = this.buildThreeStateFilter(entityFinderAction, key);
				if (null != filterToAdd) {
					filters = this.addFilter(filters, filterToAdd);
				}
			} else if (ref.isBooleanLike()) {
				String booleanValue = entityFinderAction.getSearchFormFieldValue(key + "_booleanFieldName");
				if (null != booleanValue && booleanValue.trim().length() > 0) {
					EntitySearchFilter filterToAdd = new EntitySearchFilter(key, true, booleanValue, false);
					filters = this.addFilter(filters, filterToAdd);
				}
			} else if (ref.isNumber()) {
				BigDecimal numberStart = this.getNumberSearchFormValue(entityFinderAction, key, "_numberStartFieldName", true);
				BigDecimal numberEnd = this.getNumberSearchFormValue(entityFinderAction, key, "_numberEndFieldName", false);
				if (null != numberStart || null != numberEnd) {
					EntitySearchFilter filterToAdd = new EntitySearchFilter(key, true, numberStart, numberEnd);
					filters = this.addFilter(filters, filterToAdd);
				}
			}
		}
		return filters;
	}

	@Override
	public String[] getAttributeFilterFieldName(ApsEntity prototype, String attrName) {
		AbstractAttribute attr = (AbstractAttribute) prototype.getAttribute(attrName);
		if (null == attr) {
			// Not a top-level attribute: it may be a Composite-nested boolean addressed by its
			// path key "<composite>_<boolean>". Resolve it so the remembered search round-trips.
			attr = (AbstractAttribute) NestedSearchSupport.resolveNestedByKey(prototype, attrName);
		}
		if (null == attr) {
			return new String[0];
		}
		// Same dispatch as getAttributeFilters, so the field names derived here are the ones parsed there.
		// Null-safe on purpose: a searchable Composite/Monolist declares no search field type.
		SearchFieldType searchFieldType = attr.getSearchFieldType();
		if (attr.isTextAttribute()) {
			return new String[] {attrName + "_textFieldName"};
		} else if (SearchFieldType.DATE == searchFieldType) {
			return new String[] {attrName + "_dateStartFieldName", attrName + "_dateEndFieldName"};
		} else if (SearchFieldType.NUMBER == searchFieldType) {
			return new String[] {attrName + "_numberStartFieldName", attrName + "_numberEndFieldName"};
		} else if (null != searchFieldType && searchFieldType.isBooleanFamily()) {
			// The whole boolean family shares one form field; ThreeState differs only in the values it
			// offers, which buildThreeStateFilter handles.
			return new String[] {attrName + "_booleanFieldName"};
		}
		return new String[0];
	}
	
    /**
     * Build the search filter for a ThreeState attribute from its {@code _booleanFieldName} form field.
     * Blank -&gt; {@code null} ("Any", no filter). {@code "none"} ("Not set") -&gt; a null-option filter,
     * because an unset ThreeState leaves no DB search record. {@code "true"}/{@code "false"} -&gt; a value
     * filter, as for a plain boolean.
     */
    private EntitySearchFilter buildThreeStateFilter(AbstractApsEntityFinderAction entityFinderAction, String attrName) {
        String value = entityFinderAction.getSearchFormFieldValue(attrName + "_booleanFieldName");
        if (null == value || value.trim().isEmpty()) {
            return null;
        }
        value = value.trim();
        if (ThreeStateAttribute.NOT_SET_SEARCH_VALUE.equalsIgnoreCase(value)) {
            EntitySearchFilter filter = new EntitySearchFilter(attrName, true);
            filter.setNullOption(true);
            return filter;
        }
        return new EntitySearchFilter(attrName, true, value, false);
    }

    private Date getDateSearchFormValue(AbstractApsEntityFinderAction entityFinderAction,
            String fieldName, String dateFieldNameSuffix, boolean start) {
        String inputFormName = fieldName + dateFieldNameSuffix;
        String insertedDate = entityFinderAction.getSearchFormFieldValue(inputFormName);
        Date date = null;
        if (insertedDate != null && insertedDate.trim().length() > 0) {
            if (CheckFormatUtil.isValidDate(insertedDate.trim())) {
                date = DateConverter.parseDate(insertedDate.trim(), EntitySearchFilter.DATE_PATTERN);
            } else {
                String[] args = {fieldName};
                if (start) {
                    entityFinderAction.addFieldError(inputFormName, entityFinderAction.getText("error.attribute.startDate.invalid", args));
                } else {
                    entityFinderAction.addFieldError(inputFormName, entityFinderAction.getText("error.attribute.endDate.invalid", args));
                }
            }
        }
        return date;
    }

    private BigDecimal getNumberSearchFormValue(AbstractApsEntityFinderAction entityFinderAction,
            String fieldName, String numberFieldNameSuffix, boolean start) {
        String inputFormName = fieldName + numberFieldNameSuffix;
        String insertedNumberString = entityFinderAction.getSearchFormFieldValue(inputFormName);
        BigDecimal bigdecimal = null;
        if (insertedNumberString != null && insertedNumberString.trim().length() > 0) {
            if (CheckFormatUtil.isValidNumber(insertedNumberString.trim())) {
                bigdecimal = new BigDecimal(Integer.parseInt(insertedNumberString.trim()));
            } else {
                String[] args = {fieldName};
                if (start) {
                    entityFinderAction.addFieldError(inputFormName, entityFinderAction.getText("error.attribute.startNumber.invalid", args));
                } else {
                    entityFinderAction.addFieldError(inputFormName, entityFinderAction.getText("error.attribute.endNumber.invalid", args));
                }
            }
        }
        return bigdecimal;
    }
	
    private EntitySearchFilter[] addFilter(EntitySearchFilter[] filters, EntitySearchFilter filterToAdd) {
        int len = filters.length;
        EntitySearchFilter[] newFilters = new EntitySearchFilter[len + 1];
        for (int i = 0; i < len; i++) {
            newFilters[i] = filters[i];
        }
        newFilters[len] = filterToAdd;
        return newFilters;
    }
    
	protected BeanFactory getBeanFactory() {
		return _beanFactory;
	}
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this._beanFactory = beanFactory;
	}
	
	private BeanFactory _beanFactory;
	
}
