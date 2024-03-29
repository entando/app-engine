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
package com.agiletec.aps.system.common.entity.model.attribute;

import com.agiletec.aps.system.common.entity.model.AttributeFieldError;
import com.agiletec.aps.system.common.entity.model.AttributeTracer;
import com.agiletec.aps.system.common.entity.model.FieldError;
import com.agiletec.aps.system.common.entity.model.IApsEntity;
import com.agiletec.aps.system.common.entity.model.attribute.util.BaseAttributeValidationRules;
import com.agiletec.aps.system.common.entity.model.attribute.util.IAttributeValidationRules;
import com.agiletec.aps.system.common.entity.parse.attribute.AttributeHandlerInterface;
import com.agiletec.aps.system.common.searchengine.IndexableAttributeInterface;
import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.util.ApsProperties;
import com.agiletec.aps.util.ApsPropertiesDOM;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.exception.EntRuntimeException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

/**
 * This abstract class must be used when implementing Entity Attributes.
 *
 * @author W.Ambu - E.Santoboni
 */
public abstract class AbstractAttribute implements AttributeInterface, Serializable {

    private static final EntLogger _logger = EntLogFactory.getSanitizedLogger(AbstractAttribute.class);

    private String _name;
    private ApsProperties _names;
    private String _description;
    private String _type;
    private String _defaultLangCode;
    private String _renderingLangCode;
    private boolean _searchable;
    private String _indexingType;
    private IApsEntity _parentEntity;
    private AttributeHandlerInterface _handler;
    private String[] _disablingCodes;
    private String[] _roles;
    private boolean _active = true;
    private IAttributeValidationRules _validationRules;

    private String _attributeManagerClassName;

    private transient ILangManager _langManager;

    @Override
    public boolean isMultilingual() {
        return false;
    }

