package org.entando.entando.keycloak.services.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

public enum DynamicMappingKind {

    GROUP("group", false),
    ROLE("role", false),
    ROLEGROUP("rolegroup", false),
    ROLECLAIM("roleclaim", true),
    GROUPCLAIM("groupclaim", true),
    ROLEGROUPCLAIM("rolegroupclaim", true);

    private final String kind;
    @Getter
    private final boolean jwtMapping;

    DynamicMappingKind(String kind, boolean jwtmapping) {
        this.kind = kind;
        this.jwtMapping = jwtmapping;
    }

    @JsonValue
    public String getXmlValue() {
        return kind;
    }

    @JsonCreator
    public static DynamicMappingKind fromValue(String value) {
        final String trimmed = StringUtils.trim(value);
        return Arrays.stream(values())
                .filter(k -> k.kind.equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown DynamicMappingKind: " + value));
    }
}
