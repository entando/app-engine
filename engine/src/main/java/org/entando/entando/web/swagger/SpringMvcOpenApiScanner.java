package org.entando.entando.web.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scans Spring MVC controllers and generates OpenAPI documentation from @RequestMapping annotations
 */
@Component
@EnableWebMvc
public class SpringMvcOpenApiScanner {

    private static final EntLogger logger = EntLogFactory.getSanitizedLogger(SpringMvcOpenApiScanner.class);

    private static final String API_PREFIX = "/api";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private OpenAPI openAPI;

    @PostConstruct
    public void scanControllers() {
        logger.info("Scanning Spring MVC controllers for OpenAPI documentation...");

        //Get all @RequestMapping and @HttpExchange annotations in @Controller classes
        RequestMappingHandlerMapping mapping = applicationContext.getBean("requestMappingHandlerMapping",RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();

        AtomicInteger count = new AtomicInteger();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            // Get path patterns
            if (mappingInfo.getPathPatternsCondition() != null &&
                !mappingInfo.getPathPatternsCondition().getPatterns().isEmpty()) {
                mappingInfo.getPathPatternsCondition().getPatterns()
                        .stream().sorted()
                        .forEach(pattern -> {
                    String path = pattern.getPatternString();
                    // Document all REST controller endpoints
                    addPathToOpenAPI(path, mappingInfo, handlerMethod);
                    count.getAndIncrement();
                });
            }
        }

        logger.info("Scanned {} endpoints and added to OpenAPI documentation", count);
    }

    private void addPathToOpenAPI(String path, RequestMappingInfo mappingInfo, HandlerMethod handlerMethod) {
        // Add /api prefix to all paths
        String apiPath = API_PREFIX + path;

        PathItem pathItem = openAPI.getPaths() != null && openAPI.getPaths().get(apiPath) != null
                ? openAPI.getPaths().get(apiPath)
                : new PathItem();

        Method method = handlerMethod.getMethod();
        String methodName = method.getName();
        String className = method.getDeclaringClass().getSimpleName();

        // Get HTTP methods
        if (mappingInfo.getMethodsCondition().getMethods().isEmpty()) {
            // If no method specified, assume GET
            addOperation(pathItem, "GET", methodName, className, handlerMethod);
            logger.debug("Assuming GET method for {}#{}", className, methodName);
        } else {
            mappingInfo.getMethodsCondition().getMethods().forEach(httpMethod -> {
                addOperation(pathItem, httpMethod.name(), methodName, className, handlerMethod);
            });
        }

        //Add generated docs for path to openAPI
        if (openAPI.getPaths() == null) {
            openAPI.paths(new io.swagger.v3.oas.models.Paths());
        }
        openAPI.getPaths().addPathItem(apiPath, pathItem);
    }

    private void addOperation(PathItem pathItem, String httpMethod, String methodName,
                              String className, HandlerMethod handlerMethod) {
        Operation operation = new Operation();

        // Set operation ID and summary
        // TODO infer information from the method's semantics
        operation.setOperationId(methodName);
        operation.setSummary(className + "." + methodName);
        operation.setDescription("Endpoint: " + className + "#" + methodName);

        // Add tag for grouping by controller
        operation.addTagsItem(className);

        // Extract responses with return types
        extractResponses(operation, handlerMethod, httpMethod);

        // Extract parameters from method annotations
        extractParameters(operation, handlerMethod);

        // Add operation to path based on HTTP method
        switch (httpMethod.toUpperCase()) {
            case "GET":
                pathItem.setGet(operation);
                break;
            case "POST":
                pathItem.setPost(operation);
                break;
            case "PUT":
                pathItem.setPut(operation);
                break;
            case "DELETE":
                pathItem.setDelete(operation);
                break;
            case "PATCH":
                pathItem.setPatch(operation);
                break;
            case "HEAD":
                pathItem.setHead(operation);
                break;
            case "OPTIONS":
                pathItem.setOptions(operation);
                break;
        }
    }

