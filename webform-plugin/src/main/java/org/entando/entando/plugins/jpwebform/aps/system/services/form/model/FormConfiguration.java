package org.entando.entando.plugins.jpwebform.aps.system.services.form.model;

import com.agiletec.aps.util.ApsProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FormConfiguration {

    private ApsProperties properties;

    public ApsProperties getProperties() {
        return properties;
    }

    public void setProperties(ApsProperties properties) {
        this.properties = properties;
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        return mapper.writeValueAsString(this);
    }
}
