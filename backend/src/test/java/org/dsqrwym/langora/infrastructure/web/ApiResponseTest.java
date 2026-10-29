package org.dsqrwym.langora.infrastructure.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiResponseTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void successResponseCarriesTraceIdAndStableCode() {
        MDC.put(ApiResponse.REQUEST_ID_KEY, "trace-1");

        ApiResponse<String> response = ApiResponse.ok("payload");

        assertEquals(200, response.status());
        assertEquals("OK", response.code());
        assertEquals("trace-1", response.traceId());
        assertEquals("payload", response.data());
    }

    @Test
    void errorResponseUsesErrorCodeDefaultMessage() {
        ApiResponse<Void> response = ApiResponse.error(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                ErrorCode.AUTH_INVALID_CREDENTIALS,
                null
        );

        assertEquals(401, response.status());
        assertEquals("AUTH_INVALID_CREDENTIALS", response.code());
        assertEquals(ErrorCode.AUTH_INVALID_CREDENTIALS.defaultMessage(), response.message());
    }
}
