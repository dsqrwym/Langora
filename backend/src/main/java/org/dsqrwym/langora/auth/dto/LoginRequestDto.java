package org.dsqrwym.langora.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.dsqrwym.langora.infrastructure.util.TextUtils;
import org.dsqrwym.langora.infrastructure.validation.email.Email;
import org.dsqrwym.langora.infrastructure.validation.password.StrongPassword;
import org.dsqrwym.langora.infrastructure.validation.username.AdminUsername;

import java.util.Locale;

public record LoginRequestDto(
        @Email
        String email,
        @AdminUsername
        String username,

        // only the 72-byte limit is enforced for BCrypt
        @NotBlank(message = "Password is required")
        @StrongPassword(minDigit = 0, minLength = 0, minLowercase = 0, minUppercase = 0)
        String password,
        @Size(max = 64)
        @NotBlank(message = "Device UUID is required")
        String deviceUuid
) {
    public LoginRequestDto {
        email = email != null ? TextUtils.sanitizeText(email).toLowerCase(Locale.ROOT) : null;
        username = TextUtils.sanitizeText(username);
    }

    /**
     * Bean Validation class-level constraint, invoked reflectively by Hibernate Validator.
     * Annotated with {@link JsonIgnore} and {@link Schema#hidden()} to avoid serialization
     * and hide it from OpenAPI specification.
     *
     * @return {@code true} if email or username is non-blank
     */
    @SuppressWarnings("unused")
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Provide exactly one of email or username")
    public boolean isIdentifierPresent() {
        final boolean hasUsername = username != null && !username.isBlank();
        final boolean hasEmail = email != null && !email.isBlank();
        return hasUsername ^ hasEmail;
    }
}
