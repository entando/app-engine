package org.entando.entando.keycloak.services.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum PersistKind {
    NONE("none"),
    // create the group (or empty role) but do NOT persist the user association
    AUTH("auth"),
    // create the group (or empty role) and persist the association with the logging-in user
    FULL("full");

    private final String kind;

    PersistKind(String kind) {
        this.kind = kind;
    }

    @JsonValue
    public String getXmlValue() {
        return kind;
    }

    @JsonCreator
    public static PersistKind fromValue(String value) {
        return Arrays.stream(values())
                .filter(k -> k.kind.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown PersistKind: " + value));
    }
}
