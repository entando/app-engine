package org.entando.entando.aps.system.services.jsonpatch;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.entando.entando.aps.system.services.jsonpatch.validator.JsonPatchValidator;
import org.springframework.data.rest.webmvc.json.patch.BindContext;
import org.springframework.data.rest.webmvc.json.patch.JsonPatchPatchConverter;
import org.springframework.data.rest.webmvc.json.patch.Patch;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class JsonPatchService<T> {

    private final Class<T> referenceClass;
    private final JsonPatchPatchConverter converter;
    private final JsonPatchValidator validator;

    public JsonPatchService(Class<T> referenceClass) {
        this.referenceClass = referenceClass;
        // ESB-678: Updated JsonPatchPatchConverter constructor for Spring Data REST 4.5.3 compatibility
        // The constructor signature changed to require both ObjectMapper and BindContext parameters.
        // This ensures JSON patch operations continue to work with the upgraded Spring Data REST version.
        // References:
        // - https://github.com/spring-projects/spring-data-rest/blob/main/spring-data-rest-webmvc/src/main/java/org/springframework/data/rest/webmvc/json/patch/JsonPatchPatchConverter.java
        // - Spring Data REST 4.5.3 migration guide
        this.converter = new JsonPatchPatchConverter(new ObjectMapper(), createSimpleBindContext());
        this.validator = new JsonPatchValidator(this.converter);
    }
    
    // ESB-678: BindContext implementation for Spring Data REST 4.5.3+ compatibility
    // Provides the required context for JSON patch operations including property access resolution
    // and SpEL expression evaluation. This implementation allows all properties to be accessed
    // and provides a standard evaluation context for expression processing.
    // References:
    // - org.springframework.data.rest.webmvc.json.patch.BindContext documentation
    // - Spring Expression Language (SpEL) reference documentation
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

    public JsonPatchPatchConverter getConverter() { return converter; }

    public JsonPatchValidator getValidator() { return validator; }

    public Class<T> getReferenceClass() { return referenceClass; }

    public T applyPatch(JsonNode patch, T source) {

        this.getValidator().validatePatch(patch);

        Patch springPatch = this.getConverter().convert(patch);
        return springPatch.apply(source, referenceClass );

    }


}
