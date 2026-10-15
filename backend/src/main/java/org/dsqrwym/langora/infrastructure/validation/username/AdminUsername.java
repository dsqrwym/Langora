package org.dsqrwym.langora.infrastructure.validation.username;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a username is valid, a minimum length of 3 characters, and a maximum length of 50 characters.
 * Only contains ASCII letters, digits, underscore, dot, or hyphen.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Constraint(validatedBy = AdminUsernameValidator.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminUsername {
    String message() default "Username must be 3-50 characters and only contain ASCII letters, digits, underscore, dot or hyphen";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
