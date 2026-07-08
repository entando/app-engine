package org.entando.entando.keycloak.services.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum FallbackKind {
    ROLE("role"),
    GROUP("group"),
    IGNORE("ignore");

    private final String kind;

    FallbackKind(String kind) {
        this.kind = kind;
    }

    @JsonValue
    public String getXmlValue() {
        return kind;
    }

    @JsonCreator
    public static FallbackKind fromValue(String value) {
        return Arrays.stream(values())
                .filter(k -> k.kind.equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }
}
