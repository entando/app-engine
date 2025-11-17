package org.entando.entando.web.swagger;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for OpenApiController
 */
class OpenApiControllerTest {

    private OpenApiController controller;
    private OpenAPI openAPI;

    @Mock
    private HttpServletResponse response;

    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a test OpenAPI instance
        openAPI = new OpenAPI()
            .info(new Info()
                .title("Test API")
                .version("1.0.0")
                .description("Test API Description"))
            .addServersItem(new Server().url("/test"));

        controller = new OpenApiController();
        setPrivateField(controller, "openAPI", openAPI);

        // Setup response mock
        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);
    }

    @Test
    void testGetOpenApiSpecReturnsJson() throws Exception {
        controller.getOpenApiSpec(response);

        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(response).getWriter();

        printWriter.flush();
        String output = stringWriter.toString();
        assertNotNull(output);
        assertFalse(output.isEmpty());
    }

    @Test
    void testOpenApiSpecContainsInfo() throws Exception {
        controller.getOpenApiSpec(response);

        printWriter.flush();
        String output = stringWriter.toString();

        assertTrue(output.contains("\"info\""));
        assertTrue(output.contains("\"title\" : \"Test API\""));
        assertTrue(output.contains("\"version\" : \"1.0.0\""));
        assertTrue(output.contains("\"description\" : \"Test API Description\""));
    }

    @Test
    void testOpenApiSpecContainsServers() throws Exception {
        controller.getOpenApiSpec(response);

        printWriter.flush();
        String output = stringWriter.toString();

        assertTrue(output.contains("\"servers\""));
        assertTrue(output.contains("\"/test\""));
    }

    @Test
    void testJsonFormatIsPretty() throws Exception {
        controller.getOpenApiSpec(response);

        printWriter.flush();
        String output = stringWriter.toString();

        // Pretty JSON should have newlines and indentation
        assertTrue(output.contains("\n"));
        assertTrue(output.contains("  "));
    }

    @Test
    void testContentTypeIsSet() throws Exception {
        controller.getOpenApiSpec(response);

        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
    }

    @Test
    void testOpenApiSpecIsValidJson() throws Exception {
        controller.getOpenApiSpec(response);

        printWriter.flush();
        String output = stringWriter.toString();

        // Should be parseable as JSON
        assertDoesNotThrow(() -> Json.mapper().readTree(output));
    }

    @Test
    void testEmptyOpenApiSpec() throws Exception {
        OpenAPI emptyApi = new OpenAPI();
        setPrivateField(controller, "openAPI", emptyApi);

        controller.getOpenApiSpec(response);

        printWriter.flush();
        String output = stringWriter.toString();

        assertNotNull(output);
        // Should still be valid JSON even if mostly empty
        assertTrue(output.contains("{"));
        assertTrue(output.contains("}"));
    }

    @Test
    void testOpenApiSpecWithPaths() throws Exception {
        openAPI.path("/test/endpoint", new io.swagger.v3.oas.models.PathItem()
            .get(new io.swagger.v3.oas.models.Operation()
                .operationId("testOp")
                .summary("Test Operation")));

        controller.getOpenApiSpec(response);

        printWriter.flush();
        String output = stringWriter.toString();

        assertTrue(output.contains("\"paths\""));
        assertTrue(output.contains("\"/test/endpoint\""));
        assertTrue(output.contains("\"operationId\" : \"testOp\""));
    }

    @Test
    void testOpenApiSpecWithComponents() throws Exception {
        openAPI.components(new io.swagger.v3.oas.models.Components()
            .addSchemas("TestSchema", new io.swagger.v3.oas.models.media.Schema<>()
                .type("object")
                .title("TestSchema")));

        controller.getOpenApiSpec(response);

        printWriter.flush();
        String output = stringWriter.toString();

        assertTrue(output.contains("\"components\""));
        assertTrue(output.contains("\"TestSchema\""));
    }

    @Test
    void testOpenApiSpecWithSecurity() throws Exception {
        openAPI.components(new io.swagger.v3.oas.models.Components()
            .addSecuritySchemes("oauth2", new io.swagger.v3.oas.models.security.SecurityScheme()
                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.OAUTH2)));

        controller.getOpenApiSpec(response);

        printWriter.flush();
        String output = stringWriter.toString();

        assertTrue(output.contains("\"securitySchemes\""));
        assertTrue(output.contains("\"oauth2\""));
    }

    // ========== Helper Methods ==========

    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}