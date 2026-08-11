package org.entando.entando.keycloak.services.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

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
        final String trimmed = StringUtils.trim(value);
        return Arrays.stream(values())
                .filter(k -> k.kind.equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown PersistKind: " + value));
    }
}