    @Override
    public boolean isTextAttribute() {
        return false;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public ApsProperties getNames() {
        return _names;
    }

    @Override
    public void setNames(ApsProperties _names) {
        this._names = _names;
    }

    @Override
    public String getDescription() {
        return _description;
    }

    @Override
    public void setDescription(String description) {
        this._description = description;
    }

    @Override
    public void setName(String name) {
        this._name = name;
    }

    /**
     * The returned type corresponds to the attribute code as found in the declaration of the attribute type.
     */
    @Override
    public String getType() {
        return _type;
    }

    @Override
    public void setType(String typeName) {
        this._type = typeName;
    }

    @Override
    public void setDefaultLangCode(String langCode) {
        this._defaultLangCode = langCode;
    }

    /**
     * Return the code of the default language.
     *
     * @return The code of the default language.
     */
    public String getDefaultLangCode() {
        return _defaultLangCode;
    }

    /**
     * Set up the language to use in the rendering process.
     *
     * @param langCode The code of the rendering language.
     */
    @Override
    public void setRenderingLang(String langCode) {
        _renderingLangCode = langCode;
    }

    /**
     * Return the code of the language used in the rendering process.
     *
     * @return The code of the language used for rendering.
     */
    public String getRenderingLang() {
        return _renderingLangCode;
    }

    /**
     * Test whether the attribute is searchable (using a query on the DB) or not. The information held by a searchable
     * attribute is replicated in an appropriate table used for SQL queries.
     *
     * @return True if the attribute is a searchable one.
     */
    @Override
    public boolean isSearchable() {
        return _searchable;
    }

    /**
     * Set up the searchable status of an attribute. When set to 'true' then the information held by the current
     * attribute is replicated in an appropriate table used for SQL queries.
     *
     * @param searchable True if the attribute is of searchable type, false otherwise.
     */
    @Override
    public void setSearchable(boolean searchable) {
        this._searchable = searchable;
    }

    @Override
    public Object getAttributePrototype() {
        AbstractAttribute clone = null;
        try {
            Class attributeClass = Class.forName(this.getClass().getName());
            clone = (AbstractAttribute) attributeClass.newInstance();
            clone.setName(this.getName());
            clone.setNames(this.getNames());
            clone.setDescription(this.getDescription());
            clone.setType(this.getType());
            clone.setSearchable(this.isSearchable());
            clone.setDefaultLangCode(this.getDefaultLangCode());
            clone.setIndexingType(this.getIndexingType());
            clone.setParentEntity(this.getParentEntity());
            AttributeHandlerInterface handler = (AttributeHandlerInterface) this.getHandler().getAttributeHandlerPrototype();
            clone.setHandler(handler);
            if (this.getDisablingCodes() != null) {
                clone.setDisablingCodes(Arrays.copyOf(this.getDisablingCodes(), this.getDisablingCodes().length));
            }
            if (this.getRoles() != null) {
                clone.setRoles(Arrays.copyOf(this.getRoles(), this.getRoles().length));
            }
            clone.setValidationRules(this.getValidationRules().clone());
            clone.setLangManager(this.getLangManager());
            clone.setAttributeManagerClassName(this.getAttributeManagerClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException t) {
            String message = "Error detected while creating the attribute prototype '" + this.getName() + "' type '" + this.getType() + "'";
            _logger.error("Error detected while creating the attribute prototype '{}' type '{}'", this.getName(), this.getType(), t);
            throw new EntRuntimeException(message, t);
        }
        return clone;
    }

    @Override
    public void setAttributeConfig(Element attributeElement) throws EntException {
        try {
            String name = this.extractXmlAttribute(attributeElement, "name", true);
            this.setName(name);

            if (null != attributeElement.getChild("names") && null != attributeElement.getChild("names").getChild("properties")) {
                String namesXml = (new XMLOutputter()).outputString(attributeElement.getChild("names").getChild("properties"));
                ApsProperties names = new ApsProperties();
                names.loadFromXml(namesXml);
                this.setNames(names);
            }

            fallbackFromNamesToName();
            fallbackFromNameToNames();

            String description = this.extractXmlAttribute(attributeElement, "description", false);
            this.setDescription(description);
            String searchable = this.extractXmlAttribute(attributeElement, "searchable", false);
            //to guaranted compatibility with previsous version of Entando 4.0.1 *** Start Block
            if (null == searchable) {
                searchable = this.extractXmlAttribute(attributeElement, "searcheable", false);
            }
            //to guaranted compatibility with previsous version of Entando 4.0.1 *** End Block
            this.setSearchable(null != searchable && searchable.equalsIgnoreCase("true"));
            IAttributeValidationRules validationCondition = this.getValidationRules();
            validationCondition.setConfig(attributeElement);
            //to guaranted compatibility with previsous version of jAPS 2.0.12 *** Start Block
            String required = this.extractXmlAttribute(attributeElement, "required", false);
            if (null != required && required.equalsIgnoreCase("true")) {
                this.setRequired(true);
            }
            //to guaranted compatibility with previsous version of jAPS 2.0.12 *** End Block
            String indexingType = this.extractXmlAttribute(attributeElement, "indexingtype", false);
            if (null != indexingType) {
                this.setIndexingType(indexingType);
            } else {
                this.setIndexingType(IndexableAttributeInterface.INDEXING_TYPE_NONE);
            }
            Element disablingCodesElements = attributeElement.getChild("disablingCodes");
            if (null != disablingCodesElements) {
                String[] disablingCodes = this.extractValues(disablingCodesElements, "code");
                this.setDisablingCodes(disablingCodes);
            } else {
                //to guaranted compatibility with previsous version of jAPS 2.0.12 *** Start Block
                String disablingCodesStr = this.extractXmlAttribute(attributeElement, "disablingCodes", false);
                if (disablingCodesStr != null) {
                    String[] disablingCodes = disablingCodesStr.split(",");
                    this.setDisablingCodes(disablingCodes);
                }
                //to guaranted compatibility with previsous version of jAPS 2.0.12 *** End Block
            }
            Element rolesElements = attributeElement.getChild("roles");
            if (null != rolesElements) {
                String[] roles = this.extractValues(rolesElements, "role");
                this.setRoles(roles);
            }
        } catch (Exception t) {
            String message = "Error detected while creating the attribute config '"
                    + this.getName() + "' type '" + this.getType() + "'";
            _logger.error("Error detected while creating the attribute config '{}' type '{}'", this.getName(), this.getType(), t);
            throw new EntRuntimeException(message, t);
        }
    }

    private void fallbackFromNameToNames() {
        if (null != this.getName() && (null == this.getNames() || this.getNames().size() == 0)) {
            ApsProperties names = new ApsProperties();
            names.put(_langManager.getDefaultLang().getCode(), this.getName());
            this.setNames(names);
        }
    }

    private void fallbackFromNamesToName() {
        if (null == this.getName() && null != this.getNames() && this.getNames().size() > 0) {
            String defaultName = (String) this.getNames().get(_langManager.getDefaultLang().getCode());
            if (null == defaultName) {
                for (String lang : getNames().stringPropertyNames()) {
                    if (null != getNames().get(lang)) {
                        defaultName = (String) getNames().get(lang);
                        break;
                    }
                }
            }
            this.setName(defaultName);
        }
    }

    private String[] extractValues(Element elements, String subElementName) {
        if (null == elements) {
            return null;
        }
        List<String> values = new ArrayList<>();
        List<Element> subElements = elements.getChildren(subElementName);
        if (null == subElements || subElements.isEmpty()) {
            return null;
        }
        for (int i = 0; i < subElements.size(); i++) {
            String text = subElements.get(i).getText();
            if (null != text && text.trim().length() > 0) {
                values.add(text.trim());
            }
        }
        String[] array = new String[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }

    @Override
    public Element getJDOMConfigElement() {
        Element configElement = this.createRootElement(this.getTypeConfigElementName());
        if (null != this.getNames()) {
            Element names = new Element("names");
            ApsPropertiesDOM propertiesDom = new ApsPropertiesDOM();
            propertiesDom.buildJDOM(this.getNames());
            names.addContent(propertiesDom.getRootElement());
            configElement.addContent(names);
        }
        if (null != this.getDescription() && this.getDescription().trim().length() > 0) {
            configElement.setAttribute("description", this.getDescription());
        }
        if (this.isSearchable()) {
            configElement.setAttribute("searchable", "true");
        }
        if (null != this.getValidationRules() && !this.getValidationRules().isEmpty()) {
            Element validationElement = this.getValidationRules().getJDOMConfigElement();
            configElement.addContent(validationElement);
        }
        if (null != this.getIndexingType() && !this.getIndexingType().equals(IndexableAttributeInterface.INDEXING_TYPE_NONE)) {
            configElement.setAttribute("indexingtype", this.getIndexingType());
        }
        this.addArrayElement(configElement, this.getDisablingCodes(), "disablingCodes", "code");
        this.addArrayElement(configElement, this.getRoles(), "roles", "role");
        return configElement;
    }

    protected Element createRootElement(String elementName) {
        Element attributeElement = new Element(elementName);
        attributeElement.setAttribute("name", this.getName());
        attributeElement.setAttribute("attributetype", this.getType());
        return attributeElement;
    }

    private void addArrayElement(Element configElement, String[] values, String elementName, String subElementName) {
        if (null != values) {
            Element arrayElem = new Element(elementName);
            for (int i = 0; i < values.length; i++) {
                Element stringElem = new Element(subElementName);
                stringElem.setText(values[i]);
                arrayElem.addContent(stringElem);
            }
            configElement.addContent(arrayElem);
        }
    }

    protected String getTypeConfigElementName() {
        return "attribute";
    }

    /**
     * Get the attribute matching the given criteria from a XML string.
     *
     * @param currElement The element where to extract the value of the attribute from
     * @param attributeName Name of the requested attribute.
     * @param required Select a mandatory condition of the attribute to search for.
     * @return The value of the requested attribute.
     * @throws EntException when a attribute declared mandatory is not present in the given XML element.
     */
    protected String extractXmlAttribute(Element currElement, String attributeName,
            boolean required) throws EntException {
        String value = currElement.getAttributeValue(attributeName);
        if (required && value == null) {
            throw new EntException("Attribute '" + attributeName + "' not found in the tag <" + currElement.getName() + ">");
        }
        return value;
    }

    @Override
    public String getIndexingType() {
        return _indexingType;
    }

    @Override
    public void setIndexingType(String indexingType) {
        this._indexingType = indexingType;
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public boolean isRequired() {
        return this.getValidationRules().isRequired();
    }

    @Override
    public void setRequired(boolean required) {
        this.getValidationRules().setRequired(required);
    }

    @Override
    public IApsEntity getParentEntity() {
        return _parentEntity;
    }

    @Override
    public void setParentEntity(IApsEntity parentEntity) {
        this._parentEntity = parentEntity;
    }

    @Override
    public AttributeHandlerInterface getHandler() {
        return _handler;
    }

    @Override
    public void setHandler(AttributeHandlerInterface handler) {
        this._handler = handler;
    }

    @Override
    public void disable(String disablingCode) {
        if (_disablingCodes != null && disablingCode != null) {
            for (int i = 0; i < _disablingCodes.length; i++) {
                if (disablingCode.equals(_disablingCodes[i])) {
                    this._active = false;
                    return;
                }
            }
        }
    }

    @Override
    public void activate() {
        this._active = true;
    }

    @Override
    public boolean isActive() {
        return _active;
    }

    @Override
    public void setDisablingCodes(String[] disablingCodes) {
        this._disablingCodes = disablingCodes;
    }

    @Override
    public String[] getDisablingCodes() {
        return this._disablingCodes;
    }

    @Override
    public String[] getRoles() {
        return _roles;
    }

    @Override
    public void setRoles(String[] roles) {
        this._roles = roles;
    }

    protected IAttributeValidationRules getValidationRuleNewIntance() {
        return new BaseAttributeValidationRules();
    }

    @Override
    public IAttributeValidationRules getValidationRules() {
        if (null == this._validationRules) {
            this.setValidationRules(this.getValidationRuleNewIntance());
        }
        return _validationRules;
    }

    @Override
    public void setValidationRules(IAttributeValidationRules validationRules) {
        this._validationRules = validationRules;
    }

    /**
     * Create and return the Jaxb structure of the attribute. This method return the object only if the value of the
     * attribute is not null, and it can be overriten by custom attributes class.
     *
     * @param langCode The lang code
     * @return The jaxb structure of the atttribute
     */
    protected AbstractJAXBAttribute createJAXBAttribute(String langCode) {
        if (null == this.getValue()) {
            return null;
        }
        return this.createBaseJAXBAttribute();
    }

    /**
     * Create and return the base Jaxb structure of the attribute. This method can't be overriten by other attributes
     * class.
     *
     * @return The base jaxb structure of the attribute.
     */
    protected final AbstractJAXBAttribute createBaseJAXBAttribute() {
        AbstractJAXBAttribute jaxbAttribute = this.getJAXBAttributeInstance();
        jaxbAttribute.setDescription(this.getDescription());
        jaxbAttribute.setNames(new HashMap<>());
        Optional<ApsProperties> apsNames = Optional.ofNullable(this.getNames());
        apsNames.ifPresent(values -> values.keySet().forEach(lang -> jaxbAttribute.getNames().put((String) lang, (String) values.get(lang))));
        jaxbAttribute.setName(this.getName());
        jaxbAttribute.setType(this.getType());
        if (null != this.getRoles() && this.getRoles().length > 0) {
            List<String> roles = Arrays.asList(this.getRoles());
            jaxbAttribute.setRoles(roles);
        }
        return jaxbAttribute;
    }

    protected abstract AbstractJAXBAttribute getJAXBAttributeInstance();

    @Override
    public void valueFrom(AbstractJAXBAttribute jaxbAttribute, String langCode) {
        this.setName(jaxbAttribute.getName());
    }

    @Override
    public DefaultJAXBAttributeType getJAXBAttributeType() {
        DefaultJAXBAttributeType jaxbAttributeType = this.getJAXBAttributeTypeInstance();
        jaxbAttributeType.setName(this.getName());
        jaxbAttributeType.setNames(new HashMap<>());
        Optional<ApsProperties> apsNames = Optional.ofNullable(this.getNames());
        apsNames.ifPresent(values -> values.keySet().forEach((lang) -> {
            jaxbAttributeType.getNames().put((String) lang, (String) values.get(lang));
        }));
        jaxbAttributeType.setDescription(this.getDescription());
        jaxbAttributeType.setType(this.getType());
        if (this.isSearchable()) {
            jaxbAttributeType.setSearchable(true);
        }
        if (null != this.getIndexingType() && this.getIndexingType().equalsIgnoreCase(IndexableAttributeInterface.INDEXING_TYPE_TEXT)) {
            jaxbAttributeType.setIndexable(true);
        }
        if (null != this.getRoles() && this.getRoles().length > 0) {
            List<String> roles = Arrays.asList(this.getRoles());
            jaxbAttributeType.setRoles(roles);
        }
        if (null != this.getValidationRules() && !this.getValidationRules().isEmpty()) {
            jaxbAttributeType.setValidationRules(this.getValidationRules());
        }
        return jaxbAttributeType;
    }

    protected DefaultJAXBAttributeType getJAXBAttributeTypeInstance() {
        return new DefaultJAXBAttributeType();
    }

    @Override
    @Deprecated
    public List<AttributeFieldError> validate(AttributeTracer tracer, ILangManager langManager) {
        return this.validate(tracer, langManager, null);
    }

    @Override
    public List<AttributeFieldError> validate(AttributeTracer tracer, ILangManager langManager, BeanFactory beanFactory) {
        List<AttributeFieldError> errors = new ArrayList<>();
        try {
            if (this.getStatus().equals(Status.INCOMPLETE)) {
                errors.add(new AttributeFieldError(this, FieldError.INVALID, tracer));
            } else {
                IAttributeValidationRules validationRules = this.getValidationRules();
                if (null == validationRules) {
                    return errors;
                }
                List<AttributeFieldError> validationRulesErrors = validationRules.validate(this, tracer, langManager);
                if (null != validationRulesErrors) {
                    errors.addAll(validationRulesErrors);
                }
            }
        } catch (Exception t) {
            _logger.error("Error validating Attribute '{}'", this.getName(), t);
            throw new EntRuntimeException("Error validating Attribute '" + this.getName() + "'", t);
        }
        return errors;
    }

    @Override
    public String getAttributeManagerClassName() {
        return _attributeManagerClassName;
    }

    public void setAttributeManagerClassName(String attributeManagerClassName) {
        this._attributeManagerClassName = attributeManagerClassName;
    }

    @Deprecated
    protected ILangManager getLangManager() {
        return _langManager;
    }

    @Deprecated
    public void setLangManager(ILangManager langManager) {
        this._langManager = langManager;
    }

    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        WebApplicationContext ctx = ContextLoader.getCurrentWebApplicationContext();
        if (ctx == null) {
            _logger.warn("Null WebApplicationContext during deserialization");
            return;
        }
        this.setLangManager(ctx.getBean(ILangManager.class));
    }
}