    /**
     * Extract Response and populate Operation object
     * @param operation
     * @param handlerMethod
     * @param httpMethod
     */
    private void extractResponses(Operation operation, HandlerMethod handlerMethod, String httpMethod) {
        ApiResponses responses = new ApiResponses();
        Method method = handlerMethod.getMethod();
        Class<?> returnType = method.getReturnType();
        Type genericReturnType = method.getGenericReturnType();

        // Get response status from @ResponseStatus annotation if present
        // TODO analyze response object (in bytecode(?)) for extract possible returns
        ResponseStatus responseStatus = AnnotationUtils.findAnnotation(method, ResponseStatus.class);
        String successCode = "200";
        String successDescription = "Successful operation";

        if (responseStatus != null) {
            successCode = String.valueOf(responseStatus.value().value());
            successDescription = responseStatus.reason().isEmpty()
                ? responseStatus.value().getReasonPhrase()
                : responseStatus.reason();
        } else {
            // Determine default success code based on HTTP method
            // Note: These are defaults. Actual implementation may return different status codes
            // via ResponseEntity constructor (e.g., ResponseEntity<>(data, HttpStatus.OK))
            switch (httpMethod.toUpperCase()) {
                case "POST":
                    successCode = "201";
                    successDescription = "Created (or 200 if explicitly set in code)";
                    break;
                case "DELETE":
                    successCode = "204";
                    successDescription = "No Content (or 200 if explicitly set in code)";
                    break;
                case "PUT":
                case "PATCH":
                    successCode = "200";
                    successDescription = "Updated";
                    break;
                default:
                    successCode = "200";
                    successDescription = "Successful operation";
            }
        }

        // Extract actual return type if wrapped in ResponseEntity
        Type actualGenericType = genericReturnType;

        //Check for ResponseEntity
        if (ResponseEntity.class.isAssignableFrom(returnType) && genericReturnType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericReturnType;
            Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0) {
                actualGenericType = typeArgs[0];
            }
        }

        // Add success response with content type if return type is not void
        ApiResponse successResponse = new ApiResponse().description(successDescription);

        //Check for no generic class not void or generic
        if ( (actualGenericType instanceof Class<?> clazz && !clazz.equals(Void.TYPE) && !clazz.equals(Void.class))
        || actualGenericType instanceof ParameterizedType) {

            Content content = new Content();
            MediaType mediaType = new MediaType();

            // Init creation and extraction swagger schema
            Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromType(actualGenericType);
            mediaType.setSchema(schema);
            content.addMediaType("application/json", mediaType);
            successResponse.setContent(content);
        }

        // Add code and successResponse
        responses.addApiResponse(successCode, successResponse);

        // Add common error responses
        responses.addApiResponse("400", new ApiResponse().description("Bad request"));
        responses.addApiResponse("401", new ApiResponse().description("Unauthorized"));
        responses.addApiResponse("403", new ApiResponse().description("Forbidden"));
        responses.addApiResponse("404", new ApiResponse().description("Not found"));
        responses.addApiResponse("500", new ApiResponse().description("Internal server error"));

