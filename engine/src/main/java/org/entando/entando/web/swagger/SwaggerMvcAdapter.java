package org.entando.entando.web.swagger;

// DEPRECATED: This is no longer needed with Springdoc OpenAPI
// Springdoc auto-configures the necessary resource handlers
// Kept for reference during migration

/*
import org.springframework.web.servlet.config.annotation.*;

public class SwaggerMvcAdapter implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
*/