package org.dsqrwym.langora.infrastructure.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "langora.security.cors")
public record CorsProperties(
        List<String> allowedOrigins,
        List<String> allowedMethods,
        List<String> allowedHeaders,
        List<String> exposedHeaders,
        boolean allowCredentials,
        long maxAge
) {
    public CorsProperties {
        if (allowedMethods == null || allowedMethods.isEmpty()) {
            allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        }
        if (allowedHeaders == null || allowedHeaders.isEmpty()) {
            allowedHeaders = List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Client-Platform");
        }
        if (exposedHeaders == null || exposedHeaders.isEmpty()) {
            exposedHeaders = List.of("Set-Cookie");
        }
        if (maxAge <= 0) {
            maxAge = 3600L;
        }
    }
}