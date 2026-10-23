package org.dsqrwym.langora.infrastructure.web;

import org.dsqrwym.langora.infrastructure.web.exception.RateLimitedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Test-only endpoints that always fail, used by {@link DbBulkheadExceptionHandlerTest}.
 * Kept as a top-level controller because the {@code @WebMvcTest} type filter does not pick up
 * nested controller classes.
 */
@RestController
public class BulkheadRejectionController {

    @GetMapping("/test/rate-limited")
    void rateLimited() {
        throw new RateLimitedException(Duration.ofSeconds(2));
    }

    @GetMapping("/test/translated")
    void translated() {
        throw new org.jooq.exception.DataAccessException(
                "Error while acquiring connection", new RateLimitedException(Duration.ofSeconds(2)));
    }

    @GetMapping("/test/data-access")
    void dataAccess() {
        throw new org.jooq.exception.DataAccessException("connection refused");
    }
}
