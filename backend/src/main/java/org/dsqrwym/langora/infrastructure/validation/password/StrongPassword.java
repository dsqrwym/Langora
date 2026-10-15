package org.dsqrwym.langora.infrastructure.validation.password;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a password is strong, defaulting to a minimum length of 6 characters, containing at least one uppercase, one lowercase, and one digit.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Constraint(validatedBy = StrongPasswordValidator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    String message() default "Password must be at least 6 characters, containing at least one uppercase, one lowercase, and one digit";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int minLength() default 6;

    int minUppercase() default 1;

    int minLowercase() default 1;

    int minDigit() default 1;

    int maxBytes() default 72;
}
