package org.dsqrwym.langora.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ErrorCodeTest {

    @Test
    void everyErrorCodeHasSafeDefaultMessage() {
        for (ErrorCode code : ErrorCode.values()) {
            assertFalse(code.defaultMessage().isBlank(), () -> code + " must have a default message");
        }
    }

    @Test
    void mapsFrameworkStatusesToStableCodes() {
        assertEquals(ErrorCode.MALFORMED_REQUEST, ErrorCode.fromHttpStatus(HttpStatus.BAD_REQUEST));
        assertEquals(ErrorCode.AUTH_ACCESS_TOKEN_INVALID, ErrorCode.fromHttpStatus(HttpStatus.UNAUTHORIZED));
        assertEquals(ErrorCode.ACCESS_DENIED, ErrorCode.fromHttpStatus(HttpStatus.FORBIDDEN));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.fromHttpStatus(HttpStatus.NOT_FOUND));
        assertEquals(ErrorCode.RESOURCE_CONFLICT, ErrorCode.fromHttpStatus(HttpStatus.CONFLICT));
        assertEquals(ErrorCode.INTERNAL_ERROR, ErrorCode.fromHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
