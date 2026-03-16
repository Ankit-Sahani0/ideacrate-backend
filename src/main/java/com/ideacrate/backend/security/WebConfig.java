package com.ideacrate.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        // Allow both the standard CRA port (3000) and the Vite dev server port (5173).
                        // Add any other origins (e.g. staging/production domains) here as needed.
                        .allowedOrigins(
                                "http://localhost:3000",
                                "http://localhost:5173"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization")  // Allows frontend to read the Authorization header
                        .allowCredentials(true)
                        .maxAge(3600); // Cache the preflight response for 1 hour to reduce OPTIONS requests
            }
        };
    }
}
