package org.entando.entando.plugins.jpwebform.aps.system.services.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.model.FormData;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;

public class
DtoHelper {

    public static FormData toFormData(String json) throws IOException {
        if (StringUtils.isNotBlank(json)) {
            ObjectMapper mapper = new ObjectMapper();

            return mapper.readValue(json, FormData.class);
        }
        return null;
    }

    public static Form toForm(String json) throws IOException {
        if (StringUtils.isNotBlank(json)) {
            ObjectMapper mapper = new ObjectMapper();

            return mapper.readValue(json, Form.class);
        }
        return null;
    }
}
