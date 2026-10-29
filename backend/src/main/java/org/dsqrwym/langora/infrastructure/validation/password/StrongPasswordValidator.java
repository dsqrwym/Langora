package org.dsqrwym.langora.infrastructure.validation.password;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

/**
 * Validates a password against the configured criteria. Only {@code null} is treated as "not provided"
 * and skipped; a blank value is a provided-but-invalid password and is rejected.
 *
 * <p>Presence is the responsibility of {@code @NotBlank} (or of the cross-field constraint when the
 * value is optional); configuration sources that use empty placeholders should normalise them to
 * {@code null} before this constraint runs.
 */
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    private int minLength;
    private int minUppercase;
    private int minLowercase;
    private int minDigit;
    private int maxBytes;

    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        minLength = constraintAnnotation.minLength();
        minUppercase = constraintAnnotation.minUppercase();
        minLowercase = constraintAnnotation.minLowercase();
        minDigit = constraintAnnotation.minDigit();
        maxBytes = constraintAnnotation.maxBytes();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        if (password.getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            context.buildConstraintViolationWithTemplate("Password must not exceed " + maxBytes + " bytes in UTF-8 encoding")
                    .addConstraintViolation();
            return false;
        }

        if (password.length() < minLength) {
            context.buildConstraintViolationWithTemplate("Password must be at least " + minLength + " characters long")
                    .addConstraintViolation();
            return false;
        }
        if (password.chars().filter(Character::isUpperCase).count() < minUppercase) {
            context.buildConstraintViolationWithTemplate("Password must contain at least " + minUppercase + " uppercase letter(s)")
                    .addConstraintViolation();
            return false;
        }
        if (password.chars().filter(Character::isLowerCase).count() < minLowercase) {
            context.buildConstraintViolationWithTemplate("Password must contain at least " + minLowercase + " lowercase letter(s)")
                    .addConstraintViolation();
            return false;
        }

        if (password.chars().filter(Character::isDigit).count() < minDigit) {
            context.buildConstraintViolationWithTemplate("Password must contain at least " + minDigit + " digit(s)")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
