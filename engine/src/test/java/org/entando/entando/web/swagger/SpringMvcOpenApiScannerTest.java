package org.entando.entando.web.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import jakarta.servlet.ServletContext;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SpringMvcOpenApiScanner
 */
@SpringJUnitWebConfig(classes = {
    SpringMvcOpenApiScannerTest.TestConfig.class,
    SpringMvcOpenApiScannerTest.TestController.class
})
@WebAppConfiguration
class SpringMvcOpenApiScannerTest {

    @Autowired
    private ApplicationContext applicationContext;

    private SpringMvcOpenApiScanner scanner;
    private OpenAPI openAPI;

    @BeforeEach
    void setUp() {
        openAPI = new OpenAPI();
        scanner = new SpringMvcOpenApiScanner();
        setPrivateField(scanner, "applicationContext", applicationContext);
        setPrivateField(scanner, "openAPI", openAPI);
    }

    // ========== Test Configuration ==========

    @org.springframework.context.annotation.Configuration
    @EnableWebMvc
    static class TestConfig {

        @Bean
        public ServletContext servletContext() {
            return new MockServletContext();
        }
    }

    // ========== Test Controller ==========

    @Controller
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/simple")
        public ResponseEntity<String> simpleGet() {
            return ResponseEntity.ok("test");
        }

        @PostMapping("/create")
        public ResponseEntity<TestDto> createResource(@org.springframework.web.bind.annotation.RequestBody TestDto dto) {
            return ResponseEntity.ok(dto);
        }

        @GetMapping("/path/{id}")
        public ResponseEntity<TestDto> getById(@PathVariable String id) {
            return ResponseEntity.ok(new TestDto());
        }

        @GetMapping("/query")
        public ResponseEntity<List<TestDto>> search(
            @RequestParam String name,
            @RequestParam(required = false) Integer age
        ) {
            return ResponseEntity.ok(List.of());
        }

        @PutMapping("/update/{id}")
        public ResponseEntity<TestDto> update(
            @PathVariable String id,
            @org.springframework.web.bind.annotation.RequestBody TestDto dto
        ) {
            return ResponseEntity.ok(dto);
        }

        @DeleteMapping("/delete/{id}")
        public ResponseEntity<Void> delete(@PathVariable String id) {
            return ResponseEntity.noContent().build();
        }

