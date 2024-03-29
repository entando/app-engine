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
package org.entando.entando.aps.system.services.widgettype.api;

import com.agiletec.aps.util.ApsProperties;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.entando.entando.aps.system.services.api.IApiErrorCodes;
import org.entando.entando.aps.system.services.api.model.ApiException;
import org.entando.entando.aps.system.services.guifragment.GuiFragment;
import org.entando.entando.aps.system.services.guifragment.api.JAXBGuiFragment;
import org.entando.entando.aps.system.services.widgettype.IWidgetTypeManager;
import org.entando.entando.aps.system.services.widgettype.WidgetType;
import org.entando.entando.aps.system.services.widgettype.WidgetTypeParameter;
import org.springframework.http.HttpStatus;

/**
 * @author E.Santoboni
 */
@XmlRootElement(name = "widgetType")
@XmlType(propOrder = {"code", "titles", "pluginCode", "mainGroup", "typeParameters", "action", "parentTypeCode", "config", "locked", "gui", "fragments", "readonlyPageWidgetConfig", "widgetCategory", "icon"})
public class JAXBWidgetType implements Serializable {

	public JAXBWidgetType() {}

	public JAXBWidgetType(WidgetType widgetType, GuiFragment fragment, List<GuiFragment> fragments) {
		this.setAction(widgetType.getAction());
		this.setCode(widgetType.getCode());
		this.setConfig(widgetType.getConfig());
		this.setLocked(widgetType.isLocked());
		this.setMainGroup(widgetType.getMainGroup());
		if (null != widgetType.getParentType()) {
			this.setParentTypeCode(widgetType.getParentType().getCode());
		}
		this.setPluginCode(widgetType.getPluginCode());
		this.setTitles(widgetType.getTitles());
		this.setTypeParameters(widgetType.getTypeParameters());
        this.setReadonlyPageWidgetConfig(widgetType.isReadonlyPageWidgetConfig());
        this.setWidgetCategory(widgetType.getWidgetCategory());
        this.setIcon(widgetType.getIcon());
        if (null != fragment) {
			this.setGui(fragment.getCurrentGui());
		}
		if (null != fragments) {
			List<JAXBGuiFragment> jaxbFragments = null;
			for (int i = 0; i < fragments.size(); i++) {
				GuiFragment guiFragment = fragments.get(i);
				if (null != guiFragment) {
					JAXBGuiFragment jaxbGuiFragment = new JAXBGuiFragment(guiFragment);
					if (null == jaxbFragments) {
						jaxbFragments = new ArrayList<JAXBGuiFragment>();
					}
					jaxbFragments.add(jaxbGuiFragment);
				}
			}
			this.setFragments(jaxbFragments);
		}
	}

	public WidgetType getNewWidgetType(IWidgetTypeManager widgetTypeManager) {
		WidgetType type = new WidgetType();
		type.setCode(this.getCode());
		type.setTitles(this.getTitles());
		List<WidgetTypeParameter> parameters = this.getTypeParameters();
		if (null != parameters && !parameters.isEmpty()) {
			type.setTypeParameters(parameters);
			type.setAction("configSimpleParameter");
		} else {
			ApsProperties configuration = this.getConfig();
			String parentTypeCode = this.getParentTypeCode();
			if (null != parentTypeCode && null != configuration && !configuration.isEmpty()) {
				WidgetType parentType = widgetTypeManager.getWidgetType(parentTypeCode);
				type.setParentType(parentType);
				type.setConfig(configuration);
			}
		}
		type.setMainGroup(this.getMainGroup());
		//type.setLocked(this.isLocked());

		type.setPluginCode(this.getPluginCode());
		return type;
	}

