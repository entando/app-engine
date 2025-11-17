package org.entando.entando.plugins.jacms.web.contentsettings.model;

import jakarta.validation.constraints.NotNull;

public class ContentSettingsEditorRequest {

    @NotNull
    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
