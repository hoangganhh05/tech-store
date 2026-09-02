package com.techstore.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI techStoreOpenApi() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("TechStore API")
                        .description("REST API documentation for TechStore")
                        .version("v1")
                        .contact(new Contact().name("TechStore Team"))
                        .license(new License().name("Private project")));
    }
}
