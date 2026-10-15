package org.dsqrwym.langora.infrastructure.security.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("langora.security.csrf")
public record CsrfProperties(
        @NotBlank(message = "JWT secretKey is required")
        @Size(min = 32, message = "JWT secretKey must be at least 32 bytes for HS256")
        String secretKey
) {
}
