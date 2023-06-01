package org.entando.entando.web.component;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Map;
import java.util.TreeMap;
import org.entando.entando.aps.system.services.IComponentDto;

public class ComponentUsageEntity {

    public static final String TYPE_PAGE = "page";
    public static final String TYPE_WIDGET = "widget";
    public static final String TYPE_FRAGMENT = "fragment";
    public static final String TYPE_PAGE_TEMPLATE = "pageTemplate";
    public static final String TYPE_CONTENT = "content";
    
    private String type;
    private String code;
    private String status;
    
    private Map<String, Object> extraProperties = new TreeMap<>();

    public ComponentUsageEntity() {
    }

    public ComponentUsageEntity(String type, String code) {
        this.type = type;
        this.code = code;
    }

    public ComponentUsageEntity(String type, String code, String status) {
        this(type, code);
        this.status = status;
    }

    public ComponentUsageEntity(String type, IComponentDto dto) {
        this(type, dto.getCode(), dto.getStatus());
        this.extraProperties.putAll(dto.getExtraProperties());
    }

    public String getType() {
        return type;
    }

    public ComponentUsageEntity setType(String type) {
        this.type = type;
        return this;
    }

    public String getCode() {
        return code;
    }

    public ComponentUsageEntity setCode(String code) {
        this.code = code;
        return this;
    }

    @JsonInclude(Include.NON_NULL)
    public String getStatus() {
        return status;
    }

    public ComponentUsageEntity setStatus(String status) {
        this.status = status;
        return this;
    }

    public ComponentUsageEntity addExtraProperties(Map<String, Object> extraProperties) {
        this.extraProperties.putAll(extraProperties);
        return this;
    }
    
    @JsonAnyGetter
    public Map<String, Object> getExtraProperties() {
        return this.extraProperties;
    }
    
}
