package org.dsqrwym.langora.infrastructure.validation.password;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the semantics of {@link StrongPassword}: {@code null} means "not provided" and is skipped,
 * while a blank value is a provided-but-invalid password and must be rejected.
 */
class StrongPasswordValidatorTest {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @Test
    void nullPasswordIsTreatedAsNotProvided() {
        assertThat(VALIDATOR.validate(new Sample(null))).isEmpty();
    }

    @Test
    void blankPasswordIsRejected() {
        assertThat(VALIDATOR.validate(new Sample(""))).isNotEmpty();
        assertThat(VALIDATOR.validate(new Sample("   "))).isNotEmpty();
    }

    @Test
    void weakPasswordIsRejected() {
        assertThat(VALIDATOR.validate(new Sample("Superadmin"))).isNotEmpty();  // no digit
        assertThat(VALIDATOR.validate(new Sample("superadmin1"))).isNotEmpty(); // no uppercase
        assertThat(VALIDATOR.validate(new Sample("SUPERADMIN1"))).isNotEmpty(); // no lowercase
        assertThat(VALIDATOR.validate(new Sample("Su1"))).isNotEmpty();         // too short
    }

    @Test
    void passwordLongerThanBcryptLimitIsRejected() {
        assertThat(VALIDATOR.validate(new Sample("Aa1" + "x".repeat(70)))).isNotEmpty();
    }

    @Test
    void strongPasswordIsAccepted() {
        assertThat(VALIDATOR.validate(new Sample("SuperAdmin123"))).isEmpty();
        assertThat(VALIDATOR.validate(new Sample("Su1per"))).isEmpty();
    }

    private record Sample(@StrongPassword String password) {
    }
}