        //Finally populated operation
        operation.setResponses(responses);
    }


    /**
     * Extracting REST API parameters to populate the operation object
     * @param operation
     * @param handlerMethod
     */
    private void extractParameters(Operation operation, HandlerMethod handlerMethod) {
        // Cycle in method params
        java.lang.reflect.Parameter[] parameters = handlerMethod.getMethod().getParameters();

        for (java.lang.reflect.Parameter param : parameters) {
            //Check for pathVariable, requestParam, requestBody and requestAttribute
            RequestAttribute requestAttribute = AnnotationUtils.findAnnotation(param, RequestAttribute.class);
            if (requestAttribute != null) {
                // Skip @RequestAttribute parameters (used only for UserDetails from authentication)
                continue;
            }

            PathVariable pathVar = AnnotationUtils.findAnnotation(param, PathVariable.class);
            if (pathVar != null) {
                String name = pathVar.value().isEmpty() ? pathVar.name() : pathVar.value();
                createAndAddParameter(operation, param.getType(), param.getName(), name, "path", pathVar.required(), null);
                continue;
            }

            RequestParam requestParam = AnnotationUtils.findAnnotation(param, RequestParam.class);
            if (requestParam != null) {
                String name = requestParam.value().isEmpty() ? requestParam.name() : requestParam.value();
                createAndAddParameter(operation, param.getType(), param.getName(), name, "query", requestParam.required(), null);
                continue;
            }

            org.springframework.web.bind.annotation.RequestBody requestBodyAnnotation =
                    AnnotationUtils.findAnnotation(param, org.springframework.web.bind.annotation.RequestBody.class);
            if (requestBodyAnnotation != null) {
                // Add request body schema
                RequestBody requestBody = new RequestBody();
                requestBody.setDescription("Request body");
                requestBody.setRequired(requestBodyAnnotation.required());

                Content content = new Content();
                MediaType mediaType = new MediaType();

                // Handle generic types like List<Entity>
                Class<?> paramType = param.getType();
                Type genericParamType = param.getParameterizedType();

                // Init creation and extraction swagger schema
                Schema<?> schema = OpenApiSchemaBuilder.createSchemaFromType(genericParamType);

                mediaType.setSchema(schema);
                content.addMediaType("application/json", mediaType);
                requestBody.setContent(content);

                operation.setRequestBody(requestBody);
                continue;
            }

            // Handle model attribute objects - introspect fields as query parameters
            if (!isSpringFrameworkType(param.getType())) {
                extractModelAttributeParameters(operation, param.getType());
            }
        }
    }

    /**
     * Check for spring or UserDetails class injected in controller method
     */
    private boolean isSpringFrameworkType(Class<?> type) {
        String typeName = type.getName();
        return typeName.startsWith("org.springframework.") ||
               typeName.startsWith("jakarta.servlet.") ||
               typeName.equals("com.agiletec.aps.system.services.user.UserDetails");
    }

    private void extractModelAttributeParameters(Operation operation, Class<?> modelClass) {
        // Recursively extract fields from the class and its superclasses
        Class<?> currentClass = modelClass;
        while (currentClass != null && !currentClass.equals(Object.class)) {
            for (java.lang.reflect.Field field : currentClass.getDeclaredFields()) {
                // Skip static, synthetic, and logger fields
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                    field.isSynthetic() ||
                    field.getType().getName().contains("Logger")) {
                    continue;
                }

                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                Type genericFieldType = field.getGenericType();

                // Check for @ApiParam annotation for additional metadata
                io.swagger.annotations.ApiParam apiParam = field.getAnnotation(io.swagger.annotations.ApiParam.class);

                createAndAddParameter(operation, field.getGenericType(), field.getName(), null, "query",
                        false, apiParam);

            }
            currentClass = currentClass.getSuperclass();
        }
    }

    /**
     * Creates an OpenAPI Parameter object and adds it to the operation.
     */
    private void createAndAddParameter(Operation operation, Type type, String paramName,
                                       String name, String in, boolean isRequired,
                                       io.swagger.annotations.ApiParam apiParam) {
        // If the annotation name is empty, fall back to the method's parameter name
        if (name== null || name.isEmpty()) {
            name = paramName;
        }

        String description = null;
        String defaultValue = null;
        String allowableValues = null;

        if (apiParam != null) {
            if (!apiParam.value().isEmpty()) {
                description = apiParam.value();
            }
            if (!apiParam.defaultValue().isEmpty()) {
                defaultValue = apiParam.defaultValue();
            }
            if (!apiParam.allowableValues().isEmpty()) {
                allowableValues = apiParam.allowableValues();
            }

            if (!apiParam.name().isEmpty()) {
                name = apiParam.name();
            }
        }

        //INIT creation schema for swagger
        Schema schema = OpenApiSchemaBuilder.createSchemaFromType(type);

        // Set schema based on field type using OpenApiSchemaBuilder with generic type info
        if (defaultValue != null) {
            schema.setDefault(defaultValue);
        }
        if (allowableValues != null) {
            String[] values = allowableValues.split(",");
            java.util.List enumList = new java.util.ArrayList();
            for (String value : values) {
                enumList.add(value.trim());
            }
            schema.setEnum(enumList);
        }

        Parameter parameter = new Parameter()
                .in(in)
                .name(name)
                .required(isRequired)
                // Use the schema builder to correctly determine the type (e.g., string, integer)
//                .schema(OpenApiSchemaBuilder.createSchemaFromClass(param.getType()));
                .schema(schema);
        if(description != null && !description.isEmpty()) {
            parameter.description(description);
        }

        operation.addParametersItem(parameter);
    }

}