package org.dsqrwym.langora.infrastructure.security.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "langora.security.csrf")
public record CsrfProperties(
        @NotBlank(message = "CSRF secretKey is required")
        @Size(min = 32, message = "CSRF secretKey must be at least 32 characters for HMAC-SHA256")
        String secretKey
) {
}