        @GetMapping("/model-attribute")
        public ResponseEntity<List<TestDto>> searchWithModel(SearchRequest request) {
            return ResponseEntity.ok(List.of());
        }
    }

    static class TestDto {
        private String name;
        private Integer value;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getValue() { return value; }
        public void setValue(Integer value) { this.value = value; }
    }

    static class SearchRequest {
        private String query;
        private Integer pageSize;
        private List<String> filters;

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
        public List<String> getFilters() { return filters; }
        public void setFilters(List<String> filters) { this.filters = filters; }
    }

    // ========== Tests ==========

    @Test
    void testScannerInitialization() {
        assertNotNull(scanner);
        scanner.scanControllers();
        assertNotNull(openAPI.getPaths());
    }

    @Test
    void testSimpleGetEndpointScanned() {
        scanner.scanControllers();

        PathItem pathItem = openAPI.getPaths().get("/api/test/simple");
        assertNotNull(pathItem, "GET /api/test/simple should be scanned");

        Operation getOp = pathItem.getGet();
        assertNotNull(getOp);
        assertEquals("simpleGet", getOp.getOperationId());
        assertTrue(getOp.getTags().contains("TestController"));
    }

    @Test
    void testPostEndpointWithRequestBody() {
        scanner.scanControllers();

        PathItem pathItem = openAPI.getPaths().get("/api/test/create");
        assertNotNull(pathItem);

        Operation postOp = pathItem.getPost();
        assertNotNull(postOp);

        // Check request body
        RequestBody requestBody = postOp.getRequestBody();
        assertNotNull(requestBody);
        assertTrue(requestBody.getRequired());

        // Check response
        assertNotNull(postOp.getResponses());
        assertTrue(postOp.getResponses().containsKey("201"));
    }

    @Test
    void testPathVariableExtraction() {
        scanner.scanControllers();

        PathItem pathItem = openAPI.getPaths().get("/api/test/path/{id}");
        assertNotNull(pathItem);

        Operation getOp = pathItem.getGet();
        assertNotNull(getOp);

        List<Parameter> parameters = getOp.getParameters();
        assertNotNull(parameters);
        assertFalse(parameters.isEmpty());

        Parameter idParam = parameters.stream()
            .filter(p -> "id".equals(p.getName()))
            .findFirst()
            .orElse(null);

        assertNotNull(idParam);
        assertEquals("path", idParam.getIn());
        assertTrue(idParam.getRequired());
    }

    @Test
    void testRequestParamExtraction() {
        scanner.scanControllers();

        PathItem pathItem = openAPI.getPaths().get("/api/test/query");
        assertNotNull(pathItem);

        Operation getOp = pathItem.getGet();
        List<Parameter> parameters = getOp.getParameters();

        // Should have 'name' and 'age' parameters
        assertTrue(parameters.size() >= 2);

        Parameter nameParam = findParameter(parameters, "name");
        assertNotNull(nameParam);
        assertEquals("query", nameParam.getIn());
        assertTrue(nameParam.getRequired());

        Parameter ageParam = findParameter(parameters, "age");
        assertNotNull(ageParam);
        assertEquals("query", ageParam.getIn());
        assertFalse(ageParam.getRequired());
    }

    @Test
    void testPutEndpointMapping() {
        scanner.scanControllers();

        PathItem pathItem = openAPI.getPaths().get("/api/test/update/{id}");
        assertNotNull(pathItem);

        Operation putOp = pathItem.getPut();
        assertNotNull(putOp);
        assertEquals("update", putOp.getOperationId());

        // Should have path variable
        assertTrue(putOp.getParameters().stream()
            .anyMatch(p -> "id".equals(p.getName()) && "path".equals(p.getIn())));

        // Should have request body
        assertNotNull(putOp.getRequestBody());
    }

    @Test
    void testDeleteEndpointMapping() {
        scanner.scanControllers();

        PathItem pathItem = openAPI.getPaths().get("/api/test/delete/{id}");
        assertNotNull(pathItem);

        Operation deleteOp = pathItem.getDelete();
        assertNotNull(deleteOp);

        // Check response code
        assertNotNull(deleteOp.getResponses());
        assertTrue(deleteOp.getResponses().containsKey("204"));
    }

    @Test
    void testModelAttributeParametersExtraction() {
        scanner.scanControllers();

        PathItem pathItem = openAPI.getPaths().get("/api/test/model-attribute");
        assertNotNull(pathItem);

        Operation getOp = pathItem.getGet();
        assertNotNull(getOp);

        List<Parameter> parameters = getOp.getParameters();
        assertNotNull(parameters);

        // Should extract fields from SearchRequest as query parameters
        Parameter queryParam = findParameter(parameters, "query");
        assertNotNull(queryParam);
        assertEquals("query", queryParam.getIn());

        Parameter pageSizeParam = findParameter(parameters, "pageSize");
        assertNotNull(pageSizeParam);
        assertEquals("query", pageSizeParam.getIn());

        Parameter filtersParam = findParameter(parameters, "filters");
        assertNotNull(filtersParam);
        assertEquals("query", filtersParam.getIn());
        assertEquals("array", filtersParam.getSchema().getType());
    }

    @Test
    void testResponseStatusCodeMapping() {
        scanner.scanControllers();

        // GET should default to 200
        Operation getOp = openAPI.getPaths().get("/api/test/simple").getGet();
        assertTrue(getOp.getResponses().containsKey("200"));

        // POST should default to 201
        Operation postOp = openAPI.getPaths().get("/api/test/create").getPost();
        assertTrue(postOp.getResponses().containsKey("201"));

        // DELETE should default to 204
        Operation deleteOp = openAPI.getPaths().get("/api/test/delete/{id}").getDelete();
        assertTrue(deleteOp.getResponses().containsKey("204"));
    }

    @Test
    void testCommonErrorResponses() {
        scanner.scanControllers();

        Operation getOp = openAPI.getPaths().get("/api/test/simple").getGet();

        // Should include common error responses
        assertTrue(getOp.getResponses().containsKey("400"));
        assertTrue(getOp.getResponses().containsKey("401"));
        assertTrue(getOp.getResponses().containsKey("403"));
        assertTrue(getOp.getResponses().containsKey("404"));
        assertTrue(getOp.getResponses().containsKey("500"));
    }

    @Test
    void testApiPrefixAddedToPaths() {
        scanner.scanControllers();

        // All paths should start with /api
        for (String path : openAPI.getPaths().keySet()) {
            assertTrue(path.startsWith("/api/"), "Path should start with /api: " + path);
        }
    }

    @Test
    void testOperationTags() {
        scanner.scanControllers();

        Operation getOp = openAPI.getPaths().get("/api/test/simple").getGet();
        assertNotNull(getOp.getTags());
        assertTrue(getOp.getTags().contains("TestController"));
    }

    @Test
    void testGenericTypePreservation() {
        scanner.scanControllers();

        PathItem pathItem = openAPI.getPaths().get("/api/test/query");
        Operation getOp = pathItem.getGet();

        // Response should be List<TestDto>
        assertNotNull(getOp.getResponses().get("200"));
        assertNotNull(getOp.getResponses().get("200").getContent());
    }

    // ========== Helper Methods ==========

    private Parameter findParameter(List<Parameter> parameters, String name) {
        return parameters.stream()
            .filter(p -> name.equals(p.getName()))
            .findFirst()
            .orElse(null);
    }

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