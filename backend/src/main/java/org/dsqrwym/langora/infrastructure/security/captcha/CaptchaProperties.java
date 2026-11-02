package org.dsqrwym.langora.infrastructure.security.captcha;

import jakarta.validation.constraints.AssertTrue;
import org.dsqrwym.langora.infrastructure.util.TextUtils;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Validated
@ConfigurationProperties(prefix = "langora.security.captcha")
public record CaptchaProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("https://challenges.cloudflare.com/turnstile/v0/siteverify") String verifyUrl,
        String siteKey,
        String secretKey,
        @DefaultValue("login") String expectedAction,
        @DefaultValue List<String> allowedHostnames,
        @DefaultValue("true") boolean failOpen,
        @DefaultValue("2s") @DurationMin(seconds = 1) Duration connectTimeout,
        @DefaultValue("3s") @DurationMin(seconds = 1) Duration readTimeout
) {
    private static final String DEFAULT_VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    public CaptchaProperties {
        verifyUrl = TextUtils.defaultIfBlank(verifyUrl, DEFAULT_VERIFY_URL);
        siteKey = Objects.requireNonNullElse(siteKey, "");
        secretKey = Objects.requireNonNullElse(secretKey, "");
        expectedAction = Objects.requireNonNullElse(expectedAction, "");
        allowedHostnames = Objects.requireNonNullElse(allowedHostnames, List.of());
        connectTimeout = Objects.requireNonNullElse(connectTimeout, Duration.ofSeconds(2));
        readTimeout = Objects.requireNonNullElse(readTimeout, Duration.ofSeconds(3));
    }

    @AssertTrue(message = "langora.security.captcha.enabled=true requires both site-key and secret-key")
    public boolean isSiteKeyAndSecretKeyPresentWhenEnabled() {
        return !enabled || (StringUtils.hasText(siteKey) && StringUtils.hasText(secretKey));
    }
}
