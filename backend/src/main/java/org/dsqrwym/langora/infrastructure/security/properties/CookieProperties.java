package org.dsqrwym.langora.infrastructure.security.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Refresh-cookie settings.
 *
 * <p>{@code domain} stays nullable: an absent/empty value means "host-only cookie".
 * {@code secure} defaults to {@code true} so a missing profile override fails closed;
 * local HTTP development must set it to {@code false} explicitly.
 */
@Validated
@ConfigurationProperties(prefix = "langora.security.cookie")
public record CookieProperties(
        String domain,
        @DefaultValue("true") boolean secure,
        @DefaultValue("Lax")
        @Pattern(regexp = "(?i)strict|lax|none", message = "sameSite must be one of Strict, Lax or None")
        String sameSite,
        @DefaultValue("/auth/admin/refresh") @NotBlank(message = "refreshTokenPath is required")
        String refreshTokenPath
) {

    @AssertTrue(message = "sameSite=None requires secure=true")
    public boolean isSameSiteCompatibleWithSecure() {
        return !"none".equalsIgnoreCase(sameSite) || secure;
    }
}
