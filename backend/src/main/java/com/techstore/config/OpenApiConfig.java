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
    OpenAPI storeOpenApi(StoreBrandProperties brand) {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title(brand.getName() + " API")
                        .description(brand.getName() + " - " + brand.getIndustry())
                        .version("v1")
                        .contact(new Contact()
                                .name(brand.getName())
                                .email(brand.getContactEmail()))
                        .license(new License().name("Private project")));
    }
}
