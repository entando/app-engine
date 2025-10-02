package org.entando.entando.web.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scans Spring MVC controllers and generates OpenAPI documentation from @RequestMapping annotations
 */
@Component
@EnableWebMvc
public class SpringMvcOpenApiScanner {

    private static final Logger logger = LoggerFactory.getLogger(SpringMvcOpenApiScanner.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private OpenAPI openAPI;

    @PostConstruct
    public void scanControllers() {
        logger.info("Scanning Spring MVC controllers for OpenAPI documentation...");

        RequestMappingHandlerMapping mapping = applicationContext.getBean("requestMappingHandlerMapping",RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();

        AtomicInteger count = new AtomicInteger();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();

            // Get path patterns
            if (mappingInfo.getPathPatternsCondition() != null &&
                !mappingInfo.getPathPatternsCondition().getPatterns().isEmpty()) {

                mappingInfo.getPathPatternsCondition().getPatterns().forEach(pattern -> {
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
        PathItem pathItem = openAPI.getPaths() != null && openAPI.getPaths().get(path) != null
                ? openAPI.getPaths().get(path)
                : new PathItem();

        Method method = handlerMethod.getMethod();
        String methodName = method.getName();
        String className = method.getDeclaringClass().getSimpleName();

        // Get HTTP methods
        if (mappingInfo.getMethodsCondition().getMethods().isEmpty()) {
            // If no method specified, assume GET
            addOperation(pathItem, "GET", methodName, className, handlerMethod);
        } else {
            mappingInfo.getMethodsCondition().getMethods().forEach(httpMethod -> {
                addOperation(pathItem, httpMethod.name(), methodName, className, handlerMethod);
            });
        }

        if (openAPI.getPaths() == null) {
            openAPI.paths(new io.swagger.v3.oas.models.Paths());
        }
        openAPI.getPaths().addPathItem(path, pathItem);
    }

    private void addOperation(PathItem pathItem, String httpMethod, String methodName,
                              String className, HandlerMethod handlerMethod) {
        Operation operation = new Operation();

        // Set operation ID and summary
        operation.setOperationId(methodName);
        operation.setSummary(className + "." + methodName);
        operation.setDescription("Endpoint: " + className + "#" + methodName);

        // Add default response
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("200", new ApiResponse().description("Successful operation"));
        responses.addApiResponse("400", new ApiResponse().description("Bad request"));
        responses.addApiResponse("401", new ApiResponse().description("Unauthorized"));
        responses.addApiResponse("500", new ApiResponse().description("Internal server error"));
        operation.setResponses(responses);

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

    private void extractParameters(Operation operation, HandlerMethod handlerMethod) {
        java.lang.reflect.Parameter[] parameters = handlerMethod.getMethod().getParameters();

        for (java.lang.reflect.Parameter param : parameters) {
            PathVariable pathVar = AnnotationUtils.findAnnotation(param, PathVariable.class);
            RequestParam requestParam = AnnotationUtils.findAnnotation(param, RequestParam.class);
            RequestBody requestBody = AnnotationUtils.findAnnotation(param, RequestBody.class);

            if (pathVar != null) {
                String name = pathVar.value().isEmpty() ? pathVar.name() : pathVar.value();
                if (name.isEmpty()) {
                    name = param.getName();
                }
                Parameter parameter = new Parameter()
                        .in("path")
                        .name(name)
                        .required(pathVar.required())
                        .description("Path variable: " + name)
                        .schema(new io.swagger.v3.oas.models.media.Schema<>().type("string"));
                operation.addParametersItem(parameter);
            } else if (requestParam != null) {
                String name = requestParam.value().isEmpty() ? requestParam.name() : requestParam.value();
                if (name.isEmpty()) {
                    name = param.getName();
                }
                Parameter parameter = new Parameter()
                        .in("query")
                        .name(name)
                        .required(requestParam.required())
                        .description("Query parameter: " + name)
                        .schema(new io.swagger.v3.oas.models.media.Schema<>().type("string"));
                operation.addParametersItem(parameter);
            } else if (requestBody != null) {
                // Request body is handled differently in OpenAPI 3.0
                operation.setDescription(operation.getDescription() + " (Accepts request body)");
            }
        }
    }
}