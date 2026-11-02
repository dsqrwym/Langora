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
import org.hibernate.validator.constraints.UUID;

import java.util.Locale;

public record LoginRequestDto(
        @Email
        String email,
        @AdminUsername
        String username,

        // only the 72-byte limit is enforced for BCrypt
        // 登录只强制 BCrypt 的 72 字节上限，不校验密码强度
        @NotBlank(message = "Password is required")
        @StrongPassword(minDigit = 0, minLength = 0, minLowercase = 0, minUppercase = 0)
        String password,
        @Size(max = 64)
        @UUID
        @NotBlank(message = "Device UUID is required")
        String deviceUuid,
        // 2048 是 Cloudflare 文档给的最大 token 长度
        @Size(max = 2048)
        String captchaToken
) {
    public LoginRequestDto {
        email = email != null ? TextUtils.sanitizeText(email).toLowerCase(Locale.ROOT) : null;
        username = TextUtils.sanitizeText(username);
        captchaToken = TextUtils.sanitizeText(captchaToken);
    }

    public LoginRequestDto(String email, String username, String password, String deviceUuid) {
        this(email, username, password, deviceUuid, null);
    }

    /**
     * Bean Validation class-level constraint, invoked reflectively by Hibernate Validator.
     * Annotated with {@link JsonIgnore} and {@link Schema#hidden()} to avoid serialization
     * and hide it from OpenAPI specification.
     * <p>
     * 类级 Bean Validation 约束，由 Hibernate Validator 反射调用；
     * 用 {@link JsonIgnore} 和 {@link Schema#hidden()} 避免被序列化和出现在 OpenAPI 文档中。
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
