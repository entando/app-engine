package org.entando.entando.keycloak.services.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;

public enum DynamicMappingKind {

    GROUP("group", false),
    ROLE("role", false),
    GROUPROLE("grouprole", false),
    ROLECLAIM("roleclaim", true),
    GROUPCLAIM("groupclaim", true),
    ROLEGROUPCLAIM("ROLEGROUPCLAIM", true);

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
        return Arrays.stream(values())
                .filter(k -> k.kind.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown DynamicMappingKind: " + value));
    }
}
