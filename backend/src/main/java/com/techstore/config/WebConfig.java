package com.techstore.config;

import com.techstore.security.RoleAuthorizationInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.http.HttpHeaders;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;
    private final ObjectProvider<RoleAuthorizationInterceptor> roleAuthorizationInterceptorProvider;

    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;

    public WebConfig(
            @Value("${app.cors.allowed-origins}") String allowedOrigins,
            ObjectProvider<RoleAuthorizationInterceptor> roleAuthorizationInterceptorProvider
    ) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
        this.roleAuthorizationInterceptorProvider = roleAuthorizationInterceptorProvider;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                // Authentication is sent via the Authorization header, never an
                // ambient cookie. Keep the cross-origin surface explicit.
                .allowedHeaders(
                        HttpHeaders.AUTHORIZATION,
                        HttpHeaders.CONTENT_TYPE,
                        HttpHeaders.ACCEPT,
                        "X-Session-Id",
                        "X-Requested-With"
                )
                .exposedHeaders(HttpHeaders.CONTENT_DISPOSITION)
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        RoleAuthorizationInterceptor interceptor = roleAuthorizationInterceptorProvider.getIfAvailable();
        if (interceptor != null) {
            registry.addInterceptor(interceptor)
                    .addPathPatterns("/api/**");
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadBaseDir).toAbsolutePath().normalize();
        String uploadUri = uploadPath.toUri().toString();
        if (!uploadUri.endsWith("/")) {
            uploadUri += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadUri);
    }
}
