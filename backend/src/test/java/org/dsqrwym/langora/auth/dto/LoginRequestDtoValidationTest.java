package org.dsqrwym.langora.auth.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRequestDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void normalizesEmailToLowercaseAndTrimsIdentifierText() {
        LoginRequestDto request = new LoginRequestDto(
                "  Admin@Langora.COM  ", null, "old-password", "device-1");

        assertEquals("admin@langora.com", request.email());
    }

    @Test
    void preservesUsernameCaseWhileTrimmingIt() {
        LoginRequestDto request = new LoginRequestDto(
                null, "  Admin.User  ", "old-password", "device-1");

        assertEquals("Admin.User", request.username());
    }

    @Test
    void requiresExactlyOneLoginIdentifier() {
        LoginRequestDto both = new LoginRequestDto(
                "admin@langora.com", "Admin", "old-password", "device-1");
        LoginRequestDto neither = new LoginRequestDto(
                null, null, "old-password", "device-1");

        assertTrue(validator.validate(both).stream()
                .anyMatch(error -> error.getMessage().equals("Provide exactly one of email or username")));
        assertTrue(validator.validate(neither).stream()
                .anyMatch(error -> error.getMessage().equals("Provide exactly one of email or username")));
    }

    @Test
    void requiresPasswordAndDeviceUuid() {
        LoginRequestDto request = new LoginRequestDto(
                "admin@langora.com", null, " ", " ");

        var violations = validator.validate(request);
        assertTrue(violations.stream().anyMatch(error -> error.getPropertyPath().toString().equals("password")));
        assertTrue(violations.stream().anyMatch(error -> error.getPropertyPath().toString().equals("deviceUuid")));
    }

    @Test
    void rejectsPasswordLongerThanBcryptUtf8Limit() {
        // Each Chinese character is three bytes in UTF-8: 25 * 3 = 75 bytes.
        LoginRequestDto request = new LoginRequestDto(
                "admin@langora.com", null, "中".repeat(25), "device-1");

        assertTrue(validator.validate(request).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("password")));
    }

    @Test
    void acceptsExactlySeventyTwoAsciiPasswordBytesDuringLogin() {
        LoginRequestDto request = new LoginRequestDto(
                "admin@langora.com", null, "a".repeat(72), "device-1");

        assertTrue(validator.validate(request).isEmpty());
    }
}
