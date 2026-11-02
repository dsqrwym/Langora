package org.dsqrwym.langora.infrastructure.security.login;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "langora.security.login-attempt")
public record LoginAttemptProperties(
        @DefaultValue("memory") Store store,
        @DefaultValue("20") @Positive int ipBlockThreshold,
        /** The number of failed login attempts in same IP required to trigger a captcha*/
        @DefaultValue("3") @Positive int captchaThreshold,
        /** The number of failed login attempts in same account required to trigger an account lock*/
        @DefaultValue("5") @Positive int accountBlockThreshold,
        @DefaultValue("15m") @NotNull @DurationMin(seconds = 1) Duration window,
        @DefaultValue("5m") @NotNull @DurationMin(seconds = 1) Duration cleanupInterval,
        /** The maximum number of login attempts to keep in memory, 50000 approximately 5~10 MB */
        @DefaultValue("50000") @Positive int maxLocalEntries,
        /** The cooldown period after a Redis connection failure */
        @DefaultValue("30s") @NotNull @DurationMin(seconds = 1) Duration redisDegradedCooldown
) {
    public enum Store {
        MEMORY, REDIS
    }

    @AssertTrue(message = "captchaThreshold must be lower than ipBlockThreshold")
    public boolean isCaptchaThresholdBelowIpBlockThreshold() {
        return captchaThreshold < ipBlockThreshold;
    }

    @AssertTrue(message = "accountBlockThreshold must be lower than ipBlockThreshold")
    public boolean isAccountThresholdBelowIpBlockThreshold() {
        return accountBlockThreshold < ipBlockThreshold;
    }
}
