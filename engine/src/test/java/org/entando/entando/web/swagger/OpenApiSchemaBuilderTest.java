package org.entando.entando.web.swagger;

import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for OpenApiSchemaBuilder
 */
class OpenApiSchemaBuilderTest {

    // Test DTOs
    static class SimpleDto {
        private String name;
        private Integer age;
        private Boolean active;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    static class ComplexDto {
        private List<String> tags;
        private Map<String, Integer> counts;
        private Set<String> categories;
        private SimpleDto nested;

        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public Map<String, Integer> getCounts() { return counts; }
        public void setCounts(Map<String, Integer> counts) { this.counts = counts; }
        public Set<String> getCategories() { return categories; }
        public void setCategories(Set<String> categories) { this.categories = categories; }
        public SimpleDto getNested() { return nested; }
        public void setNested(SimpleDto nested) { this.nested = nested; }
    }

    static class CircularDto {
        private String id;
        private CircularDto parent;
        private List<CircularDto> children;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public CircularDto getParent() { return parent; }
        public void setParent(CircularDto parent) { this.parent = parent; }
        public List<CircularDto> getChildren() { return children; }
        public void setChildren(List<CircularDto> children) { this.children = children; }
    }

    static class ArrayDto {
        private String[] stringArray;
        private int[] intArray;
        private SimpleDto[] objectArray;

        public String[] getStringArray() { return stringArray; }
        public void setStringArray(String[] stringArray) { this.stringArray = stringArray; }
        public int[] getIntArray() { return intArray; }
        public void setIntArray(int[] intArray) { this.intArray = intArray; }
        public SimpleDto[] getObjectArray() { return objectArray; }
        public void setObjectArray(SimpleDto[] objectArray) { this.objectArray = objectArray; }
    }

    // ========== Primitive Type Tests ==========

    @Test
    void testStringSchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(String.class);

        assertEquals("string", schema.getType());
        assertEquals("string", schema.getExample());
    }

    @Test
    void testIntegerSchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(Integer.class);

