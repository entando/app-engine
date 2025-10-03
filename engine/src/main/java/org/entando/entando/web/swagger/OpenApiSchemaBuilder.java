package org.entando.entando.web.swagger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.models.media.Schema;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for building OpenAPI schemas from Java classes using reflection
 */
public class OpenApiSchemaBuilder {

    /**
     * Maximum depth for nested object introspection to prevent stack overflow
     * while still providing meaningful schema details
     */
    private static final int MAX_DEPTH = 5;

    /**
     * Create an OpenAPI schema from a Java class, including field introspection
     */
    public static Schema<?> createSchemaFromClass(Class<?> clazz) {
        return createSchemaFromClass(clazz, new HashSet<>(), 0);
    }

    /**
     * Create an OpenAPI schema from a Type (which may include generic information like List&lt;ContentDto&gt;)
     * This is useful when you have generic type information available from reflection
     */
    public static Schema<?> createSchemaFromType(Type type) {
        return createSchemaFromType(type, new HashSet<>(), 0);
    }

    /**
     * Internal method to create schema from Type with depth and circular reference tracking
     */
    private static Schema<?> createSchemaFromType(Type type, Set<Class<?>> visited, int depth) {
        if (type instanceof Class) {
            return createSchemaFromClass((Class<?>) type, visited, depth);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Class<?> rawType = (Class<?>) paramType.getRawType();

            // Handle List<T> and Set<T> - they both become arrays in OpenAPI
            if (isCollectionType(rawType)) {
                return createArraySchema(paramType, visited, depth);
            }

            // Handle Map<K, V>
            if (java.util.Map.class.isAssignableFrom(rawType)) {
                return createMapSchema(paramType, visited, depth);
            }

            // For other parameterized types, introspect with generic type information
            return createSchemaFromParameterizedType(paramType, visited, depth);
        }

        // Fallback for unknown types
        return new Schema<>().type("object");
    }

    /**
     * Check if a class is a collection type (List or Set)
     */
    private static boolean isCollectionType(Class<?> clazz) {
        return java.util.List.class.isAssignableFrom(clazz) ||
               java.util.Set.class.isAssignableFrom(clazz);
    }

    /**
     * Create array schema for List<T> or Set<T>
     */
    private static Schema<?> createArraySchema(ParameterizedType paramType, Set<Class<?>> visited, int depth) {
        Type[] typeArgs = paramType.getActualTypeArguments();
        if (typeArgs.length > 0) {
            Schema<?> schema = new Schema<>();
            schema.setType("array");
            schema.setItems(createSchemaFromType(typeArgs[0], visited, depth + 1));
            return schema;
        }
        // Fallback if no type arguments
        return new Schema<>().type("array").items(new Schema<>().type("object"));
    }

    /**
     * Create object schema for Map<K, V>
     */
    private static Schema<?> createMapSchema(ParameterizedType paramType, Set<Class<?>> visited, int depth) {
        Type[] typeArgs = paramType.getActualTypeArguments();
        if (typeArgs.length > 1) {
            Schema<?> schema = new Schema<>();
            schema.setType("object");
            schema.additionalProperties(createSchemaFromType(typeArgs[1], visited, depth + 1));
            return schema;
        }
        // Fallback if insufficient type arguments
        return new Schema<>().type("object").additionalProperties(new Schema<>().type("string"));
    }

    /**
     * Create schema from a ParameterizedType, preserving generic type information
     */
    private static Schema<?> createSchemaFromParameterizedType(ParameterizedType paramType, Set<Class<?>> visited, int depth) {
        Class<?> rawType = (Class<?>) paramType.getRawType();
        Schema<?> schema = new Schema<>();
        schema.setType("object");

        // Check for circular references
        if (visited.contains(rawType)) {
            schema.setDescription(rawType.getSimpleName() + " (circular reference)");
            return schema;
        }

        // Check depth limit
        if (depth >= MAX_DEPTH) {
            schema.setDescription(rawType.getSimpleName() + " (max depth reached)");
            return schema;
        }

        schema.setTitle(rawType.getSimpleName());
        visited.add(rawType);
        introspectObjectFieldsWithGenerics(rawType, paramType, schema, visited, depth);
        visited.remove(rawType);
        return schema;
    }

