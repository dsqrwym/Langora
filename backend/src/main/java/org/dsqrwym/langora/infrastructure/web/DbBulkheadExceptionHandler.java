package org.dsqrwym.langora.infrastructure.web;

import lombok.extern.slf4j.Slf4j;
import org.dsqrwym.langora.infrastructure.web.exception.RateLimitedException;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DbBulkheadExceptionHandler {
    @ExceptionHandler(RateLimitedException.class)
    public ResponseEntity<ApiResponse<Void>> onRateLimited(RateLimitedException ex) {
        return ResponseEntity.status(ex.status)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfterSeconds))
                .body(ApiResponse.error(ex.status, ex.code, null));
    }

    /**
     * jOOQ translates exceptions thrown by {@code ConnectionProvider.acquire()} into its own
     * {@code DataAccessException}, so the rejection may reach MVC wrapped.
     */
    @ExceptionHandler({org.jooq.exception.DataAccessException.class, org.springframework.dao.DataAccessException.class})
    public ResponseEntity<ApiResponse<Void>> onDataAccess(RuntimeException ex) {
        RateLimitedException limited = findRateLimited(ex);
        if (limited != null) {
            return onRateLimited(limited);
        }

        log.error("Database access failure", ex);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, null));
    }

    private static @Nullable RateLimitedException findRateLimited(Throwable ex) {
        for (Throwable current = ex; current != null; current = current.getCause()) {
            if (current instanceof RateLimitedException limited) {
                return limited;
            }
        }
        return null;
    }
}