        assertEquals("integer", schema.getType());
        assertEquals("int32", schema.getFormat());
        assertEquals(0, schema.getExample());
    }

    @Test
    void testPrimitiveIntSchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(int.class);

        assertEquals("integer", schema.getType());
        assertEquals("int32", schema.getFormat());
    }

    @Test
    void testLongSchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(Long.class);

        assertEquals("integer", schema.getType());
        assertEquals("int64", schema.getFormat());
    }

    @Test
    void testBooleanSchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(Boolean.class);

        assertEquals("boolean", schema.getType());
        assertEquals(true, schema.getExample());
    }

    @Test
    void testDoubleSchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(Double.class);

        assertEquals("number", schema.getType());
        assertEquals(0.0, schema.getExample());
    }

    @Test
    void testFloatSchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(Float.class);

        assertEquals("number", schema.getType());
    }

    // ========== Date/Time Type Tests ==========

    @Test
    void testDateSchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(Date.class);

        assertEquals("string", schema.getType());
        assertEquals("date-time", schema.getFormat());
    }

    @Test
    void testLocalDateSchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(LocalDate.class);

        assertEquals("string", schema.getType());
        assertEquals("date-time", schema.getFormat());
    }

    @Test
    void testLocalDateTimeSchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(LocalDateTime.class);

        assertEquals("string", schema.getType());
        assertEquals("date-time", schema.getFormat());
    }

    @Test
    void testZonedDateTimeSchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(ZonedDateTime.class);

        assertEquals("string", schema.getType());
        assertEquals("date-time", schema.getFormat());
    }

    @Test
    void testInstantSchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(Instant.class);

        assertEquals("string", schema.getType());
        assertEquals("date-time", schema.getFormat());
    }

    // ========== Array Tests ==========

    @Test
    void testStringArraySchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(String[].class);

        assertEquals("array", schema.getType());
        assertNotNull(schema.getItems());
        assertEquals("string", schema.getItems().getType());
    }

    @Test
    void testIntArraySchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(int[].class);

        assertEquals("array", schema.getType());
        assertNotNull(schema.getItems());
        assertEquals("integer", schema.getItems().getType());
    }

    @Test
    void testObjectArraySchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(SimpleDto[].class);

        assertEquals("array", schema.getType());
        assertNotNull(schema.getItems());
        assertEquals("object", schema.getItems().getType());
    }

    // ========== Collection Tests (raw types) ==========

    @Test
    void testRawListSchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(List.class);

        assertEquals("array", schema.getType());
        assertNotNull(schema.getItems());
        assertEquals("object", schema.getItems().getType());
        assertTrue(schema.getDescription().contains("generic type information not available"));
    }

    @Test
    void testRawMapSchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(Map.class);

        assertEquals("object", schema.getType());
        assertNotNull(schema.getAdditionalProperties());
        assertTrue(schema.getDescription().contains("generic type information not available"));
    }

    // ========== Parameterized Type Tests ==========

    @Test
    void testListOfStringsSchema() throws Exception {
        Type listType = getGenericType(ComplexDto.class, "tags");
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromType(listType);

        assertEquals("array", schema.getType());
        assertNotNull(schema.getItems());
        assertEquals("string", schema.getItems().getType());
    }

    @Test
    void testMapOfStringIntegerSchema() throws Exception {
        Type mapType = getGenericType(ComplexDto.class, "counts");
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromType(mapType);

        assertEquals("object", schema.getType());
        assertNotNull(schema.getAdditionalProperties());
        assertEquals("integer", ((Schema<?>) schema.getAdditionalProperties()).getType());
    }

    @Test
    void testSetOfStringsSchema() throws Exception {
        Type setType = getGenericType(ComplexDto.class, "categories");
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromType(setType);

        assertEquals("array", schema.getType());
        assertNotNull(schema.getItems());
        assertEquals("string", schema.getItems().getType());
    }

    // ========== Custom Object Tests ==========

    @Test
    void testSimpleCustomObjectSchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(SimpleDto.class);

        assertEquals("object", schema.getType());
        assertEquals("SimpleDto", schema.getTitle());
        assertNotNull(schema.getProperties());
        assertTrue(schema.getProperties().containsKey("name"));
        assertTrue(schema.getProperties().containsKey("age"));
        assertTrue(schema.getProperties().containsKey("active"));

        assertEquals("string", ((Schema<?>) schema.getProperties().get("name")).getType());
        assertEquals("integer", ((Schema<?>) schema.getProperties().get("age")).getType());
        assertEquals("boolean", ((Schema<?>) schema.getProperties().get("active")).getType());
    }

    @Test
    void testComplexNestedObjectSchema() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(ComplexDto.class);

        assertEquals("object", schema.getType());
        assertNotNull(schema.getProperties());

        // Check list property
        Schema<?> tagsSchema = (Schema<?>) schema.getProperties().get("tags");
        assertEquals("array", tagsSchema.getType());

        // Check map property
        Schema<?> countsSchema = (Schema<?>) schema.getProperties().get("counts");
        assertEquals("object", countsSchema.getType());

        // Check set property
        Schema<?> categoriesSchema = (Schema<?>) schema.getProperties().get("categories");
        assertEquals("array", categoriesSchema.getType());

        // Check nested object
        Schema<?> nestedSchema = (Schema<?>) schema.getProperties().get("nested");
        assertEquals("object", nestedSchema.getType());
    }

    @Test
    void testCircularReferenceHandling() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(CircularDto.class);

        assertEquals("object", schema.getType());
        assertNotNull(schema.getProperties());

        // Circular reference should be detected and handled
        Schema<?> parentSchema = (Schema<?>) schema.getProperties().get("parent");
        assertNotNull(parentSchema);
        assertTrue(parentSchema.getDescription() != null &&
                   parentSchema.getDescription().contains("circular reference"));
    }

    @Test
    void testArrayFieldsInObject() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(ArrayDto.class);

        assertEquals("object", schema.getType());
        assertNotNull(schema.getProperties());

        // String array
        Schema<?> stringArraySchema = (Schema<?>) schema.getProperties().get("stringArray");
        assertEquals("array", stringArraySchema.getType());
        assertEquals("string", stringArraySchema.getItems().getType());

        // int array
        Schema<?> intArraySchema = (Schema<?>) schema.getProperties().get("intArray");
        assertEquals("array", intArraySchema.getType());
        assertEquals("integer", intArraySchema.getItems().getType());

        // Object array
        Schema<?> objectArraySchema = (Schema<?>) schema.getProperties().get("objectArray");
        assertEquals("array", objectArraySchema.getType());
        assertEquals("object", objectArraySchema.getItems().getType());
    }

    // ========== Example Value Tests ==========

    @Test
    void testSchemaContainsExample() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(SimpleDto.class);

        assertNotNull(schema.getExample());
        assertTrue(schema.getExample() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> example = (Map<String, Object>) schema.getExample();
        assertTrue(example.containsKey("name"));
        assertTrue(example.containsKey("age"));
        assertTrue(example.containsKey("active"));
    }

    @Test
    void testExampleValuesTypes() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(SimpleDto.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> example = (Map<String, Object>) schema.getExample();

        assertTrue(example.get("name") instanceof String);
        assertTrue(example.get("age") instanceof Integer);
        assertTrue(example.get("active") instanceof Boolean);
    }

    // ========== Edge Cases ==========

    @Test
    void testNullType() {
        assertThrows(NullPointerException.class, () ->
            OpenApiSchemaBuilder.createSchemaFromClass(null)
        );
    }

    @Test
    void testObjectClass() {
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(Object.class);

        assertEquals("object", schema.getType());
    }

    @Test
    void testDepthLimit() {
        // Create a deeply nested structure
        Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromClass(CircularDto.class);

        // Schema should be created without stack overflow
        assertNotNull(schema);
        assertEquals("object", schema.getType());
    }

    // ========== Helper Methods ==========

    private Type getGenericType(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        return clazz.getDeclaredField(fieldName).getGenericType();
    }
}