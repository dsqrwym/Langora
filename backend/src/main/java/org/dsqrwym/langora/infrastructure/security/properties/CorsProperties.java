package org.dsqrwym.langora.infrastructure.security.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * CORS settings.
 *
 * <p>{@code allowedOrigins} has no default: every environment must opt in explicitly, and an
 * absent or empty list fails startup instead of silently disabling CORS.
 */
@Validated
@ConfigurationProperties(prefix = "langora.security.cors")
public record CorsProperties(
        @NotEmpty(message = "allowedOrigins must not be empty")
        List<@NotBlank String> allowedOrigins,
        @DefaultValue({"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
        @NotEmpty List<@NotBlank String> allowedMethods,
        @DefaultValue({"Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Client-Platform"})
        @NotEmpty List<@NotBlank String> allowedHeaders,
        @DefaultValue("Set-Cookie")
        List<@NotBlank String> exposedHeaders,
        @DefaultValue("false") boolean allowCredentials,
        @DefaultValue("3600") @PositiveOrZero long maxAge
) {

    @AssertTrue(message = "allowedOrigins cannot contain '*' when allowCredentials is true")
    public boolean isCredentialedOriginConfigurationValid() {
        return !allowCredentials
                || allowedOrigins == null
                || !allowedOrigins.contains("*");
    }
}
