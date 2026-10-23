package org.dsqrwym.langora.infrastructure.resilience;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(DbBulkheadProperties.PREFIX)
public record DbBulkheadProperties(
        @DefaultValue("true") boolean enabled,
        /** 0 = derive the permit count from the Hikari pool size. */
        @DefaultValue("0") @PositiveOrZero int maxConcurrentCalls,
        /** Must stay well below Hikari's connection timeout, otherwise a pool wait defeats the rejection. */
        @DefaultValue("2s") @NotNull Duration maxWaitDuration,
        /** FIFO admission control to prevent tail queue starvation.*/
        @DefaultValue("true") boolean fairCallHandlingEnabled
) {
    public static final String PREFIX = "langora.resilience.db.bulkhead";
}