    /**
     * Introspect object fields with generic type resolution
     */
    private static void introspectObjectFieldsWithGenerics(Class<?> clazz, ParameterizedType paramType, Schema<?> schema, Set<Class<?>> visited, int depth) {
        Map<String, Schema> properties = new LinkedHashMap<>();
        Map<String, Object> exampleObject = new LinkedHashMap<>();

        // Build type variable to actual type mapping for the entire class hierarchy
        Map<String, Type> typeMap = buildTypeMap(clazz, paramType);

        // Get all fields including inherited ones
        Class<?> currentClass = clazz;
        while (currentClass != null && !currentClass.equals(Object.class)) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (shouldSkipField(field)) {
                    continue;
                }

                String fieldName = field.getName();
                Type genericFieldType = field.getGenericType();

                // Resolve generic type if it's a type variable
                if (genericFieldType instanceof java.lang.reflect.TypeVariable) {
                    java.lang.reflect.TypeVariable<?> typeVar = (java.lang.reflect.TypeVariable<?>) genericFieldType;
                    Type resolvedType = typeMap.get(typeVar.getName());
                    if (resolvedType != null) {
                        genericFieldType = resolvedType;
                    }
                }

                // Create property schema based on field type
                Schema<?> propertySchema = createSchemaFromType(genericFieldType, visited, depth + 1);
                properties.put(fieldName, propertySchema);

                // Add example value
                exampleObject.put(fieldName, getExampleValueFromType(genericFieldType, visited, depth + 1));
            }
            currentClass = currentClass.getSuperclass();
        }

        schema.setProperties(properties);
        if (!exampleObject.isEmpty()) {
            schema.example(exampleObject);
        }
    }

    /**
     * Check if a field should be skipped during introspection
     */
    private static boolean shouldSkipField(Field field) {
        return Modifier.isStatic(field.getModifiers()) ||
               field.isSynthetic() ||
               field.isAnnotationPresent(JsonIgnore.class) ||
               field.getType().getName().contains("Logger");
    }

    /**
     * Build a complete type variable mapping by traversing the class hierarchy
     */
    private static Map<String, Type> buildTypeMap(Class<?> clazz, ParameterizedType paramType) {
        Map<String, Type> typeMap = new LinkedHashMap<>();

        // Start with the current class's type parameters
        java.lang.reflect.TypeVariable<?>[] typeVars = clazz.getTypeParameters();
        Type[] actualTypes = paramType.getActualTypeArguments();
        for (int i = 0; i < Math.min(typeVars.length, actualTypes.length); i++) {
            typeMap.put(typeVars[i].getName(), actualTypes[i]);
        }

        // Resolve parent class generics
        Type genericSuperclass = clazz.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType parentParamType = (ParameterizedType) genericSuperclass;
            Class<?> parentClass = (Class<?>) parentParamType.getRawType();
            java.lang.reflect.TypeVariable<?>[] parentTypeVars = parentClass.getTypeParameters();
            Type[] parentActualTypes = parentParamType.getActualTypeArguments();

            for (int i = 0; i < Math.min(parentTypeVars.length, parentActualTypes.length); i++) {
                Type parentActualType = parentActualTypes[i];
                Type resolvedParentType = resolveTypeVariables(parentActualType, typeMap);
                typeMap.put(parentTypeVars[i].getName(), resolvedParentType);
            }
        }

        return typeMap;
    }

    /**
     * Recursively resolve type variables within a Type using the provided type map
     */
    private static Type resolveTypeVariables(Type type, Map<String, Type> typeMap) {
        if (type instanceof java.lang.reflect.TypeVariable) {
            java.lang.reflect.TypeVariable<?> typeVar = (java.lang.reflect.TypeVariable<?>) type;
            Type resolved = typeMap.get(typeVar.getName());
            return resolved != null ? resolved : type;
        } else if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Type[] typeArgs = paramType.getActualTypeArguments();
            Type[] resolvedArgs = new Type[typeArgs.length];
            for (int i = 0; i < typeArgs.length; i++) {
                resolvedArgs[i] = resolveTypeVariables(typeArgs[i], typeMap);
            }
            // Create a new ParameterizedType with resolved arguments
            return new ParameterizedTypeImpl(paramType.getRawType(), resolvedArgs, paramType.getOwnerType());
        }
        return type;
    }

    /**
     * Simple ParameterizedType implementation for creating resolved types
     */
    private static class ParameterizedTypeImpl implements ParameterizedType {
        private final Type rawType;
        private final Type[] actualTypeArguments;
        private final Type ownerType;

        public ParameterizedTypeImpl(Type rawType, Type[] actualTypeArguments, Type ownerType) {
            this.rawType = rawType;
            this.actualTypeArguments = actualTypeArguments;
            this.ownerType = ownerType;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return actualTypeArguments;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return ownerType;
        }
    }

    /**
     * Get example value from Type (handles both Class and ParameterizedType)
     */
    private static Object getExampleValueFromType(Type type, Set<Class<?>> visited, int depth) {
        if (type instanceof Class) {
            return getExampleValue((Class<?>) type, type, visited, depth);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) type;
            Class<?> rawType = (Class<?>) paramType.getRawType();
            return getExampleValue(rawType, type, visited, depth);
        }
        return null;
    }


    /**
     * Create an OpenAPI schema from a Java class with circular reference protection and depth limiting
     * @param clazz The class to create schema for
     * @param visited Set of classes already being processed (for circular reference detection)
     * @param depth Current recursion depth
     */
    private static Schema<?> createSchemaFromClass(Class<?> clazz, Set<Class<?>> visited, int depth) {
        Schema<?> schema = new Schema<>();

        // Handle primitive and wrapper types
        if (clazz.equals(String.class)) {
            return schema.type("string").example("string");
        } else if (clazz.equals(Integer.class) || clazz.equals(int.class)) {
            return schema.type("integer").format("int32").example(0);
        } else if (clazz.equals(Long.class) || clazz.equals(long.class)) {
            return schema.type("integer").format("int64").example(0);
        } else if (clazz.equals(Boolean.class) || clazz.equals(boolean.class)) {
            return schema.type("boolean").example(true);
        } else if (clazz.equals(Double.class) || clazz.equals(double.class) ||
                   clazz.equals(Float.class) || clazz.equals(float.class)) {
            return schema.type("number").example(0.0);
        } else if (clazz.equals(java.util.Date.class) ||
                   clazz.equals(java.time.LocalDate.class) ||
                   clazz.equals(java.time.LocalDateTime.class) ||
                   clazz.equals(java.time.ZonedDateTime.class) ||
                   clazz.equals(java.time.Instant.class)) {
            return schema.type("string").format("date-time").example("2025-10-02T13:37:23.346Z");
        } else if (clazz.isArray()) {
            // Handle Java arrays
            Class<?> componentType = clazz.getComponentType();
            return schema.type("array").items(createSchemaFromClass(componentType, visited, depth + 1));
        } else if (isCollectionType(clazz)) {
            // Collection without generic type info - fallback
            return schema.type("array")
                .items(new Schema<>().type("object"))
                .description("Array of objects (generic type information not available)");
        } else if (java.util.Map.class.isAssignableFrom(clazz)) {
            // Map without generic type info - fallback
            return schema.type("object")
                .additionalProperties(new Schema<>().type("string"))
                .description("Map with string values (generic type information not available)");
        } else if (!clazz.getName().startsWith("java.") && !clazz.isPrimitive()) {
            // Custom object types
            if (visited.contains(clazz)) {
                return schema.type("object").description(clazz.getSimpleName() + " (circular reference)");
            }

            if (depth >= MAX_DEPTH) {
                return schema.type("object").description(clazz.getSimpleName() + " (max depth reached)");
            }

            // For custom objects, introspect fields to create detailed schema
            schema.setType("object");
            schema.setTitle(clazz.getSimpleName());
            visited.add(clazz);
            introspectObjectFields(clazz, schema, visited, depth);
            visited.remove(clazz);
            return schema;
        } else {
            // Fallback for unknown types
            return schema.type("object");
        }
    }

    /**
     * Introspect object fields and populate schema properties and examples
     */
    private static void introspectObjectFields(Class<?> clazz, Schema<?> schema, Set<Class<?>> visited, int depth) {
        Map<String, Schema> properties = new LinkedHashMap<>();
        Map<String, Object> exampleObject = new LinkedHashMap<>();

        // Get all fields including inherited ones
        Class<?> currentClass = clazz;
        while (currentClass != null && !currentClass.equals(Object.class)) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (shouldSkipField(field)) {
                    continue;
                }

                String fieldName = field.getName();
                Type genericType = field.getGenericType();

                // Use createSchemaFromType for consistent schema creation
                Schema<?> propertySchema = createSchemaFromType(genericType, visited, depth + 1);
                properties.put(fieldName, propertySchema);

                // Add example value
                exampleObject.put(fieldName, getExampleValueFromType(genericType, visited, depth + 1));
            }
            currentClass = currentClass.getSuperclass();
        }

        schema.setProperties(properties);
        if (!exampleObject.isEmpty()) {
            schema.example(exampleObject);
        }
    }

    /**
     * Get example value for a field type
     */
    private static Object getExampleValue(Class<?> fieldType, Type genericType) {
        return getExampleValue(fieldType, genericType, new HashSet<>(), 0);
    }

    /**
     * Internal method to get example value with depth and circular reference tracking
     */
    private static Object getExampleValue(Class<?> fieldType, Type genericType, Set<Class<?>> visited, int depth) {
        if (fieldType.equals(String.class)) {
            return "string";
        } else if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
            return 0;
        } else if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
            return 0L;
        } else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
            return true;
        } else if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
            return 0.0;
        } else if (fieldType.equals(Float.class) || fieldType.equals(float.class)) {
            return 0.0f;
        } else if (java.util.Date.class.isAssignableFrom(fieldType) ||
                   java.time.LocalDate.class.isAssignableFrom(fieldType) ||
                   java.time.LocalDateTime.class.isAssignableFrom(fieldType)) {
            return "2025-10-02T13:37:23.346Z";
        } else if (isCollectionType(fieldType)) {
            // Handle both List and Set
            return createCollectionExample(genericType, visited, depth);
        } else if (java.util.Map.class.isAssignableFrom(fieldType)) {
            return createMapExample(genericType, visited, depth);
        } else if (!fieldType.getName().startsWith("java.") && !fieldType.isPrimitive()
                   && !fieldType.equals(Object.class)) {
            return createCustomObjectExample(fieldType, visited, depth);
        } else {
            return null;
        }
    }

    /**
     * Create example for collection types (List/Set)
     */
    private static java.util.List<Object> createCollectionExample(Type genericType, Set<Class<?>> visited, int depth) {
        java.util.List<Object> list = new java.util.ArrayList<>();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericType;
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0) {
                Type itemType = typeArgs[0];
                if (itemType instanceof Class) {
                    list.add(getExampleValue((Class<?>) itemType, itemType, visited, depth + 1));
                } else if (itemType instanceof ParameterizedType) {
                    Class<?> rawItemType = (Class<?>) ((ParameterizedType) itemType).getRawType();
                    list.add(getExampleValue(rawItemType, itemType, visited, depth + 1));
                } else {
                    list.add("string");
                }
            } else {
                list.add("string");
            }
        } else {
            list.add("string");
        }
        return list;
    }

    /**
     * Create example for Map types
     */
    private static Map<String, Object> createMapExample(Type genericType, Set<Class<?>> visited, int depth) {
        Map<String, Object> map = new LinkedHashMap<>();
        Object exampleValue = "string";

        if (genericType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericType;
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 1 && typeArgs[1] instanceof Class) {
                Class<?> valueType = (Class<?>) typeArgs[1];
                exampleValue = getExampleValue(valueType, valueType, visited, depth + 1);
            }
        }

        map.put("additionalProp1", exampleValue);
        map.put("additionalProp2", exampleValue);
        map.put("additionalProp3", exampleValue);
        return map;
    }

    /**
     * Create example for custom object types
     */
    private static Map<String, Object> createCustomObjectExample(Class<?> fieldType, Set<Class<?>> visited, int depth) {
        // Check for circular references
        if (visited.contains(fieldType)) {
            return null; // Avoid circular reference in examples
        }

        // Check depth limit
        if (depth >= MAX_DEPTH) {
            return null; // Avoid too deep nesting
        }

        // Build example object from fields
        Map<String, Object> exampleObject = new LinkedHashMap<>();
        visited.add(fieldType);

        Class<?> currentClass = fieldType;
        while (currentClass != null && !currentClass.equals(Object.class)) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (shouldSkipField(field)) {
                    continue;
                }

                String fieldName = field.getName();
                Class<?> fieldClass = field.getType();
                Type fieldGenericType = field.getGenericType();
                exampleObject.put(fieldName, getExampleValue(fieldClass, fieldGenericType, visited, depth + 1));
            }
            currentClass = currentClass.getSuperclass();
        }

        visited.remove(fieldType);
        return exampleObject;
    }
}