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
package org.entando.entando.aps.system.services.category.model;

import com.agiletec.aps.system.services.category.Category;
import com.agiletec.aps.system.services.category.ICategoryManager;
import com.agiletec.aps.util.ApsProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.entando.entando.aps.system.services.component.ComponentUsageEntity;
import org.entando.entando.aps.system.services.component.IComponentDto;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author E.Santoboni
 */
public class CategoryDto implements IComponentDto {

    @NotBlank(message = "category.code.notBlank")
    private String code;

    @NotBlank(message = "category.parent.notBlank")
    private String parentCode;

    @NotEmpty(message = "category.titles.notBlank")
    private Map<String, String> titles = new HashMap<>();

    private Map<String, String> fullTitles = new HashMap<>();

    private List<String> children = new ArrayList<>();
    private Map<String, Boolean> references = new HashMap<>();

    public CategoryDto() {
    }

    public CategoryDto(Category category, ICategoryManager categoryManager) {
        this.setCode(category.getCode());
        this.setParentCode(category.getParentCode());
        Optional<ApsProperties> apsTitles = Optional.ofNullable(category.getTitles());
        apsTitles.ifPresent(values -> values.keySet().forEach((lang)
                -> {
            this.getTitles().put((String) lang, (String) values.get(lang));
            this.getFullTitles().put((String) lang, (String) category.getFullTitle((String) lang, categoryManager));
        }
        ));
        Optional.ofNullable(category.getChildrenCodes()).
                ifPresent(values -> Arrays.asList(values).forEach((child) -> this.children.add(child)));
    }

    @JsonIgnore
    @Override
    public String getType() {
        return ComponentUsageEntity.TYPE_CATEGORY;
    }
    
    @Override
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public Map<String, String> getTitles() {
        return titles;
    }

    public void setTitles(Map<String, String> titles) {
        this.titles = titles;
    }

    public void addTitle(String lang, String title) {
        this.titles.put(lang, title);
    }

    public Map<String, String> getFullTitles() {
        return fullTitles;
    }

    public void setFullTitles(Map<String, String> fullTitles) {
        this.fullTitles = fullTitles;
    }

    public List<String> getChildren() {
        return children;
    }

    public void setChildren(List<String> children) {
        this.children = children;
    }

    public void addChild(String child) {
        this.children.add(child);
    }

    public Map<String, Boolean> getReferences() {
        return references;
    }

    public void setReferences(Map<String, Boolean> references) {
        this.references = references;
    }

    public static String getEntityFieldName(String dtoFieldName) {
        switch (dtoFieldName) {
            case "code":
                return "code";
            case "name":
                return "descr";
            default:
                return dtoFieldName;
        }
    }

}
