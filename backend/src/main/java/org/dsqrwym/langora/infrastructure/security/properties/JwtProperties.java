package org.dsqrwym.langora.infrastructure.security.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "langora.security.jwt")
public record JwtProperties(
        @NotBlank(message = "JWT secretKey is required")
        @Size(min = 32, message = "JWT secretKey must be at least 32 characters for HS256")
        String secretKey,
        @DefaultValue("langora-auth") @NotBlank String issuer,
        @DefaultValue("15m") @NotNull @DurationMin(seconds = 60) Duration accessTokenExpiration,
        @DefaultValue("1d") @NotNull @DurationMin(minutes = 1) Duration refreshTokenExpiration
) {

    /**
     * A refresh token that expires at or before the access token cannot extend a session.
     */
    @AssertTrue(message = "refreshTokenExpiration must be greater than accessTokenExpiration")
    public boolean isRefreshTokenLongerThanAccessToken() {
        if (accessTokenExpiration == null || refreshTokenExpiration == null) {
            return true; // @NotNull reports the missing value
        }
        return refreshTokenExpiration.compareTo(accessTokenExpiration) > 0;
    }
}
