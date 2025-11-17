package org.entando.entando.web.common.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;

public class BooleanStringDeserializer extends JsonDeserializer<Boolean> {

    private final EntLogger logger = EntLogFactory.getSanitizedLogger(getClass());

    protected static final String FALSE = "false";
    protected static final String TRUE = "true";

    @Override
    public Boolean deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonToken currentToken = jp.getCurrentToken();

        if (currentToken.equals(JsonToken.VALUE_STRING)) {
            String text = jp.getText().trim();
            if (TRUE.equalsIgnoreCase(text)) {
                return Boolean.TRUE;
            } else if (FALSE.equalsIgnoreCase(text)) {
                return Boolean.FALSE;
            } else {
                logger.warn("only {}, {} and {}, {} values are supported as boolean input", TRUE, "\"true\"", FALSE, "\"false\"");
                ;
                return null;
            }

        } else if (currentToken.equals(JsonToken.VALUE_NULL)) {
            return null;
        } else if (currentToken.equals(JsonToken.VALUE_TRUE)) {
            return Boolean.TRUE;
        } else if (currentToken.equals(JsonToken.VALUE_FALSE)) {
            return Boolean.FALSE;

        }
        // ESB-678: Updated exception handling for Jackson 2.20.0 compatibility
        // The mappingException(Class) method was deprecated and removed in newer Jackson versions.
        // Replaced with reportInputMismatch() which is the recommended approach for reporting
        // deserialization mismatches and type conversion errors in Jackson 2.15+.
        // References:
        // - Jackson 2.20.0 API documentation for DeserializationContext
        // - https://github.com/FasterXML/jackson-databind/blob/2.20/src/main/java/com/fasterxml/jackson/databind/DeserializationContext.java
        return (Boolean) ctxt.reportInputMismatch(Boolean.class, "Invalid boolean value: %s", currentToken);
    }

    @Override
    public Boolean getNullValue() {
        return null; //Boolean.FALSE;
    }

}
