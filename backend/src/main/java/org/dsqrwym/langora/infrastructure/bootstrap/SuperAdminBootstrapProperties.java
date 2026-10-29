package org.dsqrwym.langora.infrastructure.bootstrap;

import jakarta.validation.constraints.AssertTrue;
import org.dsqrwym.langora.infrastructure.util.TextUtils;
import org.dsqrwym.langora.infrastructure.validation.email.Email;
import org.dsqrwym.langora.infrastructure.validation.password.StrongPassword;
import org.dsqrwym.langora.infrastructure.validation.username.AdminUsername;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Super-admin bootstrap settings.
 *
 * <p>Credentials are only mandatory when bootstrap is enabled. Production runs with
 * {@code enabled=false} and no default credentials, so it can start without them; enabling
 * bootstrap there requires supplying email, username and password explicitly.
 *
 * <p>While {@code enabled=false} the whole credential block is ignored: the values are dropped
 * during binding, so neither presence nor format is checked and a stale or malformed entry cannot
 * block startup. Once {@code enabled=true} the credentials become mandatory and must be valid, and
 * an empty placeholder (for example {@code ${BOOTSTRAP_SUPER_ADMIN_PASSWORD}} while the variable is
 * unset or empty) counts as "not provided" because blank values are normalised to {@code null}.
 */
@Validated
@ConfigurationProperties(prefix = "langora.bootstrap.super-admin")
public record SuperAdminBootstrapProperties(
        @DefaultValue("false") boolean enabled,
        @Email String email,
        @AdminUsername String username,
        @StrongPassword String password,
        @DefaultValue("false") boolean overrideExistingPassword,
        // Derived flag set by the canonical constructor, never read from the environment.
        @DefaultValue("false") boolean credentialsIgnored
) {

    public SuperAdminBootstrapProperties {
        if (enabled) {
            email = TextUtils.defaultIfBlank(email, null);
            username = TextUtils.defaultIfBlank(username, null);
            password = TextUtils.defaultIfBlank(password, null);
            credentialsIgnored = false;
        } else {
            // Disabled: credentials are never used, so drop them to keep every constraint inert.
            credentialsIgnored = StringUtils.hasText(email)
                    || StringUtils.hasText(username)
                    || StringUtils.hasText(password);
            email = null;
            username = null;
            password = null;
        }
    }

    /**
     * Cross-field constraint: credentials are all-or-nothing, but only when bootstrap is enabled.
     * Presence is checked here so the format constraints can keep skipping {@code null} only.
     */
    @AssertTrue(message = "email, username and password are required when super-admin bootstrap is enabled")
    public boolean isCredentialsPresentWhenEnabled() {
        return !enabled || (StringUtils.hasText(email) && StringUtils.hasText(username) && StringUtils.hasText(password));
    }
}
