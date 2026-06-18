package org.entando.entando.keycloak.services.mapping;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum SingleTokenFallback {

    ROLE("role"),
    GROUP("group"),
    IGNORE("ignore");

    private final String value;

    SingleTokenFallback(String value) {
        this.value = value;
    }

    @JsonValue
    public String getXmlValue() {
        return value;
    }

    @JsonCreator
    public static SingleTokenFallback fromValue(String value) {
        return Arrays.stream(values())
                .filter(f -> f.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown SingleTokenFallback: " + value));
    }
}
