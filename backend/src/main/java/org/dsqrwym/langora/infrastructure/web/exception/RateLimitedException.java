package org.dsqrwym.langora.infrastructure.web.exception;

import org.dsqrwym.langora.infrastructure.web.ErrorCode;
import org.springframework.http.HttpStatus;

import java.time.Duration;

public final class RateLimitedException extends BusinessException {
    public final int retryAfterSeconds;

    public RateLimitedException(Duration retryAfter) {
        super(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.RATE_LIMITED);
        long millis = retryAfter.toMillis();
        // 纯整数向上取整：(millis + 999) / 1000 等价于 Math.ceil(millis / 1000.0)
        long calculatedSeconds = (millis + 999) / 1000;
        // 确保不低于 1 秒
        this.retryAfterSeconds = (int) Math.max(1L, calculatedSeconds);
    }
}
