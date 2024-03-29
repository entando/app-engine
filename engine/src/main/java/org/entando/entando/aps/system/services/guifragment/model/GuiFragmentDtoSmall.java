/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.system.services.guifragment.model;

import com.agiletec.aps.system.services.lang.ILangManager;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.guifragment.GuiFragment;
import org.entando.entando.aps.system.services.widgettype.WidgetType;

import java.util.Optional;
import org.entando.entando.aps.system.services.component.ComponentUsageEntity;
import org.entando.entando.aps.system.services.component.IComponentDto;

/**
 * @author E.Santoboni
 */
public class GuiFragmentDtoSmall implements IComponentDto {

    private String code;
    private boolean locked;
    private WidgetTypeRef widgetType = null;
    private String pluginCode;

    public GuiFragmentDtoSmall() {
    }

    public GuiFragmentDtoSmall(GuiFragment guiFragment, WidgetType type, ILangManager langManager) {
        this.setCode(guiFragment.getCode());
        this.setLocked(guiFragment.isLocked());
        if (!StringUtils.isEmpty(guiFragment.getWidgetTypeCode())) {
            WidgetTypeRef widgetType = new WidgetTypeRef(type.getCode(),
                    (String) type.getTitles().get(langManager.getDefaultLang().getCode()));
            this.setWidgetType(widgetType);
        }
        if (!StringUtils.isEmpty(guiFragment.getPluginCode())) {
            this.setPluginCode(guiFragment.getPluginCode());
        }
    }

    @JsonIgnore
    @Override
    public String getType() {
        return ComponentUsageEntity.TYPE_FRAGMENT;
    }
    
    @Override
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public WidgetTypeRef getWidgetType() {
        return widgetType;
    }

    public void setWidgetType(WidgetTypeRef widgetType) {
        this.widgetType = widgetType;
    }

    public String getWidgetTypeCode() {
        return Optional.ofNullable(widgetType).map(WidgetTypeRef::getCode).orElse("");
    }

    public String getPluginCode() {
        return pluginCode;
    }

    public void setPluginCode(String pluginCode) {
        this.pluginCode = pluginCode;
    }

    protected class WidgetTypeRef {

        private String code;
        private String title;

        public WidgetTypeRef(String code, String title) {
            this.setCode(code);
            this.setTitle(title);
        }

        private WidgetTypeRef() {
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

}
