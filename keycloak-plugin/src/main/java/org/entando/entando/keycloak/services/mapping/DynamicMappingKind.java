package org.entando.entando.keycloak.services.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum DynamicMappingKind {

    GROUP("group"),
    ROLE("role"),
    GROUPROLE("grouprole");

    public String kind;
    DynamicMappingKind(String kind) {
        this.kind = kind;
    }

    @JsonValue
    public String getXmlValue() {
        return kind;
    }

    @JsonCreator
    public static DynamicMappingKind fromValue(String value) {
        return Arrays.stream(values())
                .filter(k -> k.kind.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown DynamicMappingKind: " + value));
    }

}
