package org.entando.entando.aps.system.services.jsonpatch.validator;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableSet;
import org.springframework.data.rest.webmvc.json.patch.BindContext;
import org.springframework.data.rest.webmvc.json.patch.JsonPatchPatchConverter;
import org.springframework.data.rest.webmvc.json.patch.PatchException;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

@Service
public class JsonPatchValidator {

    private final Set<String> ENTANDO_SUPPORTED_OPERATIONS = ImmutableSet.of("replace");

    private final JsonPatchPatchConverter converter;

    public JsonPatchValidator() {
        // ESB-678: Added BindContext parameter to JsonPatchPatchConverter constructor
        // Spring Data REST 4.5.3 changed JsonPatchPatchConverter constructor signature to require
        // both ObjectMapper and BindContext parameters instead of just ObjectMapper alone.
        // This change maintains backward compatibility by providing a simple BindContext implementation.
        // References:
        // - https://github.com/spring-projects/spring-data-rest/blob/main/spring-data-rest-webmvc/src/main/java/org/springframework/data/rest/webmvc/json/patch/JsonPatchPatchConverter.java
        // - Spring Data REST 4.5.3 API documentation
        this.converter = new JsonPatchPatchConverter(new ObjectMapper(), createSimpleBindContext());
    }
    
    // ESB-678: Simple BindContext implementation for JsonPatchPatchConverter compatibility
    // Provides basic property access and evaluation context required by Spring Data REST 4.5.3+
    // Returns segment names as-is for both readable and writable properties, and provides
    // a standard SpEL evaluation context for expression evaluation.
    // References:
    // - org.springframework.data.rest.webmvc.json.patch.BindContext interface
    // - Spring Framework SpEL documentation
    private static BindContext createSimpleBindContext() {
        return new BindContext() {
            @Override
            public Optional<String> getWritableProperty(String segment, Class<?> type) {
                return Optional.of(segment);
            }
            
            @Override
            public Optional<String> getReadableProperty(String segment, Class<?> type) {
                return Optional.of(segment);
            }
            
            @Override
            public org.springframework.expression.EvaluationContext getEvaluationContext() {
                return new StandardEvaluationContext();
            }
        };
    }

    public JsonPatchValidator(JsonPatchPatchConverter converter) {
        this.converter = converter;
    }

    /**
     * Validate the provided jsonNode using Entando criteria
     * @param jsonNode
     */
    public void validatePatch(JsonNode jsonNode) {

        // Test if the json node is generically convertible to a Patch
        this.converter.convert(jsonNode);

        // Check if the operations are supported, can't access Spring PatchOperations as they are protected
        ArrayNode opNodes = (ArrayNode) jsonNode;

        for (Iterator<JsonNode> elements = opNodes.elements(); elements.hasNext(); ) {

            JsonNode opNode = elements.next();

            String opType = opNode.get("op").textValue();
            if (!ENTANDO_SUPPORTED_OPERATIONS.contains(opType)) {
                throw new PatchException("Not supported operation type: " + opType);
            }
        }
    }

}
