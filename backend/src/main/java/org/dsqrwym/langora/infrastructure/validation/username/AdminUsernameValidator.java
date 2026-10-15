package org.dsqrwym.langora.infrastructure.validation.username;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.dsqrwym.langora.infrastructure.validation.FieldLimits;

import java.util.regex.Pattern;

/**
 * Validates a password based on the specified criteria except null.
 */
public class AdminUsernameValidator implements ConstraintValidator<AdminUsername, String> {
    /**
     * ASCII letters, digits, underscore, dot, hyphen characters allowed in username
     */
    private static final Pattern allowed = Pattern.compile("^[a-zA-Z0-9_.-]+$");

    @Override
    public boolean isValid(String adminUsername, ConstraintValidatorContext context) {
        if (adminUsername == null || adminUsername.isBlank()) {
            return true;
        }

        if (adminUsername.length() < FieldLimits.ADMIN_USERNAME_MIN_LENGTH ||
                adminUsername.length() > FieldLimits.ADMIN_USERNAME_MAX_LENGTH) {
            return false;
        }

        return allowed.matcher(adminUsername).matches();
    }
}
