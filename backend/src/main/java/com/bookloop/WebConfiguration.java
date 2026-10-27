package com.bookloop;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private final String frontendOrigin;

    public WebConfiguration(
            @Value("${bookloop.web.frontend-origin:http://localhost:5173}") String frontendOrigin
    ) {
        this.frontendOrigin = frontendOrigin;
    }

    /** Allows the configured BookLoop frontend to call the backend API. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(frontendOrigin)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization", "X-XSRF-TOKEN")
                .allowCredentials(true);
    }
}