	public WidgetType getModifiedWidgetType(IWidgetTypeManager widgetTypeManager) throws ApiException {
		WidgetType type = widgetTypeManager.getWidgetType(this.getCode());
		type.setTitles(this.getTitles());
		if (type.isLogic()) {
			ApsProperties configuration = this.getConfig();
			type.setConfig(configuration);
		} else {
			List<WidgetTypeParameter> parameters = this.getTypeParameters();
			if (null != parameters && !parameters.isEmpty()) {
				//Parameters of existing widget mustn't been changed
				//type.setTypeParameters(parameters);
				//type.setAction("configSimpleParameter");
				throw new ApiException(IApiErrorCodes.API_VALIDATION_ERROR, "Parameters of existing widget mustn't been changed", HttpStatus.CONFLICT);
			}
		}
		type.setMainGroup(this.getMainGroup());
		//type.setLocked(this.isLocked());
		//type.setPluginCode(this.getPluginCode());
		return type;
	}

	@XmlElement(name = "code", required = false)
	public String getCode() {
		return _code;
	}
	public void setCode(String code) {
		this._code = code;
	}

	@XmlElement(name = "title", required = false)
    @XmlElementWrapper(name = "titles", required = false)
	public ApsProperties getTitles() {
		return _titles;
	}
	public void setTitles(ApsProperties titles) {
		this._titles = titles;
	}

	@XmlElement(name = "typeParameter", required = false)
    @XmlElementWrapper(name = "typeParameters", required = false)
	public List<WidgetTypeParameter> getTypeParameters() {
		return _parameters;
	}
	public void setTypeParameters(List<WidgetTypeParameter> typeParameters) {
		this._parameters = typeParameters;
	}

	@XmlElement(name = "action", required = false)
	public String getAction() {
		return _action;
	}
	public void setAction(String action) {
		this._action = action;
	}

	@XmlElement(name = "pluginCode", required = false)
	public String getPluginCode() {
		return _pluginCode;
	}
	public void setPluginCode(String pluginCode) {
		this._pluginCode = pluginCode;
	}

	@XmlElement(name = "parentTypeCode", required = false)
	protected String getParentTypeCode() {
		return _parentTypeCode;
	}
	protected void setParentTypeCode(String parentTypeCode) {
		this._parentTypeCode = parentTypeCode;
	}

	@XmlElement(name = "configuration", required = false)
    @XmlElementWrapper(name = "configurations", required = false)
	public ApsProperties getConfig() {
		return _config;
	}
	public void setConfig(ApsProperties config) {
		this._config = config;
	}

	@XmlElement(name = "locked", required = false)
	public boolean isLocked() {
		return _locked;
	}
	public void setLocked(boolean locked) {
		this._locked = locked;
	}

	@XmlElement(name = "mainGroup", required = false)
	public String getMainGroup() {
		return _mainGroup;
	}
	public void setMainGroup(String mainGroup) {
		this._mainGroup = mainGroup;
	}

	@XmlElement(name = "gui", required = false)
	public String getGui() {
		return _gui;
	}
	public void setGui(String gui) {
		this._gui = gui;
	}

	@XmlElement(name = "fragment", required = false)
    @XmlElementWrapper(name = "fragments", required = false)
	public List<JAXBGuiFragment> getFragments() {
		return _fragments;
	}
	protected void setFragments(List<JAXBGuiFragment> fragments) {
		this._fragments = fragments;
	}

	@XmlElement(name = "readonlypagewidgetconfig", required = false)
	public boolean isReadonlyPageWidgetConfig() {
		return readonlyPageWidgetConfig;
	}
	public void setReadonlyPageWidgetConfig(boolean readonlyPageWidgetConfig) {
		this.readonlyPageWidgetConfig = readonlyPageWidgetConfig;
	}

    @XmlElement(name = "widgetCategory")
    public String getWidgetCategory() {
        return widgetCategory;
    }
    public void setWidgetCategory(String widgetCategory) {
        this.widgetCategory = widgetCategory;
    }

    @XmlElement(name = "icon")
    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    private String _code;
	private ApsProperties _titles;
	private List<WidgetTypeParameter> _parameters;
	private String _action;
	private String _pluginCode;
	private String _parentTypeCode;
	private ApsProperties _config;
	private boolean _locked;
	private String _mainGroup;
	private String _gui;
	private List<JAXBGuiFragment> _fragments;
	private boolean readonlyPageWidgetConfig;
    private String widgetCategory;
    private String icon;

}