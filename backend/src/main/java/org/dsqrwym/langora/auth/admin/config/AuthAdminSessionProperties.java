package org.dsqrwym.langora.auth.admin.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.dsqrwym.langora.infrastructure.jooq.enums.AdminUserRole;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Admin session policy.
 *
 * <p>All numeric limits are primitives with {@link DefaultValue} fallbacks so that an empty
 * property (for example {@code super-admin-max-session:}) fails at startup instead of silently
 * becoming {@code null} and disabling the session limit at runtime.
 *
 * <p>{@code cleanup.zone} is also read by the {@code @Scheduled} placeholder in
 * {@code AuthAdminSessionCleanupTask}; it is validated here so an empty value fails at startup
 * instead of at scheduler registration.
 */
@Validated
@ConfigurationProperties(prefix = "langora.auth.admin.session")
public record AuthAdminSessionProperties(
        @DefaultValue("3") @Positive @Max(50) int superAdminMaxSession,
        @DefaultValue("5") @Positive @Max(50) int adminMaxSession,
        @DefaultValue("5") @Positive @Max(50) int editorMaxSession,
        @DefaultValue("5") @PositiveOrZero @Max(3600) int refreshGraceSeconds,
        @Valid @DefaultValue Cleanup cleanup
) {

    public record Cleanup(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("365d") @NotNull @DurationMin(days = 1) Duration retention,
            @DefaultValue("1000") @Min(1) @Max(10_000) int batchSize,
            @DefaultValue("Europe/Madrid") @NotBlank String zone
    ) {
    }

    public int getMaxSessionByRole(AdminUserRole role) {
        return switch (role) {
            case SUPER_ADMIN -> superAdminMaxSession;
            case ADMIN -> adminMaxSession;
            case EDITOR -> editorMaxSession;
        };
    }
}
