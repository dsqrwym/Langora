package org.dsqrwym.langora.infrastructure.security.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "langora.security.jwt")
public record JwtProperties(
        @NotBlank(message = "JWT secretKey is required")
        @Size(min = 32, message = "JWT secretKey must be at least 32 bytes for HS256")
        String secretKey,
        String issuer,
        Duration accessTokenExpiration,
        Duration refreshTokenExpiration
) {
    public JwtProperties {
        if (issuer == null) {
            issuer = "langora-auth";
        }
        if (accessTokenExpiration == null) {
            accessTokenExpiration = Duration.ofMinutes(15);
        }
        if (refreshTokenExpiration == null) {
            refreshTokenExpiration = Duration.ofDays(1);
        }
    }
}
