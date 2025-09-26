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
package org.entando.entando.aps.system.services.guifragment.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.entando.entando.aps.system.services.guifragment.GuiFragment;

/**
 * @author E.Santoboni
 */
@XmlRootElement(name = "guiFragment")
@JsonRootName("guiFragment")
@XmlType(propOrder = {"code", "widgetTypeCode", "pluginCode", "gui", "defaultGui", "locked"})
public class JAXBGuiFragment {
	
    public JAXBGuiFragment() {
        super();
    }
	
    public JAXBGuiFragment(GuiFragment guiFragment) {
		this.setCode(guiFragment.getCode());
		this.setWidgetTypeCode(guiFragment.getWidgetTypeCode());
		this.setPluginCode(guiFragment.getPluginCode());
		this.setGui(guiFragment.getGui());
		this.setDefaultGui(guiFragment.getDefaultGui());
		this.setLocked(guiFragment.isLocked());
    }
    
	@XmlTransient
    public GuiFragment getGuiFragment() {
    	GuiFragment guiFragment = new GuiFragment();
		guiFragment.setCode(this.getCode());
		guiFragment.setWidgetTypeCode(this.getWidgetTypeCode());
		guiFragment.setPluginCode(this.getPluginCode());
		guiFragment.setGui(this.getGui());
    	guiFragment.setDefaultGui(this.getDefaultGui());
    	guiFragment.setLocked(this.isLocked());
    	return guiFragment;
    }
	
	@XmlElement(name = "code", required = true)
	@JsonProperty("code")
	public String getCode() {
		return _code;
	}
	public void setCode(String code) {
		this._code = code;
	}
	
	@XmlElement(name = "widgetTypeCode", required = true)
	@JsonProperty("widgetTypeCode")
	public String getWidgetTypeCode() {
		return _widgetTypeCode;
	}
	public void setWidgetTypeCode(String widgetTypeCode) {
		this._widgetTypeCode = widgetTypeCode;
	}
	
	@XmlElement(name = "pluginCode", required = true)
	@JsonProperty("pluginCode")
	public String getPluginCode() {
		return _pluginCode;
	}
	public void setPluginCode(String pluginCode) {
		this._pluginCode = pluginCode;
	}

	@XmlElement(name = "gui", required = true)
	@JsonProperty("gui")
	public String getGui() {
		return _gui;
	}
	public void setGui(String gui) {
		this._gui = gui;
	}

	@XmlElement(name = "defaultGui", required = true)
	@JsonProperty("defaultGui")
	public String getDefaultGui() {
		return _defaultGui;
	}
	public void setDefaultGui(String defaultGui) {
		this._defaultGui = defaultGui;
	}
	
	@XmlElement(name = "locked", required = true)
	@JsonProperty("locked")
	public boolean isLocked() {
		return _locked;
	}
	public void setLocked(boolean locked) {
		this._locked = locked;
	}
	
	private String _code;
	private String _widgetTypeCode;
	private String _pluginCode;
	private String _gui;
	private String _defaultGui;
	private boolean _locked;
	
}
