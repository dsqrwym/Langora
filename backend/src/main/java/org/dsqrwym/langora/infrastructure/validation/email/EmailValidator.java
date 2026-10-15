package org.dsqrwym.langora.infrastructure.validation.email;

import com.sanctionco.jmail.JMail;
import jakarta.validation.ConstraintValidatorContext;
import org.dsqrwym.langora.infrastructure.validation.FieldLimits;

public class EmailValidator implements jakarta.validation.ConstraintValidator<Email, String>{
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        if (value.length() > FieldLimits.EMAIL_MAX_LENGTH){
            return false;
        }

        return JMail.isValid(value);

    }
}
