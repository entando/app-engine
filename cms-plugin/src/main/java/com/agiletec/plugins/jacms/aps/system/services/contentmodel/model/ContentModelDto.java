package com.agiletec.plugins.jacms.aps.system.services.contentmodel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.entando.entando.aps.system.services.component.ComponentUsageEntity;
import org.entando.entando.aps.system.services.component.IComponentDto;
import org.springframework.validation.annotation.Validated;

@Validated
@ApiModel("ContentModel")
public class ContentModelDto implements IComponentDto {

    @Min(value = 0)
    @Max(value = 2147483647)
    @JsonProperty("id")
    @ApiModelProperty(required = true)
    private Long id;
    
    @Size(min = 3, max = 3, message = "string.size.invalid")
    @JsonProperty("contentType")
    @ApiModelProperty(required = true, value = "it must have exactly 3 characters")
    private String contentType;
    
    @NotNull
    @Size(max = 50, message = "string.size.invalid")
    @JsonProperty("descr")
    @ApiModelProperty(required = true, value = "description")
    private String descr;

    @NotNull
    @JsonProperty("contentShape")
    private String contentShape;
    
    @JsonProperty("stylesheet")
    private String stylesheet;

    @JsonIgnore
    @Override
    public String getType() {
        return ComponentUsageEntity.TYPE_CONTENT_TEMPLATE;
    }

    @JsonIgnore
    @Override
    public String getCode() {
        return String.valueOf(this.getId());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    public String getContentShape() {
        return contentShape;
    }

    public void setContentShape(String contentShape) {
        this.contentShape = contentShape;
    }

    public String getStylesheet() {
        return stylesheet;
    }

    public void setStylesheet(String stylesheet) {
        this.stylesheet = stylesheet;
    }
    
}
